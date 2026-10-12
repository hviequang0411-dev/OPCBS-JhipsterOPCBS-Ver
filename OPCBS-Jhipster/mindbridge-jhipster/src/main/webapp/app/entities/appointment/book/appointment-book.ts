import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import dayjs from 'dayjs/esm';
import { AccountService } from 'app/core/auth';
import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { IAvailabilitySlot } from 'app/entities/availability-slot/availability-slot.model';
import { AvailabilitySlotService } from 'app/entities/availability-slot/service/availability-slot.service';
import { PatientBookingService, ICreateBookingRequest } from '../service/patient-booking.service';

function getStartOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
  const day = d.day();
  const diff = day === 0 ? -6 : 1 - day;
  return d.add(diff, 'day').startOf('day');
}

function getEndOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
  return getStartOfIsoWeek(d).add(6, 'day').endOf('day');
}

export interface IBookingDayColumn {
  date: dayjs.Dayjs;
  dateStr: string;
  dayName: string;
  dayNumber: string;
  isToday: boolean;
  slots: IAvailabilitySlot[];
}

@Component({
  selector: 'jhi-appointment-book',
  standalone: true,
  templateUrl: './appointment-book.html',
  styleUrls: ['./appointment-book.scss'],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, FontAwesomeModule, NgbModalModule],
})
export class AppointmentBook implements OnInit {
  protected readonly accountService = inject(AccountService);
  protected readonly doctorProfileService = inject(DoctorProfileService);
  protected readonly availabilitySlotService = inject(AvailabilitySlotService);
  protected readonly patientBookingService = inject(PatientBookingService);
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly router = inject(Router);
  protected readonly modalService = inject(NgbModal);
  private readonly fb = inject(FormBuilder);

  // States
  readonly isLoading = signal<boolean>(false);
  readonly doctor = signal<IDoctorProfile | null>(null);
  readonly doctorId = signal<number | null>(null);
  readonly slots = signal<IAvailabilitySlot[]>([]);
  readonly currentWeekStart = signal<dayjs.Dayjs>(getStartOfIsoWeek(dayjs()));
  readonly selectedSlot = signal<IAvailabilitySlot | null>(null);
  readonly bookingSuccess = signal<boolean>(false);
  readonly successBookingCode = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly modalErrorMessage = signal<string | null>(null);

  // Booking Form
  bookingForm: FormGroup = this.fb.group({
    slotId: [null, [Validators.required]],
    consultationMode: ['ONLINE', [Validators.required]],
    patientName: ['', [Validators.required]],
    patientEmail: ['', [Validators.required, Validators.email]],
    contactPhone: ['', [Validators.required]],
    contactZalo: [''],
    patientAddress: [''],
    reason: ['', [Validators.required]],
    symptoms: [''],
    medicalHistory: [''],
    expectations: [''],
  });

  // Computed properties
  readonly weekEnd = computed(() => getEndOfIsoWeek(this.currentWeekStart()));
  readonly weekRangeDisplay = computed(() => {
    const start = this.currentWeekStart();
    const end = this.weekEnd();
    return `${start.format('DD/MM/YYYY')} - ${end.format('DD/MM/YYYY')}`;
  });

  readonly doctorInitials = computed(() => {
    const name = this.doctor()?.fullName;
    if (!name) return 'DR';
    const parts = name.replace('Dr.', '').replace('BS.', '').trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  });

