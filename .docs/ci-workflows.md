# Tài Liệu Continuous Integration (CI) - Hệ thống YAS

## Tổng quan

Hệ thống YAS sử dụng **GitHub Actions** làm nền tảng CI. Toàn bộ pipeline được định nghĩa trong thư mục `.github/workflows/` với **22 file workflow** riêng biệt, phục vụ 3 nhóm ứng dụng khác nhau: Backend Java (13 services), Frontend Next.js (2 apps), và BFF Gateway (2 services).

---

## 1. Kiến trúc CI phân tán (Per-Service Pipeline)

### 1.1. Tại sao không dùng một pipeline duy nhất?

Trong kiến trúc Microservices, mỗi service là một đơn vị triển khai độc lập. Nếu dùng chung một pipeline, mỗi lần commit thay đổi 1 service sẽ phải build lại toàn bộ 17 services còn lại, gây lãng phí thời gian và tài nguyên GitHub Actions Runner.

Do đó, mỗi Microservice có file workflow riêng (ví dụ: `product-ci.yml`, `cart-ci.yml`, `order-ci.yml`,...). Mỗi file sử dụng cơ chế **Path Filtering** để chỉ kích hoạt khi đúng thư mục của service đó thay đổi.

### 1.2. Cơ chế Path Filtering hoạt động như thế nào?

Lấy ví dụ file `product-ci.yml`:

```yaml
on:
  push:
    branches: [ "**" ]       # Kích hoạt trên MỌI nhánh
    tags:
      - "v*"                 # Kích hoạt khi gắn tag release (v1.0.0, v2.3.1,...)
    paths:                   # CHỈ chạy khi các file sau thay đổi:
      - "product/**"         #   → Code của chính service product
      - "common-library/**"  #   → Thư viện dùng chung (vì product phụ thuộc vào nó)
      - ".github/workflows/product-ci.yml"  # → Chính file pipeline này
      - "pom.xml"            #   → File Maven gốc (ảnh hưởng đến dependency resolution)
```

**Ý nghĩa thực tế:** Khi Developer A sửa code trong thư mục `cart/`, GitHub Actions sẽ chỉ chạy `cart-ci.yml`. Các workflow của `product`, `order`,... hoàn toàn im lặng. Điều này giúp tiết kiệm hàng trăm phút runner mỗi tháng.

### 1.3. Sự kiện kích hoạt (Triggers)

| Sự kiện | Điều kiện | Hành động |
|---------|-----------|-----------|
| `push` trên mọi branch | Paths khớp | Build + Test + Push Docker Image (tag = commit SHA) |
| `push` trên branch `main` | Paths khớp | Build + Test + Push Image (tag = `latest`) + Trigger CD |
| `push` tag `v*` | Tag khớp pattern | Build + Test + Push Image (tag = `vX.Y.Z`) + Deploy Staging |
| `pull_request` vào `main` | Paths khớp | Build + Test (KHÔNG push image) |
| `workflow_dispatch` | Thủ công | Cho phép chạy lại pipeline bất kỳ lúc nào |

**Câu hỏi vấn đáp thường gặp:** *"Tại sao pull_request không push image?"*
→ Vì Pull Request chỉ là bước review, code chưa được merge vào main. Việc push image lúc này sẽ tạo ra các image "rác" không bao giờ được deploy.

---

## 2. Quy trình CI cho Backend Java (Chi tiết từng Step)

Dưới đây là phân tích từng bước trong pipeline của một Backend Service (lấy `product-ci.yml` làm ví dụ đại diện):

### Step 1: Checkout Code
```yaml
- name: Checkout Code
  uses: actions/checkout@v4
  with:
    fetch-depth: 0   # Clone TOÀN BỘ lịch sử (không shallow clone)
```
**Tại sao `fetch-depth: 0`?** Vì Gitleaks cần quét toàn bộ lịch sử commit để tìm secret bị lộ. Nếu để mặc định (`fetch-depth: 1`), Gitleaks chỉ quét commit mới nhất và có thể bỏ sót secret đã bị push ở commit cũ.

