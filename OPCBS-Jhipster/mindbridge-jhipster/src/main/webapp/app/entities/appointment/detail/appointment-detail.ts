import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import dayjs from 'dayjs/esm';
import { AccountService } from 'app/core/auth';
import { FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { IAppointment } from '../appointment.model';
import { AppointmentService } from '../service/appointment.service';
import { DoctorAppointmentService } from '../service/doctor-appointment.service';
import { PatientBookingService } from '../service/patient-booking.service';
import { DoctorPatientService, ISaveConsultationNoteRequest } from 'app/entities/doctor-profile/service/doctor-patient.service';

@Component({
  selector: 'jhi-appointment-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule, FormatMediumDatePipe, FormatMediumDatetimePipe],
  templateUrl: './appointment-detail.html',
  styleUrls: ['./appointment-detail.scss'],
})
export class AppointmentDetail implements OnInit {
  // Bound automatically by Angular Router withComponentInputBinding() via route resolve
  readonly appointmentInput = input<IAppointment | null>(null, { alias: 'appointment' });
  private readonly appointmentState = signal<IAppointment | null>(null);

  // Unified reactive signal consumed by HTML template: appointment()
  readonly appointment = computed(() => this.appointmentState() ?? this.appointmentInput());

  // Reactive computed pre-appointment evaluation parse
  readonly parsedReason = computed(() => this.parseReason(this.appointment()?.reason));

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly doctorAppointmentService = inject(DoctorAppointmentService);
  private readonly patientBookingService = inject(PatientBookingService);
  private readonly doctorPatientService = inject(DoctorPatientService);
  private readonly cdr = inject(ChangeDetectorRef);

  isLoading = false;
  successMessage = '';
  errorMessage = '';

  // Current user role flags
  isDoctor = false;
  isPatient = false;
  isAdmin = false;

  // Modal Consultation Note (for Doctor)
  isEditingNote = false;
  noteForm: ISaveConsultationNoteRequest = {
    diagnosis: '',
    consultationSummary: '',
    therapyPlan: '',
    recommendations: '',
    followUpNotes: '',
    nextAppointmentRecommendedDate: null,
    markCompleted: true,
  };
  isSavingNote = false;

  // Modal Cancel (for Patient)
  isCancelling = false;
  cancellationReason = '';
  isProcessingCancel = false;

  // Modal Reject (for Doctor)
  isRejecting = false;
  rejectionReason = '';
  isProcessingReject = false;

  constructor() {
    effect(() => {
      const inputVal = this.appointmentInput();
      if (inputVal && !this.appointmentState()) {
        this.appointmentState.set(inputVal);
      }
    });
  }

  ngOnInit(): void {
    this.checkRoles();

    const id = this.route.snapshot.params['id'];
    if (id && !this.appointment()) {
      this.loadAppointment(+id);
    }
  }

  checkRoles(): void {
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.isDoctor = account.authorities.includes('ROLE_DOCTOR');
        this.isPatient = account.authorities.includes('ROLE_PATIENT');
        this.isAdmin = account.authorities.includes('ROLE_ADMIN');
        this.cdr.markForCheck();
      }
    });
  }

  loadAppointment(id: number): void {
    this.isLoading = true;
    this.appointmentService.find(id).subscribe({
      next: data => {
        this.appointmentState.set(data);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Không thể tải thông tin lịch hẹn.';
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // Checks if scheduled start time is reached (allows starting up to 15 mins before scheduled time)
  canStartConsultation(app?: IAppointment | null): boolean {
    const target = app ?? this.appointment();
    if (!target || !target.scheduledStart) return false;
    const startTime = dayjs(target.scheduledStart);
    return dayjs().isAfter(startTime.subtract(15, 'minute'));
  }

  hasClinicalNotes(app?: IAppointment | null): boolean {
    const target = app ?? this.appointment();
    if (!target) return false;
    return Boolean(
      (target.diagnosis && target.diagnosis.trim().length > 0) ||
      (target.consultationSummary && target.consultationSummary.trim().length > 0) ||
      (target.therapyPlan && target.therapyPlan.trim().length > 0) ||
      (target.recommendations && target.recommendations.trim().length > 0) ||
      (target.followUpNotes && target.followUpNotes.trim().length > 0)
    );
  }

  // Doctor: Accept appointment
  acceptAppointment(): void {
    const app = this.appointment();
    if (!app) return;

    this.isLoading = true;
    this.doctorAppointmentService.acceptAppointment(app.id).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isLoading = false;
        this.showSuccess('Đã phê duyệt ca khám thành công!');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isLoading = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi phê duyệt ca khám.');
        this.cdr.markForCheck();
      },
    });
  }

  // Doctor: Start consultation (switches status to IN_PROGRESS)
  startAppointment(): void {
    const app = this.appointment();
    if (!app) return;

    if (!this.canStartConsultation(app)) {
      this.showError('Chưa đến thời gian bắt đầu ca khám (chỉ có thể bắt đầu trước tối đa 15 phút so với giờ hẹn).');
      return;
    }

    this.isLoading = true;
    this.doctorAppointmentService.startAppointment(app.id).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isLoading = false;
        this.showSuccess('Đã bắt đầu buổi tham vấn y tế! Trạng thái chuyển sang ĐANG KHÁM (In Progress).');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isLoading = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi bắt đầu ca khám.');
        this.cdr.markForCheck();
      },
    });
  }

  // Doctor: Complete appointment (requires clinical notes)
  completeAppointment(): void {
    const app = this.appointment();
    if (!app) return;

    if (!this.hasClinicalNotes(app)) {
      this.showError('Vui lòng ghi chép hồ sơ chẩn đoán / phác đồ lâm sàng trước khi đánh dấu hoàn thành ca khám.');
      this.openNoteEditor();
      return;
    }

    if (!confirm('Xác nhận hoàn thành ca khám này?')) return;

    this.isLoading = true;
    this.doctorAppointmentService.completeAppointment(app.id).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isLoading = false;
        this.showSuccess('Đã đánh dấu hoàn thành ca khám thành công!');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isLoading = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi hoàn thành ca khám.');
        this.cdr.markForCheck();
      },
    });
  }

  // Doctor: Open Reject Modal
  openRejectModal(): void {
    this.rejectionReason = '';
    this.isRejecting = true;
    this.cdr.markForCheck();
  }

  closeRejectModal(): void {
    this.isRejecting = false;
    this.rejectionReason = '';
    this.cdr.markForCheck();
  }

  confirmReject(): void {
    const app = this.appointment();
    if (!app) return;

    this.isProcessingReject = true;
    this.doctorAppointmentService.rejectAppointment(app.id, this.rejectionReason).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isProcessingReject = false;
        this.closeRejectModal();
        this.showSuccess('Đã từ chối ca khám và mở lại khung giờ cho bệnh nhân khác.');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isProcessingReject = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi từ chối ca khám.');
        this.cdr.markForCheck();
      },
    });
  }

  // Patient: Open Cancel Modal
  openCancelModal(): void {
    this.cancellationReason = '';
    this.isCancelling = true;
    this.cdr.markForCheck();
  }

  closeCancelModal(): void {
    this.isCancelling = false;
    this.cancellationReason = '';
    this.cdr.markForCheck();
  }

  confirmCancel(): void {
    const app = this.appointment();
    if (!app) return;

    this.isProcessingCancel = true;
    this.patientBookingService.cancelAppointment(app.id, this.cancellationReason).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isProcessingCancel = false;
        this.closeCancelModal();
        this.showSuccess('Đã hủy lịch hẹn thành công.');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isProcessingCancel = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi hủy lịch hẹn.');
        this.cdr.markForCheck();
      },
    });
  }

  // Doctor: Open Consultation Note Editor
  openNoteEditor(): void {
    const app = this.appointment();
    if (!app) return;

    this.noteForm = {
      diagnosis: app.diagnosis ?? '',
      consultationSummary: app.consultationSummary ?? '',
      therapyPlan: app.therapyPlan ?? '',
      recommendations: app.recommendations ?? '',
      followUpNotes: app.followUpNotes ?? '',
      nextAppointmentRecommendedDate: app.nextAppointmentRecommendedDate
        ? dayjs(app.nextAppointmentRecommendedDate).format('YYYY-MM-DDTHH:mm')
        : null,
      markCompleted: true,
    };
    this.isEditingNote = true;
    this.cdr.markForCheck();
  }

  closeNoteEditor(): void {
    this.isEditingNote = false;
    this.cdr.markForCheck();
  }

  saveConsultationNote(): void {
    const app = this.appointment();
    if (!app) return;

    this.isSavingNote = true;
    const payload: ISaveConsultationNoteRequest = {
      ...this.noteForm,
      nextAppointmentRecommendedDate: this.noteForm.nextAppointmentRecommendedDate
        ? dayjs(this.noteForm.nextAppointmentRecommendedDate).toISOString()
        : null,
    };

    this.doctorPatientService.saveConsultationNote(app.id, payload).subscribe({
      next: updated => {
        this.appointmentState.set(updated);
        this.isSavingNote = false;
        this.closeNoteEditor();
        this.showSuccess('Đã lưu hồ sơ lâm sàng và cập nhật ca khám thành công!');
        this.cdr.markForCheck();
      },
      error: err => {
        this.isSavingNote = false;
        this.showError(err?.error?.message || 'Có lỗi xảy ra khi lưu ghi chú khám.');
        this.cdr.markForCheck();
      },
    });
  }

  printSummary(): void {
    window.print();
  }

  previousState(): void {
    this.router.navigate(['/appointment']);
  }

  parseReason(raw?: string | null): { reason?: string; symptoms?: string; medicalHistory?: string; expectations?: string; isStructured?: boolean } {
    if (!raw) return {};

    const clean = raw.trim();
    // Check if contains structured tags
    const hasTags = /Lý do:|Triệu chứng:|Tiền sử:|Kỳ vọng:/i.test(clean);
    if (!hasTags) {
      return { reason: clean, isStructured: false };
    }

    const reasonMatch = clean.match(/Lý do:\s*([^]*?)(?=(?:\r?\n)?(?:Triệu chứng:|Tiền sử:|Kỳ vọng:|$))/i);
    const symptomsMatch = clean.match(/Triệu chứng:\s*([^]*?)(?=(?:\r?\n)?(?:Lý do:|Tiền sử:|Kỳ vọng:|$))/i);
    const historyMatch = clean.match(/Tiền sử:\s*([^]*?)(?=(?:\r?\n)?(?:Lý do:|Triệu chứng:|Kỳ vọng:|$))/i);
    const expectationsMatch = clean.match(/Kỳ vọng:\s*([^]*?)(?=(?:\r?\n)?(?:Lý do:|Triệu chứng:|Tiền sử:|$))/i);

    return {
      reason: reasonMatch ? reasonMatch[1].trim() : undefined,
      symptoms: symptomsMatch ? symptomsMatch[1].trim() : undefined,
      medicalHistory: historyMatch ? historyMatch[1].trim() : undefined,
      expectations: expectationsMatch ? expectationsMatch[1].trim() : undefined,
      isStructured: true,
    };
  }

  getAge(dob?: dayjs.Dayjs | null | string | Date): number | string {
    if (!dob) return '--';
    const d = dayjs.isDayjs(dob) ? dob : dayjs(dob);
    if (!d.isValid()) return '--';
    return dayjs().diff(d, 'year');
  }

  getPatientDisplayName(app?: IAppointment | null): string {
    const target = app ?? this.appointment();
    if (!target) return 'Bệnh nhân MindBridge';
    if (target.patient?.fullName && target.patient.fullName.trim().length > 0) {
      return target.patient.fullName.trim();
    }
    if (target.patient?.user?.login && target.patient.user.login.trim().length > 0) {
      return target.patient.user.login.trim();
    }
    return 'Bệnh nhân MindBridge';
  }

  getDoctorDisplayName(app?: IAppointment | null): string {
    const target = app ?? this.appointment();
    if (!target) return 'Bác sĩ chuyên khoa MindBridge';
    if (target.doctor?.fullName && target.doctor.fullName.trim().length > 0) {
      return target.doctor.fullName.trim();
    }
    if (target.doctor?.user?.login && target.doctor.user.login.trim().length > 0) {
      return target.doctor.user.login.trim();
    }
    return 'Bác sĩ chuyên khoa MindBridge';
  }

  getInitials(name?: string | null, fallback?: string | null): string {
    const raw = (name && name.trim().length > 0) ? name : (fallback && fallback.trim().length > 0) ? fallback : '';
    if (!raw) return 'BN';
    const parts = raw.trim().split(' ').filter(Boolean);
    if (parts.length === 0) return 'BN';
    if (parts.length === 1) return parts[0].substring(0, Math.min(2, parts[0].length)).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  getStatusText(status?: string | null): string {
    switch (status) {
      case 'PENDING':
        return 'Chờ Phê Duyệt (Pending)';
      case 'ACCEPTED':
        return 'Đã Xác Nhận (Accepted)';
      case 'IN_PROGRESS':
        return 'Đang Khám (In Progress)';
      case 'COMPLETED':
        return 'Đã Hoàn Thành (Completed)';
      case 'CANCELLED':
        return 'Đã Hủy (Cancelled)';
      case 'REJECTED':
        return 'Đã Từ Chối (Rejected)';
      default:
        return status || '—';
    }
  }

  getStatusIcon(status?: string | null): string {
    switch (status) {
      case 'PENDING':
        return 'hourglass-half';
      case 'ACCEPTED':
        return 'check-circle';
      case 'IN_PROGRESS':
        return 'play-circle';
      case 'COMPLETED':
        return 'check-circle';
      case 'CANCELLED':
        return 'times-circle';
      case 'REJECTED':
        return 'ban';
      default:
        return 'calendar-check';
    }
  }

  getBootstrapStatusIcon(status?: string | null): string {
    switch (status) {
      case 'PENDING':
        return 'bi-hourglass-split text-warning';
      case 'ACCEPTED':
        return 'bi-check2-circle text-primary';
      case 'IN_PROGRESS':
        return 'bi-play-circle-fill text-success';
      case 'COMPLETED':
        return 'bi-check-circle-fill text-success';
      case 'CANCELLED':
        return 'bi-x-circle-fill text-danger';
      case 'REJECTED':
        return 'bi-slash-circle-fill text-secondary';
      default:
        return 'bi-calendar-event';
    }
  }

  formatScheduledTime(app?: IAppointment | null): string {
    if (!app || !app.scheduledStart) return 'Chưa có giờ khám';
    const start = dayjs(app.scheduledStart);
    if (!app.scheduledEnd) return start.format('HH:mm');
    const end = dayjs(app.scheduledEnd);
    return `${start.format('HH:mm')} — ${end.format('HH:mm')} (${start.format('DD/MM/YYYY')})`;
  }

  getModeText(mode?: string | null): string {
    switch (mode) {
      case 'ONLINE':
        return 'Khám Trực Tuyến (Telehealth)';
      case 'IN_PERSON':
        return 'Khám Tại Phòng Khám (In-Person)';
      default:
        return mode || 'Tham vấn y tế';
    }
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    this.errorMessage = '';
    setTimeout(() => (this.successMessage = ''), 5000);
  }

  private showError(msg: string): void {
    this.errorMessage = msg;
    this.successMessage = '';
    setTimeout(() => (this.errorMessage = ''), 5000);
  }
}
