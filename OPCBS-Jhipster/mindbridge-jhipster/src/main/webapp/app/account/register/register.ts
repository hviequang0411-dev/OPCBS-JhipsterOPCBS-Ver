import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { EMAIL_ALREADY_USED_TYPE, LOGIN_ALREADY_USED_TYPE } from 'app/shared/jhipster/error.constants';
import PasswordStrengthBar from '../password/password-strength-bar/password-strength-bar';
import { RegisterService } from './register.service';
import { GoogleAuthService } from 'app/core/auth';

@Component({
  selector: 'jhi-register',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, PasswordStrengthBar],
  templateUrl: './register.html',
  styleUrls: ['./register.scss'],
})
export default class Register implements AfterViewInit {
  readonly login = viewChild.required<ElementRef>('login');

  readonly doNotMatch = signal(false);
  readonly error = signal(false);
  readonly errorEmailExists = signal(false);
  readonly errorUserExists = signal(false);
  readonly success = signal(false);
  readonly isGoogleLoading = signal(false);

  registerForm = new FormGroup({
    fullName: new FormControl(''),
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
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
  });

  private readonly registerService = inject(RegisterService);
  private readonly router = inject(Router);
  private readonly googleAuthService = inject(GoogleAuthService);

  ngAfterViewInit(): void {
    this.login().nativeElement.focus();
  }

  register(): void {
    this.doNotMatch.set(false);
    this.error.set(false);
    this.errorEmailExists.set(false);
    this.errorUserExists.set(false);

    const { password, confirmPassword, fullName } = this.registerForm.getRawValue();
    if (password === confirmPassword) {
      const { login, email } = this.registerForm.getRawValue();
      const names = (fullName ?? '').trim().split(' ');
      const firstName = names[0] ?? '';
      const lastName = names.slice(1).join(' ');

      this.registerService
        .save({ login, email, password, langKey: 'en', firstName, lastName } as any)
        .subscribe({
          next: () => {
            this.success.set(true);
            // Tự động chuyển hướng sang trang xác thực OTP sau 1.5s
            setTimeout(() => {
              this.router.navigate(['/account/verify-otp'], {
                queryParams: { email, username: login },
              });
            }, 1200);
          },
          error: response => this.processError(response),
        });
    } else {
      this.doNotMatch.set(true);
    }
  }

  continueWithGoogle(): void {
    this.isGoogleLoading.set(true);
    this.googleAuthService.signInWithGoogleGSI(
      () => {
        this.isGoogleLoading.set(false);
        this.router.navigate(['']);
      },
      () => {
        this.isGoogleLoading.set(false);
        this.error.set(true);
      }
    );
  }

  private processError(response: HttpErrorResponse): void {
    if (response.status === 400 && response.error.type === LOGIN_ALREADY_USED_TYPE) {
      this.errorUserExists.set(true);
    } else if (response.status === 400 && response.error.type === EMAIL_ALREADY_USED_TYPE) {
      this.errorEmailExists.set(true);
    } else {
      this.error.set(true);
    }
  }
}
