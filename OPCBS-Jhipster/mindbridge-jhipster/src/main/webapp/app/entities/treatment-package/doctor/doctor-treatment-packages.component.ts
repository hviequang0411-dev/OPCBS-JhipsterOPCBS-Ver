import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TreatmentPackageService } from '../service/treatment-package.service';
import { ITreatmentGoal, ITreatmentPackage, ITreatmentProgressSummary, ITreatmentSession, TreatmentPackageStatus } from '../treatment-package.model';
import { PatientProfileService } from 'app/entities/patient-profile/service/patient-profile.service';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

@Component({
  selector: 'jhi-doctor-treatment-packages',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgbModule],
  templateUrl: './doctor-treatment-packages.component.html',
  styleUrls: ['./doctor-treatment-packages.component.scss']
})
export class DoctorTreatmentPackagesComponent implements OnInit {
  private packageService = inject(TreatmentPackageService);
  private patientService = inject(PatientProfileService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  packages: ITreatmentPackage[] = [];
  patients: IPatientProfile[] = [];
  isLoading = false;
  selectedStatusFilter = 'ALL';
  searchKeyword = '';

  // Form Modal state
  editingPackage: ITreatmentPackage = this.getEmptyPackage();
  isEditMode = false;
  isSaving = false;
  errorMessage = '';

  // Detail Modal & 5 Tabs state (Clone C# Gốc)
  activeDetailPackage: ITreatmentPackage | null = null;
  activeTab: 'overview' | 'sessions' | 'goals' | 'activities' | 'trajectory' = 'overview';
  sessions: ITreatmentSession[] = [];
  goals: ITreatmentGoal[] = [];
  progressSummary: ITreatmentProgressSummary | null = null;
  isLoadingDetails = false;

  // New Session Form
  newSession: ITreatmentSession = this.getEmptySession();
  isSavingSession = false;

  // New Goal Form
  newGoal: ITreatmentGoal = this.getEmptyGoal();
  isSavingGoal = false;

  // Hold Action Modal state
  targetHoldPackage: ITreatmentPackage | null = null;
  holdDoctorNote = '';
  isProcessingHold = false;

  ngOnInit(): void {
    this.loadPackages();
    this.loadPatients();
  }

  loadPackages(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.packageService.getDoctorPackages().subscribe({
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

  loadPatients(): void {
    this.patientService.query().subscribe({
      next: res => {
        this.patients = res.body || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.patients = [];
        this.cdr.markForCheck();
      }
    });
  }

  get totalCount(): number {
    return this.packages.length;
  }

  get activeCount(): number {
    return this.packages.filter(p => p.status === 'ACTIVE').length;
  }

  get onHoldCount(): number {
    return this.packages.filter(p => p.status === 'ON_HOLD' || p.isHoldRequested).length;
  }

  get proposedCount(): number {
    return this.packages.filter(p => p.status === 'PROPOSED').length;
  }

  get completedCount(): number {
    return this.packages.filter(p => p.status === 'COMPLETED').length;
  }

  get filteredPackages(): ITreatmentPackage[] {
    let list = this.packages;
    if (this.selectedStatusFilter === 'ON_HOLD') {
      list = list.filter(p => p.status === 'ON_HOLD' || p.isHoldRequested);
    } else if (this.selectedStatusFilter !== 'ALL') {
      list = list.filter(p => p.status === this.selectedStatusFilter);
    }
    if (this.searchKeyword && this.searchKeyword.trim()) {
      const q = this.searchKeyword.trim().toLowerCase();
      list = list.filter(p => 
        (p.name && p.name.toLowerCase().includes(q)) ||
        (p.description && p.description.toLowerCase().includes(q)) ||
        (p.targetOutcome && p.targetOutcome.toLowerCase().includes(q)) ||
        (p.patient?.fullName && p.patient.fullName.toLowerCase().includes(q)) ||
        (p.patient?.user?.login && p.patient.user.login.toLowerCase().includes(q))
      );
    }
    return list;
  }

  setStatusFilter(filter: string): void {
    this.selectedStatusFilter = filter;
  }

  // === Modal Xem Chi Tiết 5 Tab Phác Đồ ===
  openDetailsModal(content: any, pkg: ITreatmentPackage): void {
    this.activeDetailPackage = pkg;
    this.activeTab = 'overview';
    this.isLoadingDetails = true;
    this.newSession = this.getEmptySession();
    this.newGoal = this.getEmptyGoal();

    if (pkg.id) {
      // Tải song song Sessions, Goals và Database Metrics
      this.packageService.getSessions(pkg.id).subscribe({
        next: sList => {
          this.sessions = sList || [];
          this.cdr.markForCheck();
        }
      });
      this.packageService.getGoals(pkg.id).subscribe({
        next: gList => {
          this.goals = gList || [];
          this.cdr.markForCheck();
        }
      });
      this.packageService.getProgressSummary(pkg.id).subscribe({
        next: summary => {
          this.progressSummary = summary;
          this.isLoadingDetails = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.isLoadingDetails = false;
          this.cdr.markForCheck();
        }
      });
    }

    this.modalService.open(content, { size: 'xl', backdrop: 'static', scrollable: true });
  }

  // === Xử lý Buổi Tham Vấn (Sessions) ===
  saveSession(): void {
    if (!this.activeDetailPackage?.id || !this.newSession.title?.trim()) {
      alert('Vui lòng nhập tiêu đề hoặc chủ đề buổi tham vấn!');
      return;
    }
    this.isSavingSession = true;
    this.packageService.createSession(this.activeDetailPackage.id, this.newSession).subscribe({
      next: created => {
        this.sessions.push(created);
        this.newSession = this.getEmptySession();
        this.isSavingSession = false;
        // Cập nhật lại chỉ số DB
        if (this.activeDetailPackage?.id) {
          this.packageService.getProgressSummary(this.activeDetailPackage.id).subscribe(s => this.progressSummary = s);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.isSavingSession = false;
        alert('Có lỗi xảy ra khi lưu buổi tham vấn!');
      }
    });
  }

  // === Xử lý Mục Tiêu Điều Trị (Goals) ===
  saveGoal(): void {
    if (!this.activeDetailPackage?.id || !this.newGoal.title?.trim()) {
      alert('Vui lòng nhập tên mục tiêu điều trị!');
      return;
    }
    this.isSavingGoal = true;
    this.packageService.createGoal(this.activeDetailPackage.id, this.newGoal).subscribe({
      next: created => {
        this.goals.push(created);
        this.newGoal = this.getEmptyGoal();
        this.isSavingGoal = false;
        if (this.activeDetailPackage?.id) {
          this.packageService.getProgressSummary(this.activeDetailPackage.id).subscribe(s => this.progressSummary = s);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.isSavingGoal = false;
        alert('Có lỗi xảy ra khi lưu mục tiêu điều trị!');
      }
    });
  }

  updateGoalProgress(goal: ITreatmentGoal, newPercent: number): void {
    if (!this.activeDetailPackage?.id || !goal.id) return;
    goal.progressPercent = newPercent;
    if (newPercent >= 100) {
      goal.status = 'ACHIEVED';
    }
    this.packageService.updateGoal(this.activeDetailPackage.id, goal.id, goal).subscribe({
      next: updated => {
        const idx = this.goals.findIndex(g => g.id === updated.id);
        if (idx !== -1) {
          this.goals[idx] = updated;
        }
        if (this.activeDetailPackage?.id) {
          this.packageService.getProgressSummary(this.activeDetailPackage.id).subscribe(s => this.progressSummary = s);
        }
        this.cdr.markForCheck();
      }
    });
  }

  // === Vòng Đời Bảo Lưu (Hold Lifecycle) ===
  openApproveHoldModal(content: any, pkg: ITreatmentPackage): void {
    this.targetHoldPackage = pkg;
    this.holdDoctorNote = 'Bác sĩ đã chấp thuận bảo lưu lộ trình theo nguyện vọng của bệnh nhân.';
    this.modalService.open(content, { size: 'md', backdrop: 'static' });
  }

  confirmApproveHold(modal: any): void {
    if (!this.targetHoldPackage?.id) return;
    this.isProcessingHold = true;
    this.packageService.approveHold(this.targetHoldPackage.id, this.holdDoctorNote).subscribe({
      next: () => {
        this.isProcessingHold = false;
        modal.close();
        this.loadPackages();
      },
      error: err => {
        this.isProcessingHold = false;
        alert(err.error?.message || 'Không thể phê duyệt yêu cầu bảo lưu!');
      }
    });
  }

  openRejectHoldModal(content: any, pkg: ITreatmentPackage): void {
    this.targetHoldPackage = pkg;
    this.holdDoctorNote = 'Bác sĩ nhận định tiến trình tâm lý cần được duy trì liên tục để tránh tái phát triệu chứng.';
    this.modalService.open(content, { size: 'md', backdrop: 'static' });
  }

  confirmRejectHold(modal: any): void {
    if (!this.targetHoldPackage?.id) return;
    this.isProcessingHold = true;
    this.packageService.rejectHold(this.targetHoldPackage.id, this.holdDoctorNote).subscribe({
      next: () => {
        this.isProcessingHold = false;
        modal.close();
        this.loadPackages();
      },
      error: err => {
        this.isProcessingHold = false;
        alert(err.error?.message || 'Không thể từ chối yêu cầu bảo lưu!');
      }
    });
  }

  resumePackage(id?: number): void {
    if (!id) return;
    if (confirm('Bác sĩ có chắc chắn muốn kích hoạt lại phác đồ điều trị này ngay không?')) {
      this.packageService.resumePackage(id).subscribe({
        next: () => {
          this.loadPackages();
          if (this.activeDetailPackage?.id === id) {
            this.activeDetailPackage.status = 'ACTIVE';
            this.cdr.markForCheck();
          }
        },
        error: err => {
          alert(err.error?.message || 'Không thể kích hoạt lại phác đồ!');
        }
      });
    }
  }

  // === Modal Tạo/Sửa Phác Đồ ===
  openCreateModal(content: any): void {
    this.isEditMode = false;
    this.editingPackage = this.getEmptyPackage();
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  openEditModal(content: any, pkg: ITreatmentPackage): void {
    this.isEditMode = true;
    this.editingPackage = { ...pkg };
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  savePackage(modal: any): void {
    if (!this.editingPackage.name?.trim()) {
      this.errorMessage = 'Vui lòng nhập tên gói trị liệu!';
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';

    if (this.isEditMode && this.editingPackage.id) {
      this.packageService.updateDoctorPackage(this.editingPackage.id, this.editingPackage).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadPackages();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi cập nhật gói!';
        }
      });
    } else {
      this.packageService.createDoctorPackage(this.editingPackage).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadPackages();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi tạo gói!';
        }
      });
    }
  }

  deletePackage(pkg: ITreatmentPackage): void {
    if (!pkg.id) return;
    if (confirm(`Bạn có chắc chắn muốn xóa gói trị liệu "${pkg.name}" không?`)) {
      this.packageService.deleteDoctorPackage(pkg.id).subscribe({
        next: () => {
          this.loadPackages();
        }
      });
    }
  }

  getStatusBadgeClass(status?: TreatmentPackageStatus | null, isHoldRequested?: boolean | null): string {
    if (isHoldRequested) {
      return 'badge bg-warning text-dark border border-warning';
    }
    switch (status) {
      case 'ACTIVE':
        return 'badge bg-success';
      case 'ON_HOLD':
        return 'badge bg-warning text-dark';
      case 'PROPOSED':
        return 'badge bg-info text-dark';
      case 'COMPLETED':
        return 'badge bg-primary';
      case 'REJECTED':
      case 'CANCELLED':
        return 'badge bg-danger';
      case 'EXPIRED':
        return 'badge bg-secondary';
      default:
        return 'badge bg-light text-dark';
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
        return 'Đã đề xuất';
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'REJECTED':
        return 'Bị từ chối';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'EXPIRED':
        return 'Đã hết hạn';
      default:
        return 'Bản nháp';
    }
  }

  private getEmptyPackage(): ITreatmentPackage {
    return {
      name: '',
      description: '',
      targetOutcome: '',
      recommendedExercises: '',
      instructions: '',
      sessionQuantity: 4,
      remainingSessions: 4,
      validityDays: 30,
      recommendedSessionsPerWeek: 1,
      price: 0,
      status: 'CREATED'
    };
  }

  private getEmptySession(): ITreatmentSession {
    return {
      title: '',
      sessionSummary: '',
      doctorClinicalAssessment: '',
      patientFriendlySummary: '',
      moodBefore: 5,
      moodAfter: 7,
      status: 'COMPLETED'
    };
  }

  private getEmptyGoal(): ITreatmentGoal {
    return {
      title: '',
      description: '',
      category: 'EMOTION',
      priority: 'MEDIUM',
      status: 'IN_PROGRESS',
      progressPercent: 0
    };
  }
}
