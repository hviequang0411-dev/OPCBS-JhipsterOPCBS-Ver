import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { Gender } from 'app/entities/enumerations/gender.model';
import { SpecializationService } from 'app/entities/specialization/service/specialization.service';
import { ISpecialization } from 'app/entities/specialization/specialization.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { type BlobType } from 'app/shared/jhipster/data-utils';
import { IDoctorProfile } from '../doctor-profile.model';
import { DoctorProfileService } from '../service/doctor-profile.service';

import { DoctorProfileFormGroup, DoctorProfileFormService } from './doctor-profile-form.service';

@Component({
  selector: 'jhi-doctor-profile-update',
  templateUrl: './doctor-profile-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class DoctorProfileUpdate implements OnInit {
  readonly isSaving = signal(false);
  doctorProfile: IDoctorProfile | null = null;
  genderValues = Object.keys(Gender);

  usersSharedCollection = signal<IUser[]>([]);
  specializationsSharedCollection = signal<ISpecialization[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected doctorProfileService = inject(DoctorProfileService);
  protected doctorProfileFormService = inject(DoctorProfileFormService);
  protected userService = inject(UserService);
  protected specializationService = inject(SpecializationService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: DoctorProfileFormGroup = this.doctorProfileFormService.createDoctorProfileFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  compareSpecialization = (o1: ISpecialization | null, o2: ISpecialization | null): boolean =>
    this.specializationService.compareSpecialization(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ doctorProfile }) => {
      this.doctorProfile = doctorProfile;
      if (doctorProfile) {
        this.updateForm(doctorProfile);
      }

      this.loadRelationshipsOptions();
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined, blobType?: BlobType): void {
    this.dataUtils.openFile(base64String, contentType, blobType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertErrorModel>('opcbsApp.error', { message: err.message })),
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const doctorProfile = this.doctorProfileFormService.getDoctorProfile(this.editForm);
    if (doctorProfile.id === null) {
      this.subscribeToSaveResponse(this.doctorProfileService.create(doctorProfile));
    } else {
      this.subscribeToSaveResponse(this.doctorProfileService.update(doctorProfile));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IDoctorProfile | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(doctorProfile: IDoctorProfile): void {
    this.doctorProfile = doctorProfile;
    this.doctorProfileFormService.resetForm(this.editForm, doctorProfile);

    this.usersSharedCollection.update(users => this.userService.addUserToCollectionIfMissing<IUser>(users, doctorProfile.user));
    this.specializationsSharedCollection.update(specializations =>
      this.specializationService.addSpecializationToCollectionIfMissing<ISpecialization>(
        specializations,
        ...(doctorProfile.specializationses ?? []),
      ),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.doctorProfile?.user)))
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));

    this.specializationService
      .query()
      .pipe(map((res: HttpResponse<ISpecialization[]>) => res.body ?? []))
      .pipe(
        map((specializations: ISpecialization[]) =>
          this.specializationService.addSpecializationToCollectionIfMissing<ISpecialization>(
            specializations,
            ...(this.doctorProfile?.specializationses ?? []),
          ),
        ),
      )
      .subscribe((specializations: ISpecialization[]) => this.specializationsSharedCollection.set(specializations));
  }
}
