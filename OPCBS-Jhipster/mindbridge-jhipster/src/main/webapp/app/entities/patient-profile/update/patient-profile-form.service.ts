import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IPatientProfile, NewPatientProfile } from '../patient-profile.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IPatientProfile for edit and NewPatientProfileFormGroupInput for create.
 */
type PatientProfileFormGroupInput = IPatientProfile | PartialWithRequiredKeyOf<NewPatientProfile>;

type PatientProfileFormDefaults = Pick<NewPatientProfile, 'id'>;

type PatientProfileFormGroupContent = {
  id: FormControl<IPatientProfile['id'] | NewPatientProfile['id']>;
  fullName: FormControl<IPatientProfile['fullName']>;
  phone: FormControl<IPatientProfile['phone']>;
  dateOfBirth: FormControl<IPatientProfile['dateOfBirth']>;
  gender: FormControl<IPatientProfile['gender']>;
  address: FormControl<IPatientProfile['address']>;
  medicalHistory: FormControl<IPatientProfile['medicalHistory']>;
  user: FormControl<IPatientProfile['user']>;
};

export type PatientProfileFormGroup = FormGroup<PatientProfileFormGroupContent>;

@Service()
export class PatientProfileFormService {
  createPatientProfileFormGroup(patientProfile?: PatientProfileFormGroupInput): PatientProfileFormGroup {
    const patientProfileRawValue = {
      ...this.getFormDefaults(),
      ...(patientProfile ?? { id: null }),
    };

    return new FormGroup<PatientProfileFormGroupContent>({
      id: new FormControl(
        { value: patientProfileRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      fullName: new FormControl(patientProfileRawValue.fullName, {
        validators: [Validators.required, Validators.maxLength(160)],
      }),
      phone: new FormControl(patientProfileRawValue.phone, {
        validators: [Validators.required, Validators.maxLength(30)],
      }),
      dateOfBirth: new FormControl(patientProfileRawValue.dateOfBirth),
      gender: new FormControl(patientProfileRawValue.gender),
      address: new FormControl(patientProfileRawValue.address, {
        validators: [Validators.maxLength(255)],
      }),
      medicalHistory: new FormControl(patientProfileRawValue.medicalHistory),
      user: new FormControl(patientProfileRawValue.user, {
        validators: [Validators.required],
      }),
    });
  }

  getPatientProfile(form: PatientProfileFormGroup): IPatientProfile | NewPatientProfile {
    return form.getRawValue();
  }

  resetForm(form: PatientProfileFormGroup, patientProfile: PatientProfileFormGroupInput): void {
    const patientProfileRawValue = { ...this.getFormDefaults(), ...patientProfile };
    form.reset({
      ...patientProfileRawValue,
      id: { value: patientProfileRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): PatientProfileFormDefaults {
    return {
      id: null,
    };
  }
}
