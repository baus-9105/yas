# Tài Liệu Observability (Giám sát Hệ thống) - Hệ thống YAS

## Tổng quan

Observability (Khả năng quan sát) là khả năng hiểu được trạng thái bên trong của một hệ thống phức tạp chỉ bằng cách quan sát các tín hiệu đầu ra (output signals) của nó. Trong kiến trúc Microservices, Observability là yếu tố sống còn vì một request của người dùng có thể đi qua 5-10 services khác nhau trước khi trả về kết quả.

Hệ thống YAS triển khai Observability dựa trên **3 trụ cột** (Three Pillars of Observability): **Metrics**, **Logs**, và **Traces**. Toàn bộ cấu hình được lưu tại `k8s/deploy/observability/`.

---

## 1. Kiến trúc Observability tổng thể

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Namespace: yas                                   │
│                                                                             │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐                │
│  │ product  │   │  cart    │   │  order   │   │  ...     │   Microservices │
│  │ :8090    │   │ :8090    │   │ :8090    │   │ :8090    │   (Metrics)     │
│  │ (OTel)   │   │ (OTel)   │   │ (OTel)   │   │ (OTel)   │   (Traces)     │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘                │
│       │              │              │              │                        │
│       │ Traces(gRPC) │              │              │                        │
│       └──────────────┴──────────────┴──────────────┘                        │
│                              │                                              │
└──────────────────────────────┼──────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Namespace: observability                              │
│                                                                             │
│  ┌─────────────────────────────────────────────┐                           │
│  │         OpenTelemetry Collector             │    Trạm trung chuyển      │
│  │  ┌──────────────┬──────────────┐            │                           │
│  │  │ Receiver     │ Receiver     │            │                           │
│  │  │ OTLP(:4317)  │ Loki(:3500)  │            │                           │
│  │  └──────┬───────┴──────┬───────┘            │                           │
│  │         │              │                     │                           │
│  │  ┌──────▼───────┐ ┌────▼──────────┐          │                           │
│  │  │ Exporter     │ │ Exporter      │          │                           │
│  │  │ → Tempo:4318 │ │ → Loki-GW     │          │                           │
│  │  └──────────────┘ └───────────────┘          │                           │
│  └─────────────────────────────────────────────┘                           │
│                                                                             │
│  ┌───────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐              │
│  │Prometheus │   │  Loki    │   │  Tempo   │   │ Grafana  │              │
│  │ (Metrics) │   │ (Logs)   │   │ (Traces) │   │ (UI)     │              │
│  └───────────┘   └──────────┘   └──────────┘   └──────────┘              │
│                                                                             │
│  ┌───────────┐   ┌──────────┐                                              │
│  │Promtail   │   │Node      │   Chạy trên MỌI Node (DaemonSet)            │
│  │(Log Agent)│   │Exporter  │                                              │
│  └───────────┘   └──────────┘                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Trụ cột 1: Metrics (Đo lường hiệu suất) — Prometheus

### 2.1. Prometheus là gì?

Prometheus là hệ thống giám sát mã nguồn mở, hoạt động theo mô hình **Pull-based**: Prometheus chủ động gọi đến các endpoint `/metrics` của các ứng dụng theo chu kỳ (mặc định 15 giây/lần) để thu thập dữ liệu. Dữ liệu được lưu trữ dưới dạng **Time-series Database** (chuỗi thời gian).

### 2.2. Prometheus thu thập dữ liệu từ đâu?

#### a) Metrics từ Microservices (Cổng 8090)

Mỗi Spring Boot Microservice trong YAS đều expose endpoint `/actuator/prometheus` trên cổng `8090` (cổng quản lý, tách biệt khỏi cổng nghiệp vụ `80`).

