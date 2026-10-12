import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IAppointment, NewAppointment } from '../appointment.model';

export type PartialUpdateAppointment = Partial<IAppointment> & Pick<IAppointment, 'id'>;

type RestOf<T extends IAppointment | NewAppointment> = Omit<
  T,
  'scheduledStart' | 'scheduledEnd' | 'createdAt' | 'updatedAt' | 'nextAppointmentRecommendedDate' | 'completedAt'
> & {
  scheduledStart?: string | null;
  scheduledEnd?: string | null;
  nextAppointmentRecommendedDate?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestAppointment = RestOf<IAppointment>;

export type NewRestAppointment = RestOf<NewAppointment>;

export type PartialUpdateRestAppointment = RestOf<PartialUpdateAppointment>;

@Injectable({ providedIn: 'root' })
export class AppointmentsService {
  readonly appointmentsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly appointmentsResource = httpResource<RestAppointment[]>(() => {
    const params = this.appointmentsParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of appointment that have been fetched. It is updated when the appointmentsResource emits a new value.
   * In case of error while fetching the appointments, the signal is set to an empty array.
   */
  readonly appointments = computed(() =>
    (this.appointmentsResource.hasValue() ? this.appointmentsResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/appointments`;

  protected convertValueFromServer(restAppointment: RestAppointment): IAppointment {
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

@Injectable({ providedIn: 'root' })
export class AppointmentService extends AppointmentsService {
  protected readonly http = inject(HttpClient);

  create(appointment: NewAppointment): Observable<IAppointment> {
    const copy = this.convertValueFromClient(appointment);
    return this.http.post<RestAppointment>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(appointment: IAppointment): Observable<IAppointment> {
    const copy = this.convertValueFromClient(appointment);
    return this.http
      .put<RestAppointment>(`${this.resourceUrl}/${encodeURIComponent(this.getAppointmentIdentifier(appointment))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(appointment: PartialUpdateAppointment): Observable<IAppointment> {
    const copy = this.convertValueFromClient(appointment);
    return this.http
      .patch<RestAppointment>(`${this.resourceUrl}/${encodeURIComponent(this.getAppointmentIdentifier(appointment))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IAppointment> {
    return this.http
      .get<RestAppointment>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IAppointment[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestAppointment[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getAppointmentIdentifier(appointment: Pick<IAppointment, 'id'>): number {
    return appointment.id;
  }

  compareAppointment(o1: Pick<IAppointment, 'id'> | null, o2: Pick<IAppointment, 'id'> | null): boolean {
    return o1 && o2 ? this.getAppointmentIdentifier(o1) === this.getAppointmentIdentifier(o2) : o1 === o2;
  }

  addAppointmentToCollectionIfMissing<Type extends Pick<IAppointment, 'id'>>(
    appointmentCollection: Type[],
    ...appointmentsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const appointments: Type[] = appointmentsToCheck.filter(appointmentItem => appointmentItem !== null && appointmentItem !== undefined);
    if (appointments.length > 0) {
      const appointmentCollectionIdentifiers = appointmentCollection.map(appointmentItem => this.getAppointmentIdentifier(appointmentItem));
      const appointmentsToAdd = appointments.filter(appointmentItem => {
        const appointmentIdentifier = this.getAppointmentIdentifier(appointmentItem);
        if (appointmentCollectionIdentifiers.includes(appointmentIdentifier)) {
          return false;
        }
        appointmentCollectionIdentifiers.push(appointmentIdentifier);
        return true;
      });
      return [...appointmentsToAdd, ...appointmentCollection];
    }
    return appointmentCollection;
  }

  protected convertValueFromClient<T extends IAppointment | NewAppointment | PartialUpdateAppointment>(appointment: T): RestOf<T> {
    return {
      ...appointment,
      scheduledStart: appointment.scheduledStart?.toJSON() ?? null,
      scheduledEnd: appointment.scheduledEnd?.toJSON() ?? null,
      nextAppointmentRecommendedDate: appointment.nextAppointmentRecommendedDate?.toJSON() ?? null,
      completedAt: appointment.completedAt?.toJSON() ?? null,
      createdAt: appointment.createdAt?.toJSON() ?? null,
      updatedAt: appointment.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestAppointment): IAppointment {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestAppointment[]): IAppointment[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
