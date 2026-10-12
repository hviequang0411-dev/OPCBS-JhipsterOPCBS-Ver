# 📘 HƯỚNG DẪN CHI TIẾT CẤU HÌNH DỰ ÁN JHIPSTER 6.10.5
> **Dự án**: MindBridge OPCBS (Online Psychological Counseling & Booking System)  
> **Phiên bản Framework**: JHipster 6.10.5 (Spring Boot 2.2.7.RELEASE + Angular 10 + MySQL)

---

## 1. JHIPSTER 6.10.5 LÀ GÌ? DÙNG ĐỂ LÀM GÌ?

### 🔹 JHipster là gì?
**JHipster** (Java + Hipster) là một nền tảng tạo khung dự án (Application Generator) mã nguồn mở hàng đầu thế giới. Nó tự động tạo ra một ứng dụng hoàn chỉnh (Full-stack) bao gồm:
1. **Backend**: Viết bằng **Java Spring Boot** (bảo mật Spring Security, xác thực JWT, ORM Hibernate/JPA, quản lý migration Liquibase, tài liệu Swagger API).
2. **Frontend**: Viết bằng **Angular / TypeScript** (kèm giao diện Bootstrap, Responsive, phân quyền người dùng, quản lý State, Hot-reload).
3. **Database**: Hỗ trợ MySQL, PostgreSQL, Oracle, MongoDB... cùng cơ chế migrate schema tự động.

### 🔹 Tại sao lại dùng JHipster 6.10.5?
- **Tiêu chuẩn giảng dạy & doanh nghiệp**: JHipster 6.x là phiên bản cực kỳ ổn định, tương thích với Java 8/11/17/21 và hệ sinh thái Spring Boot 2.x truyền thống.
- **Tiết kiệm 80% thời gian tạo CRUD**: Thay vì phải tự viết từng Entity, DTO, Mapper, Repository, Service, Rest Controller và các màn hình List/Detail/Create/Edit phía Angular, JHipster tự động sinh ra chuẩn kiến trúc nhiều tầng (N-tier Architecture).
- **Tích hợp sẵn các tính năng Enterprise**: Quản lý tài khoản (User Management), đổi mật khẩu, kích hoạt email, phân quyền `ROLE_ADMIN` / `ROLE_USER`, theo dõi hệ thống qua Health Metrics/Logs.

---

## 2. GIẢI THÍCH CHI TIẾT CÁC FILE CẤU HÌNH CỐT LÕI

Khi mở thư mục dự án `mindbridge-jhipster-6.10.5`, dưới đây là các file quan trọng nhất bạn cần nắm vững:

```
mindbridge-jhipster-6.10.5/
├── .yo-rc.json                 <-- Bản thiết kế kiến trúc toàn dự án (JHipster Blueprint)
├── app.jdl                     <-- Bản vẽ mô hình dữ liệu (10 Entity, quan hệ, DTO)
├── pom.xml                     <-- Quản lý thư viện Java & cấu hình build Backend Maven
├── package.json                <-- Quản lý thư viện Javascript/Angular & scripts chạy Frontend
├── tsconfig.app.json           <-- Cấu hình biên dịch TypeScript Angular
├── webpack/
│   ├── webpack.common.js       <-- Cấu hình đóng gói assets, html, css
│   └── webpack.dev.js          <-- Cấu hình Webpack Dev Server (Port 9000, Proxy sang 8081)
└── src/
    └── main/
        ├── java/com/ocbs/      <-- Mã nguồn Java Backend
        ├── resources/config/
        │   ├── application.yml <-- Cấu hình chung của ứng dụng Spring Boot
        │   └── application-dev.yml <-- Cấu hình môi trường dev (MySQL port, user, pass)
        └── webapp/             <-- Mã nguồn Angular 10 Frontend
```

---

### 📄 File 1: `.yo-rc.json` (Trái tim cấu hình JHipster)
File này lưu lại toàn bộ các lựa chọn khi khởi tạo ứng dụng. Khi chạy lệnh JHipster, nó sẽ đọc file này để biết dự án cấu hình như thế nào:

| Thuộc tính trong `.yo-rc.json` | Giá trị | Ý nghĩa & Mục đích |
| :--- | :--- | :--- |
| `"jhipsterVersion"` | `"6.10.5"` | Khai báo dự án sử dụng đúng chuẩn JHipster 6.10.5. |
| `"applicationType"` | `"monolith"` | Ứng dụng đơn khối (Monolith), gộp cả Backend và Frontend chung 1 repo, dễ triển khai. |
| `"baseName"` | `"opcbs"` | Tên ứng dụng (đặt tên cho database, package Java, application name). |
| `"packageName"` | `"com.ocbs"` | Package gốc của mã nguồn Java (`com.ocbs`). |
| `"serverPort"` | `"8081"` | Cổng chạy server Backend Spring Boot (tránh trùng cổng 8080 mặc định). |
| `"authenticationType"` | `"jwt"` | Xác thực người dùng bằng JSON Web Token (Stateless, an toàn). |
| `"databaseType"` | `"sql"` | Loại cơ sở dữ liệu quan hệ SQL. |
| `"devDatabaseType"` | `"mysql"` | Cơ sở dữ liệu phát triển: MySQL. |
| `"prodDatabaseType"` | `"mysql"` | Cơ sở dữ liệu production: MySQL. |
| `"buildTool"` | `"maven"` | Công cụ build backend bằng Maven (`pom.xml`, `./mvnw`). |
| `"clientFramework"` | `"angularX"` | Framework Frontend sử dụng Angular (Angular 10). |
| `"useSass"` | `true` | Sử dụng cú pháp SCSS/SASS cho styling chuyên nghiệp. |
| `"enableTranslation"` | `false` | Tắt đa ngôn ngữ phức tạp để code gọn gàng, tập trung tính năng. |
| `"entities"` | `[...]` | Danh sách 10 thực thể được tích hợp trong dự án. |

