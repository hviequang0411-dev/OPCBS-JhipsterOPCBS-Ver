import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { ConsultationMode } from 'app/entities/enumerations/consultation-mode.model';
import { SlotStatus } from 'app/entities/enumerations/slot-status.model';
import { AlertError } from 'app/shared/alert';
import { IAvailabilitySlot } from '../availability-slot.model';
import { AvailabilitySlotService } from '../service/availability-slot.service';

import { AvailabilitySlotFormGroup, AvailabilitySlotFormService } from './availability-slot-form.service';

@Component({
  selector: 'jhi-availability-slot-update',
  templateUrl: './availability-slot-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class AvailabilitySlotUpdate implements OnInit {
  readonly isSaving = signal(false);
  availabilitySlot: IAvailabilitySlot | null = null;
  consultationModeValues = Object.keys(ConsultationMode);
  slotStatusValues = Object.keys(SlotStatus);

  doctorProfilesSharedCollection = signal<IDoctorProfile[]>([]);

  protected availabilitySlotService = inject(AvailabilitySlotService);
  protected availabilitySlotFormService = inject(AvailabilitySlotFormService);
  protected doctorProfileService = inject(DoctorProfileService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: AvailabilitySlotFormGroup = this.availabilitySlotFormService.createAvailabilitySlotFormGroup();

  compareDoctorProfile = (o1: IDoctorProfile | null, o2: IDoctorProfile | null): boolean =>
    this.doctorProfileService.compareDoctorProfile(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ availabilitySlot }) => {
      this.availabilitySlot = availabilitySlot;
      if (availabilitySlot) {
        this.updateForm(availabilitySlot);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const availabilitySlot = this.availabilitySlotFormService.getAvailabilitySlot(this.editForm);
    if (availabilitySlot.id === null) {
      this.subscribeToSaveResponse(this.availabilitySlotService.create(availabilitySlot));
    } else {
      this.subscribeToSaveResponse(this.availabilitySlotService.update(availabilitySlot));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IAvailabilitySlot | null>): void {
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

  protected updateForm(availabilitySlot: IAvailabilitySlot): void {
    this.availabilitySlot = availabilitySlot;
    this.availabilitySlotFormService.resetForm(this.editForm, availabilitySlot);

    this.doctorProfilesSharedCollection.update(doctorProfiles =>
      this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(doctorProfiles, availabilitySlot.doctor),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.doctorProfileService
      .query()
      .pipe(map((res: HttpResponse<IDoctorProfile[]>) => res.body ?? []))
      .pipe(
        map((doctorProfiles: IDoctorProfile[]) =>
          this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(doctorProfiles, this.availabilitySlot?.doctor),
        ),
      )
      .subscribe((doctorProfiles: IDoctorProfile[]) => this.doctorProfilesSharedCollection.set(doctorProfiles));
  }
}
