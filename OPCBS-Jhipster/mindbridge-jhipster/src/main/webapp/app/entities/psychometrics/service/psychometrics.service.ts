import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IPsychometricTest, IPsychometricSubmission, ISubmissionRequestVM, IAssignTestVM } from '../psychometrics.model';

@Injectable({ providedIn: 'root' })
export class PsychometricsService {
  private http = inject(HttpClient);

  private publicTestsUrl = `${serverApiUrl}api/psychometrics/tests`;
  private submissionsUrl = `${serverApiUrl}api/psychometrics/submissions`;
  private patientSubmissionsUrl = `${serverApiUrl}api/patient/psychometrics/my-submissions`;
  private doctorAssignUrl = `${serverApiUrl}api/doctor/psychometrics/assign`;
  private doctorPatientSubmissionsUrl = `${serverApiUrl}api/doctor/patients`;

  getAllActiveTests(): Observable<IPsychometricTest[]> {
    return this.http.get<IPsychometricTest[]>(this.publicTestsUrl);
  }

  getTestWithQuestions(id: number): Observable<IPsychometricTest> {
    return this.http.get<IPsychometricTest>(`${this.publicTestsUrl}/${id}`);
  }

  submitTest(testId: number, req: ISubmissionRequestVM): Observable<IPsychometricSubmission> {
    return this.http.post<IPsychometricSubmission>(`${this.publicTestsUrl}/${testId}/submit`, req);
  }

  getMySubmissions(): Observable<IPsychometricSubmission[]> {
    return this.http.get<IPsychometricSubmission[]>(this.patientSubmissionsUrl);
  }

  getSubmissionDetails(submissionId: number): Observable<IPsychometricSubmission> {
    return this.http.get<IPsychometricSubmission>(`${this.submissionsUrl}/${submissionId}`);
  }

  assignTest(vm: IAssignTestVM): Observable<IPsychometricSubmission> {
    return this.http.post<IPsychometricSubmission>(this.doctorAssignUrl, vm);
  }

  getPatientPsychometricsForDoctor(patientId: number): Observable<IPsychometricSubmission[]> {
    return this.http.get<IPsychometricSubmission[]>(`${this.doctorPatientSubmissionsUrl}/${patientId}/psychometrics`);
  }
}
