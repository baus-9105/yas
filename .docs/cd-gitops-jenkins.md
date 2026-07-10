# Tài Liệu Continuous Deployment (CD) - Hệ thống YAS

## Tổng quan

Hệ thống YAS triển khai CD theo 2 mô hình song song, phục vụ 2 mục đích hoàn toàn khác nhau:
- **Jenkins Pipeline** → Phục vụ Developer test code nhanh trên môi trường K8s thật (On-demand, tạm thời).
- **ArgoCD + GitOps** → Phục vụ triển khai tự động cho 3 môi trường chính thức (Dev / Staging / Production).

---

## 1. Jenkins: Môi trường Developer cô lập (Developer Build)

### 1.1. Bài toán cần giải quyết

Khi Developer đang phát triển tính năng mới trên branch `dev_tax_service`, họ cần test code trên môi trường Kubernetes thật (không phải localhost) để kiểm tra:
- Các kết nối HTTP giữa các Microservices có hoạt động không?
- Kubernetes Service Discovery có resolve đúng không?
- Ingress routing có trả đúng domain không?

Tuy nhiên, họ **không thể deploy trực tiếp lên Dev/Staging** vì sẽ phá vỡ môi trường của cả team. Jenkins giải quyết bài toán này bằng cách tạo ra **môi trường K8s cô lập hoàn toàn** cho từng Developer.

### 1.2. Cách sử dụng (Dành cho Developer)

1. Truy cập Jenkins Dashboard.
2. Mở Job **`developer_build`**.
3. Nhấn **"Build with Parameters"**.
4. Điền các thông số:
   - `DEVELOPER_NAMESPACE`: Tên namespace riêng (ví dụ: `dev-john`, `test-feature-x`).
   - Các ô service: Để `main` nếu muốn dùng phiên bản mới nhất. Nhập tên branch (ví dụ: `dev_tax_service`) nếu muốn deploy branch riêng.
5. Nhấn **"Build"**.
6. Sau khi hoàn tất, Jenkins in ra thông tin truy cập:
   ```
   HƯỚNG DẪN TRUY CẬP:
   Hãy mở file hosts trên máy cá nhân và thêm:
   
   34.126.xxx.xxx    api-dev-john.yas.local.com
   34.126.xxx.xxx    storefront-dev-john.yas.local.com
   34.126.xxx.xxx    backoffice-dev-john.yas.local.com
   ```
7. Sau khi test xong, chạy Job **`cleanup_developer_build`** để xóa namespace.

### 1.3. Chi tiết kỹ thuật Pipeline

#### Stage 1: Resolve Image Tags

Pipeline phải xác định Docker Image Tag cho từng service. Logic như sau:

```
Developer nhập "main" cho product     →  Image tag = "latest"
Developer nhập "dev_tax_service" cho tax  →  Pipeline chạy git ls-remote để lấy commit SHA
                                           →  Image tag = "a1b2c3d4e5f6..."
```

**Cơ chế hoạt động chi tiết:**
```groovy
// Nếu branch = main → dùng tag "latest"
if (branch == 'main') {
    serviceTags[svc] = 'latest'
} else {
    // Nếu branch khác → query GitHub API để lấy commit ID mới nhất
    def commitId = sh(
        script: "git ls-remote ${env.GITHUB_REPO} refs/heads/${branch} | awk '{print \$1}'",
        returnStdout: true
    ).trim()
    
    // Kiểm tra branch có tồn tại không
    if (commitId == '') {
        error("Không tìm thấy branch '${branch}' cho service '${svc}'!")
    }
    
    serviceTags[svc] = commitId   // Dùng commit SHA làm image tag
}
```

**Câu hỏi vấn đáp:** *"Tại sao dùng `git ls-remote` thay vì `git log`?"*
→ Vì `git ls-remote` query trực tiếp GitHub API mà **không cần clone toàn bộ repo**. Nó chỉ trả về commit SHA mới nhất của branch chỉ định, rất nhanh và nhẹ.

#### Stage 2: Deploy Services via Helm

Pipeline thực hiện 3 bước lớn:

**Bước 1 - Cài đặt công cụ:**
```bash
# Tải và cài Helm vào thư mục tạm của workspace
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-4
USE_SUDO="false" HELM_INSTALL_DIR="${WORKSPACE}/bin" ./get_helm.sh
```

**Bước 2 - Deploy cấu hình chung:**
```bash
# Deploy yas-configuration (ConfigMap/Secret dùng chung cho mọi service)
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
    --namespace ${DEVELOPER_NAMESPACE} \
    --create-namespace
```

