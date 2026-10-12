import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IAppointment, NewAppointment } from '../appointment.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IAppointment for edit and NewAppointmentFormGroupInput for create.
 */
type AppointmentFormGroupInput = IAppointment | PartialWithRequiredKeyOf<NewAppointment>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IAppointment | NewAppointment> = Omit<T, 'scheduledStart' | 'scheduledEnd' | 'createdAt' | 'updatedAt'> & {
  scheduledStart?: string | null;
  scheduledEnd?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type AppointmentFormRawValue = FormValueOf<IAppointment>;

type NewAppointmentFormRawValue = FormValueOf<NewAppointment>;

type AppointmentFormDefaults = Pick<NewAppointment, 'id' | 'scheduledStart' | 'scheduledEnd' | 'createdAt' | 'updatedAt'>;

type AppointmentFormGroupContent = {
  id: FormControl<AppointmentFormRawValue['id'] | NewAppointment['id']>;
  bookingCode: FormControl<AppointmentFormRawValue['bookingCode']>;
  scheduledStart: FormControl<AppointmentFormRawValue['scheduledStart']>;
  scheduledEnd: FormControl<AppointmentFormRawValue['scheduledEnd']>;
  consultationMode: FormControl<AppointmentFormRawValue['consultationMode']>;
  status: FormControl<AppointmentFormRawValue['status']>;
  reason: FormControl<AppointmentFormRawValue['reason']>;
  cancellationReason: FormControl<AppointmentFormRawValue['cancellationReason']>;
  createdAt: FormControl<AppointmentFormRawValue['createdAt']>;
  updatedAt: FormControl<AppointmentFormRawValue['updatedAt']>;
  patient: FormControl<AppointmentFormRawValue['patient']>;
  doctor: FormControl<AppointmentFormRawValue['doctor']>;
  slot: FormControl<AppointmentFormRawValue['slot']>;
};

export type AppointmentFormGroup = FormGroup<AppointmentFormGroupContent>;

@Service()
export class AppointmentFormService {
  createAppointmentFormGroup(appointment?: AppointmentFormGroupInput): AppointmentFormGroup {
    const appointmentRawValue = this.convertAppointmentToAppointmentRawValue({
      ...this.getFormDefaults(),
      ...(appointment ?? { id: null }),
    });

    return new FormGroup<AppointmentFormGroupContent>({
      id: new FormControl(
        { value: appointmentRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      bookingCode: new FormControl(appointmentRawValue.bookingCode, {
        validators: [Validators.required, Validators.maxLength(32)],
      }),
      scheduledStart: new FormControl(appointmentRawValue.scheduledStart, {
        validators: [Validators.required],
      }),
      scheduledEnd: new FormControl(appointmentRawValue.scheduledEnd, {
        validators: [Validators.required],
      }),
      consultationMode: new FormControl(appointmentRawValue.consultationMode, {
        validators: [Validators.required],
      }),
      status: new FormControl(appointmentRawValue.status, {
        validators: [Validators.required],
      }),
      reason: new FormControl(appointmentRawValue.reason, {
        validators: [Validators.maxLength(1000)],
      }),
      cancellationReason: new FormControl(appointmentRawValue.cancellationReason, {
        validators: [Validators.maxLength(500)],
      }),
      createdAt: new FormControl(appointmentRawValue.createdAt, {
        validators: [Validators.required],
      }),
      updatedAt: new FormControl(appointmentRawValue.updatedAt),
      patient: new FormControl(appointmentRawValue.patient, {
        validators: [Validators.required],
      }),
      doctor: new FormControl(appointmentRawValue.doctor, {
        validators: [Validators.required],
      }),
      slot: new FormControl(appointmentRawValue.slot, {
        validators: [Validators.required],
      }),
    });
  }

  getAppointment(form: AppointmentFormGroup): IAppointment | NewAppointment {
    return this.convertAppointmentRawValueToAppointment(form.getRawValue());
  }

  resetForm(form: AppointmentFormGroup, appointment: AppointmentFormGroupInput): void {
    const appointmentRawValue = this.convertAppointmentToAppointmentRawValue({ ...this.getFormDefaults(), ...appointment });
    form.reset({
      ...appointmentRawValue,
      id: { value: appointmentRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): AppointmentFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      scheduledStart: currentTime,
      scheduledEnd: currentTime,
      createdAt: currentTime,
      updatedAt: currentTime,
    };
  }

  private convertAppointmentRawValueToAppointment(
    rawAppointment: AppointmentFormRawValue | NewAppointmentFormRawValue,
  ): IAppointment | NewAppointment {
    return {
      ...rawAppointment,
      scheduledStart: dayjs(rawAppointment.scheduledStart, DATE_TIME_FORMAT),
      scheduledEnd: dayjs(rawAppointment.scheduledEnd, DATE_TIME_FORMAT),
      createdAt: dayjs(rawAppointment.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawAppointment.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertAppointmentToAppointmentRawValue(
    appointment: IAppointment | (Partial<NewAppointment> & AppointmentFormDefaults),
  ): AppointmentFormRawValue | PartialWithRequiredKeyOf<NewAppointmentFormRawValue> {
    return {
      ...appointment,
      scheduledStart: appointment.scheduledStart ? appointment.scheduledStart.format(DATE_TIME_FORMAT) : undefined,
      scheduledEnd: appointment.scheduledEnd ? appointment.scheduledEnd.format(DATE_TIME_FORMAT) : undefined,
      createdAt: appointment.createdAt ? appointment.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: appointment.updatedAt ? appointment.updatedAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
