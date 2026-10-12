import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IDoctorVerification, IStoredFileMetadata } from './doctor-verification.model';

@Injectable({
  providedIn: 'root',
})
export class DoctorVerificationService {
  private readonly http = inject(HttpClient);

  getVerification(): Observable<IDoctorVerification> {
    return this.http.get<IDoctorVerification>('/api/doctor/verification');
  }

  submitVerification(data: Partial<IDoctorVerification>): Observable<IDoctorVerification> {
    return this.http.post<IDoctorVerification>('/api/doctor/submit-verification', data);
  }

  uploadCertificate(file: File, documentType = 'MEDICAL_LICENSE'): Observable<IStoredFileMetadata> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('documentType', documentType);
    return this.http.post<IStoredFileMetadata>('/api/doctor/upload-certificate', formData);
  }

  uploadCertificateBase64(
    base64Content: string,
    fileName: string,
    contentType: string,
    documentType = 'MEDICAL_LICENSE'
  ): Observable<IStoredFileMetadata> {
    const body = {
      fileName,
      documentType,
      contentType,
      base64Content,
    };
    return this.http.post<IStoredFileMetadata>('/api/doctor/upload-certificate-base64', body);
  }
}