**Bước 3 - Deploy từng service với Ingress routing thông minh:**
```groovy
// Domain được tạo tự động theo tên namespace để tránh đụng độ
def apiDomain        = "api-${DEVELOPER_NAMESPACE}.yas.local.com"
def storefrontDomain = "storefront-${DEVELOPER_NAMESPACE}.yas.local.com"
def backofficeDomain = "backoffice-${DEVELOPER_NAMESPACE}.yas.local.com"

// Mỗi service được deploy qua Helm với image tag tương ứng
serviceTags.each { svc, tag ->
    helm upgrade --install ${chartName} k8s/charts/${chartName} \
        --namespace ${DEVELOPER_NAMESPACE} \
        --set backend.image.repository=${DOCKER_HUB_USER}/${svc} \
        --set backend.image.tag=${tag} \           // Tag từ Stage 1
        --set backend.ingress.host=${domain} \     // Domain riêng
        --wait --timeout 120s
}
```

**Điểm đặc biệt về ánh xạ Chart Name:**
```groovy
// Tên chart không trùng với tên service trong một số trường hợp:
if (svc == 'backoffice')  → chartName = 'backoffice-ui'   // Next.js frontend
if (svc == 'storefront')  → chartName = 'storefront-ui'   // Next.js frontend
// Các service khác: chartName = serviceName
```

#### Stage 3: Print Access Info

Pipeline tự động detect IP của Worker Node để in ra hướng dẫn:
```groovy
// Ưu tiên lấy ExternalIP từ K8s API
IP = kubectl get nodes -l '!node-role.kubernetes.io/control-plane' \
    -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}'

// Fallback: Lấy public IP qua ifconfig.me
if (IP == '') {
    IP = curl -s ifconfig.me
}
```

### 1.4. Cleanup Pipeline (Dọn dẹp môi trường)

Job `cleanup_developer_build` (`Jenkinsfile.cleanup_developer_build`) thực hiện:

1. **Safety Check:** Kiểm tra Developer đã tích ô xác nhận `CONFIRM_DESTROY`. Chặn tuyệt đối việc xóa các namespace hệ thống (`default`, `kube-system`, `jenkins`,...).
2. **Uninstall Helm Releases:** Liệt kê tất cả Helm release trong namespace và gỡ từng cái.
3. **Delete Namespace:** Xóa hoàn toàn namespace Kubernetes, giải phóng tài nguyên.

```groovy
// Danh sách namespace bảo vệ - không bao giờ được xóa
def protectedNamespaces = ['default', 'kube-system', 'kube-public', 'jenkins', 'stakater']
if (protectedNamespaces.contains(params.DEVELOPER_NAMESPACE)) {
    error("BẢO MẬT: Tuyệt đối không được phép xóa namespace hệ thống!")
}
```

---

## 2. ArgoCD + GitOps: Triển khai tự động (Dev / Staging / Production)

### 2.1. Nguyên lý GitOps

GitOps là phương pháp quản lý hạ tầng và deployment bằng cách lưu toàn bộ trạng thái mong muốn (Desired State) của hệ thống vào một Git Repository. Repository này được gọi là **Source of Truth** (Nguồn chân lý duy nhất).

**Quy tắc vàng của GitOps:**
- Trạng thái của hệ thống trên Cluster **PHẢI** khớp 100% với nội dung trong Git Repository.
- Mọi thay đổi lên hệ thống **PHẢI** thông qua Git commit (không ai được phép `kubectl apply` trực tiếp).
- Nếu có ai đó sửa trực tiếp trên Cluster (drift), ArgoCD sẽ phát hiện và tự động đưa về đúng trạng thái trong Git.

### 2.2. Cấu trúc Repository GitOps

Repository `yas-gitops` (tách riêng khỏi repo source code) có cấu trúc:

```
yas-gitops/
├── environments/
│   ├── dev/
│   │   ├── product/values.yaml        # backend.image.tag: "a1b2c3d..."
│   │   ├── cart/values.yaml
│   │   └── ...
│   ├── staging/
│   │   ├── product/values.yaml        # backend.image.tag: "v1.2.3"
│   │   └── ...
│   └── production/
│       ├── product/values.yaml        # backend.image.tag: "a1b2c3d..."
│       └── ...
```

**Câu hỏi vấn đáp:** *"Tại sao tách repo GitOps riêng?"*
→ 3 lý do:
1. **Separation of Concerns:** Source code và deployment config có lifecycle khác nhau.
2. **Bảo mật:** Team QA/Ops có thể có quyền truy cập repo GitOps mà không cần quyền truy cập source code.
3. **Tránh vòng lặp vô hạn:** Nếu GitOps nằm chung repo source, mỗi lần CI update `values.yaml` sẽ trigger lại CI → CI lại update → trigger lại... (infinite loop).

