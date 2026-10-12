import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Injectable, inject, signal, computed } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { ISpecialization, NewSpecialization } from '../specialization.model';

export type PartialUpdateSpecialization = Partial<ISpecialization> & Pick<ISpecialization, 'id'>;

@Injectable({ providedIn: 'root' })
export class SpecializationsService {
  readonly specializationsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly specializationsResource = httpResource<ISpecialization[]>(() => {
    const params = this.specializationsParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of specialization that have been fetched. It is updated when the specializationsResource emits a new value.
   * In case of error while fetching the specializations, the signal is set to an empty array.
   */
  readonly specializations = computed(() => (this.specializationsResource.hasValue() ? this.specializationsResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/specializations`;
}

@Injectable({ providedIn: 'root' })
export class SpecializationService extends SpecializationsService {
  protected readonly http = inject(HttpClient);

  create(specialization: NewSpecialization): Observable<ISpecialization> {
    return this.http.post<ISpecialization>(this.resourceUrl, specialization);
  }

  update(specialization: ISpecialization): Observable<ISpecialization> {
    return this.http.put<ISpecialization>(
      `${this.resourceUrl}/${encodeURIComponent(this.getSpecializationIdentifier(specialization))}`,
      specialization,
    );
  }

  partialUpdate(specialization: PartialUpdateSpecialization): Observable<ISpecialization> {
    return this.http.patch<ISpecialization>(
      `${this.resourceUrl}/${encodeURIComponent(this.getSpecializationIdentifier(specialization))}`,
      specialization,
    );
  }

  find(id: number): Observable<ISpecialization> {
    return this.http.get<ISpecialization>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<ISpecialization[]>> {
    const options = createRequestOption(req);
    return this.http.get<ISpecialization[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getSpecializationIdentifier(specialization: Pick<ISpecialization, 'id'>): number {
    return specialization.id;
  }

  compareSpecialization(o1: Pick<ISpecialization, 'id'> | null, o2: Pick<ISpecialization, 'id'> | null): boolean {
    return o1 && o2 ? this.getSpecializationIdentifier(o1) === this.getSpecializationIdentifier(o2) : o1 === o2;
  }

  addSpecializationToCollectionIfMissing<Type extends Pick<ISpecialization, 'id'>>(
    specializationCollection: Type[],
    ...specializationsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const specializations: Type[] = specializationsToCheck.filter(
      specializationItem => specializationItem !== null && specializationItem !== undefined,
    );
    if (specializations.length > 0) {
      const specializationCollectionIdentifiers = specializationCollection.map(specializationItem =>
        this.getSpecializationIdentifier(specializationItem),
      );
      const specializationsToAdd = specializations.filter(specializationItem => {
        const specializationIdentifier = this.getSpecializationIdentifier(specializationItem);
        if (specializationCollectionIdentifiers.includes(specializationIdentifier)) {
          return false;
        }
        specializationCollectionIdentifiers.push(specializationIdentifier);
        return true;
      });
      return [...specializationsToAdd, ...specializationCollection];
    }
    return specializationCollection;
  }
}
