import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { PsychometricsService } from '../service/psychometrics.service';
import { IPsychometricTest, IPsychometricSubmission } from '../psychometrics.model';
import { PatientProfileService } from 'app/entities/patient-profile/service/patient-profile.service';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

@Component({
  selector: 'jhi-doctor-psychometrics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgbModule],
  templateUrl: './doctor-psychometrics.component.html',
  styleUrls: ['./doctor-psychometrics.component.scss']
})
export class DoctorPsychometricsComponent implements OnInit {
  private psychometricsService = inject(PsychometricsService);
  private patientService = inject(PatientProfileService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  tests: IPsychometricTest[] = [];
  patients: IPatientProfile[] = [];
  submissions: IPsychometricSubmission[] = [];
  selectedPatientId: number | null = null;
  isLoading = false;

  // Assign modal state
  selectedTestId: number | null = null;
  assignPatientId: number | null = null;
  dueDate: string = '';
  doctorNotes: string = '';
  isSaving = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadTests();
    this.loadPatients();
  }

  loadTests(): void {
    this.psychometricsService.getAllActiveTests().subscribe({
      next: res => {
        this.tests = res || [];
        if (this.tests.length > 0) {
          this.selectedTestId = this.tests[0].id || null;
        }
        this.cdr.markForCheck();
      },
      error: () => {
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

  onPatientChange(): void {
    if (!this.selectedPatientId) {
      this.submissions = [];
      this.cdr.markForCheck();
      return;
    }
    this.isLoading = true;
    this.cdr.markForCheck();
    this.psychometricsService.getPatientPsychometricsForDoctor(this.selectedPatientId).subscribe({
      next: res => {
        this.submissions = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openAssignModal(content: any): void {
    this.assignPatientId = this.selectedPatientId;
    this.dueDate = '';
    this.doctorNotes = '';
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg' });
  }

  assignTest(modal: any): void {
    if (!this.selectedTestId || !this.assignPatientId) {
      this.errorMessage = 'Vui lòng chọn bài test và bệnh nhân cần chỉ định!';
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';

    const payload = {
      testId: this.selectedTestId,
      patientId: this.assignPatientId,
      dueDate: this.dueDate ? new Date(this.dueDate).toISOString() : undefined,
      doctorNotes: this.doctorNotes
    };

    this.psychometricsService.assignTest(payload).subscribe({
      next: () => {
        this.isSaving = false;
        modal.close();
        this.selectedPatientId = this.assignPatientId;
        this.onPatientChange();
      },
      error: err => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi chỉ định bài test!';
      }
    });
  }
}
