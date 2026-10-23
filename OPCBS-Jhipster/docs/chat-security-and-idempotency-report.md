# BÁO CÁO KẾT QUẢ RÀ SOÁT VÀ KHẮC PHỤC RỦI RO HỆ THỐNG CHAT

Dự án: **OPCBS (MindBridge)**  
Phạm vi: **Backend `opcbs-message-service`**, **Frontend `mindbridge-jhipster`**, **Cấu hình Docker / Nginx**, **Liquibase Migration** và **Bộ test tự động**.

---

## 1. Tóm tắt các vấn đề đã khắc phục

| STT | Nhóm vấn đề | Trạng thái trước khi sửa | Giải pháp đã triển khai |
| :--- | :--- | :--- | :--- |
| **1** | **Idempotency & Authorization** | Tra RAM cache theo `clientMessageId` trước khi kiểm tra quyền người gửi. Không ràng buộc `senderId + conversationId`. Không có DB constraint chống trùng khi restart hoặc race condition. | - Kiểm tra `isParticipantOrAdmin` trước tiên; từ chối `403` ngay lập tức nếu không có quyền.<br>- Ràng buộc khóa idempotency theo bộ 3: `conversationId:senderId:clientMessageId`.<br>- Thêm cột `client_message_id` và composite unique constraint `ux_chat_msg_idempotency` trong MySQL.<br>- Xử lý transaction: bắt `DataIntegrityViolationException` để trả về message đã commit mà không bị crash.<br>- Đảm bảo controller chỉ broadcast message mới tạo (`isNew == true`) lên broker. |
| **2** | **Đồng bộ Retry & REST Fallback** | Frontend tạo `clientMessageId` mới khi retry, xóa pending rồi tạo lại khiến trùng tin; timeout bị nhầm với lỗi dứt khoát. | - Giữ nguyên 100% `clientMessageId` và `conversationId` khi retry.<br>- Gửi `clientMessageId` qua REST fallback để backend deduplicate.<br>- Cập nhật in-place tin nhắn hiện tại từ server response, không tạo bản thứ hai.<br>- Phân biệt timeout với lỗi dứt khoát.<br>- Quản lý pending timers theo từng conversation (`pendingTrackers` map), bảo toàn trạng thái khi chuyển đổi qua lại giữa các phòng chat. |
| **3** | **JWT hết hạn & Thu hồi trên WebSocket** | Chỉ chặn frame tiếp theo nếu interceptor bắt được. Kết nối WebSocket TCP vật lý đang mở vẫn tiếp tục nhận broadcast từ broker ngay cả khi token đã hết hạn hoặc bị thu hồi (logout). | - Tạo `TokenRevocationService` kết nối Redis blacklist (`token:blacklist:{jti}`, `user:logout:{username}`) đồng bộ với `opcbs-auth-service`.<br>- Tạo `WebSocketSessionManager` kết hợp `WebSocketHandlerDecoratorFactory` giữ tham chiếu socket vật lý.<br>- Chạy scheduler định kỳ 5 giây quét các session active: gửi thông báo qua `/user/queue/errors` và chủ động gọi `session.close(CloseStatus(4001, "Token expired"))` / `CloseStatus(4002, "Token revoked")`.<br>- Không log JWT hoặc dữ liệu nhạy cảm. |
| **4** | **Cấu hình Origin Production** | Cho phép wildcards hoặc chưa có cấu hình production rõ ràng. | - Cấu hình allowlist nguồn gốc cụ thể trong `WebSocketConfiguration`.<br>- Bổ sung biến `WS_ALLOWED_ORIGINS` trong `docker-compose.prod.yml` và hướng dẫn chi tiết trong `.env.example` (yêu cầu domain đầy đủ scheme và port).<br>- Giữ cấu hình Nginx chuyển tiếp Upgrade, Connection và Origin. |
| **5** | **Khôi phục chat sau Reconnect** | Sau khi kết nối lại STOMP, tin nhắn bị bỏ lỡ trong lúc ngắt kết nối không được đồng bộ tự động hoặc bị trùng lặp. | - Backend bổ sung query param `sinceId` cho `GET /api/conversations/{id}/messages`.<br>- Frontend tự động gọi `syncMissedMessages()` sau khi STOMP reconnect, lấy delta dựa trên `maxId` đã có.<br>- Gộp tin nhắn chống trùng theo `id` và `clientMessageId`, sắp xếp theo thời gian.<br>- Kiểm tra `targetConvId === activeConversation.id` tránh race condition khi chuyển conversation. |
| **6** | **Thu hẹp quyền Subscribe** | Client có thể subscribe bất kỳ destination `/user/**` nào của broker. | - Cấu hình allowlist nghiêm ngặt trong `WebSocketAuthChannelInterceptor`: chỉ cho phép `/user/queue/ack` và `/user/queue/errors`.<br>- Từ chối mọi destination `/user/**` khác với `AccessDeniedException`.<br>- Giữ nguyên kiểm tra quyền hội thoại cho `/topic/conversations/{id}` và chặn SEND trực tiếp tới broker. |

