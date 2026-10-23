import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import dayjs from 'dayjs/esm';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { AccountService } from 'app/core/auth';
import { IAvailabilitySlot } from '../availability-slot.model';
import { DoctorScheduleService, ICreateSlotRequest, IBatchCreateSlotRequest } from '../service/doctor-schedule.service';

function getStartOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
  const day = d.day();
  const diff = day === 0 ? -6 : 1 - day;
  return d.add(diff, 'day').startOf('day');
}

function getEndOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
  return getStartOfIsoWeek(d).add(6, 'day').endOf('day');
}

export interface IDayColumn {
  date: dayjs.Dayjs;
  dateStr: string; // "YYYY-MM-DD"
  dayName: string; // "Thứ 2", "Thứ 3", etc.
  dayNumber: string; // "29/09"
  isToday: boolean;
  isPast: boolean;
  slots: IAvailabilitySlot[];
}

@Component({
  selector: 'jhi-availability-slot',
  standalone: true,
  templateUrl: './availability-slot.html',
  styleUrls: ['./availability-slot.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    FontAwesomeModule,
    NgbModalModule,
    AlertError,
    Alert,
    FormatMediumDatetimePipe,
  ],
})
export class AvailabilitySlot implements OnInit {
  protected readonly accountService = inject(AccountService);
  protected readonly doctorScheduleService = inject(DoctorScheduleService);
  protected readonly modalService = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  // States
  readonly isLoading = signal<boolean>(false);
  readonly slots = signal<IAvailabilitySlot[]>([]);
  readonly viewMode = signal<'week' | 'list'>('week');
  readonly currentWeekStart = signal<dayjs.Dayjs>(getStartOfIsoWeek(dayjs()));
  readonly filterMode = signal<string>('ALL'); // ALL, ONLINE, OFFLINE, BOTH
  readonly filterStatus = signal<string>('ALL'); // ALL, AVAILABLE, BOOKED, CANCELLED

  // Messages
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly quota = signal<any>(null);

  // Forms
  singleSlotForm: FormGroup = this.fb.group({
    date: [dayjs().format('YYYY-MM-DD'), [Validators.required]],
    startTime: ['08:00', [Validators.required]],
    endTime: ['09:00', [Validators.required]],
    consultationMode: ['BOTH', [Validators.required]],
    notes: [''],
  });

  batchSlotForm: FormGroup = this.fb.group({
    startDate: [dayjs().format('YYYY-MM-DD'), [Validators.required]],
    endDate: [dayjs().add(13, 'day').format('YYYY-MM-DD'), [Validators.required]],
    dailyStartTime: ['08:00', [Validators.required]],
    dailyEndTime: ['17:00', [Validators.required]],
    slotDurationMinutes: [60, [Validators.required]],
    consultationMode: ['BOTH', [Validators.required]],
    notes: ['Khung giờ tư vấn MindBridge'],
  });

  selectedDaysOfWeek = signal<number[]>([1, 2, 3, 4, 5]); // Mon-Fri default

  editSlotForm: FormGroup = this.fb.group({
    id: [null, [Validators.required]],
    date: ['', [Validators.required]],
    startTime: ['', [Validators.required]],
    endTime: ['', [Validators.required]],
    consultationMode: ['BOTH', [Validators.required]],
    notes: [''],
  });

  // Computed Properties
  readonly todayDateStr = computed(() => dayjs().format('YYYY-MM-DD'));
  readonly weekEnd = computed(() => getEndOfIsoWeek(this.currentWeekStart()));
  readonly weekRangeDisplay = computed(() => {
    const start = this.currentWeekStart();
    const end = this.weekEnd();
    return `${start.format('DD/MM/YYYY')} - ${end.format('DD/MM/YYYY')}`;
  });

  // Stats computed
  readonly totalSlotsCount = computed(() => this.slots().length);
  readonly availableSlotsCount = computed(() => this.slots().filter(s => s.status === 'AVAILABLE').length);
  readonly bookedSlotsCount = computed(() => this.slots().filter(s => s.status === 'BOOKED').length);
  readonly unavailableSlotsCount = computed(() => this.slots().filter(s => s.status === 'CANCELLED').length);