---

### 📄 File 2: `app.jdl` (File định nghĩa cấu trúc dữ liệu JDL)
JDL (JHipster Domain Language) là ngôn ngữ khai báo để mô hình hóa toàn bộ cơ sở dữ liệu:
- **Khai báo Entities**: `Specialization`, `DoctorProfile`, `PatientProfile`, `AvailabilitySlot`, `Appointment`, `TreatmentPackage`, `EmotionJournal`, `PsychometricTest`, `PsychometricQuestion`, `PsychometricSubmission`.
- **Khai báo Quan hệ (Relationships)**:
  - `OneToMany`: 1 Bác sĩ có nhiều Lịch rảnh (`AvailabilitySlot`), nhiều Gói dịch vụ (`TreatmentPackage`).
  - `ManyToOne`: Cuộc hẹn (`Appointment`) gắn với 1 Bệnh nhân và 1 Bác sĩ.
- **Khai báo Tùy chọn Kiến trúc**:
  - `dto * with mapstruct`: Tự động tạo lớp DTO và Mapper để che giấu Entity gốc, tăng cường bảo mật.
  - `service * with serviceImpl`: Tự động tạo Interface Service và Service Implementation (Clean Architecture).
  - `paginate * with pagination`: Tự động phân trang dữ liệu ở cả Backend SQL và Frontend UI.

---

### 📄 File 3: `src/main/resources/config/application-dev.yml`
File cấu hình kết nối database khi chạy ở chế độ phát triển (`dev`):

```yaml
server:
  port: 8081                     # Cổng chạy Backend

spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:mysql://localhost:3306/opcbs?useUnicode=true&characterEncoding=utf8&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC&createDatabaseIfNotExist=true
    username: root               # Username MySQL
    password: 'your_password'    # Mật khẩu MySQL của bạn (VD: 123456)
```

---

### 📄 File 4: `package.json` (Cấu hình Frontend Scripts)
Quản lý các lệnh khởi chạy giao diện:

```json
"scripts": {
  "start": "npm run webpack:dev",
  "webpack:dev": "npm run webpack-dev-server -- --config webpack/webpack.dev.js --inline --hot --port=9000 --watch-content-base --env.stats=minimal",
  "webpack:build": "npm run cleanup && npm run webpack:build:main",
  "webpack-dev-server": "node --openssl-legacy-provider --max_old_space_size=4096 node_modules/webpack-dev-server/bin/webpack-dev-server.js",
  "webpack": "node --openssl-legacy-provider --max_old_space_size=4096 node_modules/webpack/bin/webpack.js"
}
```
> **Lưu ý quan trọng**: Cờ `--openssl-legacy-provider` giúp Webpack 4 tương thích hoàn toàn với các phiên bản Node.js hiện đại (Node 17, 18, 20, 22, 24).

---

## 3. CÁC BƯỚC CẤU HÌNH DỰ ÁN TỪ ĐẦU (STEP-BY-STEP)

Nếu bạn muốn cấu hình lại dự án từ đầu trên một máy mới, hãy thực hiện theo đúng 7 bước sau:

### 🔹 Bước 1: Chuẩn bị môi trường máy tính
Cài đặt các phần mềm cần thiết:
1. **Java JDK** (JDK 11, 17 hoặc 21).
2. **Node.js** (LTS khuyến nghị, hoặc phiên bản bất kỳ).
3. **MySQL Server** (Khởi động dịch vụ MySQL trên cổng `3306`).
4. **JHipster 6.10.5 CLI**:
   ```bash
   npm install -g generator-jhipster@6.10.5
   ```

---

### 🔹 Bước 2: Tạo thư mục và chuẩn bị file `.yo-rc.json` & `app.jdl`
1. Tạo thư mục dự án:
   ```bash
   mkdir mindbridge-jhipster-6.10.5
   cd mindbridge-jhipster-6.10.5
   ```
2. Đặt file `.yo-rc.json` và file `app.jdl` vào thư mục này.

---

### 🔹 Bước 3: Sinh toàn bộ mã nguồn tự động
Chạy lệnh import JDL:
```bash
jhipster import-jdl app.jdl --force --no-insight --skip-install
```
*Lệnh này sẽ tự sinh đầy đủ 100% mã nguồn Java Spring Boot + Angular 10 trong vòng 10 giây.*