**Tại sao tách cổng 8090 riêng?**
- **Bảo mật:** Cổng 80 được bảo vệ bởi Istio mTLS + AuthorizationPolicy. Nếu Prometheus phải gọi vào cổng 80, nó cần có chứng chỉ mTLS → Phức tạp hóa cấu hình.
- **Giải pháp:** Tách metrics ra cổng 8090 và cấu hình `portLevelMtls: 8090: PERMISSIVE` trong `PeerAuthentication`, cho phép Prometheus gọi bằng HTTP thuần mà không cần mTLS.

**Các metrics quan trọng mà Prometheus thu thập từ Spring Boot:**
| Metric | Ý nghĩa |
|--------|---------|
| `jvm_memory_used_bytes` | RAM đang dùng bởi JVM |
| `jvm_threads_live_threads` | Số thread đang hoạt động |
| `http_server_requests_seconds_count` | Tổng số HTTP request đã xử lý |
| `http_server_requests_seconds_sum` | Tổng thời gian xử lý HTTP request |
| `hikaricp_connections_active` | Số kết nối Database đang hoạt động |
| `spring_kafka_consumer_records_consumed_total` | Số message Kafka đã tiêu thụ |

#### b) Metrics từ Node (DaemonSet `prometheus-node-exporter`)

`node-exporter` chạy trên **mọi Node** (cả Master và Worker) dưới dạng DaemonSet, thu thập các chỉ số phần cứng:

| Metric | Ý nghĩa |
|--------|---------|
| `node_cpu_seconds_total` | Thời gian CPU đã sử dụng |
| `node_memory_MemAvailable_bytes` | RAM khả dụng |
| `node_disk_io_time_seconds_total` | Thời gian I/O của ổ đĩa |
| `node_network_receive_bytes_total` | Lưu lượng mạng nhận vào |

#### c) Metrics từ Istio Envoy Sidecars

Istio Envoy Proxy tự động expose metrics về lưu lượng mạng giữa các service:

| Metric | Ý nghĩa |
|--------|---------|
| `istio_requests_total` | Tổng số request giữa 2 services |
| `istio_request_duration_milliseconds` | Độ trễ (latency) của mỗi request |
| `istio_tcp_connections_opened_total` | Số kết nối TCP đã mở |

### 2.3. Cấu hình Prometheus (`prometheus.values.yaml`)

```yaml
prometheus:
  prometheusSpec:
    enableRemoteWriteReceiver: true   # Cho phép Tempo ghi metrics ngược vào Prometheus
grafana:
  grafana.ini:
    database:
      type: postgres                  # Lưu cấu hình Grafana vào PostgreSQL (không mất khi restart)
      host: postgresql.postgres:5432
  adminUser: admin
  adminPassword: admin
  ingress:
    ingressClassName: nginx
    enabled: true
    hosts:
      - grafana.yas.local.com         # Truy cập Grafana qua domain
```

**Tại sao `enableRemoteWriteReceiver: true`?**
→ Tempo (hệ thống Traces) có tính năng tạo **metrics từ traces** (ví dụ: tính toán tỷ lệ lỗi, latency P99 từ dữ liệu trace). Các metrics này được ghi ngược vào Prometheus qua Remote Write API, giúp tạo dashboard tổng hợp trên Grafana.

---

## 3. Trụ cột 2: Logs (Quản lý Nhật ký) — Loki + Promtail

### 3.1. Tại sao không dùng ELK Stack (Elasticsearch + Logstash + Kibana)?

| Tiêu chí | ELK Stack | Loki Stack |
|----------|-----------|------------|
| RAM tối thiểu | 4-8 GB (Elasticsearch rất tốn RAM) | 512 MB - 1 GB |
| Cách đánh index | Full-text indexing (index mọi từ trong log) | Label-based indexing (chỉ index metadata) |
| Tốc độ truy vấn log cụ thể | Rất nhanh (nhờ full-text index) | Nhanh (lọc theo labels trước, rồi grep) |
| Dung lượng lưu trữ | Rất lớn (index chiếm nhiều) | Nhỏ hơn 10-100x |
| Tích hợp Grafana | Cần Kibana riêng | Native (cùng hệ sinh thái Grafana Labs) |