  // Days grid computed
  readonly weekDays = computed<IDayColumn[]>(() => {
    const start = this.currentWeekStart();
    const result: IDayColumn[] = [];
    const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
    const todayStr = dayjs().format('YYYY-MM-DD');

    const allSlots = this.filteredSlots();

    for (let i = 0; i < 7; i++) {
      const d = start.add(i, 'day');
      const dStr = d.format('YYYY-MM-DD');
      const daySlots = allSlots
        .filter(s => s.startTime && dayjs(s.startTime).format('YYYY-MM-DD') === dStr)
        .sort((a, b) => {
          const tA = a.startTime ? dayjs(a.startTime).valueOf() : 0;
          const tB = b.startTime ? dayjs(b.startTime).valueOf() : 0;
          return tA - tB;
        });

      result.push({
        date: d,
        dateStr: dStr,
        dayName: dayNames[i],
        dayNumber: d.format('DD/MM'),
        isToday: dStr === todayStr,
        isPast: d.isBefore(dayjs().startOf('day')),
        slots: daySlots,
      });
    }

    return result;
  });

  readonly filteredSlots = computed<IAvailabilitySlot[]>(() => {
    let list = this.slots();
    const mode = this.filterMode();
    const status = this.filterStatus();

    if (mode !== 'ALL') {
      list = list.filter(s => s.consultationMode === mode);
    }
    if (status !== 'ALL') {
      list = list.filter(s => s.status === status);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadSlots();
    this.loadQuota();
  }

  loadQuota(): void {
    this.doctorScheduleService.getMySubscriptionQuota().subscribe({
      next: q => this.quota.set(q),
      error: () => this.quota.set(null),
    });
  }

  loadSlots(): void {
    this.isLoading.set(true);
    // Load from 1 month before current week start to 2 months after for smooth navigation
    const start = this.currentWeekStart().subtract(1, 'month').startOf('day');
    const end = this.currentWeekStart().add(2, 'month').endOf('day');

    this.doctorScheduleService.getMySlots(start, end).subscribe({
      next: data => {
        this.slots.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        this.errorMessage.set('Không thể tải danh sách khung giờ: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      },
    });
  }

  // Week Navigation
  prevWeek(): void {
    this.currentWeekStart.set(this.currentWeekStart().subtract(1, 'week'));
  }

  nextWeek(): void {
    this.currentWeekStart.set(this.currentWeekStart().add(1, 'week'));
  }

  goToToday(): void {
    this.currentWeekStart.set(getStartOfIsoWeek(dayjs()));
  }

  onDatePicked(event: any): void {
    const val = event.target.value;
    if (val) {
      this.currentWeekStart.set(getStartOfIsoWeek(dayjs(val)));
    }
  }

  // Filter setters
  setFilterMode(mode: string): void {
    this.filterMode.set(mode);
  }

  setFilterStatus(status: string): void {
    this.filterStatus.set(status);
  }

  setViewMode(mode: 'week' | 'list'): void {
    this.viewMode.set(mode);
  }

  // Modals Open
  openAddSingleModal(content: any, defaultDate?: string): void {
    const initialDate = defaultDate && !dayjs(defaultDate).isBefore(dayjs().startOf('day'))
      ? defaultDate
      : dayjs().format('YYYY-MM-DD');

    this.singleSlotForm.reset({
      date: initialDate,
      startTime: '08:00',
      endTime: '09:00',
      consultationMode: 'BOTH',
      notes: '',
    });
    this.modalService.open(content, { size: 'md', centered: true, backdrop: 'static' });
  }

  openBatchModal(content: any): void {
    this.batchSlotForm.reset({
      startDate: dayjs().format('YYYY-MM-DD'),
      endDate: dayjs().add(13, 'day').format('YYYY-MM-DD'),
      dailyStartTime: '08:00',
      dailyEndTime: '17:00',
      slotDurationMinutes: 60,
      consultationMode: 'BOTH',
      notes: 'Khung giờ tư vấn MindBridge',
    });
    this.selectedDaysOfWeek.set([1, 2, 3, 4, 5]);
    this.modalService.open(content, { size: 'lg', centered: true, backdrop: 'static' });
  }

  openEditModal(content: any, slot: IAvailabilitySlot): void {
    if (this.isSlotInPast(slot)) {
      this.showError('Khung giờ này đã qua trong quá khứ, không thể chỉnh sửa!');
      return;
    }
    if (slot.status === 'BOOKED') {
      this.showError('Khung giờ này đã có bệnh nhân đặt hẹn, không thể chỉnh sửa thời gian!');
      return;
    }
    const start = dayjs(slot.startTime);
    const end = dayjs(slot.endTime);

    this.editSlotForm.reset({
      id: slot.id,
      date: start.format('YYYY-MM-DD'),
      startTime: start.format('HH:mm'),
      endTime: end.format('HH:mm'),
      consultationMode: slot.consultationMode || 'BOTH',
      notes: slot.notes || '',
    });
    this.modalService.open(content, { size: 'md', centered: true, backdrop: 'static' });
  }

  // Toggle Day of Week checkbox for batch
  toggleDayOfWeek(day: number): void {
    const current = [...this.selectedDaysOfWeek()];
    const index = current.indexOf(day);
    if (index > -1) {
      if (current.length > 1) {
        current.splice(index, 1);
      }
    } else {
      current.push(day);
    }
    this.selectedDaysOfWeek.set(current.sort());
  }

  isDaySelected(day: number): boolean {
    return this.selectedDaysOfWeek().includes(day);
  }

  // Submit Single Slot
  submitSingleSlot(modal: any): void {
    if (this.singleSlotForm.invalid) {
      return;
    }
    const val = this.singleSlotForm.value;
    const startIso = dayjs(`${val.date}T${val.startTime}:00`).toISOString();
    const endIso = dayjs(`${val.date}T${val.endTime}:00`).toISOString();

    if (dayjs(startIso).isBefore(dayjs())) {
      this.showError('Không thể tạo khung giờ làm việc trong quá khứ!');
      return;
    }

    if (dayjs(startIso).isAfter(dayjs(endIso)) || dayjs(startIso).isSame(dayjs(endIso))) {
      this.showError('Giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    const req: ICreateSlotRequest = {
      startTime: startIso,
      endTime: endIso,
      consultationMode: val.consultationMode,
      notes: val.notes,
    };

    this.isLoading.set(true);
    this.doctorScheduleService.createSlot(req).subscribe({
      next: created => {
        this.slots.update(prev => [...prev, created]);
        this.isLoading.set(false);
        modal.close();
        this.showSuccess('Đã thêm khung giờ thành công!');
      },
      error: err => {
        this.isLoading.set(false);
        this.showError(err.error?.message || err.error?.title || 'Không thể tạo khung giờ, vui lòng kiểm tra trùng giờ!');
      },
    });
  }

  // Submit Batch Slots
  submitBatchSlots(modal: any): void {
    if (this.batchSlotForm.invalid) {
      return;
    }
    const val = this.batchSlotForm.value;

    if (dayjs(val.endDate).isBefore(dayjs().startOf('day'))) {
      this.showError('Ngày kết thúc của cấu hình tự động không được nằm trong quá khứ!');
      return;
    }

    const req: IBatchCreateSlotRequest = {
      startDate: val.startDate,
      endDate: val.endDate,
      dailyStartTime: val.dailyStartTime,
      dailyEndTime: val.dailyEndTime,
      slotDurationMinutes: Number(val.slotDurationMinutes),
      daysOfWeek: this.selectedDaysOfWeek(),
      consultationMode: val.consultationMode,
      notes: val.notes,
    };

    this.isLoading.set(true);
    this.doctorScheduleService.batchCreateSlots(req).subscribe({
      next: createdList => {
        this.isLoading.set(false);
        modal.close();
        this.loadSlots();
        this.showSuccess(`Đã tạo thành công ${createdList.length} khung giờ rảnh mới!`);
      },
      error: err => {
        this.isLoading.set(false);
        this.showError(err.error?.message || err.error?.title || 'Lỗi khi tạo hàng loạt khung giờ.');
      },
    });
  }

  // Submit Edit Slot
  submitEditSlot(modal: any): void {
    if (this.editSlotForm.invalid) {
      return;
    }
    const val = this.editSlotForm.value;
    const startIso = dayjs(`${val.date}T${val.startTime}:00`).toISOString();
    const endIso = dayjs(`${val.date}T${val.endTime}:00`).toISOString();

    if (dayjs(startIso).isBefore(dayjs())) {
      this.showError('Không thể chỉnh sửa khung giờ thành thời gian trong quá khứ!');
      return;
    }

    if (dayjs(startIso).isAfter(dayjs(endIso)) || dayjs(startIso).isSame(dayjs(endIso))) {
      this.showError('Giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    const req: ICreateSlotRequest = {
      startTime: startIso,
      endTime: endIso,
      consultationMode: val.consultationMode,
      notes: val.notes,
    };

    this.isLoading.set(true);
    this.doctorScheduleService.updateSlot(val.id, req).subscribe({
      next: updated => {
        this.slots.update(prev => prev.map(s => (s.id === updated.id ? updated : s)));
        this.isLoading.set(false);
        modal.close();
        this.showSuccess('Cập nhật khung giờ thành công!');
      },
      error: err => {
        this.isLoading.set(false);
        this.showError(err.error?.message || err.error?.title || 'Không thể cập nhật, vui lòng kiểm tra trùng giờ!');
      },
    });
  }

  // Toggle status
  toggleSlotStatus(slot: IAvailabilitySlot): void {
    if (this.isSlotInPast(slot)) {
      this.showError('Không thể thay đổi trạng thái khung giờ đã qua trong quá khứ!');
      return;
    }
    if (slot.status === 'BOOKED') {
      this.showError('Không thể tắt khung giờ đã có bệnh nhân đặt hẹn!');
      return;
    }
    this.doctorScheduleService.toggleSlotStatus(slot.id).subscribe({
      next: updated => {
        this.slots.update(prev => prev.map(s => (s.id === updated.id ? updated : s)));
        this.showSuccess(`Đã chuyển trạng thái sang ${updated.status === 'AVAILABLE' ? 'Sẵn sàng' : 'Tạm đóng'}!`);
      },
      error: err => {
        this.showError('Không thể đổi trạng thái: ' + (err.error?.message || err.message));
      },
    });
  }

  // Delete Slot
  deleteSlot(slot: IAvailabilitySlot): void {
    if (slot.status === 'BOOKED') {
      this.showError('Không thể xóa khung giờ đã có bệnh nhân đặt!');
      return;
    }
    if (!confirm(`Bạn có chắc chắn muốn xóa khung giờ ${this.formatSlotTime(slot)}?`)) {
      return;
    }
    this.doctorScheduleService.deleteSlot(slot.id).subscribe({
      next: () => {
        this.slots.update(prev => prev.filter(s => s.id !== slot.id));
        this.showSuccess('Đã xóa khung giờ!');
      },
      error: err => {
        this.showError('Không thể xóa khung giờ: ' + (err.error?.message || err.message));
      },
    });
  }

  // Helpers
  formatSlotTime(slot: IAvailabilitySlot): string {
    if (!slot.startTime || !slot.endTime) return '';
    return `${dayjs(slot.startTime).format('HH:mm')} - ${dayjs(slot.endTime).format('HH:mm')}`;
  }

  formatSlotDate(slot: IAvailabilitySlot): string {
    if (!slot.startTime) return '';
    return dayjs(slot.startTime).format('DD/MM/YYYY');
  }

  getModeBadgeClass(mode: string | null | undefined): string {
    switch (mode) {
      case 'ONLINE':
        return 'text-primary bg-primary-subtle';
      case 'OFFLINE':
        return 'text-success bg-success-subtle';
      default:
        return 'text-info bg-info-subtle';
    }
  }

  getModeIcon(mode: string | null | undefined): string {
    switch (mode) {
      case 'ONLINE':
        return 'bi-camera-video';
      case 'OFFLINE':
        return 'bi-hospital';
      default:
        return 'bi-arrow-repeat';
    }
  }

  isSlotInPast(slot: IAvailabilitySlot): boolean {
    return !slot.startTime || dayjs(slot.startTime).isBefore(dayjs());
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
