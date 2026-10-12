import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IAppointment } from 'app/entities/appointment/appointment.model';
import { RestAppointment } from 'app/entities/appointment/service/appointment.service';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export interface IDoctorPatientSummary {
  patientId: number;
  fullName: string;
  email?: string | null;
  phone?: string | null;
  gender?: string | null;
  dateOfBirth?: dayjs.Dayjs | null;
  address?: string | null;
  medicalHistory?: string | null;
  allergies?: string | null;
  bloodGroup?: string | null;
  isGuest?: boolean;
  createdAt?: dayjs.Dayjs | null;
  totalAppointments: number;
  completedAppointments: number;
  upcomingAppointments: number;
  lastAppointmentDate?: dayjs.Dayjs | null;
  lastDiagnosis?: string | null;
}

export interface IPatientDetailRecord {
  patient: IPatientProfile;
  totalAppointments: number;
  completedAppointments: number;
  appointments: IAppointment[];
}

export interface ISaveConsultationNoteRequest {
  diagnosis?: string | null;
  consultationSummary?: string | null;
  therapyPlan?: string | null;
  recommendations?: string | null;
  followUpNotes?: string | null;
  nextAppointmentRecommendedDate?: string | null;
  markCompleted?: boolean;
}

@Injectable({ providedIn: 'root' })
export class DoctorPatientService {
  private readonly http = inject(HttpClient);
  private readonly doctorUrl = `${serverApiUrl}api/doctor`;

  getMyPatients(): Observable<IDoctorPatientSummary[]> {
    return this.http.get<any[]>(`${this.doctorUrl}/my-patients`).pipe(
      map(list =>
        list.map(item => ({
          ...item,
          isGuest: item.guest ?? item.isGuest ?? false,
          dateOfBirth: item.dateOfBirth ? dayjs(item.dateOfBirth) : undefined,
          lastAppointmentDate: item.lastAppointmentDate ? dayjs(item.lastAppointmentDate) : undefined,
          createdAt: item.createdAt ? dayjs(item.createdAt) : undefined,
        }))
      )
    );
  }

  getPatientDetail(patientId: number): Observable<IPatientDetailRecord> {
    return this.http.get<any>(`${this.doctorUrl}/patients/${patientId}`).pipe(
      map(res => ({
        ...res,
        patient: {
          ...res.patient,
          dateOfBirth: res.patient?.dateOfBirth ? dayjs(res.patient.dateOfBirth) : undefined,
        },
        appointments: (res.appointments || []).map((app: RestAppointment) => this.convertAppointmentFromServer(app)),
      }))
    );
  }

  exportPatientPdf(patientId: number): Observable<Blob> {
    return this.http.get(`${this.doctorUrl}/patients/${patientId}/export-pdf`, {
      responseType: 'blob',
    });
  }

  saveConsultationNote(appointmentId: number, req: ISaveConsultationNoteRequest): Observable<IAppointment> {
    return this.http
      .post<RestAppointment>(`${this.doctorUrl}/appointments/${appointmentId}/consultation-note`, req)
      .pipe(map(res => this.convertAppointmentFromServer(res)));
  }

  private convertAppointmentFromServer(restAppointment: RestAppointment): IAppointment {
    return {
      ...restAppointment,
      scheduledStart: restAppointment.scheduledStart ? dayjs(restAppointment.scheduledStart) : undefined,
      scheduledEnd: restAppointment.scheduledEnd ? dayjs(restAppointment.scheduledEnd) : undefined,
      nextAppointmentRecommendedDate: restAppointment.nextAppointmentRecommendedDate
        ? dayjs(restAppointment.nextAppointmentRecommendedDate)
        : undefined,
      completedAt: restAppointment.completedAt ? dayjs(restAppointment.completedAt) : undefined,
      createdAt: restAppointment.createdAt ? dayjs(restAppointment.createdAt) : undefined,
      updatedAt: restAppointment.updatedAt ? dayjs(restAppointment.updatedAt) : undefined,
    };
  }
}
