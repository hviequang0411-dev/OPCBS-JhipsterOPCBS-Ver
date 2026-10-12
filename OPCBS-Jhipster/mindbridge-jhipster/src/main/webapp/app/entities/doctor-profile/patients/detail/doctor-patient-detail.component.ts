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

  readonly activeTab = signal<'OVERVIEW' | 'CLINICAL'>('CLINICAL');

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

  setActiveTab(tab: 'OVERVIEW' | 'CLINICAL'): void {
    this.activeTab.set(tab);
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
