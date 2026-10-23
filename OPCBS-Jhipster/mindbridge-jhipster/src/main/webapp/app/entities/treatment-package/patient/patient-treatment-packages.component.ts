import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TreatmentPackageService } from '../service/treatment-package.service';
import { ITreatmentGoal, ITreatmentPackage, ITreatmentProgressSummary, ITreatmentSession, TreatmentPackageStatus } from '../treatment-package.model';

@Component({
  selector: 'jhi-patient-treatment-packages',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgbModule],
  templateUrl: './patient-treatment-packages.component.html',
  styleUrls: ['./patient-treatment-packages.component.scss']
})
export class PatientTreatmentPackagesComponent implements OnInit {
  private packageService = inject(TreatmentPackageService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  packages: ITreatmentPackage[] = [];
  isLoading = false;
  viewMode: 'active' | 'history' = 'active';
  searchTerm = '';

  selectedPackage: ITreatmentPackage | null = null;
  rejectionReason = '';
  isProcessing = false;
  successMessage = '';
  errorMessage = '';

  // Hold State
  holdDurationDays = 30;
  holdReason = 'Bận việc gia đình / công tác cá nhân';
  isProcessingHold = false;

  // Clinical Details View
  sessions: ITreatmentSession[] = [];
  goals: ITreatmentGoal[] = [];
  progressSummary: ITreatmentProgressSummary | null = null;
  activeTab: 'overview' | 'sessions' | 'goals' = 'overview';

  ngOnInit(): void {
    this.loadPackages();
  }

  loadPackages(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.packageService.getPatientPackages().subscribe({
      next: res => {
        this.packages = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get pendingCount(): number {
    return this.packages.filter(p => p.status === 'PROPOSED').length;
  }

  get onHoldCount(): number {
    return this.packages.filter(p => p.status === 'ON_HOLD' || p.isHoldRequested).length;
  }

  get activePackages(): ITreatmentPackage[] {
    return this.packages.filter(p => {
      const status = p.status;
      const matches = status === 'ACTIVE' || status === 'ON_HOLD' || status === 'PROPOSED' || status === 'CREATED';
      if (!matches) return false;
      if (!this.searchTerm.trim()) return true;
      const term = this.searchTerm.toLowerCase();
      return (p.name || '').toLowerCase().includes(term) ||
             (p.doctor?.fullName || '').toLowerCase().includes(term) ||
             (p.targetOutcome || '').toLowerCase().includes(term);
    });
  }

  get historyPackages(): ITreatmentPackage[] {
    return this.packages.filter(p => {
      const status = p.status;
      const matches = status === 'COMPLETED' || status === 'REJECTED' || status === 'CANCELLED' || status === 'EXPIRED';
      if (!matches) return false;
      if (!this.searchTerm.trim()) return true;
      const term = this.searchTerm.toLowerCase();
      return (p.name || '').toLowerCase().includes(term) ||
             (p.doctor?.fullName || '').toLowerCase().includes(term);
    });
  }

  get displayList(): ITreatmentPackage[] {
    return this.viewMode === 'history' ? this.historyPackages : this.activePackages;
  }

  setViewMode(mode: 'active' | 'history'): void {
    this.viewMode = mode;
  }

  getUsedSessions(pkg: ITreatmentPackage): number {
    const total = pkg.sessionQuantity || 0;
    const remaining = pkg.remainingSessions !== undefined && pkg.remainingSessions !== null ? pkg.remainingSessions : total;
    return Math.max(0, total - remaining);
  }

  getProgressPercentage(pkg: ITreatmentPackage): number {
    const total = pkg.sessionQuantity || 1;
    const used = this.getUsedSessions(pkg);
    return Math.min(100, Math.round((used / total) * 100));
  }

  openDetailModal(content: any, pkg: ITreatmentPackage): void {
    this.selectedPackage = pkg;
    this.rejectionReason = '';
    this.errorMessage = '';
    this.activeTab = 'overview';

    if (pkg.id) {
      this.packageService.getSessions(pkg.id).subscribe(s => this.sessions = s || []);
      this.packageService.getGoals(pkg.id).subscribe(g => this.goals = g || []);
      this.packageService.getProgressSummary(pkg.id).subscribe(summary => this.progressSummary = summary);
    }

    this.modalService.open(content, { size: 'lg', centered: true, scrollable: true });
  }

  // === Nghiệp vụ Bảo lưu (Hold Lifecycle) ===
  openRequestHoldModal(content: any, pkg: ITreatmentPackage): void {
    this.selectedPackage = pkg;
    this.holdDurationDays = 30;
    this.holdReason = 'Bận việc gia đình / công tác cá nhân';
    this.modalService.open(content, { size: 'md', centered: true });
  }

  submitRequestHold(modal: any): void {
    if (!this.selectedPackage?.id) return;
    this.isProcessingHold = true;
    this.packageService.requestHold(this.selectedPackage.id, this.holdDurationDays, this.holdReason).subscribe({
      next: () => {
        this.isProcessingHold = false;
        modal.close();
        this.successMessage = 'Đã gửi yêu cầu bảo lưu đến bác sĩ điều trị!';
        this.loadPackages();
        setTimeout(() => (this.successMessage = ''), 5000);
      },
      error: err => {
        this.isProcessingHold = false;
        alert(err.error?.message || 'Không thể gửi yêu cầu bảo lưu!');
      }
    });
  }

  cancelHold(pkgId?: number): void {
    if (!pkgId) return;
    if (confirm('Bạn có chắc chắn muốn hủy yêu cầu bảo lưu này không?')) {
      this.packageService.cancelHoldRequest(pkgId).subscribe({
        next: () => {
          this.successMessage = 'Đã hủy yêu cầu bảo lưu thành công.';
          this.loadPackages();
          setTimeout(() => (this.successMessage = ''), 4000);
        },
        error: err => {
          alert(err.error?.message || 'Không thể hủy yêu cầu bảo lưu!');
        }
      });
    }
  }

  resumePackage(pkgId?: number): void {
    if (!pkgId) return;
    if (confirm('Bạn có muốn mở lại gói trị liệu trước hạn bảo lưu không?')) {
      this.packageService.resumePackage(pkgId).subscribe({
        next: () => {
          this.successMessage = 'Phác đồ điều trị đã được kích hoạt lại thành công!';
          this.loadPackages();
          setTimeout(() => (this.successMessage = ''), 4000);
        },
        error: err => {
          alert(err.error?.message || 'Không thể kích hoạt lại gói!');
        }
      });
    }
  }

  acceptPackage(modal: any, pkg: ITreatmentPackage): void {
    if (!pkg.id) return;
    this.isProcessing = true;
    this.packageService.acceptPackage(pkg.id).subscribe({
      next: () => {
        this.isProcessing = false;
        this.successMessage = 'Tiếp nhận gói trị liệu thành công!';
        modal.close();
        this.loadPackages();
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: err => {
        this.isProcessing = false;
        this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi xác nhận gói!';
      }
    });
  }

  rejectPackage(modal: any, pkg: ITreatmentPackage): void {
    if (!pkg.id) return;
    this.isProcessing = true;
    this.packageService.rejectPackage(pkg.id, this.rejectionReason).subscribe({
      next: () => {
        this.isProcessing = false;
        this.successMessage = 'Đã từ chối gói trị liệu.';
        modal.close();
        this.loadPackages();
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: err => {
        this.isProcessing = false;
        this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi từ chối gói!';
      }
    });
  }

  getStatusBadgeClass(status?: TreatmentPackageStatus | null, isHoldRequested?: boolean | null): string {
    if (isHoldRequested) {
      return 'pp-status pending border border-warning';
    }
    switch (status) {
      case 'ACTIVE':
        return 'pp-status active';
      case 'ON_HOLD':
        return 'pp-status pending';
      case 'PROPOSED':
        return 'pp-status pending';
      case 'COMPLETED':
        return 'pp-status completed';
      case 'REJECTED':
        return 'pp-status rejected';
      case 'CANCELLED':
        return 'pp-status cancelled';
      case 'EXPIRED':
        return 'pp-status expired';
      default:
        return 'pp-status pending';
    }
  }

  getStatusLabel(status?: TreatmentPackageStatus | null, isHoldRequested?: boolean | null): string {
    if (isHoldRequested) {
      return 'Đang chờ duyệt bảo lưu';
    }
    switch (status) {
      case 'ACTIVE':
        return 'Đang điều trị';
      case 'ON_HOLD':
        return 'Tạm dừng / Bảo lưu';
      case 'PROPOSED':
        return 'Chờ bạn phản hồi';
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'REJECTED':
        return 'Đã từ chối';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'EXPIRED':
        return 'Đã hết hạn';
      default:
        return 'Bản nháp';
    }
  }
}
