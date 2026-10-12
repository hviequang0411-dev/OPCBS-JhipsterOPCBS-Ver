import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IAvailabilitySlot, NewAvailabilitySlot } from '../availability-slot.model';

export type PartialUpdateAvailabilitySlot = Partial<IAvailabilitySlot> & Pick<IAvailabilitySlot, 'id'>;

type RestOf<T extends IAvailabilitySlot | NewAvailabilitySlot> = Omit<T, 'startTime' | 'endTime'> & {
  startTime?: string | null;
  endTime?: string | null;
};

export type RestAvailabilitySlot = RestOf<IAvailabilitySlot>;

export type NewRestAvailabilitySlot = RestOf<NewAvailabilitySlot>;

export type PartialUpdateRestAvailabilitySlot = RestOf<PartialUpdateAvailabilitySlot>;

@Service()
export class AvailabilitySlotsService {
  readonly availabilitySlotsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly availabilitySlotsResource = httpResource<RestAvailabilitySlot[]>(() => {
    const params = this.availabilitySlotsParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of availabilitySlot that have been fetched. It is updated when the availabilitySlotsResource emits a new value.
   * In case of error while fetching the availabilitySlots, the signal is set to an empty array.
   */
  readonly availabilitySlots = computed(() =>
    (this.availabilitySlotsResource.hasValue() ? this.availabilitySlotsResource.value() : []).map(item =>
      this.convertValueFromServer(item),
    ),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/availability-slots`;

  protected convertValueFromServer(restAvailabilitySlot: RestAvailabilitySlot): IAvailabilitySlot {
    return {
      ...restAvailabilitySlot,
      startTime: restAvailabilitySlot.startTime ? dayjs(restAvailabilitySlot.startTime) : undefined,
      endTime: restAvailabilitySlot.endTime ? dayjs(restAvailabilitySlot.endTime) : undefined,
    };
  }
}

@Service()
export class AvailabilitySlotService extends AvailabilitySlotsService {
  protected readonly http = inject(HttpClient);

  create(availabilitySlot: NewAvailabilitySlot): Observable<IAvailabilitySlot> {
    const copy = this.convertValueFromClient(availabilitySlot);
    return this.http.post<RestAvailabilitySlot>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(availabilitySlot: IAvailabilitySlot): Observable<IAvailabilitySlot> {
    const copy = this.convertValueFromClient(availabilitySlot);
    return this.http
      .put<RestAvailabilitySlot>(`${this.resourceUrl}/${encodeURIComponent(this.getAvailabilitySlotIdentifier(availabilitySlot))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(availabilitySlot: PartialUpdateAvailabilitySlot): Observable<IAvailabilitySlot> {
    const copy = this.convertValueFromClient(availabilitySlot);
    return this.http
      .patch<RestAvailabilitySlot>(`${this.resourceUrl}/${encodeURIComponent(this.getAvailabilitySlotIdentifier(availabilitySlot))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IAvailabilitySlot> {
    return this.http
      .get<RestAvailabilitySlot>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IAvailabilitySlot[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestAvailabilitySlot[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getAvailabilitySlotIdentifier(availabilitySlot: Pick<IAvailabilitySlot, 'id'>): number {
    return availabilitySlot.id;
  }

  compareAvailabilitySlot(o1: Pick<IAvailabilitySlot, 'id'> | null, o2: Pick<IAvailabilitySlot, 'id'> | null): boolean {
    return o1 && o2 ? this.getAvailabilitySlotIdentifier(o1) === this.getAvailabilitySlotIdentifier(o2) : o1 === o2;
  }

  addAvailabilitySlotToCollectionIfMissing<Type extends Pick<IAvailabilitySlot, 'id'>>(
    availabilitySlotCollection: Type[],
    ...availabilitySlotsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const availabilitySlots: Type[] = availabilitySlotsToCheck.filter(
      availabilitySlotItem => availabilitySlotItem !== null && availabilitySlotItem !== undefined,
    );
    if (availabilitySlots.length > 0) {
      const availabilitySlotCollectionIdentifiers = availabilitySlotCollection.map(availabilitySlotItem =>
        this.getAvailabilitySlotIdentifier(availabilitySlotItem),
      );
      const availabilitySlotsToAdd = availabilitySlots.filter(availabilitySlotItem => {
        const availabilitySlotIdentifier = this.getAvailabilitySlotIdentifier(availabilitySlotItem);
        if (availabilitySlotCollectionIdentifiers.includes(availabilitySlotIdentifier)) {
          return false;
        }
        availabilitySlotCollectionIdentifiers.push(availabilitySlotIdentifier);
        return true;
      });
      return [...availabilitySlotsToAdd, ...availabilitySlotCollection];
    }
    return availabilitySlotCollection;
  }

  protected convertValueFromClient<T extends IAvailabilitySlot | NewAvailabilitySlot | PartialUpdateAvailabilitySlot>(
    availabilitySlot: T,
  ): RestOf<T> {
    return {
      ...availabilitySlot,
      startTime: availabilitySlot.startTime?.toJSON() ?? null,
      endTime: availabilitySlot.endTime?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestAvailabilitySlot): IAvailabilitySlot {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestAvailabilitySlot[]): IAvailabilitySlot[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
