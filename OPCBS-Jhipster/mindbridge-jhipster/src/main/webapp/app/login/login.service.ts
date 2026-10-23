import { Service, inject } from '@angular/core';

import { Observable, mergeMap, shareReplay } from 'rxjs';

import { Account, AccountService, AuthServerProvider, Login } from 'app/core/auth';

@Service()
export class LoginService {
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);

  login(credentials: Login): Observable<Account | null> {
    return this.authServerProvider.login(credentials).pipe(mergeMap(() => this.accountService.identity(true)));
  }

  logout(): Observable<void> {
    // =========================================================================
    // [CƠ CHẾ MỚI - ENTERPRISE NON-BLOCKING LOGOUT]:
    // 1. Reset ngay lập tức trạng thái account trên client để UI chuyển sang 'Khách'
    // 2. Gửi POST /api/logout lên Server để đưa JTI vào Redis Blacklist và xóa sạch HttpOnly Cookie
    // 3. Dùng shareReplay(1) và tự subscribe để request luôn được gửi kể cả khi caller không subscribe.
    // =========================================================================
    this.accountService.authenticate(null);
    const logout$ = this.authServerProvider.logout().pipe(shareReplay(1));
    logout$.subscribe();
    return logout$;
  }
}
