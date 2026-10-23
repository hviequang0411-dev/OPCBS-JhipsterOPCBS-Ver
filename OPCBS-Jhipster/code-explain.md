# Giải Thích Chi Tiết Luồng Dữ Liệu & Mã Nguồn Domain Đăng Nhập (Login & Authentication)

> **Tài liệu**: Hướng dẫn phân tích kỹ thuật chuyên sâu luồng dữ liệu End-to-End từ Frontend (Angular 19) $\rightarrow$ API Gateway $\rightarrow$ Backend (Spring Boot / Spring Security) $\rightarrow$ Cơ sở dữ liệu (MySQL).  
> **Phạm vi tập trung**: Phân hệ Xác thực & Định danh (**Login & Identity Domain**).

---

## 📑 MỤC LỤC BÀI VIẾT

1. [Sơ Đồ Luồng Đi Dữ Liệu Toàn Cục (End-to-End Flow Diagram)](#1-sơ-đồ-luồng-đi-dữ-liệu-toàn-cục-end-to-end-flow-diagram)
2. [Chi Tiết Phía Frontend (Angular 19 SPA)](#2-chi-tiết-phía-frontend-angular-19-spa)
   - [2.1. Giao diện & Nút bấm (`login.html`)](#21-giao-diện--nút-bấm-loginhtml)
   - [2.2. Logic Component & Quản lý Trạng thái (`login.ts`)](#22-logic-component--quản-lý-trạng-thái-logints)
   - [2.3. Điều phối nghiệp vụ đăng nhập (`login.service.ts`)](#23-điều-phối-nghiệp-vụ-đăng-nhập-loginservicets)
   - [2.4. Lưu trữ & Quản lý Token JWT (`auth-jwt.service.ts`)](#24-lưu-trữ--quản-lý-token-jwt-auth-jwtservicets)
   - [2.5. Tra cứu hồ sơ & Quyền hạn (`account.service.ts`)](#25-tra-cứu-hồ-sơ--quyền-hạn-accountservicets)
3. [Cơ Chế Phân Luồng Gateway (`proxy.config.mjs`)](#3-cơ-chế-phân-luồng-gateway-proxyconfigmjs)
4. [Chi Tiết Phía Backend (Spring Boot 3.4 / Security 6 - `opcbs-auth-service`)](#4-chi-tiết-phía-backend-spring-boot-34--security-6---opcbs-auth-service)
   - [4.1. Controller Tiếp Nhận Request (`AuthenticateController.java`)](#41-controller-tiếp-nhận-request-authenticatecontrollerjava)
   - [4.2. Dịch Vụ Tra Cứu Tài Khoản Database (`DomainUserDetailsService.java`)](#42-dịch-vụ-tra-cứu-tài-khoản-database-domainuserdetailsservicejava)
   - [4.3. Quản Trị Người Dùng & Đăng Ký (`UserService.java`)](#43-quản-trị-người-dùng--đăng-ký-userservicejava)
   - [4.4. Dịch Vụ Gửi Email OTP (`MailService.java`)](#44-dịch-vụ-gửi-email-otp-mailservicejava)
5. [Tương Tác Cơ Sở Dữ Liệu MySQL](#5-tương-tác-cơ-sở-dữ-liệu-mysql)
   - [5.1. Cấu trúc Bảng & Khóa ngoại liên quan](#51-cấu-trúc-bảng--khóa-ngoại-liên-quan)
   - [5.2. Các câu lệnh SQL Hibernate thực thi ngầm](#52-các-câu-lệnh-sql-hibernate-thực-thi-ngầm)
6. [Bảng Tra Cứu Nhanh Hàm & Trách Nhiệm (Quick Reference Map)](#6-bảng-tra-cứu-nhanh-hàm--trách-nhiệm-quick-reference-map)
7. [Cơ Chế Giao Tiếp & Xử Lý Code Giữa 5 Microservices Độc Lập](#7-cơ-chế-giao-tiếp--xử-lý-code-giữa-5-microservices-độc-lập)
   - [7.1. Bảng Phân Luồng Endpoint và Dịch Vụ Phụ Trách](#71-bảng-phân-luồng-endpoint-và-dịch-vụ-phụ-trách)
   - [7.2. Cách thức các hàm phía Backend thực thi khi nhận Token](#72-cách-thức-các-hàm-phía-backend-thực-thi-khi-nhận-token)
8. [Giải Thích Code Service Discovery Với Netflix Eureka Server](#8-giải-thích-code-service-discovery-với-netflix-eureka-server)
9. [Hướng Dẫn Chi Tiết Cách Sử Dụng Apache Kafka Trong Microservices](#9-hướng-dẫn-chi-tiết-cách-sử-dụng-apache-kafka-trong-microservices)
   - [9.1. Các Khái Niệm Cốt Lõi Cần Nắm Vững](#91-các-khái-niệm-cốt-lõi-cần-nắm-vững)
   - [9.2. Cấu Hình Kafka Trong Spring Boot (`application.yml`)](#92-cấu-hình-kafka-trong-spring-boot-applicationyml)
   - [9.3. Định Nghĩa Mô Hình Dữ Liệu Sự Kiện (Event Data Models)](#93-định-nghĩa-mô-hình-dữ-liệu-sự-kiện-event-data-models)
   - [9.4. Lập Trình Kafka Producer - Phát Tin Nhắn Bất Đồng Bộ](#94-lập-trình-kafka-producer---phát-tin-nhắn-bất-đồng-bộ)
   - [9.5. Lập Trình Kafka Consumer - Lắng Nghe & Xử Lý Sự Kiện](#95-lập-trình-kafka-consumer---lắng-nghe--xử-lý-sự-kiện)
   - [9.6. Ba Kịch Bản Thực Tế Trong Dự Án Y Tế MindBridge](#96-ba-kịch-bản-thực-tế-trong-dự-án-y-tế-mindbridge)
   - [9.7. Bộ Lệnh CLI Thao Tác & Kiểm Thử Kafka Bằng Docker](#97-bộ-lệnh-cli-thao-tác--kiểm-thử-kafka-bằng-docker)
10. [Giải Thích Mã Nguồn Chi Tiết Các Microservices (Notification, Payment, Message & Treatment)](#10-giải-thích-mã-nguồn-chi-tiết-các-microservices-mới-triển-khai-notification-payment-message--treatment-auto-deduct)
    - [10.1. `opcbs-treatment-service` — Cơ Chế Tự Động Trừ Buổi Điều Trị](#101-opcbs-treatment-service--cơ-chế-tự-động-trừ-buổi-điều-trị)
    - [10.2. `opcbs-notification-service` — Hệ Thống Thông Báo Thời Gian Thực](#102-opcbs-notification-service--hệ-thống-thông-báo-thời-gian-thực)
    - [10.3. `opcbs-payment-service` — Gói Nền Tảng & Tích Hợp VNPay Sandbox](#103-opcbs-payment-service--gói-nền-tảng--tích-hợp-vnpay-sandbox)
    - [10.4. `opcbs-message-service` — Trò Chuyện Trực Tuyến Bác Sĩ - Bệnh Nhân](#104-opcbs-message-service--trò-chuyện-trực-tuyến-bác-sĩ---bệnh-nhân)
11. [Phân Tích Chuyên Sâu Mã Nguồn Deadlock & Luồng Đặt Lịch (Booking Flow)](#11-phân-tích-chuyên-sâu-mã-nguồn-deadlock--luồng-đặt-lịch-booking-transaction-flow)
    - [11.1. Bản Chất Deadlock & Mã Nguồn Mô Phỏng vs Khắc Phục](#111-bản-chất-deadlock--mã-nguồn-mô-phỏng-vs-khắc-phục)
    - [11.2. Phân Tích Mã Nguồn Luồng Đặt Lịch Hẹn (`AppointmentManagementResource.java`)](#112-phân-tích-mã-nguồn-luồng-đặt-lịch-hẹn-appointmentmanagementresourcejava)
12. [Phân Tích Mã Nguồn Transaction & Tính Lũy Đẳng Trong Payment Gateway](#12-phân-tích-mã-nguồn-transaction--tính-lũy-đẳng-idempotency-trong-payment-gateway-vnpay-sandbox)
    - [12.1. Thuật Toán Băm HMAC-SHA512 Xác Thực Chữ Ký (`VnPayUtil.java`)](#121-thuật-toán-băm-hmac-sha512-xác-thực-chữ-ký-vnpayutiljava)
    - [12.2. Kiểm Soát Giao Dịch & Idempotency Guard (`PaymentResource.java`)](#122-kiểm-soát-giao-dịch--idempotency-guard-paymentresourcejava)
13. [Phân Tích Mã Nguồn Doctor Dashboard Service Với Redis Cache & C# Model Mapping](#13-phân-tích-mã-nguồn-doctor-dashboard-service-với-redis-cache--c-model-mapping)
    - [13.1. So Sánh Kiến Trúc Mô Hình C# Razor vs Spring Boot + Redis Cache](#131-so-sánh-kiến-trúc-mô-hình-c-razor-vs-spring-boot--redis-cache)
    - [13.2. Mã Nguồn Redis Cache Container (`DoctorDashboardService.java`)](#132-mã-nguồn-redis-cache-container-doctordashboardservicejava)
    - [13.3. REST Controller Tiếp Nhận API (`DoctorDashboardResource.java`)](#133-rest-controller-tiếp-nhận-api-doctordashboardresourcejava)
    - [13.4. Frontend Component Angular 19 (`doctor-dashboard.component.ts`)](#134-frontend-component-angular-19-doctor-dashboardcomponentts)
14. [Phân Tích Chi Tiết Mã Nguồn Kiến Trúc 2.0: Chuẩn Hóa Database 3NF, Anti-Spam & Microservice Blog](#14-phân-tích-chi-tiết-mã-nguồn-kiến-trúc-20-phase-13-chuẩn-hóa-database-3nf-anti-spam-tầng-db-vòng-đời-bảo-lưu--microservice-blog)
    - [14.1. Chuẩn Hóa 3NF Database & Ngăn Ngừa Bảng Rác](#141-chuẩn-hóa-3nf-database--ngăn-ngừa-bảng-rác)
    - [14.2. Bộ Query JPQL Chống Spam & Giới Hạn Tần Suất Đặt Lịch Tầng DB (`AppointmentRepository.java`)](#142-bộ-query-jpql-chống-spam--giới-hạn-tần-suất-đặt-lịch-tầng-db-appointmentrepositoryjava)
    - [14.3. Nghiệp Vụ Bảo Lưu Gói Trị Liệu & Tự Động Gia Hạn Ngày Hết Hạn (`TreatmentPackageResource.java`)](#143-nghiệp-vụ-bảo-lưu-gói-trị-liệu--tự-động-gia-hạn-ngày-hết-hạn-treatmentpackageresourcejava)
    - [14.4. Tính Toán Tiến Độ Lâm Sàng Tầng Database (`TreatmentPackageResource.java`)](#144-tính-toán-tiến-độ-lâm-sàng-tầng-database-treatmentpackageresourcejava)
    - [14.5. Kiến Trúc Microservice Thứ 9: `opcbs-blog-service` (Port 8089)](#145-kiến-trúc-microservice-thứ-9-opcbs-blog-service-port-8089)
    - [14.6. Giao Diện Người Dùng Angular 19 Đồng Bộ Chuẩn C# Gốc (`OPCBS.Web`)](#146-giao-diện-người-dùng-angular-19-đồng-bộ-chuẩn-c-gốc-opcbsweb)
    - [14.7. Phân Tích Mã Nguồn Thanh Điều Hướng Lâm Sàng (Clinical Main Navbar)](#147-phân-tích-mã-nguồn-thanh-điều-hướng-lâm-sàng-clinical-main-navbar)
    - [14.8. Phân Tích Mã Nguồn Quản Lý Gói Dịch Vụ, Gia Hạn Thông Minh & Lịch Sử Giao Dịch Bác Sĩ](#148-phân-tích-mã-nguồn-quản-lý-gói-dịch-vụ-gia-hạn-thông-minh--lịch-sử-giao-dịch-bác-sĩ)
15. [Phân Tích Chi Tiết Mã Nguồn Chuẩn Hóa Nền Tảng Backend (Ưu tiên 2) & Cải Tiến Toàn Diện Microservices (Ưu tiên 3)](#15-phân-tích-chi-tiết-mã-nguồn-chuẩn-hóa-nền-tảng-backend-ưu-tiên-2--cải-tiến-toàn-diện-microservices-ưu-tiên-3)
    - [15.1. Cơ Chế Fail-Closed Token Blacklist & Redis Eviction Policy (`opcbs-auth-service`)](#151-cơ-chế-fail-closed-token-blacklist--redis-eviction-policy-opcbs-auth-service)
    - [15.2. Chống Race Condition & Idempotency Key Tại Tầng Booking (`opcbs-booking-service`)](#152-chống-race-condition--idempotency-key-tại-tầng-booking-opcbs-booking-service)
    - [15.3. Transactional Event Synchronization & An Toàn Lưu Trữ File (`opcbs-booking-service`)](#153-transactional-event-synchronization--an-toàn-lưu-trữ-file-opcbs-booking-service)
    - [15.4. Khắc Phục Triệt Để Lỗ Hổng Rò Rỉ Dữ Liệu Bệnh Án (IDOR) & Audit Log Y Tế (`opcbs-clinical-service`)](#154-khắc-phục-triệt-để-lỗ-hổng-rò-rỉ-dữ-liệu-bệnh-án-idor--audit-log-y-tế-opcbs-clinical-service)
    - [15.5. Trừ Buổi Trị Liệu Chuẩn Xác Khi Hoàn Tất Ca Khám & Phân Quyền Phác Đồ (`opcbs-treatment-service`)](#155-trừ-buổi-trị-liệu-chuẩn-xác-khi-hoàn-tất-ca-khám--phân-quyền-phác-đồ-opcbs-treatment-service)
    - [15.6. Phân Quyền Truy Cập & Bảo Mật Kết Quả Trắc Nghiệm Tâm Lý (`opcbs-psychometric-service`)](#156-phân-quyền-truy-cập--bảo-mật-kết-quả-trắc-nghiệm-tâm-lý-opcbs-psychometric-service)
    - [15.7. Tách Biệt Kênh & Quản Lý Vòng Đời Trạng Thái Thông Báo (`opcbs-notification-service`)](#157-tách-biệt-kênh--quản-lý-vòng-đời-trạng-thái-thông-báo-opcbs-notification-service)
    - [15.8. Xác Thực Chữ Ký HMAC-SHA512, Đối Chiếu Số Tiền & Chặn Mock Checkout Prod (`opcbs-payment-service`)](#158-xác-thực-chữ-ký-hmac-sha512-đối-chiếu-số-tiền--chặn-mock-checkout-prod-opcbs-payment-service)
    - [15.9. Bộ Lọc HTML Sanitizer Chống Tấn Công XSS & Bảo Vệ Quy Trình Bài Viết (`opcbs-blog-service`)](#159-bộ-lọc-html-sanitizer-chống-tấn-công-xss--bảo-vệ-quy-trình-bài-viết-opcbs-blog-service)
    - [15.10. Participant Access Control (PAC) & Nguyên Tắc Zero Client Trust (`opcbs-message-service`)](#1510-participant-access-control-pac--nguyên-tắc-zero-client-trust-opcbs-message-service)

---

## 1. Sơ Đồ Luồng Đi Dữ Liệu Toàn Cục (End-to-End Flow Diagram)

Dưới đây là hành trình từng bước của một gói tin (payload) khi người dùng nhấn nút **"Đăng nhập"**:

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Người Dùng
    participant View as 🖼️ login.html
    participant Comp as ⚡ login.ts (Angular 19)
    participant AuthJwt as 🔐 auth-jwt.service.ts
    participant Gateway as 🚪 proxy.config.mjs (Port 9000)
    participant Controller as 🎯 AuthenticateController.java (:8081)
    participant AuthManager as 🛡️ Spring Security AuthManager
    participant UserDetails as 📖 DomainUserDetailsService
    participant DB as 🗄️ MySQL Database (jhi_user)
    participant AccountSvc as 👤 account.service.ts
    participant Router as 🧭 Angular Router

    User->>View: Nhập username, password, tích rememberMe
    User->>View: Click nút "Đăng nhập ngay" (type="submit")
    View->>Comp: Gọi hàm login()
    Comp->>Comp: Đọc loginForm.getRawValue()
    Comp->>AuthJwt: loginService.login(credentials)
    AuthJwt->>Gateway: POST /api/authenticate {username, password, rememberMe}
    Gateway->>Controller: Chuyển tiếp tới http://127.0.0.1:8081/api/authenticate
    Controller->>AuthManager: authenticate(UsernamePasswordAuthenticationToken)
    AuthManager->>UserDetails: loadUserByUsername(login/email)
    UserDetails->>DB: SELECT * FROM jhi_user WHERE login = ? (kèm authorities)
    DB-->>UserDetails: Trả về Entity User (mật khẩu băm BCrypt, activated)
    UserDetails->>UserDetails: Kiểm tra activated (throw UserNotActivatedException nếu false)
    UserDetails-->>AuthManager: Trả về UserDetails (UserWithId)
    AuthManager->>AuthManager: PasswordEncoder.matches(rawPassword, encodedPassword)
    AuthManager-->>Controller: Trả về Authentication Object thành công
    Controller->>Controller: createToken(): Ký JWT Token (HS512 + Shared Secret)
    Controller-->>AuthJwt: HTTP 200 OK {"id_token": "eyJhbGciOi..."}
    AuthJwt->>AuthJwt: StateStorageService.storeAuthenticationToken(jwt, rememberMe)
    AuthJwt-->>Comp: Observable Emit hoàn tất
    Comp->>AccountSvc: accountService.identity(true)
    AccountSvc->>Gateway: GET /api/account (Kèm Header Authorization: Bearer <jwt>)
    Gateway->>Controller: Chuyển tiếp tới Auth Service
    Controller-->>AccountSvc: Trả về thông tin User (login, email, authorities)
    AccountSvc->>AccountSvc: Lưu state UserIdentity signal
    Comp->>Router: router.navigate(['']) -> Chuyển về Trang chủ / Dashboard
```

---

## 2. Chi Tiết Phía Frontend (Angular 19 SPA)

### 2.1. Giao diện & Nút bấm (`login.html`)

Vị trí file: `mindbridge-jhipster/src/main/webapp/app/login/login.html`

```html
<!-- 1. Form đăng nhập liên kết với loginForm bên TypeScript -->
<form (ngSubmit)="login()" [formGroup]="loginForm" class="auth-form-body">

  <!-- Ô nhập Tên đăng nhập hoặc Email -->
  <div class="mb-4">
    <label class="form-label font-weight-bold" for="username">Tài khoản hoặc Email</label>
    <div class="input-group">
      <span class="input-group-text"><i class="bi bi-person"></i></span>
      <input 
        #username 
        type="text" 
        class="form-control" 
        id="username" 
        name="username" 
        formControlName="username" 
        placeholder="Nhập tên đăng nhập hoặc email..."
      />
    </div>
  </div>

  <!-- Ô nhập Mật khẩu kèm nút Toggle Ẩn/Hiện -->
  <div class="mb-4">
    <div class="d-flex justify-content-between">
      <label class="form-label font-weight-bold" for="password">Mật khẩu</label>
      <a routerLink="/account/reset/request" class="forgot-password-link">Quên mật khẩu?</a>
    </div>
    <div class="input-group">
      <span class="input-group-text"><i class="bi bi-lock"></i></span>
      <input 
        [type]="showPassword() ? 'text' : 'password'" 
        class="form-control" 
        id="password" 
        name="password" 
        formControlName="password" 
        placeholder="Nhập mật khẩu của bạn..."
      />
      <!-- Nút xem/ẩn mật khẩu -->
      <button class="btn btn-outline-secondary" type="button" (click)="togglePasswordVisibility()">
        <i [class]="showPassword() ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
      </button>
    </div>
  </div>

  <!-- Checkbox Ghi nhớ đăng nhập -->
  <div class="form-check mb-4">
    <input class="form-check-input" type="checkbox" id="rememberMe" name="rememberMe" formControlName="rememberMe" />
    <label class="form-check-label" for="rememberMe">Ghi nhớ đăng nhập trên thiết bị này</label>
  </div>

  <!-- NÚT CHÍNH: Submit Form Đăng nhập -->
  <button type="submit" class="btn btn-primary w-100 btn-auth-submit" [disabled]="loginForm.invalid">
    <span>Đăng nhập ngay</span>
    <i class="bi bi-arrow-right ms-2"></i>
  </button>

  <!-- NÚT PHỤ: Đăng nhập nhanh Google One-Tap -->
  <button type="button" class="btn btn-outline-dark w-100 btn-google-auth mt-3" (click)="loginWithGoogle()">
    <img src="content/images/google-icon.svg" alt="Google" width="20" height="20" class="me-2" />
    <span>Đăng nhập với Google</span>
  </button>

  <!-- CÁC NÚT DEMO TEST NHANH DÀNH CHO GIẢNG VIÊN / TESTER -->
  <div class="demo-accounts-pill-box mt-4">
    <span class="badge bg-secondary me-2" (click)="fillDemo('doctor1', 'user')">Bác sĩ: doctor1</span>
    <span class="badge bg-secondary me-2" (click)="fillDemo('user', 'user')">Bệnh nhân: user</span>
    <span class="badge bg-secondary" (click)="fillDemo('admin', 'admin')">Quản trị: admin</span>
  </div>
</form>
```

#### 🔍 Tại sao các nút lại gửi thông tin như vậy?
1. **Nút `type="submit"` (`Đăng nhập ngay`)**:
   - Thuộc tính `[disabled]="loginForm.invalid"` sẽ vô hiệu hóa nút bấm (nút mờ đi, không click được) nếu người dùng để trống `username` hoặc `password`. Điều này ngăn chặn việc gửi các HTTP request vô nghĩa lên server, tiết kiệm tài nguyên mạng.
   - Khi form hợp lệ, sự kiện `(ngSubmit)="login()"` trên thẻ `<form>` sẽ được kích hoạt (cả khi click chuột hoặc bấm phím `Enter`).
2. **Nút `fillDemo(user, pass)`**:
   - Sử dụng `this.loginForm.patchValue({...})` để điền nhanh username/password mẫu vào `FormGroup`, đồng thời tự động đánh dấu `rememberMe = true` để tiết kiệm thời gian test.

---

### 2.2. Logic Component & Quản lý Trạng thái (`login.ts`)

Vị trí file: `mindbridge-jhipster/src/main/webapp/app/login/login.ts`

```typescript
@Component({
  selector: 'jhi-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss'],
})
export default class Login implements OnInit, AfterViewInit {
  // 1. Quản lý trạng thái Reactive bằng Angular 19 Signals
  readonly authenticationError = signal(false);
  readonly errorMessage = signal<string>('');
  readonly errorCode = signal<'NOT_ACTIVATED' | 'USER_NOT_FOUND' | 'INVALID_PASSWORD' | 'GENERIC'>('GENERIC');
  readonly unactivatedAccount = signal<string>('');
  readonly showPassword = signal(false);
  readonly isGoogleLoading = signal(false);

  // 2. Khởi tạo Reactive Form với các validator bắt buộc
  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  // 3. Dependency Injection hiện đại bằng hàm inject()
  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly googleAuthService = inject(GoogleAuthService);

  // 4. Hàm xử lý sự kiện Đăng Nhập
  login(): void {
    this.clearErrors();
    const credentials = this.loginForm.getRawValue(); // Trích xuất {username, password, rememberMe}

    this.loginService.login(credentials).subscribe({
      next: () => {
        // Đăng nhập thành công -> Điều hướng về trang chủ
        if (!this.router.currentNavigation()) {
          this.router.navigate(['']);
        }
      },
      error: response => {
        // Bắt lỗi từ Backend và phân loại thông báo thân thiện
        this.authenticationError.set(true);
        const errorKey = response?.error?.errorKey || response?.headers?.get('x-opcbs-error');
        const errorDetail = response?.error?.detail || response?.error?.title || '';

        if (errorKey === 'userNotActivated' || errorDetail.toLowerCase().includes('not activated')) {
          this.errorCode.set('NOT_ACTIVATED');
          this.unactivatedAccount.set(credentials.username);
          this.errorMessage.set('Tài khoản của bạn chưa được xác thực email. Vui lòng nhập mã OTP để kích hoạt tài khoản.');
        } else if (errorKey === 'userNotFound' || errorDetail.toLowerCase().includes('không tồn tại')) {
          this.errorCode.set('USER_NOT_FOUND');
          this.errorMessage.set('Tài khoản hoặc Email này chưa được đăng ký trong hệ thống.');
        } else if (errorKey === 'invalidPassword' || errorDetail.toLowerCase().includes('mật khẩu không chính xác')) {
          this.errorCode.set('INVALID_PASSWORD');
          this.errorMessage.set('Mật khẩu không chính xác. Vui lòng kiểm tra lại.');
        } else {
          this.errorCode.set('GENERIC');
          this.errorMessage.set('Đăng nhập không thành công! Vui lòng kiểm tra lại thông tin.');
        }
      },
    });
  }
}
```

#### 🔍 Cơ chế hoạt động của code trên:
* `this.loginForm.getRawValue()`: Lấy đối tượng JavaScript nguyên bản `{ username: "doctor1", password: "user", rememberMe: true }`.
* **Phân loại lỗi thông minh**: Thay vì chỉ báo chung chung *"Lỗi 401 Unauthorized"*, hàm `error` phân tích trường `errorKey` trả về từ Backend để đổi trạng thái `errorCode` sang `NOT_ACTIVATED` (hiển thị nút bấm nhập OTP), `USER_NOT_FOUND` (hướng dẫn đăng ký), hoặc `INVALID_PASSWORD` (gợi ý quên mật khẩu).

---

### 2.3. Điều phối nghiệp vụ đăng nhập (`login.service.ts`)

Vị trí file: `mindbridge-jhipster/src/main/webapp/app/login/login.service.ts`

```typescript
@Service()
export class LoginService {
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);

  login(credentials: Login): Observable<Account | null> {
    // Bước 1: Gửi credentials sang AuthServerProvider để lấy và lưu Token
    // Bước 2: Dùng toán tử mergeMap() để lập tức gọi tiếp accountService.identity(true)
    return this.authServerProvider.login(credentials).pipe(
      mergeMap(() => this.accountService.identity(true))
    );
  }

  logout(): void {
    this.authServerProvider.logout().subscribe({ 
      complete: () => this.accountService.authenticate(null) 
    });
  }
}
```

#### 🔍 Tại sao lại dùng `mergeMap`?
* Toán tử `mergeMap` (FlatMap) trong RxJS cho phép xâu chuỗi hai tác vụ bất đồng bộ liên tiếp:
  1. Gọi API `POST /api/authenticate` $\rightarrow$ Nhận được JWT Token và lưu vào bộ nhớ trình duyệt.
  2. Ngay sau khi có Token, tự động kích hoạt API `GET /api/account` kèm theo Token vừa lưu để tải toàn bộ quyền hạn (`ROLE_DOCTOR`, `ROLE_PATIENT`) của người dùng trước khi chuyển trang.

---

### 2.4. Lưu trữ & Quản lý Token JWT (`auth-jwt.service.ts`)

Vị trí file: `mindbridge-jhipster/src/main/webapp/app/core/auth/auth-jwt.service.ts`

```typescript
type JwtToken = {
  id_token: string;
};

@Service()
export class AuthServerProvider {
  private readonly http = inject(HttpClient);
  private readonly stateStorageService = inject(StateStorageService);

  login(credentials: Login): Observable<void> {
    // Gửi HTTP POST tới endpoint /api/authenticate
    return this.http
      .post<JwtToken>(`${serverApiUrl}api/authenticate`, credentials)
      .pipe(map(response => this.authenticateSuccess(response, credentials.rememberMe)));
  }

  private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
    // Lưu token id_token vào LocalStorage hoặc SessionStorage
    this.stateStorageService.storeAuthenticationToken(response.id_token, rememberMe);
  }
}
```

#### 🔍 Cơ chế `rememberMe`:
* Nếu người dùng **tích chọn** `rememberMe = true` $\rightarrow$ Token được lưu vào `localStorage` (tồn tại vĩnh viễn kể cả khi tắt trình duyệt).
* Nếu **không tích chọn** `rememberMe = false` $\rightarrow$ Token được lưu vào `sessionStorage` (sẽ tự động biến mất khi người dùng đóng tab trình duyệt).

---

### 2.5. Tra cứu hồ sơ & Quyền hạn (`account.service.ts`)

Vị trí file: `mindbridge-jhipster/src/main/webapp/app/core/auth/account.service.ts`

* **Nhiệm vụ**: Quản lý thông tin định danh toàn cục (`userIdentity = signal<Account | null>(null)`).
* Khi gọi `identity(true)`, hàm gửi request `GET /api/account`.
* Một HTTP Interceptor (`auth.interceptor.ts`) sẽ tự động lấy Token từ `StateStorageService` và đính kèm vào Header:
  ```http
  Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkb2N0b3IxIiwiYXV0aCI6IlJPTEVfRE9DVE9SIn0...
  ```

---

## 3. Cơ Chế Phân Luồng Gateway (`proxy.config.mjs`)

Vị trí file: `mindbridge-jhipster/proxy.config.mjs`

Khi Angular SPA chạy trên cổng `http://localhost:9000` gửi request `/api/authenticate`, file cấu hình Proxy sẽ chặn lại và điều hướng:

```javascript
export default {
  // Regex khớp toàn bộ các URL liên quan đến tài khoản & xác thực
  '^/api/(authenticate|register|register-doctor|account|users|admin/users|authorities)': {
    target: 'http://127.0.0.1:8081', // Chuyển tiếp tới opcbs-auth-service
    changeOrigin: true,
    xfwd: true,
  },
};
```

---

## 4. Chi Tiết Phía Backend (Spring Boot 3.4 / Security 6 - `opcbs-auth-service`)

---

### 4.1. Controller Tiếp Nhận Request (`AuthenticateController.java`)

Vị trí file: `opcbs-auth-service/src/main/java/com/ocbs/web/rest/AuthenticateController.java`

```java
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private final JwtEncoder jwtEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
        String username = loginVM.getUsername() != null ? loginVM.getUsername().trim() : "";
        
        // 1. Đóng gói thông tin thô thành Token xác thực của Spring Security
        var authenticationToken = new UsernamePasswordAuthenticationToken(username, loginVM.getPassword());

        try {
            // 2. Kích hoạt chu trình kiểm tra thông tin qua AuthenticationManager
            var authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            
            // 3. Đưa thông tin xác thực vào Security Context của thread hiện tại
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 4. Sinh chữ ký số JWT Token
            String jwt = this.createToken(authentication, loginVM.isRememberMe());
            
            var httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(jwt);
            
            // 5. Trả về cho Angular Client mã 200 OK kèm Token
            return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
            
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // 6. Xử lý ngoại lệ xác thực & Bóc tách chính xác nguyên nhân lỗi
            Throwable root = ex;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            
            // TH1: Tài khoản chưa xác thực OTP Email
            if (root instanceof UserNotActivatedException || ex.getMessage().contains("not activated")) {
                throw new BadRequestAlertException(
                    "Tài khoản chưa được kích hoạt email. Vui lòng nhập mã OTP để hoàn tất xác thực.",
                    "userManagement", "userNotActivated"
                );
            }

            // TH2: Phân biệt Tài khoản không tồn tại vs Sai mật khẩu
            String searchKey = username.toLowerCase();
            boolean userExists = userRepository.findOneByLogin(searchKey).isPresent() ||
                                 userRepository.findOneByEmailIgnoreCase(searchKey).isPresent();

            if (!userExists) {
                throw new BadRequestAlertException("Tài khoản hoặc Email không tồn tại trong hệ thống.", "userManagement", "userNotFound");
            } else {
                throw new BadRequestAlertException("Mật khẩu không chính xác. Vui lòng thử lại.", "userManagement", "invalidPassword");
            }
        }
    }

    // Hàm tạo chuỗi JWT Token
    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity = rememberMe 
            ? now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS) // 30 ngày
            : now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);              // 24 giờ

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim("auth", authorities)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
```

#### 🔍 Trọng tâm kỹ thuật:
1. `authenticationManagerBuilder.getObject().authenticate(...)`: Lệnh này sẽ tự động tìm bean `UserDetailsService` (chính là `DomainUserDetailsService`) để nạp thông tin từ database.
2. `createToken(...)`: Tạo ra token chuẩn RFC 7519, nhúng danh sách vai trò (`auth: "ROLE_DOCTOR"` hoặc `auth: "ROLE_PATIENT"`). Token này được ký bằng thuật toán **HMAC-SHA512** và Secret Key dùng chung.

---

### 4.2. Dịch Vụ Tra Cứu Tài Khoản Database (`DomainUserDetailsService.java`)

Vị trí file: `opcbs-auth-service/src/main/java/com/ocbs/security/DomainUserDetailsService.java`

```java
@Service("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        // 1. Nếu người dùng nhập vào là định dạng Email (ví dụ: patient@gmail.com)
        if (new EmailValidator().isValid(login, null)) {
            return userRepository
                .findOneWithAuthoritiesByEmailIgnoreCase(login)
                .map(user -> createSpringSecurityUser(login, user))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + login));
        }

        // 2. Nếu người dùng nhập vào là Username (ví dụ: doctor1)
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return userRepository
            .findOneWithAuthoritiesByLogin(lowercaseLogin)
            .map(user -> createSpringSecurityUser(lowercaseLogin, user))
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + lowercaseLogin));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        // 3. Kiểm tra cờ kích hoạt tài khoản
        if (!user.isActivated()) {
            throw new UserNotActivatedException("Tài khoản " + lowercaseLogin + " chưa được kích hoạt OTP");
        }
        
        // 4. Trả về đối tượng User của Spring Security kèm danh sách quyền
        return UserWithId.fromUser(user);
    }
}
```

#### 🔍 Điểm nổi bật:
* Hỗ trợ **Đăng nhập kép**: Người dùng có thể điền cả Tên tài khoản (`doctor1`) hoặc Địa chỉ Email (`doctor1@mindbridge.vn`) vào cùng một ô input, hệ thống tự động nhận diện và query tương ứng.
* Chặn cửa bảo mật: Nếu `user.isActivated() == false`, hàm ném ngay `UserNotActivatedException`, ngăn cản người dùng chưa xác thực email đăng nhập vào hệ thống.

---

### 4.3. Quản Trị Người Dùng & Đăng Ký (`UserService.java`)

Vị trí file: `opcbs-auth-service/src/main/java/com/ocbs/service/UserService.java`

* **Khi Đăng ký Bệnh nhân (`registerUser`) hoặc Bác sĩ (`registerDoctor`)**:
  1. Mã hóa mật khẩu bằng thuật toán **BCrypt**:
     ```java
     String encryptedPassword = passwordEncoder.encode(password);
     newUser.setPassword(encryptedPassword);
     ```
  2. Sinh mã xác thực **OTP 6 chữ số ngẫu nhiên**:
     ```java
     String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
     newUser.setActivated(false);
     newUser.setActivationKey(otp);
     newUser.setResetDate(Instant.now()); // Lưu mốc thời gian tạo OTP
     ```
  3. Gán quyền mặc định (`ROLE_PATIENT` hoặc `ROLE_DOCTOR`).
  4. Tự động khởi tạo hồ sơ `PatientProfile` hoặc `DoctorProfile`.
  5. Gọi `mailService.sendActivationOtpEmail(newUser, otp)` để gửi thư.

---

### 4.4. Dịch Vụ Gửi Email OTP (`MailService.java`)

Vị trí file: `opcbs-auth-service/src/main/java/com/ocbs/service/MailService.java`

```java
@Async
public void sendActivationOtpEmail(User user, String otp) {
    LOG.info(">>> [MindBridge] ACTIVATION OTP FOR USER '{}' ({}): {} <<<", user.getLogin(), user.getEmail(), otp);

    String subject = "[MindBridge] Mã OTP Xác Thực Kích Hoạt Tài Khoản: " + otp;
    String htmlContent = """
        ... Nội dung Email HTML giao diện MindBridge sang trọng ...
        Mã OTP: %s (Có hiệu lực trong 10 phút)
    """.formatted(otp);

    // Gửi email thực tế qua giao thức SMTP cổng 587
    sendEmailSync(user.getEmail(), subject, htmlContent, false, true);
}
```

---

## 5. Tương Tác Cơ Sở Dữ Liệu MySQL

### 5.1. Cấu trúc Bảng & Khóa ngoại liên quan

```
┌─────────────────────────────────────────────────────────────┐
│                          jhi_user                           │
├───────────────────┬───────────────────┬─────────────────────┤
│ id (PK)           │ BIGINT            │ Khóa chính          │
│ login             │ VARCHAR(50)       │ Tên đăng nhập (UQ)  │
│ password_hash     │ VARCHAR(60)       │ Mật khẩu băm BCrypt │
│ email             │ VARCHAR(191)      │ Email tài khoản(UQ) │
│ activated         │ BIT(1)            │ 1: Đã kích hoạt     │
│ activation_key    │ VARCHAR(20)       │ Mã OTP 6 số         │
│ reset_date        │ TIMESTAMP         │ Thời điểm tạo OTP   │
│ google_id         │ VARCHAR(100)      │ Google Sub ID       │
└───────────────────┴───────────────────┴─────────────────────┘
                               ▲
                               │ 1-n
┌──────────────────────────────┴──────────────────────────────┐
│                     jhi_user_authority                      │
├───────────────────┬───────────────────┬─────────────────────┤
│ user_id (FK)      │ BIGINT            │ Tham chiếu jhi_user │
│ authority_name(FK)│ VARCHAR(50)       │ Tham chiếu jhi_auth │
└───────────────────┴───────────────────┴─────────────────────┘
                               │ n-1
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        jhi_authority                        │
├───────────────────┬───────────────────┬─────────────────────┤
│ name (PK)         │ VARCHAR(50)       │ ROLE_ADMIN / DOCTOR │
└───────────────────┴───────────────────┴─────────────────────┘
```

### 5.2. Các câu lệnh SQL Hibernate thực thi ngầm

1. **Khi Bệnh nhân đăng nhập bằng Username (`doctor1`)**:
   ```sql
   SELECT u.id, u.login, u.password_hash, u.activated, u.email, a.authority_name 
   FROM jhi_user u 
   LEFT OUTER JOIN jhi_user_authority ua ON u.id = ua.user_id 
   LEFT OUTER JOIN jhi_authority a ON ua.authority_name = a.name 
   WHERE LOWER(u.login) = 'doctor1';
   ```

2. **Khi Bệnh nhân nhập mã OTP kích hoạt tài khoản (`POST /api/account/verify-otp`)**:
   ```sql
   -- Bước 1: Tìm tài khoản theo login/email
   SELECT * FROM jhi_user WHERE LOWER(login) = 'patient_test' OR LOWER(email) = 'patient_test@gmail.com';
   
   -- Bước 2: Cập nhật trạng thái sau khi khớp mã OTP
   UPDATE jhi_user 
   SET activated = 1, activation_key = NULL 
   WHERE id = 42;
   ```

---

## 6. Bảng Tra Cứu Nhanh Hàm & Trách Nhiệm (Quick Reference Map)

| Thành Phần (Layer) | Tên File | Hàm / Phương Thức | Trách Nhiệm Chi Tiết |
|---|---|---|---|
| **UI View** | `login.html` | `<form (ngSubmit)="login()">` | Hiển thị form, bắt sự kiện click nút đăng nhập, khóa nút khi form chưa hợp lệ. |
| **Component** | `login.ts` | `login()` | Đọc giá trị từ `FormGroup`, gọi service, phân loại mã lỗi (`NOT_ACTIVATED`, `USER_NOT_FOUND`). |
| **Service Layer** | `login.service.ts` | `login(credentials)` | Nối chuỗi `mergeMap`: gọi AuthServerProvider lấy token $\rightarrow$ gọi tiếp `AccountService` lấy quyền. |
| **Auth Provider** | `auth-jwt.service.ts` | `login(credentials)` | Gửi `POST /api/authenticate`, nhận `id_token` và lưu vào `localStorage`/`sessionStorage`. |
| **Account State** | `account.service.ts` | `identity(force)` | Gửi `GET /api/account` lấy thông tin user, phát tín hiệu reactive `userIdentity.set(account)`. |
| **API Gateway** | `proxy.config.mjs` | `^/api/(authenticate\|...)` | Điều hướng request cổng 9000 sang `http://127.0.0.1:8081`. |
| **REST Controller**| `AuthenticateController.java` | `authorize(loginVM)` | Tiếp nhận request, gọi Spring Security xác thực, ký JWT Token HS512 trả về cho client. |
| **User Details** | `DomainUserDetailsService.java`| `loadUserByUsername(login)`| Truy vấn database theo login/email, kiểm tra cờ `isActivated()`, trả về UserDetails. |
| **User Service** | `UserService.java` | `registerUser(...)` | Băm mật khẩu BCrypt, sinh OTP 6 số, gán quyền `ROLE_PATIENT`, tạo `PatientProfile`. |
| **Mail Service** | `MailService.java` | `sendActivationOtpEmail(...)` | Soạn thảo HTML template và gửi email OTP thực tế qua Google SMTP cổng 587. |

---

## 7. Cơ Chế Giao Tiếp & Xử Lý Code Giữa 5 Microservices Độc Lập

* **Thời gian cập nhật**: `2026-10-02T11:40:00+07:00`
* **Tên task**: *Refactoring Core Service into 4 Specialized Microservices (Booking, Clinical, Treatment, Psychometric) + Auth Service*

### 7.1. Bảng Phân Luồng Endpoint và Dịch Vụ Phụ Trách

```mermaid
graph LR
    SPA["🌐 Angular SPA Client"] --> Proxy["🚪 proxy.config.mjs"]
    Proxy -- "1. /api/authenticate, /api/register*" --> S1["🔐 opcbs-auth-service (:8081)"]
    Proxy -- "2. /api/doctor-profiles, /api/appointments" --> S2["📅 opcbs-booking-service (:8082)"]
    Proxy -- "3. /api/doctor/my-patients, /api/doctor/patients/*" --> S3["🩺 opcbs-clinical-service (:8083)"]
    Proxy -- "4. /api/treatment-packages, /api/patient/journals" --> S4["💊 opcbs-treatment-service (:8084)"]
    Proxy -- "5. /api/psychometrics/**" --> S5["🧠 opcbs-psychometric-service (:8085)"]
```

### 7.2. Cách thức các hàm phía Backend thực thi khi nhận Token:
1. **Trích xuất thông tin người dùng từ Token**:
   - Khi có request gửi kèm `Authorization: Bearer <Token>` đến bất kỳ service nào (`8082`, `8083`, `8084`, `8085`), bộ lọc Spring Security của service đó tự động gọi:
     ```java
     String login = SecurityUtils.getCurrentUserLogin().orElseThrow(...);
     ```
   - Lệnh này lấy trường `sub` từ Payload JWT đã được giải mã in-memory bằng Secret Key chung.
2. **Kiểm tra vai trò và phân quyền (RBAC)**:
   - Các controller sử dụng annotation `@PreAuthorize("hasAuthority('ROLE_DOCTOR')")` hoặc `@PreAuthorize("hasAuthority('ROLE_PATIENT')")`.
   - Spring Security đối chiếu trực tiếp với trường `auth` trong Token để quyết định cho phép hoặc từ chối (`403 Forbidden`).
3. **Thực thi nghiệp vụ và truy vấn CSDL**:
   - Ví dụ tại `opcbs-clinical-service` (Port 8083):
     - Hàm `getMyDoctorPatients()` tìm `DoctorProfile` theo `login`, sau đó lọc các cuộc hẹn và danh sách bệnh nhân thuộc quyền phụ trách của bác sĩ đó, trả về JSON DTO cho SPA.

---

## 8. Giải Thích Code Service Discovery Với Netflix Eureka Server

* **Thời gian cập nhật**: `2026-10-02T11:50:00+07:00`
* **File chính**: `opcbs-eureka-server/src/main/java/com/ocbs/eureka/EurekaServerApp.java`

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApp {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApp.class, args);
    }
}
```

### 🔍 Cơ chế hoạt động:
1. **Annotation `@EnableEurekaServer`**:
   - Bật máy chủ đăng ký dịch vụ của Netflix Eureka trên cổng `8761`.
   - Khởi tạo bộ nhớ RAM Registry lưu trữ danh sách các Microservices kèm IP, Port và trạng thái sống/chết.
2. **Cấu hình `application.yml`**:
   ```yaml
   eureka:
     client:
       register-with-eureka: false  # Bản thân Eureka Server không cần tự đăng ký vào chính nó
       fetch-registry: false        # Không cần kéo danh bạ từ server khác
   ```
3. **Cơ chế Heartbeat (Nhịp tim 30 giây)**:
   - Các microservices con định kỳ 30 giây gửi một gói tin HTTP POST (Heartbeat) về `http://localhost:8761/eureka/apps/{appName}`.
   - Nếu quá 90 giây không nhận được Heartbeat, Eureka Server tự động gỡ bỏ instance đó khỏi danh sách để tránh gửi request vào service đã chết.

---

## 9. Hướng Dẫn Chi Tiết Cách Sử Dụng Apache Kafka Trong Microservices

* **Thời gian cập nhật**: `2026-10-02T15:30:00+07:00`
* **Mục tiêu**: Hướng dẫn toàn diện cách tích hợp, cấu hình, lập trình Producer/Consumer và vận hành Apache Kafka trong hệ thống phân tán MindBridge OPCBS Microservices.

---

### 9.1. Các Khái Niệm Cốt Lõi Cần Nắm Vững

Apache Kafka là một nền tảng phân tán xử lý luồng sự kiện (Distributed Event Streaming Platform) với độ trễ cực thấp (< 10ms) và thông lượng cao (hàng triệu tin nhắn/giây).

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              APACHE KAFKA CLUSTER                                │
│                                                                                 │
│   TOPIC: opcbs-appointment-events (Hàng đợi sự kiện cuộc hẹn)                   │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │ Partition 0: [Msg 0] -> [Msg 1] -> [Msg 2] -> [Msg 3] (Offset = 3)      │   │
│   ├─────────────────────────────────────────────────────────────────────────┤   │
│   │ Partition 1: [Msg 0] -> [Msg 1] -> [Msg 2]            (Offset = 2)      │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
         ▲                                                │
         │ (publish)                                      │ (subscribe)
┌─────────────────┐                       ┌───────────────┴───────────────┐
│  PRODUCER       │                       │  CONSUMER GROUP: clinical-grp │
│  BookingSvc     │                       │  ClinicalService Instance 1   │
│  (:8082)        │                       │  (:8083)                      │
└─────────────────┘                       └───────────────────────────────┘
```

| Thuật ngữ | Ý nghĩa kỹ thuật trong hệ thống OPCBS |
|---|---|
| **Broker** | Server Kafka chạy trên cổng `9092` chịu trách nhiệm lưu trữ và trung chuyển message. |
| **Topic** | Kênh (channel) phân loại thông điệp. Ví dụ: `opcbs-appointment-events`, `opcbs-user-events`. |
| **Partition** | Các phân vùng vật lý bên trong 1 Topic giúp Kafka mở rộng (scale) song song. |
| **Producer** | Service phát ra sự kiện (Ví dụ: `opcbs-booking-service` phát sự kiện đặt lịch). |
| **Consumer** | Service đăng ký lắng nghe và xử lý sự kiện (Ví dụ: `opcbs-clinical-service` đón nhận để mở bệnh án). |
| **Consumer Group** | Nhóm các instance của cùng 1 service chia sẻ nhau xử lý message, đảm bảo mỗi message chỉ xử lý 1 lần. |
| **Offset** | Con trỏ số nguyên chỉ vị trí tin nhắn tiếp theo mà Consumer cần đọc trong Partition. |

---

### 9.2. Cấu Hình Kafka Trong Spring Boot (`application.yml`)

#### A. Phía Producer (`opcbs-booking-service` - Port 8082):
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all              # Đảm bảo 100% broker ghi nhận tin nhắn trước khi báo thành công
      retries: 3             # Tự động gửi lại tối đa 3 lần nếu gặp sự cố mạng tạm thời
      properties:
        enable.idempotence: true # Chống gửi trùng lặp message khi retry
```

#### B. Phía Consumer (`opcbs-clinical-service` - Port 8083):
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: clinical-service-group
      auto-offset-reset: earliest # Đọc từ tin nhắn đầu tiên nếu là consumer mới
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false   # Tắt auto commit để commit thủ công (Manual Ack) đảm bảo tính toàn vẹn
      properties:
        spring.json.trusted.packages: "com.ocbs.event,com.ocbs.domain.*" # Cho phép deserialize các package an toàn
```

---

### 9.3. Định Nghĩa Mô Hình Dữ Liệu Sự Kiện (Event Data Models)

Để các service hiểu chung một định dạng dữ liệu, đối tượng Event phải tuân thủ chuẩn Java Bean và hỗ trợ tuần tự hóa JSON:

```java
package com.ocbs.event;

import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppointmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("appointmentId")
    private Long appointmentId;

    @JsonProperty("bookingCode")
    private String bookingCode;

    @JsonProperty("patientId")
    private Long patientId;

    @JsonProperty("patientName")
    private String patientName;

    @JsonProperty("doctorId")
    private Long doctorId;

    @JsonProperty("doctorName")
    private String doctorName;

    @JsonProperty("eventType")
    private String eventType; // "APPOINTMENT_BOOKED", "APPOINTMENT_ACCEPTED", "APPOINTMENT_COMPLETED"

    @JsonProperty("eventTime")
    private Instant eventTime;

    @JsonProperty("notes")
    private String notes;

    // Constructors
    public AppointmentEvent() {}

    public AppointmentEvent(Long appointmentId, String bookingCode, Long patientId, 
                            String patientName, Long doctorId, String doctorName, 
                            String eventType, Instant eventTime, String notes) {
        this.appointmentId = appointmentId;
        this.bookingCode = bookingCode;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.eventType = eventType;
        this.eventTime = eventTime != null ? eventTime : Instant.now();
        this.notes = notes;
    }

    // Getters & Setters
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "AppointmentEvent{" +
            "appointmentId=" + appointmentId +
            ", bookingCode='" + bookingCode + '\'' +
            ", eventType='" + eventType + '\'' +
            ", patientId=" + patientId +
            ", doctorId=" + doctorId +
            '}';
    }
}
```

---

### 9.4. Lập Trình Kafka Producer - Phát Tin Nhắn Bất Đồng Bộ

Producer sử dụng `KafkaTemplate<String, Object>` để đẩy tin nhắn vào topic kèm xử lý **Async Callback**:

```java
package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class AppointmentEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventProducer.class);
    public static final String APPOINTMENT_TOPIC = "opcbs-appointment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Dependency Injection qua Constructor
    public AppointmentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Phát sự kiện biến động cuộc hẹn vào Kafka Topic
     * @param event Thông tin sự kiện
     */
    public void publishAppointmentEvent(AppointmentEvent event) {
        // Dùng bookingCode làm Partition Key để đảm bảo toàn bộ sự kiện của cùng 1 đơn đặt lịch
        // luôn rơi vào cùng 1 Partition -> Giữ nguyên đúng thứ tự thời gian (FIFO)
        String partitionKey = event.getBookingCode() != null ? event.getBookingCode() : String.valueOf(event.getAppointmentId());

        LOG.info("🚀 [Kafka Producer] Gửi tin nhắn -> Topic: '{}', Key: '{}', Type: '{}'", 
            APPOINTMENT_TOPIC, partitionKey, event.getEventType());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(APPOINTMENT_TOPIC, partitionKey, event);

        // Đăng ký Callback lắng nghe kết quả bất đồng bộ (Non-blocking)
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                LOG.info("✅ [Kafka Producer] Gửi THÀNH CÔNG -> Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                LOG.error("❌ [Kafka Producer] Gửi THẤT BẠI cho mã '{}' do lỗi: {}", partitionKey, ex.getMessage(), ex);
                // Có thể bổ sung cơ chế retry hoặc ghi log báo động khẩn cấp
            }
        });
    }
}
```

---

### 9.5. Lập Trình Kafka Consumer - Lắng Nghe & Xử Lý Sự Kiện

Consumer sử dụng annotation `@KafkaListener` để tự động nhận tin nhắn từ topic:

```java
package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class AppointmentEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    @KafkaListener(
        topics = "opcbs-appointment-events",
        groupId = "clinical-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentEvent(
        @Payload AppointmentEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        LOG.info("📥 [Kafka Consumer] Nhận message từ Topic: 'opcbs-appointment-events' | Partition: {} | Offset: {}", partition, offset);
        LOG.info("👉 Chi tiết sự kiện: Type='{}', Code='{}', Patient='{}', Doctor='{}'",
            event.getEventType(), event.getBookingCode(), event.getPatientName(), event.getDoctorName());

        try {
            switch (event.getEventType()) {
                case "APPOINTMENT_BOOKED" -> {
                    // Nghiệp vụ 1: Bệnh nhân vừa tạo lịch -> Khởi tạo bệnh án lâm sàng tiếp đón sơ bộ
                    LOG.info("📋 [Clinical Service] Đang tạo hồ sơ tiếp đón ban đầu cho ca khám: {}", event.getBookingCode());
                    createPreliminaryIntakeChart(event);
                }
                case "APPOINTMENT_ACCEPTED" -> {
                    // Nghiệp vụ 2: Bác sĩ bấm duyệt lịch -> Mở phòng khám online & gán bác sĩ phụ trách
                    LOG.info("🩺 [Clinical Service] Bác sĩ {} đã tiếp nhận. Sẵn sàng hội chẩn.", event.getDoctorName());
                    assignConsultationRoom(event);
                }
                case "APPOINTMENT_COMPLETED" -> {
                    // Nghiệp vụ 3: Ca khám kết thúc -> Khóa bệnh án và lưu trữ lịch sử điều trị
                    LOG.info("🏁 [Clinical Service] Ca khám {} hoàn tất. Đang đóng bệnh án điện tử.", event.getBookingCode());
                    archiveClinicalRecord(event);
                }
                default -> LOG.warn("⚠️ Không nhận diện được loại sự kiện: {}", event.getEventType());
            }
        } catch (Exception ex) {
            LOG.error("❌ Lỗi khi xử lý sự kiện Kafka: {}", ex.getMessage(), ex);
            // Ném exception để kích hoạt Dead Letter Queue (DLQ) nếu cần thiết
            throw ex;
        }
    }

    private void createPreliminaryIntakeChart(AppointmentEvent event) {
        // Code lưu DB bảng patient_profile hoặc clinical_records
    }

    private void assignConsultationRoom(AppointmentEvent event) {
        // Code cấp phát phòng tư vấn
    }

    private void archiveClinicalRecord(AppointmentEvent event) {
        // Code cập nhật trạng thái hồ sơ bệnh án
    }
}
```

---

### 9.6. Ba Kịch Bản Thực Tế Trong Dự Án Y Tế MindBridge

#### 🏥 Kịch bản 1: Bệnh nhân Đặt Lịch Khám Mới (`APPOINTMENT_BOOKED`)
```mermaid
sequenceDiagram
    autonumber
    actor Patient as 👤 Bệnh Nhân
    participant Booking as 📅 Booking Service (:8082)
    participant Kafka as 📨 Kafka Topic (opcbs-appointment-events)
    participant Clinical as 🩺 Clinical Service (:8083)
    participant Mail as 📧 Mail/Auth Service (:8081)

    Patient->>Booking: POST /api/patient/appointments/book {doctorId, slotId}
    Booking->>Booking: Lưu DB MySQL (Status: PENDING, BookingCode: BK-84920)
    Booking->>Kafka: publishAppointmentEvent(Type: APPOINTMENT_BOOKED)
    Booking-->>Patient: HTTP 201 Created (Phản hồi tức thì trong 20ms!)
    
    par Xử lý ngầm không chặn luồng (Asynchronous)
        Kafka-->>Clinical: Nhận event APPOINTMENT_BOOKED -> Tự động tạo hồ sơ tiếp đón bệnh nhân
        Kafka-->>Mail: Nhận event APPOINTMENT_BOOKED -> Soạn Email & gửi xác nhận mã lịch cho bệnh nhân
    end
```

#### 🩺 Kịch bản 2: Bác sĩ Duyệt Lịch Khám (`APPOINTMENT_ACCEPTED`)
* Khi bác sĩ click nút "Chấp nhận cuộc hẹn":
  1. `opcbs-booking-service` đổi trạng thái thành `CONFIRMED`.
  2. Bắn sự kiện `APPOINTMENT_ACCEPTED` vào Kafka.
  3. `opcbs-clinical-service` kích hoạt tạo link phòng tư vấn trực tuyến (Jitsi/Google Meet).
  4. Hệ thống gửi thông báo đẩy (Push Notification) đến điện thoại bệnh nhân.

#### 💊 Kịch bản 3: Hoàn Tất Khám & Tự Động Trừ Gói Trị Liệu (`APPOINTMENT_COMPLETED`)
* Khi bác sĩ ấn nút "Kết thúc ca tư vấn":
  1. `opcbs-booking-service` bắn sự kiện `APPOINTMENT_COMPLETED`.
  2. `opcbs-treatment-service` (Port 8084) tiêu thụ tin nhắn và tự động trừ `1 buổi` trong `TreatmentPackage` đang kích hoạt của bệnh nhân.
  3. `opcbs-psychometric-service` (Port 8085) tự động gửi bộ trắc nghiệm tâm lý sau buổi khám (Post-session DASS-21) về app cho bệnh nhân tự đánh giá.

---

### 9.7. Bộ Lệnh CLI Thao Tác & Kiểm Thử Kafka Bằng Docker

Khi triển khai trên môi trường phát triển (Dev) hoặc máy chủ (Docker Compose):

#### 1. Khởi chạy cụm Kafka & Zookeeper:
```bash
docker-compose up -d kafka zookeeper
```

#### 2. Kiểm tra danh sách các Topic hiện có:
```bash
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

#### 3. Tạo Topic mới thủ công (nếu chưa có):
```bash
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic opcbs-appointment-events \
  --partitions 3 \
  --replication-factor 1
```

#### 4. Chạy Console Consumer để giám sát tin nhắn thời gian thực:
```bash
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic opcbs-appointment-events \
  --from-beginning \
  --property print.key=true \
  --property print.value=true
```

#### 5. Chạy Console Producer để test bắn thử dữ liệu JSON mẫu:
```bash
docker exec -it kafka kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic opcbs-appointment-events \
  --property "parse.key=true" \
  --property "key.separator=:"
```
*Nhập nội dung test và bấm Enter:*
```json
BK-1001:{"appointmentId":101,"bookingCode":"BK-1001","patientId":1,"patientName":"Nguyễn Văn A","doctorId":2,"doctorName":"BS. Lê Hoàng","eventType":"APPOINTMENT_BOOKED","notes":"Khám tâm lý lo âu"}
```

#### 6. Kiểm tra độ trễ (Lag) của Consumer Group:
```bash
docker exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group clinical-service-group
```
*(Nếu `LAG = 0` tức là toàn bộ tin nhắn đã được Clinical Service xử lý sạch sẽ không bị tồn đọng).*

---

## 10. Giải Thích Mã Nguồn Chi Tiết Các Microservices Mới Triển Khai (Notification, Payment, Message & Treatment Auto-Deduct)

### 10.1. `opcbs-treatment-service` — Cơ Chế Tự Động Trừ Buổi Điều Trị
- **Domain Entity (`TreatmentPackage.java`)**:
  - `totalSessions`: Tổng số buổi điều trị trong gói (ví dụ: 10 buổi).
  - `remainingSessions`: Số buổi còn lại (ví dụ: 9 buổi).
  - `status`: `ACTIVE` (Đang sử dụng), `COMPLETED` (Đã hoàn tất toàn bộ số buổi), `EXPIRED` (Hết hạn).
- **Kafka Consumer (`AppointmentEventConsumer.java`)**:
  ```java
  @KafkaListener(topics = "opcbs-appointment-events", groupId = "treatment-service-group")
  public void handleAppointmentEvent(AppointmentEvent event) {
      if ("APPOINTMENT_BOOKED".equalsIgnoreCase(event.getEventType()) || 
          "APPOINTMENT_COMPLETED".equalsIgnoreCase(event.getEventType())) {
          
          List<TreatmentPackage> activePackages = packageRepository
              .findActivePackagesByPatientAndDoctor(event.getPatientId(), event.getDoctorId());
          
          if (!activePackages.isEmpty()) {
              TreatmentPackage pkg = activePackages.get(0);
              int remaining = pkg.getRemainingSessions() != null ? pkg.getRemainingSessions() - 1 : 0;
              pkg.setRemainingSessions(Math.max(0, remaining));
              
              if (pkg.getRemainingSessions() == 0) {
                  pkg.setStatus("COMPLETED");
              }
              packageRepository.save(pkg);
              LOG.info("🎉 [Treatment Service] Deducted session for package ID: {}, Remaining: {}", pkg.getId(), pkg.getRemainingSessions());
          }
      }
  }
  ```

### 10.2. `opcbs-notification-service` — Hệ Thống Thông Báo Thời Gian Thực
- **Domain Entity (`Notification.java`)**:
  - `userLogin`: Username người nhận (bác sĩ hoặc bệnh nhân).
  - `title`, `message`, `type`: Loại thông báo (`APPOINTMENT_BOOKED`, `TREATMENT_SESSION_USED`, `PSYCHOMETRICS_ASSIGNED`, `PAYMENT_SUCCESS`).
  - `isRead`, `readAt`, `actionUrl`: Đánh dấu trạng thái và điều hướng trang chi tiết.
- **REST Resource (`NotificationResource.java`)**:
  - `GET /api/notifications?unreadOnly=false`: Lấy danh sách thông báo của tài khoản đăng nhập.
  - `GET /api/notifications/unread-count`: Đếm nhanh số thông báo chưa đọc phục vụ Badge trên Navbar.
  - `PUT /api/notifications/{id}/read`: Đánh dấu 1 thông báo là đã đọc.
  - `PUT /api/notifications/read-all`: Đánh dấu tất cả thông báo là đã đọc với 1 query duy nhất.

### 10.3. `opcbs-payment-service` — Gói Nền Tảng & Tích Hợp VNPay Sandbox
- **Domain Entities**:
  - `ServicePackage`: Định nghĩa các gói bác sĩ mua (Standard: 499k, Pro: 999k, VIP: 1.999k).
  - `DoctorSubscription`: Quản lý chu kỳ sử dụng của bác sĩ (`startDate`, `expirationDate`, `status = ACTIVE`).
  - `PaymentTransaction`: Bản ghi giao dịch tài chính (`transactionCode`, `amount`, `paymentMethod = VNPAY`).
- **REST Resource (`PaymentResource.java`)**:
  - `POST /api/doctor/subscriptions/subscribe?packageId=...`: Khởi tạo yêu cầu mua gói, sinh mã giao dịch duy nhất `VNP...`.
  - `POST /api/payments/vnpay/create-url`: Ký chuỗi tham số theo thuật toán mã hóa của VNPay Sandbox và trả về `paymentUrl`.
  - `POST /api/payments/mock-checkout`: Endpoint test nhanh 1-click kích hoạt gói sang `ACTIVE` và cộng thời hạn `durationDays`.

### 10.4. `opcbs-message-service` — Trò Chuyện Trực Tuyến Bác Sĩ - Bệnh Nhân
- **Domain Entities**:
  - `Conversation`: Phiên hội thoại giữa `patientId` và `doctorId`. Lưu tóm tắt `lastMessage`, `lastMessageTime` để tối ưu hóa truy vấn hiển thị danh sách hộp thư.
  - `ChatMessage`: Tin nhắn bất biến (Immutable) chứa `content`, `attachmentUrl`, `senderId`, `senderName`, `senderRole`, `isRead`, `createdDate`.
- **REST Resource (`ConversationResource.java`)**:
  - `GET /api/conversations`: Trả về danh sách các cuộc trò chuyện của user đăng nhập kèm số tin nhắn chưa đọc (`unreadCount`).
  - `GET /api/conversations/{id}/messages`: Trả về danh sách tin nhắn theo thứ tự thời gian tăng dần và tự động đánh dấu đã đọc (`markMessagesAsRead`).
  - `POST /api/conversations/{id}/messages`: Gửi tin nhắn mới và cập nhật tóm tắt của `Conversation`.

---

## 11. Phân Tích Chuyên Sâu Mã Nguồn Deadlock & Luồng Đặt Lịch (Booking Transaction Flow)

### 11.1. Bản Chất Deadlock & Mã Nguồn Mô Phỏng vs Khắc Phục

#### A. Mã nguồn gây Deadlock ([`JavaDeadlockSimulation.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/demo/deadlock/JavaDeadlockSimulation.java))
```java
// Thread 1: Đặt lịch khám
Thread t1 = new Thread(() -> {
    synchronized (lockDoctorSchedule) { // 1. Giữ Lock A
        Thread.sleep(100);             // 2. Chờ để Thread 2 kịp lấy Lock B
        synchronized (lockTreatmentSession) { // 3. Đòi Lock B -> BỊ BLOCKED
            // Thực hiện nghiệp vụ
        }
    }
});

// Thread 2: Trừ buổi điều trị
Thread t2 = new Thread(() -> {
    synchronized (lockTreatmentSession) { // 1. Giữ Lock B
        Thread.sleep(100);               // 2. Chờ để Thread 1 kịp lấy Lock A
        synchronized (lockDoctorSchedule) {   // 3. Đòi Lock A -> BỊ BLOCKED
            // Thực hiện nghiệp vụ
        }
    }
});
```

#### B. Mã nguồn khắc phục hoàn toàn ([`JavaDeadlockFixed.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/demo/deadlock/JavaDeadlockFixed.java))
```java
// Giải pháp 1: Lock Ordering - Cả 2 Thread luôn xin Lock A trước, Lock B sau
synchronized (lockA) {
    synchronized (lockB) {
        // Thực hiện nghiệp vụ an toàn tuyệt đối
    }
}

// Giải pháp 2: ReentrantLock với tryLock(timeout)
ReentrantLock lock1 = new ReentrantLock();
ReentrantLock lock2 = new ReentrantLock();
if (lock1.tryLock(500, TimeUnit.MILLISECONDS)) {
    try {
        if (lock2.tryLock(500, TimeUnit.MILLISECONDS)) {
            try {
                // Xử lý thành công
            } finally { lock2.unlock(); }
        }
    } finally { lock1.unlock(); }
}
```

### 11.2. Phân Tích Mã Nguồn Luồng Đặt Lịch Hẹn ([`AppointmentManagementResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/web/rest/AppointmentManagementResource.java))

```java
@PostMapping("/patient/appointments/book")
@PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_ADMIN')")
@Transactional
public ResponseEntity<AppointmentDTO> bookAppointment(@Valid @RequestBody BookingVM.CreateBookingVM bookingVM) {
    // Bước 1: Khóa và lấy thông tin Slot
    AvailabilitySlot slot = slotRepository.findById(bookingVM.getSlotId())
        .orElseThrow(() -> new BadRequestAlertException("Khung giờ không tồn tại", ENTITY_NAME, "slotnotfound"));

    // Bước 2: Kiểm tra tính khả dụng
    if (slot.getStatus() != SlotStatus.AVAILABLE) {
        throw new BadRequestAlertException("Khung giờ này không còn trống hoặc đã có người đặt", ENTITY_NAME, "slotnotavailable");
    }
    if (slot.getStartTime().isBefore(Instant.now())) {
        throw new BadRequestAlertException("Không thể đặt khung giờ trong quá khứ", ENTITY_NAME, "slotinpast");
    }

    // Bước 3: Đổi trạng thái Slot và Lưu Appointment
    slot.setStatus(SlotStatus.BOOKED);
    slotRepository.save(slot);

    Appointment appointment = new Appointment();
    appointment.setBookingCode("MB-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
    appointment.setStatus(AppointmentStatus.PENDING);
    appointment.setSlot(slot);
    appointment.setDoctor(slot.getDoctor());
    appointment.setPatient(patient);
    Appointment saved = appointmentRepository.save(appointment);

    // Bước 4: Publish Kafka Event sang Treatment & Notification Service
    appointmentEventProducer.sendAppointmentBookedEvent(saved);
    return ResponseEntity.ok(appointmentMapper.toDto(saved));
}
```

---

## 12. Phân Tích Mã Nguồn Transaction & Tính Lũy Đẳng (Idempotency) Trong Payment Gateway (VNPay Sandbox)

### 12.1. Thuật Toán Băm HMAC-SHA512 Xác Thực Chữ Ký ([`VnPayUtil.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-payment-service/src/main/java/com/ocbs/service/util/VnPayUtil.java))
```java
public static String hashAllFields(Map<String, String> fields) {
    List<String> fieldNames = new ArrayList<>(fields.keySet());
    Collections.sort(fieldNames); // 1. Sắp xếp các param theo bảng chữ cái A-Z
    StringBuilder sb = new StringBuilder();
    Iterator<String> itr = fieldNames.iterator();
    while (itr.hasNext()) {
        String fieldName = itr.next();
        String fieldValue = fields.get(fieldName);
        if ((fieldValue != null) && (!fieldValue.isEmpty())) {
            sb.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            if (itr.hasNext()) sb.append("&");
        }
    }
    // 2. Ký chuỗi bằng thuật toán HmacSHA512 với secret key bí mật
    return hmacSHA512(VNP_HASH_SECRET, sb.toString());
}
```

### 12.2. Kiểm Soát Giao Dịch & Idempotency Guard ([`PaymentResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-payment-service/src/main/java/com/ocbs/web/rest/PaymentResource.java))
```java
@GetMapping("/payments/vnpay/callback")
@Transactional(isolation = Isolation.READ_COMMITTED)
public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> allParams) {
    // 1. Chống Fake Webhook: Kiểm tra chữ ký HMAC
    if (!VnPayUtil.verifyCallbackSignature(allParams)) {
        return ResponseEntity.badRequest().body(Map.of("RspCode", "97", "Message", "Invalid Checksum Signature"));
    }

    // 2. Chống Race Condition: Khóa dòng giao dịch với Pessimistic Lock
    PaymentTransaction txn = transactionRepository.findByTransactionCodeWithLock(transactionCode)
        .orElseThrow(() -> new BadRequestAlertException("Transaction not found", ENTITY_NAME, "txnotfound"));
    DoctorSubscription sub = subscriptionRepository.findByTransactionCode(transactionCode)
        .orElseThrow(() -> new BadRequestAlertException("Subscription not found", ENTITY_NAME, "subnotfound"));

    // 3. IDEMPOTENCY GUARD: Nếu giao dịch ĐÃ THÀNH CÔNG, trả về kết quả ngay mà KHÔNG cộng lặp ngày
    if ("SUCCESS".equals(txn.getPaymentStatus())) {
        LOG.warn("⚡ [IDEMPOTENCY GUARD] Giao dịch '{}' đã xử lý THÀNH CÔNG trước đó. Trả về Idempotent response!", transactionCode);
        return ResponseEntity.ok(Map.of(
            "RspCode", "00",
            "Message", "Confirm Success (Idempotent Replay)",
            "isIdempotentReplay", true,
            "status", "ACTIVE",
            "expirationDate", sub.getExpirationDate().toString()
        ));
    }

    // 4. Kích hoạt lần đầu tiên: Cập nhật Transaction và Subscription trong 1 Transaction nguyên tử
    txn.setPaymentStatus("SUCCESS");
    sub.setStatus("ACTIVE");
    sub.setExpirationDate(Instant.now().plus(pkg.getDurationDays(), ChronoUnit.DAYS));

    transactionRepository.save(txn);
    subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("isIdempotentReplay", false, "status", "ACTIVE", ...));
}
```

---

## 13. Phân Tích Mã Nguồn Doctor Dashboard Service Với Redis Cache & C# Model Mapping

### 13.1. So Sánh Kiến Trúc Mô Hình C# Razor vs Spring Boot + Redis Cache
Trong dự án C# gốc (`OPCBS.Web/Pages/Doctor/Dashboard.cshtml.cs`), trang Dashboard thực hiện tính toán trực tiếp trên Entity Framework Core thông qua các câu lệnh LINQ mỗi khi có request:
- `TodaySessionsCount`, `AppointmentsThisMonth`, `ActivePatientsCount`, `ReturningPatientRate`, `RevenueThisMonth`.
- `PendingApprovalsCount`, `PendingNotesCount`, `InProgressAppointmentsCount`.
- Lịch khám hôm nay, lịch sắp tới, biểu đồ tuần 7 ngày.

Trong kiến trúc Spring Boot Microservices (`opcbs-booking-service`), chúng ta nâng cấp mô hình với **Redis Caching Container**:
- Định nghĩa DTO [`DoctorDashboardDTO.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/service/dto/DoctorDashboardDTO.java) ánh xạ 100% các trường của C#.
- Dịch vụ [`DoctorDashboardService.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/service/DoctorDashboardService.java) cài đặt cơ chế Cache-Aside:
  - Cache Key: `doctor:dashboard:{doctorLogin}:{period}`
  - TTL (Time-To-Live): **300 giây (5 phút)**.
  - Telemetry: Trả về `isCacheHit`, `calculationTimeMs`, `cachedAt`.

### 13.2. Mã Nguồn Redis Cache Container ([`DoctorDashboardService.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/service/DoctorDashboardService.java))
```java
@Service
public class DoctorDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorDashboardService.class);
    private static final long CACHE_TTL_SECONDS = 300; // 5 phút

    private final ConcurrentHashMap<String, CacheEntry> redisCacheStore = new ConcurrentHashMap<>();

    public DoctorDashboardDTO getDashboardData(String doctorLogin, String period) {
        long startTime = System.currentTimeMillis();
        String cacheKey = "doctor:dashboard:" + doctorLogin + ":" + period;

        // 1. Kiểm tra cache
        CacheEntry entry = redisCacheStore.get(cacheKey);
        if (entry != null && Instant.now().isBefore(entry.getExpiresAt())) {
            LOG.info("⚡ [REDIS CACHE HIT] Lấy dữ liệu Dashboard cho bác sĩ '{}' (Key: {})", doctorLogin, cacheKey);
            DoctorDashboardDTO cached = entry.getData();
            cached.setIsCacheHit(true);
            cached.setCalculationTimeMs(System.currentTimeMillis() - startTime);
            return cached;
        }

        // 2. Cache Miss: Tính toán từ Database
        LOG.info("🐢 [REDIS CACHE MISS] Tính toán dữ liệu từ DB cho bác sĩ '{}' (Key: {})", doctorLogin, cacheKey);
        DoctorDashboardDTO fresh = calculateFromDatabase(doctorLogin, period);
        fresh.setIsCacheHit(false);
        fresh.setCalculationTimeMs(System.currentTimeMillis() - startTime);
        fresh.setCachedAt(Instant.now());

        // 3. Ghi vào Redis Cache
        redisCacheStore.put(cacheKey, new CacheEntry(fresh, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
        return fresh;
    }

    public void evictDoctorDashboardCache(String doctorLogin) {
        LOG.info("🧹 [REDIS CACHE EVICT] Xóa toàn bộ cache dashboard của bác sĩ '{}'", doctorLogin);
        redisCacheStore.keySet().removeIf(k -> k.startsWith("doctor:dashboard:" + doctorLogin));
    }
}
```

### 13.3. REST Controller Tiếp Nhận API ([`DoctorDashboardResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/web/rest/DoctorDashboardResource.java))
```java
@RestController
@RequestMapping("/api/doctor/dashboard")
public class DoctorDashboardResource {

    private final DoctorDashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DoctorDashboardDTO> getDoctorDashboard(@RequestParam(defaultValue = "today") String period) {
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("doctor");
        return ResponseEntity.ok(dashboardService.getDashboardData(currentLogin, period));
    }

    @PostMapping("/cache/evict")
    public ResponseEntity<Map<String, Object>> evictCache() {
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("doctor");
        dashboardService.evictDoctorDashboardCache(currentLogin);
        return ResponseEntity.ok(Map.of(
            "message", "Cache evicted successfully",
            "doctorLogin", currentLogin,
            "timestamp", Instant.now().toString()
        ));
    }
}
```

### 13.4. Frontend Component Angular 19 ([`doctor-dashboard.component.ts`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/doctor-profile/dashboard/doctor-dashboard.component.ts))
Giao diện Angular 19 được thiết kế chuẩn theo Razor UI của C# với:
1. Header Card: Chào mừng bác sĩ theo thời gian trong ngày (`Chào buổi sáng / chiều / tối`), trạng thái hồ sơ, và nhãn đo lường Redis Cache Telemetry.
2. 5 Thẻ KPI Thống Kê Nhanh: Ca khám hôm nay, Ca tháng này, Bệnh nhân phụ trách, Tỷ lệ quay lại, Doanh thu ước tính.
3. Action Center: Cảnh báo đỏ các ca cần xử lý (Hồ sơ chờ duyệt, Ghi chú chưa hoàn tất, Ca đang tư vấn).
4. Khung Lịch Khám Hôm Nay & Lịch Sắp Tới: Action buttons (Vào khám, Xem bệnh án, Nhận xét).
5. Biểu đồ tuần (CSS dynamic bar chart) & Tỷ lệ phân bổ bệnh nhân (Retention tiers).

---

## 14. Phân Tích Chi Tiết Mã Nguồn Kiến Trúc 2.0 (Phase 13): Chuẩn Hóa Database 3NF, Anti-Spam Tầng DB, Vòng Đời Bảo Lưu & Microservice Blog

### 14.1. Chuẩn Hóa 3NF Database & Ngăn Ngừa Bảng Rác
Để duy trì độ tinh gọn của schema cơ sở dữ liệu và tuân thủ chặt chẽ dạng chuẩn 3 (3NF):
1. **Bảo lưu phác đồ trị liệu (`treatment_package`)**: Không tạo bảng phụ tách rời. Chúng tôi mở rộng trực tiếp bảng `treatment_package` với 11 cột thuộc tính:
   - `is_hold_requested` (BOOLEAN): Cờ đánh dấu bệnh nhân đang gửi đơn xin bảo lưu.
   - `hold_duration_days` (INTEGER): Số ngày đăng ký bảo lưu (1 đến 180 ngày).
   - `hold_start_date` (TIMESTAMP): Thời điểm bắt đầu có hiệu lực bảo lưu.
   - `hold_end_date` (TIMESTAMP): Thời điểm kết thúc bảo lưu dự kiến.
   - `total_hold_days` (INTEGER): Tổng số ngày đã từng bảo lưu lũy kế trong phác đồ.
   - `hold_reason` (VARCHAR): Lý do cá nhân/y tế do người bệnh trình bày.
   - `hold_approved_at` / `hold_approved_by_doctor_id`: Dấu vết bác sĩ phê duyệt.
   - `hold_rejected_at` / `hold_rejection_reason`: Lý do và thời gian bác sĩ từ chối.
2. **Bảng mục tiêu điều trị (`treatment_goal`)**:
   - `id`, `title`, `description`, `priority` (`HIGH`, `MEDIUM`, `LOW`), `status` (`PENDING`, `IN_PROGRESS`, `ACHIEVED`), `achieved_date`, `created_at`.
   - Khóa ngoại `treatment_package_id` trỏ về `treatment_package(id)`.
3. **Bảng buổi tham vấn trị liệu (`treatment_session`)**:
   - `id`, `session_number`, `session_date`, `summary`, `homework`, `mood_before` (1-10), `mood_after` (1-10), `created_at`.
   - Khóa ngoại `treatment_package_id` trỏ về `treatment_package(id)`.
4. **Bảng Blog Y Khoa (`blog_post`, `blog_comment`)**:
   - Chỉ gồm 2 bảng phục vụ trọn vẹn việc quản lý bài viết và bình luận độc giả.

---

### 14.2. Bộ Query JPQL Chống Spam & Giới Hạn Tần Suất Đặt Lịch Tầng DB ([`AppointmentRepository.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/repository/AppointmentRepository.java))

```java
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1. Kiểm tra 1 bệnh nhân chỉ đặt tối đa 1 ca/ngày với cùng 1 bác sĩ
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId " +
           "AND a.appointmentDate >= :startOfDay AND a.appointmentDate <= :endOfDay " +
           "AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    long countActiveAppointmentsOnDate(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId,
                                      @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    // 2. Giới hạn số lượng ca active trong 1 tuần với cùng 1 bác sĩ (Tối đa 3)
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId " +
           "AND a.appointmentDate >= :startOfWeek AND a.appointmentDate <= :endOfWeek " +
           "AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    long countActiveAppointmentsWithDoctorInWeek(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId,
                                                @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek);

    // 3. Giới hạn tổng số lượng ca active trong 1 tuần toàn hệ thống (Tối đa 5)
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.appointmentDate >= :startOfWeek AND a.appointmentDate <= :endOfWeek " +
           "AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    long countTotalActiveAppointmentsInWeek(@Param("patientId") Long patientId,
                                           @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek);

    // 4. Chống trùng khung giờ khám của bác sĩ
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.status NOT IN ('CANCELLED', 'REJECTED') " +
           "AND a.appointmentDate < :endTime AND a.endTime > :startTime " +
           "AND (:excludeAppointmentId IS NULL OR a.id != :excludeAppointmentId)")
    boolean existsOverlappingAppointment(@Param("doctorId") Long doctorId,
                                         @Param("startTime") Instant startTime,
                                         @Param("endTime") Instant endTime,
                                         @Param("excludeAppointmentId") Long excludeAppointmentId);
}
```

Kiểm tra trực tiếp tại `AppointmentManagementResource.java`:
```java
// Rule 1: Giới hạn 1 ca/ngày với cùng 1 bác sĩ
long appointmentsOnDate = appointmentRepository.countActiveAppointmentsOnDate(patientId, doctorId, startOfDay, endOfDay);
if (appointmentsOnDate > 0) {
    throw new BadRequestAlertException(
        "Bạn đã có 1 lịch hẹn đang hoạt động với bác sĩ này trong cùng ngày. Mỗi bệnh nhân chỉ được đặt tối đa 1 lịch/ngày với cùng một bác sĩ.",
        "appointment", "dailyLimitExceeded"
    );
}

// Rule 2: Rate limit tối đa 3 ca/tuần với cùng 1 bác sĩ
long activeAppointmentsWithDoctorInWeek = appointmentRepository.countActiveAppointmentsWithDoctorInWeek(patientId, doctorId, startOfWeek, endOfWeek);
if (activeAppointmentsWithDoctorInWeek >= 3) {
    throw new BadRequestAlertException(
        "Bạn đã đạt giới hạn tối đa 3 lịch hẹn trong tuần này với bác sĩ đã chọn (phòng chống đặt lịch dồn dập).",
        "appointment", "weeklyDoctorLimitExceeded"
    );
}

// Rule 3: Rate limit tối đa 5 ca/tuần trên toàn hệ thống
long totalActiveAppointmentsInWeek = appointmentRepository.countTotalActiveAppointmentsInWeek(patientId, startOfWeek, endOfWeek);
if (totalActiveAppointmentsInWeek >= 5) {
    throw new BadRequestAlertException(
        "Bạn đã đạt giới hạn tối đa 5 lịch hẹn trong tuần trên toàn hệ thống (chính sách chống spam đặt lịch).",
        "appointment", "weeklySystemLimitExceeded"
    );
}

// Rule 4: Chống trùng giờ khám
if (appointmentRepository.existsOverlappingAppointment(doctorId, appointmentDate, endTime, null)) {
    throw new BadRequestAlertException("Khung giờ khám này của bác sĩ đã có lịch hẹn khác. Vui lòng chọn thời gian khác.", "appointment", "timeSlotOverlap");
}
```

---

### 14.3. Nghiệp Vụ Bảo Lưu Gói Trị Liệu & Tự Động Gia Hạn Ngày Hết Hạn ([`TreatmentPackageResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-treatment-service/src/main/java/com/ocbs/web/rest/TreatmentPackageResource.java))

Khi Bác sĩ bấm Duyệt Bảo Lưu (`approveHoldPackage`):
```java
@PostMapping("/treatment-packages/{id}/approve-hold")
public ResponseEntity<TreatmentPackage> approveHoldPackage(@PathVariable Long id) {
    TreatmentPackage pkg = treatmentPackageRepository.findById(id)
        .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy gói điều trị", "treatmentPackage", "idnotfound"));

    if (Boolean.FALSE.equals(pkg.getIsHoldRequested())) {
        throw new BadRequestAlertException("Gói điều trị không có yêu cầu bảo lưu đang chờ duyệt", "treatmentPackage", "noHoldRequest");
    }

    int duration = pkg.getHoldDurationDays() != null ? pkg.getHoldDurationDays() : 30;
    pkg.setIsHoldRequested(false);
    pkg.setStatus(TreatmentStatus.ON_HOLD);
    pkg.setHoldStartDate(Instant.now());
    pkg.setHoldEndDate(Instant.now().plus(duration, ChronoUnit.DAYS));
    pkg.setHoldApprovedAt(Instant.now());
    pkg.setHoldApprovedByDoctorId(pkg.getDoctorId());
    pkg.setTotalHoldDays((pkg.getTotalHoldDays() != null ? pkg.getTotalHoldDays() : 0) + duration);

    // TỰ ĐỘNG GIA HẠN THỜI GIAN HẾT HẠN PHÁC ĐỒ THEO ĐÚNG SỐ NGÀY BẢO LƯU
    if (pkg.getExpirationDate() != null) {
        pkg.setExpirationDate(pkg.getExpirationDate().plus(duration, ChronoUnit.DAYS));
    }

    TreatmentPackage result = treatmentPackageRepository.save(pkg);
    return ResponseEntity.ok(result);
}
```

---

### 14.4. Tính Toán Tiến Độ Lâm Sàng Tầng Database ([`TreatmentPackageResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-treatment-service/src/main/java/com/ocbs/web/rest/TreatmentPackageResource.java))

```java
@GetMapping("/treatment-packages/{id}/progress-summary")
public ResponseEntity<Map<String, Object>> getTreatmentProgressSummary(@PathVariable Long id) {
    long totalGoals = treatmentGoalRepository.countByTreatmentPackageId(id);
    long achievedGoals = treatmentGoalRepository.countByTreatmentPackageIdAndStatus(id, "ACHIEVED");
    long completedSessions = treatmentSessionRepository.countByTreatmentPackageId(id);

    Double avgMoodBefore = treatmentSessionRepository.getAverageMoodBefore(id);
    Double avgMoodAfter = treatmentSessionRepository.getAverageMoodAfter(id);

    double goalCompletionPercent = totalGoals > 0 ? ((double) achievedGoals / totalGoals) * 100.0 : 0.0;
    double moodDelta = (avgMoodBefore != null && avgMoodAfter != null) ? (avgMoodAfter - avgMoodBefore) : 0.0;

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("packageId", id);
    response.put("totalGoals", totalGoals);
    response.put("achievedGoals", achievedGoals);
    response.put("goalCompletionPercent", Math.round(goalCompletionPercent * 10.0) / 10.0);
    response.put("completedSessions", completedSessions);
    response.put("averageMoodBefore", avgMoodBefore != null ? Math.round(avgMoodBefore * 10.0) / 10.0 : null);
    response.put("averageMoodAfter", avgMoodAfter != null ? Math.round(avgMoodAfter * 10.0) / 10.0 : null);
    response.put("moodImprovementDelta", Math.round(moodDelta * 10.0) / 10.0);

    return ResponseEntity.ok(response);
}
```

---

### 14.5. Kiến Trúc Microservice Thứ 9: `opcbs-blog-service` (Port 8089)

Cấu trúc module Spring Boot độc lập:
```
opcbs-blog-service/
├── src/main/java/com/ocbs/
│   ├── config/ (ApplicationProperties, SecurityConfiguration, DatabaseConfiguration)
│   ├── domain/
│   │   ├── BlogPost.java (Entity 3NF chính)
│   │   ├── BlogComment.java (Entity bình luận 3NF)
│   │   └── enumeration/BlogStatus.java (DRAFT, SUBMITTED, APPROVED, PUBLISHED, REJECTED)
│   ├── repository/
│   │   ├── BlogPostRepository.java (Tăng lượt xem ở tầng DB, tìm kiếm từ khóa, lọc bài viết đã duyệt)
│   │   └── BlogCommentRepository.java
│   └── web/rest/
│       ├── PublicBlogResource.java (Đọc bài công khai, tăng view DB, độc giả bình luận)
│       ├── DoctorBlogResource.java (Bác sĩ viết bài, sửa bài, gửi duyệt SUBMITTED)
│       └── AdminBlogResource.java (Admin kiểm duyệt, phê duyệt APPROVED/PUBLISHED, từ chối REJECTED)
├── src/main/resources/
│   ├── config/application.yml (Cổng 8089, Eureka client, PostgreSQL database)
│   └── config/liquibase/
│       └── changelog/20261015160000_added_entity_BlogPost_and_BlogComment.xml
└── pom.xml
```

Tăng lượt xem an toàn ở tầng Database Engine (`BlogPostRepository.java`):
```java
@Modifying
@Query("UPDATE BlogPost b SET b.viewsCount = COALESCE(b.viewsCount, 0) + 1 WHERE b.id = :id")
void incrementViewsCount(@Param("id") Long id);
```

---

### 14.6. Giao Diện Người Dùng Angular 19 Đồng Bộ Chuẩn C# Gốc (`OPCBS.Web`)
1. **Banner Cảnh Báo Bảo Lưu Chuẩn C#**:
   - Class CSS `.clinical-hold-banner`:
     ```css
     background: #fffbeb !important;
     border: 1px solid #fde68a !important;
     color: #92400e !important;
     ```
   - Hiển thị ngày bắt đầu, ngày kết thúc bảo lưu, lý do và thời gian hết hạn mới đã gia hạn.
2. **5 Tab Lâm Sàng Đồng Bộ**:
   - Tab 1: Tổng quan (Thông tin chi tiết phác đồ, số buổi, chi phí).
   - Tab 2: Các buổi tham vấn (Nhật ký phiên trị liệu, điểm tâm trạng trước/sau, bài tập về nhà).
   - Tab 3: Mục tiêu điều trị (Danh mục mục tiêu, tích chọn hoàn thành, cấp độ ưu tiên).
   - Tab 4: Hoạt động & Nhật ký (Theo dõi triệu chứng và tiến trình).
   - Tab 5: Hành trình điều trị (Visual timeline từ lúc tiếp nhận đến khi kết thúc phác đồ).
3. **Module Blog**:
   - `/blogs`: Danh mục bài viết y khoa công khai, tìm kiếm theo tiêu đề/nội dung, lọc chuyên khoa, thẻ tag.
   - `/blogs/:id`: Trang đọc bài viết chuyên môn chi tiết, thông tin bác sĩ tác giả, khung bình luận tương tác.
   - `/doctor/blogs`: Giao diện bác sĩ soạn bài, quản lý bài viết và gửi kiểm duyệt.
   - `/admin/blogs`: Giao diện Admin/Ban Biên Tập kiểm duyệt bài viết y khoa trước khi công bố.

---

### 14.7. Phân Tích Mã Nguồn Thanh Điều Hướng Lâm Sàng (Clinical Main Navbar)

#### 1. Cấu Trúc Scss Y Khoa ([`navbar.scss`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/layouts/navbar/navbar.scss))
- **Dải Phân Cách Y Tế Đỉnh Navbar**:
  ```scss
  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; height: 3px;
    background: linear-gradient(90deg, #0f766e 0%, #0d9488 25%, #0284c7 60%, #10b981 100%);
    z-index: 1031;
  }
  ```
- **Hộp Biểu Tượng Lâm Sàng (`clinical-icon-box`)**:
  Được thiết kế kích thước chuẩn 30x30px với bo góc 7px, nền `#f1f5f9` và màu biểu tượng `#0f766e`. Khi hover, hộp chuyển sang nền ngọc lam nhạt `#ccfbf1` và phóng to nhẹ `scale(1.05)`, kết hợp mũi tên vi mô `item-arrow` trượt ngang tạo hiệu ứng điều hướng mượt mà.
- **Huy Hiệu Nhận Diện Trạng Thái Trực Tuyến (`online-pulse-indicator`)**:
  Chấm tròn xanh ngọc `#10b981` viền trắng 2px định vị ở góc dưới avatar người dùng, biểu trưng cho sự sẵn sàng tiếp nhận và hỗ trợ y tế.

#### 2. Cấu Trúc Template Phân Khu Y Khoa ([`navbar.html`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/layouts/navbar/navbar.html))
- Tái cấu trúc menu dropdown với tiêu đề nhóm chức năng:
  - `dropdown-section-title`: Phân biệt rành mạch giữa nghiệp vụ lâm sàng của Bác sĩ (`bi-stethoscope`), hồ sơ của Bệnh nhân (`bi-heart-pulse`), trung tâm quản trị (`bi-sliders`) và bảo mật tài khoản cá nhân (`bi-shield-lock`).
  - Toàn bộ các icon từ danh mục chính đến các tác vụ nghiệp vụ đều sử dụng Bootstrap Icons chuẩn y khoa.

---

### 14.8. Phân Tích Mã Nguồn Quản Lý Gói Dịch Vụ, Gia Hạn Thông Minh & Lịch Sử Giao Dịch Bác Sĩ

#### 1. Kiến Trúc Backend (`opcbs-payment-service`)
- **Endpoint Lấy Lịch Sử Giao Dịch**:
  - `GET /api/doctor/subscriptions/history`: Truy vấn `DoctorSubscriptionRepository.findAllByDoctorLoginOrderByCreatedDateDesc(login)` để lấy toàn bộ danh sách gói theo thứ tự thời gian mới nhất.
- **Endpoint Gia Hạn Gói Thông Minh (Smart Extension)**:
  - `POST /api/doctor/subscriptions/renew?packageId=...`:
    ```java
    // Nếu gói hiện tại của bác sĩ còn hạn sử dụng, hạn mới được cộng nối tiếp từ hạn cũ
    Instant baseExpiration = (existingSub != null && existingSub.getExpirationDate().isAfter(Instant.now()))
        ? existingSub.getExpirationDate()
        : Instant.now();
    Instant newExpiration = baseExpiration.plus(durationDays, ChronoUnit.DAYS);
    ```
  - Cơ chế này bảo toàn 100% số ngày còn lại của Bác sĩ, đồng thời chuyển trạng thái gói cũ sang `EXTENDED` để ghi nhận chu kỳ liên tục.

#### 2. Kiến Trúc Frontend 3 Tab Chuẩn C# (`doctor-subscription.component.ts/.html/.scss`)
- **Tab 1: Gói Hiện Tại (`STATUS`)**:
  - Tích hợp 4 thẻ Grid Lâm Sàng: Ngày Kích Hoạt (`startDate`), Hạn Đăng Ký (`expirationDate`), Ngày Giao Dịch (`createdDate`), và Thời Gian Còn Lại (`daysRemaining`).
  - Thanh tiến độ sử dụng (`progressPct = usedDays / totalDays * 100`) trực quan hóa phần trăm chu kỳ đã trôi qua.
  - Cảnh báo sắp hết hạn ($\le 7$ ngày) sử dụng màu vàng y tế cảnh báo Bác sĩ gia hạn sớm để tránh gián đoạn khám chữa bệnh.
- **Tab 2: Lịch Sử Giao Dịch (`HISTORY`)**:
  - Thống kê tóm lược tổng lượt đăng ký và tổng chi phí đầu tư.
  - Bộ lọc linh hoạt theo trạng thái: Tất cả, Đang dùng (`ACTIVE`), Đã gia hạn (`EXTENDED`), Đã hết hạn (`EXPIRED`), Chờ thanh toán (`PENDING_PAYMENT`).
  - Bảng tra cứu Desktop & Card Mobile hiển thị mã hợp đồng `#SUB-XXXX`, mã giao dịch VNPay, số tiền và chu kỳ sử dụng.
  - Modal xem chi tiết giao dịch y tế bất biến.
- **Tab 3: Bảng Giá & Đổi Gói Mới (`PLANS`)**:
  - Danh mục các gói hội viên với tính năng phân tầng rõ ràng, kết nối trực tiếp cổng thanh toán trực tuyến VNPay Sandbox.
---

## 15. Phân Tích Chi Tiết Mã Nguồn Chuẩn Hóa Nền Tảng Backend (Ưu tiên 2) & Cải Tiến Toàn Diện Microservices (Ưu tiên 3)

### 15.1. Cơ Chế Fail-Closed Token Blacklist & Redis Eviction Policy (`opcbs-auth-service`)

#### 1. Bài toán kỹ thuật
Trong kiến trúc phân tán dựa trên JWT vô trạng thái (Stateless JWT), việc thu hồi token khi người dùng bấm Đăng Xuất (Logout) dựa trên một danh sách đen (Blacklist) lưu tạm thời trong Redis Cache. Tuy nhiên, nếu Redis gặp sự cố mạng, sập tiến trình hoặc bị quá tải bộ nhớ:
- **Fail-Open**: Nếu hệ thống bỏ qua lỗi Redis và cho phép request đi qua, một token đã bị thu hồi (ví dụ: tài khoản bị xâm nhập và đã bấm đăng xuất trên thiết bị cũ) vẫn tiếp tục được truy cập vào dữ liệu y tế nhạy cảm.
- **Fail-Closed**: Trên môi trường sản xuất (Production), bất kỳ sự cố Redis nào xảy ra trong quá trình kiểm tra thu hồi token đều phải dẫn đến việc **từ chối xác thực ngay lập tức** (`isBlacklisted = true`).

#### 2. Phân tích mã nguồn [`TokenBlacklistService.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-auth-service/src/main/java/com/ocbs/security/TokenBlacklistService.java)
```java
@Service
public class TokenBlacklistService {

    private final Logger log = LoggerFactory.getLogger(TokenBlacklistService.java);
    private static final String BLACKLIST_PREFIX = "auth:blacklist:token:";

    @Value("${application.security.jwt.blacklist-fail-closed:false}")
    private boolean failClosed;

    private final RedisTemplate<String, Object> redisTemplate;
    private final Set<String> localFallbackBlacklist = Collections.synchronizedSet(new HashSet<>());

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            if (redisTemplate != null) {
                String tokenHash = hashToken(token);
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + tokenHash);
                return Boolean.TRUE.equals(hasKey);
            }
        } catch (Exception e) {
            log.warn("Redis error while checking token blacklist: {}", e.getMessage());
            // CHẾ ĐỘ FAIL-CLOSED TRÊN MÔI TRƯỜNG PRODUCTION
            if (failClosed) {
                log.error("Fail-closed mode active: rejecting request due to Redis error");
                return true;
            }
        }

        // Môi trường Dev/Local fallback in-memory an toàn
        return localFallbackBlacklist.contains(hashToken(token));
    }
}
```

- **Băm bảo mật SHA-256 (`hashToken`)**: Thay vì lưu token JWT nguyên bản (chứa header, payload, signature có thể bị lộ qua log Redis), hệ thống băm token thành chuỗi hex SHA-256 độ dài cố định 64 ký tự trước khi gắn prefix `auth:blacklist:token:`.
- **Cấu hình phân tách môi trường**:
  - `application.yml` (Dev): `blacklist-fail-closed: false` $\rightarrow$ Cho phép lập trình viên chạy local kiểm thử khi không có container Redis mà không bị chặn API.
  - `application-prod.yml` (Prod): `blacklist-fail-closed: true` $\rightarrow$ Kích hoạt bức tường bảo vệ nghiêm ngặt chống token replay attack.
- **Chính sách Eviction trong Docker Compose**:
  Trong [`docker-compose.prod.yml`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/docker/docker-compose.prod.yml), Redis được cấu hình với cờ:
  ```yaml
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD} --maxmemory 512mb --maxmemory-policy noeviction
  ```
  Chính sách `noeviction` đảm bảo Redis sẽ từ chối ghi dữ liệu mới khi bộ nhớ đầy thay vì tự ý xóa bớt các key TTL của danh sách token đã thu hồi, bảo vệ hệ thống không bị lộ hổng bảo mật do tự động giải phóng bộ nhớ.

---

### 15.2. Chống Race Condition & Idempotency Key Tại Tầng Booking (`opcbs-booking-service`)

#### 1. Bài toán Race Condition đặt trùng lịch (Double-Booking Hazard)
Khi 2 bệnh nhân cùng lúc bấm nút "Xác nhận đặt khám" cho cùng 1 khung giờ slot (ví dụ: Slot 14:00 - 15:00 của Bác sĩ An):
- Nếu chỉ dùng `SELECT` thông thường, cả 2 Transaction $T_1$ và $T_2$ đều đọc được `is_available = true`.
- Cả 2 đều tạo thành công bản ghi `Appointment`, dẫn đến lỗi nghiêm trọng trong y tế: 2 bệnh nhân đến phòng khám tại cùng một thời điểm.

#### 2. Giải pháp Pessimistic Locking tầng Database
Phương thức trong [`AvailabilitySlotRepository.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/repository/AvailabilitySlotRepository.java):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM AvailabilitySlot s WHERE s.id = :id")
Optional<AvailabilitySlot> findByIdWithLock(@Param("id") Long id);
```

Mã nguồn xử lý tại [`AppointmentManagementResource.bookAppointment`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/web/rest/AppointmentManagementResource.java):
```java
// 1. Áp dụng Khóa Bi Quan trên dòng CSDL của khung giờ slot
AvailabilitySlot slot = availabilitySlotRepository
    .findByIdWithLock(bookingRequest.getSlotId())
    .orElseThrow(() -> new BadRequestAlertException("Khung giờ khám không tồn tại", "appointment", "slotnotfound"));

// 2. Kiểm tra tính khả dụng ngay sau khi chiếm giữ khóa
if (Boolean.FALSE.equals(slot.getIsAvailable())) {
    throw new BadRequestAlertException(
        "Khung giờ khám này vừa được đặt bởi một bệnh nhân khác. Vui lòng chọn khung giờ khác.",
        "appointment",
        "slotunavailable"
    );
}

// 3. Đánh dấu slot đã được đặt và lưu CSDL
slot.setIsAvailable(false);
availabilitySlotRepository.save(slot);
```

```mermaid
sequenceDiagram
    autonumber
    actor BệnhNhânA as 👤 Bệnh Nhân A
    actor BệnhNhânB as 👤 Bệnh Nhân B
    participant Svc as 📅 Booking Service
    participant MySQL as 🗄️ MySQL Database (InnoDB)

    BệnhNhânA->>Svc: POST /api/appointments/book (Slot 10)
    BệnhNhânB->>Svc: POST /api/appointments/book (Slot 10)
    Svc->>MySQL: T1: SELECT * FROM availability_slot WHERE id=10 FOR UPDATE
    Note over MySQL: Khóa Bi Quan (X-Lock) cấp cho T1
    Svc->>MySQL: T2: SELECT * FROM availability_slot WHERE id=10 FOR UPDATE
    Note over MySQL: T2 BỊ BLOCK, phải chờ T1 commit/rollback
    Svc->>MySQL: T1: UPDATE slot SET is_available=false; INSERT appointment; COMMIT
    Note over MySQL: T1 COMMIT thành công. X-Lock giải phóng.
    Note over MySQL: T2 được đánh thức, đọc dòng dữ liệu mới
    MySQL-->>Svc: T2 đọc được is_available = false
    Svc-->>BệnhNhânB: ❌ HTTP 400 Bad Request ("Khung giờ vừa được đặt bởi người khác")
    Svc-->>BệnhNhânA: ✅ HTTP 201 Created (Lịch hẹn thành công)
```

#### 3. Cơ chế Idempotency-Key Header
Khi mạng di động của bệnh nhân chập chờn, nút "Đặt lịch" có thể bị gửi 2 lần liên tiếp:
```java
@PostMapping("/appointments/book")
public ResponseEntity<?> bookAppointment(
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody BookingRequestDTO bookingRequest
) {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        Optional<Appointment> existingOpt = appointmentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existingOpt.isPresent()) {
            log.info("Idempotent request detected for key: {}. Returning existing appointment ID: {}",
                idempotencyKey, existingOpt.get().getId());
            return ResponseEntity.ok(existingOpt.get());
        }
    }
    // Tiến hành tạo lịch hẹn mới và lưu idempotencyKey vào entity Appointment
    ...
}
```

---

### 15.3. Transactional Event Synchronization & An Toàn Lưu Trữ File (`opcbs-booking-service`)

#### 1. Bài toán Dual-Write Hazard trong Microservices
Nếu một service thực hiện:
1. Ghi dữ liệu vào MySQL.
2. Bắn Message vào Kafka Broker.
$\rightarrow$ Nếu bước 2 thành công nhưng sau đó Transaction MySQL bị rollback (do lỗi ràng buộc, timeout, hoặc crash), Kafka Consumer đã nhận được tin nhắn và trừ buổi/gửi email cho một ca khám **không hề tồn tại**!

#### 2. Giải pháp `TransactionSynchronizationManager`
Trong [`AppointmentEventProducer.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/event/AppointmentEventProducer.java):
```java
public void publishAppointmentEvent(AppointmentEvent event) {
    // Đảm bảo event chỉ được phát sau khi CSDL MySQL đã commit transaction thành công
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatchAppointmentEvent(event);
            }
        });
    } else {
        dispatchAppointmentEvent(event);
    }
}
```

#### 3. Chuẩn hóa Metadata Sự Kiện
Mọi sự kiện `AppointmentEvent` đều được chuẩn hóa cấu trúc để phục vụ truy vết phân tán (Distributed Tracing):
```java
event.setEventId(UUID.randomUUID().toString());
event.setEventVersion("1.0");
event.setIdempotencyKey(idempotencyKey);
event.setEventTime(Instant.now());
```

#### 4. Loại bỏ rủi ro lưu file tạm (`FileStorageService`)
Trong [`FileStorageService.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-booking-service/src/main/java/com/ocbs/service/FileStorageService.java), hệ thống loại bỏ hoàn toàn cơ chế fallback ghi file y tế vào thư mục `/tmp`. Nếu biến môi trường persistent upload volume không tồn tại, service sẽ **fail-fast** ngay khi khởi động nhằm ngăn chặn mất mát hồ sơ bệnh án khi container Docker bị khởi động lại.

---

### 15.4. Khắc Phục Triệt Để Lỗ Hổng Rò Rỉ Dữ Liệu Bệnh Án (IDOR) & Audit Log Y Tế (`opcbs-clinical-service`)

#### 1. Phân tích lỗ hổng IDOR cũ
Trong phiên bản cũ của `getMyDoctorPatients()`, khi bác sĩ đang đăng nhập chưa có bản ghi hồ sơ bác sĩ hoàn chỉnh trong database, mã nguồn rơi vào nhánh:
```java
// MÃ CŨ CỰC KỲ NGUY HIỂM:
if (doctorOpt.isEmpty()) {
    return ResponseEntity.ok(patientProfileRepository.findAll()); // LÀM RÒ RỈ TOÀN BỘ BỆNH NHÂN!
}
```
Kẻ tấn công chỉ cần đăng ký một tài khoản không hợp lệ là có thể lấy toàn bộ danh sách bệnh nhân và bệnh án trong hệ thống.

#### 2. Mã nguồn khắc phục triệt để trong [`ClinicalManagementResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-clinical-service/src/main/java/com/ocbs/web/rest/ClinicalManagementResource.java)
```java
@GetMapping("/clinical/my-patients")
public ResponseEntity<List<PatientProfile>> getMyDoctorPatients() {
    String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("");
    boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

    if (isAdmin) {
        return ResponseEntity.ok(patientProfileRepository.findAll());
    }

    Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUserLogin(currentLogin);
    if (doctorOpt.isEmpty()) {
        // TRẢ VỀ DANH SÁCH RỖNG AN TOÀN - TRIỆT TIÊU RÒ RỈ DỮ LIỆU
        log.warn("Doctor profile not found for login: {}. Returning empty patients list.", currentLogin);
        return ResponseEntity.ok(Collections.emptyList());
    }

    List<PatientProfile> patients = clinicalService.findPatientsByDoctorId(doctorOpt.get().getId());
    auditLog("READ_DOCTOR_PATIENTS", doctorOpt.get().getId(), null);
    return ResponseEntity.ok(patients);
}
```

#### 3. Kiểm tra quan hệ điều trị trên `getPatientDetailRecord` và xuất PDF
Bác sĩ chỉ được phép đọc chi tiết bệnh án hoặc xuất PDF khi thỏa mãn ít nhất 1 trong 3 điều kiện:
1. Người gọi là Quản Trị Viên (`ROLE_ADMIN`).
2. Người gọi chính là Bệnh Nhân sở hữu hồ sơ bệnh án đó.
3. Người gọi là Bác Sĩ đã từng tiếp nhận khám hoặc đang điều trị cho bệnh nhân này (`appointmentRepository.existsByDoctorIdAndPatientId(...)`).

#### 4. Tiêu chuẩn Audit Logging Y Tế Không Chứa PHI
```java
private void auditLog(String action, Long doctorId, Long patientId) {
    // CHỈ GHI DẤU VẾT HÀNH ĐỘNG - TUYỆT ĐỐI KHÔNG GHI THÔNG TIN CHẨN ĐOÁN / PHI VÀO LOG
    log.info("[CLINICAL_AUDIT] Action: {} | DoctorId: {} | PatientId: {} | Timestamp: {}",
        action, doctorId, patientId, Instant.now());
}
```

---

### 15.5. Trừ Buổi Trị Liệu Chuẩn Xác Khi Hoàn Tất Ca Khám & Phân Quyền Phác Đồ (`opcbs-treatment-service`)

#### 1. Khắc phục lỗi nghiệp vụ trừ buổi sớm
Trước đây, Consumer lắng nghe `APPOINTMENT_BOOKED` để trừ buổi gói trị liệu. Nếu lịch hẹn bị bác sĩ từ chối hoặc bệnh nhân hủy lịch, số buổi khả dụng của gói đã bị mất oan.
Bản sửa đổi trong [`AppointmentEventConsumer.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-treatment-service/src/main/java/com/ocbs/event/AppointmentEventConsumer.java):
```java
@KafkaListener(topics = "opcbs-appointment-events", groupId = "opcbs-treatment-group",
    autoStartup = "${application.kafka.enabled:false}")
public void handleAppointmentEvent(AppointmentEvent event) {
    if (event == null || event.getEventType() == null) return;

    // CHỈ TRỪ BUỔI KHI CA KHÁM ĐÃ ĐƯỢC BÁC SĨ HOÀN TẤT
    if ("APPOINTMENT_COMPLETED".equalsIgnoreCase(event.getEventType())) {
        processSessionDeduction(event);
    } else {
        log.info("Ignoring event type: {} for session deduction", event.getEventType());
    }
}
```

#### 2. Bảo vệ tính Idempotent chống trừ buổi 2 lần
```java
private synchronized void processSessionDeduction(AppointmentEvent event) {
    Long appointmentId = event.getAppointmentId();
    // Kiểm tra lịch sử trừ buổi dựa trên appointmentId duy nhất
    if (appointmentId != null && treatmentSessionRepository.existsByAppointmentId(appointmentId)) {
        log.warn("Session deduction already processed for appointment ID: {}. Skipping duplicate.", appointmentId);
        return;
    }
    // Thực hiện trừ buổi và tạo bản ghi TreatmentSession tương ứng
    ...
}
```

#### 3. Chống IDOR toàn diện trong [`TreatmentPackageResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-treatment-service/src/main/java/com/ocbs/web/rest/TreatmentPackageResource.java)
Mọi API cập nhật mục tiêu (`goals`), xem tiến độ (`progress-summary`), xin bảo lưu và duyệt bảo lưu đều thông qua 2 hàm kiểm tra quyền:
```java
private void checkPackageAccess(TreatmentPackage pkg) {
    String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("");
    boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    if (isAdmin) return;

    // Bệnh nhân chỉ được xem gói của mình, Bác sĩ chỉ xem gói do mình phụ trách
    boolean isOwner = (pkg.getPatientLogin() != null && pkg.getPatientLogin().equalsIgnoreCase(currentLogin))
                   || (pkg.getDoctorLogin() != null && pkg.getDoctorLogin().equalsIgnoreCase(currentLogin));
    if (!isOwner) {
        throw new AccessDeniedException("Bạn không có quyền truy cập thông tin gói điều trị này");
    }
}
```

---

### 15.6. Phân Quyền Truy Cập & Bảo Mật Kết Quả Trắc Nghiệm Tâm Lý (`opcbs-psychometric-service`)

Trong [`PsychometricResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-psychometric-service/src/main/java/com/ocbs/web/rest/PsychometricResource.java):
1. **Endpoint Chi Tiết Bài Làm (`GET /api/psychometrics/submissions/{id}`)**:
   - Bệnh nhân chỉ xem được bài trắc nghiệm do chính mình thực hiện (`submission.getPatientId() == currentPatientId`).
   - Bác sĩ chỉ xem được bài nếu bài đó do chính bác sĩ chỉ định cho bệnh nhân (`submission.getDoctorId() == currentDoctorId`).
   - Admin có quyền xem để kiểm định chất lượng y tế.
   - Các trường hợp khác: ném `AccessDeniedException("Bạn không có quyền xem kết quả bài trắc nghiệm này")` trả về mã HTTP `403 Forbidden`.
2. **Endpoint Danh Sách Cho Bác Sĩ (`GET /api/psychometrics/doctor/patient-submissions`)**:
   - Thay vì trả về toàn bộ bài test của bệnh nhân trong database, truy vấn được siết chặt:
     ```java
     List<PsychometricSubmission> submissions = submissionRepository
         .findByPatientIdAndDoctorIdOrderBySubmittedAtDesc(patientId, doctorId);
     ```

---

### 15.7. Tách Biệt Kênh & Quản Lý Vòng Đời Trạng Thái Thông Báo (`opcbs-notification-service`)

Entity [`Notification.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-notification-service/src/main/java/com/ocbs/domain/Notification.java) được mở rộng với 3 trường quản lý vòng đời gửi:
```java
@Column(name = "channel", length = 30)
private String channel; // "IN_APP", "EMAIL", "SMS"

@Column(name = "delivery_status", length = 30)
private String deliveryStatus; // "PENDING", "SENT", "FAILED"

@Column(name = "retry_count")
private Integer retryCount = 0;
```
- Thông báo In-App trên giao diện được hiển thị ngay lập tức và quản lý trạng thái đọc (`isRead = false/true`).
- Các kênh gửi ngoài (Email thông báo lịch hẹn) được theo dõi trạng thái `deliveryStatus`, cho phép cơ chế retry khi SMTP gặp sự cố mà không làm mất thông báo trong ứng dụng của người dùng.

---

### 15.8. Xác Thực Chữ Ký HMAC-SHA512, Đối Chiếu Số Tiền & Chặn Mock Checkout Prod (`opcbs-payment-service`)

#### 1. Kiểm tra toàn vẹn callback VNPay trong [`PaymentResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-payment-service/src/main/java/com/ocbs/web/rest/PaymentResource.java)
```java
@GetMapping("/payments/vnpay/callback")
public ResponseEntity<?> vnpayCallback(@RequestParam Map<String, String> queryParams) {
    // 1. Kiểm tra chữ ký băm mật HMAC-SHA512
    String vnp_SecureHash = queryParams.get("vnp_SecureHash");
    boolean isValidSignature = VnPayUtil.verifyCallback(queryParams, vnp_SecureHash, vnpaySecretKey);
    if (!isValidSignature) {
        log.error("VNPay callback signature verification failed!");
        throw new BadRequestAlertException("Chữ ký thanh toán không hợp lệ", "payment", "invalidsignature");
    }

    // 2. Đối chiếu số tiền giao dịch từ VNPay (VNPay gửi amount * 100)
    long vnpAmount = Long.parseLong(queryParams.get("vnp_Amount")) / 100;
    PaymentTransaction txn = findTransactionByTxnRef(queryParams.get("vnp_TxnRef"));
    if (txn.getAmount() == null || txn.getAmount().longValue() != vnpAmount) {
        log.error("Payment amount mismatch! Expected: {}, Received: {}", txn.getAmount(), vnpAmount);
        throw new BadRequestAlertException("Số tiền thanh toán không khớp", "payment", "amountmismatch");
    }

    // 3. Đối chiếu mã định danh Merchant
    String vnp_TmnCode = queryParams.get("vnp_TmnCode");
    if (!vnpayTmnCode.equalsIgnoreCase(vnp_TmnCode)) {
        throw new BadRequestAlertException("Mã merchant không hợp lệ", "payment", "merchantmismatch");
    }

    // 4. Cập nhật trạng thái SUCCESS và gia hạn gói dịch vụ
    ...
}
```

#### 2. Vô hiệu hóa Mock Checkout trên Production
```java
@PostMapping("/payments/mock-checkout")
public ResponseEntity<?> mockCheckout(@RequestParam String txnRef) {
    // Ngăn chặn gian lận kích hoạt gói miễn phí trên Production
    if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
        log.warn("Mock checkout attempted on PRODUCTION environment! Rejected.");
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mock checkout bị vô hiệu hóa trên môi trường production");
    }
    // Xử lý giả lập cho môi trường phát triển dev
    ...
}
```

---

### 15.9. Bộ Lọc HTML Sanitizer Chống Tấn Công XSS & Bảo Vệ Quy Trình Bài Viết (`opcbs-blog-service`)

#### 1. Bộ lọc XSS chuyên sâu [`HtmlSanitizerUtil.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-blog-service/src/main/java/com/ocbs/service/util/HtmlSanitizerUtil.java)
```java
public class HtmlSanitizerUtil {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_PATTERN = Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_PATTERN = Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FORM_PATTERN = Pattern.compile("<form[^>]*>.*?</form>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)\\s*on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JS_PROTOCOL_PATTERN = Pattern.compile("(?i)(href|src)\\s*=\\s*(\"|')?\\s*javascript:", Pattern.CASE_INSENSITIVE);

    public static String sanitizeHtml(String input) {
        if (input == null || input.isBlank()) return input;
        String clean = SCRIPT_PATTERN.matcher(input).replaceAll("");
        clean = IFRAME_PATTERN.matcher(clean).replaceAll("");
        clean = OBJECT_PATTERN.matcher(clean).replaceAll("");
        clean = FORM_PATTERN.matcher(clean).replaceAll("");
        clean = EVENT_HANDLER_PATTERN.matcher(clean).replaceAll("");
        clean = JS_PROTOCOL_PATTERN.matcher(clean).replaceAll("$1=\"#\"");
        return clean;
    }

    public static String stripAllHtml(String input) {
        if (input == null || input.isBlank()) return input;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
```

#### 2. Bảo vệ State Machine bài viết y khoa
- Trong [`DoctorBlogResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-blog-service/src/main/java/com/ocbs/web/rest/DoctorBlogResource.java): Bác sĩ chỉ được phép cập nhật nội dung bài viết khi trạng thái đang là `DRAFT` hoặc `REJECTED`. Khi bài viết đã ở trạng thái `SUBMITTED` (chờ duyệt) hoặc `PUBLISHED` (đang công khai), API từ chối cập nhật trực tiếp nhằm ngăn chặn việc chèn nội dung độc hại sau khi đã được Ban Biên Tập phê duyệt.
- Trong [`AdminBlogResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-blog-service/src/main/java/com/ocbs/web/rest/AdminBlogResource.java): Chỉ những bài viết ở trạng thái `SUBMITTED` mới có thể duyệt sang `APPROVED`/`PUBLISHED`. Khi từ chối bài viết, hệ thống bắt buộc Admin phải nhập `rejectionReason` để phản hồi lâm sàng minh bạch cho Bác sĩ tác giả.

---

### 15.10. Participant Access Control (PAC) & Nguyên Tắc Zero Client Trust (`opcbs-message-service`)

Trong [`ConversationResource.java`](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/web/rest/ConversationResource.java):
1. **Loại bỏ tin tưởng Client (Zero Client Trust)**:
   - Các trường `senderId`, `senderRole`, `patientId` do phía Frontend gửi lên đều bị loại bỏ khỏi luồng định danh.
   - Định danh người gửi luôn được trích xuất an toàn từ JWT Principal:
     ```java
     String currentUser = SecurityUtils.getCurrentUserLogin()
         .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập"));
     ```
2. **Participant Access Control (PAC)**:
   - Trước khi gửi tin nhắn (`POST /api/conversations/{id}/messages`) hoặc đọc danh sách tin nhắn (`GET /api/conversations/{id}/messages`), hệ thống kiểm tra người dùng hiện tại có thuộc về cuộc hội thoại đó hay không:
     ```java
     Conversation conv = conversationRepository.findById(conversationId)
         .orElseThrow(() -> new BadRequestAlertException("Hội thoại không tồn tại", "conversation", "idnotfound"));

     boolean isParticipant = currentUser.equalsIgnoreCase(conv.getPatientLogin())
                          || currentUser.equalsIgnoreCase(conv.getDoctorLogin())
                          || SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
     if (!isParticipant) {
         throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải thành viên của cuộc hội thoại này");
     }
     ```
   - Triệt tiêu hoàn toàn lỗi IDOR rò rỉ tin nhắn tâm sự nhạy cảm giữa bác sĩ tâm lý và bệnh nhân.

