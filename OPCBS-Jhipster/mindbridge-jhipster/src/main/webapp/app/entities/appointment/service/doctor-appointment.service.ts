import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IAppointment } from '../appointment.model';
import { RestAppointment } from './appointment.service';

export interface IAppointmentStats {
  total: number;
  pending: number;
  accepted: number;
  inProgress: number;
  completed: number;
  cancelled: number;
  rejected: number;
}

@Injectable({ providedIn: 'root' })
export class DoctorAppointmentService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${serverApiUrl}api/doctor/appointments`;

  getAppointments(status?: string): Observable<IAppointment[]> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    return this.http
      .get<RestAppointment[]>(this.resourceUrl, { params })
      .pipe(map(list => list.map(item => this.convertValueFromServer(item))));
  }

  getStats(): Observable<IAppointmentStats> {
    return this.http.get<IAppointmentStats>(`${this.resourceUrl}/stats`);
  }

  acceptAppointment(id: number): Observable<IAppointment> {
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${id}/accept`, {})
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  startAppointment(id: number): Observable<IAppointment> {
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${id}/start`, {})
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  rejectAppointment(id: number, rejectionReason?: string): Observable<IAppointment> {
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${id}/reject`, { rejectionReason })
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  completeAppointment(id: number): Observable<IAppointment> {
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${id}/complete`, {})
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  private convertValueFromServer(restAppointment: RestAppointment): IAppointment {
    return {
      ...restAppointment,
      scheduledStart: restAppointment.scheduledStart ? dayjs(restAppointment.scheduledStart) : undefined,
      scheduledEnd: restAppointment.scheduledEnd ? dayjs(restAppointment.scheduledEnd) : undefined,
      createdAt: restAppointment.createdAt ? dayjs(restAppointment.createdAt) : undefined,
      updatedAt: restAppointment.updatedAt ? dayjs(restAppointment.updatedAt) : undefined,
      nextAppointmentRecommendedDate: restAppointment.nextAppointmentRecommendedDate
        ? dayjs(restAppointment.nextAppointmentRecommendedDate)
        : undefined,
      completedAt: restAppointment.completedAt ? dayjs(restAppointment.completedAt) : undefined,
    };
  }
}
