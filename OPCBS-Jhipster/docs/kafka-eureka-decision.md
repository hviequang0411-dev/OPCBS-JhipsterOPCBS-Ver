# Báo Cáo Quyết Định Kỹ Thuật: Chuẩn Hóa Kafka & Eureka trong Hệ Thống OPCBS

## 1. Quyết Định Về Netflix Eureka Service Discovery

### Hiện Trạng Trước Khi Sửa:
- Dự án có một container `opcbs-eureka-server` (Port 8761) được định nghĩa trong `docker-compose.prod.yml`.
- Tuy nhiên, trong runtime môi trường production, **toàn bộ 9 microservices đều KHÔNG đăng ký vào Eureka** (thiếu biến môi trường `eureka.client.serviceUrl.defaultZone`).
- Toàn bộ lưu lượng được định tuyến thông qua **Nginx Reverse Proxy Gateway** (`docker/nginx/nginx.conf`) sử dụng cơ chế cân bằng tải và phân giải tên miền nội bộ của **Docker Network DNS** (`least_conn; server opcbs-auth-service:8081;`, v.v.).
- Các giao tiếp giữa các service nội bộ (ví dụ `Psychometric` sang `Clinical`) đều sử dụng HTTP URL nội bộ hoặc loopback.

### Quyết Định Kiến Trúc:
- **Loại bỏ container `opcbs-eureka-server` khỏi triển khai Docker Compose Production.**
- **Lý do**:
  1. *Tiết kiệm tài nguyên*: Eureka Server là một ứng dụng Spring Boot tiêu tốn khoảng 300MB - 512MB RAM. Việc chạy một Service Discovery rỗng mà không có client nào tra cứu/đăng ký gây lãng phí tài nguyên nghiêm trọng trên các VPS hoặc server triển khai.
  2. *Độ tin cậy & Đơn giản hóa*: Docker Compose DNS và Nginx upstream đã cung cấp đầy đủ khả năng Service Discovery, Health-check và Load Balancing tĩnh rất ổn định, không phụ thuộc vào trạng thái heartbeat của Eureka.
  3. *Tránh nhầm lẫn kiến trúc*: Việc tồn tại Eureka Server trong compose tạo ra giả định sai lầm rằng hệ thống đang dùng Eureka dynamic discovery.

---

## 2. Quyết Định Về Apache Kafka Event Broker

### Hiện Trạng Trước Khi Sửa:
- Mã nguồn có `AppointmentEventProducer.java` (`opcbs-booking-service`), `AppointmentEventConsumer.java` (`opcbs-treatment-service`), `NotificationEventConsumer.java` (`opcbs-notification-service`).
- Tuy nhiên:
  - Producer trong `opcbs-booking-service` thực chất chỉ ghi log ra console dạng `LOG.info("🚀 [Kafka Producer] Publishing Event to topic...")` mà **hoàn toàn không có `KafkaTemplate.send(...)` thực sự**.
  - Không có container Kafka broker hay Zookeeper/KRaft nào trong Docker Compose hay triển khai production.
  - Các consumer có `@KafkaListener` cố gắng kết nối tới `localhost:9092` mặc định khi khởi động, dẫn đến spam lỗi kết nối (`Connection refused`) liên tục trong log.

### Quyết Định Kiến Trúc:
- **Không tự ý thêm cụm Kafka broker mới vào Docker Compose**:
  - Apache Kafka kèm KRaft/Zookeeper yêu cầu cấu hình bộ nhớ tối thiểu 1.5GB - 2GB RAM, gây quá tải cho các môi trường chạy thử nghiệm hoặc máy chủ có tài nguyên hạn chế.
- **Chuẩn hóa tính trung thực của mã nguồn**:
  1. *Cập nhật `AppointmentEventProducer.java`*:
     - Kiểm tra sự hiện diện của `KafkaTemplate` (`@Autowired(required = false)`).
     - Khi có Kafka broker: Phát sự kiện thực sự qua `kafkaTemplate.send(...)`.
     - Khi không có Kafka broker (Standalone Mode): Ghi nhận rõ ràng là sự kiện In-Memory (`[Event Dispatcher - Standalone Mode]`), tuyệt đối không in log giả vờ rằng đã gửi lên Kafka broker.
  2. *Cập nhật `@KafkaListener` trong các Consumer*:
     - Thêm cờ điều kiện `autoStartup = "${application.kafka.enabled:false}"` vào `@KafkaListener` tại `AppointmentEventConsumer` và `NotificationEventConsumer`.
     - Mặc định không khởi chạy Listener khi broker chưa được bật, ngăn chặn hoàn toàn tình trạng spam exception kết nối.
     - Khi hạ tầng bổ sung Kafka broker trong tương lai, chỉ cần cấu hình `APPLICATION_KAFKA_ENABLED=true` và `SPRING_KAFKA_BOOTSTRAP_SERVERS` để kích hoạt toàn bộ luồng Event-Driven mà không cần sửa code.
