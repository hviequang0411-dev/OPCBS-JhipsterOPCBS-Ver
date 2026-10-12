import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IEmotionJournal, IJournalStats } from '../emotion-journal.model';

@Injectable({ providedIn: 'root' })
export class EmotionJournalService {
  private http = inject(HttpClient);

  private patientResourceUrl = `${serverApiUrl}api/patient/journals`;
  private doctorResourceUrl = `${serverApiUrl}api/doctor/patients`;

  getMyJournals(): Observable<IEmotionJournal[]> {
    return this.http.get<IEmotionJournal[]>(this.patientResourceUrl);
  }

  createJournal(journal: IEmotionJournal): Observable<IEmotionJournal> {
    return this.http.post<IEmotionJournal>(this.patientResourceUrl, journal);
  }

  updateJournal(id: number, journal: IEmotionJournal): Observable<IEmotionJournal> {
    return this.http.put<IEmotionJournal>(`${this.patientResourceUrl}/${id}`, journal);
  }

  deleteJournal(id: number): Observable<void> {
    return this.http.delete<void>(`${this.patientResourceUrl}/${id}`);
  }

  getStats(): Observable<IJournalStats> {
    return this.http.get<IJournalStats>(`${this.patientResourceUrl}/stats`);
  }

  getSharedJournalsForDoctor(patientId: number): Observable<IEmotionJournal[]> {
    return this.http.get<IEmotionJournal[]>(`${this.doctorResourceUrl}/${patientId}/shared-journals`);
  }
}
