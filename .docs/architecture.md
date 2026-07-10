# Tài Liệu Kiến Trúc Hệ Thống - YAS (Yet Another Shop)

## Tổng quan

YAS là một ứng dụng thương mại điện tử (E-commerce) được xây dựng theo kiến trúc **Microservices**, sử dụng **Java 25 / Spring Boot 3** cho Backend và **Next.js** cho Frontend. Toàn bộ hệ thống được triển khai trên cụm **Kubernetes** (1 Master + 1 Worker) chạy trên Google Cloud Platform.

---

## 1. Cấu trúc cụm Kubernetes (Cluster Topology)

### 1.1. Phần cứng

| Node | Vai trò | IP nội bộ | OS | Container Runtime |
|------|---------|-----------|----|--------------------|
| `yas-master` | Control Plane | `10.148.0.2` | Ubuntu 22.04 | containerd 2.2.1 |
| `yas-worker` | Worker Node | `10.148.0.3` | Ubuntu 22.04 | containerd 2.2.5 |

- **Master Node** chỉ chạy các thành phần hệ thống Kubernetes (etcd, kube-apiserver, kube-scheduler, CoreDNS, Calico CNI) và các DaemonSet thu thập metrics (node-exporter, promtail).
- **Worker Node** gánh toàn bộ 73+ Pods ứng dụng và hạ tầng.

### 1.2. Phân vùng Namespace

Hệ thống chia các workload vào **19 namespace** riêng biệt theo nguyên tắc **Separation of Concerns**:

| Namespace | Mục đích | Số lượng Pods | Thành phần chính |
|-----------|----------|---------------|------------------|
| `yas` | Ứng dụng chính | ~36 | 18 microservices + Istio sidecars |
| `istio-system` | Service Mesh | ~5 | istiod, ingressgateway, kiali, jaeger, grafana |
| `argocd` | CD GitOps | ~7 | server, repo-server, controller, redis |
| `observability` | Giám sát | ~12 | Prometheus, Loki, Tempo, Grafana, OTel, Promtail |
| `kafka` | Message Broker | ~4 | Kafka Broker, Strimzi Operator, AKHQ |
| `postgres` | Database | ~3 | PostgreSQL (Zalando Operator), PgAdmin |
| `elasticsearch` | Search Engine | ~3 | Elasticsearch, Kibana, ECK Operator |
| `redis` | Cache | ~1 | Redis |
| `keycloak` | Identity Provider | ~2 | Keycloak Server |
| `ingress-nginx` | Ingress Controller | ~1 | NGINX (hostNetwork: true) |
| `jenkins` | CI/CD Server | ~1 | Jenkins Controller |
| `cert-manager` | TLS Certificate | ~3 | cert-manager, cainjector, webhook |
| `zookeeper` | Coordination | ~1 | ZooKeeper (cho Kafka) |

**Câu hỏi vấn đáp:** *"Tại sao không để tất cả vào một namespace?"*
→ Namespace giúp:
1. **Phân quyền RBAC:** Team Dev chỉ được quyền truy cập namespace `yas`, không được đụng vào `postgres` hay `kafka`.
2. **Cách ly tài nguyên:** Có thể đặt ResourceQuota (giới hạn CPU/RAM) cho từng namespace.
3. **Cách ly mạng:** NetworkPolicy/Istio AuthorizationPolicy hoạt động theo namespace.
4. **Quản lý lifecycle:** Xóa namespace Developer (`dev-john`) mà không ảnh hưởng đến Production.

---

## 2. Kiến trúc ứng dụng Microservices

### 2.1. Sơ đồ tổng thể

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    Internet                             │
                    └────────────────────────┬────────────────────────────────┘
                                             │
                                  ┌──────────▼──────────┐
                                  │   NGINX Ingress     │
                                  │  (hostNetwork:true)  │
                                  └──────────┬──────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
          ┌─────────▼────────┐   ┌───────────▼──────────┐   ┌────────▼───────┐
          │  storefront-bff  │   │   backoffice-bff     │   │   swagger-ui   │
          │  (Gateway :80)   │   │   (Gateway :80)      │   │   (:8080)      │
          └─────────┬────────┘   └───────────┬──────────┘   └────────────────┘
                    │                        │
       ┌────────────┼───────────┬────────────┼───────────┬──────────────┐
       │            │           │            │           │              │
  ┌────▼───┐  ┌─────▼──┐  ┌────▼───┐  ┌─────▼──┐  ┌────▼───┐  ┌──────▼───┐
  │product │  │  cart   │  │ order  │  │customer│  │ media  │  │   ...    │
  │  :80   │  │  :80   │  │  :80   │  │  :80   │  │  :80   │  │   :80   │
  └────┬───┘  └────┬───┘  └────┬───┘  └────┬───┘  └────┬───┘  └──────┬──┘
       │           │           │           │           │             │
       └───────────┴───────────┴───────────┴───────────┴─────────────┘
                                       │
                    ┌──────────────────┼──────────────────────┐
                    │                  │                      │
              ┌─────▼─────┐     ┌──────▼─────┐      ┌────────▼────────┐
              │ PostgreSQL │     │   Kafka    │      │  Elasticsearch  │
              │  (postgres)│     │   (kafka)  │      │ (elasticsearch) │
              └───────────┘     └────────────┘      └─────────────────┘
