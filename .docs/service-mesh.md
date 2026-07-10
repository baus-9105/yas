# Tài Liệu Service Mesh (Istio) - Hệ thống YAS

## Tổng quan

Service Mesh là tầng hạ tầng mạng (infrastructure layer) được đặt xen giữa các Microservices, quản lý toàn bộ giao tiếp service-to-service mà **không cần thay đổi code ứng dụng**. YAS sử dụng **Istio** làm Service Mesh, triển khai trong namespace `istio-system`. Toàn bộ cấu hình nằm tại `k8s/istio/`.

---

## 1. Kiến trúc Istio trong YAS

### 1.1. Thành phần

| Component | Namespace | Vai trò |
|-----------|-----------|---------|
| `istiod` | istio-system | Control Plane — cấp cert mTLS, push config xuống Envoy |
| `istio-ingressgateway` | istio-system | Ingress Gateway (hiện không sử dụng, dùng NGINX thay thế) |
| `kiali` | istio-system | Dashboard trực quan hóa Service Mesh topology |
| `jaeger` | istio-system | Distributed tracing UI |
| `grafana` | istio-system | Grafana instance riêng cho Istio metrics |
| Envoy Sidecar | yas (mỗi Pod) | Data Plane — proxy mọi traffic vào/ra container |

### 1.2. Cơ chế Sidecar Injection

Namespace `yas` được gắn label `istio-injection=enabled`:
```bash
kubectl label namespace yas istio-injection=enabled
```

Khi một Pod mới được tạo trong namespace `yas`, Istio Mutating Webhook tự động tiêm thêm container `istio-proxy` (Envoy) vào Pod. Mọi traffic TCP vào/ra container ứng dụng đều bị Envoy chặn lại và xử lý trước (thông qua iptables rules trong Pod).

**Kiểm tra:** Mỗi Pod trong namespace `yas` luôn có 2 containers:
```bash
kubectl get pods -n yas -l app=product
# NAME                      READY   STATUS
# product-xxx-yyy           2/2     Running    ← 2/2 = app container + istio-proxy
```

---

## 2. mTLS (Mutual TLS) — Mã hóa giao tiếp nội bộ

### 2.1. mTLS là gì?

Trong TLS thông thường (HTTPS), chỉ **client** xác minh danh tính của **server** (qua certificate). Trong mTLS (Mutual TLS), **cả hai bên đều xác minh lẫn nhau**: server xác minh client VÀ client xác minh server. Điều này đảm bảo không có kẻ giả mạo nào có thể xen vào giữa (Man-in-the-Middle).

### 2.2. Cách Istio tự động hóa mTLS

1. **Istiod** đóng vai trò Certificate Authority (CA), tự cấp chứng chỉ X.509 cho mỗi Envoy Sidecar.
2. Cert chứa danh tính **SPIFFE ID**: `spiffe://cluster.local/ns/yas/sa/<service-account>`.
3. Cert được tự động rotate (xoay vòng) mỗi 24 giờ — không cần can thiệp thủ công.
4. Khi `storefront-bff` gọi `product`:
   - Envoy của `storefront-bff` xuất trình cert → Envoy của `product` xác minh.
   - Envoy của `product` xuất trình cert → Envoy của `storefront-bff` xác minh.
   - Sau khi xác minh xong, kênh TLS được thiết lập → dữ liệu truyền qua hoàn toàn mã hóa.

### 2.3. Cấu hình PeerAuthentication

**File:** `k8s/istio/peer-authentication.yaml`

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: yas-mtls
  namespace: yas
spec:
  selector:
    matchLabels:
      security.istio.io/tlsMode: istio    # Áp dụng cho mọi Pod có Istio Sidecar
  mtls:
    mode: STRICT                           # Bắt buộc mTLS trên mọi cổng
  portLevelMtls:
    8090:
      mode: PERMISSIVE   # Ngoại lệ: Prometheus scrape metrics bằng HTTP thuần
    80:
      mode: PERMISSIVE   # Ngoại lệ: NGINX Ingress gọi BFFs bằng HTTP thuần
    8080:
      mode: PERMISSIVE   # Ngoại lệ: NGINX Ingress gọi Swagger-UI bằng HTTP thuần
