import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { tap } from 'rxjs';

import { EventManager, EventWithContent } from 'app/core/util';

export const errorHandlerInterceptor: HttpInterceptorFn = (req, next) => {
  const eventManager = inject(EventManager);

  return next(req).pipe(
    tap({
      error(err: HttpErrorResponse) {
        // =========================================================================
        // [CƠ CHẾ MỚI]: Bỏ qua hoàn toàn lỗi 401.
        // Khi gặp 401 (hết hạn phiên / logout), authExpiredInterceptor sẽ tự động
        // dọn sạch session và đưa người dùng về trang /login một cách êm ái,
        // KHÔNG hiển thị alert/banner báo lỗi màu đỏ làm người dùng hoang mang.
        // =========================================================================
        if (err.status === 401) {
          return;
        }
        eventManager.broadcast(new EventWithContent('opcbsApp.httpError', err));
      },
    }),
  );
};
