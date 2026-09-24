# RikkeiBank - Hệ Thống Microservice Ngân Hàng Số

Hệ thống Backend phân tán phục vụ ngân hàng số **RikkeiBank**, được chuyển đổi từ mô hình Monolithic sang kiến trúc **Microservice Architecture (MSA)**. Hệ thống cung cấp nền tảng RESTful API an toàn, chịu lỗi cao, hiệu năng vượt trội cho ứng dụng Web và Mobile Banking.

Dự án được xây dựng hoàn toàn trên **Java Spring Boot 3**, **Spring Cloud**, **Gradle Multi-project**, **Lombok**, **Apache Kafka**, **Redis Caching & Blacklist**, **Resilience4j Circuit Breaker**, và **Saga Pattern**.

---

## I. Tổng Quan Kiến Trúc & Lý Do Lựa Chọn Microservices (MSA)

| Tiêu chí | Hệ thống Monolithic cũ | Hệ thống Microservices RikkeiBank mới |
| :--- | :--- | :--- |
| **Tính độc lập** | Tất cả module nằm chung 1 khối; lỗi ở 1 chức năng có thể kéo sập toàn hệ thống | Từng service hoạt động độc lập; sự cố ở notification hay catalog không ảnh hưởng tới chuyển khoản |
| **Cơ sở dữ liệu** | 1 Database tập trung duy nhất, dễ tắc nghẽn I/O khi lượng giao dịch tăng đột biến | **Database-per-service**: Mỗi service sở hữu cơ sở dữ liệu riêng, cô lập hoàn toàn |
| **Khả năng mở rộng (Scalability)** | Phải nhân bản toàn bộ ứng dụng cồng kềnh | Tự do scale các service chịu tải cao (như Account, Transaction) thông qua Eureka & API Gateway |
| **Phát triển & Triển khai (CI/CD)**| Triển khai chậm chạp, rủi ro cao khi thay đổi code | Các đội ngũ có thể phát triển, kiểm thử và release độc lập từng service |

---

## II. Danh Sách Microservice & Port Ánh Xạ

| Service | Port | Công Nghệ & Vai Trò |
| :--- | :--- | :--- |
| **`discovery-server`** | `8761` | Netflix Eureka Server - Đăng ký và khám phá dịch vụ tự động |
| **`config-server`** | `8888` | Spring Cloud Config Server - Quản lý cấu hình tập trung |
| **`api-gateway`** | `8080` | Spring Cloud Gateway (WebFlux) - Định tuyến, JWT Verification & Redis Blacklist Filter |
| **`identity-service`** | `8081` | Spring Security + JWT + Redis: Đăng ký, Đăng nhập, Refresh Token, ADMIN Cưỡng chế Logout |
| **`customer-service`** | `8082` | Quản lý Customer, Staff/Teller, Danh mục Account Type, Redis Cache-Aside (`@Cacheable`, `@CacheEvict`) |
| **`account-service`** | `8083` | Mở tài khoản, Quản lý số dư, Saga primitives: `debit`, `credit`, `compensate-debit` |
| **`transaction-service`**| `8084` | **Saga Orchestrator**, Kháng lỗi Circuit Breaker, Báo cáo Teller trong ngày, Kafka Producer |
| **`notification-service`**| `8085`| Spring WebFlux + Apache Kafka Consumer: Nhận sự kiện biến động số dư, Stream SSE |
| **Redis** | `6379` | Distributed Cache-Aside và Token Blacklist / Session Revocation |
| **Apache Kafka (KRaft)** | `9092` | Event-driven Broker cho các luồng sự kiện phân tán |
| **Kafka UI** | `8089` | Giao diện trực quan theo dõi Topics, Messages và Consumer Groups |

---

## III. Các Luồng Nghiệp Vụ Trọng Tâm (Business Flows)