### Step 2: Gitleaks Scan (Quét rò rỉ bí mật)
```yaml
- name: Gitleaks Scan
  uses: gitleaks/gitleaks-action@v2
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
**Giải thích:** Gitleaks là công cụ quét mã nguồn tự động để phát hiện các chuỗi ký tự nhạy cảm (API keys, passwords, tokens, private keys,...) đã vô tình bị commit vào repo. Nếu phát hiện, pipeline sẽ **dừng ngay lập tức** (fail) để ngăn chặn rò rỉ.

**Ví dụ thực tế:** Nếu Developer viết `spring.datasource.password=admin123` trực tiếp trong `application.yaml` thay vì dùng Kubernetes Secret, Gitleaks sẽ bắt và báo lỗi.

### Step 3: Set up JDK 25
```yaml
- name: Set up JDK 25
  uses: actions/setup-java@v4
  with:
    java-version: '25'
    distribution: 'temurin'
    cache: 'maven'          # Cache thư mục ~/.m2 giữa các lần chạy
```
**Tại sao dùng `cache: 'maven'`?** Lần chạy đầu tiên, Maven phải tải hàng trăm MB dependency từ Maven Central. Với cache, các lần chạy sau chỉ tải thêm dependency mới, giảm thời gian build từ 5-7 phút xuống còn 1-2 phút.

### Step 4: Build và Test bằng Maven
```yaml
- name: Build and Test with Maven
  run: |
    mvn clean install -pl product -am -Drevision=1.0-SNAPSHOT -B
```
**Giải thích các flag:**
- `-pl product`: Chỉ build module `product` (không build toàn bộ mono-repo).
- `-am` (also-make): Tự động build các module mà `product` phụ thuộc (ví dụ: `common-library`).
- `-Drevision=1.0-SNAPSHOT`: Truyền biến version vào POM.
- `-B` (batch mode): Tắt interactive mode, phù hợp cho CI (không hỏi input từ người dùng).

**Quan trọng:** Bước `install` của Maven sẽ tự động chạy tất cả Unit Test (Surefire) và Integration Test (Failsafe) được định nghĩa trong module. Nếu bất kỳ test nào fail, pipeline sẽ dừng tại đây.

### Step 5: Snyk Vulnerability Scan (Quét lỗ hổng dependency)
```yaml
- name: Run Snyk to check for vulnerabilities
  continue-on-error: true     # Không dừng pipeline nếu phát hiện lỗ hổng
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
  run: |
    snyk test --file=product/pom.xml -- -Drevision=1.0-SNAPSHOT
```
**Giải thích:** Snyk quét file `pom.xml` để kiểm tra xem các thư viện Java đang dùng (Spring Boot, Jackson, Hibernate,...) có chứa lỗ hổng bảo mật (CVE) đã được công bố hay không.

**Tại sao `continue-on-error: true`?** Vì một số CVE có mức độ thấp (Low/Medium) hoặc chưa có bản vá. Nếu dừng pipeline vì các lỗ hổng này sẽ gây gián đoạn phát triển không cần thiết. Kết quả Snyk được ghi nhận để review sau.

### Step 6: SonarQube Scan (Phân tích chất lượng code)
```yaml
- name: SonarQube Scan
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
  run: |
    mvn sonar:sonar \
      -pl product -am \
      -Dsonar.projectKey=ffr-key_yas-product \
      -Dsonar.organization=ffr-key \
      -Dsonar.coverage.jacoco.xmlReportPaths=product/target/site/jacoco/jacoco.xml
```
**Giải thích:** SonarQube (SonarCloud) phân tích mã nguồn Java để đánh giá:
- **Bugs:** Lỗi logic tiềm ẩn (null pointer, resource leak,...).
- **Code Smells:** Code xấu, khó bảo trì (hàm quá dài, duplicate code,...).
- **Vulnerabilities:** Lỗ hổng bảo mật trong code (SQL Injection, XSS,...).
- **Code Coverage:** Tỷ lệ code được bao phủ bởi Unit Test (lấy từ báo cáo JaCoCo).

**Tại sao mỗi service có `projectKey` riêng?** Vì trên SonarCloud, mỗi project key tương ứng với một dashboard riêng biệt. Điều này cho phép từng team phụ trách từng service có thể theo dõi độc lập chất lượng code của mình.

### Step 7: Test Results Reporter
```yaml
- name: Test Results
  uses: dorny/test-reporter@v1
  if: success() || failure()   # Chạy cả khi test fail
  with:
    name: Product-Service-Unit-Test-Results
    path: "product/**/surefire-reports/*.xml"
    reporter: java-junit
