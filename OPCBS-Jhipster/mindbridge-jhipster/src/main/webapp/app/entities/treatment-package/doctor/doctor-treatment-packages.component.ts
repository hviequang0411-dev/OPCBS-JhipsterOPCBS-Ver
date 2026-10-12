import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TreatmentPackageService } from '../service/treatment-package.service';
import { ITreatmentPackage, TreatmentPackageStatus } from '../treatment-package.model';
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

  // Package Form Modal state
  editingPackage: ITreatmentPackage = this.getEmptyPackage();
  isEditMode = false;
  isSaving = false;
  errorMessage = '';

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

  get filteredPackages(): ITreatmentPackage[] {
    if (this.selectedStatusFilter === 'ALL') {
      return this.packages;
    }
    return this.packages.filter(p => p.status === this.selectedStatusFilter);
  }

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

  getStatusBadgeClass(status?: TreatmentPackageStatus | null): string {
    switch (status) {
      case 'ACTIVE':
        return 'badge bg-success';
      case 'PROPOSED':
        return 'badge bg-warning text-dark';
      case 'COMPLETED':
        return 'badge bg-primary';
      case 'REJECTED':
      case 'CANCELLED':
        return 'badge bg-danger';
      case 'EXPIRED':
        return 'badge bg-secondary';
      default:
        return 'badge bg-info text-dark';
    }
  }

  getStatusLabel(status?: TreatmentPackageStatus | null): string {
    switch (status) {
      case 'ACTIVE':
        return 'Đang hoạt động';
      case 'PROPOSED':
        return 'Đã đề xuất cho bệnh nhân';
      case 'COMPLETED':
        return 'Hoàn thành';
      case 'REJECTED':
        return 'Bị từ chối';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'EXPIRED':
        return 'Đã hết hạn';
      default:
        return 'Bản nháp / Mới tạo';
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
}