### 2.3. Luồng hoạt động End-to-End

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐     ┌──────────────┐
│  Developer  │     │   GitHub     │     │   yas-gitops   │     │   ArgoCD     │
│  push code  │────▶│   Actions    │────▶│   Repository   │────▶│   on K8s     │
└─────────────┘     │  (CI + Push  │     │  (values.yaml  │     │  (Sync &     │
                    │   Image)     │     │   updated)     │     │   Deploy)    │
                    └──────────────┘     └───────────────┘     └──────────────┘
```

**Chi tiết từng bước:**

1. Developer push code thay đổi `product/` lên nhánh `main`.
2. GitHub Actions `product-ci.yml` được kích hoạt:
   - Build + Test + Push image `ndbau/product:a1b2c3d` lên Docker Hub.
3. Job `update-dev` gọi `gitops-updater.yml`:
   - Clone repo `yas-gitops`.
   - Dùng `yq` sửa file `environments/dev/product/values.yaml`:
     ```yaml
     backend:
       image:
         tag: "a1b2c3d"   # ← Cập nhật tag mới
     ```
   - Commit + Push lên `yas-gitops`.
4. ArgoCD (đang chạy trong Cluster) phát hiện repo `yas-gitops` có commit mới.
5. ArgoCD so sánh Desired State (Git) vs Live State (Cluster):
   - Thấy rằng image tag của `product` trên Cluster vẫn là tag cũ.
   - Kích hoạt **Sync**: Pull image mới từ Docker Hub và thực hiện Rolling Update.
6. Pod `product` cũ bị terminate dần, Pod mới với image `a1b2c3d` được tạo ra.

### 2.4. Chiến lược phân luồng môi trường

| Sự kiện | Môi trường | Image Tag | Ý nghĩa |
|---------|------------|-----------|----------|
| Push vào `main` | Dev | `commit-sha` | Tích hợp sớm, test nhanh code mới nhất |
| Push vào `main` | Production | `commit-sha` | Continuous Deployment trực tiếp |
| Push tag `v1.2.3` | Staging | `v1.2.3` | Phiên bản release, test trước khi lên production |

**Câu hỏi vấn đáp:** *"Tại sao Production cũng deploy từ main mà không qua Staging?"*
→ Đây là thiết kế Continuous Deployment (CD) thực sự: Mỗi commit vào main đều được deploy ngay lập tức. Nếu muốn triển khai theo mô hình thận trọng hơn (CD → Continuous Delivery), có thể cấu hình ArgoCD ở chế độ **Manual Sync** cho Production, yêu cầu người có thẩm quyền nhấn nút Approve trước khi deploy.

### 2.5. Cơ chế xử lý xung đột (Race Condition)

Khi nhiều CI workflow cùng cố push vào `yas-gitops` đồng thời:

```bash
MAX_RETRIES=5
for i in $(seq 1 $MAX_RETRIES); do
  if git pull --rebase origin main && git push origin main; then
    echo "Push successful!"
    break
  else
    echo "Push failed. Retrying in 5 seconds... ($i/$MAX_RETRIES)"
    sleep 5
  fi
done
```

**Giải thích:**
- `git pull --rebase`: Kéo commit mới nhất từ remote và đặt commit của mình lên trên (rebase), tránh tạo merge commit không cần thiết.
- Retry 5 lần với 5 giây giữa mỗi lần: Đủ để xử lý hầu hết các trường hợp đụng độ.

### 2.6. ArgoCD trên Cluster

ArgoCD được cài đặt trong namespace `argocd` trên Cluster. Các thành phần chính:

| Component | Vai trò |
|-----------|---------|
| `argocd-server` | Dashboard Web UI + API |
| `argocd-repo-server` | Clone và render Helm Charts từ Git |
| `argocd-application-controller` | So sánh Desired vs Live State, kích hoạt Sync |
| `argocd-applicationset-controller` | Quản lý nhiều Application theo template |
| `argocd-redis` | Cache nội bộ |

**Cách ArgoCD biết phải theo dõi repo nào?**
Mỗi Application trong ArgoCD được cấu hình với:
- `source.repoURL`: URL của repo GitOps.
- `source.path`: Đường dẫn đến thư mục chứa Helm chart/values.
- `destination.namespace`: Namespace trên Cluster để deploy vào.
- `syncPolicy.automated`: Tự động Sync khi có thay đổi trên Git.