  readonly weekDays = computed<IBookingDayColumn[]>(() => {
    const start = this.currentWeekStart();
    const result: IBookingDayColumn[] = [];
    const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
    const todayStr = dayjs().format('YYYY-MM-DD');
    const allSlots = this.slots();

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
        slots: daySlots,
      });
    }

    return result;
  });

  ngOnInit(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params['doctorId'] || this.activatedRoute.snapshot.queryParams['doctorId'];
      if (id) {
        this.doctorId.set(Number(id));
        this.loadDoctor(Number(id));
      }
    });

    this.activatedRoute.queryParams.subscribe(queryParams => {
      const id = queryParams['doctorId'];
      if (id && !this.doctorId()) {
        this.doctorId.set(Number(id));
        this.loadDoctor(Number(id));
      }
    });

    // Điền thông tin profile người dùng hiện tại nếu có
    this.accountService.identity().subscribe(acc => {
      if (acc) {
        this.bookingForm.patchValue({
          patientName: `${acc.firstName || ''} ${acc.lastName || ''}`.trim() || acc.login,
          patientEmail: acc.email || '',
        });
      }
    });
  }

  loadDoctor(id: number): void {
    this.isLoading.set(true);
    this.doctorProfileService.find(id).subscribe({
      next: res => {
        if (res) {
          this.doctor.set(res);
          this.loadDoctorSlots(id);
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Không tìm thấy thông tin bác sĩ');
      },
    });
  }

  loadDoctorSlots(doctorId: number): void {
    this.availabilitySlotService
      .query({
        'doctorId.equals': doctorId,
        size: 100,
        sort: ['startTime,asc'],
      })
      .subscribe({
        next: res => {
          this.slots.set(res.body || []);
          this.isLoading.set(false);
        },
        error: () => {
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

  isSlotInPast(slot: IAvailabilitySlot): boolean {
    return !slot.startTime || dayjs(slot.startTime).isBefore(dayjs());
  }

  // Click slot -> Open Modal
  selectSlotForBooking(slot: IAvailabilitySlot, content: any): void {
    this.errorMessage.set(null);
    this.modalErrorMessage.set(null);

    if (slot.status !== 'AVAILABLE' || this.isSlotInPast(slot)) {
      this.errorMessage.set('Khung giờ này không khả dụng hoặc đã qua trong quá khứ!');
      return;
    }

    this.selectedSlot.set(slot);
    this.bookingForm.patchValue({
      slotId: slot.id,
      consultationMode: slot.consultationMode === 'IN_PERSON' ? 'IN_PERSON' : 'ONLINE',
    });

    this.modalService.open(content, { size: 'lg', centered: true, backdrop: 'static' });
  }

  submitBooking(modal: any): void {
    this.modalErrorMessage.set(null);

    if (this.bookingForm.invalid) {
      this.bookingForm.markAllAsTouched();
      this.modalErrorMessage.set('Vui lòng điền đầy đủ và hợp lệ các thông tin bắt buộc (*).');
      return;
    }

    if (!this.selectedSlot()) {
      this.modalErrorMessage.set('Chưa chọn khung giờ khám hợp lệ.');
      return;
    }

    const val = this.bookingForm.value;
    const req: ICreateBookingRequest = {
      slotId: val.slotId,
      consultationMode: val.consultationMode,
      reason: val.reason,
      symptoms: val.symptoms,
      medicalHistory: val.medicalHistory,
      expectations: val.expectations,
      contactPhone: val.contactPhone,
      contactZalo: val.contactZalo,
      patientAddress: val.patientAddress,
    };

    this.isLoading.set(true);
    this.patientBookingService.bookAppointment(req).subscribe({
      next: res => {
        this.isLoading.set(false);
        this.bookingSuccess.set(true);
        this.successBookingCode.set(res.bookingCode || null);
        modal.close();
      },
      error: err => {
        this.isLoading.set(false);
        let msg = 'Không thể đặt lịch, vui lòng thử lại.';
        if (err.error?.errorKey === 'slotnotavailable') {
          msg = 'Khung giờ này vừa có người khác đặt hoặc không còn khả dụng.';
        } else if (err.error?.errorKey === 'slotinpast') {
          msg = 'Khung giờ này đã qua trong quá khứ.';
        } else if (err.error?.errorKey === 'patientnotfound') {
          msg = 'Không tìm thấy hồ sơ bệnh nhân cho tài khoản này.';
        } else if (err.error?.errorKey === 'unauthenticated') {
          msg = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
        } else if (err.error?.title) {
          msg = err.error.title;
        } else if (err.error?.message) {
          msg = err.error.message;
        } else if (typeof err.error === 'string') {
          msg = err.error;
        }
        this.modalErrorMessage.set(msg);
        this.errorMessage.set(msg);
      },
    });
  }

  formatSlotTime(slot: IAvailabilitySlot): string {
    if (!slot.startTime || !slot.endTime) return '';
    return `${dayjs(slot.startTime).format('HH:mm')} - ${dayjs(slot.endTime).format('HH:mm')}`;
  }

  formatSlotDate(slot: IAvailabilitySlot): string {
    if (!slot.startTime) return '';
    return dayjs(slot.startTime).format('DD/MM/YYYY');
  }

  getModeIcon(mode: string | null | undefined): string {
    switch (mode) {
      case 'ONLINE':
        return 'bi-camera-video';
      case 'IN_PERSON':
        return 'bi-hospital';
      default:
        return 'bi-arrow-repeat';
    }
  }

  goToMyAppointments(): void {
    this.router.navigate(['/appointment']);
  }
}
