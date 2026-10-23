import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import dayjs from 'dayjs/esm';
import { FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { IAppointment } from 'app/entities/appointment/appointment.model';
import { DoctorPatientService, IPatientDetailRecord, ISaveConsultationNoteRequest } from 'app/entities/doctor-profile/service/doctor-patient.service';

@Component({
  selector: 'jhi-doctor-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule, FormatMediumDatePipe, FormatMediumDatetimePipe],
  templateUrl: './doctor-patient-detail.component.html',
  styleUrls: ['./doctor-patient-detail.component.scss'],
})
export class DoctorPatientDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly doctorPatientService = inject(DoctorPatientService);

  readonly patientId = signal<number | null>(null);
  readonly patientRecord = signal<IPatientDetailRecord | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly activeTab = signal<'CLINICAL' | 'ANALYTICS' | 'TESTS' | 'JOURNALS' | 'PACKAGES' | 'OVERVIEW'>('CLINICAL');
  readonly Math = Math;

  // Modal / Form state for Creating Package
  readonly isCreatingPackage = signal<boolean>(false);
  readonly isSavingPackage = signal<boolean>(false);
  packageForm = {
    name: '',
    description: '',
    sessionQuantity: 6,
    validityDays: 60,
    price: 1800000,
    recommendedSessionsPerWeek: 1,
    targetOutcome: '',
    recommendedExercises: '',
    instructions: '',
  };

  // Modal / Form state for Consultation Note
  readonly isEditingNote = signal<boolean>(false);
  readonly selectedAppointment = signal<IAppointment | null>(null);
  noteForm: ISaveConsultationNoteRequest = {
    diagnosis: '',
    consultationSummary: '',
    therapyPlan: '',
    recommendations: '',
    followUpNotes: '',
    nextAppointmentRecommendedDate: null,
    markCompleted: true,
  };
  readonly isSavingNote = signal<boolean>(false);
  readonly isExportingPdf = signal<boolean>(false);

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.patientId.set(id);
        this.loadPatientDetail(id);
      }
    });
  }

  loadPatientDetail(id?: number): void {
    const targetId = id || this.patientId();
    if (!targetId) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.doctorPatientService.getPatientDetail(targetId).subscribe({
      next: data => {
        this.patientRecord.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        this.patientRecord.set(null);
        this.errorMessage.set('Không thể tải hồ sơ bệnh nhân hoặc bạn không có quyền truy cập hồ sơ này.');
        this.isLoading.set(false);
      },
    });
  }

  setActiveTab(tab: 'CLINICAL' | 'ANALYTICS' | 'TESTS' | 'JOURNALS' | 'PACKAGES' | 'OVERVIEW'): void {
    this.activeTab.set(tab);
  }

  openCreatePackageModal(): void {
    const pName = this.patientRecord()?.patient?.fullName || 'Bệnh nhân';
    this.packageForm = {
      name: `Gói Trị Liệu Tâm Lý Chuyên Sâu - ${pName}`,
      description: 'Liệu trình tâm lý cá nhân hóa can thiệp hành vi nhận thức và phục hồi cảm xúc',
      sessionQuantity: 6,
      validityDays: 60,
      price: 1800000,
      recommendedSessionsPerWeek: 1,
      targetOutcome: 'Kiểm soát căng thẳng, cải thiện giấc ngủ và thuyên giảm lo âu',
      recommendedExercises: 'Ghi nhật ký cảm xúc hàng ngày và thực hành bài tập chánh niệm',
      instructions: 'Tham gia đầy đủ các buổi tham vấn theo lịch hẹn đã thống nhất với bác sĩ',
    };
    this.isCreatingPackage.set(true);
  }

  closeCreatePackageModal(): void {
    this.isCreatingPackage.set(false);
  }

  saveTreatmentPackage(): void {
    const id = this.patientId();
    if (!id) return;

    this.isSavingPackage.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload = {
      ...this.packageForm,
      remainingSessions: this.packageForm.sessionQuantity,
      patient: { id },
    };

    this.doctorPatientService.createPackageForPatient(id, payload).subscribe({
      next: (createdPkg: any) => {
        this.isSavingPackage.set(false);
        this.isCreatingPackage.set(false);
        this.successMessage.set(`Đã tạo và gán gói trị liệu "${createdPkg.name || 'mới'}" cho bệnh nhân thành công!`);
        this.loadPatientDetail();
        setTimeout(() => this.successMessage.set(null), 5000);
      },
      error: (err: any) => {
        this.isSavingPackage.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi khi tạo gói trị liệu cho bệnh nhân.');
      },
    });
  }

  getMoodDistributionEntries(): { label: string; count: number; percentage: number; color: string }[] {
    const dist = this.patientRecord()?.moodDistribution || {};
    const total = Object.values(dist).reduce((a, b) => a + (b as number), 0) || 1;
    const colors: Record<string, string> = {
      'Rất tệ (1⭐)': '#ef4444',
      'Tệ (2⭐)': '#f97316',
      'Bình thường (3⭐)': '#eab308',
      'Tốt (4⭐)': '#10b981',
      'Rất tốt (5⭐)': '#06b6d4',
    };
    return Object.entries(dist).map(([label, count]) => ({
      label,
      count: count as number,
      percentage: Math.round(((count as number) / total) * 100),
      color: colors[label] || '#0f766e',
    }));
  }

  getTestSeverityEntries(): { label: string; count: number; percentage: number; color: string }[] {
    const dist = this.patientRecord()?.testSeverityDistribution || {};
    const total = Object.values(dist).reduce((a, b) => a + (b as number), 0) || 1;
    const colors: Record<string, string> = {
      'Bình thường': '#10b981',
      'Nhẹ (Mild)': '#06b6d4',
      'Vừa (Moderate)': '#f59e0b',
      'Nặng / Rất Nặng (Severe)': '#ef4444',
    };
    return Object.entries(dist).map(([label, count]) => ({
      label,
      count: count as number,
      percentage: Math.round(((count as number) / total) * 100),
      color: colors[label] || '#6366f1',
    }));
  }

  getAverageMood(): number {
    const journals = this.patientRecord()?.sharedJournals || [];
    if (journals.length === 0) return 0;
    const sum = journals.reduce((acc: number, j: any) => acc + (j.moodScale || 3), 0);
    return Math.round((sum / journals.length) * 10) / 10;
  }

  openNoteEditor(appointment: IAppointment): void {
    this.selectedAppointment.set(appointment);
    this.noteForm = {
      diagnosis: appointment.diagnosis ?? '',
      consultationSummary: appointment.consultationSummary ?? '',
      therapyPlan: appointment.therapyPlan ?? '',
      recommendations: appointment.recommendations ?? '',
      followUpNotes: appointment.followUpNotes ?? '',
      nextAppointmentRecommendedDate: appointment.nextAppointmentRecommendedDate
        ? dayjs(appointment.nextAppointmentRecommendedDate).format('YYYY-MM-DDTHH:mm')
        : null,
      markCompleted: true,
    };
    this.isEditingNote.set(true);
  }

  closeNoteEditor(): void {
    this.isEditingNote.set(false);
    this.selectedAppointment.set(null);
  }

  saveNote(): void {
    const app = this.selectedAppointment();
    if (!app) return;

    this.isSavingNote.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload: ISaveConsultationNoteRequest = {
      ...this.noteForm,
      nextAppointmentRecommendedDate: this.noteForm.nextAppointmentRecommendedDate
        ? dayjs(this.noteForm.nextAppointmentRecommendedDate).toISOString()
        : null,
    };

    this.doctorPatientService.saveConsultationNote(app.id, payload).subscribe({
      next: updatedApp => {
        this.isSavingNote.set(false);
        this.successMessage.set('Đã lưu ghi chú khám lâm sàng thành công!');
        this.closeNoteEditor();
        this.loadPatientDetail();
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.isSavingNote.set(false);
        this.errorMessage.set(err?.error?.message || 'Có lỗi khi lưu ghi chú khám.');
      },
    });
  }

  exportPdf(): void {
    const id = this.patientId();
    if (!id) return;

    this.isExportingPdf.set(true);
    this.errorMessage.set(null);

    this.doctorPatientService.exportPatientPdf(id).subscribe({
      next: (blob: Blob) => {
        this.isExportingPdf.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const patientName = this.patientRecord()?.patient?.fullName?.trim().replace(/\s+/g, '_') || `patient_${id}`;
        a.download = `Benh_An_${patientName}_${dayjs().format('YYYYMMDD')}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.successMessage.set('Đã xuất file PDF bệnh án thành công!');
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.isExportingPdf.set(false);
        this.errorMessage.set('Không thể xuất file PDF bệnh án hoặc bạn chưa có quyền tải.');
      },
    });
  }

  parseReason(raw?: string | null): { reason?: string; symptoms?: string; medicalHistory?: string; expectations?: string; isStructured?: boolean } {
    if (!raw) return {};

    const clean = raw.trim();
    const hasTags = /Lý do:|Triệu chứng:|Tiền sử:|Kỳ vọng:/i.test(clean);
    if (!hasTags) {
      return { reason: clean, isStructured: false };
    }

    const reasonMatch = clean.match(/Lý do:\s*([^]*?)(?=(?:Triệu chứng:|Tiền sử:|Kỳ vọng:|$))/i);
    const symptomsMatch = clean.match(/Triệu chứng:\s*([^]*?)(?=(?:Lý do:|Tiền sử:|Kỳ vọng:|$))/i);
    const historyMatch = clean.match(/Tiền sử:\s*([^]*?)(?=(?:Lý do:|Triệu chứng:|Kỳ vọng:|$))/i);
    const expectationsMatch = clean.match(/Kỳ vọng:\s*([^]*?)(?=(?:Lý do:|Triệu chứng:|Tiền sử:|$))/i);

    return {
      reason: reasonMatch ? reasonMatch[1].trim() : undefined,
      symptoms: symptomsMatch ? symptomsMatch[1].trim() : undefined,
      medicalHistory: historyMatch ? historyMatch[1].trim() : undefined,
      expectations: expectationsMatch ? expectationsMatch[1].trim() : undefined,
      isStructured: true,
    };
  }

  getAge(dob?: dayjs.Dayjs | null): number | string {
    if (!dob) return '--';
    return dayjs().diff(dayjs(dob), 'year');
  }

  getInitials(name?: string | null): string {
    if (!name) return 'BN';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 0) return 'BN';
    if (parts.length === 1) return parts[0].substring(0, Math.min(2, parts[0].length)).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
}
