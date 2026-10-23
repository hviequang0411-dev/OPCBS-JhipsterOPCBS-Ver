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

/* =========================================================================
 * CODE CŨ TRƯỚC ĐÂY:
 * Tính toán ngày theo tuần ISO (Luôn bắt đầu từ Thứ 2)
 * -------------------------------------------------------------------------
 * function getStartOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
 *   const day = d.day();
 *   const diff = day === 0 ? -6 : 1 - day;
 *   return d.add(diff, 'day').startOf('day');
 * }
 *
 * function getEndOfIsoWeek(d: dayjs.Dayjs): dayjs.Dayjs {
 *   return getStartOfIsoWeek(d).add(6, 'day').endOf('day');
 * }
 * ========================================================================= */

export interface IBookingDayColumn {
  date: dayjs.Dayjs;
  dateStr: string;
  dayName: string;
  dayNumber: string;
  isToday: boolean;
  isSelected: boolean;
  slots?: IAvailabilitySlot[];
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

  /* CODE CŨ TRƯỚC ĐÂY:
   * readonly currentWeekStart = signal<dayjs.Dayjs>(getStartOfIsoWeek(dayjs()));
   * readonly slots = signal<IAvailabilitySlot[]>([]);
   */
  readonly slots = signal<IAvailabilitySlot[]>([]);

  // CODE MỚI: Cột đầu tiên của Grid luôn bắt đầu từ Ngày Hôm Nay
  readonly currentStartDate = signal<dayjs.Dayjs>(dayjs().startOf('day'));
  readonly selectedDate = signal<dayjs.Dayjs>(dayjs().startOf('day'));

  // CODE MỚI: Quản lý Slot của 1 ngày dưới dạng 1 trang riêng (Daily Slot Page & Pagination)
  readonly dailySlots = signal<IAvailabilitySlot[]>([]);
  readonly dailyPage = signal<number>(1);
  readonly dailyPageSize = signal<number>(8); // 8 khung giờ / trang
  readonly dailyTotalSlots = signal<number>(0);
  readonly isLoadingDailySlots = signal<boolean>(false);

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
  readonly canGoPrev = computed(() => this.currentStartDate().isAfter(dayjs().startOf('day')));
  readonly endDate = computed(() => this.currentStartDate().add(6, 'day').endOf('day'));

  readonly dateRangeDisplay = computed(() => {
    const start = this.currentStartDate();
    const end = this.endDate();
    return `${start.format('DD/MM/YYYY')} - ${end.format('DD/MM/YYYY')}`;
  });
  // Giữ alias tương thích
  readonly weekRangeDisplay = computed(() => this.dateRangeDisplay());

  readonly dailyTotalPages = computed(() => Math.max(1, Math.ceil(this.dailyTotalSlots() / this.dailyPageSize())));

  readonly selectedDateFormatted = computed(() => {
    const d = this.selectedDate();
    const dayNames = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
    const isToday = d.isSame(dayjs(), 'day');
    const isTomorrow = d.isSame(dayjs().add(1, 'day'), 'day');
    const prefix = isToday ? 'Hôm nay' : isTomorrow ? 'Ngày mai' : dayNames[d.day()];
    return `${prefix} (${dayNames[d.day()]}), ${d.format('DD/MM/YYYY')}`;
  });

  // Phân chia slot theo các ca trong ngày cho giao diện dễ chọn
  readonly morningSlots = computed(() =>
    this.dailySlots().filter(s => {
      if (!s.startTime) return false;
      const h = dayjs(s.startTime).hour();
      return h < 12;
    })
  );

  readonly afternoonSlots = computed(() =>
    this.dailySlots().filter(s => {
      if (!s.startTime) return false;
      const h = dayjs(s.startTime).hour();
      return h >= 12 && h < 18;
    })
  );

  readonly eveningSlots = computed(() =>
    this.dailySlots().filter(s => {
      if (!s.startTime) return false;
      const h = dayjs(s.startTime).hour();
      return h >= 18;
    })
  );

  readonly doctorInitials = computed(() => {
    const name = this.doctor()?.fullName;
    if (!name) return 'DR';
    const parts = name.replace('Dr.', '').replace('BS.', '').trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  });

