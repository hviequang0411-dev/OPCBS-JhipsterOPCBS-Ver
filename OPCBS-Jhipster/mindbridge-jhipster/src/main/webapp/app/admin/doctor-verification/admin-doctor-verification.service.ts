import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IDoctorVerification, IReviewVerification, IVerificationSummary } from 'app/entities/doctor-profile/verification/doctor-verification.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class AdminDoctorVerificationService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = '/api/admin/doctor-verifications';

  getAllVerifications(
    page: number = 0,
    size: number = 10,
    status?: string,
    search?: string
  ): Observable<PageResponse<IDoctorVerification>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'submittedAt,desc');

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PageResponse<IDoctorVerification>>(this.resourceUrl, { params });
  }

  getVerificationSummary(): Observable<IVerificationSummary> {
    return this.http.get<IVerificationSummary>(`${this.resourceUrl}/summary`);
  }

  getVerificationById(id: number): Observable<IDoctorVerification> {
    return this.http.get<IDoctorVerification>(`${this.resourceUrl}/${id}`);
  }

  reviewVerification(id: number, reviewDto: IReviewVerification): Observable<IDoctorVerification> {
    return this.http.put<IDoctorVerification>(`${this.resourceUrl}/${id}/review`, reviewDto);
  }

  approveVerification(id: number): Observable<IDoctorVerification> {
    return this.http.post<IDoctorVerification>(`${this.resourceUrl}/${id}/approve`, {});
  }

  rejectVerification(id: number, reason: string): Observable<IDoctorVerification> {
    return this.http.post<IDoctorVerification>(`${this.resourceUrl}/${id}/reject`, { reason });
  }

  requestAdditionalInfo(id: number, reason: string): Observable<IDoctorVerification> {
    return this.http.post<IDoctorVerification>(`${this.resourceUrl}/${id}/request-info`, { reason });
  }
}
