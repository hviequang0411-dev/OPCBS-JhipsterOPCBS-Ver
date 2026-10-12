import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DATE_FORMAT } from 'app/config';
import { IPatientProfile } from '../patient-profile.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../patient-profile.test-samples';

import { PatientProfileService, RestPatientProfile } from './patient-profile.service';

const requireRestSample: RestPatientProfile = {
  ...sampleWithRequiredData,
  dateOfBirth: sampleWithRequiredData.dateOfBirth?.format(DATE_FORMAT),
};

describe('PatientProfile Service', () => {
  let service: PatientProfileService;
  let httpMock: HttpTestingController;
  let expectedResult: IPatientProfile | IPatientProfile[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(PatientProfileService);
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

    it('should create a PatientProfile', () => {
      const patientProfile = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(patientProfile).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a PatientProfile', () => {
      const patientProfile = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(patientProfile).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a PatientProfile', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of PatientProfile', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a PatientProfile', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addPatientProfileToCollectionIfMissing', () => {
      it('should add a PatientProfile to an empty array', () => {
        const patientProfile: IPatientProfile = sampleWithRequiredData;
        expectedResult = service.addPatientProfileToCollectionIfMissing([], patientProfile);
        expect(expectedResult).toEqual([patientProfile]);
      });

      it('should not add a PatientProfile to an array that contains it', () => {
        const patientProfile: IPatientProfile = sampleWithRequiredData;
        const patientProfileCollection: IPatientProfile[] = [
          {
            ...patientProfile,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addPatientProfileToCollectionIfMissing(patientProfileCollection, patientProfile);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a PatientProfile to an array that doesn't contain it", () => {
        const patientProfile: IPatientProfile = sampleWithRequiredData;
        const patientProfileCollection: IPatientProfile[] = [sampleWithPartialData];
        expectedResult = service.addPatientProfileToCollectionIfMissing(patientProfileCollection, patientProfile);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(patientProfile);
      });

      it('should add only unique PatientProfile to an array', () => {
        const patientProfileArray: IPatientProfile[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const patientProfileCollection: IPatientProfile[] = [sampleWithRequiredData];
        expectedResult = service.addPatientProfileToCollectionIfMissing(patientProfileCollection, ...patientProfileArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const patientProfile: IPatientProfile = sampleWithRequiredData;
        const patientProfile2: IPatientProfile = sampleWithPartialData;
        expectedResult = service.addPatientProfileToCollectionIfMissing([], patientProfile, patientProfile2);
        expect(expectedResult).toEqual([patientProfile, patientProfile2]);
      });

      it('should accept null and undefined values', () => {
        const patientProfile: IPatientProfile = sampleWithRequiredData;
        expectedResult = service.addPatientProfileToCollectionIfMissing([], null, patientProfile, undefined);
        expect(expectedResult).toEqual([patientProfile]);
      });

      it('should return initial array if no PatientProfile is added', () => {
        const patientProfileCollection: IPatientProfile[] = [sampleWithRequiredData];
        expectedResult = service.addPatientProfileToCollectionIfMissing(patientProfileCollection, undefined, null);
        expect(expectedResult).toEqual(patientProfileCollection);
      });
    });

    describe('comparePatientProfile', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.comparePatientProfile(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 11369 };
        const entity2 = null;

        const compareResult1 = service.comparePatientProfile(entity1, entity2);
        const compareResult2 = service.comparePatientProfile(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 11369 };
        const entity2 = { id: 30515 };

        const compareResult1 = service.comparePatientProfile(entity1, entity2);
        const compareResult2 = service.comparePatientProfile(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 11369 };
        const entity2 = { id: 11369 };

        const compareResult1 = service.comparePatientProfile(entity1, entity2);
        const compareResult2 = service.comparePatientProfile(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
