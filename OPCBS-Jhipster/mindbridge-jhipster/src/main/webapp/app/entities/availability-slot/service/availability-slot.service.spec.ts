import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IAvailabilitySlot } from '../availability-slot.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../availability-slot.test-samples';

import { AvailabilitySlotService, RestAvailabilitySlot } from './availability-slot.service';

const requireRestSample: RestAvailabilitySlot = {
  ...sampleWithRequiredData,
  startTime: sampleWithRequiredData.startTime?.toJSON(),
  endTime: sampleWithRequiredData.endTime?.toJSON(),
};

describe('AvailabilitySlot Service', () => {
  let service: AvailabilitySlotService;
  let httpMock: HttpTestingController;
  let expectedResult: IAvailabilitySlot | IAvailabilitySlot[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(AvailabilitySlotService);
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

    it('should create a AvailabilitySlot', () => {
      const availabilitySlot = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(availabilitySlot).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a AvailabilitySlot', () => {
      const availabilitySlot = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(availabilitySlot).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a AvailabilitySlot', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of AvailabilitySlot', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a AvailabilitySlot', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addAvailabilitySlotToCollectionIfMissing', () => {
      it('should add a AvailabilitySlot to an empty array', () => {
        const availabilitySlot: IAvailabilitySlot = sampleWithRequiredData;
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing([], availabilitySlot);
        expect(expectedResult).toEqual([availabilitySlot]);
      });

      it('should not add a AvailabilitySlot to an array that contains it', () => {
        const availabilitySlot: IAvailabilitySlot = sampleWithRequiredData;
        const availabilitySlotCollection: IAvailabilitySlot[] = [
          {
            ...availabilitySlot,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing(availabilitySlotCollection, availabilitySlot);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a AvailabilitySlot to an array that doesn't contain it", () => {
        const availabilitySlot: IAvailabilitySlot = sampleWithRequiredData;
        const availabilitySlotCollection: IAvailabilitySlot[] = [sampleWithPartialData];
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing(availabilitySlotCollection, availabilitySlot);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(availabilitySlot);
      });

      it('should add only unique AvailabilitySlot to an array', () => {
        const availabilitySlotArray: IAvailabilitySlot[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const availabilitySlotCollection: IAvailabilitySlot[] = [sampleWithRequiredData];
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing(availabilitySlotCollection, ...availabilitySlotArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const availabilitySlot: IAvailabilitySlot = sampleWithRequiredData;
        const availabilitySlot2: IAvailabilitySlot = sampleWithPartialData;
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing([], availabilitySlot, availabilitySlot2);
        expect(expectedResult).toEqual([availabilitySlot, availabilitySlot2]);
      });

      it('should accept null and undefined values', () => {
        const availabilitySlot: IAvailabilitySlot = sampleWithRequiredData;
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing([], null, availabilitySlot, undefined);
        expect(expectedResult).toEqual([availabilitySlot]);
      });

      it('should return initial array if no AvailabilitySlot is added', () => {
        const availabilitySlotCollection: IAvailabilitySlot[] = [sampleWithRequiredData];
        expectedResult = service.addAvailabilitySlotToCollectionIfMissing(availabilitySlotCollection, undefined, null);
        expect(expectedResult).toEqual(availabilitySlotCollection);
      });
    });

    describe('compareAvailabilitySlot', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareAvailabilitySlot(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 19542 };
        const entity2 = null;

        const compareResult1 = service.compareAvailabilitySlot(entity1, entity2);
        const compareResult2 = service.compareAvailabilitySlot(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 19542 };
        const entity2 = { id: 922 };

        const compareResult1 = service.compareAvailabilitySlot(entity1, entity2);
        const compareResult2 = service.compareAvailabilitySlot(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 19542 };
        const entity2 = { id: 19542 };

        const compareResult1 = service.compareAvailabilitySlot(entity1, entity2);
        const compareResult2 = service.compareAvailabilitySlot(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