---

### 🔹 Bước 4: Tinh chỉnh tương thích phiên bản (Quan trọng)
1. **Sửa `pom.xml` (Hỗ trợ Java hiện đại)**:
   Tìm đến thẻ `<requireJavaVersion>` trong `pom.xml`, sửa từ `[1.8,15)` thành:
   ```xml
   <requireJavaVersion>
       <message>You are running an incompatible version of Java. JHipster supports JDK 8 to 22.</message>
       <version>[1.8,)</version>
   </requireJavaVersion>
   ```

2. **Sửa `package.json` (Hỗ trợ Node.js hiện đại)**:
   Thêm `--openssl-legacy-provider` vào scripts `webpack-dev-server` và `webpack`.

3. **Cài đặt thư viện websocket tương thích**:
   ```bash
   npm install websocket-driver@0.7.4 @types/babel__traverse@7.18.2 --legacy-peer-deps --save-dev
   ```

---

### 🔹 Bước 5: Cấu hình mật khẩu Database
Mở file `src/main/resources/config/application-dev.yml`:
- Điền đúng `password: 'mật_khẩu_mysql_của_bạn'`.
- Database `opcbs` sẽ tự động được tạo khi chạy.

---

### 🔹 Bước 6: Cài đặt Node modules Frontend
Chạy lệnh cài đặt:
```bash
npm install --legacy-peer-deps
```

---

### 🔹 Bước 7: Khởi chạy hệ thống

Mở 2 cửa sổ Terminal (PowerShell):

#### 🖥️ Terminal 1 - Chạy Backend (Spring Boot):
```bash
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\mindbridge-jhipster-6.10.5
.\mvnw.cmd spring-boot:run "-P-webpack,dev" "-DskipTests"
```
*(Backend sẽ khởi động trên cổng **`http://localhost:8081`**)*.

#### 🌐 Terminal 2 - Chạy Frontend (Angular Dev Server):
```bash
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\mindbridge-jhipster-6.10.5
npm start
```
*(Giao diện sẽ chạy trên cổng **`http://localhost:9000`**)*.

---

## 4. BẢNG TỔNG HỢP CÁC LỆNH QUAN TRỌNG (CHEAT SHEET)

| Mục đích | Lệnh thực thi | Ghi chú |
| :--- | :--- | :--- |
| **Chạy Frontend Dev** | `npm start` | Chạy Webpack Dev Server tại `http://localhost:9000` |
| **Đóng gói Frontend** | `npm run webpack:build` | Build static HTML/JS vào `target/classes/static/` |
| **Chạy Backend Dev** | `.\mvnw.cmd spring-boot:run "-P-webpack,dev" "-DskipTests"` | Chạy Backend nhanh, bỏ qua bước build frontend của maven |
| **Kiểm tra Health Backend** | `curl http://localhost:8081/management/health` | Kết quả trả về `{"status":"UP"}` |
| **Xem Swagger API Docs** | Truy cập `http://localhost:8081/v2/api-docs` | Danh sách 43 REST API |
| **Tạo Entity mới bằng JDL** | `jhipster import-jdl file.jdl` | Cập nhật schema & code |

---

## 5. CÁC LỖI THƯỜNG GẶP VÀ CÁCH XỬ LÝ NHANH

### 🔴 1. Lỗi *"An error has occurred :-( ... You started the application from an IDE and you didn't run npm start"*
- **Nguyên nhân**: Mở trình duyệt vào cổng Backend `http://localhost:8081` khi chưa chạy build static assets.
- **Cách xử lý**:
  - Cách 1: Mở trình duyệt vào cổng Frontend chuẩn: **`http://localhost:9000`**.
  - Cách 2: Chạy lệnh `npm run webpack:build`, sau đó mở lại `http://localhost:8081`.

### 🔴 2. Lỗi `ERR_OSSL_EVP_UNSUPPORTED`
- **Nguyên nhân**: Phiên bản Node.js mới (Node 17+) chặn thuật toán mã hóa cũ của Webpack 4.
- **Cách xử lý**: Thêm cờ `--openssl-legacy-provider` vào lệnh chạy node trong `package.json`.

### 🔴 3. Lỗi `RequireJavaVersion: JHipster supports JDK 8 to 14`
- **Nguyên nhân**: Máy tính cài JDK 17 hoặc 21 cao hơn giới hạn mặc định của JHipster 6.
- **Cách xử lý**: Mở `pom.xml`, sửa `<version>[1.8,15)</version>` thành `<version>[1.8,)</version>`.

### 🔴 4. Lỗi `Access denied for user 'root'@'localhost'`
- **Nguyên nhân**: Sai mật khẩu MySQL trong `application-dev.yml`.
- **Cách xử lý**: Sửa dòng `password:` trong `src/main/resources/config/application-dev.yml` trùng với mật khẩu MySQL của máy bạn.

---
*Tài liệu hướng dẫn chuẩn cho dự án OPCBS MindBridge - JHipster 6.10.5.*
