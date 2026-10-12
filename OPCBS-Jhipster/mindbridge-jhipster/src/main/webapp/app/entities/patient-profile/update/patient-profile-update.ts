import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbInputDatepicker } from '@ng-bootstrap/ng-bootstrap/datepicker';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { Gender } from 'app/entities/enumerations/gender.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { type BlobType } from 'app/shared/jhipster/data-utils';
import { IPatientProfile } from '../patient-profile.model';
import { PatientProfileService } from '../service/patient-profile.service';

import { PatientProfileFormGroup, PatientProfileFormService } from './patient-profile-form.service';

@Component({
  selector: 'jhi-patient-profile-update',
  templateUrl: './patient-profile-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule, NgbInputDatepicker],
})
export class PatientProfileUpdate implements OnInit {
  readonly isSaving = signal(false);
  patientProfile: IPatientProfile | null = null;
  genderValues = Object.keys(Gender);

  usersSharedCollection = signal<IUser[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected patientProfileService = inject(PatientProfileService);
  protected patientProfileFormService = inject(PatientProfileFormService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: PatientProfileFormGroup = this.patientProfileFormService.createPatientProfileFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ patientProfile }) => {
      this.patientProfile = patientProfile;
      if (patientProfile) {
        this.updateForm(patientProfile);
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
    const patientProfile = this.patientProfileFormService.getPatientProfile(this.editForm);
    if (patientProfile.id === null) {
      this.subscribeToSaveResponse(this.patientProfileService.create(patientProfile));
    } else {
      this.subscribeToSaveResponse(this.patientProfileService.update(patientProfile));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IPatientProfile | null>): void {
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

  protected updateForm(patientProfile: IPatientProfile): void {
    this.patientProfile = patientProfile;
    this.patientProfileFormService.resetForm(this.editForm, patientProfile);

    this.usersSharedCollection.update(users => this.userService.addUserToCollectionIfMissing<IUser>(users, patientProfile.user));
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.patientProfile?.user)))
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));
  }
}