**Kết luận:** Với cluster nhỏ (1 Master + 1 Worker), Loki Stack là lựa chọn tối ưu hơn hẳn về tài nguyên.

### 3.2. Luồng thu thập Log

```
Container viết log vào stdout/stderr
        │
        ▼
Kubernetes lưu log vào file: /var/log/containers/<pod>_<namespace>_<container>-<id>.log
        │
        ▼
Promtail (DaemonSet trên mỗi Node) mount thư mục /var/log/containers/ và đọc file log
        │
        ▼
Promtail gửi log đến OpenTelemetry Collector (:3500)
        │
        ▼
OTel Collector xử lý (thêm labels: namespace, container, pod, level, traceId)
        │
        ▼
OTel Collector forward đến Loki Gateway → Loki lưu trữ
```

### 3.3. Cấu hình Promtail (`promtail.values.yaml`)

```yaml
config:
  clients:
    - url: http://opentelemetry-collector:3500/loki/api/v1/push
  snippets:
    pipelineStages:
      - docker: {}    # Parse log format của Docker container
```

**Tại sao Promtail gửi đến OTel Collector thay vì gửi thẳng vào Loki?**
→ OTel Collector đóng vai trò **trạm trung chuyển thống nhất**. Bằng cách đi qua OTel, chúng ta có thể:
1. Thêm labels tự động (`namespace`, `container`, `traceId`,...) vào log.
2. Trong tương lai, dễ dàng thêm các exporter khác (ví dụ: gửi log sang Elasticsearch backup) mà không cần sửa Promtail.

### 3.4. Cấu hình Loki (`loki.values.yaml`)

```yaml
write:
  replicas: 1       # Chỉ 1 replica (cluster nhỏ)
read:
  replicas: 1
backend:
  replicas: 1
loki:
  commonConfig:
    replication_factor: 1
  auth_enabled: false     # Tắt multi-tenancy (không cần trong môi trường đơn)
storage:
  type: 'filesystem'      # Lưu trên ổ đĩa local (không dùng S3)
minio:
  enabled: true           # MinIO làm object storage cho Loki
```

**Câu hỏi vấn đáp:** *"Tại sao Loki cần MinIO?"*
→ Kiến trúc Loki chia thành 3 thành phần (write, read, backend). Chúng cần một nơi lưu trữ dùng chung (shared storage) để trao đổi dữ liệu. MinIO cung cấp giao diện S3-compatible ngay trong cluster, tránh phụ thuộc vào dịch vụ cloud bên ngoài.

---

## 4. Trụ cột 3: Traces (Truy vết phân tán) — Tempo + OpenTelemetry

### 4.1. Bài toán Distributed Tracing

Khi người dùng nhấn nút "Đặt hàng" trên Storefront:

```
storefront-bff → product (kiểm tra sản phẩm)
               → cart (lấy giỏ hàng)
               → customer (lấy thông tin khách)
               → inventory (kiểm tra tồn kho)
               → order (tạo đơn hàng)
               → payment (xử lý thanh toán)
               → tax (tính thuế)
```

Nếu request này mất 5 giây để hoàn thành, làm sao biết thời gian kẹt ở service nào? **Distributed Tracing** giải quyết bài toán này bằng cách gắn một **Trace ID** duy nhất vào request ban đầu và truyền qua tất cả services. Mỗi service tạo ra một **Span** (đoạn thời gian) ghi lại thời điểm bắt đầu và kết thúc xử lý.

### 4.2. Cách Traces được thu thập trong YAS

**Bước 1 — Application Instrumentation:**
Mỗi Spring Boot service được tích hợp OpenTelemetry SDK. SDK tự động:
- Tạo Trace ID cho request đầu tiên.
- Tạo Span cho mỗi hàm xử lý (Controller, Service, Repository).
- Truyền Trace ID qua HTTP Header `traceparent` khi gọi sang service khác.

