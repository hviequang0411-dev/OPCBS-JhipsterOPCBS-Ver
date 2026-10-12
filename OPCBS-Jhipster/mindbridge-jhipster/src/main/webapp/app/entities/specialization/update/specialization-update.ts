import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { AlertError } from 'app/shared/alert';
import { SpecializationService } from '../service/specialization.service';
import { ISpecialization } from '../specialization.model';

import { SpecializationFormGroup, SpecializationFormService } from './specialization-form.service';

@Component({
  selector: 'jhi-specialization-update',
  templateUrl: './specialization-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class SpecializationUpdate implements OnInit {
  readonly isSaving = signal(false);
  specialization: ISpecialization | null = null;

  doctorProfilesSharedCollection = signal<IDoctorProfile[]>([]);

  protected specializationService = inject(SpecializationService);
  protected specializationFormService = inject(SpecializationFormService);
  protected doctorProfileService = inject(DoctorProfileService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: SpecializationFormGroup = this.specializationFormService.createSpecializationFormGroup();

  compareDoctorProfile = (o1: IDoctorProfile | null, o2: IDoctorProfile | null): boolean =>
    this.doctorProfileService.compareDoctorProfile(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ specialization }) => {
      this.specialization = specialization;
      if (specialization) {
        this.updateForm(specialization);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const specialization = this.specializationFormService.getSpecialization(this.editForm);
    if (specialization.id === null) {
      this.subscribeToSaveResponse(this.specializationService.create(specialization));
    } else {
      this.subscribeToSaveResponse(this.specializationService.update(specialization));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ISpecialization | null>): void {
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

  protected updateForm(specialization: ISpecialization): void {
    this.specialization = specialization;
    this.specializationFormService.resetForm(this.editForm, specialization);

    this.doctorProfilesSharedCollection.update(doctorProfiles =>
      this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(doctorProfiles, ...(specialization.doctorses ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.doctorProfileService
      .query()
      .pipe(map((res: HttpResponse<IDoctorProfile[]>) => res.body ?? []))
      .pipe(
        map((doctorProfiles: IDoctorProfile[]) =>
          this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(
            doctorProfiles,
            ...(this.specialization?.doctorses ?? []),
          ),
        ),
      )
      .subscribe((doctorProfiles: IDoctorProfile[]) => this.doctorProfilesSharedCollection.set(doctorProfiles));
  }
}
