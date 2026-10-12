import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { EMAIL_ALREADY_USED_TYPE, LOGIN_ALREADY_USED_TYPE } from 'app/shared/jhipster/error.constants';
import PasswordStrengthBar from '../password/password-strength-bar/password-strength-bar';
import { RegisterService } from '../register/register.service';
import { SpecializationService } from 'app/entities/specialization/service/specialization.service';
import { ISpecialization } from 'app/entities/specialization/specialization.model';

@Component({
  selector: 'jhi-register-doctor',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, PasswordStrengthBar],
  templateUrl: './register-doctor.html',
})
export default class RegisterDoctor implements OnInit {
  readonly doNotMatch = signal(false);
  readonly error = signal(false);
  readonly errorEmailExists = signal(false);
  readonly errorUserExists = signal(false);
  readonly success = signal(false);
  readonly specializations = signal<ISpecialization[]>([]);
  readonly selectedSpecializationIds = signal<number[]>([]);

  registerForm = new FormGroup({
    fullName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    login: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(30)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),

    // Professional info
    title: new FormControl(''),
    bio: new FormControl(''),
    experienceYears: new FormControl<number | null>(0),
    consultationFee: new FormControl<number | null>(50),
  });

  private readonly registerService = inject(RegisterService);
  private readonly specializationService = inject(SpecializationService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.specializationService.query({ size: 50 }).subscribe({
      next: res => {
        if (res.body) {
          this.specializations.set(res.body);
        }
      },
    });
  }

  toggleSpecialization(id: number): void {
    const current = this.selectedSpecializationIds();
    if (current.includes(id)) {
      this.selectedSpecializationIds.set(current.filter(x => x !== id));
    } else {
      this.selectedSpecializationIds.set([...current, id]);
    }
  }

  registerDoctor(): void {
    this.doNotMatch.set(false);
    this.error.set(false);
    this.errorEmailExists.set(false);
    this.errorUserExists.set(false);

    const values = this.registerForm.getRawValue();
    if (values.password !== values.confirmPassword) {
      this.doNotMatch.set(true);
      return;
    }

    const payload = {
      login: values.login,
      email: values.email,
      password: values.password,
      fullName: values.fullName,
      phone: values.phone,
      title: values.title ?? '',
      bio: values.bio ?? '',
      experienceYears: values.experienceYears ?? 0,
      consultationFee: values.consultationFee ?? 50,
      specializationIds: this.selectedSpecializationIds(),
    };

    this.registerService.saveDoctor(payload).subscribe({
      next: () => {
        this.success.set(true);
        // Tự động chuyển sang xác thực OTP
        setTimeout(() => {
          this.router.navigate(['/account/verify-otp'], {
            queryParams: { email: values.email, username: values.login },
          });
        }, 1200);
      },
      error: response => this.processError(response),
    });
  }

  private processError(response: HttpErrorResponse): void {
    if (response.status === 400 && response.error?.type === LOGIN_ALREADY_USED_TYPE) {
      this.errorUserExists.set(true);
    } else if (response.status === 400 && response.error?.type === EMAIL_ALREADY_USED_TYPE) {
      this.errorEmailExists.set(true);
    } else {
      this.error.set(true);
    }
  }
}
