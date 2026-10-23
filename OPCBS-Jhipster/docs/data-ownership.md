# Tài Liệu Phân Định Quyền Sở Hữu Dữ Liệu & Quy Tắc Migration (Data Ownership & Schema Governance)

## 1. Bảng Phân Định Quyền Sở Hữu Nhóm Dữ Liệu

| Nhóm Nghiệp Vụ | Nhóm Bảng CSDL | Service Sở Hữu Độc Quyền (Single Source of Truth) | Quyền Sửa Schema (Migration) | Cách Các Service Khác Truy Cập |
| :--- | :--- | :--- | :--- | :--- |
| **User & Authentication** | `jhi_user`<br>`jhi_authority`<br>`jhi_user_authority` | **`opcbs-auth-service`** (Port 8081) | Chỉ `opcbs-auth-service` | Trích xuất từ JWT Claims; hoặc gọi REST API `/api/users/*`. Không được tự ý `INSERT`/`UPDATE` trực tiếp. |
| **Bác Sĩ & Bệnh Nhân** | `doctor_profile`<br>`patient_profile`<br>`specialization` | **`opcbs-booking-service`** (Port 8082) | Chỉ `opcbs-booking-service` | Gọi REST API `/api/doctors/*`, `/api/patients/*`. |
| **Lịch Rảnh & Đặt Hẹn** | `availability_slot`<br>`appointment` | **`opcbs-booking-service`** (Port 8082) | Chỉ `opcbs-booking-service` | Lắng nghe Appointment Events hoặc gọi REST API. |
| **Lâm Sàng & Bệnh Án** | `clinical_record`<br>Consultation notes | **`opcbs-clinical-service`** (Port 8083) | Chỉ `opcbs-clinical-service` | Gọi REST API `/api/clinical/*`. |
| **Gói Trị Liệu & Cảm Xúc** | `treatment_package`<br>`emotion_journal` | **`opcbs-treatment-service`** (Port 8084) | Chỉ `opcbs-treatment-service` | Gọi REST API `/api/treatment-packages/*`. Trừ số buổi qua Event hoặc API. |
| **Trắc Nghiệm Tâm Lý** | `psychometric_test`<br>`psychometric_question`<br>`psychometric_option`<br>`psychometric_result` | **`opcbs-psychometric-service`** (Port 8085) | Chỉ `opcbs-psychometric-service` | Gọi REST API `/api/psychometrics/*`. |
| **Thông Báo Người Dùng** | `notification` | **`opcbs-notification-service`** (Port 8086) | Chỉ `opcbs-notification-service` | Gửi tin thông báo qua REST API `/api/notifications/send` hoặc Event. |
| **Thanh Toán & Đăng Ký Gói** | `service_package`<br>`doctor_subscription`<br>`payment_transaction` | **`opcbs-payment-service`** (Port 8087) | Chỉ `opcbs-payment-service` | Gọi REST API `/api/doctor/my-subscription`, `/api/payments/*`. |
| **Nhắn Tin Tư Vấn Y Tế** | `conversation`<br>`chat_message` | **`opcbs-message-service`** (Port 8088) | Chỉ `opcbs-message-service` | Gọi REST API `/api/conversations/*`. |
| **Blog & Kiến Thức Sức Khỏe** | `blog_post`<br>`blog_comment`<br>`blog_category` | **`opcbs-blog-service`** (Port 8089) | Chỉ `opcbs-blog-service` | Gọi REST API `/api/blog-posts/*`. |

---

## 2. Ghi Nhận Hiện Trạng Trùng Lặp Schema & Migration

### Các Vấn Đề Tồn Tại Được Ghi Nhận:
1. **Chia sẻ Database Vật Lý (Shared Database)**:
   - Toàn bộ 9 microservices hiện tại đang cùng kết nối vào một database duy nhất tên là `opcbs` trên MySQL container (`3306`).
   - Mặc dù tiện lợi trong giai đoạn chuyển đổi từ Monolith, điều này vi phạm nguyên tắc "Database per Service" của Microservices.
2. **Khai báo Trùng Lặp Entity `User`**:
   - `User.java` được định nghĩa lại ở nhiều service (`opcbs-auth-service`, `opcbs-message-service`, `opcbs-booking-service`, v.v.) và đều ánh xạ vào bảng `jhi_user`.
   - Nguy cơ: Nếu một service thay đổi cấu trúc `jhi_user`, các service khác sẽ gặp lỗi runtime không tương thích.
3. **Liquibase Changelogs Kế Thừa Bị Trùng Lặp**:
   - Các changelog `00000000000000_initial_schema.xml` và `20260928102425_...` từ bản JHipster Monolith ban đầu đang nằm trong thư mục `changelog/` của hầu hết các service.
   - Khi chạy ở môi trường nhiều service, bảng `DATABASECHANGELOG` chung ghi nhận các changeset đã chạy, tránh xung đột thực thi, nhưng gây khó khăn trong việc xác định service nào chịu trách nhiệm cho migration nào.

### Hướng Dẫn Kiến Trúc Định Hướng (Future Roadmap):
- **Giai đoạn tiếp theo (Khi tách Database vật lý)**:
  - Tách mỗi service sử dụng một Schema/Database riêng biệt (ví dụ: `opcbs_auth`, `opcbs_booking`, `opcbs_payment`, `opcbs_message`, v.v.).
  - Loại bỏ hoàn toàn việc truy cập trực tiếp bảng của service khác qua SQL. Mọi truy vấn liên kết phải đi qua REST API hoặc bất đồng bộ qua Message Queue/Kafka.
