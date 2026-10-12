import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IAvailabilitySlot, NewAvailabilitySlot } from '../availability-slot.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IAvailabilitySlot for edit and NewAvailabilitySlotFormGroupInput for create.
 */
type AvailabilitySlotFormGroupInput = IAvailabilitySlot | PartialWithRequiredKeyOf<NewAvailabilitySlot>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IAvailabilitySlot | NewAvailabilitySlot> = Omit<T, 'startTime' | 'endTime'> & {
  startTime?: string | null;
  endTime?: string | null;
};

type AvailabilitySlotFormRawValue = FormValueOf<IAvailabilitySlot>;

type NewAvailabilitySlotFormRawValue = FormValueOf<NewAvailabilitySlot>;

type AvailabilitySlotFormDefaults = Pick<NewAvailabilitySlot, 'id' | 'startTime' | 'endTime'>;

type AvailabilitySlotFormGroupContent = {
  id: FormControl<AvailabilitySlotFormRawValue['id'] | NewAvailabilitySlot['id']>;
  startTime: FormControl<AvailabilitySlotFormRawValue['startTime']>;
  endTime: FormControl<AvailabilitySlotFormRawValue['endTime']>;
  consultationMode: FormControl<AvailabilitySlotFormRawValue['consultationMode']>;
  status: FormControl<AvailabilitySlotFormRawValue['status']>;
  notes: FormControl<AvailabilitySlotFormRawValue['notes']>;
  doctor: FormControl<AvailabilitySlotFormRawValue['doctor']>;
};

export type AvailabilitySlotFormGroup = FormGroup<AvailabilitySlotFormGroupContent>;

@Service()
export class AvailabilitySlotFormService {
  createAvailabilitySlotFormGroup(availabilitySlot?: AvailabilitySlotFormGroupInput): AvailabilitySlotFormGroup {
    const availabilitySlotRawValue = this.convertAvailabilitySlotToAvailabilitySlotRawValue({
      ...this.getFormDefaults(),
      ...(availabilitySlot ?? { id: null }),
    });

    return new FormGroup<AvailabilitySlotFormGroupContent>({
      id: new FormControl(
        { value: availabilitySlotRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      startTime: new FormControl(availabilitySlotRawValue.startTime, {
        validators: [Validators.required],
      }),
      endTime: new FormControl(availabilitySlotRawValue.endTime, {
        validators: [Validators.required],
      }),
      consultationMode: new FormControl(availabilitySlotRawValue.consultationMode, {
        validators: [Validators.required],
      }),
      status: new FormControl(availabilitySlotRawValue.status, {
        validators: [Validators.required],
      }),
      notes: new FormControl(availabilitySlotRawValue.notes, {
        validators: [Validators.maxLength(255)],
      }),
      doctor: new FormControl(availabilitySlotRawValue.doctor, {
        validators: [Validators.required],
      }),
    });
  }

  getAvailabilitySlot(form: AvailabilitySlotFormGroup): IAvailabilitySlot | NewAvailabilitySlot {
    return this.convertAvailabilitySlotRawValueToAvailabilitySlot(form.getRawValue());
  }

  resetForm(form: AvailabilitySlotFormGroup, availabilitySlot: AvailabilitySlotFormGroupInput): void {
    const availabilitySlotRawValue = this.convertAvailabilitySlotToAvailabilitySlotRawValue({
      ...this.getFormDefaults(),
      ...availabilitySlot,
    });
    form.reset({
      ...availabilitySlotRawValue,
      id: { value: availabilitySlotRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): AvailabilitySlotFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      startTime: currentTime,
      endTime: currentTime,
    };
  }

  private convertAvailabilitySlotRawValueToAvailabilitySlot(
    rawAvailabilitySlot: AvailabilitySlotFormRawValue | NewAvailabilitySlotFormRawValue,
  ): IAvailabilitySlot | NewAvailabilitySlot {
    return {
      ...rawAvailabilitySlot,
      startTime: dayjs(rawAvailabilitySlot.startTime, DATE_TIME_FORMAT),
      endTime: dayjs(rawAvailabilitySlot.endTime, DATE_TIME_FORMAT),
    };
  }

  private convertAvailabilitySlotToAvailabilitySlotRawValue(
    availabilitySlot: IAvailabilitySlot | (Partial<NewAvailabilitySlot> & AvailabilitySlotFormDefaults),
  ): AvailabilitySlotFormRawValue | PartialWithRequiredKeyOf<NewAvailabilitySlotFormRawValue> {
    return {
      ...availabilitySlot,
      startTime: availabilitySlot.startTime ? availabilitySlot.startTime.format(DATE_TIME_FORMAT) : undefined,
      endTime: availabilitySlot.endTime ? availabilitySlot.endTime.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
