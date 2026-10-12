import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../specialization.test-samples';

import { SpecializationFormService } from './specialization-form.service';

describe('Specialization Form Service', () => {
  let service: SpecializationFormService;

  beforeEach(() => {
    service = TestBed.inject(SpecializationFormService);
  });

  describe('Service methods', () => {
    describe('createSpecializationFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createSpecializationFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            description: expect.any(Object),
            doctorses: expect.any(Object),
          }),
        );
      });

      it('passing ISpecialization should create a new form with FormGroup', () => {
        const formGroup = service.createSpecializationFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            description: expect.any(Object),
            doctorses: expect.any(Object),
          }),
        );
      });
    });

    describe('getSpecialization', () => {
      it('should return NewSpecialization for default Specialization initial value', () => {
        const formGroup = service.createSpecializationFormGroup(sampleWithNewData);

        const specialization = service.getSpecialization(formGroup);

        expect(specialization).toMatchObject(sampleWithNewData);
      });

      it('should return NewSpecialization for empty Specialization initial value', () => {
        const formGroup = service.createSpecializationFormGroup();

        const specialization = service.getSpecialization(formGroup);

        expect(specialization).toMatchObject({});
      });

      it('should return ISpecialization', () => {
        const formGroup = service.createSpecializationFormGroup(sampleWithRequiredData);

        const specialization = service.getSpecialization(formGroup);

        expect(specialization).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing ISpecialization should not enable id FormControl', () => {
        const formGroup = service.createSpecializationFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewSpecialization should disable id FormControl', () => {
        const formGroup = service.createSpecializationFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
