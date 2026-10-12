import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';

import { DATE_FORMAT, serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IPatientProfile, NewPatientProfile } from '../patient-profile.model';

export type PartialUpdatePatientProfile = Partial<IPatientProfile> & Pick<IPatientProfile, 'id'>;

type RestOf<T extends IPatientProfile | NewPatientProfile> = Omit<T, 'dateOfBirth'> & {
  dateOfBirth?: string | null;
};

export type RestPatientProfile = RestOf<IPatientProfile>;

export type NewRestPatientProfile = RestOf<NewPatientProfile>;

export type PartialUpdateRestPatientProfile = RestOf<PartialUpdatePatientProfile>;

@Service()
export class PatientProfilesService {
  readonly patientProfilesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly patientProfilesResource = httpResource<RestPatientProfile[]>(() => {
    const params = this.patientProfilesParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of patientProfile that have been fetched. It is updated when the patientProfilesResource emits a new value.
   * In case of error while fetching the patientProfiles, the signal is set to an empty array.
   */
  readonly patientProfiles = computed(() =>
    (this.patientProfilesResource.hasValue() ? this.patientProfilesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/patient-profiles`;

  protected convertValueFromServer(restPatientProfile: RestPatientProfile): IPatientProfile {
    return {
      ...restPatientProfile,
      dateOfBirth: restPatientProfile.dateOfBirth ? dayjs(restPatientProfile.dateOfBirth) : undefined,
    };
  }
}

@Service()
export class PatientProfileService extends PatientProfilesService {
  protected readonly http = inject(HttpClient);

  create(patientProfile: NewPatientProfile): Observable<IPatientProfile> {
    const copy = this.convertValueFromClient(patientProfile);
    return this.http.post<RestPatientProfile>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(patientProfile: IPatientProfile): Observable<IPatientProfile> {
    const copy = this.convertValueFromClient(patientProfile);
    return this.http
      .put<RestPatientProfile>(`${this.resourceUrl}/${encodeURIComponent(this.getPatientProfileIdentifier(patientProfile))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(patientProfile: PartialUpdatePatientProfile): Observable<IPatientProfile> {
    const copy = this.convertValueFromClient(patientProfile);
    return this.http
      .patch<RestPatientProfile>(`${this.resourceUrl}/${encodeURIComponent(this.getPatientProfileIdentifier(patientProfile))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IPatientProfile> {
    return this.http
      .get<RestPatientProfile>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IPatientProfile[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestPatientProfile[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getPatientProfileIdentifier(patientProfile: Pick<IPatientProfile, 'id'>): number {
    return patientProfile.id;
  }

  comparePatientProfile(o1: Pick<IPatientProfile, 'id'> | null, o2: Pick<IPatientProfile, 'id'> | null): boolean {
    return o1 && o2 ? this.getPatientProfileIdentifier(o1) === this.getPatientProfileIdentifier(o2) : o1 === o2;
  }

  addPatientProfileToCollectionIfMissing<Type extends Pick<IPatientProfile, 'id'>>(
    patientProfileCollection: Type[],
    ...patientProfilesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const patientProfiles: Type[] = patientProfilesToCheck.filter(
      patientProfileItem => patientProfileItem !== null && patientProfileItem !== undefined,
    );
    if (patientProfiles.length > 0) {
      const patientProfileCollectionIdentifiers = patientProfileCollection.map(patientProfileItem =>
        this.getPatientProfileIdentifier(patientProfileItem),
      );
      const patientProfilesToAdd = patientProfiles.filter(patientProfileItem => {
        const patientProfileIdentifier = this.getPatientProfileIdentifier(patientProfileItem);
        if (patientProfileCollectionIdentifiers.includes(patientProfileIdentifier)) {
          return false;
        }
        patientProfileCollectionIdentifiers.push(patientProfileIdentifier);
        return true;
      });
      return [...patientProfilesToAdd, ...patientProfileCollection];
    }
    return patientProfileCollection;
  }

  protected convertValueFromClient<T extends IPatientProfile | NewPatientProfile | PartialUpdatePatientProfile>(
    patientProfile: T,
  ): RestOf<T> {
    return {
      ...patientProfile,
      dateOfBirth: patientProfile.dateOfBirth?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertResponseFromServer(res: RestPatientProfile): IPatientProfile {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestPatientProfile[]): IPatientProfile[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
