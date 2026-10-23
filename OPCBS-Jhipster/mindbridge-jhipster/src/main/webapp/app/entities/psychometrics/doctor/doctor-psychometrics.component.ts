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

  // Library toolbar state (Matching C# Razor layout)
  libraryTab: 'all' | 'system' | 'my' = 'all';
  librarySearch = '';

  // Assign modal state
  selectedTestId: number | null = null;
  assignPatientId: number | null = null;
  dueDate: string = '';
  doctorNotes: string = '';
  isSaving = false;
  errorMessage = '';

  // Detail modals
  selectedTestForDetails: IPsychometricTest | null = null;
  selectedSubmissionForDetails: IPsychometricSubmission | null = null;

  ngOnInit(): void {
    this.loadTests();
    this.loadPatients();
  }

  loadTests(): void {
    this.psychometricsService.getAllActiveTests().subscribe({
      next: res => {
        this.tests = res || [];
        if (this.tests.length > 0 && !this.selectedTestId) {
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

  isSystemTest(t: IPsychometricTest): boolean {
    return t.isSystemTemplate === true || (!t.doctor && !t.createdByDoctor);
  }

  get systemTemplateCount(): number {
    return this.tests.filter(t => this.isSystemTest(t)).length;
  }

  get myAssessmentCount(): number {
    return this.tests.filter(t => !this.isSystemTest(t)).length;
  }

  get filteredLibrary(): IPsychometricTest[] {
    let list = this.tests;
    if (this.libraryTab === 'system') {
      list = list.filter(t => this.isSystemTest(t));
    } else if (this.libraryTab === 'my') {
      list = list.filter(t => !this.isSystemTest(t));
    }

    if (this.librarySearch && this.librarySearch.trim()) {
      const q = this.librarySearch.trim().toLowerCase();
      list = list.filter(t => 
        (t.title && t.title.toLowerCase().includes(q)) ||
        (t.category && t.category.toLowerCase().includes(q)) ||
        (t.description && t.description.toLowerCase().includes(q)) ||
        (t.testType && t.testType.toLowerCase().includes(q))
      );
    }
    return list;
  }

  setLibraryTab(tab: 'all' | 'system' | 'my'): void {
    this.libraryTab = tab;
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
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  openAssignModalForTest(content: any, test: IPsychometricTest): void {
    this.selectedTestId = test.id || null;
    this.assignPatientId = this.selectedPatientId;
    this.dueDate = '';
    this.doctorNotes = '';
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  openTestDetailsModal(content: any, test: IPsychometricTest): void {
    this.selectedTestForDetails = test;
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  openSubmissionReportModal(content: any, sub: IPsychometricSubmission): void {
    this.selectedSubmissionForDetails = sub;
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
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

  getEffectiveSeverity(sub: IPsychometricSubmission): { label: string; bg: string; text: string; border: string; isHighRisk: boolean } {
    const text = (sub.interpretation || '').toLowerCase();
    if (text.includes('rất nặng') || text.includes('extremely severe')) {
      return { label: 'Rất Nặng (Nguy Cơ Cao)', bg: '#fee2e2', text: '#991b1b', border: '#fca5a5', isHighRisk: true };
    }
    if (text.includes('nặng') || text.includes('severe')) {
      return { label: 'Mức Độ Nặng', bg: '#ffedd5', text: '#c2410c', border: '#fdba74', isHighRisk: true };
    }
    if (text.includes('vừa') || text.includes('moderate')) {
      return { label: 'Mức Độ Vừa', bg: '#fef3c7', text: '#854d0e', border: '#fcd34d', isHighRisk: false };
    }
    if (text.includes('nhẹ') || text.includes('mild')) {
      return { label: 'Mức Độ Nhẹ', bg: '#eff6ff', text: '#1d4ed8', border: '#bfdbfe', isHighRisk: false };
    }
    return { label: 'Bình Thường', bg: '#ecfdf5', text: '#047857', border: '#a7f3d0', isHighRisk: false };
  }

  getParsedScoreData(jsonStr?: string | null): any {
    if (!jsonStr) return null;
    try {
      return JSON.parse(jsonStr);
    } catch {
      return null;
    }
  }
}