### 1. Phân quyền & Trải nghiệm Đăng nhập (Security & Session)
- **ADMIN**: Toàn quyền quản trị danh mục, mở/khóa tài khoản, và đặc biệt có API **cưỡng chế logout (`/api/v1/auth/admin/revoke-user/{userId}`)** đưa vào Redis Blacklist, lập tức tước quyền truy cập ngay tức khắc.
- **CUSTOMER**: Cấp cặp `accessToken` (15 phút) và `refreshToken` (7 ngày) giúp duy trì phiên đăng nhập lâu dài trên ứng dụng mà không bắt đăng nhập liên tục.
- **TELLER (Giao dịch viên)**: Xem danh sách giao dịch trong ngày của chính mình (`/api/v1/transactions/teller/daily`), không được can thiệp vào giao dịch của giao dịch viên khác.
- **Khách vãng lai**: Mọi request không có token hợp lệ đều bị API Gateway chặn lại với mã **401 Unauthorized**.

### 2. Giao dịch Chuyển khoản Phân tán (Saga Orchestration & Rollback)
Do mỗi service sở hữu Database riêng biệt, nghiệp vụ chuyển khoản 2 tài khoản không thể dùng ACID truyền thống mà sử dụng **Saga Orchestrator**:

```
[Chuyển khoản Thành công]
Client --> API Gateway --> Transaction Service (Orchestrator)
                             │
                             ├─ Step 1: Gọi Account Service (Debit A: -2.000.000) -> OK
                             ├─ Step 2: Gọi Account Service (Credit B: +2.000.000) -> OK
                             ├─ Step 3: Transaction status = SUCCESS
                             └─ Step 4: Bắn TransferCompletedEvent lên Kafka -> Notification Service
```

```
[Kịch bản Rollback (Compensating Transaction)]
Client --> Chuyển khoản tới tài khoản B (bị KHÓA hoặc LỖI)
                             │
                             ├─ Step 1: Gọi Account Service (Debit A: -1.500.000) -> OK
                             ├─ Step 2: Gọi Account Service (Credit B) -> THẤT BẠI (Account locked)
                             ├─ Step 3: Kích hoạt COMPENSATING TRANSACTION:
                             │          Gọi Account Service (Compensate-Debit: Hoàn lại +1.500.000 cho A)
                             ├─ Step 4: Transaction status = FAILED (Đã bồi hoàn)
                             └─ Step 5: Bắn TransferFailedEvent lên Kafka -> Số dư của A được bảo toàn tuyệt đối!
```

### 3. Distributed Caching (Cache-Aside với Redis)
- Thông tin hồ sơ khách hàng (`Customer`) và danh mục loại tài khoản (`AccountType`) được lưu trên Redis với `@Cacheable`.
- Khi cập nhật thông tin, `@CachePut` hoặc `@CacheEvict` tự động xóa cache tương ứng để đảm bảo tính nhất quán dữ liệu.

### 4. Kháng lỗi Dây chuyền (Fault Tolerance với Resilience4j)
- Feign Client kết nối giữa các service được bảo vệ bởi **Circuit Breaker** (3 trạng thái: `CLOSED`, `OPEN`, `HALF_OPEN`).
- Khi service phụ thuộc gặp sự cố liên tục, Circuit Breaker lập tức ngắt mạch (chuyển sang `OPEN`) và trả về fallback xử lý, ngăn chặn sụp đổ dây chuyền (Cascading Failure).

---

## IV. Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Yêu cầu môi trường
- **Java**: JDK 17 hoặc 21
- **Docker & Docker Compose**: Để chạy Redis & Apache Kafka
- **Gradle**: Đã tích hợp sẵn Gradle Wrapper (`gradlew.bat` trên Windows / `./gradlew` trên Linux/macOS)

### 2. Bước 1: Khởi động Hạ tầng Docker (Redis & Kafka)
Tại thư mục gốc của dự án:
```bash
docker compose up -d
```
Kiểm tra container đang chạy:
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8089`

### 3. Bước 2: Build toàn bộ dự án
```bash
# Trên Windows
gradlew.bat build -x test

