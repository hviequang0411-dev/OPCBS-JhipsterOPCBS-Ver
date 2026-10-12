# HƯỚNG DẪN CẤU HÌNH VÀ CÀI ĐẶT DỰ ÁN TỪ ĐẦU (FROM SCRATCH)
## DỰ ÁN: OPCBS - MINDBRIDGE HEALTHCARE SYSTEM

> 💡 **Xác nhận trạng thái dự án hiện tại**:  
> Dự án **`mindbridge-jhipster`** trên máy của bạn **ĐÃ ĐƯỢC CẤU HÌNH HOÀN CHỈNH 100% VÀ ĐANG CHẠY SẴN SÀNG**:
> - **Backend**: Đang chạy tại `http://localhost:8081` (Spring Boot).
> - **Frontend**: Đang chạy tại `http://localhost:4200` (Angular).
> - **Database**: Đã nạp đầy đủ dữ liệu demo (Bác sĩ, Chuyên khoa, Lịch khám, Bệnh án, Trắc nghiệm tâm lý).
>
> Tài liệu này đóng vai trò là **Sổ tay hướng dẫn thiết lập từ A đến Z** nếu bạn muốn cài đặt lại dự án trên một máy tính mới hoặc tự tạo một dự án tương tự từ đầu.

---

## MỤC LỤC

