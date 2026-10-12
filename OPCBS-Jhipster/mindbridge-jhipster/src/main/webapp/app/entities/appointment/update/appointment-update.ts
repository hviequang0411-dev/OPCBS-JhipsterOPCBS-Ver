import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { IAvailabilitySlot } from 'app/entities/availability-slot/availability-slot.model';
import { AvailabilitySlotService } from 'app/entities/availability-slot/service/availability-slot.service';
import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { AppointmentStatus } from 'app/entities/enumerations/appointment-status.model';
import { ConsultationMode } from 'app/entities/enumerations/consultation-mode.model';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';
import { PatientProfileService } from 'app/entities/patient-profile/service/patient-profile.service';
import { AlertError } from 'app/shared/alert';

import { IAppointment } from '../appointment.model';

import { AppointmentService } from '../service/appointment.service';
import { AppointmentFormService, AppointmentFormGroup } from './appointment-form.service';

@Component({
  selector: 'jhi-appointment-update',
  templateUrl: './appointment-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class AppointmentUpdate implements OnInit {
  readonly isSaving = signal(false);
  appointment: IAppointment | null = null;
  consultationModeValues = Object.keys(ConsultationMode);
  appointmentStatusValues = Object.keys(AppointmentStatus);

  patientProfilesSharedCollection = signal<IPatientProfile[]>([]);
  doctorProfilesSharedCollection = signal<IDoctorProfile[]>([]);
  availabilitySlotsSharedCollection = signal<IAvailabilitySlot[]>([]);

  protected appointmentService = inject(AppointmentService);
  protected appointmentFormService = inject(AppointmentFormService);
  protected patientProfileService = inject(PatientProfileService);
  protected doctorProfileService = inject(DoctorProfileService);
  protected availabilitySlotService = inject(AvailabilitySlotService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: AppointmentFormGroup = this.appointmentFormService.createAppointmentFormGroup();

  comparePatientProfile = (o1: IPatientProfile | null, o2: IPatientProfile | null): boolean =>
    this.patientProfileService.comparePatientProfile(o1, o2);

  compareDoctorProfile = (o1: IDoctorProfile | null, o2: IDoctorProfile | null): boolean =>
    this.doctorProfileService.compareDoctorProfile(o1, o2);

  compareAvailabilitySlot = (o1: IAvailabilitySlot | null, o2: IAvailabilitySlot | null): boolean =>
    this.availabilitySlotService.compareAvailabilitySlot(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ appointment }) => {
      this.appointment = appointment;
      if (appointment) {
        this.updateForm(appointment);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const appointment = this.appointmentFormService.getAppointment(this.editForm);
    if (appointment.id === null) {
      this.subscribeToSaveResponse(this.appointmentService.create(appointment));
    } else {
      this.subscribeToSaveResponse(this.appointmentService.update(appointment));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IAppointment | null>): void {
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

  protected updateForm(appointment: IAppointment): void {
    this.appointment = appointment;
    this.appointmentFormService.resetForm(this.editForm, appointment);

    this.patientProfilesSharedCollection.update(patientProfiles =>
      this.patientProfileService.addPatientProfileToCollectionIfMissing<IPatientProfile>(patientProfiles, appointment.patient),
    );
    this.doctorProfilesSharedCollection.update(doctorProfiles =>
      this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(doctorProfiles, appointment.doctor),
    );
    this.availabilitySlotsSharedCollection.update(availabilitySlots =>
      this.availabilitySlotService.addAvailabilitySlotToCollectionIfMissing<IAvailabilitySlot>(availabilitySlots, appointment.slot),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.patientProfileService
      .query()
      .pipe(map((res: HttpResponse<IPatientProfile[]>) => res.body ?? []))
      .pipe(
        map((patientProfiles: IPatientProfile[]) =>
          this.patientProfileService.addPatientProfileToCollectionIfMissing<IPatientProfile>(patientProfiles, this.appointment?.patient),
        ),
      )
      .subscribe((patientProfiles: IPatientProfile[]) => this.patientProfilesSharedCollection.set(patientProfiles));

    this.doctorProfileService
      .query()
      .pipe(map((res: HttpResponse<IDoctorProfile[]>) => res.body ?? []))
      .pipe(
        map((doctorProfiles: IDoctorProfile[]) =>
          this.doctorProfileService.addDoctorProfileToCollectionIfMissing<IDoctorProfile>(doctorProfiles, this.appointment?.doctor),
        ),
      )
      .subscribe((doctorProfiles: IDoctorProfile[]) => this.doctorProfilesSharedCollection.set(doctorProfiles));

    this.availabilitySlotService
      .query()
      .pipe(map((res: HttpResponse<IAvailabilitySlot[]>) => res.body ?? []))
      .pipe(
        map((availabilitySlots: IAvailabilitySlot[]) =>
          this.availabilitySlotService.addAvailabilitySlotToCollectionIfMissing<IAvailabilitySlot>(
            availabilitySlots,
            this.appointment?.slot,
          ),
        ),
      )
      .subscribe((availabilitySlots: IAvailabilitySlot[]) => this.availabilitySlotsSharedCollection.set(availabilitySlots));
  }
}
