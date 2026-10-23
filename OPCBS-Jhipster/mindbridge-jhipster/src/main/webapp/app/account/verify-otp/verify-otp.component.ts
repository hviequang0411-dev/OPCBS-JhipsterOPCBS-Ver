import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OtpService } from 'app/core/auth';

const SESSION_OTP_KEY = 'mb_last_verify_account';

@Component({
  selector: 'jhi-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './verify-otp.component.html',
  styleUrls: ['./verify-otp.component.scss'],
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  @ViewChild('digit0') digit0Ref!: ElementRef<HTMLInputElement>;
  @ViewChild('digit1') digit1Ref!: ElementRef<HTMLInputElement>;
  @ViewChild('digit2') digit2Ref!: ElementRef<HTMLInputElement>;
  @ViewChild('digit3') digit3Ref!: ElementRef<HTMLInputElement>;
  @ViewChild('digit4') digit4Ref!: ElementRef<HTMLInputElement>;
  @ViewChild('digit5') digit5Ref!: ElementRef<HTMLInputElement>;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly otpService = inject(OtpService);

  emailOrLogin = '';
  inputAccount = '';
  isEditingAccount = false;

  otpDigits: string[] = ['', '', '', '', '', ''];
  isSubmitting = false;
  isResending = false;
  errorMessage = '';
  successMessage = '';

  // 1 minute 30 seconds limit = 90 seconds
  readonly initialSeconds = 90;
  countdown = 90;
  isExpired = false;
  private timerInterval?: any;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const urlAccount = params['email'] || params['username'] || '';
      if (urlAccount) {
        this.emailOrLogin = urlAccount.trim();
        sessionStorage.setItem(SESSION_OTP_KEY, this.emailOrLogin);
      } else {
        // Retrieve from session storage if user navigated away or reloaded
        const cached = sessionStorage.getItem(SESSION_OTP_KEY);
        if (cached) {
          this.emailOrLogin = cached;
        } else {
          this.isEditingAccount = true;
        }
      }

      this.inputAccount = this.emailOrLogin;
      if (this.emailOrLogin) {
        this.startCountdown();
      }

      const devOtp = params['devOtp'] || sessionStorage.getItem('mb_dev_otp_hint');
      if (devOtp && typeof devOtp === 'string' && devOtp.length === 6) {
        this.successMessage = `Mã OTP xác thực đã được gửi về email! (Gợi ý test nhanh: ${devOtp})`;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  get formattedCountdown(): string {
    const minutes = Math.floor(this.countdown / 60);
    const seconds = this.countdown % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  saveAccountAndSendOtp(): void {
    if (!this.inputAccount || !this.inputAccount.trim()) {
      this.errorMessage = 'Vui lòng nhập Email hoặc Tên đăng nhập của bạn.';
      return;
    }

    this.emailOrLogin = this.inputAccount.trim();
    sessionStorage.setItem(SESSION_OTP_KEY, this.emailOrLogin);
    this.isEditingAccount = false;
    this.resendOtp();
  }

  changeAccount(): void {
    this.isEditingAccount = true;
    this.inputAccount = this.emailOrLogin;
  }

  startCountdown(): void {
    this.countdown = this.initialSeconds;
    this.isExpired = false;

    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }

    this.timerInterval = setInterval(() => {
      if (this.countdown > 0) {
        this.countdown--;
      } else {
        this.isExpired = true;
        this.errorMessage = 'Mã OTP đã hết hạn sau 1 phút 30 giây. Vui lòng bấm "Gửi lại mã OTP" để nhận mã mới.';
        clearInterval(this.timerInterval);
      }
    }, 1000);
  }

  getDigitRef(index: number): ElementRef<HTMLInputElement> | undefined {
    switch (index) {
      case 0: return this.digit0Ref;
      case 1: return this.digit1Ref;
      case 2: return this.digit2Ref;
      case 3: return this.digit3Ref;
      case 4: return this.digit4Ref;
      case 5: return this.digit5Ref;
      default: return undefined;
    }
  }

  onDigitInput(event: any, index: number): void {
    const val = (event.target.value || '').trim();
    if (val.length >= 1) {
      this.otpDigits[index] = val.substring(val.length - 1);
      if (index < 5) {
        const nextInput = this.getDigitRef(index + 1);
        nextInput?.nativeElement.focus();
      }
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.otpDigits[index] && index > 0) {
      const prevInput = this.getDigitRef(index - 1);
      prevInput?.nativeElement.focus();
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text') || '';
    const digits = pastedData.replace(/\D/g, '').substring(0, 6);
    if (digits.length > 0) {
      for (let i = 0; i < 6; i++) {
        this.otpDigits[i] = digits[i] || '';
      }
      const lastIndex = Math.min(digits.length - 1, 5);
      this.getDigitRef(lastIndex)?.nativeElement.focus();
    }
  }

  isCompleteOtp(): boolean {
    return this.otpDigits.every(d => d.trim().length === 1);
  }

  submitOtp(): void {
    if (!this.emailOrLogin) {
      this.errorMessage = 'Vui lòng nhập email hoặc tài khoản cần xác thực.';
      this.isEditingAccount = true;
      return;
    }

    if (!this.isCompleteOtp() || this.isSubmitting) return;

    if (this.isExpired) {
      this.errorMessage = 'Mã OTP đã hết hạn sau 1 phút 30 giây. Vui lòng bấm "Gửi lại mã OTP" bên dưới.';
      return;
    }

    const fullOtp = this.otpDigits.join('');
    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.otpService.verifyOtp({ emailOrLogin: this.emailOrLogin, otp: fullOtp }).subscribe({
      next: () => {
        this.isSubmitting = false;
        if (this.timerInterval) {
          clearInterval(this.timerInterval);
        }
        sessionStorage.removeItem(SESSION_OTP_KEY);
        this.successMessage = 'Tài khoản của bạn đã được kích hoạt thành công! Đang chuyển đến trang đăng nhập...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: err => {
        this.isSubmitting = false;
        const detail = err?.error?.detail || err?.error?.title || err?.error?.message;
        const errorKey = err?.error?.errorKey;

        if (errorKey === 'otpExpired' || detail?.includes('hết hạn')) {
          this.isExpired = true;
          this.errorMessage = 'Mã OTP đã hết hiệu lực sau 1 phút 30 giây. Vui lòng bấm "Gửi lại mã OTP" để nhận mã mới.';
        } else if (errorKey === 'alreadyActivated' || detail?.includes('kích hoạt trước đó')) {
          this.successMessage = 'Tài khoản này đã được kích hoạt trước đó! Bạn có thể đăng nhập ngay.';
          setTimeout(() => this.router.navigate(['/login']), 1500);
        } else if (errorKey === 'userNotFound' || detail?.includes('Không tìm thấy')) {
          this.errorMessage = 'Không tìm thấy tài khoản với thông tin này. Vui lòng kiểm tra lại.';
        } else {
          this.errorMessage = detail || 'Mã OTP không chính xác. Vui lòng kiểm tra lại.';
        }
      },
    });
  }

  resendOtp(): void {
    if (!this.emailOrLogin) {
      this.errorMessage = 'Vui lòng nhập Email hoặc Tên đăng nhập của bạn trước.';
      this.isEditingAccount = true;
      return;
    }

    if (this.isResending) return;

    this.isResending = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.otpService.resendOtp({ emailOrLogin: this.emailOrLogin }).subscribe({
      next: (res: any) => {
        this.isResending = false;
        this.otpDigits = ['', '', '', '', '', ''];
        this.startCountdown();
        const devOtp = res?.otp;
        if (devOtp) {
          sessionStorage.setItem('mb_dev_otp_hint', devOtp);
          this.successMessage = `Mã OTP 6 số mới đã được gửi về email của bạn! (Gợi ý test nhanh: ${devOtp})`;
        } else {
          this.successMessage = 'Mã OTP 6 số mới đã được gửi về email của bạn!';
        }
        setTimeout(() => {
          this.digit0Ref?.nativeElement.focus();
        }, 300);
      },
      error: err => {
        this.isResending = false;
        const detail = err?.error?.detail || err?.error?.title || err?.error?.message;
        this.errorMessage = detail || 'Không thể gửi lại mã OTP. Vui lòng kiểm tra lại tài khoản hoặc email của bạn.';
      },
    });
  }
}
