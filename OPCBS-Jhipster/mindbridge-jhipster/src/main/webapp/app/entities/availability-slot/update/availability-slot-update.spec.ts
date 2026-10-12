import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { IAvailabilitySlot } from '../availability-slot.model';
import { AvailabilitySlotService } from '../service/availability-slot.service';

import { AvailabilitySlotFormService } from './availability-slot-form.service';
import { AvailabilitySlotUpdate } from './availability-slot-update';

describe('AvailabilitySlot Management Update Component', () => {
  let comp: AvailabilitySlotUpdate;
  let fixture: ComponentFixture<AvailabilitySlotUpdate>;
  let activatedRoute: ActivatedRoute;
  let availabilitySlotFormService: AvailabilitySlotFormService;
  let availabilitySlotService: AvailabilitySlotService;
  let doctorProfileService: DoctorProfileService;

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

    fixture = TestBed.createComponent(AvailabilitySlotUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    availabilitySlotFormService = TestBed.inject(AvailabilitySlotFormService);
    availabilitySlotService = TestBed.inject(AvailabilitySlotService);
    doctorProfileService = TestBed.inject(DoctorProfileService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call DoctorProfile query and add missing value', () => {
      const availabilitySlot: IAvailabilitySlot = { id: 922 };
      const doctor: IDoctorProfile = { id: 27873 };
      availabilitySlot.doctor = doctor;

      const doctorProfileCollection: IDoctorProfile[] = [{ id: 27873 }];
      vi.spyOn(doctorProfileService, 'query').mockReturnValue(of(new HttpResponse({ body: doctorProfileCollection })));
      const additionalDoctorProfiles = [doctor];
      const expectedCollection: IDoctorProfile[] = [...additionalDoctorProfiles, ...doctorProfileCollection];
      vi.spyOn(doctorProfileService, 'addDoctorProfileToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ availabilitySlot });
      comp.ngOnInit();

      expect(doctorProfileService.query).toHaveBeenCalled();
      expect(doctorProfileService.addDoctorProfileToCollectionIfMissing).toHaveBeenCalledWith(
        doctorProfileCollection,
        ...additionalDoctorProfiles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.doctorProfilesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const availabilitySlot: IAvailabilitySlot = { id: 922 };
      const doctor: IDoctorProfile = { id: 27873 };
      availabilitySlot.doctor = doctor;

      activatedRoute.data = of({ availabilitySlot });
      comp.ngOnInit();

      expect(comp.doctorProfilesSharedCollection()).toContainEqual(doctor);
      expect(comp.availabilitySlot).toEqual(availabilitySlot);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IAvailabilitySlot>();
      const availabilitySlot = { id: 19542 };
      vi.spyOn(availabilitySlotFormService, 'getAvailabilitySlot').mockReturnValue(availabilitySlot);
      vi.spyOn(availabilitySlotService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ availabilitySlot });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(availabilitySlot);
      saveSubject.complete();

      // THEN
      expect(availabilitySlotFormService.getAvailabilitySlot).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(availabilitySlotService.update).toHaveBeenCalledWith(expect.objectContaining(availabilitySlot));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IAvailabilitySlot>();
      const availabilitySlot = { id: 19542 };
      vi.spyOn(availabilitySlotFormService, 'getAvailabilitySlot').mockReturnValue({ id: null });
      vi.spyOn(availabilitySlotService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ availabilitySlot: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(availabilitySlot);
      saveSubject.complete();

      // THEN
      expect(availabilitySlotFormService.getAvailabilitySlot).toHaveBeenCalled();
      expect(availabilitySlotService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IAvailabilitySlot>();
      const availabilitySlot = { id: 19542 };
      vi.spyOn(availabilitySlotService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ availabilitySlot });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(availabilitySlotService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareDoctorProfile', () => {
      it('should forward to doctorProfileService', () => {
        const entity = { id: 27873 };
        const entity2 = { id: 7088 };
        vi.spyOn(doctorProfileService, 'compareDoctorProfile');
        comp.compareDoctorProfile(entity, entity2);
        expect(doctorProfileService.compareDoctorProfile).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