  // CODE MỚI: Grid hiển thị 7 ngày, cột đầu tiên BẮT ĐẦU TỪ NGÀY HÔM NAY (hoặc ngày bắt đầu được duyệt)
  readonly rollingDays = computed<IBookingDayColumn[]>(() => {
    const start = this.currentStartDate();
    const result: IBookingDayColumn[] = [];
    const dayNames = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
    const today = dayjs().startOf('day');
    const todayStr = today.format('YYYY-MM-DD');
    const tomorrowStr = today.add(1, 'day').format('YYYY-MM-DD');
    const selectedStr = this.selectedDate().format('YYYY-MM-DD');

    for (let i = 0; i < 7; i++) {
      const d = start.add(i, 'day');
      const dStr = d.format('YYYY-MM-DD');
      const isToday = dStr === todayStr;
      const isTomorrow = dStr === tomorrowStr;

      let label = dayNames[d.day()];
      if (isToday) {
        label = 'Hôm nay';
      } else if (isTomorrow) {
        label = 'Ngày mai';
      }

      result.push({
        date: d,
        dateStr: dStr,
        dayName: label,
        dayNumber: d.format('DD/MM'),
        isToday,
        isSelected: dStr === selectedStr,
        slots: [],
      });
    }

    return result;
  });

  // Giữ alias weekDays để tương thích
  readonly weekDays = computed<IBookingDayColumn[]>(() => this.rollingDays());

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

  /* =========================================================================
   * CODE CŨ TRƯỚC ĐÂY:
   * Query bừa 200 slot theo tuần không giới hạn ngày kết thúc, sau đó lọc thủ công cả tuần
   * -------------------------------------------------------------------------
   * loadDoctorSlots(doctorId: number): void {
   *   const queryStart = this.currentWeekStart().isBefore(dayjs().startOf('day'))
   *     ? this.currentWeekStart().startOf('day').toISOString()
   *     : dayjs().startOf('day').toISOString();
   *
   *   this.availabilitySlotService
   *     .query({
   *       'doctorId.equals': doctorId,
   *       'status.equals': 'AVAILABLE',
   *       'startTime.greaterThanOrEqual': queryStart,
   *       size: 200,
   *       sort: ['startTime,asc'],
   *     })
   *     .subscribe({
   *       next: res => {
   *         this.slots.set(res.body || []);
   *         this.isLoading.set(false);
   *       },
   *       error: () => {
   *         this.isLoading.set(false);
   *       },
   *     });
   * }
   * ========================================================================= */

  /**
   * CODE MỚI: Tải các slot cho đúng 1 ngày được chọn (Daily Slot Page).
   * Phân trang theo từng ngày (1 ngày là 1 trang), không lấy hết slot rồi đếm lên cả tuần.
   */
  loadSlotsForDate(doctorId: number, targetDate: dayjs.Dayjs, page = 1): void {
    this.isLoadingDailySlots.set(true);
    const startOfDay = targetDate.startOf('day').toISOString();
    const endOfDay = targetDate.endOf('day').toISOString();

    this.availabilitySlotService
      .query({
        'doctorId.equals': doctorId,
        'status.equals': 'AVAILABLE',
        'startTime.greaterThanOrEqual': startOfDay,
        'startTime.lessThanOrEqual': endOfDay,
        page: page - 1,
        size: this.dailyPageSize(),
        sort: ['startTime,asc'],
      })
      .subscribe({
        next: res => {
          this.dailySlots.set(res.body || []);
          const totalHeader = res.headers.get('X-Total-Count');
          const total = totalHeader ? Number(totalHeader) : (res.body ? res.body.length : 0);
          this.dailyTotalSlots.set(total);
          this.isLoadingDailySlots.set(false);
          this.isLoading.set(false);
        },
        error: () => {
          this.dailySlots.set([]);
          this.dailyTotalSlots.set(0);
          this.isLoadingDailySlots.set(false);
          this.isLoading.set(false);
        },
      });
  }

  // Tương thích khi gọi load slot ban đầu
  loadDoctorSlots(doctorId: number): void {
    this.loadSlotsForDate(doctorId, this.selectedDate(), 1);
  }

  // Chọn ngày để xem trang slot trong ngày đó
  selectDate(date: dayjs.Dayjs): void {
    if (date.isBefore(dayjs().startOf('day'))) {
      return; // Không cho phép chọn ngày quá khứ
    }
    this.selectedDate.set(date.startOf('day'));
    this.dailyPage.set(1);
    if (this.doctorId()) {
      this.loadSlotsForDate(this.doctorId()!, this.selectedDate(), 1);
    }
  }

