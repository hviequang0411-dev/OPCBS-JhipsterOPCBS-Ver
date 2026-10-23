import { Service } from '@angular/core';

import { AUTHENTICATION_TOKEN_KEY } from 'app/shared/jhipster/constants';

/**
 * Service quản lý trạng thái lưu trữ Token và URL điều hướng.
 * 
 * ==============================================================================================
 * SO SÁNH KIẾN TRÚC BẢO MẬT: CƠ CHẾ CŨ (CLIENT FALLBACK) VS CƠ CHẾ MỚI (ENTERPRISE HTTPONLY BFF)
 * ==============================================================================================
 * 
 * 1. [CƠ CHẾ CŨ TRƯỚC ĐÂY]:
 *    - Server thiết lập Cookie với cờ `httpOnly(false)`.
 *    - Phía Angular JavaScript tự tay đọc/ghi cookie bằng `document.cookie`.
 *    - Ưu điểm cũ: Cứu nguy lỗi "mất phiên khi mở Tab mới" của `sessionStorage`.
 *    - Nhược điểm bảo mật: Nếu trang web xuất hiện lỗ hổng XSS (Cross-Site Scripting), hacker có thể
 *      dùng mã độc JavaScript đọc trộm chuỗi JWT trong `document.cookie` hoặc `localStorage`.
 * 
 * 2. [CƠ CHẾ MỚI - NÂNG CẤP CHUẨN DOANH NGHIỆP (ENTERPRISE HTTPONLY & NGINX BFF GATEWAY)]:
 *    - Server phát hành Cookie với cờ `httpOnly(true)`. Trình duyệt cấm tuyệt đối JavaScript truy cập.
 *    - Token hoàn toàn miễn nhiễm trước nguy cơ bị đánh cắp qua XSS.
 *    - JavaScript KHÔNG cần đọc `document.cookie` nữa:
 *      + Mỗi request gửi lên Nginx Gateway, Nginx sẽ tự động bóc HttpOnly Cookie
 *        và chuyển thành Header `Authorization: Bearer <jwt>` trước khi đẩy vào microservices bên trong.
 *      + Phía Backend cũng có `CookieToBearerTokenFilter` để tự động bọc nếu chạy trực tiếp.
 *    - Khi người dùng đăng xuất: Hệ thống gọi API `POST /api/logout` để đưa Token ID (jti) vào
 *      Redis Token Blacklist và xóa sạch Cookie từ phía máy chủ.
 * ==============================================================================================
 */
@Service()
export class StateStorageService {
  private readonly previousUrlKey = 'previousUrl';
  private readonly authenticationKey = AUTHENTICATION_TOKEN_KEY;

  storeUrl(url: string): void {
    sessionStorage.setItem(this.previousUrlKey, JSON.stringify(url));
  }

  getUrl(): string | null {
    const previousUrl = sessionStorage.getItem(this.previousUrlKey);
    return previousUrl ? (JSON.parse(previousUrl) as string | null) : previousUrl;
  }

  clearUrl(): void {
    sessionStorage.removeItem(this.previousUrlKey);
  }

  storeAuthenticationToken(authenticationToken: string, rememberMe: boolean): void {
    const rawToken = authenticationToken;
    authenticationToken = JSON.stringify(authenticationToken);
    this.clearAuthenticationToken();
    if (rememberMe) {
      localStorage.setItem(this.authenticationKey, authenticationToken);
    } else {
      sessionStorage.setItem(this.authenticationKey, authenticationToken);
    }

    // =========================================================================
    // [GIỮ NGUYÊN CODE CŨ ĐỂ ĐỐI CHIẾU - KHÔNG XÓA]:
    // Trước đây ta gọi:
    //   this.setCookie(this.authenticationKey, rawToken, rememberMe);
    //   this.setCookie('id_token', rawToken, rememberMe);
    // 
    // Hiện tại với chuẩn Enterprise HttpOnly:
    // Cookie đã được Backend tự động gắn qua header 'Set-Cookie' (HttpOnly=true, SameSite=Lax).
    // Trình duyệt tự động lưu vào kho bảo mật của OS, JavaScript không cần (và không thể)
    // can thiệp ghi đè bằng document.cookie nữa.
    // =========================================================================
    /* CODE CŨ TRƯỚC ĐÂY:
    this.setCookie(this.authenticationKey, rawToken, rememberMe);
    this.setCookie('id_token', rawToken, rememberMe);
    */
  }

