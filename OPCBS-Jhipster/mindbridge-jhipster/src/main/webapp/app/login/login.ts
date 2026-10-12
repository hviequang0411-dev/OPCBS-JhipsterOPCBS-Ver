import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AccountService, GoogleAuthService } from 'app/core/auth';
import { LoginService } from 'app/login/login.service';

@Component({
  selector: 'jhi-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss'],
})
export default class Login implements OnInit, AfterViewInit {
  readonly username = viewChild.required<ElementRef>('username');
  readonly authenticationError = signal(false);
  readonly errorMessage = signal<string>('');
  readonly errorCode = signal<'NOT_ACTIVATED' | 'USER_NOT_FOUND' | 'INVALID_PASSWORD' | 'GENERIC'>('GENERIC');
  readonly unactivatedAccount = signal<string>('');
  readonly showPassword = signal(false);
  readonly isGoogleLoading = signal(false);

  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly googleAuthService = inject(GoogleAuthService);

  ngOnInit(): void {
    // if already authenticated then navigate to home page
    this.accountService.identity().subscribe(() => {
      if (this.accountService.isAuthenticated()) {
        this.router.navigate(['']);
      }
    });
  }

  ngAfterViewInit(): void {
    this.username().nativeElement.focus();
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(prev => !prev);
  }

  fillDemo(user: string, pass: string): void {
    this.loginForm.patchValue({
      username: user,
      password: pass,
      rememberMe: true,
    });
    this.clearErrors();
  }

  clearErrors(): void {
    this.authenticationError.set(false);
    this.errorMessage.set('');
    this.errorCode.set('GENERIC');
    this.unactivatedAccount.set('');
  }

  login(): void {
    this.clearErrors();
    const username = this.loginForm.getRawValue().username;

    this.loginService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        if (!this.router.currentNavigation()) {
          this.router.navigate(['']);
        }
      },
      error: response => {
        this.authenticationError.set(true);
        const errorKey = response?.error?.errorKey || response?.headers?.get('x-opcbs-error');
        const errorDetail = response?.error?.detail || response?.error?.title || '';

        if (errorKey === 'userNotActivated' || errorDetail.toLowerCase().includes('not activated') || errorDetail.toLowerCase().includes('chưa được kích hoạt')) {
          this.errorCode.set('NOT_ACTIVATED');
          this.unactivatedAccount.set(username);
          this.errorMessage.set('Tài khoản của bạn chưa được xác thực email. Vui lòng nhập mã OTP để kích hoạt tài khoản.');
        } else if (errorKey === 'userNotFound' || errorDetail.toLowerCase().includes('không tồn tại')) {
          this.errorCode.set('USER_NOT_FOUND');
          this.errorMessage.set('Tài khoản hoặc Email này chưa được đăng ký trong hệ thống.');
        } else if (errorKey === 'invalidPassword' || errorDetail.toLowerCase().includes('mật khẩu không chính xác')) {
          this.errorCode.set('INVALID_PASSWORD');
          this.errorMessage.set('Mật khẩu không chính xác. Vui lòng kiểm tra lại.');
        } else {
          this.errorCode.set('GENERIC');
          this.errorMessage.set('Đăng nhập không thành công! Vui lòng kiểm tra lại thông tin.');
        }
      },
    });
  }

  loginWithGoogle(): void {
    this.isGoogleLoading.set(true);
    this.clearErrors();

    this.googleAuthService.signInWithGoogleGSI(
      () => {
        this.isGoogleLoading.set(false);
        this.router.navigate(['']);
      },
      () => {
        this.isGoogleLoading.set(false);
        this.authenticationError.set(true);
        this.errorMessage.set('Không thể kết nối đến tài khoản Google. Vui lòng thử lại sau.');
      }
    );
  }
}
