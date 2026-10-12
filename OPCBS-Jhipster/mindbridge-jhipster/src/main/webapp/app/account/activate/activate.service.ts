import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

@Injectable({ providedIn: 'root' })
export class ActivateService {
  private readonly http = inject(HttpClient);

  get(key: string): Observable<{}> {
    return this.http.get(`${serverApiUrl}api/activate`, {
      params: new HttpParams().set('key', key),
    });
  }
}
