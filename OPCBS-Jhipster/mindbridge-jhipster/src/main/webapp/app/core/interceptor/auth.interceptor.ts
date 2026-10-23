import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { serverApiUrl } from 'app/config';
import { StateStorageService } from 'app/core/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const stateStorageService = inject(StateStorageService);

  if (!req.url || (req.url.startsWith('http') && !(serverApiUrl && req.url.startsWith(serverApiUrl)))) {
    return next(req);
  }

  const token = stateStorageService.getAuthenticationToken();
  
  // =========================================================================
  // [BFF HTTPONLY COOKIE SUPPORT]:
  // withCredentials: true đảm bảo trình duyệt luôn tự động gửi kèm HttpOnly Cookie
  // ('jhi-authenticationToken') trong mọi request API tới Nginx Gateway / Backend.
  // Nếu có sẵn token trong bộ nhớ thì gắn kèm Header 'Authorization: Bearer <token>'
  // để tương thích ngược 100% với legacy flow.
  // =========================================================================
  req = req.clone({
    withCredentials: true,
    ...(token ? { setHeaders: { Authorization: `Bearer ${token}` } } : {}),
  });
  return next(req);
};