```

### 2.2. Danh sách các Microservices

#### Nhóm Frontend (Next.js)

| Service | Vai trò | Cổng | Ghi chú |
|---------|---------|------|---------|
| `storefront-ui` | Giao diện mua hàng cho khách hàng | 3000 | Server-Side Rendering (SSR) |
| `backoffice-ui` | Giao diện quản trị cho admin | 3000 | Server-Side Rendering (SSR) |

#### Nhóm BFF (Backend For Frontend)

| Service | Vai trò | Cổng | Ghi chú |
|---------|---------|------|---------|
| `storefront-bff` | API Gateway cho storefront-ui | 80 | Aggregator pattern, gom nhiều API backend |
| `backoffice-bff` | API Gateway cho backoffice-ui | 80 | Aggregator pattern, gom nhiều API backend |

**Tại sao cần BFF?**
Frontend Next.js không gọi trực tiếp đến 13 Backend services. Thay vào đó, nó gọi qua BFF. BFF đóng vai trò:
1. **Aggregation:** Gom nhiều API calls thành 1 response (ví dụ: trang chi tiết sản phẩm cần gọi `product` + `rating` + `media` + `inventory`).
2. **Authentication Proxy:** BFF kiểm tra JWT token từ Keycloak trước khi forward request xuống Backend.
3. **Response Transformation:** Biến đổi response từ Backend cho phù hợp với nhu cầu của Frontend.

#### Nhóm Backend Core (Spring Boot)

| Service | Vai trò | Cổng App | Cổng Metrics | Phụ thuộc chính |
|---------|---------|----------|--------------|-----------------|
| `product` | Quản lý sản phẩm, danh mục, thương hiệu | 80 | 8090 | PostgreSQL, Elasticsearch, Media |
| `cart` | Quản lý giỏ hàng | 80 | 8090 | PostgreSQL, Product |
| `order` | Quản lý đơn hàng, checkout | 80 | 8090 | PostgreSQL, Kafka, Cart, Product, Tax, Payment |
| `customer` | Quản lý thông tin khách hàng, địa chỉ | 80 | 8090 | PostgreSQL, Keycloak |
| `payment` | Xử lý thanh toán | 80 | 8090 | PostgreSQL, Media |
| `inventory` | Quản lý tồn kho, warehouse | 80 | 8090 | PostgreSQL, Location, Product |
| `location` | Quản lý quốc gia, tỉnh/thành, quận/huyện | 80 | 8090 | PostgreSQL |
| `media` | Upload và phục vụ hình ảnh/video | 80 | 8090 | PostgreSQL, File Storage |
| `rating` | Đánh giá, review sản phẩm | 80 | 8090 | PostgreSQL, Order, Customer |
| `search` | Tìm kiếm full-text sản phẩm | 80 | 8090 | Elasticsearch, Product |
| `tax` | Tính thuế theo khu vực | 80 | 8090 | PostgreSQL, Location |
| `promotion` | Quản lý khuyến mãi, mã giảm giá | 80 | 8090 | PostgreSQL, Product |
| `sampledata` | Tạo dữ liệu mẫu để test | 80 | 8090 | Product, Keycloak |

#### Nhóm tiện ích

| Service | Vai trò | Cổng |
|---------|---------|------|
| `swagger-ui` | Giao diện Swagger/OpenAPI cho tất cả Backend APIs | 8080 |
| `yas-reloader` | Tự động restart pods khi ConfigMap/Secret thay đổi (Stakater Reloader) | - |

### 2.3. Mẫu thiết kế Helm Chart (DRY Pattern)

Thay vì viết 13 Helm Chart riêng cho 13 Backend services (gây trùng lặp), hệ thống sử dụng mô hình **Umbrella Chart**:

```
k8s/charts/
├── backend/                    ← Library Chart dùng chung (Deployment, Service, Ingress, SA)
│   ├── templates/
│   │   ├── deployment.yaml     ← Template Deployment chung cho mọi Backend
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   └── serviceaccount.yaml
│   └── values.yaml             ← Giá trị mặc định (httpPort: 80, metricPort: 8090,...)
│
├── product/                    ← Wrapper Chart cho Product Service
│   ├── Chart.yaml              ← dependencies: [backend]
│   └── values.yaml             ← Override: image, databaseName, extraEnvs,...
│
├── cart/                       ← Wrapper Chart cho Cart Service
│   ├── Chart.yaml
│   └── values.yaml
│
└── ...                         ← Tương tự cho 11 services còn lại
```

**Cách hoạt động:** Mỗi service chart (ví dụ: `product/Chart.yaml`) khai báo dependency vào `backend` chart. Khi Helm render, nó sẽ lấy templates từ `backend/` và áp values từ `product/values.yaml` vào. Nhờ đó, khi cần thay đổi cấu hình chung (ví dụ: thêm annotation), chỉ cần sửa 1 file `backend/templates/deployment.yaml`.

**Các tham số quan trọng trong template Deployment:**

```yaml
# Mỗi Pod Backend được cấu hình:
containers:
  - image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
    envFrom:
      - secretRef:
          name: yas-postgresql-credentials-secret   # Lấy DB password từ K8s Secret
    env:
      - name: SPRING_DATASOURCE_URL
        value: jdbc:postgresql://.../$DATABASE_NAME  # URL kết nối DB riêng cho từng service
      - name: SPRING_CONFIG_ADDITIONAL_LOCATION
        value: /opt/yas/config/application.yaml      # Config chung từ ConfigMap
    volumeMounts:
      - mountPath: /opt/yas/config
        name: yas-configuration                      # Mount ConfigMap dùng chung
    ports:
      - containerPort: 80     # Cổng nghiệp vụ (HTTP)
        name: http
      - containerPort: 8090   # Cổng metrics (Prometheus scrape)
        name: metric
