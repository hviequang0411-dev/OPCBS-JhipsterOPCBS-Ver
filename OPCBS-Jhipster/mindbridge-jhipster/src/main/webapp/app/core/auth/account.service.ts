import { HttpClient } from '@angular/common/http';
import { Service, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Observable, catchError, of, shareReplay, tap } from 'rxjs';

import { serverApiUrl } from 'app/config';

import { Account } from './account.model';
import { StateStorageService } from './state-storage.service';

@Service()
export class AccountService {
  private readonly userIdentity = signal<Account | null>(null);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly account = this.userIdentity.asReadonly();
  private accountCache$?: Observable<Account> | null;

  private readonly http = inject(HttpClient);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly router = inject(Router);

  authenticate(identity: Account | null): void {
    this.userIdentity.set(identity);
    if (!identity) {
      this.accountCache$ = null;
    }
  }

  hasAnyAuthority(authorities: string[] | string): boolean {
    const userIdentity = this.userIdentity();
    if (!userIdentity) {
      return false;
    }
    if (!Array.isArray(authorities)) {
      authorities = [authorities];
    }
    return userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }

  identity(force?: boolean): Observable<Account | null> {
    // =========================================================================
    // [CƠ CHẾ MỚI - TRÁNH LỖI 401 KHÔNG CẦN THIẾT KHI CHƯA ĐĂNG NHẬP / ĐÃ LOGOUT]:
    // Nếu trong Web Storage không có token (đã đăng xuất hoặc chưa đăng nhập),
    // lập tức trả về of(null), KHÔNG gửi request GET /api/account lên server.
    // Điều này ngăn chặn triệt để tình trạng trình duyệt báo lỗi đỏ 401 khi F5 hoặc mở tab mới.
    // =========================================================================
    if (!this.stateStorageService.getAuthenticationToken()) {
      this.authenticate(null);
      this.accountCache$ = null;
      return of(null);
    }

    if (!this.accountCache$ || force) {
      this.accountCache$ = this.fetch().pipe(
        tap((account: Account) => {
          this.authenticate(account);

          this.navigateToStoredUrl();
        }),
        shareReplay(),
      );
    }
    return this.accountCache$.pipe(
      catchError(() => {
        this.authenticate(null);
        this.stateStorageService.clearAuthenticationToken();
        this.accountCache$ = null;
        return of(null);
      }),
    );
  }

  isAuthenticated(): boolean {
    return this.userIdentity() !== null;
  }

  getProfile(): Observable<any> {
    return this.http.get<any>(`${serverApiUrl}api/account`);
  }

  updateProfile(account: any): Observable<any> {
    return this.http.post<any>(`${serverApiUrl}api/account`, account);
  }

  save(account: any): Observable<any> {
    return this.http.post<any>(`${serverApiUrl}api/account`, account);
  }

  private fetch(): Observable<Account> {
    return this.http.get<Account>(`${serverApiUrl}api/account`);
  }

  private navigateToStoredUrl(): void {
    // previousState can be set in the authExpiredInterceptor and in the userRouteAccessService
    // if login is successful, go to stored previousState and clear previousState
    const previousUrl = this.stateStorageService.getUrl();
    if (previousUrl) {
      this.stateStorageService.clearUrl();
      this.router.navigateByUrl(previousUrl);
    }
  }
}
