import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ISpecialization } from '../specialization.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../specialization.test-samples';

import { SpecializationService } from './specialization.service';

const requireRestSample: ISpecialization = {
  ...sampleWithRequiredData,
};

describe('Specialization Service', () => {
  let service: SpecializationService;
  let httpMock: HttpTestingController;
  let expectedResult: ISpecialization | ISpecialization[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(SpecializationService);
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

    it('should create a Specialization', () => {
      const specialization = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(specialization).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Specialization', () => {
      const specialization = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(specialization).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Specialization', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Specialization', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Specialization', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addSpecializationToCollectionIfMissing', () => {
      it('should add a Specialization to an empty array', () => {
        const specialization: ISpecialization = sampleWithRequiredData;
        expectedResult = service.addSpecializationToCollectionIfMissing([], specialization);
        expect(expectedResult).toEqual([specialization]);
      });

      it('should not add a Specialization to an array that contains it', () => {
        const specialization: ISpecialization = sampleWithRequiredData;
        const specializationCollection: ISpecialization[] = [
          {
            ...specialization,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addSpecializationToCollectionIfMissing(specializationCollection, specialization);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Specialization to an array that doesn't contain it", () => {
        const specialization: ISpecialization = sampleWithRequiredData;
        const specializationCollection: ISpecialization[] = [sampleWithPartialData];
        expectedResult = service.addSpecializationToCollectionIfMissing(specializationCollection, specialization);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(specialization);
      });

      it('should add only unique Specialization to an array', () => {
        const specializationArray: ISpecialization[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const specializationCollection: ISpecialization[] = [sampleWithRequiredData];
        expectedResult = service.addSpecializationToCollectionIfMissing(specializationCollection, ...specializationArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const specialization: ISpecialization = sampleWithRequiredData;
        const specialization2: ISpecialization = sampleWithPartialData;
        expectedResult = service.addSpecializationToCollectionIfMissing([], specialization, specialization2);
        expect(expectedResult).toEqual([specialization, specialization2]);
      });

      it('should accept null and undefined values', () => {
        const specialization: ISpecialization = sampleWithRequiredData;
        expectedResult = service.addSpecializationToCollectionIfMissing([], null, specialization, undefined);
        expect(expectedResult).toEqual([specialization]);
      });

      it('should return initial array if no Specialization is added', () => {
        const specializationCollection: ISpecialization[] = [sampleWithRequiredData];
        expectedResult = service.addSpecializationToCollectionIfMissing(specializationCollection, undefined, null);
        expect(expectedResult).toEqual(specializationCollection);
      });
    });

    describe('compareSpecialization', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareSpecialization(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 29993 };
        const entity2 = null;

        const compareResult1 = service.compareSpecialization(entity1, entity2);
        const compareResult2 = service.compareSpecialization(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 29993 };
        const entity2 = { id: 30083 };

        const compareResult1 = service.compareSpecialization(entity1, entity2);
        const compareResult2 = service.compareSpecialization(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 29993 };
        const entity2 = { id: 29993 };

        const compareResult1 = service.compareSpecialization(entity1, entity2);
        const compareResult2 = service.compareSpecialization(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
