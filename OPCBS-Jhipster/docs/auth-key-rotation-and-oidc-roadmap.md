# Lộ trình Quản lý Khóa JWT, Key Rotation & Tích hợp External OIDC Provider

## 1. Hiện trạng kiến trúc xác thực (Current State)

Hiện tại, hệ thống microservices **OPCBS-JHipster** đã hoàn tất nâng cấp mô hình xác thực bất đối xứng thông qua `opcbs-auth-service`:
- **Giải thuật ký**: Bất đối xứng RSA-SHA256 (`RS256`), Auth Service ký bằng RSA Private Key và cấp header `kid`.
- **Xác minh phân tán**: 8 downstream microservices chỉ sử dụng RSA Public Key hoặc truy vấn JWKS endpoint (`/.well-known/jwks.json`, `/api/jwks`).
- **Cơ chế thu hồi (Revocation)**: `TokenBlacklistService` ghi nhận `jti`/token bị vô hiệu hóa vào Redis cache kèm TTL đồng bộ với thời gian hết hạn của token.
- **Fail-Closed Resilience**: Đã chuẩn hóa cơ chế từ chối xác thực (fail-closed) trong môi trường production khi Redis gặp sự cố kết nối, ngăn chặn tuyệt đối rủi ro bypass token thu hồi.
- **Không chia sẻ Private Key**: Chỉ duy nhất `opcbs-auth-service` giữ Private Key; các service khác chỉ nhận Public Key. Môi trường production tự động fail-fast nếu thiếu private key ở auth service.

---

## 2. Lộ trình hỗ trợ Khóa bất đối xứng (Asymmetric Signing) & Key Rotation

Để tiến tới chuẩn bảo mật Zero Trust (chỉ `opcbs-auth-service` giữ Private Key để ký, các service khác chỉ giữ Public Key để verify):

### Giai đoạn 1: Chuẩn bị cặp khóa RSA và JWKS Endpoint
1. `opcbs-auth-service` tạo cặp khóa RSA-2048 / RSA-4096 (Private Key dùng để ký token với thuật toán `RS256`, Public Key được export qua endpoint tiêu chuẩn RFC 7517).
2. Triển khai endpoint công khai: `GET /.well-known/jwks.json` trả về danh sách các JSON Web Key (JWK) kèm thuộc tính định danh khóa `kid` (Key ID).

### Giai đoạn 2: Cơ chế chuyển đổi khóa mượt mà (Zero-Downtime Dual-Key Verification)
- Khi thực hiện **Key Rotation**, hệ sinh thái microservices không được phép ngắt quãng phiên đăng nhập của người dùng còn hạn:
  - Khóa mới ($K_{new}$) được sinh ra với `kid = "k-2026-q4"`.
  - Khóa cũ ($K_{old}$) được đánh dấu "deprecated" nhưng vẫn được giữ trong danh sách verify của JWKS thêm một khoảng thời gian bằng thời hạn token tối đa (ví dụ: 24 giờ cho access token, 7 ngày cho refresh token).
  - `opcbs-auth-service` phát hành token mới ký bằng $K_{new}$.
  - Các downstream microservices tự động cache JWKS (thông qua Nimbus JOSE hoặc Spring Security NimbusJwtDecoder) và tải lại keyset khi gặp `kid` mới. Token cũ ký bởi $K_{old}$ vẫn hợp lệ cho tới khi hết hạn tự nhiên.

---

## 3. Lộ trình chuyển dịch sang External OIDC Provider (Keycloak / Okta / Azure AD)

Thay vì tự duy trì hệ thống quản lý mật khẩu và mã hóa JWT nội bộ, hệ thống có thể chuyển sang OpenID Connect (OIDC) chuẩn quốc tế:

### Giai đoạn 1: Lựa chọn và cấp phát OIDC Provider
- Triển khai **Keycloak** (Self-hosted trên Kubernetes/Docker Compose) hoặc dịch vụ Identity-as-a-Service (IDaaS) như Google Cloud Identity Platform, Auth0, Microsoft Entra ID.
- Cấu hình Realm `opcbs`, tạo các Client:
  - `opcbs-frontend` (Public Client, Authorization Code Flow + PKCE).
  - `opcbs-backend` (Bearer-only Resource Server).

### Giai đoạn 2: Migration dữ liệu tài khoản người dùng
1. Xuất dữ liệu từ bảng `jhi_user` (username, email, password hash BCrypt, trạng thái kích hoạt, roles).
2. Tạo script nạp vào Keycloak sử dụng User Federation Provider hoặc Keycloak REST API. Đối với password hash BCrypt, cấu hình Keycloak BCrypt Password Hashing Provider tương thích để người dùng không phải reset mật khẩu.
3. Đồng bộ các bảng nghiệp vụ (`doctor_profile`, `patient_profile`) liên kết thông qua Subject UUID (`sub` claim) của OIDC thay vì ID số tự tăng nội bộ.

### Giai đoạn 3: Cấu hình Spring Security Resource Server
- Cập nhật các microservices chuyển từ manual JWT filter sang chuẩn:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: https://auth.mindbridge.vn/realms/opcbs
            jwk-set-uri: https://auth.mindbridge.vn/realms/opcbs/protocol/openid-connect/certs
  ```
- Tận dụng chuẩn UserInfo endpoint, Token Introspection và phân quyền RBAC/ABAC trên Keycloak Admin Console.