```

---

## 3. Hạ tầng (Infrastructure Components)

### 3.1. Luồng triển khai hạ tầng

Toàn bộ hạ tầng được cài đặt bằng script `k8s/deploy/setup-cluster.sh`, thực thi các lệnh `helm upgrade --install` theo thứ tự:

```
1. PostgreSQL (Zalando Operator + Cluster)
2. Kafka (Strimzi Operator + Cluster + Debezium Connector)
3. Elasticsearch (ECK Operator + Cluster + Kibana)
4. Redis
5. ZooKeeper
6. Observability Stack (Loki → Tempo → OTel → Promtail → Prometheus + Grafana)
7. Cert-Manager
8. Keycloak
```

### 3.2. Chi tiết từng thành phần

#### PostgreSQL (Namespace: `postgres`)

- **Operator:** Zalando Postgres Operator — quản lý lifecycle của PostgreSQL cluster (failover, backup, clone).
- **Cluster:** Mỗi Microservice có database riêng (multi-database, single cluster) trên cùng một PostgreSQL instance.
- **PgAdmin:** Giao diện web quản trị database, truy cập qua `pgadmin.yas.local.com`.

#### Kafka (Namespace: `kafka`)

- **Operator:** Strimzi — quản lý Kafka cluster trên Kubernetes.
- **Kafka Broker:** Xử lý message queue giữa các services (ví dụ: `order` publish event → `search` consume để reindex sản phẩm).
- **Debezium Connector:** Bắt thay đổi từ PostgreSQL (Change Data Capture) và đẩy vào Kafka topic.
- **AKHQ:** Giao diện web quản lý Kafka topics, consumers, messages.

#### Elasticsearch (Namespace: `elasticsearch`)

- **Operator:** Elastic Cloud on Kubernetes (ECK).
- **Elasticsearch:** Full-text search engine cho service `search`.
- **Kibana:** Giao diện web xem logs và debug Elasticsearch queries.

#### Keycloak (Namespace: `keycloak`)

- **Vai trò:** Identity Provider (IdP) — quản lý xác thực người dùng, phát hành JWT token.
- **Realm:** `yas` realm chứa toàn bộ cấu hình OAuth2/OIDC cho ứng dụng.
- **Tích hợp:** BFFs validate JWT token bằng cách gọi Keycloak JWKS endpoint.

---

## 4. Luồng mạng (Network Flow)

### 4.1. Inbound Traffic (Người dùng → Ứng dụng)

```
Trình duyệt (storefront.yas.local.com)
    │
    ├── DNS phân giải → IP Worker Node (10.148.0.3 hoặc Public IP)
    │
    ├── TCP port 80 → Vào thẳng tiến trình NGINX (hostNetwork:true, không qua kube-proxy)
    │
    ├── NGINX đọc Host header → Tra bảng Ingress rules
    │       → storefront.yas.local.com/            → storefront-bff:80
    │       → storefront.yas.local.com/swagger-ui  → production-swagger-ui:8080
    │       → backoffice.yas.local.com/             → backoffice-bff:80
    │
    ├── NGINX gửi request đến Pod IP qua mạng Calico CNI
    │
    └── Istio Sidecar (Envoy) của Pod đích nhận request
            → Kiểm tra PeerAuthentication (mTLS hay PERMISSIVE?)
            → Kiểm tra AuthorizationPolicy (có quyền không?)
            → Forward vào container ứng dụng
