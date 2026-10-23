import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable, catchError, finalize, map, of } from 'rxjs';

import { serverApiUrl } from 'app/config';

import { Login } from './login.model';
import { StateStorageService } from './state-storage.service';

type JwtToken = {
  id_token: string;
};

@Service()
export class AuthServerProvider {
  private readonly http = inject(HttpClient);
  private readonly stateStorageService = inject(StateStorageService);

  getToken(): string {
    return this.stateStorageService.getAuthenticationToken() ?? '';
  }

  login(credentials: Login): Observable<void> {
    return this.http
      .post<JwtToken>(`${serverApiUrl}api/authenticate`, credentials)
      .pipe(map(response => this.authenticateSuccess(response, credentials.rememberMe)));
  }

  logout(): Observable<void> {
    // =========================================================================
    // [CƠ CHẾ CŨ TRƯỚC ĐÂY]:
    // Chỉ dọn dẹp local storage ở trình duyệt:
    //   this.stateStorageService.clearAuthenticationToken();
    //   observer.complete();
    //
    // [CƠ CHẾ MỚI - ENTERPRISE LOGOUT WITH REDIS BLACKLIST]:
    // 1. Trích xuất token hiện tại và xóa sạch Client Storage (localStorage, sessionStorage, cookie) NGAY LẬP TỨC.
    // 2. Gửi POST /api/logout tới Backend:
    //    - Backend trích xuất JTI của token và lưu vào Redis Token Blacklist.
    //    - Backend trả về Header Set-Cookie: max-age=0 để ép trình duyệt xóa sạch HttpOnly Cookie.
    // 3. finalize(): Dọn sạch lại một lần nữa đảm bảo không còn tàn dư.
    // =========================================================================
    const token = this.stateStorageService.getAuthenticationToken();
    this.stateStorageService.clearAuthenticationToken();

    return this.http
      .post(
        `${serverApiUrl}api/logout`,
        {},
        {
          withCredentials: true,
          ...(token ? { headers: { Authorization: `Bearer ${token}` } } : {}),
        }
      )
      .pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
        finalize(() => {
          this.stateStorageService.clearAuthenticationToken();
        })
      );
  }

  private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
    this.stateStorageService.storeAuthenticationToken(response.id_token, rememberMe);
  }
}