```
**Giải thích:** Bước này đọc file XML từ Surefire (JUnit format) và render thành một giao diện trực quan trên tab **Checks** của GitHub. Developer có thể nhấn vào để xem chi tiết test nào pass, test nào fail, và stack trace của lỗi.

**Tại sao `if: success() || failure()`?** Vì khi test fail, pipeline mặc định sẽ bỏ qua các bước sau. Nhưng chúng ta vẫn muốn render báo cáo test để Developer biết chính xác test nào bị lỗi.

### Step 8: JaCoCo Coverage Report trên Pull Request
```yaml
- name: Add coverage report to PR
  uses: madrapps/jacoco-report@v1.6.1
  with:
    paths: ${{ github.workspace }}/product/target/site/jacoco/jacoco.xml
    token: ${{ secrets.GITHUB_TOKEN }}
    min-coverage-overall: 70         # Yêu cầu tối thiểu 70% coverage tổng thể
    min-coverage-changed-files: 70   # Yêu cầu tối thiểu 70% coverage cho file thay đổi
    title: 'Product Coverage Report'
    update-comment: true
```
**Giải thích:** Bước này tự động comment một bảng thống kê Coverage vào Pull Request. Nếu tỷ lệ coverage dưới 70%, nó sẽ đánh dấu cảnh báo (nhưng không fail pipeline). Reviewer có thể dựa vào đây để yêu cầu Developer bổ sung test.

### Step 9-10: Docker Build & Push
```yaml
- name: Build and Push Docker Image
  if: github.event_name == 'push'   # Chỉ chạy khi push (không chạy cho PR)
  uses: docker/build-push-action@v6
  with:
    context: ./product
    push: true
    tags: |
      ${{ secrets.DOCKERHUB_USERNAME }}/product:${{ github.sha }}
      ${{ github.ref == 'refs/heads/main' && format('{0}/product:latest', secrets.DOCKERHUB_USERNAME) || '' }}
      ${{ startsWith(github.ref, 'refs/tags/v') && format('{0}/product:{1}', secrets.DOCKERHUB_USERNAME, github.ref_name) || '' }}
