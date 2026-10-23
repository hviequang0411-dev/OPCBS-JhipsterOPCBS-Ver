# Quy Tắc Phân Quyền & Kiểm Soát Truy Cập Hội Thoại (Conversation Security Policy)

## 1. Nguyên Tắc Cốt Lõi
Hệ thống nhắn tin y tế (`opcbs-message-service`) bảo vệ tính riêng tư và bí mật y khoa giữa Bác sĩ và Bệnh nhân theo các quy chuẩn bảo mật nghiêm ngặt:
- **Zero Client Trust**: Tuyệt đối không tin tưởng bất kỳ thông tin nhận dạng người gửi (`senderId`, `senderName`, `senderRole`, `patientId`, `doctorId`) do client truyền lên qua Request Body. Toàn bộ danh tính người gửi được trích xuất trực tiếp từ JWT Principal (`SecurityUtils.getCurrentUserLogin()`).
- **Participant Access Control (PAC)**: Mọi thao tác đọc thông tin hội thoại, xem lịch sử tin nhắn, gửi tin nhắn mới và đánh dấu đã đọc đều phải trải qua bước kiểm tra xác thực quyền sở hữu:
  $$\text{Cho phép} \iff \text{User ID} == \text{patientId} \lor \text{User ID} == \text{doctorId} \lor \text{hasRole(ROLE\_ADMIN)}$$
  Nếu không thỏa mãn điều kiện trên, hệ thống ngay lập tức trả về mã trạng thái `403 Forbidden` và ghi nhận cảnh báo bảo mật (`Security Alert`).
- **No Global Fallback Leakage**: Xóa bỏ hoàn toàn cơ chế fallback trả về tất cả cuộc hội thoại (`findAll()`) khi người dùng chưa có cuộc trò chuyện nào. Người dùng mới hoặc người dùng chưa có hội thoại sẽ luôn nhận về danh sách rỗng `[]`.

---

## 2. Ma Trận Quyền Truy Cập API

| Endpoint | HTTP Method | Vai Trò Cho Phép | Quy Tắc Kiểm Soát & Ràng Buộc Danh Tính |
| :--- | :--- | :--- | :--- |
| `/api/conversations` | `GET` | `ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_ADMIN` | - User thông thường chỉ nhận danh sách hội thoại có `patientId == userId` hoặc `doctorId == userId`.<br>- Admin xem toàn bộ danh sách hội thoại.<br>- Không fallback `findAll()` khi danh sách cá nhân rỗng. |
| `/api/conversations/{id}` | `GET` | Thành viên hoặc Admin | - Kiểm tra `userId == conv.patientId` hoặc `userId == conv.doctorId`.<br>- Người ngoài cuộc hội thoại nhận `403 Forbidden`. |
| `/api/conversations/{id}/messages` | `GET` | Thành viên hoặc Admin | - Kiểm tra tư cách thành viên trước khi trả về dữ liệu tin nhắn.<br>- Tự động đánh dấu `isRead = true` cho các tin nhắn được gửi đến `currentUser`. |
| `/api/conversations/{id}/messages` | `POST` | Thành viên hoặc Admin | - Bắt buộc `userId` thuộc hội thoại.<br>- `senderId` gán cứng bằng `currentUser.getId()`.<br>- `senderRole` gán từ authorities trong token (`ROLE_DOCTOR` hoặc `ROLE_PATIENT`).<br>- Client gửi `senderId` hay `role` giả mạo đều bị ghi đè. |
| `/api/conversations/start` | `POST` | Bệnh nhân, Bác sĩ, Admin | - Bác sĩ tạo hội thoại: `doctorId` gán cứng bằng `currentUser.getId()`; `patientId` chọn từ request.<br>- Bệnh nhân tạo hội thoại: `patientId` gán cứng bằng `currentUser.getId()`; `doctorId` chọn từ request.<br>- Admin: Có thể tạo cuộc hội thoại chỉ định cả 2 bên. |
| `/api/conversations/{id}/read` | `PUT` | Thành viên hoặc Admin | - Kiểm tra quyền thành viên trước khi cập nhật trạng thái đã đọc của các tin nhắn đối phương gửi đến. |

---

## 3. Log Giám Sát Cảnh Báo Vi Phạm
Mọi nỗ lực truy cập trái phép được ghi nhận vào Log hệ thống dưới định dạng:
```text
🚨 [Security Alert] User '{login}' (ID: {id}) attempted to access unauthorized conversation #{convId}
```
Giúp đội ngũ vận hành hệ thống dễ dàng audit và phát hiện sớm các hành vi tấn công dò ID (IDOR).