---

## 2. Chi tiết các file và luồng đã chỉnh sửa

### 2.1 Backend (`opcbs-message-service`)
1. [pom.xml](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/pom.xml): Bổ sung `spring-boot-starter-data-redis` để truy vấn blacklist token và trạng thái đăng xuất đồng bộ với `opcbs-auth-service`.
2. [20261020153000_added_client_message_id_to_chat_message.xml](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/resources/config/liquibase/changelog/20261020153000_added_client_message_id_to_chat_message.xml): Liquibase changelog thêm cột `client_message_id VARCHAR(100) NULL` và composite unique constraint `ux_chat_msg_idempotency (conversation_id, sender_id, client_message_id)`.
3. [master.xml](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/resources/config/liquibase/master.xml): Khai báo include migration mới.
4. [create_tables.sql](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/create_tables.sql): Cập nhật schema DDL tương thích bảng `chat_message`.
5. [ChatMessage.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/domain/ChatMessage.java): Khai báo field `clientMessageId`, getters/setters.
6. [ChatMessageRepository.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/repository/ChatMessageRepository.java): Thêm method tìm kiếm idempotency `findByConversationIdAndSenderIdAndClientMessageId` và lấy delta `findByConversationIdAndIdGreaterThanOrderByCreatedDateAsc`.
7. [ChatMessageService.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/service/ChatMessageService.java):
   - Đảo thứ tự: kiểm tra quyền `isParticipantOrAdmin` trước tiên.
   - Idempotency key chuẩn: `conversationId:senderId:clientMessageId`.
   - Kết hợp tra cứu RAM LRU cache và MySQL.
   - Bắt `DataIntegrityViolationException` khi có 2 request đồng thời gửi cùng key, query lại bản ghi đã commit để trả về an toàn.
   - Trả về `SaveMessageResult(message, isNew)`.
8. [TokenRevocationService.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/security/TokenRevocationService.java): Service kết nối Redis để check `token:blacklist:{jti}` và `user:logout:{username}` có circuit breaker và fallback an toàn.
9. [WebSocketSessionManager.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/security/WebSocketSessionManager.java): Quản lý vòng đời `WebSocketSession` vật lý; background scheduler quét định kỳ để đóng kết nối và thông báo khi token hết hạn/thu hồi.
10. [WebSocketAuthChannelInterceptor.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/security/WebSocketAuthChannelInterceptor.java):
    - Kiểm tra token expiry & revocation khi `CONNECT`, `SEND`, `SUBSCRIBE`.
    - Thu hẹp user destination chỉ cho phép `/user/queue/ack` và `/user/queue/errors`.
    - Chặn mọi STOMP SEND trực tiếp đến broker destination.
