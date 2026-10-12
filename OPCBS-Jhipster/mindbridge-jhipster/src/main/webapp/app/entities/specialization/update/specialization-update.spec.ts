import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { SpecializationService } from '../service/specialization.service';
import { ISpecialization } from '../specialization.model';

import { SpecializationFormService } from './specialization-form.service';
import { SpecializationUpdate } from './specialization-update';

describe('Specialization Management Update Component', () => {
  let comp: SpecializationUpdate;
  let fixture: ComponentFixture<SpecializationUpdate>;
  let activatedRoute: ActivatedRoute;
  let specializationFormService: SpecializationFormService;
  let specializationService: SpecializationService;
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

    fixture = TestBed.createComponent(SpecializationUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    specializationFormService = TestBed.inject(SpecializationFormService);
    specializationService = TestBed.inject(SpecializationService);
    doctorProfileService = TestBed.inject(DoctorProfileService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call DoctorProfile query and add missing value', () => {
      const specialization: ISpecialization = { id: 30083 };
      const doctorses: IDoctorProfile[] = [{ id: 27873 }];
      specialization.doctorses = doctorses;

      const doctorProfileCollection: IDoctorProfile[] = [{ id: 27873 }];
      vi.spyOn(doctorProfileService, 'query').mockReturnValue(of(new HttpResponse({ body: doctorProfileCollection })));
      const additionalDoctorProfiles = [...doctorses];
      const expectedCollection: IDoctorProfile[] = [...additionalDoctorProfiles, ...doctorProfileCollection];
      vi.spyOn(doctorProfileService, 'addDoctorProfileToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ specialization });
      comp.ngOnInit();

      expect(doctorProfileService.query).toHaveBeenCalled();
      expect(doctorProfileService.addDoctorProfileToCollectionIfMissing).toHaveBeenCalledWith(
        doctorProfileCollection,
        ...additionalDoctorProfiles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.doctorProfilesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const specialization: ISpecialization = { id: 30083 };
      const doctors: IDoctorProfile = { id: 27873 };
      specialization.doctorses = [doctors];

      activatedRoute.data = of({ specialization });
      comp.ngOnInit();

      expect(comp.doctorProfilesSharedCollection()).toContainEqual(doctors);
      expect(comp.specialization).toEqual(specialization);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ISpecialization>();
      const specialization = { id: 29993 };
      vi.spyOn(specializationFormService, 'getSpecialization').mockReturnValue(specialization);
      vi.spyOn(specializationService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ specialization });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(specialization);
      saveSubject.complete();

      // THEN
      expect(specializationFormService.getSpecialization).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(specializationService.update).toHaveBeenCalledWith(expect.objectContaining(specialization));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ISpecialization>();
      const specialization = { id: 29993 };
      vi.spyOn(specializationFormService, 'getSpecialization').mockReturnValue({ id: null });
      vi.spyOn(specializationService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ specialization: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(specialization);
      saveSubject.complete();

      // THEN
      expect(specializationFormService.getSpecialization).toHaveBeenCalled();
      expect(specializationService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ISpecialization>();
      const specialization = { id: 29993 };
      vi.spyOn(specializationService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ specialization });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(specializationService.update).toHaveBeenCalled();
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
