import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

@Injectable({ providedIn: 'root' })
export class PasswordResetInitService {
  private readonly http = inject(HttpClient);

  save(mail: string): Observable<{}> {
    return this.http.post(`${serverApiUrl}api/account/reset-password/init`, mail);
  }
}