**Bước 2 — Gửi Spans về OTel Collector:**
SDK gửi dữ liệu Span về OpenTelemetry Collector qua gRPC (port `4317`).

**Bước 3 — OTel Collector forward đến Tempo:**
```yaml
# Cấu hình OTel Collector (opentelemetry/values.yaml)
exporters:
  otlphttp:
    endpoint: http://tempo:4318   # Gửi traces đến Tempo qua HTTP

service:
  pipelines:
    traces:
      receivers: [otlp]           # Nhận traces từ applications
      processors: [batch]         # Gom batch để tối ưu hiệu suất
      exporters: [otlphttp]       # Forward đến Tempo
```

**Bước 4 — Tempo lưu trữ và phục vụ truy vấn:**
Grafana kết nối đến Tempo để hiển thị biểu đồ **Waterfall** (thác nước), cho thấy chính xác thời gian mỗi service xử lý.

### 4.3. Cấu hình Tempo (`tempo.values.yaml`)

```yaml
tempo:
  metricsGenerator:
    enabled: true
    remoteWriteUrl: "http://prometheus-kube-prometheus-prometheus:9090/api/v1/write"
```

**Giải thích `metricsGenerator`:**
Tempo có khả năng tự động tạo ra **RED metrics** (Rate, Error, Duration) từ dữ liệu traces:
- **Rate:** Số request/giây của mỗi service.
- **Error:** Tỷ lệ lỗi (5xx) của mỗi service.
- **Duration:** Latency P50/P95/P99 của mỗi service.

Các metrics này được ghi vào Prometheus qua `remoteWriteUrl`, cho phép tạo dashboard hiệu suất trên Grafana mà KHÔNG cần cấu hình thêm Prometheus scrape rules.

---

## 5. OpenTelemetry Collector — Trạm trung chuyển thống nhất

### 5.1. Tại sao cần OTel Collector?

Thay vì để mỗi ứng dụng gửi trực tiếp đến Loki và Tempo (tạo ra coupling chặt), OTel Collector đóng vai trò **abstraction layer**:

```
Không có OTel Collector:
  App → Tempo (traces)
  Promtail → Loki (logs)
  Prometheus → scrape (metrics)
  → 3 luồng riêng biệt, khó quản lý

Có OTel Collector:
  App → OTel Collector → Tempo (traces)
  Promtail → OTel Collector → Loki (logs)
  → Một điểm tập trung, dễ thêm/bớt exporter
```

### 5.2. Cấu hình OTel Collector chi tiết

```yaml
receivers:
  otlp:                         # Nhận Traces từ applications
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317  # gRPC endpoint (hiệu suất cao)
      http:
        endpoint: 0.0.0.0:4318  # HTTP endpoint (fallback)
  loki:                         # Nhận Logs từ Promtail
    protocols:
      http:
        endpoint: 0.0.0.0:3500

processors:
  batch: {}                     # Gom batch traces trước khi gửi (tối ưu network I/O)
  attributes:
    actions:
      - action: insert
        key: loki.attribute.labels
        value: namespace,container,pod,level,traceId   # Gắn labels để Loki indexing
      - action: insert
        key: loki.format
        value: raw              # Giữ nguyên format log gốc

exporters:
  loki:
    endpoint: http://loki-gateway/loki/api/v1/push     # Forward logs đến Loki
  otlphttp:
    endpoint: http://tempo:4318                         # Forward traces đến Tempo

service:
  pipelines:
    logs:
      receivers: [loki]
      processors: [attributes]
      exporters: [loki]
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlphttp]
```

**Điểm đặc biệt — Label `traceId` trong Logs:**
OTel Collector tự động gắn label `traceId` vào mỗi dòng log. Nhờ đó, trên Grafana, khi đang xem một trace chậm trong Tempo, có thể nhấn vào nút "View Logs" để nhảy sang Loki xem chính xác dòng log nào được ghi tại thời điểm đó. Đây gọi là **Correlation** (tương quan giữa Logs và Traces).

