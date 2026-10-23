import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { ITreatmentGoal, ITreatmentPackage, ITreatmentProgressSummary, ITreatmentSession } from '../treatment-package.model';

@Injectable({ providedIn: 'root' })
export class TreatmentPackageService {
  private http = inject(HttpClient);

  private doctorResourceUrl = `${serverApiUrl}api/doctor/treatment-packages`;
  private patientResourceUrl = `${serverApiUrl}api/patient/my-treatment-packages`;
  private sharedPackageUrl = `${serverApiUrl}api/treatment-packages`;

  // === Doctor Packages ===
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

  // === Patient Packages ===
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

  // === Hold (Bảo Lưu) Lifecycle ===
  requestHold(id: number, durationDays: number, reason: string): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(`${this.patientResourceUrl}/${id}/request-hold`, { durationDays, reason });
  }

  cancelHoldRequest(id: number): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(`${this.patientResourceUrl}/${id}/cancel-hold-request`, {});
  }

  approveHold(id: number, doctorNote?: string): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(`${this.doctorResourceUrl}/${id}/approve-hold`, { doctorNote });
  }

  rejectHold(id: number, doctorNote?: string): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(`${this.doctorResourceUrl}/${id}/reject-hold`, { doctorNote });
  }

  resumePackage(id: number): Observable<ITreatmentPackage> {
    return this.http.post<ITreatmentPackage>(`${this.sharedPackageUrl}/${id}/resume`, {});
  }

  // === Goals (Mục Tiêu Điều Trị) ===
  getGoals(packageId: number): Observable<ITreatmentGoal[]> {
    return this.http.get<ITreatmentGoal[]>(`${this.sharedPackageUrl}/${packageId}/goals`);
  }

  createGoal(packageId: number, goal: ITreatmentGoal): Observable<ITreatmentGoal> {
    return this.http.post<ITreatmentGoal>(`${this.sharedPackageUrl}/${packageId}/goals`, goal);
  }

  updateGoal(packageId: number, goalId: number, goal: ITreatmentGoal): Observable<ITreatmentGoal> {
    return this.http.put<ITreatmentGoal>(`${this.sharedPackageUrl}/${packageId}/goals/${goalId}`, goal);
  }

  deleteGoal(packageId: number, goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.sharedPackageUrl}/${packageId}/goals/${goalId}`);
  }

  // === Sessions (Buổi Tham Vấn & Diễn Biến Lâm Sàng) ===
  getSessions(packageId: number): Observable<ITreatmentSession[]> {
    return this.http.get<ITreatmentSession[]>(`${this.sharedPackageUrl}/${packageId}/sessions`);
  }

  createSession(packageId: number, session: ITreatmentSession): Observable<ITreatmentSession> {
    return this.http.post<ITreatmentSession>(`${this.sharedPackageUrl}/${packageId}/sessions`, session);
  }

  // === Database-level Progress Summary ===
  getProgressSummary(packageId: number): Observable<ITreatmentProgressSummary> {
    return this.http.get<ITreatmentProgressSummary>(`${this.sharedPackageUrl}/${packageId}/progress`);
  }
}