```

**Giải thích các mode:**
| Mode | Ý nghĩa |
|------|---------|
| `STRICT` | CHỈ chấp nhận kết nối mTLS. HTTP thuần bị từ chối ngay lập tức (Connection Reset). |
| `PERMISSIVE` | Chấp nhận CẢ mTLS lẫn HTTP thuần. Dùng cho giai đoạn chuyển đổi hoặc ngoại lệ. |
| `DISABLE` | Tắt mTLS hoàn toàn. Không khuyến khích trong Production. |

**Tại sao cần ngoại lệ PERMISSIVE?**

| Cổng | Lý do | Ai gọi vào? |
|------|-------|-------------|
| 8090 | Prometheus (namespace `observability`) không có Istio Sidecar → không thể gửi mTLS | Prometheus |
| 80 | NGINX Ingress (namespace `ingress-nginx`) không có Sidecar → gửi HTTP thuần tới BFFs | NGINX Ingress |
| 8080 | Tương tự cổng 80, nhưng Swagger-UI chạy trên cổng 8080 thay vì 80 | NGINX Ingress |

**Câu hỏi vấn đáp:** *"Mở PERMISSIVE ở cổng 80 có phải lỗ hổng bảo mật không?"*
→ KHÔNG. Vì lớp bảo vệ thứ 2 (AuthorizationPolicy) sẽ kiểm tra danh tính. Backend services như `product`, `cart` yêu cầu `principals` (chứng chỉ mTLS) → HTTP thuần sẽ bị chặn ở AuthorizationPolicy dù lọt qua PeerAuthentication. Chỉ có BFFs (mở `rules: - {}`) mới chấp nhận request không có danh tính.

### 2.4. Cấu hình DestinationRule

**File:** `k8s/istio/destination-rule.yaml`

```yaml
apiVersion: networking.istio.io/v1
kind: DestinationRule
metadata:
  name: yas-mtls-destination
  namespace: yas
spec:
  host: "*.yas.svc.cluster.local"     # Áp dụng cho TẤT CẢ services trong namespace yas
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL              # Client-side: Luôn gửi mTLS khi gọi đi
```

**Mối quan hệ PeerAuthentication vs DestinationRule:**
- `PeerAuthentication` = **Server-side** (quy định cổng nhận traffic phải có mTLS hay không).
- `DestinationRule` = **Client-side** (quy định khi gọi đi phải đính kèm cert mTLS).
- Cả hai phải đồng bộ: Nếu server yêu cầu STRICT mà client không gửi mTLS → Connection Reset.

---

## 3. Authorization Policy — Kiểm soát truy cập (RBAC)

### 3.1. Mô hình Zero-Trust

**File:** `k8s/istio/authorization-policy.yaml`

Hệ thống áp dụng mô hình **Deny-All by Default** (Cấm tất cả, chỉ cho phép theo danh sách trắng):

```yaml
# Policy gốc: Deny ALL traffic trong namespace yas
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: yas
spec:
  {}   # Không có rules = deny tất cả
```

Sau đó, từng service được mở quyền riêng biệt:

### 3.2. Các loại Authorization Policy

#### Loại 1: Mở toàn bộ (cho Frontend/BFF nhận traffic từ Ingress)

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-storefront-bff
spec:
  selector:
    matchLabels:
      app: storefront-bff
  rules:
  - {}   # Cho phép BẤT KỲ AI gọi vào (vì NGINX Ingress không có mTLS identity)
```

Áp dụng cho: `storefront-bff`, `backoffice-bff`, `storefront-ui`, `backoffice-ui`, `swagger-ui`.

#### Loại 2: Whitelist theo Service Account (cho Backend)

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-product
spec:
  selector:
    matchLabels:
      app: product
  rules:
  - from:
    - source:
        principals:                                    # CHỈ cho phép các SA sau:
        - "cluster.local/ns/yas/sa/storefront-bff"     # storefront-bff
        - "cluster.local/ns/yas/sa/backoffice-bff"     # backoffice-bff
        - "cluster.local/ns/yas/sa/cart"                # cart
        - "cluster.local/ns/yas/sa/order"               # order
        - "cluster.local/ns/yas/sa/rating"              # rating
        - "cluster.local/ns/yas/sa/inventory"           # inventory
        - "cluster.local/ns/yas/sa/promotion"           # promotion
        - "cluster.local/ns/yas/sa/sampledata"          # sampledata
        - "cluster.local/ns/yas/sa/search"              # search
```

#### Loại 3: Cho phép theo Port (cho Prometheus)

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-prometheus
spec:
  action: ALLOW
  rules:
  - to:
    - operation:
        ports: ["8090"]     # Bất kỳ ai cũng được gọi vào cổng 8090
```

### 3.3. Ma trận truy cập (Access Matrix)

Bảng tổng hợp: **AI được gọi AI?** (✅ = được phép, ❌ = bị chặn)

| Caller ↓ \ Target → | product | cart | order | customer | location | media | payment | search | rating | inventory | tax | promotion |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| storefront-bff | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| backoffice-bff | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| cart | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| order | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| rating | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| payment | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| inventory | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| tax | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| search | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Pod lạ (default SA) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 4. Retry Policy — Tự động phục hồi lỗi

### 4.1. Cấu hình VirtualService