```
**Chiến lược gắn tag Docker Image (rất quan trọng cho vấn đáp):**

| Điều kiện | Tag được gắn | Mục đích |
|-----------|--------------|----------|
| Mọi push | `<commit-sha>` (ví dụ: `a1b2c3d4`) | Truy xuất chính xác (Traceability) - biết image này được build từ commit nào |
| Push vào `main` | `latest` | Dùng cho môi trường Dev, luôn lấy phiên bản mới nhất |
| Push tag `v1.2.3` | `v1.2.3` | Dùng cho Staging/Production, đánh dấu phiên bản release ổn định |

**Tại sao dùng Docker Buildx?** Buildx hỗ trợ build cache nâng cao và multi-platform image. Mặc dù hiện tại chỉ build cho `linux/amd64`, việc dùng Buildx giúp tận dụng layer caching tốt hơn, giảm thời gian build Docker image.

---

## 3. Quy trình CI cho Frontend Next.js

Các Frontend (`backoffice-ci.yml`, `storefront-ci.yml`) có pipeline khác biệt đáng kể so với Backend:

| Khác biệt | Backend (Java) | Frontend (Next.js) |
|------------|----------------|---------------------|
| Ngôn ngữ | JDK 25 + Maven | Node.js 20 + npm |
| Build | `mvn clean install` | `npm ci` + `npm run build` |
| Test | Surefire + JaCoCo | Lint + Prettier + npm audit |
| Scan bảo mật | Snyk (pom.xml) | Snyk (package.json) + npm audit |
| SonarQube | Maven plugin | SonarCloud GitHub Action |
| Dependency path | `common-library/**` | Không có (self-contained) |

**Các bước đặc thù của Frontend:**
- `npm ci`: Cài dependency từ `package-lock.json` (deterministic, nhanh hơn `npm install`).
- `npm run lint`: Kiểm tra coding style bằng ESLint.
- `npx prettier --check .`: Kiểm tra format code (spacing, indentation,...).
- `npm audit --omit=dev`: Quét lỗ hổng trong production dependencies.

---

## 4. Tích hợp CI → CD (GitOps Trigger)

Sau khi Docker image được push thành công, pipeline CI sẽ tự động kích hoạt bước chuyển giao cho CD thông qua **Reusable Workflow** `gitops-updater.yml`. Đây là cầu nối giữa CI và CD.

### 4.1. Cơ chế Reusable Workflow

Thay vì copy-paste logic update GitOps vào 22 file workflow, hệ thống sử dụng `workflow_call` để tái sử dụng:

```yaml
# Trong product-ci.yml:
update-dev:
  needs: build-and-test                              # Chờ CI pass
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
  uses: ./.github/workflows/gitops-updater.yml       # Gọi reusable workflow
  with:
    service_name: 'product'
    environment: 'dev'
    image_tag: ${{ github.sha }}                      # Truyền commit SHA làm tag
  secrets:
    GITOPS_TOKEN: ${{ secrets.GITOPS_TOKEN }}
```

### 4.2. Luồng phân phối theo nhánh/tag

```
Push lên feature branch  →  CI (build + test + push image:commit-sha)  →  DỪNG (không trigger CD)
Push lên main            →  CI (build + test + push image:latest)      →  CD (update dev + production)
Push tag v1.2.3          →  CI (build + test + push image:v1.2.3)      →  CD (update staging)
```

### 4.3. gitops-updater.yml hoạt động như thế nào?

```yaml
steps:
  - name: Checkout GitOps repo
    uses: actions/checkout@v4
    with:
      repository: baus-9105/yas-gitops    # Clone repo GitOps riêng biệt
      token: ${{ secrets.GITOPS_TOKEN }}  # Dùng Personal Access Token

  - name: Update file values.yaml and Push
    run: |
      # Dùng yq để sửa image tag trong file values.yaml
      yq e ".backend.image.tag = \"${TAG}\"" -i $TARGET_FILE
      
      # Commit và Push với cơ chế retry (tối đa 5 lần)
      # để tránh xung đột khi nhiều CI chạy song song
      for i in $(seq 1 $MAX_RETRIES); do
        if git pull --rebase origin main && git push origin main; then
          break
        fi
        sleep 5
      done
```

**Câu hỏi vấn đáp:** *"Tại sao cần retry khi push?"*
→ Vì khi nhiều service CI chạy đồng thời (ví dụ: push commit thay đổi cả `product` lẫn `cart`), cả 2 workflow `gitops-updater` sẽ cùng cố push vào repo `yas-gitops`. Nếu workflow A push trước, workflow B sẽ bị rejected vì remote đã thay đổi. Cơ chế `git pull --rebase` + retry giải quyết race condition này.

---

## 5. Danh sách các GitHub Secrets cần cấu hình

| Secret | Mục đích | Nơi tạo |
|--------|----------|---------|
| `DOCKERHUB_USERNAME` | Username đăng nhập Docker Hub | Docker Hub Account |
| `DOCKERHUB_TOKEN` | Access Token cho Docker Hub | Docker Hub → Security → Access Tokens |
| `SONAR_TOKEN` | Token xác thực SonarCloud | sonarcloud.io → Security |
| `SONAR_HOST_URL` | URL của SonarCloud | `https://sonarcloud.io` |
| `SNYK_TOKEN` | Token xác thực Snyk | app.snyk.io → Settings → API Token |
| `GITOPS_TOKEN` | Personal Access Token có quyền push vào `yas-gitops` | GitHub → Settings → Developer Settings → PAT |
