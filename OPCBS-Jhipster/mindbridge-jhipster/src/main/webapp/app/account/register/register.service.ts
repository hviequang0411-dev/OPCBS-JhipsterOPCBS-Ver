import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

import { Registration } from './register.model';

@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly http = inject(HttpClient);

  save(registration: Registration): Observable<{}> {
    return this.http.post(`${serverApiUrl}api/register`, registration);
  }

  saveDoctor(doctorVM: any): Observable<{}> {
    return this.http.post(`${serverApiUrl}api/register-doctor`, doctorVM);
  }
}