**File:** `k8s/istio/virtual-service-retry.yaml`

Áp dụng cho 13 Backend Services (product, cart, order, customer, inventory, location, media, payment, promotion, rating, sampledata, search, tax):

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: product-retry
spec:
  hosts:
  - product
  http:
  - match:
    - port: 80                        # CHỈ áp dụng cho cổng 80 (không ảnh hưởng 8090)
    retries:
      attempts: 3                     # Retry tối đa 3 lần
      perTryTimeout: 5s               # Mỗi lần thử timeout sau 5 giây
      retryOn: "5xx,reset,connect-failure"   # Retry khi: lỗi 5xx, TCP reset, connection failure
    timeout: 30s                      # Tổng thời gian tối đa cho cả request + retries
    route:
    - destination:
        host: product
        port:
          number: 80
```

**Tại sao cần `match: port: 80`?** Nếu không có điều kiện match, VirtualService sẽ áp dụng cho MỌI cổng, bao gồm cả cổng 8090. Điều này khiến Istio bẻ lái traffic metrics (8090) sang cổng 80, gây lỗi 500 khi Prometheus cố scrape metrics.

### 4.2. Cơ chế hoạt động

```
storefront-bff gọi product:80/api/products
    │
    ├── Envoy Sidecar (storefront-bff) gửi request
    │
    ├── Lần 1: product trả 500 Internal Server Error
    │       → Envoy phát hiện "5xx" khớp retryOn
    │       → Đợi backoff → Retry
    │
    ├── Lần 2: product trả 503 Service Unavailable
    │       → Envoy retry lần nữa
    │
    ├── Lần 3: product trả 200 OK
    │       → Envoy forward 200 về cho storefront-bff
    │
    └── storefront-bff nhận 200 OK (không biết đã retry 2 lần)
```

**Điểm quan trọng:** Retry diễn ra ở tầng Sidecar Proxy, hoàn toàn **transparent** (trong suốt) với code ứng dụng. Code Java không cần viết `try/catch/retry`, không cần thư viện Resilience4j. Istio xử lý tất cả ở tầng infrastructure.

---

## 5. Kịch bản Test

### 5.1. Test mTLS + AuthorizationPolicy

```bash
# Test 1: Pod hợp lệ (có mTLS + đúng SA) → PASS
kubectl exec -n yas deploy/storefront-bff -c storefront-bff -- \
  wget -qO- --header="Host: product" http://product.yas:80/ 2>&1 | head -5
# Kết quả: 404 Not Found (kết nối thành công, chỉ là không có route /)

# Test 2: Pod lạ không có mTLS → BLOCKED
kubectl exec -it -n default test-no-mtls -- curl -s -I http://product.yas:80/
# Kết quả: 403 Forbidden (RBAC: access denied)

# Test 3: Pod trong mesh nhưng sai ServiceAccount → BLOCKED
kubectl run test-hacker -n yas --image=curlimages/curl -- sleep 3600
kubectl exec -it -n yas test-hacker -c test-hacker -- curl -s -I http://product.yas:80/
# Kết quả: 403 Forbidden
kubectl delete pod test-hacker -n yas

# Test 4: Prometheus scrape metrics (cổng 8090 PERMISSIVE) → PASS
kubectl exec -it -n default test-no-mtls -- curl -s -I http://product.yas:8090/actuator/health
# Kết quả: 200 OK
```

### 5.2. Test Retry Policy

```bash
# Terminal 1: Theo dõi log của product
kubectl logs -n yas deploy/product -c product -f | grep "ERROR\|Exception"

# Terminal 2: Gọi URL gây lỗi 500 (chỉ gọi 1 lần)
kubectl exec -n yas deploy/storefront-bff -c storefront-bff -- \
  wget -qO- http://product.yas:80/product/actuator/health 2>&1

# Quan sát Terminal 1: Sẽ thấy 4 dòng lỗi (1 gốc + 3 retry)
```

### 5.3. Kiểm tra Kiali Topology

```bash
kubectl port-forward svc/kiali 20001:20001 -n istio-system
# Truy cập http://localhost:20001 → Graph → Namespace: yas
```

---

## 6. Tóm tắt các file cấu hình

| File | Nội dung | Số lượng resources |
|------|----------|--------------------|
| `peer-authentication.yaml` | mTLS STRICT + ngoại lệ port-level | 1 PeerAuthentication |
| `destination-rule.yaml` | Client-side mTLS cho toàn namespace | 1 DestinationRule |
| `authorization-policy.yaml` | Deny-all + Whitelist cho 18 services + Prometheus | 16 AuthorizationPolicy |
| `virtual-service-retry.yaml` | Retry 3 lần cho 13 backend services | 13 VirtualService |