# Trên Linux / macOS
./gradlew build -x test
```

### 4. Bước 3: Khởi chạy các Microservices theo thứ tự
Nên khởi chạy lần lượt theo thứ tự sau (trong các cửa sổ terminal riêng biệt hoặc qua IDE):

1. **Discovery Server**:
   ```bash
   ./gradlew :discovery-server:bootRun
   ```
   *Dashboard Eureka truy cập tại: `http://localhost:8761`*

2. **Config Server** (tùy chọn / có thể chạy độc lập):
   ```bash
   ./gradlew :config-server:bootRun
   ```

3. **API Gateway**:
   ```bash
   ./gradlew :api-gateway:bootRun
   ```

4. **Identity Service**:
   ```bash
   ./gradlew :identity-service:bootRun
   ```

5. **Customer Service**:
   ```bash
   ./gradlew :customer-service:bootRun
   ```

6. **Account Service**:
   ```bash
   ./gradlew :account-service:bootRun
   ```

7. **Transaction Service**:
   ```bash
   ./gradlew :transaction-service:bootRun
   ```

8. **Notification Service**:
   ```bash
   ./gradlew :notification-service:bootRun
   ```

---

## V. Tài Khoản Mặc Định & Dữ Liệu Khởi Tạo (Seed Data)

Hệ thống tự động nạp sẵn dữ liệu mẫu khi khởi động:

| Role | Username | Password | Thông tin bổ sung |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Quản trị viên toàn hệ thống |
| **TELLER** | `teller1` | `teller123` | Giao dịch viên chi nhánh Hà Nội (Staff ID 1) |
| **CUSTOMER** | `customer1` | `customer123` | Khách hàng sở hữu tài khoản `1000000001` (50.000.000 VND) |

Tài khoản ngân hàng có sẵn:
- `1000000001`: Tài khoản nguồn (50.000.000 VND, ACTIVE)
- `1000000002`: Tài khoản đích (10.000.000 VND, ACTIVE)
- `1000000003`: Tài khoản đích bị KHÓA (LOCKED) - Phục vụ kiểm thử luồng Saga Rollback

---

## VI. Hướng Dẫn Kiểm Thử Bằng Postman Collection

File Postman Collection đầy đủ đã được đính kèm tại:
**`RikkeiBank_Postman_Collection.json`**

### Cách sử dụng:
1. Mở Postman -> Chọn **Import** -> Chọn file `RikkeiBank_Postman_Collection.json`.
2. Kiểm tra Collection Variable `baseUrl` đang là `http://localhost:8080`.
3. Chạy theo thứ tự các thư mục:
   - **`01. Authentication & Security`**: Chạy `1.1 Login as Admin`, `1.2 Login as Teller`, `1.3 Login as Customer`. Token sẽ tự động được lưu vào biến môi trường để sử dụng cho các request sau.
   - **`02. Catalog & Profile Management`**: Xem danh mục loại tài khoản (Redis Cache), tạo mới và quản lý khách hàng.
   - **`03. Bank Account Management`**: Xem số dư tài khoản `1000000001` và `1000000002`.
   - **`04. Core Transfer & Saga Transactions`**:
     - Chạy `4.1 SAGA FLOW 1: Transfer Success`: Chuyển 2.000.000 VND từ `1000000001` sang `1000000002`. Kiểm tra lại số dư: `1000000001` còn 48.000.000 VND, `1000000002` tăng lên 12.000.000 VND.
     - Chạy `4.2 SAGA FLOW 2: Transfer to LOCKED Account`: Thử chuyển 1.500.000 VND sang tài khoản bị khóa `1000000003`. Bước 1 trừ tiền, Bước 2 lỗi -> Hệ thống tự động kích hoạt **Compensating Refund** -> Số dư tài khoản nguồn `1000000001` được hoàn trả đầy đủ, trạng thái giao dịch ghi nhận `FAILED` kèm lý do chi tiết!
     - Chạy `4.6 TELLER View Daily Transactions`: Đăng nhập bằng Teller để xem các giao dịch do teller thực hiện trong ngày.
   - **`05. Event-Driven Notifications`**: Xem danh sách thông báo biến động số dư do Kafka Consumer xử lý và stream SSE.
