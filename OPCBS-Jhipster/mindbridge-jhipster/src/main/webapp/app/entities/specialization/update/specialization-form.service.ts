import { Injectable, inject, signal, computed } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ISpecialization, NewSpecialization } from '../specialization.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ISpecialization for edit and NewSpecializationFormGroupInput for create.
 */
type SpecializationFormGroupInput = ISpecialization | PartialWithRequiredKeyOf<NewSpecialization>;

type SpecializationFormDefaults = Pick<NewSpecialization, 'id' | 'doctorses'>;

type SpecializationFormGroupContent = {
  id: FormControl<ISpecialization['id'] | NewSpecialization['id']>;
  name: FormControl<ISpecialization['name']>;
  description: FormControl<ISpecialization['description']>;
  doctorses: FormControl<ISpecialization['doctorses']>;
};

export type SpecializationFormGroup = FormGroup<SpecializationFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class SpecializationFormService {
  createSpecializationFormGroup(specialization?: SpecializationFormGroupInput): SpecializationFormGroup {
    const specializationRawValue = {
      ...this.getFormDefaults(),
      ...(specialization ?? { id: null }),
    };

    return new FormGroup<SpecializationFormGroupContent>({
      id: new FormControl(
        { value: specializationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(specializationRawValue.name, {
        validators: [Validators.required, Validators.maxLength(160)],
      }),
      description: new FormControl(specializationRawValue.description, {
        validators: [Validators.maxLength(500)],
      }),
      doctorses: new FormControl(specializationRawValue.doctorses ?? []),
    });
  }

  getSpecialization(form: SpecializationFormGroup): ISpecialization | NewSpecialization {
    return form.getRawValue();
  }

  resetForm(form: SpecializationFormGroup, specialization: SpecializationFormGroupInput): void {
    const specializationRawValue = { ...this.getFormDefaults(), ...specialization };
    form.reset({
      ...specializationRawValue,
      id: { value: specializationRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): SpecializationFormDefaults {
    return {
      id: null,
      doctorses: [],
    };
  }
}