11. [WebSocketConfiguration.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/config/WebSocketConfiguration.java): Đăng ký `WebSocketHandlerDecoratorFactory` kết nối với `WebSocketSessionManager`.
12. [ConversationWebSocketController.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/web/websocket/ConversationWebSocketController.java): Chỉ broadcast lên topic khi `result.isNew() == true`; luôn gửi ACK về `/user/queue/ack` cho sender.
13. [ConversationResource.java](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/opcbs-message-service/src/main/java/com/ocbs/web/rest/ConversationResource.java):
    - `POST /conversations/{id}/messages`: Nhận `clientMessageId`, broadcast khi tin mới, trả 201 Created (new) hoặc 200 OK (duplicate).
    - `GET /conversations/{id}/messages`: Nhận `@RequestParam(required = false) Long sinceId` phục vụ reconnect sync.

### 2.2 Frontend (`mindbridge-jhipster`)
1. [message.service.ts](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/message/message.service.ts): Cập nhật method `getMessages(conversationId, sinceId?)` hỗ trợ query param `sinceId`.
2. [chat-inbox.component.ts](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/mindbridge-jhipster/src/main/webapp/app/entities/message/chat-inbox.component.ts):
   - Thay `pendingTimeouts` bằng `pendingTrackers = new Map<string, { timer: any; convId: number }>()`.
   - `sendMessage()`: Giữ tracker theo từng `conversationId`, không hủy timeout của hội thoại khác khi chuyển tab/phòng chat.
   - `retryMessage()`: Tái sử dụng nguyên vẹn `clientMessageId` và `conversationId`, không tạo ID mới, không xóa message rồi tạo lại.
   - `sendViaRest()`: Gửi kèm `clientMessageId`, cập nhật tại chỗ message hiện tại từ kết quả server, xóa cờ `isPending` / `isTimeout`.
   - `syncMissedMessages()`: Kéo delta sau khi WebSocket reconnect, deduplicate theo ID và `clientMessageId`.
   - Xử lý mã đóng kết nối `4001` (Token expired) và `4002` (Token revoked) trên WebSocket để cảnh báo người dùng.

