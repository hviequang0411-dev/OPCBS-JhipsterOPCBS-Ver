import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { SpecializationService } from 'app/entities/specialization/service/specialization.service';
import { ISpecialization } from 'app/entities/specialization/specialization.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { IDoctorProfile } from '../doctor-profile.model';
import { DoctorProfileService } from '../service/doctor-profile.service';

import { DoctorProfileFormService } from './doctor-profile-form.service';
import { DoctorProfileUpdate } from './doctor-profile-update';

describe('DoctorProfile Management Update Component', () => {
  let comp: DoctorProfileUpdate;
  let fixture: ComponentFixture<DoctorProfileUpdate>;
  let activatedRoute: ActivatedRoute;
  let doctorProfileFormService: DoctorProfileFormService;
  let doctorProfileService: DoctorProfileService;
  let userService: UserService;
  let specializationService: SpecializationService;

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

    fixture = TestBed.createComponent(DoctorProfileUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    doctorProfileFormService = TestBed.inject(DoctorProfileFormService);
    doctorProfileService = TestBed.inject(DoctorProfileService);
    userService = TestBed.inject(UserService);
    specializationService = TestBed.inject(SpecializationService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const doctorProfile: IDoctorProfile = { id: 7088 };
      const user: IUser = { id: 3944 };
      doctorProfile.user = user;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [user];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ doctorProfile });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Specialization query and add missing value', () => {
      const doctorProfile: IDoctorProfile = { id: 7088 };
      const specializationses: ISpecialization[] = [{ id: 29993 }];
      doctorProfile.specializationses = specializationses;

      const specializationCollection: ISpecialization[] = [{ id: 29993 }];
      vi.spyOn(specializationService, 'query').mockReturnValue(of(new HttpResponse({ body: specializationCollection })));
      const additionalSpecializations = [...specializationses];
      const expectedCollection: ISpecialization[] = [...additionalSpecializations, ...specializationCollection];
      vi.spyOn(specializationService, 'addSpecializationToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ doctorProfile });
      comp.ngOnInit();

      expect(specializationService.query).toHaveBeenCalled();
      expect(specializationService.addSpecializationToCollectionIfMissing).toHaveBeenCalledWith(
        specializationCollection,
        ...additionalSpecializations.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.specializationsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const doctorProfile: IDoctorProfile = { id: 7088 };
      const user: IUser = { id: 3944 };
      doctorProfile.user = user;
      const specializations: ISpecialization = { id: 29993 };
      doctorProfile.specializationses = [specializations];

      activatedRoute.data = of({ doctorProfile });
      comp.ngOnInit();

      expect(comp.usersSharedCollection()).toContainEqual(user);
      expect(comp.specializationsSharedCollection()).toContainEqual(specializations);
      expect(comp.doctorProfile).toEqual(doctorProfile);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IDoctorProfile>();
      const doctorProfile = { id: 27873 };
      vi.spyOn(doctorProfileFormService, 'getDoctorProfile').mockReturnValue(doctorProfile);
      vi.spyOn(doctorProfileService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ doctorProfile });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(doctorProfile);
      saveSubject.complete();

      // THEN
      expect(doctorProfileFormService.getDoctorProfile).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(doctorProfileService.update).toHaveBeenCalledWith(expect.objectContaining(doctorProfile));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IDoctorProfile>();
      const doctorProfile = { id: 27873 };
      vi.spyOn(doctorProfileFormService, 'getDoctorProfile').mockReturnValue({ id: null });
      vi.spyOn(doctorProfileService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ doctorProfile: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(doctorProfile);
      saveSubject.complete();

      // THEN
      expect(doctorProfileFormService.getDoctorProfile).toHaveBeenCalled();
      expect(doctorProfileService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IDoctorProfile>();
      const doctorProfile = { id: 27873 };
      vi.spyOn(doctorProfileService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ doctorProfile });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(doctorProfileService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        vi.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareSpecialization', () => {
      it('should forward to specializationService', () => {
        const entity = { id: 29993 };
        const entity2 = { id: 30083 };
        vi.spyOn(specializationService, 'compareSpecialization');
        comp.compareSpecialization(entity, entity2);
        expect(specializationService.compareSpecialization).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