---

## 6. Grafana — Giao diện tập trung

### 6.1. Data Sources

Grafana kết nối đồng thời tới 3 nguồn dữ liệu:

| Data Source | Loại dữ liệu | Endpoint nội bộ |
|-------------|---------------|-----------------|
| Prometheus | Metrics | `http://prometheus-kube-prometheus-prometheus:9090` |
| Loki | Logs | `http://loki-gateway:80` |
| Tempo | Traces | `http://tempo:3100` |

### 6.2. Truy cập Grafana

Grafana được expose qua NGINX Ingress với domain `grafana.yas.local.com`:
```bash
# Thêm vào /etc/hosts:
<Worker-Node-IP>    grafana.yas.local.com

# Truy cập: http://grafana.yas.local.com
# Login: admin / admin
```

### 6.3. Tính năng Correlation (Tương quan dữ liệu)

Đây là sức mạnh lớn nhất khi dùng bộ công cụ Grafana Labs (Prometheus + Loki + Tempo). Kịch bản debug một lỗi production:

1. **Phát hiện bất thường trên Metrics:** Dashboard Grafana hiển thị đỉnh gai (spike) tỷ lệ lỗi 5xx trên service `payment` lúc 14:30.
2. **Drill-down vào Logs:** Nhấn vào đỉnh gai, chọn "Explore Logs" → Grafana tự động chuyển sang Loki, lọc logs của `payment` trong khoảng 14:29-14:31. Phát hiện dòng log: `NullPointerException at PaymentService.java:142`.
3. **Truy vết Request Path:** Trong dòng log có kèm `traceId=abc123`. Nhấn vào → Grafana chuyển sang Tempo, hiển thị biểu đồ Waterfall: `storefront-bff (2ms) → order (5ms) → payment (FAILED after 3000ms)`. → Kết luận: Lỗi xảy ra tại service `payment`, hàm `processPayment()`, mất 3 giây trước khi crash.

---

## 7. Tích hợp với Istio Service Mesh

### 7.1. PeerAuthentication cho Observability

Prometheus scrape metrics qua HTTP thuần (không có mTLS) trên cổng `8090`. Để cho phép điều này khi mTLS đang ở chế độ STRICT, cấu hình `PeerAuthentication` đã được thiết lập:

```yaml
portLevelMtls:
  8090:
    mode: PERMISSIVE   # Cho phép cả mTLS lẫn HTTP thuần trên cổng metrics
```

### 7.2. AuthorizationPolicy cho Prometheus

Policy `allow-prometheus` cho phép bất kỳ ai truy cập cổng 8090:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-prometheus
  namespace: yas
spec:
  action: ALLOW
  rules:
  - to:
    - operation:
        ports:
        - "8090"
```

**Lưu ý bảo mật:** Policy này hiện đang mở rộng (cho phép mọi nguồn). Để bảo mật hơn, có thể thêm `source.namespaces: ["observability"]` để chỉ cho phép Prometheus từ namespace `observability`.

---

## 8. Lệnh kiểm tra & Xác minh

```bash
# Kiểm tra tất cả pod observability đang chạy
kubectl get pods -n observability

# Kiểm tra Prometheus có scrape được metrics không
kubectl exec -n observability deploy/prometheus-kube-prometheus-prometheus -- \
  wget -qO- http://product.yas:8090/actuator/prometheus | head -20

# Kiểm tra Loki có nhận được logs không
kubectl exec -n observability deploy/loki-read -- \
  wget -qO- "http://localhost:3100/loki/api/v1/query?query={namespace=\"yas\"}&limit=5"

# Kiểm tra OTel Collector đang chạy
kubectl logs -n observability deploy/opentelemetry-collector --tail=20
```
