import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../appointment.test-samples';

import { AppointmentFormService } from './appointment-form.service';

describe('Appointment Form Service', () => {
  let service: AppointmentFormService;

  beforeEach(() => {
    service = TestBed.inject(AppointmentFormService);
  });

  describe('Service methods', () => {
    describe('createAppointmentFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createAppointmentFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            bookingCode: expect.any(Object),
            scheduledStart: expect.any(Object),
            scheduledEnd: expect.any(Object),
            consultationMode: expect.any(Object),
            status: expect.any(Object),
            reason: expect.any(Object),
            cancellationReason: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            patient: expect.any(Object),
            doctor: expect.any(Object),
            slot: expect.any(Object),
          }),
        );
      });

      it('passing IAppointment should create a new form with FormGroup', () => {
        const formGroup = service.createAppointmentFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            bookingCode: expect.any(Object),
            scheduledStart: expect.any(Object),
            scheduledEnd: expect.any(Object),
            consultationMode: expect.any(Object),
            status: expect.any(Object),
            reason: expect.any(Object),
            cancellationReason: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            patient: expect.any(Object),
            doctor: expect.any(Object),
            slot: expect.any(Object),
          }),
        );
      });
    });

    describe('getAppointment', () => {
      it('should return NewAppointment for default Appointment initial value', () => {
        const formGroup = service.createAppointmentFormGroup(sampleWithNewData);

        const appointment = service.getAppointment(formGroup);

        expect(appointment).toMatchObject(sampleWithNewData);
      });

      it('should return NewAppointment for empty Appointment initial value', () => {
        const formGroup = service.createAppointmentFormGroup();

        const appointment = service.getAppointment(formGroup);

        expect(appointment).toMatchObject({});
      });

      it('should return IAppointment', () => {
        const formGroup = service.createAppointmentFormGroup(sampleWithRequiredData);

        const appointment = service.getAppointment(formGroup);

        expect(appointment).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IAppointment should not enable id FormControl', () => {
        const formGroup = service.createAppointmentFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewAppointment should disable id FormControl', () => {
        const formGroup = service.createAppointmentFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
