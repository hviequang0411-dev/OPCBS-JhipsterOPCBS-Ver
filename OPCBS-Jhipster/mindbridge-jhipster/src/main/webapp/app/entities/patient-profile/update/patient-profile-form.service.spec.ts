import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../patient-profile.test-samples';

import { PatientProfileFormService } from './patient-profile-form.service';

describe('PatientProfile Form Service', () => {
  let service: PatientProfileFormService;

  beforeEach(() => {
    service = TestBed.inject(PatientProfileFormService);
  });

  describe('Service methods', () => {
    describe('createPatientProfileFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createPatientProfileFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fullName: expect.any(Object),
            phone: expect.any(Object),
            dateOfBirth: expect.any(Object),
            gender: expect.any(Object),
            address: expect.any(Object),
            medicalHistory: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IPatientProfile should create a new form with FormGroup', () => {
        const formGroup = service.createPatientProfileFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fullName: expect.any(Object),
            phone: expect.any(Object),
            dateOfBirth: expect.any(Object),
            gender: expect.any(Object),
            address: expect.any(Object),
            medicalHistory: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getPatientProfile', () => {
      it('should return NewPatientProfile for default PatientProfile initial value', () => {
        const formGroup = service.createPatientProfileFormGroup(sampleWithNewData);

        const patientProfile = service.getPatientProfile(formGroup);

        expect(patientProfile).toMatchObject(sampleWithNewData);
      });

      it('should return NewPatientProfile for empty PatientProfile initial value', () => {
        const formGroup = service.createPatientProfileFormGroup();

        const patientProfile = service.getPatientProfile(formGroup);

        expect(patientProfile).toMatchObject({});
      });

      it('should return IPatientProfile', () => {
        const formGroup = service.createPatientProfileFormGroup(sampleWithRequiredData);

        const patientProfile = service.getPatientProfile(formGroup);

        expect(patientProfile).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IPatientProfile should not enable id FormControl', () => {
        const formGroup = service.createPatientProfileFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewPatientProfile should disable id FormControl', () => {
        const formGroup = service.createPatientProfileFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
