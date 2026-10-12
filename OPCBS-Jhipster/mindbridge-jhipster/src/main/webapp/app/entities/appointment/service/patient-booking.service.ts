import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IAppointment } from '../appointment.model';
import { RestAppointment } from './appointment.service';

export interface ICreateBookingRequest {
  slotId: number;
  consultationMode: string;
  reason?: string | null;
  symptoms?: string | null;
  medicalHistory?: string | null;
  expectations?: string | null;
  contactPhone?: string | null;
  contactZalo?: string | null;
  patientAddress?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PatientBookingService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${serverApiUrl}api/patient/appointments`;

  bookAppointment(req: ICreateBookingRequest): Observable<IAppointment> {
    return this.http
      .post<RestAppointment>(`${this.resourceUrl}/book`, req)
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  getMyAppointments(status?: string): Observable<IAppointment[]> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    return this.http
      .get<RestAppointment[]>(`${serverApiUrl}api/patient/my-appointments`, { params })
      .pipe(map(list => list.map(item => this.convertValueFromServer(item))));
  }

  cancelAppointment(id: number, cancellationReason?: string): Observable<IAppointment> {
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${id}/cancel`, { cancellationReason })
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  getMyClinicalRecords(): Observable<IAppointment[]> {
    return this.http
      .get<RestAppointment[]>(`${serverApiUrl}api/patient/my-clinical-records`)
      .pipe(map(list => list.map(item => this.convertValueFromServer(item))));
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
