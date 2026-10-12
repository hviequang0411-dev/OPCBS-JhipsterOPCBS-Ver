import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IDoctorProfile, NewDoctorProfile } from '../doctor-profile.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IDoctorProfile for edit and NewDoctorProfileFormGroupInput for create.
 */
type DoctorProfileFormGroupInput = IDoctorProfile | PartialWithRequiredKeyOf<NewDoctorProfile>;

type DoctorProfileFormDefaults = Pick<NewDoctorProfile, 'id' | 'isActive' | 'specializationses'>;

type DoctorProfileFormGroupContent = {
  id: FormControl<IDoctorProfile['id'] | NewDoctorProfile['id']>;
  fullName: FormControl<IDoctorProfile['fullName']>;
  title: FormControl<IDoctorProfile['title']>;
  bio: FormControl<IDoctorProfile['bio']>;
  experienceYears: FormControl<IDoctorProfile['experienceYears']>;
  consultationFee: FormControl<IDoctorProfile['consultationFee']>;
  phone: FormControl<IDoctorProfile['phone']>;
  gender: FormControl<IDoctorProfile['gender']>;
  isActive: FormControl<IDoctorProfile['isActive']>;
  user: FormControl<IDoctorProfile['user']>;
  specializationses: FormControl<IDoctorProfile['specializationses']>;
};

export type DoctorProfileFormGroup = FormGroup<DoctorProfileFormGroupContent>;

@Service()
export class DoctorProfileFormService {
  createDoctorProfileFormGroup(doctorProfile?: DoctorProfileFormGroupInput): DoctorProfileFormGroup {
    const doctorProfileRawValue = {
      ...this.getFormDefaults(),
      ...(doctorProfile ?? { id: null }),
    };

    return new FormGroup<DoctorProfileFormGroupContent>({
      id: new FormControl(
        { value: doctorProfileRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      fullName: new FormControl(doctorProfileRawValue.fullName, {
        validators: [Validators.required, Validators.maxLength(160)],
      }),
      title: new FormControl(doctorProfileRawValue.title, {
        validators: [Validators.maxLength(100)],
      }),
      bio: new FormControl(doctorProfileRawValue.bio),
      experienceYears: new FormControl(doctorProfileRawValue.experienceYears, {
        validators: [Validators.min(0)],
      }),
      consultationFee: new FormControl(doctorProfileRawValue.consultationFee, {
        validators: [Validators.min(0)],
      }),
      phone: new FormControl(doctorProfileRawValue.phone, {
        validators: [Validators.maxLength(30)],
      }),
      gender: new FormControl(doctorProfileRawValue.gender),
      isActive: new FormControl(doctorProfileRawValue.isActive, {
        validators: [Validators.required],
      }),
      user: new FormControl(doctorProfileRawValue.user, {
        validators: [Validators.required],
      }),
      specializationses: new FormControl(doctorProfileRawValue.specializationses ?? []),
    });
  }

  getDoctorProfile(form: DoctorProfileFormGroup): IDoctorProfile | NewDoctorProfile {
    return form.getRawValue();
  }

  resetForm(form: DoctorProfileFormGroup, doctorProfile: DoctorProfileFormGroupInput): void {
    const doctorProfileRawValue = { ...this.getFormDefaults(), ...doctorProfile };
    form.reset({
      ...doctorProfileRawValue,
      id: { value: doctorProfileRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): DoctorProfileFormDefaults {
    return {
      id: null,
      isActive: false,
      specializationses: [],
    };
  }
}
