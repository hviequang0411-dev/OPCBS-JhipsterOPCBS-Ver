# Sổ Tay Docker & Container Hóa Hệ Thống MindBridge OPCBS (Docker Master Guide)
## Phương Pháp Luận 3 Bước: WHAT - WHY - HOW & Hướng Dẫn Vận Hành Toàn Tập

> **Dự án**: MindBridge OPCBS - 8 Microservices Y Tế & Chăm Sóc Sức Khỏe Tâm Thần  
> **Kiến trúc**: Granular 8 Microservices + Eureka Server + MySQL 8.4 + Redis 7.4 + Angular 19 SPA + Nginx Gateway Load Balancer  
> **Mục tiêu**: Vận hành ổn định, sẵn sàng chịu tải thực tế cho **1,000+ người dùng hoạt động đồng thời (High-Concurrency Enterprise)**.

---

## 📑 MỤC LỤC
0. [Phần 0: Hướng Dẫn Kiểm Tra & Cài Đặt Docker Trên Máy Tính (Pre-Flight Check)](#phần-0-hướng-dẫn-kiểm-tra--cài-đặt-docker-trên-máy-tính-pre-flight-check)
1. [Phần 1: Bản Chất Docker Theo 3 Bước (WHAT - WHY - HOW)](#phần-1-bản-chất-docker-theo-3-bước-what---why---how)
2. [Phần 2: Sơ Đồ Kiến Trúc Cụm Container Production](#phần-2-sơ-đồ-kiến-trúc-cụm-container-production)
3. [Phần 3: Giải Mã Chi Tiết Các Tệp Cấu Hình & Mã Nguồn](#phần-3-giải-mã-chi-tiết-các-tệp-cấu-hình--mã-nguồn)
4. [Phần 4: Hướng Dẫn Chạy Từng Bước & Lệnh Thao Tác Thực Chiến (A-Z Run Guide)](#phần-4-hướng-dẫn-chạy-từng-bước--lệnh-thao-tác-thực-chiến-a-z-run-guide)
5. [Phần 5: Sao Lưu & Khôi Phục Dữ Liệu Y Tế (Backup & Restore MySQL)](#phần-5-sao-lưu--khôi-phục-dữ-liệu-y-tế-backup--restore-mysql)
6. [Phần 6: Xử Lý Sự Cố Thường Gặp (Troubleshooting)](#phần-6-xử-lý-sự-cố-thường-gặp-troubleshooting)

---

## Phần 0: Hướng Dẫn Kiểm Tra & Cài Đặt Docker Trên Máy Tính (Pre-Flight Check)

Trước khi chạy bất kỳ lệnh nào, máy tính của bạn cần đảm bảo đã cài đặt Docker và Docker Daemon đang hoạt động.

### 0.1. Kiểm Tra Xem Máy Đã Có Docker Chưa
Mở cửa sổ dòng lệnh (**PowerShell** trên Windows hoặc **Terminal** trên macOS/Linux) và gõ:

```powershell
# 1. Kiểm tra phiên bản Docker Engine
docker --version

# 2. Kiểm tra phiên bản Docker Compose (bắt buộc có Compose v2)
docker compose version
```

- **Kết quả tốt (Đã có Docker)**:
  ```text
  Docker version 27.x.x, build ...
  Docker Compose version v2.x.x
  ```
- **Kết quả báo lỗi (Chưa cài hoặc chưa bật)**:
  `docker : The term 'docker' is not recognized...` $\to$ Bạn cần cài đặt theo mục 0.2 bên dưới.

### 0.2. Hướng Dẫn Cài Đặt Docker & WSL 2 (Nếu Máy Chưa Có)
- **Bước 1: Cài đặt WSL 2 (Windows Subsystem for Linux)**:
  Mở **PowerShell với quyền Admin** (chuột phải vào Start $\to$ chọn *Terminal (Admin)* hoặc *PowerShell (Run as Administrator)*) và chạy:
  ```powershell
  # Lệnh cài đặt tự động toàn bộ WSL 2 và nhân Linux
  wsl --install
  ```
  *(Sau khi chạy xong, **khởi động lại máy tính (Restart PC)** để hệ thống kích hoạt tính năng ảo hóa).*

- **Bước 2: Cài đặt Docker Desktop**:
  1. Tải bộ cài chính thức: [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).
  2. Khi cài đặt, chọn tích vào ô: **"Use WSL 2 instead of Hyper-V (recommended)"**.
  3. Mở ứng dụng **Docker Desktop** từ Start Menu. Chờ khoảng 15-30 giây cho biểu tượng **Con cá voi màu xanh lá cây** ở góc phải thanh Taskbar báo **"Docker Desktop is running"**.
- **Trên Linux (Ubuntu/Debian)**:
  ```bash
  curl -fsSL https://get.docker.com -o get-docker.sh
  sudo sh get-docker.sh
  sudo usermod -aG docker $USER
  ```

### 0.3. Kiểm Tra Xem Docker Engine Đang Bật Hay Tắt
Gõ lệnh sau vào terminal:
```powershell
docker info
```
- Nếu hiển thị thông tin chi tiết về *Containers, Images, Server Version...* $\implies$ **Docker đang hoạt động 100%**.
- Nếu báo lỗi: `error during connect: This error may indicate that the docker daemon is not running` $\implies$ Docker Desktop chưa được bật, hãy mở ứng dụng **Docker Desktop** lên và đợi thanh trạng thái chuyển sang màu xanh lá!

### 0.4. Lệnh Kiểm Tra Thử Nghiệm Container Đầu Tiên (Hello World)
```powershell
docker run hello-world
```
- Nếu màn hình in ra dòng chữ: *"Hello from Docker! This message shows that your installation appears to be working correctly."* $\implies$ Bạn đã sẵn sàng 100% để khởi chạy hệ thống MindBridge OPCBS!

### 0.5. Kiểm Tra Xung Đột Cổng Mạng (Port Conflict Pre-Check)
Hệ thống MindBridge OPCBS sử dụng các cổng chính trên máy:
- **Cổng 80**: Nginx Ingress Gateway (Cổng chính tiếp nhận người dùng)
- **Cổng 3306**: MySQL Database
- **Cổng 6379**: Redis Cache
- **Cổng 8761**: Eureka Service Registry

Trước khi bật Docker, hãy kiểm tra xem có ứng dụng nào trên máy bạn đang chiếm cổng 80 hoặc 3306 không (ví dụ: XAMPP, MySQL Server cài sẵn trên Windows, Skype, IIS):
```powershell
# Kiểm tra xem cổng 80 có ai đang dùng không
netstat -ano | findstr :80

# Kiểm tra xem cổng 3306 có ai đang dùng không
netstat -ano | findstr :3306
```
- **Nếu không hiện ra dòng nào**: Tuyệt vời, các cổng đều đang rảnh rỗi!
- **Nếu hiện ra tiến trình đang chiếm cổng 80**: 
  - Thường là do dịch vụ **World Wide Web Publishing Service (IIS)** của Windows. Bạn có thể tạm dừng nó bằng: `Stop-Service W3SVC` (chạy PowerShell Admin).
  - Hoặc nếu đang mở XAMPP / WampServer, hãy tắt Apache và MySQL trong XAMPP đi để nhường cổng cho Docker.

### 0.6. Cấu Hình Giới Hạn RAM Cho Docker Desktop (Khuyên Dùng Cho Máy Windows 8GB-16GB RAM)
Mặc định trên Windows, WSL2 (tiến trình `vmmem`) có thể chiếm tới 50% - 80% RAM máy tính. Để giữ máy luôn mát và mượt mà:
1. Nhấn tổ hợp phím `Windows + R`, gõ `%USERPROFILE%` và nhấn Enter.
2. Tạo (hoặc mở) tệp tin tên là `.wslconfig` và dán nội dung sau:
   ```ini
   [wsl2]
   memory=6GB      # Giới hạn tối đa 6GB RAM cho Docker (thay bằng 4GB nếu máy bạn 8GB RAM)
   processors=4    # Giới hạn số nhân CPU
   swap=2GB        # Bộ nhớ ảo đệm
   ```
3. Mở PowerShell gõ `wsl --shutdown` rồi mở lại Docker Desktop. Máy bạn sẽ chạy cực kỳ êm ái!

---

## Phần 1: Bản Chất Docker Theo 3 Bước (WHAT - WHY - HOW)

### 1.1. WHAT - Docker Là Gì?
- **Ẩn dụ thực tế chiếc thùng container chở hàng**:
  - Trước đây, hàng hải vận chuyển đồ đạc lộn xộn: bao gạo, xe đạp, cá hộp xếp chung, dễ vỡ hỏng và sang cảng khác thì xe tải không vừa.
  - Ngành vận tải phát minh ra **chiếc thùng sắt tiêu chuẩn (Container)**: Bất kể bên trong chứa gì, cứ đóng vào thùng container là cẩu lên tàu biển nào cũng vừa, xe tải nào cũng chở được.
- **Trong lập trình**:
  - Thay vì phải cài đặt thủ công Java 21, MySQL, Redis, Nginx... lên từng máy tính (dễ dẫn đến lỗi kinh điển: *"Ủa trên máy em chạy ngon mà lên server lại lỗi!"*).
  - **Docker là chiếc thùng sắt đóng gói**: Nó nhét toàn bộ **Code Java + Java Runtime 21 + Mọi thư viện phụ thuộc + Cấu hình hệ điều hành** vào MỘT chiếc thùng duy nhất (gọi là **Container**). Chiếc thùng này đem lên máy Windows, Mac hay Linux Server đều chạy y hệt nhau 100%.

### 1.2. WHY - Tại Sao Dự Án 1,000 Người Dùng Bắt Buộc Dùng Docker?
1. **Chữa dứt điểm lỗi môi trường**: Không còn sự khác biệt giữa máy tính của lập trình viên và máy chủ thật ngoài đời.
2. **Cô lập tài nguyên (Isolation)**: Mỗi microservice sống trong một căn phòng riêng. Nếu `Booking Service` bị nghẽn CPU thì chỉ một mình nó chậm, `Auth Service` và `MySQL` vẫn chạy mượt mà, không bao giờ bị hiệu ứng sụp đổ dây chuyền.
3. **Nhân bản thần tốc (Horizontal Scaling)**: Giờ cao điểm có 1,000 bệnh nhân ùa vào đặt lịch, chỉ cần gõ 1 lệnh `docker compose up --scale opcbs-booking-service=3`, trong 5 giây Docker sẽ tạo thêm 2 container chạy song song chia sẻ gánh nặng.
4. **Bảo mật chuẩn y tế (HIPAA)**: Database MySQL và Redis nằm hoàn toàn trong mạng nội bộ riêng (`internal: true`), đóng kín mọi cổng với thế giới bên ngoài, hacker không thể quét cổng hay tấn công trực tiếp vào cơ sở dữ liệu.

### 1.3. HOW - Docker Hoạt Động Như Thế Nào?
Quy trình gói gọn trong 3 bước:
1. **Viết `Dockerfile`**: Như cuốn công thức nấu ăn, chỉ dẫn từng bước cách tải Java, copy file code `.jar` và đặt lệnh khởi chạy.
2. **Viết `docker-compose.prod.yml`**: Như người nhạc trưởng, điều phối cả dàn nhạc 8 services, bật MySQL trước rồi mới bật các backend sau.
3. **Chạy lệnh `docker compose up -d`**: Bật công tắc nguồn, toàn bộ 8 services, database, cache và gateway tự động thức dậy và vận hành trơn tru.

---

## Phần 2: Sơ Đồ Kiến Trúc Cụm Container Production

```
                                  [🌐 INTERNET / 1,000+ USERS]
                                                │
                                    (Cổng 80 HTTP / 443 HTTPS)
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ TIER 1: INGRESS GATEWAY & LOAD BALANCER (Nginx 1.27 Alpine)                             │
│ - worker_connections: 10240, keepalive: 64, Gzip level 6                                │
│ - 🛡️ Rate Limiting: 15 req/s cho Auth API, 50 req/s cho Booking API                     │
└───────────────────────────────────────────┬─────────────────────────────────────────────┘
                                            │ (Network: opcbs-frontend-net)
        ┌───────────────────┬───────────────┴───────────────┬───────────────────┐
        ▼                   ▼                               ▼                   ▼
┌──────────────┐    ┌──────────────┐                ┌──────────────┐    ┌──────────────┐
│ Auth Service │    │Booking Serv. │   ... (8 Svc)  │Message Serv. │    │ Frontend SPA │
│  (Port 8081) │    │ (Port 8082)  │                │ (Port 8088)  │    │ (Angular 19) │
└───────┬──────┘    └───────┬──────┘                └───────┬──────┘    └──────────────┘
        │                   │                               │
        └───────────────────┴───────────────┬───────────────┴───────────────────────────┐
                                            │ (Network: opcbs-backend-net [INTERNAL: TRUE])
                                            ▼
                    ┌───────────────────────────────────────────────┐
                    │ TIER 3: PERSISTENCE, CACHE & DISCOVERY        │
                    │ - MySQL 8.4 LTS (max_connections = 500)       │
                    │ - Redis JWT Blacklist (noeviction, 512MB)     │
                    │ - Redis Dashboard Cache (allkeys-lru, 256MB)  │
                    └───────────────────────────────────────────────┘
```

---

## Phần 3: Giải Mã Chi Tiết Các Tệp Cấu Hình & Mã Nguồn

### 3.1. `Dockerfile` Đa Giai Đoạn (Multi-Stage Build)
Tất cả các dịch vụ Java (Auth, Booking, Clinical, Treatment, Psychometric, Notification, Payment, Message) đều dùng chung kiến trúc Multi-Stage:
- **Giai đoạn 1 (`builder`)**: Dùng `maven:3.9.9-eclipse-temurin-21-alpine`. Cache dependencies qua `mvn dependency:go-offline` trước khi copy source `src`, build ra file `app.jar` bằng profile `-Pprod`.
- **Giai đoạn 2 (`runtime`)**: Dùng `eclipse-temurin:21-jre-alpine` (~150MB). Tạo user không đặc quyền `appuser` (UID 10001) để chạy ứng dụng (chống leo thang quyền root).
- **JVM Ergonomics**:
  ```bash
  -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
  ```
  Giúp JVM tự động căn chỉnh RAM theo Docker Container mà không bị tràn bộ nhớ.

### 3.2. `docker-compose.prod.yml`
File điều phối chứa 13 container phối hợp nhịp nhàng:
- **`mysql`**: MySQL 8.4 LTS, gắn volume bền vững `mysql-data`, file config tinh chỉnh `my.cnf`.
- **`redis`**: Redis 7.4 lưu cache slot rảnh và khóa chống double-booking.
- **`opcbs-eureka-server`**: Định danh và theo dõi heartbeat các microservices (Port 8761).
- **9 Domain Microservices**: Chạy độc lập, tự động restart khi gặp sự cố (`restart: unless-stopped`), có giới hạn CPU và RAM rõ ràng (Auth: 8081, Booking: 8082, Clinical: 8083, Treatment: 8084, Psychometric: 8085, Notification: 8086, Payment: 8087, Message: 8088, Blog: 8089).
- **`opcbs-frontend`**: Angular 19 SPA được build tĩnh và phục vụ qua Nginx siêu tốc.
- **`nginx`**: Đứng ở cổng vào (Port 80/443), định tuyến toàn bộ request theo URL pattern của `proxy.config.mjs`.

### 3.3. `docker/nginx/nginx.conf`
Định tuyến thông minh theo ranh giới nghiệp vụ (Domain-Driven Routing):
- `/api/authenticate`, `/api/register` $\to$ Chuyển tới `opcbs-auth-service:8081` (kèm Rate Limiter 15r/s).
- `/api/doctor-profiles`, `/api/patient/appointments` $\to$ Chuyển tới `opcbs-booking-service:8082`.
- `/api/patient-profiles`, `/api/patient/my-clinical-records` $\to$ Chuyển tới `opcbs-clinical-service:8083`.
- `/api/treatment-packages`, `/api/patient/journals` $\to$ Chuyển tới `opcbs-treatment-service:8084`.
- `/api/psychometrics` $\to$ Chuyển tới `opcbs-psychometric-service:8085`.
- `/api/notifications` $\to$ Chuyển tới `opcbs-notification-service:8086`.
- `/api/payments`, `/api/doctor/subscriptions`, `/api/service-packages` $\to$ Chuyển tới `opcbs-payment-service:8087`.
- `/api/conversations` $\to$ Chuyển tới `opcbs-message-service:8088`.
- `/api/blogs`, `/api/doctor/blogs`, `/api/admin/blogs` $\to$ Chuyển tới `opcbs-blog-service:8089`.
- `/` và các trang SPA $\to$ Chuyển tới `opcbs-frontend:80`.

---

## Phần 4: Hướng Dẫn Chạy Từng Bước & Lệnh Thao Tác Thực Chiến (A-Z Run Guide)

### 4.1. Bước 1: Chuẩn Bị File Môi Trường (.env)
Trước khi khởi chạy, điều hướng terminal vào thư mục `docker/`:
```powershell
cd C:\SEP_G32\SEP_G32\OPCBS-Jhipster\OPCBS-Jhipster\docker
```
Kiểm tra xem đã có file `.env` chưa. Nếu chưa có, copy từ file mẫu:
```powershell
# Trên Windows PowerShell
Copy-Item .env.example .env

# Trên Linux/macOS
cp .env.example .env
```
File `.env` này chứa thông tin mật khẩu database và khóa bí mật JWT.

---

### 4.2. Bước 2: Chọn Chế Độ Chạy Phù Hợp Với Cấu Hình Máy

#### 🎯 Lựa chọn A: Chế Độ Core Services Tinh Gọn (Khuyên dùng cho máy cá nhân 8GB – 16GB RAM)
Nếu bạn đang chạy thử trên laptop cá nhân và muốn hệ thống chạy mượt mà, không nóng máy:
```powershell
  docker compose -f docker-compose.prod.yml up -d mysql redis opcbs-auth-service opcbs-booking-service nginx
```
Lệnh này chỉ khởi chạy: **MySQL + Redis + Auth Service (8081) + Booking Service (8082) + Nginx Gateway (80)**. Tiêu tốn chỉ ~2.5GB – 3GB RAM!

#### 🚀 Lựa chọn B: Chế Độ Full-Stack Toàn Bộ 8 Microservices (Dành cho máy 16GB+ RAM hoặc Cloud VPS)
Để chạy toàn bộ hệ sinh thái (12 containers):
- **Cách tự động (Khuyên dùng)**:
  - Trên Windows: Chạy script `.\deploy.ps1`
  - Trên Linux/macOS: Chạy script `./deploy.sh`
- **Cách thủ công bằng lệnh Compose**:
  ```powershell
  docker compose -f docker-compose.prod.yml up -d --build
  ```

---

### 4.3. Bước 3: Kiểm Tra Xác Nhận Sau Khi Chạy (Verification Probes)

Ngay sau khi gõ lệnh chạy, kiểm tra xem các container đã sẵn sàng hay chưa:

#### 1. Kiểm tra danh sách container và trạng thái sức khỏe:
```powershell
docker compose -f docker-compose.prod.yml ps
```
Quan sát cột **STATUS**:
- `Up (healthy)` $\implies$ **Dịch vụ đã sẵn sàng 100%**.
- `Up (starting)` $\implies$ Dịch vụ đang trong 15-30 giây đầu khởi động Spring Boot. Chờ thêm một chút.
- `Exit (1)` $\implies$ Dịch vụ bị lỗi dừng lại. Hãy xem log ở mục 4.5 bên dưới để xem chi tiết.

#### 2. Kiểm tra cổng mạng Nginx Gateway:
Chạy lệnh curl hoặc mở trình duyệt:
```powershell
curl http://localhost/healthz
```
Nếu nhận được phản hồi: `MindBridge Gateway Healthy` $\implies$ Cổng vào Nginx đã thông suốt!

#### 3. Giám sát tài nguyên CPU & RAM thời gian thực:
```powershell
docker stats
```
Lệnh này sẽ hiển thị bảng điều khiển CPU %, RAM tiêu thụ của từng container trực quan. Nhấn `Ctrl + C` để thoát.

---

### 4.4. Bước 4: Truy Cập & Kiểm Thử Các Chức Năng Trên Trình Duyệt

Mở trình duyệt (Chrome / Edge / Firefox) và kiểm tra:

| Đường Dẫn (URL) | Mục Đích Kiểm Tra | Kết Quả Mong Đợi |
|---|---|---|
| `http://localhost` | Màn hình giao diện Frontend SPA MindBridge | Hiển thị Trang chủ giao diện y tế teal ngọc bích |
| `http://localhost/healthz` | Kiểm tra sức khỏe Nginx Gateway | Trả về `MindBridge Gateway Healthy` |
| `http://localhost/api/authenticate` (POST) | API đăng nhập người dùng | Nhận JWT Token và header `Set-Cookie` |
| `http://localhost/api/doctor-profiles` (GET) | API danh sách bác sĩ | Trả về JSON danh sách bác sĩ chuyên khoa |
| `http://localhost/api/availability-slots` (GET) | API khung giờ khám rảnh | Trả về danh sách slots làm việc |

---

### 4.5. Bước 5: Xem Log Để Giám Sát & Debug
Khi cần xem nhật ký hoạt động bên trong container:
```powershell
# Xem log liên tục của Booking Service (nhấn Ctrl+C để thoát)
docker compose -f docker-compose.prod.yml logs -f opcbs-booking-service

# Xem log liên tục của Auth Service
docker compose -f docker-compose.prod.yml logs -f opcbs-auth-service

# Xem log truy cập của Nginx
docker compose -f docker-compose.prod.yml logs -f nginx

# Xem 50 dòng log gần nhất của MySQL
docker compose -f docker-compose.prod.yml logs --tail=50 mysql
```

---

### 4.6. Bước 6: Cập Nhật Code Không Cần Tắt Hệ Thống (Zero-Downtime Hot Reload)
Giả sử bạn vừa sửa code Java trong `opcbs-booking-service`:
```powershell
docker compose -f docker-compose.prod.yml up -d --no-deps --build opcbs-booking-service
```
Docker sẽ tự động biên dịch lại đúng container đó và thay thế nó trong vài giây mà không làm gián đoạn MySQL hay các dịch vụ khác.

---

### 4.7. Bước 7: Dừng Và Tắt Hệ Thống An Toàn
Khi làm việc xong hoặc muốn tắt máy tính:
```powershell
# Tắt nhẹ nhàng toàn bộ container (Dữ liệu bệnh án trong MySQL vẫn an toàn 100%)
docker compose -f docker-compose.prod.yml down

# Tắt và xóa sạch cả ổ cứng Volume (CẨN THẬN: Chỉ dùng khi muốn reset trắng DB)
docker compose -f docker-compose.prod.yml down -v
```

---

## Phần 5: Sao Lưu & Khôi Phục Dữ Liệu Y Tế (Backup & Restore MySQL)

### 5.1. Sao Lưu Cơ Sở Dữ Liệu (Backup)
Xuất toàn bộ database y tế ra file `.sql` ngay trên máy chủ:
```bash
docker exec opcbs-mysql-prod mysqldump -uopcbs_user -popcbs_secure_password_2026 opcbs > opcbs_backup_$(date +%Y%m%d).sql
```

### 5.2. Khôi Phục Cơ Sở Dữ Liệu (Restore)
Nạp lại dữ liệu từ file backup vào container MySQL:
```bash
cat opcbs_backup_20261013.sql | docker exec -i opcbs-mysql-prod mysql -uopcbs_user -popcbs_secure_password_2026 opcbs
```

---

## Phần 6: Xử Lý Sự Cố Thường Gặp (Troubleshooting)

| Triệu chứng lỗi | Nguyên nhân | Cách khắc phục ngay lập tức |
|---|---|---|
| Container MySQL báo `Unhealthy` | Mật khẩu sai hoặc đang trong 30s khởi tạo InnoDB đầu tiên | Kiểm tra file `.env`, dùng `docker compose logs mysql` xem tiến độ khởi tạo |
| Service báo lỗi `Connection refused to mysql:3306` | Service khởi động trước khi MySQL sẵn sàng | Đã được bảo vệ bằng `depends_on: mysql: condition: service_healthy` |
| Bị lỗi `503 Service Unavailable` khi test login liên tục | Rate Limiting của Nginx kích hoạt để bảo vệ hệ thống | Đây là tính năng bảo vệ bình thường! Chờ 1 giây hoặc điều chỉnh `rate=...` trong `nginx.conf` |
| Hết dung lượng ổ cứng sau vài tuần | Log JVM ghi quá nhiều ra file | Đã được cấu hình `max-size: 50m`, `max-file: 5` tự động xoay vòng log |
| Container báo `Exit code 137` | Bị hết RAM (OOMKilled) do giới hạn quá thấp | Tăng `memory` trong `deploy.resources.limits` của service đó lên thêm 256MB |
| HikariCP: `Failed to validate connection ... No operations allowed after connection closed (shorter maxLifetime)` | MySQL `wait_timeout = 300` đóng kết nối nhàn rỗi trước khi HikariCP kịp recycle (30 phút) | Tăng `--wait_timeout=28800` và `--interactive_timeout=28800` (8 tiếng) trong MySQL command |
| `Table 'opcbs.service_package' doesn't exist` | Các entity mở rộng mới (`service_package`, `doctor_subscription`, `conversation`, ...) nằm trong `create_tables.sql` chưa chạy | Chạy `Get-Content create_tables.sql \| docker exec -i opcbs-mysql-prod mysql ...` và đã mount vào `/docker-entrypoint-initdb.d/` |
