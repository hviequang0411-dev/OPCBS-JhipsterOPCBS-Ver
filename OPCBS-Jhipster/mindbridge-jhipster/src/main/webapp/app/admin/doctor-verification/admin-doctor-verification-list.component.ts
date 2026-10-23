import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminDoctorVerificationService } from './admin-doctor-verification.service';
import { IDoctorVerification, IVerificationSummary } from 'app/entities/doctor-profile/verification/doctor-verification.model';

@Component({
  selector: 'jhi-admin-doctor-verification-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-doctor-verification-list.component.html',
  styleUrls: ['./admin-doctor-verification-list.component.scss']
})
export default class AdminDoctorVerificationListComponent implements OnInit {
  private readonly verificationService = inject(AdminDoctorVerificationService);

  verifications = signal<IDoctorVerification[]>([]);
  summary = signal<IVerificationSummary>({
    total: 0,
    submitted: 0,
    requiresAdditionalInfo: 0,
    approved: 0,
    rejected: 0
  });

  isLoading = signal<boolean>(false);
  currentStatus = signal<string>('ALL');
  searchTerm = signal<string>('');
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Quick action modal state
  selectedForQuickAction = signal<IDoctorVerification | null>(null);
  quickActionType = signal<'APPROVE' | 'REJECT' | 'REQUEST_INFO' | null>(null);
  quickActionReason = signal<string>('');
  isActionLoading = signal<boolean>(false);
  actionError = signal<string | null>(null);
  actionSuccess = signal<string | null>(null);

  ngOnInit(): void {
    this.loadSummary();
    this.loadVerifications();
  }

  loadSummary(): void {
    this.verificationService.getVerificationSummary().subscribe({
      next: (data) => this.summary.set(data),
      error: (err) => console.error('Error loading summary:', err)
    });
  }

  loadVerifications(): void {
    this.isLoading.set(true);
    this.verificationService.getAllVerifications(
      this.currentPage(),
      this.pageSize(),
      this.currentStatus(),
      this.searchTerm()
    ).subscribe({
      next: (res) => {
        this.verifications.set(res.content || []);
        this.totalElements.set(res.totalElements || 0);
        this.totalPages.set(res.totalPages || 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading verifications:', err);
        this.isLoading.set(false);
      }
    });
  }

  onFilterStatus(status: string): void {
    this.currentStatus.set(status);
    this.currentPage.set(0);
    this.loadVerifications();
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadVerifications();
  }

  clearSearch(): void {
    this.searchTerm.set('');
    this.currentPage.set(0);
    this.loadVerifications();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadVerifications();
    }
  }

  // Quick Action Modal helpers
  openQuickAction(verif: IDoctorVerification, type: 'APPROVE' | 'REJECT' | 'REQUEST_INFO'): void {
    this.selectedForQuickAction.set(verif);
    this.quickActionType.set(type);
    this.quickActionReason.set('');
    this.actionError.set(null);
  }

  closeQuickAction(): void {
    this.selectedForQuickAction.set(null);
    this.quickActionType.set(null);
    this.quickActionReason.set('');
    this.actionError.set(null);
  }

  executeQuickAction(): void {
    const verif = this.selectedForQuickAction();
    const type = this.quickActionType();
    if (!verif || !verif.id || !type) return;

    if ((type === 'REJECT' || type === 'REQUEST_INFO') && !this.quickActionReason().trim()) {
      this.actionError.set('Vui lòng nhập lý do cụ thể để gửi thông báo cho bác sĩ.');
      return;
    }

    this.isActionLoading.set(true);
    this.actionError.set(null);

    let obs;
    if (type === 'APPROVE') {
      obs = this.verificationService.approveVerification(verif.id);
    } else if (type === 'REJECT') {
      obs = this.verificationService.rejectVerification(verif.id, this.quickActionReason());
    } else {
      obs = this.verificationService.requestAdditionalInfo(verif.id, this.quickActionReason());
    }

    obs.subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.actionSuccess.set(
          type === 'APPROVE'
            ? `Đã duyệt thành công hồ sơ của bác sĩ ${verif.doctorName}!`
            : type === 'REJECT'
            ? `Đã từ chối hồ sơ của bác sĩ ${verif.doctorName}.`
            : `Đã gửi yêu cầu bổ sung thông tin cho bác sĩ ${verif.doctorName}.`
        );
        this.closeQuickAction();
        this.loadSummary();
        this.loadVerifications();

        setTimeout(() => this.actionSuccess.set(null), 5000);
      },
      error: (err) => {
        this.isActionLoading.set(false);
        this.actionError.set(err?.error?.message || 'Có lỗi xảy ra khi xử lý yêu cầu.');
      }
    });
  }

  getStatusBadge(status?: string): { label: string; class: string; icon: string } {
    switch (status?.toUpperCase()) {
      case 'APPROVED':
        return { label: 'Đã Duyệt', class: 'status-approved', icon: 'bi-check-circle-fill' };
      case 'SUBMITTED':
      case 'PENDING':
        return { label: 'Chờ Duyệt', class: 'status-pending', icon: 'bi-hourglass-split' };
      case 'REQUIRES_ADDITIONAL_INFO':
        return { label: 'Cần Bổ Sung', class: 'status-info-needed', icon: 'bi-info-circle-fill' };
      case 'REJECTED':
        return { label: 'Từ Chối', class: 'status-rejected', icon: 'bi-x-circle-fill' };
      default:
        return { label: 'Bản Nháp', class: 'status-draft', icon: 'bi-pencil-square' };
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