```

### 4.2. East-West Traffic (Service → Service)

```
storefront-bff gọi http://product:80/api/products
    │
    ├── Istio Sidecar của storefront-bff chặn request
    │       → Resolve "product" → lấy danh sách Pod IPs
    │       → Client-side Load Balancing → chọn 1 Pod IP
    │       → Bọc mTLS (mã hóa TLS bằng cert tự cấp của Istio)
    │
    ├── Gói tin mã hóa đi qua mạng Calico CNI
    │
    └── Istio Sidecar của product nhận gói tin
            → Giải mã mTLS
            → Đọc principals (danh tính): cluster.local/ns/yas/sa/storefront-bff
            → Kiểm tra AuthorizationPolicy allow-product → PASS ✓
            → Forward HTTP thuần vào container product (Tomcat/Spring Boot)
```

### 4.3. Outbound Traffic (Cluster → Internet)

```
ArgoCD cần pull manifest từ github.com/baus-9105/yas-gitops
    │
    ├── Pod ArgoCD hỏi CoreDNS → resolve github.com → IP
    │
    ├── Gói tin rời Pod (IP nguồn: 192.168.x.x — IP ảo)
    │       → Calico CNI + iptables SNAT
    │       → Thay IP nguồn = IP thật Node (10.148.0.3)
    │
    ├── Gói tin đi qua GCP VPC → Internet → GitHub
    │
    └── Response quay lại → DNAT ngược → về Pod ArgoCD
```

---

## 5. Xác thực và Phân quyền

### 5.1. Xác thực người dùng (End-user Authentication)

```
Người dùng → Storefront UI → Keycloak Login Page
                                    │
                            Nhập username/password
                                    │
                            Keycloak phát JWT Token
                                    │
Storefront UI gửi JWT trong header Authorization: Bearer <token>
        │
        ▼
Storefront-BFF validate JWT (gọi Keycloak JWKS endpoint)
        │
   Nếu hợp lệ → Forward request đến Backend services
   Nếu không   → Trả về 401 Unauthorized
```

### 5.2. Xác thực giữa các Services (Service-to-Service Authentication)

Istio mTLS tự động cấp chứng chỉ X.509 cho mỗi Pod thông qua quy trình:
1. Istiod (Certificate Authority) cấp cert cho mỗi Envoy Sidecar.
2. Cert chứa **SPIFFE ID**: `spiffe://cluster.local/ns/yas/sa/<service-account-name>`.
3. Khi service A gọi service B, Sidecar A xuất trình cert → Sidecar B xác minh cert.
4. AuthorizationPolicy kiểm tra SPIFFE ID để quyết định cho phép hay từ chối.

---

## 6. Lệnh kiểm tra cấu trúc Cluster

```bash
# Xem tất cả namespace
kubectl get ns

# Đếm pods trên mỗi node
kubectl get pods -A -o jsonpath='{range .items[*]}{.spec.nodeName}{"\n"}{end}' | sort | uniq -c

# Xem tất cả services trong namespace yas
kubectl get svc -n yas

# Xem kiến trúc mạng (Calico)
kubectl get ippool -o wide

# Xem chi tiết một Pod (bao gồm cả sidecar container)
kubectl describe pod -n yas -l app=product
```
