# Báo Cáo Kiến Trúc & Chỉ Đạo Kỹ Thuật (Lead Technical Report) - Phase 13 (2.0)

## 1. Mục Tiêu Trọng Tâm Đã Hoàn Thành
1. **Chuẩn Hóa Database 3NF - Không Bảng Rác**:
   - Tối ưu bảng `treatment_package` với 11 trường bảo lưu (tương đương `TreatmentCase` của C# gốc).
   - Bổ sung đúng 2 bảng chuẩn 3NF: `treatment_goal` và `treatment_session`.
   - Microservice Blog thứ 9 chỉ gồm đúng 2 bảng 3NF: `blog_post` và `blog_comment`.
   - Loại bỏ hoàn toàn in-memory streams, xử lý 100% bằng JPQL và DB queries (`Zero Raw Data`).

2. **Nghiệp Vụ Đặt Lịch & Chống Spam Tầng DB**:
   - Giới hạn 1 bệnh nhân chỉ đặt tối đa 1 ca/ngày với 1 bác sĩ (`countActiveAppointmentsOnDate`).
   - Giới hạn tối đa 3 ca active/tuần với cùng 1 bác sĩ (`countActiveAppointmentsWithDoctorInWeek`).
   - Giới hạn tối đa 5 ca active/tuần trên toàn hệ thống (`countTotalActiveAppointmentsInWeek`).
   - Chống trùng giờ khám (`existsOverlappingAppointment`).
   - Bác sĩ chỉ quản lý lịch hẹn thuộc phạm vi của mình (`Doctor Scoping`).

3. **Vòng Đời Bảo Lưu Gói Điều Trị (Treatment Hold)**:
   - Bệnh nhân xin bảo lưu (1 - 180 ngày).
   - Bác sĩ duyệt bảo lưu: Chuyển sang `ON_HOLD`, tự động gia hạn `expirationDate` theo đúng số ngày bảo lưu.
   - Bác sĩ từ chối bảo lưu kèm lý do tiếng Việt.
   - Hỗ trợ kích hoạt lại phác đồ trước hạn (Resume).

4. **Quản Lý Tiến Độ Lâm Sàng (Clinical Progress)**:
   - Thống kê tỷ lệ % hoàn thành mục tiêu điều trị ở tầng DB.
   - Đo lường và tính toán chỉ số cải thiện tâm trạng trung bình ($\Delta \text{Mood}$) từ thang điểm 1-10.

5. **Microservice Thứ 9: `opcbs-blog-service` (Port 8089)**:
   - Module Spring Boot riêng biệt kèm Liquibase changelog.
   - Cơ chế kiểm duyệt bài viết y khoa: `DRAFT` $\to$ `SUBMITTED` $\to$ `APPROVED`/`PUBLISHED` hoặc `REJECTED`.
   - Tăng lượt xem tầng DB (`viewsCount = viewsCount + 1`), bình luận tương tác.
   - Nginx Reverse Gateway và Docker Compose Production đã đồng bộ cổng 8089.

6. **Giao Diện 100% Tiếng Việt Chuẩn C# Gốc (`OPCBS.Web`)**:
   - Banner bảo lưu màu vàng cam cổ điển `#fffbeb` viền `#fde68a` chữ `#92400e`.
   - Thanh tiến độ Progress Strip sắc nét.
   - Hệ thống 5 Tab lâm sàng: Tổng quan, Các buổi tham vấn, Mục tiêu điều trị, Hoạt động & Nhật ký, Hành trình điều trị.
   - Giao diện Blog hoàn chỉnh: `/blogs`, `/blogs/:id`, `/doctor/blogs`, `/admin/blogs`.
   - Toàn bộ Frontend Angular 19 đã build thành công với Exit code 0!

7. **Nâng Cấp Main Navbar Chuẩn Y Khoa Lâm Sàng (Clinical Healthcare UI)**:
   - Dải màu y tế Deep Clinical Teal `#0f766e` / `#0d9488`, Hospital Slate `#0f172a` và dải accent 3px trên đỉnh.
   - Huy hiệu thương hiệu Brand Emblem nhịp tim ECG `bi-heart-pulse-fill` và phụ đề *Hệ Thống Y Tế Tâm Lý Lâm Sàng*.
   - Bộ biểu tượng chuẩn y khoa: `bi-hospital`, `bi-person-badge-fill`, `bi-clipboard2-pulse-fill`, `bi-journal-medical`, `bi-stethoscope`...
   - Phân khu chức năng Dropdown rành mạch kèm hộp icon y tế `clinical-icon-box` 30x30px và chỉ báo trực tuyến `online-pulse-indicator`.

8. **Tinh Gọn Header Bác Sĩ & Cải Tổ Sidebar Đồng Bộ Thiết Kế Gốc (Phase 13.1)**:
   - Bỏ hoàn toàn dải tab ngang và các nút điều hướng trùng lặp ở thẻ Header phụ của Bác sĩ (`doctor-top-navbar`), giữ Breadcrumb lâm sàng và Live Status Online thoáng đãng.
   - Sidebar clone 100% thiết kế gốc: Kích thước icon chuẩn `1.25rem`, bo góc tròn `10px`, màu sắc phân tầng rực rỡ (Bảng điều khiển active nền Teal `#0d766e` chữ trắng; Bệnh nhân & bệnh án màu Lime nõn chuối `#84cc16`; Lịch hẹn khám màu Emerald `#10b981`; Khung giờ ca khám màu Sky Blue `#0284c7`; Gói trị liệu màu Amber `#f59e0b`; Trắc nghiệm màu Rose `#f43f5e`; Tư vấn trực tuyến màu Cyan `#06b6d4`; Gói hội viên VNPay màu Teal `#0d9488`; Hồ sơ chuyên gia màu Indigo `#6366f1`...).

9. **Quản Lý Gói Dịch Vụ, Thông Tin Chi Tiết & Lịch Sử Giao Dịch Bác Sĩ (Phase 13.2)**:
   - Tái hiện trọn vẹn từ dự án C# gốc (`Pages/Doctor/Subscriptions/Status.cshtml` và `History.cshtml`).
   - Backend `opcbs-payment-service`: Bổ sung endpoint `GET /api/doctor/subscriptions/history` và `POST /api/doctor/subscriptions/renew`.
   - Thuật toán gia hạn thông minh (Smart Extension): Nếu gói cũ còn hạn, hạn mới được cộng nối tiếp từ ngày hết hạn cũ (`newExpiration = oldExpiration + durationDays`), bảo toàn 100% số ngày còn lại của Bác sĩ.
   - Frontend Angular: Kiến trúc 3 Tab điều hướng mượt mà:
     - **Tab 1 - Gói Hiện Tại**: 4 ô Grid lâm sàng (Ngày kích hoạt, Hạn đăng ký, Ngày giao dịch, Thời gian còn lại), thanh tiến độ thời hạn % đã dùng, cảnh báo sắp hết hạn $\le 7$ ngày chuẩn C# (`#fffbeb`, `#92400e`), danh mục đặc quyền y tế, nút Gia hạn gói ngay & Nâng cấp gói.
     - **Tab 2 - Lịch Sử Giao Dịch**: Thống kê 3 chỉ số tổng quan, bộ lọc trạng thái (Tất cả, Đang dùng, Đã gia hạn, Đã hết hạn, Chờ thanh toán), bảng tra cứu Desktop & Card Mobile với mã hợp đồng `#SUB-XXXX`, mã giao dịch VNPay, chu kỳ và modal xem chi tiết.
     - **Tab 3 - Bảng Giá & Đổi Gói**: Thẻ bảng giá phân cấp rõ ràng kết nối cổng thanh toán trực tuyến VNPay Sandbox.

---
Chi tiết mã nguồn và hướng dẫn chuyên sâu vui lòng tham khảo:
- [learn.md](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/learn.md) (Chương 10, mục 10.8)
- [code-explain.md](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/code-explain.md) (Chương 14, mục 14.8)