  // Chuyển trang trong ngày (Daily Pagination)
  changeDailyPage(newPage: number): void {
    if (newPage < 1 || newPage > this.dailyTotalPages() || newPage === this.dailyPage()) {
      return;
    }
    this.dailyPage.set(newPage);
    if (this.doctorId()) {
      this.loadSlotsForDate(this.doctorId()!, this.selectedDate(), newPage);
    }
  }

  // Điều hướng ngày: Lùi 7 ngày (nhưng không vượt quá Ngày hôm nay)
  /* CODE CŨ TRƯỚC ĐÂY:
   * prevWeek(): void {
   *   this.currentWeekStart.set(this.currentWeekStart().subtract(1, 'week'));
   * }
   */
  prevDays(): void {
    if (!this.canGoPrev()) return;
    const newStart = this.currentStartDate().subtract(7, 'day');
    const today = dayjs().startOf('day');
    const safeStart = newStart.isBefore(today) ? today : newStart;
    this.currentStartDate.set(safeStart);
    this.selectDate(safeStart);
  }
  prevWeek(): void {
    this.prevDays();
  }

  // Điều hướng ngày: Tiến 7 ngày tiếp theo
  /* CODE CŨ TRƯỚC ĐÂY:
   * nextWeek(): void {
   *   this.currentWeekStart.set(this.currentWeekStart().add(1, 'week'));
   * }
   */
  nextDays(): void {
    const nextStart = this.currentStartDate().add(7, 'day');
    this.currentStartDate.set(nextStart);
    this.selectDate(nextStart);
  }
  nextWeek(): void {
    this.nextDays();
  }