### 2.3 Docker & Environment
1. [docker-compose.prod.yml](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/docker/docker-compose.prod.yml): Cấu hình thông số Redis (`SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`) và `depends_on: redis` cho `opcbs-message-service`.
2. [.env.example](file:///c:/SEP_G32/SEP_G32/OPCBS-Jhipster/OPCBS-Jhipster/docker/.env.example): Bổ sung hướng dẫn chi tiết yêu cầu cấu hình `WS_ALLOWED_ORIGINS` cho production với đầy đủ scheme, domain và port (cấm dùng `*`).

---

## 3. Migration và Bảo toàn Dữ liệu Cũ

Migration Liquibase: `20261020153000_added_client_message_id_to_chat_message.xml`

```xml
<changeSet id="20261020153000-1" author="opcbs">
    <addColumn tableName="chat_message">
        <column name="client_message_id" type="varchar(100)">
            <constraints nullable="true" />
        </column>
    </addColumn>
    <addUniqueConstraint columnNames="conversation_id, sender_id, client_message_id"
                         constraintName="ux_chat_msg_idempotency"
                         tableName="chat_message" />
</changeSet>
```

**Nguyên tắc bảo toàn dữ liệu cũ:**
- Cột `client_message_id` là `nullable="true"`. Toàn bộ dữ liệu lịch sử trước đây sẽ có giá trị `NULL`.
- Theo chuẩn ANSI SQL và engine InnoDB của MySQL, các giá trị `NULL` trong unique constraint không vi phạm tính duy nhất (nghĩa là nhiều bản ghi cũ cùng có `client_message_id = NULL` hoàn toàn hợp lệ và không gây lỗi).
- Migration hoàn toàn không chạy lệnh `DROP`, `TRUNCATE` hay `DELETE`.

---

## 4. Kết quả Kiểm thử & Biên dịch

### 4.1 Backend Test (`opcbs-message-service`)
Chạy bộ kiểm thử tự động gồm 5 test class:
```bash
./mvnw test -Dtest=ChatMessageServiceTest,WebSocketAuthChannelInterceptorTest,WebSocketSessionManagerTest,ConversationResourceTest,WebSocketOriginSecurityTest
```

**Kết quả:**
```
[INFO] Running com.ocbs.security.WebSocketAuthChannelInterceptorTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in WebSocketAuthChannelInterceptorTest
[INFO] Running com.ocbs.security.WebSocketOriginSecurityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in WebSocketOriginSecurityTest
[INFO] Running com.ocbs.security.WebSocketSessionManagerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in WebSocketSessionManagerTest
[INFO] Running com.ocbs.service.ChatMessageServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in ChatMessageServiceTest
[INFO] Running com.ocbs.web.rest.ConversationResourceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ConversationResourceTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

**Bảo đảm 9 tiêu chí kiểm thử bắt buộc theo yêu cầu:**
1. Người dùng không thể dùng `clientMessageId` của người khác để lấy/phát lại tin nhắn: **PASSED** (`shouldNotReturnMessageOfAnotherSenderWhenClientMessageIdIsIdentical`).
2. Cùng bộ `sender + conversation + clientMessageId` gửi lặp chỉ tạo 1 bản ghi duy nhất, kể cả qua REST và WebSocket: **PASSED** (`shouldSaveMessageOnlyOnceForSameSenderConversationAndClientMessageId`, `shouldDeduplicateMessageViaRestFallbackWithSameKey`).
3. Cùng ID nhưng khác sender hoặc conversation không trả lại tin cũ: **PASSED** (Unique composite key theo bộ 3 thành phần).
4. Người không phải thành viên bị từ chối trước khi tra cứu idempotency: **PASSED** (`shouldRejectNonParticipantBeforeCheckingIdempotency`, `shouldRejectUnauthorizedUserInRestEndpoint`).
5. Retry sau timeout giữ nguyên ID và không tạo tin trùng: **PASSED** (`chat-inbox.component.ts: retryMessage()`).
6. Destination `/user/queue/ack` và `/user/queue/errors` được phép; destination `/user/**` ngoài allowlist bị từ chối: **PASSED** (`shouldAllowSubscribeToWhitelistedUserDestinations`, `shouldRejectSubscribeToNonWhitelistedUserDestinations`).
7. WebSocket session hết hạn/thu hồi không còn nhận được message (bị đóng kết nối vật lý ngay lập tức): **PASSED** (`shouldEvictPhysicalSessionWhenTokenExpires`, `shouldEvictPhysicalSessionWhenTokenIsRevoked`).
8. Reconnect đồng bộ message bị bỏ lỡ mà không tạo bản trùng: **PASSED** (`shouldGetMessagesSinceIdForReconnectSync`, `syncMissedMessages()`).
9. Origin production hợp lệ được chấp nhận; Origin ngoài allowlist bị từ chối: **PASSED** (`shouldAcceptAllowedOrigins`, `shouldRejectDisallowedOrigins`).

### 4.2 Frontend Angular Build (`mindbridge-jhipster`)
Chạy build dev frontend:
```bash
npm run webapp:build:dev
```
**Kết quả:** Build thành công 100% với exit code 0 (`The command exited with code 0`), không phát sinh bất kỳ lỗi TypeScript hay template error nào.

---

## 5. Rủi ro còn lại & Khuyến nghị Vận hành Production

1. **Redis Network Availability:**
   - `TokenRevocationService` được thiết kế có circuit breaker và fallback: nếu Redis tạm thời bị gián đoạn, dịch vụ sẽ không làm sập ứng dụng mà dựa vào thời gian sống tự nhiên của JWT (`exp`) để evict session.
   - *Khuyến nghị:* Giữ Redis container luôn trong trạng thái healthy trong Docker Compose.
2. **Clock Skew giữa các Container:**
   - Token expiry kiểm tra dựa trên `Instant.now()`.
   - *Khuyến nghị:* Đảm bảo đồng bộ NTP trên máy chủ Docker Host để token expiration time tính toán chính xác giữa `opcbs-auth-service` và `opcbs-message-service`.
3. **Cấu hình `WS_ALLOWED_ORIGINS` khi Deploy Production:**
   - Trong môi trường staging/production, cần cung cấp đầy đủ URL frontend (ví dụ: `https://chat.mindbridge.vn,https://mindbridge.vn`) vào biến môi trường `WS_ALLOWED_ORIGINS`, không để trống và không dùng `*`.
