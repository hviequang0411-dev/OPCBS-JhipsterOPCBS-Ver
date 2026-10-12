import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';

export interface IOtpVerificationRequest {
  emailOrLogin: string;
  otp: string;
}

export interface IOtpResendRequest {
  emailOrLogin: string;
}

@Injectable({ providedIn: 'root' })
export class OtpService {
  private readonly http = inject(HttpClient);

  verifyOtp(req: IOtpVerificationRequest): Observable<void> {
    return this.http.post<void>(`${serverApiUrl}api/account/verify-otp`, req);
  }

  resendOtp(req: IOtpResendRequest): Observable<void> {
    return this.http.post<void>(`${serverApiUrl}api/account/resend-otp`, req);
  }
}
