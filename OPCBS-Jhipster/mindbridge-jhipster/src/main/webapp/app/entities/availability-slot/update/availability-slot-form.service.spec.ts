import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../availability-slot.test-samples';

import { AvailabilitySlotFormService } from './availability-slot-form.service';

describe('AvailabilitySlot Form Service', () => {
  let service: AvailabilitySlotFormService;

  beforeEach(() => {
    service = TestBed.inject(AvailabilitySlotFormService);
  });

  describe('Service methods', () => {
    describe('createAvailabilitySlotFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createAvailabilitySlotFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            startTime: expect.any(Object),
            endTime: expect.any(Object),
            consultationMode: expect.any(Object),
            status: expect.any(Object),
            notes: expect.any(Object),
            doctor: expect.any(Object),
          }),
        );
      });

      it('passing IAvailabilitySlot should create a new form with FormGroup', () => {
        const formGroup = service.createAvailabilitySlotFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            startTime: expect.any(Object),
            endTime: expect.any(Object),
            consultationMode: expect.any(Object),
            status: expect.any(Object),
            notes: expect.any(Object),
            doctor: expect.any(Object),
          }),
        );
      });
    });

    describe('getAvailabilitySlot', () => {
      it('should return NewAvailabilitySlot for default AvailabilitySlot initial value', () => {
        const formGroup = service.createAvailabilitySlotFormGroup(sampleWithNewData);

        const availabilitySlot = service.getAvailabilitySlot(formGroup);

        expect(availabilitySlot).toMatchObject(sampleWithNewData);
      });

      it('should return NewAvailabilitySlot for empty AvailabilitySlot initial value', () => {
        const formGroup = service.createAvailabilitySlotFormGroup();

        const availabilitySlot = service.getAvailabilitySlot(formGroup);

        expect(availabilitySlot).toMatchObject({});
      });

      it('should return IAvailabilitySlot', () => {
        const formGroup = service.createAvailabilitySlotFormGroup(sampleWithRequiredData);

        const availabilitySlot = service.getAvailabilitySlot(formGroup);

        expect(availabilitySlot).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IAvailabilitySlot should not enable id FormControl', () => {
        const formGroup = service.createAvailabilitySlotFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewAvailabilitySlot should disable id FormControl', () => {
        const formGroup = service.createAvailabilitySlotFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
