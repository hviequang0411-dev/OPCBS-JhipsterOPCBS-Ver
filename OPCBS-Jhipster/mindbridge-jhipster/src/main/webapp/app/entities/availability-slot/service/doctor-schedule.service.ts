import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IAvailabilitySlot } from '../availability-slot.model';
import { RestAvailabilitySlot } from './availability-slot.service';

export interface ICreateSlotRequest {
  startTime: dayjs.Dayjs | string;
  endTime: dayjs.Dayjs | string;
  consultationMode: string;
  notes?: string | null;
}

export interface IBatchCreateSlotRequest {
  startDate: string; // "YYYY-MM-DD"
  endDate: string; // "YYYY-MM-DD"
  dailyStartTime: string; // "HH:mm"
  dailyEndTime: string; // "HH:mm"
  slotDurationMinutes: number; // 30, 45, 60
  daysOfWeek?: number[]; // [1, 2, 3, 4, 5]
  consultationMode: string;
  notes?: string | null;
}

@Injectable({ providedIn: 'root' })
export class DoctorScheduleService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${serverApiUrl}api/doctor`;

  getMySlots(startDate?: dayjs.Dayjs | string, endDate?: dayjs.Dayjs | string): Observable<IAvailabilitySlot[]> {
    let params = new HttpParams();
    if (startDate) {
      const startStr = typeof startDate === 'string' ? startDate : startDate.toISOString();
      params = params.set('startDate', startStr);
    }
    if (endDate) {
      const endStr = typeof endDate === 'string' ? endDate : endDate.toISOString();
      params = params.set('endDate', endStr);
    }

    return this.http
      .get<RestAvailabilitySlot[]>(`${this.resourceUrl}/my-slots`, { params })
      .pipe(map(slots => slots.map(item => this.convertValueFromServer(item))));
  }

  createSlot(req: ICreateSlotRequest): Observable<IAvailabilitySlot> {
    const payload = {
      ...req,
      startTime: typeof req.startTime === 'string' ? req.startTime : req.startTime.toISOString(),
      endTime: typeof req.endTime === 'string' ? req.endTime : req.endTime.toISOString(),
    };
    return this.http
      .post<RestAvailabilitySlot>(`${this.resourceUrl}/slots`, payload)
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  batchCreateSlots(req: IBatchCreateSlotRequest): Observable<IAvailabilitySlot[]> {
    return this.http
      .post<RestAvailabilitySlot[]>(`${this.resourceUrl}/slots/batch`, req)
      .pipe(map(slots => slots.map(item => this.convertValueFromServer(item))));
  }

  updateSlot(id: number, req: ICreateSlotRequest): Observable<IAvailabilitySlot> {
    const payload = {
      ...req,
      startTime: typeof req.startTime === 'string' ? req.startTime : req.startTime.toISOString(),
      endTime: typeof req.endTime === 'string' ? req.endTime : req.endTime.toISOString(),
    };
    return this.http
      .put<RestAvailabilitySlot>(`${this.resourceUrl}/slots/${id}`, payload)
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  toggleSlotStatus(id: number): Observable<IAvailabilitySlot> {
    return this.http
      .put<RestAvailabilitySlot>(`${this.resourceUrl}/slots/${id}/toggle-status`, {})
      .pipe(map(res => this.convertValueFromServer(res)));
  }

  deleteSlot(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/slots/${id}`);
  }

  getMySubscriptionQuota(): Observable<any> {
    return this.http.get<any>(`${this.resourceUrl}/subscription-quota`);
  }

  private convertValueFromServer(restSlot: RestAvailabilitySlot): IAvailabilitySlot {
    return {
      ...restSlot,
      startTime: restSlot.startTime ? dayjs(restSlot.startTime) : undefined,
      endTime: restSlot.endTime ? dayjs(restSlot.endTime) : undefined,
    };
  }
}
