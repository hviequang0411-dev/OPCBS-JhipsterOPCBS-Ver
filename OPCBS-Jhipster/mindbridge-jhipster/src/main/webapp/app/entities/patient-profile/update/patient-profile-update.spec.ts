import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { IPatientProfile } from '../patient-profile.model';
import { PatientProfileService } from '../service/patient-profile.service';

import { PatientProfileFormService } from './patient-profile-form.service';
import { PatientProfileUpdate } from './patient-profile-update';

describe('PatientProfile Management Update Component', () => {
  let comp: PatientProfileUpdate;
  let fixture: ComponentFixture<PatientProfileUpdate>;
  let activatedRoute: ActivatedRoute;
  let patientProfileFormService: PatientProfileFormService;
  let patientProfileService: PatientProfileService;
  let userService: UserService;

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

    fixture = TestBed.createComponent(PatientProfileUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    patientProfileFormService = TestBed.inject(PatientProfileFormService);
    patientProfileService = TestBed.inject(PatientProfileService);
    userService = TestBed.inject(UserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const patientProfile: IPatientProfile = { id: 30515 };
      const user: IUser = { id: 3944 };
      patientProfile.user = user;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [user];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ patientProfile });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const patientProfile: IPatientProfile = { id: 30515 };
      const user: IUser = { id: 3944 };
      patientProfile.user = user;

      activatedRoute.data = of({ patientProfile });
      comp.ngOnInit();

      expect(comp.usersSharedCollection()).toContainEqual(user);
      expect(comp.patientProfile).toEqual(patientProfile);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IPatientProfile>();
      const patientProfile = { id: 11369 };
      vi.spyOn(patientProfileFormService, 'getPatientProfile').mockReturnValue(patientProfile);
      vi.spyOn(patientProfileService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ patientProfile });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(patientProfile);
      saveSubject.complete();

      // THEN
      expect(patientProfileFormService.getPatientProfile).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(patientProfileService.update).toHaveBeenCalledWith(expect.objectContaining(patientProfile));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IPatientProfile>();
      const patientProfile = { id: 11369 };
      vi.spyOn(patientProfileFormService, 'getPatientProfile').mockReturnValue({ id: null });
      vi.spyOn(patientProfileService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ patientProfile: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(patientProfile);
      saveSubject.complete();

      // THEN
      expect(patientProfileFormService.getPatientProfile).toHaveBeenCalled();
      expect(patientProfileService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IPatientProfile>();
      const patientProfile = { id: 11369 };
      vi.spyOn(patientProfileService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ patientProfile });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(patientProfileService.update).toHaveBeenCalled();
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
  });
});
