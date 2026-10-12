import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { ITreatmentPackage } from '../treatment-package.model';

@Injectable({ providedIn: 'root' })
export class TreatmentPackageService {
  private http = inject(HttpClient);

  private doctorResourceUrl = `${serverApiUrl}api/doctor/treatment-packages`;
  private patientResourceUrl = `${serverApiUrl}api/patient/my-treatment-packages`;

  getDoctorPackages(): Observable<ITreatmentPackage[]> {
    return this.http.get<ITreatmentPackage[]>(this.doctorResourceUrl);
  }

  getDoctorPackageById(id: number): Observable<ITreatmentPackage> {
    return this.http.get<ITreatmentPackage>(`${this.doctorResourceUrl}/${id}`);
  }

  createDoctorPackage(pkg: ITreatmentPackage): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(this.doctorResourceUrl, pkg);
  }

  updateDoctorPackage(id: number, pkg: ITreatmentPackage): Observable<ITreatmentPackage> {
    return this.http.put<ITreatmentPackage>(`${this.doctorResourceUrl}/${id}`, pkg);
  }

  deleteDoctorPackage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.doctorResourceUrl}/${id}`);
  }

  getPatientPackages(): Observable<ITreatmentPackage[]> {
    return this.http.get<ITreatmentPackage[]>(this.patientResourceUrl);
  }

  getPatientPackageById(id: number): Observable<ITreatmentPackage> {
    return this.http.get<ITreatmentPackage>(`${this.patientResourceUrl}/${id}`);
  }

  acceptPackage(id: number): Observable<ITreatmentPackage> {
    return this.http.put<ITreatmentPackage>(`${this.patientResourceUrl}/${id}/accept`, {});
  }

  rejectPackage(id: number, reason?: string): Observable<ITreatmentPackage> {
    return this.http.put<ITreatmentPackage>(`${this.patientResourceUrl}/${id}/reject`, { reason });
  }
}
