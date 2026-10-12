import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IDoctorProfile } from '../doctor-profile.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../doctor-profile.test-samples';

import { DoctorProfileService } from './doctor-profile.service';

const requireRestSample: IDoctorProfile = {
  ...sampleWithRequiredData,
};

describe('DoctorProfile Service', () => {
  let service: DoctorProfileService;
  let httpMock: HttpTestingController;
  let expectedResult: IDoctorProfile | IDoctorProfile[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(DoctorProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a DoctorProfile', () => {
      const doctorProfile = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(doctorProfile).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a DoctorProfile', () => {
      const doctorProfile = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(doctorProfile).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a DoctorProfile', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of DoctorProfile', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a DoctorProfile', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addDoctorProfileToCollectionIfMissing', () => {
      it('should add a DoctorProfile to an empty array', () => {
        const doctorProfile: IDoctorProfile = sampleWithRequiredData;
        expectedResult = service.addDoctorProfileToCollectionIfMissing([], doctorProfile);
        expect(expectedResult).toEqual([doctorProfile]);
      });

      it('should not add a DoctorProfile to an array that contains it', () => {
        const doctorProfile: IDoctorProfile = sampleWithRequiredData;
        const doctorProfileCollection: IDoctorProfile[] = [
          {
            ...doctorProfile,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addDoctorProfileToCollectionIfMissing(doctorProfileCollection, doctorProfile);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a DoctorProfile to an array that doesn't contain it", () => {
        const doctorProfile: IDoctorProfile = sampleWithRequiredData;
        const doctorProfileCollection: IDoctorProfile[] = [sampleWithPartialData];
        expectedResult = service.addDoctorProfileToCollectionIfMissing(doctorProfileCollection, doctorProfile);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(doctorProfile);
      });

      it('should add only unique DoctorProfile to an array', () => {
        const doctorProfileArray: IDoctorProfile[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const doctorProfileCollection: IDoctorProfile[] = [sampleWithRequiredData];
        expectedResult = service.addDoctorProfileToCollectionIfMissing(doctorProfileCollection, ...doctorProfileArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const doctorProfile: IDoctorProfile = sampleWithRequiredData;
        const doctorProfile2: IDoctorProfile = sampleWithPartialData;
        expectedResult = service.addDoctorProfileToCollectionIfMissing([], doctorProfile, doctorProfile2);
        expect(expectedResult).toEqual([doctorProfile, doctorProfile2]);
      });

      it('should accept null and undefined values', () => {
        const doctorProfile: IDoctorProfile = sampleWithRequiredData;
        expectedResult = service.addDoctorProfileToCollectionIfMissing([], null, doctorProfile, undefined);
        expect(expectedResult).toEqual([doctorProfile]);
      });

      it('should return initial array if no DoctorProfile is added', () => {
        const doctorProfileCollection: IDoctorProfile[] = [sampleWithRequiredData];
        expectedResult = service.addDoctorProfileToCollectionIfMissing(doctorProfileCollection, undefined, null);
        expect(expectedResult).toEqual(doctorProfileCollection);
      });
    });

    describe('compareDoctorProfile', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareDoctorProfile(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 27873 };
        const entity2 = null;

        const compareResult1 = service.compareDoctorProfile(entity1, entity2);
        const compareResult2 = service.compareDoctorProfile(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 27873 };
        const entity2 = { id: 7088 };

        const compareResult1 = service.compareDoctorProfile(entity1, entity2);
        const compareResult2 = service.compareDoctorProfile(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 27873 };
        const entity2 = { id: 27873 };

        const compareResult1 = service.compareDoctorProfile(entity1, entity2);
        const compareResult2 = service.compareDoctorProfile(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
