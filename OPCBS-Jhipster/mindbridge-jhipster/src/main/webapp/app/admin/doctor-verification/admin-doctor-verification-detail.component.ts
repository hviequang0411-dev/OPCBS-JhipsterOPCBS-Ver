import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AdminDoctorVerificationService } from './admin-doctor-verification.service';
import { IDoctorVerification } from 'app/entities/doctor-profile/verification/doctor-verification.model';

@Component({
  selector: 'jhi-admin-doctor-verification-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-doctor-verification-detail.component.html',
  styleUrls: ['./admin-doctor-verification-detail.component.scss']
})
export default class AdminDoctorVerificationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly verificationService = inject(AdminDoctorVerificationService);
  private readonly sanitizer = inject(DomSanitizer);

  verification = signal<IDoctorVerification | null>(null);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Review forms
  rejectionReason = signal<string>('');
  additionalInfoReason = signal<string>('');

  isPending = computed(() => {
    const status = this.verification()?.status?.toUpperCase();
    return status === 'SUBMITTED' || status === 'PENDING' || status === 'REQUIRES_ADDITIONAL_INFO' || status === 'DRAFT';
  });

  isCertificateImage = computed(() => {
    const url = this.verification()?.certificateUrl?.toLowerCase() || '';
    return url.endsWith('.jpg') || url.endsWith('.jpeg') || url.endsWith('.png') || url.endsWith('.webp') || url.includes('/image/upload/');
  });

  isCertificatePdf = computed(() => {
    const url = this.verification()?.certificateUrl?.toLowerCase() || '';
    return url.endsWith('.pdf') || url.includes('/raw/upload/');
  });

  safePdfUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.verification()?.certificateUrl;
    if (url && this.isCertificatePdf()) {
      return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }
    return null;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadVerification(Number(id));
    } else {
      this.errorMessage.set('Không tìm thấy mã hồ sơ xác thực.');
      this.isLoading.set(false);
    }
  }

  loadVerification(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.verificationService.getVerificationById(id).subscribe({
      next: (data) => {
        this.verification.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading verification detail:', err);
        this.errorMessage.set(err?.error?.message || 'Không thể tải chi tiết hồ sơ xác thực.');
        this.isLoading.set(false);
      }
    });
  }

  approve(): void {
    const v = this.verification();
    if (!v?.id) return;

    if (!confirm(`Xác nhận phê duyệt chứng chỉ y khoa và kích hoạt tài khoản bác sĩ ${v.doctorName}?`)) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.verificationService.approveVerification(v.id).subscribe({
      next: (updated) => {
        this.verification.set(updated);
        this.isSubmitting.set(false);
        this.successMessage.set(`Đã phê duyệt thành công hồ sơ của bác sĩ ${updated.doctorName}! Tài khoản bác sĩ đã được kích hoạt.`);
        setTimeout(() => this.successMessage.set(null), 6000);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi phê duyệt hồ sơ.');
      }
    });
  }

  reject(): void {
    const v = this.verification();
    if (!v?.id) return;

    if (!this.rejectionReason().trim()) {
      this.errorMessage.set('Vui lòng nhập lý do từ chối hồ sơ.');
      return;
    }

    if (!confirm(`Xác nhận từ chối hồ sơ bác sĩ ${v.doctorName}?`)) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.verificationService.rejectVerification(v.id, this.rejectionReason()).subscribe({
      next: (updated) => {
        this.verification.set(updated);
        this.isSubmitting.set(false);
        this.successMessage.set(`Đã từ chối hồ sơ của bác sĩ ${updated.doctorName}. Lý do đã được lưu và gửi phản hồi.`);
        setTimeout(() => this.successMessage.set(null), 6000);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi từ chối hồ sơ.');
      }
    });
  }

  requestAdditionalInfo(): void {
    const v = this.verification();
    if (!v?.id) return;

    if (!this.additionalInfoReason().trim()) {
      this.errorMessage.set('Vui lòng nhập hướng dẫn cụ thể về tài liệu/thông tin cần bổ sung.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.verificationService.requestAdditionalInfo(v.id, this.additionalInfoReason()).subscribe({
      next: (updated) => {
        this.verification.set(updated);
        this.isSubmitting.set(false);
        this.successMessage.set(`Đã gửi yêu cầu bổ sung thông tin đến bác sĩ ${updated.doctorName}. Bác sĩ có thể đăng nhập để tải lại chứng chỉ.`);
        setTimeout(() => this.successMessage.set(null), 6000);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi xảy ra khi gửi yêu cầu bổ sung.');
      }
    });
  }

  getStatusBadge(status?: string): { label: string; class: string; icon: string } {
    switch (status?.toUpperCase()) {
      case 'APPROVED':
        return { label: 'Đã Duyệt & Kích Hoạt', class: 'status-approved', icon: 'bi-check-circle-fill' };
      case 'SUBMITTED':
      case 'PENDING':
        return { label: 'Chờ Phê Duyệt', class: 'status-pending', icon: 'bi-hourglass-split' };
      case 'REQUIRES_ADDITIONAL_INFO':
        return { label: 'Cần Bổ Sung Thông Tin', class: 'status-info-needed', icon: 'bi-info-circle-fill' };
      case 'REJECTED':
        return { label: 'Hồ Sơ Bị Từ Chối', class: 'status-rejected', icon: 'bi-x-circle-fill' };
      default:
        return { label: 'Bản Nháp (Chưa Gửi)', class: 'status-draft', icon: 'bi-pencil-square' };
    }
  }

  getInitials(name?: string): string {
    if (!name) return 'BS';
    const parts = name.trim().split(' ').filter(p => p.length > 0);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }
}
