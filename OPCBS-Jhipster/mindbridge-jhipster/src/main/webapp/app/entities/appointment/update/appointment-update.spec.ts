import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { IAvailabilitySlot } from 'app/entities/availability-slot/availability-slot.model';
import { AvailabilitySlotService } from 'app/entities/availability-slot/service/availability-slot.service';
import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';
import { PatientProfileService } from 'app/entities/patient-profile/service/patient-profile.service';
import { IAppointment } from '../appointment.model';
import { AppointmentService } from '../service/appointment.service';

import { AppointmentFormService } from './appointment-form.service';
import { AppointmentUpdate } from './appointment-update';

describe('Appointment Management Update Component', () => {
  let comp: AppointmentUpdate;
  let fixture: ComponentFixture<AppointmentUpdate>;
  let activatedRoute: ActivatedRoute;
  let appointmentFormService: AppointmentFormService;
  let appointmentService: AppointmentService;
  let patientProfileService: PatientProfileService;
  let doctorProfileService: DoctorProfileService;
  let availabilitySlotService: AvailabilitySlotService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(AppointmentUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    appointmentFormService = TestBed.inject(AppointmentFormService);
    appointmentService = TestBed.inject(AppointmentService);
    patientProfileService = TestBed.inject(PatientProfileService);
    doctorProfileService = TestBed.inject(DoctorProfileService);
    availabilitySlotService = TestBed.inject(AvailabilitySlotService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call PatientProfile query and add missing value', () => {
      const appointment: IAppointment = { id: 584 };
      const patient: IPatientProfile = { id: 11369 };
      appointment.patient = patient;

      const patientProfileCollection: IPatientProfile[] = [{ id: 11369 }];
      vi.spyOn(patientProfileService, 'query').mockReturnValue(of(new HttpResponse({ body: patientProfileCollection })));
      const additionalPatientProfiles = [patient];
      const expectedCollection: IPatientProfile[] = [...additionalPatientProfiles, ...patientProfileCollection];
      vi.spyOn(patientProfileService, 'addPatientProfileToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      expect(patientProfileService.query).toHaveBeenCalled();
      expect(patientProfileService.addPatientProfileToCollectionIfMissing).toHaveBeenCalledWith(
        patientProfileCollection,
        ...additionalPatientProfiles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.patientProfilesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call DoctorProfile query and add missing value', () => {
      const appointment: IAppointment = { id: 584 };
      const doctor: IDoctorProfile = { id: 27873 };
      appointment.doctor = doctor;

      const doctorProfileCollection: IDoctorProfile[] = [{ id: 27873 }];
      vi.spyOn(doctorProfileService, 'query').mockReturnValue(of(new HttpResponse({ body: doctorProfileCollection })));
      const additionalDoctorProfiles = [doctor];
      const expectedCollection: IDoctorProfile[] = [...additionalDoctorProfiles, ...doctorProfileCollection];
      vi.spyOn(doctorProfileService, 'addDoctorProfileToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      expect(doctorProfileService.query).toHaveBeenCalled();
      expect(doctorProfileService.addDoctorProfileToCollectionIfMissing).toHaveBeenCalledWith(
        doctorProfileCollection,
        ...additionalDoctorProfiles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.doctorProfilesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call AvailabilitySlot query and add missing value', () => {
      const appointment: IAppointment = { id: 584 };
      const slot: IAvailabilitySlot = { id: 19542 };
      appointment.slot = slot;

      const availabilitySlotCollection: IAvailabilitySlot[] = [{ id: 19542 }];
      vi.spyOn(availabilitySlotService, 'query').mockReturnValue(of(new HttpResponse({ body: availabilitySlotCollection })));
      const additionalAvailabilitySlots = [slot];
      const expectedCollection: IAvailabilitySlot[] = [...additionalAvailabilitySlots, ...availabilitySlotCollection];
      vi.spyOn(availabilitySlotService, 'addAvailabilitySlotToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      expect(availabilitySlotService.query).toHaveBeenCalled();
      expect(availabilitySlotService.addAvailabilitySlotToCollectionIfMissing).toHaveBeenCalledWith(
        availabilitySlotCollection,
        ...additionalAvailabilitySlots.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.availabilitySlotsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const appointment: IAppointment = { id: 584 };
      const patient: IPatientProfile = { id: 11369 };
      appointment.patient = patient;
      const doctor: IDoctorProfile = { id: 27873 };
      appointment.doctor = doctor;
      const slot: IAvailabilitySlot = { id: 19542 };
      appointment.slot = slot;

      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      expect(comp.patientProfilesSharedCollection()).toContainEqual(patient);
      expect(comp.doctorProfilesSharedCollection()).toContainEqual(doctor);
      expect(comp.availabilitySlotsSharedCollection()).toContainEqual(slot);
      expect(comp.appointment).toEqual(appointment);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IAppointment>();
      const appointment = { id: 3011 };
      vi.spyOn(appointmentFormService, 'getAppointment').mockReturnValue(appointment);
      vi.spyOn(appointmentService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(appointment);
      saveSubject.complete();

      // THEN
      expect(appointmentFormService.getAppointment).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(appointmentService.update).toHaveBeenCalledWith(expect.objectContaining(appointment));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IAppointment>();
      const appointment = { id: 3011 };
      vi.spyOn(appointmentFormService, 'getAppointment').mockReturnValue({ id: null });
      vi.spyOn(appointmentService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ appointment: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(appointment);
      saveSubject.complete();

      // THEN
      expect(appointmentFormService.getAppointment).toHaveBeenCalled();
      expect(appointmentService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IAppointment>();
      const appointment = { id: 3011 };
      vi.spyOn(appointmentService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ appointment });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(appointmentService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('comparePatientProfile', () => {
      it('should forward to patientProfileService', () => {
        const entity = { id: 11369 };
        const entity2 = { id: 30515 };
        vi.spyOn(patientProfileService, 'comparePatientProfile');
        comp.comparePatientProfile(entity, entity2);
        expect(patientProfileService.comparePatientProfile).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareDoctorProfile', () => {
      it('should forward to doctorProfileService', () => {
        const entity = { id: 27873 };
        const entity2 = { id: 7088 };
        vi.spyOn(doctorProfileService, 'compareDoctorProfile');
        comp.compareDoctorProfile(entity, entity2);
        expect(doctorProfileService.compareDoctorProfile).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareAvailabilitySlot', () => {
      it('should forward to availabilitySlotService', () => {
        const entity = { id: 19542 };
        const entity2 = { id: 922 };
        vi.spyOn(availabilitySlotService, 'compareAvailabilitySlot');
        comp.compareAvailabilitySlot(entity, entity2);
        expect(availabilitySlotService.compareAvailabilitySlot).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