1. [YÊU CẦU MÔI TRƯỜNG CÀI ĐẶT](#1-yêu-cầu-môi-trường-cài-đặt)
2. [THIẾT LẬP CƠ SỞ DỮ LIỆU MYSQL](#2-thiết-lập-cơ-sở-dữ-liệu-mysql)
3. [QUY TRÌNH KHỞI TẠO DỰ ÁN JHIPSTER TỪ FILE JDL](#3-quy-trình-khởi-tạo-dự-án-jhipster-từ-file-jdl)
4. [HƯỚNG DẪN CẤU HÌNH CÁC FILE YAML (.YML)](#4-hướng-dẫn-cấu-hình-các-file-yaml-yml)
5. [CÀI ĐẶT DEPENDENCIES VÀ BUILD DỰ ÁN](#5-cài-đặt-dependencies-và-build-dự-án)
6. [HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG](#6-hướng-dẫn-khởi-chạy-hệ-thống)
7. [BẢNG MẬT KHẨU & TÀI KHOẢN MẪU](#7-bảng-mật-khẩu--tài-khoản-mẫu)
8. [CÁC LỖI THƯỜNG GẶP VÀ CÁCH XỬ LÝ (TROUBLESHOOTING)](#8-các-lỗi-thường-gặp-và-cách-xử-lý-troubleshooting)

---

## 1. YÊU CẦU MÔI TRƯỜNG CÀI ĐẶT

Trước khi bắt đầu, hãy đảm bảo máy tính đã cài đặt các phần mềm sau:

| Phần Mềm | Phiên Bản Yêu Cầu | Kiểm Tra Bằng Lệnh |
|---|---|---|
| **Java JDK** | Java 21 LTS (hoặc Java 11/17) | `java -version` |
| **Node.js** | Node.js v20+ hoặc v24+ | `node -v` |
| **npm** | npm v10+ hoặc v12+ | `npm -v` |
| **MySQL Server** | MySQL 8.0+ | `mysql --version` |
| **Git** | Bản mới nhất | `git --version` |

---

## 2. THIẾT LẬP CƠ SỞ DỮ LIỆU MYSQL

Mở MySQL Workbench hoặc chạy lệnh trong MySQL Command Line để tạo Database:

```sql
-- 1. Tạo Database opcbs với bảng mã UTF-8
CREATE DATABASE IF NOT EXISTS opcbs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Đảm bảo tài khoản root có toàn quyền
GRANT ALL PRIVILEGES ON opcbs.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. QUY TRÌNH KHỞI TẠO DỰ ÁN JHIPSTER TỪ FILE JDL

Nếu bạn muốn tạo mới một project hoàn chỉnh từ file mô hình dữ liệu **`app.jdl`** hoặc **`mvp-core.jdl`**:

### Bước 3.1: Cài đặt JHipster Generator toàn cục
```bash
npm install -g generator-jhipster
```

### Bước 3.2: Tạo thư mục chứa dự án
```bash
mkdir mindbridge-jhipster
cd mindbridge-jhipster
```

### Bước 3.3: Đặt file `app.jdl` vào thư mục và sinh mã nguồn
Chạy lệnh tự động sinh toàn bộ Backend (Spring Boot) và Frontend (Angular):
```bash
jhipster import-jdl app.jdl
```
*JHipster sẽ tự động sinh Entity, JPA Repository, Service, DTO (MapStruct), REST Controllers, Liquibase migrations và toàn bộ giao diện Angular.*

---

## 4. HƯỚNG DẪN CẤU HÌNH CÁC FILE YAML (.YML)

Mở file `src/main/resources/config/application-dev.yml` và kiểm tra các thông số:

### 4.1. Cấu hình Cổng Backend
```yaml
server:
  port: 8081   # Cổng chạy Backend
```

### 4.2. Cấu hình Kết nối MySQL
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:mysql://localhost:3306/opcbs?useUnicode=true&characterEncoding=utf8&useSSL=false&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true
    username: root
    password: '123456'   # <--- Đổi thành mật khẩu MySQL của bạn
```

### 4.3. Cấu hình CORS cho Angular
```yaml
jhipster:
  cors:
    allowed-origins: 'http://localhost:4200,https://localhost:4200'
    allowed-methods: '*'
    allowed-headers: '*'
    allow-credentials: true
```

---

## 5. CÀI ĐẶT DEPENDENCIES VÀ BUILD DỰ ÁN

Di chuyển vào thư mục dự án `mindbridge-jhipster`:

### Cài đặt thư viện Frontend:
```bash
npm install
```

### Kiểm tra build dự án:
```bash
npm run build
```
*(Đảm bảo kết quả trả về `Output location: target/classes/static` không có lỗi).*

---

## 6. HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG

### Cách 1: Khởi chạy 2 Terminal độc lập (Khuyên Dùng khi Phát Triển)

#### Terminal 1 - Khởi chạy Backend (Spring Boot):
```bash
cd mindbridge-jhipster
./mvnw spring-boot:run
```
*Backend khởi chạy thành công tại: `http://localhost:8081`*

#### Terminal 2 - Khởi chạy Frontend (Angular SPA):
```bash
cd mindbridge-jhipster
npm start
```
*Frontend khởi chạy thành công tại: `http://localhost:4200`*

---

### Cách 2: Khởi chạy Trọn Gói 1 Lệnh Duy Nhất
```bash
cd mindbridge-jhipster
npm run watch
```
*Lệnh này sẽ tự động khởi chạy song song cả Spring Boot Backend và Angular Webpack Server.*

---

## 7. BẢNG MẬT KHẨU & TÀI KHOẢN MẪU

| Nhóm Tài Khoản | Tên Đăng Nhập (Username) | Mật Khẩu Mặc Định | Vai Trò (Role) | Chức Năng |
|---|---|---|---|---|
| **Quản trị viên** | `admin` | `admin` | `ROLE_ADMIN` | Quản trị người dùng, chuyên khoa, metrics |
| **Bác sĩ 1** | `doctor1` | `user` | `ROLE_DOCTOR` | Quản lý lịch rảnh, duyệt ca khám, ghi bệnh án |
| **Bác sĩ 2** | `doctor2` | `user` | `ROLE_DOCTOR` | Tư vấn hôn nhân, chỉ định gói trị liệu |
| **Bác sĩ 3** | `doctor3` | `user` | `ROLE_DOCTOR` | Chuyên gia sang chấn tâm lý PTSD |
| **Bác sĩ 4** | `doctor4` | `user` | `ROLE_DOCTOR` | Bác sĩ chuyên khoa thần kinh & stress |
| **Bệnh nhân 1** | `patient1` | `user` | `ROLE_PATIENT` | Đặt lịch, làm test DASS-21, nhật ký cảm xúc |
| **Bệnh nhân 2** | `patient2` | `user` | `ROLE_PATIENT` | Đặt lịch khám trực tiếp, xem sổ bệnh án |

---

## 8. CÁC LỖI THƯỜNG GẶP VÀ CÁCH XỬ LÝ (TROUBLESHOOTING)

### 🔴 Lỗi 1: `Access denied for user 'root'@'localhost'` (Sai mật khẩu MySQL)
- **Nguyên nhân**: Mật khẩu MySQL trên máy tính khác với file YAML.
- **Cách sửa**: Mở file `src/main/resources/config/application-dev.yml`, tìm dòng `password:` và sửa thành mật khẩu MySQL chính xác của bạn.

### 🔴 Lỗi 2: `Port 8081 is already in use` (Trùng cổng chạy Backend)
- **Cách sửa**: 
  - Mở PowerShell và tìm process đang chiếm cổng:
    ```powershell
    Get-Process -Id (Get-NetTCPConnection -LocalPort 8081).OwningProcess | Stop-Process -Force
    ```
  - Hoặc đổi `server.port: 8082` trong `application-dev.yml`.

### 🔴 Lỗi 3: `Port 4200 is already in use` (Trùng cổng Frontend)
- **Cách sửa**: Chạy `npm start -- --port 4201` để Angular chạy trên cổng khác.

---
*Tài liệu hướng dẫn cấu hình chuẩn cho hệ thống OPCBS MindBridge.*

## 🚀 Các lệnh khởi chạy toàn bộ 8 Microservices + Eureka + Frontend:

```powershell
# 0. Chạy Eureka Server Discovery (Port 8761)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-eureka-server; .\mvnw.cmd spring-boot:run

# 1. Chạy Auth Service (Port 8081)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-auth-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 2. Chạy Booking Service (Port 8082)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-booking-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 3. Chạy Clinical Service (Port 8083)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-clinical-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 4. Chạy Treatment Service (Port 8084)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-treatment-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 5. Chạy Psychometric Service (Port 8085)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-psychometric-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 6. Chạy Notification Service (Port 8086)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-notification-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 7. Chạy Payment Service (Port 8087)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-payment-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 8. Chạy Message Service (Port 8088)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-message-service; .\mvnw.cmd spring-boot:run "-Pdev" "-DskipTests"

# 9. Chạy Frontend Gateway (Port 9000)
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\mindbridge-jhipster; npm start
```

---

## 9. CÁC SCRIPT KIỂM THỬ THỰC HÀNH CHUYÊN SÂU (TESTING & DEMO SCRIPTS)

### 🧪 1. Script Mô Phỏng & Khắc Phục Deadlock JVM:
```powershell
# Chạy mô phỏng Deadlock (Phát hiện Blocked Threads qua ThreadMXBean):
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\opcbs-auth-service
javac -d target/classes src/main/java/com/ocbs/demo/deadlock/JavaDeadlockSimulation.java
java -cp target/classes com.ocbs.demo.deadlock.JavaDeadlockSimulation

# Chạy bản sửa Deadlock (Lock Ordering & ReentrantLock tryLock):
javac -d target/classes src/main/java/com/ocbs/demo/deadlock/JavaDeadlockFixed.java
java -cp target/classes com.ocbs.demo.deadlock.JavaDeadlockFixed
```

### 📅 2. Script Tự Động Hóa Đặt Lịch Hẹn & Kiểm Tra Database:
```powershell
# Chạy demo đặt lịch tự động từ tạo slot rảnh, book lịch, kiểm tra DB và Kafka:
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster
powershell -ExecutionPolicy Bypass -File .\demo_booking.ps1
```

### 💳 3. Script Kiểm Thử Transaction & Tính Lũy Đẳng (Idempotency) Cổng VNPay:
```powershell
# Chạy demo mua gói, tạo URL VNPay HMAC-SHA512 và thử nghiệm Webhook Retry:
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster
powershell -ExecutionPolicy Bypass -File .\demo_payment_idempotency.ps1
```

### 🩺 4. Script Kiểm Thử Hiệu Năng Doctor Dashboard Với Redis Cache:
```powershell
# Chạy demo đo lường Cache Miss vs Cache Hit (0ms), đổi khoảng thời gian và xóa bộ đệm (Cache Evict):
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster
powershell -ExecutionPolicy Bypass -File .\demo_doctor_dashboard_redis.ps1
```