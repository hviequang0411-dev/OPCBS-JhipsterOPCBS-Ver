import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

@Injectable({ providedIn: 'root' })
export class PasswordService {
  private readonly http = inject(HttpClient);

  save(newPassword: string, currentPassword: string): Observable<{}> {
    return this.http.post(`${serverApiUrl}api/account/change-password`, { currentPassword, newPassword });
  }
}
