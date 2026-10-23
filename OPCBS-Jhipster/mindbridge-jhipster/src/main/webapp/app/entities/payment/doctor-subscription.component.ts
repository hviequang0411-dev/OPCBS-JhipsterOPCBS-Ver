import { Component, OnInit, inject, signal, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PaymentService } from './payment.service';
import { IServicePackage, IDoctorSubscription, IDoctorSubscriptionQuota } from './payment.model';
import { FormatMediumDatePipe } from 'app/shared/date';

@Component({
  selector: 'mb-doctor-subscription',
  standalone: true,
  imports: [CommonModule, RouterModule, FormatMediumDatePipe],
  templateUrl: './doctor-subscription.component.html',
  styleUrls: ['./doctor-subscription.component.scss'],
})
export class DoctorSubscriptionComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly modalService = inject(NgbModal);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  activeTab = signal<'STATUS' | 'HISTORY' | 'PLANS'>('STATUS');
  packages = signal<IServicePackage[]>([]);
  currentSubscription = signal<IDoctorSubscription | null>(null);
  subscriptionQuota = signal<IDoctorSubscriptionQuota | null>(null);
  subscriptionHistory = signal<IDoctorSubscription[]>([]);
  historyFilter = signal<string>('all');
  selectedHistorySub = signal<IDoctorSubscription | null>(null);

  isLoading = signal<boolean>(false);
  isSubscribing = signal<boolean>(false);
  isRedirectingVnPay = signal<boolean>(false);
  selectedPackage = signal<IServicePackage | null>(null);
  selectedPaymentMethod = signal<'MOCK_CHECKOUT' | 'VNPAY' | 'BANK_TRANSFER'>('MOCK_CHECKOUT');

  // Renewal state
  isRenewing = signal<boolean>(false);
  renewalTargetSub = signal<IDoctorSubscription | null>(null);

  activeTxn = signal<{
    subscriptionId: number;
    transactionCode: string;
    amount: number;
    packageName: string;
  } | null>(null);

  paymentSuccess = signal<boolean>(false);
  vnpayCallbackResult = signal<{
    success: boolean;
    message: string;
    transactionCode?: string;
    isIdempotent?: boolean;
    expirationDate?: string;
  } | null>(null);

  ngOnInit(): void {
    this.checkVnPayCallback();
    this.loadData();
    this.loadHistory();
  }

  /**
   * Switch between Tabs: STATUS (Gói hiện tại), HISTORY (Lịch sử giao dịch), PLANS (Bảng giá gói)
   */
  setActiveTab(tab: 'STATUS' | 'HISTORY' | 'PLANS'): void {
    this.activeTab.set(tab);
    if (tab === 'HISTORY' && this.subscriptionHistory().length === 0) {
      this.loadHistory();
    }
  }

  /**
   * Check and process VNPay gateway return query parameters when redirected back
   */
  private checkVnPayCallback(): void {
    this.route.queryParams.subscribe(params => {
      const txnRef = params['vnp_TxnRef'];
      const responseCode = params['vnp_ResponseCode'];

      if (txnRef && responseCode !== undefined) {
        this.isLoading.set(true);
        this.paymentService.handleVnPayCallback(params).subscribe({
          next: res => {
            this.isLoading.set(false);
            if (res.RspCode === '00') {
              this.vnpayCallbackResult.set({
                success: true,
                message: res.isIdempotentReplay 
                  ? 'Giao dịch VNPay đã được ghi nhận thành công trước đó (Idempotent Guard kích hoạt).' 
                  : 'Thanh toán trực tuyến VNPay Sandbox thành công! Gói hội viên bác sĩ đã được kích hoạt / gia hạn tức thì.',
                transactionCode: res.transactionCode || txnRef,
                isIdempotent: res.isIdempotentReplay,
                expirationDate: res.expirationDate,
              });
              this.loadData();
              this.loadHistory();
              this.activeTab.set('STATUS');
            } else {
              this.vnpayCallbackResult.set({
                success: false,
                message: `Giao dịch VNPay không thành công hoặc đã bị người dùng hủy (Mã phản hồi VNPay: ${responseCode}).`,
                transactionCode: txnRef,
              });
            }

            // Clean url so page refresh won't re-fire callback
            this.router.navigate(['/doctor/subscriptions'], { replaceUrl: true });
          },
          error: err => {
            this.isLoading.set(false);
            this.vnpayCallbackResult.set({
              success: false,
              message: err.error?.message || 'Không thể xác thực kết quả thanh toán với VNPay Sandbox.',
              transactionCode: txnRef,
            });
            this.router.navigate(['/doctor/subscriptions'], { replaceUrl: true });
          },
        });
      }
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    this.paymentService.getMySubscription().subscribe({
      next: sub => {
        this.currentSubscription.set(sub || null);
        // If doctor has no active subscription yet, default tab can stay STATUS with empty state or switch
      },
      error: () => {
        this.currentSubscription.set(null);
      },
    });

    this.paymentService.getSubscriptionQuota().subscribe({
      next: quota => {
        this.subscriptionQuota.set(quota || null);
      },
      error: () => {
        this.subscriptionQuota.set(null);
      },
    });

    this.paymentService.getServicePackages().subscribe({
      next: pkgs => {
        this.packages.set(pkgs || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.packages.set([]);
        this.isLoading.set(false);
      },
    });
  }

  loadHistory(): void {
    this.paymentService.getMySubscriptionHistory().subscribe({
      next: history => {
        this.subscriptionHistory.set(history || []);
      },
      error: () => {
        this.subscriptionHistory.set([]);
      },
    });
  }

  /**
   * Filter subscription history list
   */
  filteredHistory(): IDoctorSubscription[] {
    const filter = this.historyFilter();
    const list = this.subscriptionHistory();
    if (filter === 'all') {
      return list;
    }
    return list.filter(item => (item.status || '').toUpperCase() === filter.toUpperCase());
  }

  setHistoryFilter(filter: string): void {
    this.historyFilter.set(filter);
  }

  /**
   * Helper: Calculate remaining days
   */
  getDaysRemaining(sub: IDoctorSubscription | null): number {
    if (!sub || !sub.expirationDate) return 0;
    const exp = new Date(sub.expirationDate).getTime();
    const now = new Date().getTime();
    const diffDays = Math.ceil((exp - now) / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  }

  /**
   * Helper: Calculate progress percentage used
   */
  getProgressPct(sub: IDoctorSubscription | null): number {
    if (!sub || !sub.startDate || !sub.expirationDate) return 100;
    const start = new Date(sub.startDate).getTime();
    const exp = new Date(sub.expirationDate).getTime();
    const now = new Date().getTime();

    if (now >= exp) return 100;
    if (now <= start) return 0;

    const total = exp - start;
    const used = now - start;
    if (total <= 0) return 100;
    return Math.min(100, Math.max(0, Math.round((used / total) * 100)));
  }

  /**
   * Check if subscription is active and not expired
   */
  isSubActive(sub: IDoctorSubscription | null): boolean {
    if (!sub) return false;
    const status = (sub.status || '').toUpperCase();
    if (status !== 'ACTIVE') return false;
    if (!sub.expirationDate) return false;
    return new Date(sub.expirationDate).getTime() > new Date().getTime();
  }

  /**
   * Check if subscription has expired
   */
  isSubExpired(sub: IDoctorSubscription | null): boolean {
    if (!sub) return false;
    const status = (sub.status || '').toUpperCase();
    if (status === 'EXPIRED') return true;
    if (!sub.expirationDate) return false;
    return new Date(sub.expirationDate).getTime() <= new Date().getTime();
  }

  /**
   * Check if subscription is near expiration (within 7 days)
   */
  isNearExpiration(sub: IDoctorSubscription | null): boolean {
    if (!this.isSubActive(sub)) return false;
    const days = this.getDaysRemaining(sub);
    return days <= 7 && days >= 0;
  }

  /**
   * Estimate new expiration date upon renewal (Smart Extension)
   */
  getProjectedExpirationDate(sub: IDoctorSubscription | null, durationDays: number = 30): Date {
    const baseDate = (sub && this.isSubActive(sub) && sub.expirationDate) 
      ? new Date(sub.expirationDate) 
      : new Date();
    const projected = new Date(baseDate);
    projected.setDate(projected.getDate() + durationDays);
    return projected;
  }

  /**
   * Resolve package duration in days
   */
  getPackageDuration(sub: IDoctorSubscription | null): number {
    if (!sub) return 30;
    if (sub.durationDays) return sub.durationDays;
    const pkg = this.packages().find(p => p.id === sub.servicePackageId);
    return pkg?.durationDays || 30;
  }

  /**
   * Resolve max appointments per month
   */
  getPackageMaxAppointments(sub: IDoctorSubscription | null): number {
    if (!sub) return 30;
    if (sub.maxAppointmentsPerMonth) return sub.maxAppointmentsPerMonth;
    const pkg = this.packages().find(p => p.id === sub.servicePackageId);
    return pkg?.maxAppointmentsPerMonth || 30;
  }

  /**
   * Calculate total spent across all historical subscriptions
   */
  getTotalSpent(): number {
    return this.subscriptionHistory()
      .filter(s => s.status === 'ACTIVE' || s.status === 'EXTENDED' || s.status === 'EXPIRED')
      .reduce((sum, s) => sum + (s.price || 0), 0);
  }

  getSanitizedDescription(pkg: IServicePackage): string {
    const desc = pkg.description || '';
    if (desc.includes('??') || desc.trim().length === 0) {
      const name = (pkg.name || '').toLowerCase();
      if (name.includes('standard') || name.includes('starter') || name.includes('cơ bản')) {
        return 'Gói dịch vụ khởi đầu dành cho Bác sĩ & Chuyên gia tư vấn cá nhân, tiếp nhận và quản lý hồ sơ bệnh nhân tiêu chuẩn.';
      }
      if (name.includes('professional') || name.includes('chuyên nghiệp') || name.includes('clinic')) {
        return 'Giải pháp chuyên sâu tối ưu hiệu suất khám chữa bệnh, tích hợp đầy đủ bộ trắc nghiệm DASS-21 và ưu tiên đề xuất trên hệ thống.';
      }
      return 'Đặc quyền phòng khám cao cấp với số ca khám không giới hạn, phác đồ điều trị chuyên biệt và hỗ trợ kỹ thuật y tế 24/7.';
    }
    return desc;
  }

  getFeaturesList(pkg: IServicePackage): string[] {
    if (pkg.features && !pkg.features.includes('??') && pkg.features.trim().length > 0) {
      const parsed = pkg.features.split(',').map(f => f.trim()).filter(f => f.length > 0);
      if (parsed.length >= 3) return parsed;
    }

    const price = pkg.price || 0;
    if (price < 300000) {
      return [
        `Tiếp nhận tới ${pkg.maxAppointmentsPerMonth || 30} ca khám / tháng`,
        'Hồ sơ bệnh án điện tử chuẩn bảo mật y tế',
        'Kho trắc nghiệm tâm lý tiêu chuẩn (DASS-21)',
        'Lên lịch hẹn & nhắc lịch tự động cho người bệnh',
        'Hỗ trợ kỹ thuật trực tuyến 24/7'
      ];
    } else if (price < 800000) {
      return [
        `Tiếp nhận tới ${pkg.maxAppointmentsPerMonth || 100} ca khám / tháng`,
        'Trọn bộ trắc nghiệm chuẩn y khoa (DASS-21, PHQ-9, GAD-7)',
        'Thiết kế phác đồ điều trị cá nhân hóa đa buổi',
        'Tự động cảnh báo ca bệnh nguy cơ cao (Event Producer)',
        'Huy hiệu Bác Sĩ Xác Thực trên danh bạ MindBridge',
        'Ưu tiên kết nối bệnh nhân mới theo chuyên khoa'
      ];
    } else {
      return [
        'Tiếp nhận số ca khám KHÔNG GIỚI HẠN',
        'Bao gồm toàn bộ quyền lợi gói Chuyên Nghiệp',
        'Kê đơn lộ trình can thiệp khủng hoảng & stress cấp',
        'Xuất báo cáo tâm lý định dạng PDF chuẩn quốc tế',
        'Bảng thống kê hiệu suất phòng khám thời gian thực',
        'Đường dây nóng hỗ trợ ưu tiên 24/7'
      ];
    }
  }

  getTierIcon(pkg: IServicePackage): string {
    const price = pkg.price || 0;
    if (price < 300000) return 'bi bi-person-check-fill';
    if (price < 800000) return 'bi bi-award-fill';
    return 'bi bi-hospital-fill';
  }

  getStatusBadgeClass(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'subscription-badge-active';
      case 'EXTENDED':
        return 'subscription-badge-extended';
      case 'PENDING_PAYMENT':
        return 'subscription-badge-pending';
      case 'EXPIRED':
        return 'subscription-badge-expired';
      case 'CANCELLED':
        return 'subscription-badge-cancelled';
      default:
        return 'subscription-badge-default';
    }
  }

  getStatusLabel(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'Đang hoạt động';
      case 'EXTENDED':
        return 'Đã gia hạn';
      case 'PENDING_PAYMENT':
        return 'Chờ thanh toán';
      case 'EXPIRED':
        return 'Đã hết hạn';
      case 'CANCELLED':
        return 'Đã hủy';
      default:
        return status || 'Chưa xác định';
    }
  }

  /**
   * Open Subscription / Upgrade Modal
   */
  openCheckoutModal(content: TemplateRef<any>, pkg: IServicePackage): void {
    this.selectedPackage.set(pkg);
    this.paymentSuccess.set(false);
    this.selectedPaymentMethod.set('MOCK_CHECKOUT');
    this.isSubscribing.set(true);

    this.paymentService.subscribePackage(pkg.id).subscribe({
      next: res => {
        this.activeTxn.set(res);
        this.isSubscribing.set(false);
        this.modalService.open(content, { centered: true, size: 'lg', backdrop: 'static' });
      },
      error: () => {
        this.isSubscribing.set(false);
      },
    });
  }

  /**
   * Open Renewal Modal for Current Subscription or expired item
   */
  openRenewModal(content: TemplateRef<any>, sub?: IDoctorSubscription): void {
    const target = sub || this.currentSubscription();
    if (!target) return;
    this.renewalTargetSub.set(target);
    this.paymentSuccess.set(false);
    this.selectedPaymentMethod.set('MOCK_CHECKOUT');
    this.isRenewing.set(true);

    // Call backend to create renewal transaction
    this.paymentService.renewSubscription(target.servicePackageId).subscribe({
      next: res => {
        this.activeTxn.set(res);
        this.isRenewing.set(false);
        this.modalService.open(content, { centered: true, size: 'lg', backdrop: 'static' });
      },
      error: () => {
        this.isRenewing.set(false);
      },
    });
  }

  /**
   * Open History Detail Modal
   */
  openHistoryDetailModal(content: TemplateRef<any>, item: IDoctorSubscription): void {
    this.selectedHistorySub.set(item);
    this.modalService.open(content, { centered: true, size: 'md' });
  }

  selectPaymentMethod(method: 'MOCK_CHECKOUT' | 'VNPAY' | 'BANK_TRANSFER'): void {
    this.selectedPaymentMethod.set(method);
  }

  processMockCheckout(modal: any): void {
    const txn = this.activeTxn();
    if (!txn) return;

    this.isSubscribing.set(true);
    this.paymentService.mockCheckout(txn.transactionCode).subscribe({
      next: () => {
        this.isSubscribing.set(false);
        this.paymentSuccess.set(true);
        this.loadData();
        this.loadHistory();
      },
      error: err => {
        this.isSubscribing.set(false);
        alert(err.error?.message || 'Không thể thực hiện thanh toán giả lập.');
      },
    });
  }

  acknowledgeBankTransfer(modal: any): void {
    modal.close();
    this.loadData();
    this.loadHistory();
  }

  getPackagePatientCapacity(sub: IDoctorSubscription | null): string {
    if (!sub) return 'Không giới hạn';
    if (sub.maxPatientCapacity) return `${sub.maxPatientCapacity} thân chủ`;
    const pkg = this.packages().find(p => p.id === sub.servicePackageId);
    return pkg?.maxPatientCapacity ? `${pkg.maxPatientCapacity} thân chủ` : 'Không giới hạn';
  }

  getPackageDailySlots(sub: IDoctorSubscription | null): string {
    if (!sub) return 'Không giới hạn';
    if (sub.maxDailySlotsCapacity) return `${sub.maxDailySlotsCapacity} slot / ngày`;
    const pkg = this.packages().find(p => p.id === sub.servicePackageId);
    return pkg?.maxDailySlotsCapacity ? `${pkg.maxDailySlotsCapacity} slot / ngày` : 'Không giới hạn';
  }

  processVnPayRedirect(): void {
    const txn = this.activeTxn();
    if (!txn) return;

    this.isRedirectingVnPay.set(true);
    // Explicitly configure returnUrl pointing back to /doctor/subscriptions
    const returnUrl = `${window.location.origin}/doctor/subscriptions`;

    this.paymentService.createVnPayUrl(txn.transactionCode, returnUrl).subscribe({
      next: res => {
        this.isRedirectingVnPay.set(false);
        if (res.paymentUrl) {
          // Direct browser navigation to VNPay Sandbox Gateway
          window.location.href = res.paymentUrl;
        }
      },
      error: () => {
        this.isRedirectingVnPay.set(false);
      },
    });
  }

  dismissCallbackAlert(): void {
    this.vnpayCallbackResult.set(null);
  }
}
