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
    // =========================================================================
    // [CƠ CHẾ BẮT ĐĂNG NHẬP LẠI]:
    // Nếu vừa logout hoặc có cờ 'loggedOut', tuyệt đối không tự động chuyển trang.
    // Bắt buộc người dùng phải ở lại trang /login và nhập lại tài khoản/mật khẩu.
    // =========================================================================
    const isLoggedOut = this.router.routerState.snapshot.root.queryParams['loggedOut'];
    if (isLoggedOut) {
      this.accountService.authenticate(null);
      return;
    }

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
      next: account => {
        if (!account) {
          this.authenticationError.set(true);
          this.errorCode.set('GENERIC');
          this.errorMessage.set('Không thể đồng bộ thông tin hồ sơ tài khoản. Vui lòng đăng nhập lại.');
          return;
        }
        if (!this.router.currentNavigation()) {
          this.router.navigate(['']);
        }
      },
      error: response => {
        this.authenticationError.set(true);

        // 1. Thu thập Error Key từ nhiều nguồn (errorKey, message, headers)
        let key = response?.error?.errorKey || '';
        if (!key && response?.error?.message && typeof response.error.message === 'string') {
          key = response.error.message.replace(/^error\./, '');
        }
        if (!key && response?.headers && typeof response.headers.get === 'function') {
          const headerKey =
            response.headers.get('x-opcbs-error') ||
            response.headers.get('x-auth-service-error') ||
            response.headers.get('x-opcbs-auth-service-error');
          if (headerKey) {
            key = headerKey.replace(/^error\./, '');
          }
        }

        const normalizedKey = key.toLowerCase();
        const errorDetail = (
          (response?.error?.defaultMessage || '') + ' ' +
          (response?.error?.title || '') + ' ' +
          (response?.error?.detail || '')
        ).toLowerCase();

        // 2. Phân loại lỗi đăng nhập thông minh
        if (
          normalizedKey.includes('usernotactivated') ||
          normalizedKey.includes('notactivated') ||
          errorDetail.includes('not activated') ||
          errorDetail.includes('chưa được kích hoạt')
        ) {
          this.errorCode.set('NOT_ACTIVATED');
          this.unactivatedAccount.set(username);
          this.errorMessage.set(
            response?.error?.defaultMessage ||
            'Tài khoản của bạn chưa được xác thực email. Vui lòng nhập mã OTP để kích hoạt tài khoản.'
          );
        } else if (
          normalizedKey.includes('usernotfound') ||
          errorDetail.includes('user not found') ||
          errorDetail.includes('không tồn tại')
        ) {
          this.errorCode.set('USER_NOT_FOUND');
          this.errorMessage.set(
            response?.error?.defaultMessage ||
            'Tài khoản hoặc Email này chưa được đăng ký trong hệ thống MindBridge.'
          );
        } else if (
          normalizedKey.includes('invalidpassword') ||
          errorDetail.includes('invalid password') ||
          errorDetail.includes('mật khẩu không chính xác') ||
          errorDetail.includes('bad credentials')
        ) {
          this.errorCode.set('INVALID_PASSWORD');
          this.errorMessage.set(
            response?.error?.defaultMessage ||
            'Mật khẩu không chính xác. Vui lòng kiểm tra lại.'
          );
        } else {
          this.errorCode.set('GENERIC');
          let customMsg = response?.error?.defaultMessage || response?.error?.detail;
          if (customMsg && !customMsg.toLowerCase().includes('bad request')) {
            this.errorMessage.set(customMsg);
          } else {
            this.errorMessage.set('Đăng nhập không thành công! Vui lòng kiểm tra lại tài khoản và mật khẩu.');
          }
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
