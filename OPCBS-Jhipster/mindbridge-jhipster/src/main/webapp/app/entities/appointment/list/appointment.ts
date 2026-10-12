import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed, ChangeDetectorRef } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth';
import { FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { IAppointment } from '../appointment.model';
import { PatientBookingService } from '../service/patient-booking.service';
import { DoctorAppointmentService, IAppointmentStats } from '../service/doctor-appointment.service';
import { DoctorPatientService, ISaveConsultationNoteRequest } from 'app/entities/doctor-profile/service/doctor-patient.service';

export interface IGroupedAppointments {
  dateKey: string;
  displayDate: string;
  appointments: IAppointment[];
}

@Component({
  selector: 'jhi-appointment',
  standalone: true,
  templateUrl: './appointment.html',
  styleUrls: ['./appointment.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    FontAwesomeModule,
    NgbModalModule,
    FormatMediumDatePipe,
    FormatMediumDatetimePipe,
  ],
})
export class Appointment implements OnInit {
  protected readonly accountService = inject(AccountService);
  protected readonly patientBookingService = inject(PatientBookingService);
  protected readonly doctorAppointmentService = inject(DoctorAppointmentService);
  protected readonly doctorPatientService = inject(DoctorPatientService);
  protected readonly router = inject(Router);
  protected readonly modalService = inject(NgbModal);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  // States
  readonly isLoading = signal<boolean>(false);
  readonly appointments = signal<IAppointment[]>([]);
  readonly currentStatusFilter = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly dateFrom = signal<string>('');
  readonly dateTo = signal<string>('');
  readonly isDoctor = signal<boolean>(false);
  readonly isPatient = signal<boolean>(false);
  readonly isAdmin = signal<boolean>(false);
  readonly stats = signal<IAppointmentStats | null>(null);

  // Pagination States
  readonly page = signal<number>(1);
  readonly pageSize = signal<number>(10);

  // Messages
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  // Forms for Action Modals
  selectedAppointmentId = signal<number | null>(null);
  rejectForm: FormGroup = this.fb.group({
    rejectionReason: [''],
  });
  cancelForm: FormGroup = this.fb.group({
    cancellationReason: [''],
  });

  // Fast consultation note modal
  selectedAppointmentForNote = signal<IAppointment | null>(null);
  noteForm: ISaveConsultationNoteRequest = {
    diagnosis: '',
    consultationSummary: '',
    therapyPlan: '',
    recommendations: '',
    followUpNotes: '',
    nextAppointmentRecommendedDate: null,
    markCompleted: true,
  };
  isSavingNote = signal<boolean>(false);

  // Computed Properties: Filtered List
  readonly filteredAppointments = computed<IAppointment[]>(() => {
    let list = this.appointments();
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.currentStatusFilter();
    const from = this.dateFrom();
    const to = this.dateTo();

    // 1. Status Filter
    if (status !== 'ALL') {
      if (status === 'CANCELLED_ALL') {
        list = list.filter(a => a.status === 'CANCELLED' || a.status === 'REJECTED');
      } else {
        list = list.filter(a => a.status === status);
      }
    }

    // 2. Search Query
    if (query) {
      list = list.filter(
        a =>
          (a.bookingCode && a.bookingCode.toLowerCase().includes(query)) ||
          (a.patient?.fullName && a.patient.fullName.toLowerCase().includes(query)) ||
          (a.patient?.phone && a.patient.phone.toLowerCase().includes(query)) ||
          (a.doctor?.fullName && a.doctor.fullName.toLowerCase().includes(query)) ||
          (a.reason && a.reason.toLowerCase().includes(query)) ||
          (a.diagnosis && a.diagnosis.toLowerCase().includes(query))
      );
    }

    // 3. Date Range
    if (from) {
      const fromDay = dayjs(from).startOf('day');
      list = list.filter(a => a.scheduledStart && dayjs(a.scheduledStart).isAfter(fromDay.subtract(1, 'second')));
    }
    if (to) {
      const toDay = dayjs(to).endOf('day');
      list = list.filter(a => a.scheduledStart && dayjs(a.scheduledStart).isBefore(toDay.add(1, 'second')));
    }

    return list;
  });

  readonly totalItems = computed<number>(() => this.filteredAppointments().length);
  readonly totalPages = computed<number>(() => Math.max(1, Math.ceil(this.totalItems() / this.pageSize())));

  // Computed Properties: Paginated List
  readonly paginatedAppointments = computed<IAppointment[]>(() => {
    const list = this.filteredAppointments();
    const start = (this.page() - 1) * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  // Grouped by Date (C# Razor Pattern from paginated items)
  readonly groupedAppointments = computed<IGroupedAppointments[]>(() => {
    const list = [...this.paginatedAppointments()];
    
    // Sort chronologically
    list.sort((a, b) => {
      if (!a.scheduledStart) return 1;
      if (!b.scheduledEnd) return -1;
      return dayjs(a.scheduledStart).valueOf() - dayjs(b.scheduledStart).valueOf();
    });

    const groupMap = new Map<string, IAppointment[]>();
    for (const appt of list) {
      const dateKey = appt.scheduledStart ? dayjs(appt.scheduledStart).format('YYYY-MM-DD') : 'UNKNOWN';
      if (!groupMap.has(dateKey)) {
        groupMap.set(dateKey, []);
      }
      groupMap.get(dateKey)!.push(appt);
    }

    const result: IGroupedAppointments[] = [];
    groupMap.forEach((appts, dateKey) => {
      let displayDate = 'Chưa xác định ngày';
      if (dateKey !== 'UNKNOWN') {
        const d = dayjs(dateKey);
        const dayOfWeekMap: Record<number, string> = {
          0: 'Chủ Nhật',
          1: 'Thứ Hai',
          2: 'Thứ Ba',
          3: 'Thứ Tư',
          4: 'Thứ Năm',
          5: 'Tháu Sáu',
          6: 'Thứ Bảy',
        };
        const dayOfWeek = dayOfWeekMap[d.day()] || '';
        displayDate = `${dayOfWeek}, ngày ${d.format('DD/MM/YYYY')}`;
      }
      result.push({
        dateKey,
        displayDate,
        appointments: appts,
      });
    });

    return result;
  });

  // Counts for tabs
  readonly totalCount = computed(() => this.appointments().length);
  readonly pendingCount = computed(() => this.appointments().filter(a => a.status === 'PENDING').length);
  readonly acceptedCount = computed(() => this.appointments().filter(a => a.status === 'ACCEPTED').length);
  readonly inProgressCount = computed(() => this.appointments().filter(a => a.status === 'IN_PROGRESS').length);
  readonly completedCount = computed(() => this.appointments().filter(a => a.status === 'COMPLETED').length);
  readonly cancelledCount = computed(() => this.appointments().filter(a => a.status === 'CANCELLED' || a.status === 'REJECTED').length);

  canStartAppointment(appt?: IAppointment | null): boolean {
    if (!appt || !appt.scheduledStart) return false;
    const startTime = dayjs(appt.scheduledStart);
    return dayjs().isAfter(startTime.subtract(15, 'minute'));
  }

  hasClinicalNotes(appt?: IAppointment | null): boolean {
    if (!appt) return false;
    return Boolean(
      (appt.diagnosis && appt.diagnosis.trim().length > 0) ||
      (appt.consultationSummary && appt.consultationSummary.trim().length > 0) ||
      (appt.therapyPlan && appt.therapyPlan.trim().length > 0) ||
      (appt.recommendations && appt.recommendations.trim().length > 0) ||
      (appt.followUpNotes && appt.followUpNotes.trim().length > 0)
    );
  }

  ngOnInit(): void {
    this.accountService.identity().subscribe({
      next: acc => {
        if (acc) {
          this.isDoctor.set(this.accountService.hasAnyAuthority('ROLE_DOCTOR'));
          this.isPatient.set(this.accountService.hasAnyAuthority('ROLE_PATIENT'));
          this.isAdmin.set(this.accountService.hasAnyAuthority('ROLE_ADMIN'));
          this.loadAppointments();
          if (this.isDoctor() || this.isAdmin()) {
            this.loadDoctorStats();
          }
        } else {
          this.isLoading.set(false);
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  loadAppointments(): void {
    this.isLoading.set(true);
    this.cdr.markForCheck();

    if (this.isDoctor() || this.isAdmin()) {
      this.doctorAppointmentService.getAppointments('ALL').subscribe({
        next: data => {
          this.appointments.set(data ?? []);
          this.isLoading.set(false);
          this.cdr.markForCheck();
        },
        error: err => {
          this.isLoading.set(false);
          this.cdr.markForCheck();
          this.showError('Không thể tải danh sách lịch hẹn bác sĩ: ' + (err.error?.message || err.message));
        },
      });
    } else {
      this.patientBookingService.getMyAppointments().subscribe({
        next: data => {
          this.appointments.set(data ?? []);
          this.isLoading.set(false);
          this.cdr.markForCheck();
        },
        error: err => {
          this.isLoading.set(false);
          this.cdr.markForCheck();
          this.showError('Không thể tải danh sách lịch hẹn: ' + (err.error?.message || err.message));
        },
      });
    }
  }

  loadDoctorStats(): void {
    this.doctorAppointmentService.getStats().subscribe({
      next: res => {
        this.stats.set(res);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  setStatusFilter(status: string): void {
    this.currentStatusFilter.set(status);
    this.page.set(1);
    this.cdr.markForCheck();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.currentStatusFilter.set('ALL');
    this.page.set(1);
    this.cdr.markForCheck();
  }

  goToPage(p: number): void {
    if (p >= 1 && p <= this.totalPages()) {
      this.page.set(p);
      this.cdr.markForCheck();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.update(p => p + 1);
      this.cdr.markForCheck();
    }
  }

  prevPage(): void {
    if (this.page() > 1) {
      this.page.update(p => p - 1);
      this.cdr.markForCheck();
    }
  }

  changePageSize(event: Event): void {
    const target = event.target as HTMLSelectElement;
    if (target) {
      this.pageSize.set(Number(target.value) || 10);
      this.page.set(1);
      this.cdr.markForCheck();
    }
  }

  get pageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.page();
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, current - 2);
    let end = Math.min(total, start + maxVisible - 1);
    if (end - start < maxVisible - 1) {
      start = Math.max(1, end - maxVisible + 1);
    }
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  // Doctor Actions
  acceptAppointment(id: number): void {
    this.isLoading.set(true);
    this.cdr.markForCheck();
    this.doctorAppointmentService.acceptAppointment(id).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isLoading.set(false);
        this.showSuccess(`Đã phê duyệt ca khám #${updated.bookingCode} thành công!`);
        this.cdr.markForCheck();
        if (this.isDoctor()) this.loadDoctorStats();
      },
      error: err => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
        this.showError('Không thể duyệt lịch: ' + (err.error?.message || err.message));
      },
    });
  }

  startAppointment(id: number): void {
    const appt = this.appointments().find(a => a.id === id);
    if (appt && !this.canStartAppointment(appt)) {
      this.showError('Chưa đến thời gian bắt đầu ca khám (chỉ có thể bắt đầu trước tối đa 15 phút so với giờ hẹn).');
      return;
    }

    this.isLoading.set(true);
    this.cdr.markForCheck();
    this.doctorAppointmentService.startAppointment(id).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isLoading.set(false);
        this.showSuccess(`Đã bắt đầu ca khám #${updated.bookingCode} (Đang khám)!`);
        this.cdr.markForCheck();
        if (this.isDoctor()) this.loadDoctorStats();
      },
      error: err => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
        this.showError('Không thể bắt đầu ca khám: ' + (err.error?.message || err.message));
      },
    });
  }

  openRejectModal(content: any, id: number): void {
    this.selectedAppointmentId.set(id);
    this.rejectForm.reset({ rejectionReason: '' });
    this.modalService.open(content, { centered: true });
  }

  submitReject(modal: any): void {
    const id = this.selectedAppointmentId();
    if (!id) return;

    const reason = this.rejectForm.value.rejectionReason;
    this.isLoading.set(true);
    this.cdr.markForCheck();
    this.doctorAppointmentService.rejectAppointment(id, reason).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isLoading.set(false);
        modal.close();
        this.showSuccess(`Đã từ chối ca khám #${updated.bookingCode}. Khung giờ đã được mở lại!`);
        this.cdr.markForCheck();
        if (this.isDoctor()) this.loadDoctorStats();
      },
      error: err => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
        this.showError('Lỗi khi từ chối lịch: ' + (err.error?.message || err.message));
      },
    });
  }

  completeAppointment(id: number, content?: any): void {
    const appt = this.appointments().find(a => a.id === id);
    if (appt && !this.hasClinicalNotes(appt)) {
      this.showError('Vui lòng ghi chép hồ sơ / chẩn đoán / ghi chú lâm sàng trước khi hoàn thành ca khám.');
      if (content) {
        this.openNoteModal(content, appt);
      }
      return;
    }

    if (!confirm('Xác nhận ca khám này đã được hoàn thành?')) return;

    this.isLoading.set(true);
    this.cdr.markForCheck();
    this.doctorAppointmentService.completeAppointment(id).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isLoading.set(false);
        this.showSuccess(`Đã đánh dấu hoàn thành ca khám #${updated.bookingCode}!`);
        this.cdr.markForCheck();
        if (this.isDoctor()) this.loadDoctorStats();
      },
      error: err => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
        this.showError('Không thể hoàn thành ca khám: ' + (err.error?.message || err.message));
      },
    });
  }

  // Fast consultation note modal
  openNoteModal(content: any, appt: IAppointment): void {
    this.selectedAppointmentForNote.set(appt);
    this.noteForm = {
      diagnosis: appt.diagnosis ?? '',
      consultationSummary: appt.consultationSummary ?? '',
      therapyPlan: appt.therapyPlan ?? '',
      recommendations: appt.recommendations ?? '',
      followUpNotes: appt.followUpNotes ?? '',
      nextAppointmentRecommendedDate: appt.nextAppointmentRecommendedDate
        ? dayjs(appt.nextAppointmentRecommendedDate).format('YYYY-MM-DDTHH:mm')
        : null,
      markCompleted: true,
    };
    this.modalService.open(content, { centered: true, size: 'lg' });
  }

  submitNote(modal: any): void {
    const appt = this.selectedAppointmentForNote();
    if (!appt) return;

    this.isSavingNote.set(true);
    this.cdr.markForCheck();
    const payload: ISaveConsultationNoteRequest = {
      ...this.noteForm,
      nextAppointmentRecommendedDate: this.noteForm.nextAppointmentRecommendedDate
        ? dayjs(this.noteForm.nextAppointmentRecommendedDate).toISOString()
        : null,
    };

    this.doctorPatientService.saveConsultationNote(appt.id, payload).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isSavingNote.set(false);
        modal.close();
        this.showSuccess(`Đã lưu ghi chú lâm sàng cho ca khám #${updated.bookingCode} thành công!`);
        this.cdr.markForCheck();
        if (this.isDoctor()) this.loadDoctorStats();
      },
      error: err => {
        this.isSavingNote.set(false);
        this.cdr.markForCheck();
        this.showError('Lỗi khi lưu ghi chú lâm sàng: ' + (err?.error?.message || err.message));
      },
    });
  }

  // Patient Actions
  openCancelModal(content: any, id: number): void {
    this.selectedAppointmentId.set(id);
    this.cancelForm.reset({ cancellationReason: '' });
    this.modalService.open(content, { centered: true });
  }

  submitCancel(modal: any): void {
    const id = this.selectedAppointmentId();
    if (!id) return;

    const reason = this.cancelForm.value.cancellationReason;
    this.isLoading.set(true);
    this.cdr.markForCheck();
    this.patientBookingService.cancelAppointment(id, reason).subscribe({
      next: updated => {
        this.appointments.update(prev => prev.map(a => (a.id === updated.id ? updated : a)));
        this.isLoading.set(false);
        modal.close();
        this.showSuccess(`Đã hủy lịch hẹn #${updated.bookingCode}!`);
        this.cdr.markForCheck();
      },
      error: err => {
        this.isLoading.set(false);
        this.cdr.markForCheck();
        this.showError('Không thể hủy lịch: ' + (err.error?.message || err.message));
      },
    });
  }

  // Helpers
  getInitials(name?: string | null): string {
    if (!name) return 'BN';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 0) return 'BN';
    if (parts.length === 1) return parts[0].substring(0, Math.min(2, parts[0].length)).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  getDayNumber(start?: dayjs.Dayjs | null): string {
    if (!start) return '--';
    return dayjs(start).format('DD');
  }

  getMonthString(start?: dayjs.Dayjs | null): string {
    if (!start) return '';
    return 'THÁNG ' + dayjs(start).format('MM');
  }

  formatTime(appt: IAppointment): string {
    if (!appt.scheduledStart || !appt.scheduledEnd) return '--:--';
    return `${dayjs(appt.scheduledStart).format('HH:mm')} — ${dayjs(appt.scheduledEnd).format('HH:mm')}`;
  }

  getStatusBadgeClass(status: string | null | undefined): string {
    switch (status) {
      case 'PENDING':
        return 'pending';
      case 'ACCEPTED':
        return 'approved';
      case 'IN_PROGRESS':
        return 'inprogress';
      case 'COMPLETED':
        return 'completed';
      case 'CANCELLED':
        return 'cancelled';
      case 'REJECTED':
        return 'rejected';
      default:
        return 'pending';
    }
  }

  getStatusIcon(status: string | null | undefined): string {
    switch (status) {
      case 'PENDING':
        return 'bi-hourglass-split';
      case 'ACCEPTED':
        return 'bi-check-circle-fill';
      case 'IN_PROGRESS':
        return 'bi-play-circle-fill';
      case 'COMPLETED':
        return 'bi-patch-check-fill';
      case 'CANCELLED':
        return 'bi-x-circle-fill';
      case 'REJECTED':
        return 'bi-slash-circle';
      default:
        return 'bi-calendar-event';
    }
  }

  getStatusText(status: string | null | undefined): string {
    switch (status) {
      case 'PENDING':
        return 'Chờ Phê Duyệt';
      case 'ACCEPTED':
        return 'Đã Xác Nhận';
      case 'IN_PROGRESS':
        return 'Đang Tiến Hành';
      case 'COMPLETED':
        return 'Đã Hoàn Thành';
      case 'CANCELLED':
        return 'Đã Hủy';
      case 'REJECTED':
        return 'Đã Từ Chối';
      default:
        return status || '—';
    }
  }

  showSuccess(msg: string): void {
    this.successMessage.set(msg);
    this.errorMessage.set(null);
    setTimeout(() => this.successMessage.set(null), 4000);
  }

  showError(msg: string): void {
    this.errorMessage.set(msg);
    this.successMessage.set(null);
    setTimeout(() => this.errorMessage.set(null), 5000);
  }
}
