import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IDoctorProfile, NewDoctorProfile } from '../doctor-profile.model';

export type PartialUpdateDoctorProfile = Partial<IDoctorProfile> & Pick<IDoctorProfile, 'id'>;

@Service()
export class DoctorProfilesService {
  readonly doctorProfilesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly doctorProfilesResource = httpResource<IDoctorProfile[]>(() => {
    const params = this.doctorProfilesParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of doctorProfile that have been fetched. It is updated when the doctorProfilesResource emits a new value.
   * In case of error while fetching the doctorProfiles, the signal is set to an empty array.
   */
  readonly doctorProfiles = computed(() => (this.doctorProfilesResource.hasValue() ? this.doctorProfilesResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/doctor-profiles`;
}

@Service()
export class DoctorProfileService extends DoctorProfilesService {
  protected readonly http = inject(HttpClient);

  create(doctorProfile: NewDoctorProfile): Observable<IDoctorProfile> {
    return this.http.post<IDoctorProfile>(this.resourceUrl, doctorProfile);
  }

  update(doctorProfile: IDoctorProfile): Observable<IDoctorProfile> {
    return this.http.put<IDoctorProfile>(
      `${this.resourceUrl}/${encodeURIComponent(this.getDoctorProfileIdentifier(doctorProfile))}`,
      doctorProfile,
    );
  }

  partialUpdate(doctorProfile: PartialUpdateDoctorProfile): Observable<IDoctorProfile> {
    return this.http.patch<IDoctorProfile>(
      `${this.resourceUrl}/${encodeURIComponent(this.getDoctorProfileIdentifier(doctorProfile))}`,
      doctorProfile,
    );
  }

  find(id: number): Observable<IDoctorProfile> {
    return this.http.get<IDoctorProfile>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IDoctorProfile[]>> {
    const options = createRequestOption(req);
    return this.http.get<IDoctorProfile[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getDoctorProfileIdentifier(doctorProfile: Pick<IDoctorProfile, 'id'>): number {
    return doctorProfile.id;
  }

  compareDoctorProfile(o1: Pick<IDoctorProfile, 'id'> | null, o2: Pick<IDoctorProfile, 'id'> | null): boolean {
    return o1 && o2 ? this.getDoctorProfileIdentifier(o1) === this.getDoctorProfileIdentifier(o2) : o1 === o2;
  }

  addDoctorProfileToCollectionIfMissing<Type extends Pick<IDoctorProfile, 'id'>>(
    doctorProfileCollection: Type[],
    ...doctorProfilesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const doctorProfiles: Type[] = doctorProfilesToCheck.filter(
      doctorProfileItem => doctorProfileItem !== null && doctorProfileItem !== undefined,
    );
    if (doctorProfiles.length > 0) {
      const doctorProfileCollectionIdentifiers = doctorProfileCollection.map(doctorProfileItem =>
        this.getDoctorProfileIdentifier(doctorProfileItem),
      );
      const doctorProfilesToAdd = doctorProfiles.filter(doctorProfileItem => {
        const doctorProfileIdentifier = this.getDoctorProfileIdentifier(doctorProfileItem);
        if (doctorProfileCollectionIdentifiers.includes(doctorProfileIdentifier)) {
          return false;
        }
        doctorProfileCollectionIdentifiers.push(doctorProfileIdentifier);
        return true;
      });
      return [...doctorProfilesToAdd, ...doctorProfileCollection];
    }
    return doctorProfileCollection;
  }
}