  // Trở về Ngày Hôm Nay: Cột đầu tiên sẽ ngay lập tức là Hôm Nay
  /* CODE CŨ TRƯỚC ĐÂY:
   * goToToday(): void {
   *   this.currentWeekStart.set(getStartOfIsoWeek(dayjs()));
   * }
   */
  goToToday(): void {
    const today = dayjs().startOf('day');
    this.currentStartDate.set(today);
    this.selectDate(today);
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
        const msg = this.extractBookingErrorMessage(err);
        this.modalErrorMessage.set(msg);
        this.errorMessage.set(msg);
      },
    });
  }

  /**
   * Chuyển đổi mã lỗi và exception từ server thành thông báo thân thiện, chuẩn y tế cho bệnh nhân.
   * Tuyệt đối không để lộ thông báo thô 'Bad Request' hay các mã kỹ thuật.
   */
  private extractBookingErrorMessage(err: any): string {
    // 1. Thu thập Error Key từ nhiều nguồn (errorKey, message, headers, error body)
    let key: string | null = null;
    if (err.error?.errorKey && typeof err.error.errorKey === 'string') {
      key = err.error.errorKey;
    } else if (err.error?.message && typeof err.error.message === 'string' && err.error.message.startsWith('error.')) {
      key = err.error.message.replace(/^error\./, '');
    } else if (err.headers && typeof err.headers.get === 'function') {
      const headerKey = err.headers.get('x-opcbs-error');
      if (headerKey) {
        key = headerKey.replace(/^error\./, '');
      }
    }

    // 2. Tra cứu theo Error Key từ nghiệp vụ backend
    switch (key) {
      case 'slotnotavailable':
        return 'Rất tiếc! Khung giờ này vừa có người khác đặt hoặc bác sĩ tạm đóng. Vui lòng chọn một khung giờ khác.';
      case 'slotinpast':
        return 'Khung giờ khám này đã qua trong quá khứ. Vui lòng chọn thời gian khám sắp tới.';
      case 'maxonedailyappointmentwithdoctor':
        return 'Bạn đã có lịch hẹn với Bác sĩ này trong ngày. Theo quy định phòng khám, mỗi bệnh nhân chỉ được đặt tối đa 1 ca khám/ngày với cùng một bác sĩ.';
      case 'overlappingappointment':
        return 'Bạn đã có một lịch khám khác diễn ra trong cùng khung giờ này. Vui lòng kiểm tra lại lịch khám cá nhân để tránh trùng lặp.';
      case 'maxthreeappointmentsperweekwithdoctor':
        return 'Bạn đã đạt giới hạn 3 lịch hẹn đang hoạt động trong tuần với Bác sĩ này. Vui lòng hoàn thành buổi khám hiện tại trước khi đặt thêm.';
      case 'maxtotalappointmentsperweek':
        return 'Bạn đã đạt hạn mức tối đa 5 lịch hẹn trong tuần trên toàn hệ thống MindBridge. Vui lòng hoàn tất các ca khám hiện tại trước khi tiếp tục đặt chỗ.';
      case 'doctornotfound':
        return 'Không tìm thấy thông tin bác sĩ phụ trách khung giờ này hoặc bác sĩ tạm ngưng tiếp nhận lịch.';
      case 'doctorsubscriptionexpired':
        return 'Bác sĩ hiện đang tạm ngưng nhận lịch mới. Vui lòng liên hệ bộ phận hỗ trợ hoặc chọn một chuyên gia khác.';
      case 'slotnotfound':
        return 'Khung giờ khám bạn chọn không tồn tại hoặc đã bị hủy.';
      case 'patientnotfound':
        return 'Không tìm thấy hồ sơ bệnh nhân cho tài khoản này. Vui lòng cập nhật thông tin cá nhân.';
      case 'unauthenticated':
        return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.';
      case 'packagenosessions':
        return 'Gói điều trị của bạn đã dùng hết số buổi tham vấn. Vui lòng đặt khám thông thường hoặc mua gói mới.';
      case 'packagenotactive':
        return 'Gói điều trị của bạn hiện chưa được kích hoạt hoặc đang tạm ngưng.';
      case 'packageexpired':
        return 'Gói điều trị của bạn đã hết hạn sử dụng. Vui lòng liên hệ bác sĩ phụ trách.';
      case 'duplicateslotbooking':
        return 'Bạn đã đặt khung giờ này trước đó và yêu cầu đang được hệ thống tiếp nhận.';
      case 'accessdenied':
        return 'Bạn không có quyền thực hiện thao tác này.';
      case 'cannotcancelcompleted':
        return 'Không thể hủy ca khám đã hoàn thành.';
      case 'alreadycancelled':
        return 'Lịch hẹn này đã bị hủy trước đó.';
    }

    // 3. Nếu Backend trả về title hoặc detail cụ thể (không phải 'Bad Request')
    const rawTitle = err.error?.title;
    const rawDetail = err.error?.detail;
    const defaultMsg = err.error?.defaultMessage;

    if (defaultMsg && typeof defaultMsg === 'string' && !defaultMsg.toLowerCase().includes('bad request')) {
      return defaultMsg;
    }
    if (rawTitle && typeof rawTitle === 'string' && !rawTitle.toLowerCase().includes('bad request')) {
      return rawTitle;
    }
    if (rawDetail && typeof rawDetail === 'string' && !rawDetail.toLowerCase().includes('bad request')) {
      return rawDetail;
    }

    // 4. Nếu là chuỗi string thuần
    if (typeof err.error === 'string' && !err.error.toLowerCase().includes('bad request')) {
      return err.error;
    }

    // 5. Fallback mặc định lịch sự & êm dịu theo HTTP status, KHÔNG BAO GIỜ hiện chữ Bad Request
    if (err.status === 400) {
      return 'Yêu cầu đặt lịch chưa hợp lệ hoặc vi phạm quy định khung giờ khám. Vui lòng kiểm tra lại thông tin hoặc chọn thời gian khác.';
    }
    if (err.status === 401) {
      return 'Vui lòng đăng nhập tài khoản bệnh nhân để thực hiện đặt lịch khám.';
    }
    if (err.status === 403) {
      return 'Tài khoản của bạn không có quyền đặt lịch khám. Tính năng dành riêng cho Bệnh nhân.';
    }
    if (err.status === 409) {
      return 'Khung giờ này vừa phát sinh xung đột với lượt đặt khác. Vui lòng làm mới trang và chọn lại.';
    }
    if (err.status === 500) {
      return 'Hệ thống phòng khám đang bận xử lý hoặc gặp sự cố tạm thời. Vui lòng thử lại sau ít phút.';
    }

    return 'Không thể hoàn tất đặt lịch lúc này. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.';
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