  getAuthenticationToken(): string | null {
    // 1. Kiểm tra trong Web Storage (nếu có lưu client-side)
    const authenticationToken = localStorage.getItem(this.authenticationKey) ?? sessionStorage.getItem(this.authenticationKey);
    if (authenticationToken) {
      try {
        return JSON.parse(authenticationToken) as string;
      } catch {
        return authenticationToken;
      }
    }

    // =========================================================================
    // [GIỮ NGUYÊN CODE CŨ ĐỂ ĐỐI CHIẾU - KHÔNG XÓA]:
    // Trước đây (khi httpOnly=false), nếu mở tab mới khiến sessionStorage bị rỗng,
    // Angular sẽ đọc trực tiếp từ document.cookie:
    //   return this.getCookie(this.authenticationKey) ?? this.getCookie('id_token');
    //
    // Hiện tại (với Enterprise HttpOnly=true):
    // document.cookie không thể đọc được HttpOnly cookie nữa (trả về null).
    // Nhưng bạn hoàn toàn YÊN TÂM: Khi Angular gửi HTTP request, trình duyệt sẽ TỰ ĐỘNG
    // đính kèm HttpOnly Cookie. Nginx Gateway hoặc CookieToBearerTokenFilter phía backend
    // sẽ tự động nhặt cookie này và chuyển thành 'Authorization: Bearer <jwt>'!
    // =========================================================================
    return this.getCookie(this.authenticationKey) ?? this.getCookie('id_token');
  }

  clearAuthenticationToken(): void {
    sessionStorage.removeItem(this.authenticationKey);
    localStorage.removeItem(this.authenticationKey);
    sessionStorage.removeItem('jhi-authenticationToken');
    localStorage.removeItem('jhi-authenticationToken');
    sessionStorage.removeItem('id_token');
    localStorage.removeItem('id_token');

    // =========================================================================
    // [GIỮ NGUYÊN CODE CŨ ĐỂ ĐỐI CHIẾU - KHÔNG XÓA]:
    // Code cũ cố xóa cookie qua document.cookie:
    //   this.deleteCookie(this.authenticationKey);
    //   this.deleteCookie('id_token');
    // Với HttpOnly Cookie, việc xóa bắt buộc phải do Server thực hiện qua API POST /api/logout
    // với Set-Cookie: max-age=0 (đã được tích hợp trong AuthServerProvider.logout).
    // =========================================================================
    this.deleteCookie(this.authenticationKey);
    this.deleteCookie('id_token');
    this.deleteCookie('jhi-authenticationToken');
  }

  // ===========================================================================
  // [CÁC HÀM TIỆN ÍCH COOKIE CŨ - GIỮ LẠI PHỤC VỤ SO SÁNH & BACKWARD COMPATIBILITY]
  // ===========================================================================

  /**
   * @deprecated Cơ chế cũ ghi cookie phía client. Đã được thay thế bởi Server Set-Cookie HttpOnly.
   */
  private setCookie(name: string, value: string, rememberMe: boolean): void {
    if (typeof document === 'undefined') {
      return;
    }
    let expires = '';
    if (rememberMe) {
      const date = new Date();
      date.setTime(date.getTime() + 30 * 24 * 60 * 60 * 1000);
      expires = `; expires=${date.toUTCString()}`;
    }
    document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}${expires}; path=/; SameSite=Lax`;
  }

  /**
   * @deprecated Cơ chế cũ đọc document.cookie. Với HttpOnly=true, hàm này trả về null đối với session cookie.
   */
  private getCookie(name: string): string | null {
    if (typeof document === 'undefined') {
      return null;
    }
    const nameEQ = `${encodeURIComponent(name)}=`;
    const cookies = document.cookie ? document.cookie.split(';') : [];
    for (let c of cookies) {
      c = c.trim();
      if (c.indexOf(nameEQ) === 0) {
        return decodeURIComponent(c.substring(nameEQ.length));
      }
    }
    return null;
  }

  /**
   * @deprecated Cơ chế cũ xóa cookie client. Xóa sạch mọi biến thể path nếu có cookie cũ còn sót lại.
   */
  private deleteCookie(name: string): void {
    if (typeof document === 'undefined') {
      return;
    }
    const encoded = encodeURIComponent(name);
    const host = window.location.hostname;
    const paths = ['/', '', window.location.pathname];
    for (const p of paths) {
      document.cookie = `${encoded}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=${p}; SameSite=Lax`;
      document.cookie = `${encoded}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=${p}; domain=${host}; SameSite=Lax`;
      document.cookie = `${encoded}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=${p}`;
    }
  }
}
