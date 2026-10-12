import { Component, OnInit, inject, signal, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PaymentService } from './payment.service';
import { IServicePackage, IDoctorSubscription } from './payment.model';
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

  packages = signal<IServicePackage[]>([]);
  currentSubscription = signal<IDoctorSubscription | null>(null);
  isLoading = signal<boolean>(false);
  isSubscribing = signal<boolean>(false);
  selectedPackage = signal<IServicePackage | null>(null);
  activeTxn = signal<{
    subscriptionId: number;
    transactionCode: string;
    amount: number;
    packageName: string;
  } | null>(null);
  paymentSuccess = signal<boolean>(false);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.paymentService.getMySubscription().subscribe({
      next: sub => {
        this.currentSubscription.set(sub || null);
      },
      error: () => {
        this.currentSubscription.set(null);
      },
    });

    this.paymentService.getServicePackages().subscribe({
      next: pkgs => {
        if (pkgs && pkgs.length > 0) {
          this.packages.set(pkgs);
        } else {
          // Default fallback presentation packages
          this.packages.set([
            {
              id: 1,
              name: 'Gói Cơ Bản (Standard)',
              description: 'Dành cho Bác sĩ mới tham gia nền tảng MindBridge, tiếp cận bệnh nhân trực tuyến',
              price: 499000,
              durationDays: 30,
              maxAppointmentsPerMonth: 30,
              features: 'Tối đa 30 lịch hẹn/tháng,Hồ sơ khám lâm sàng điện tử,Nhật ký cảm xúc bệnh nhân,Hỗ trợ kỹ thuật 24/7',
              badge: 'Phổ biến',
              isActive: true,
              displayOrder: 1,
            },
            {
              id: 2,
              name: 'Gói Chuyên Nghiệp (Pro Tier)',
              description: 'Dành cho Bác sĩ & Chuyên gia tâm lý có lượng bệnh nhân lớn và điều trị chuyên sâu',
              price: 999000,
              durationDays: 30,
              maxAppointmentsPerMonth: 100,
              features: 'Tối đa 100 lịch hẹn/tháng,Đầy đủ bài trắc nghiệm tâm lý DASS-21/PHQ-9/GAD-7,Quản lý gói trị liệu nhiều buổi,Chat trực tiếp không giới hạn,Ưu tiên hiển thị trang chủ',
              badge: 'Khuyên Dùng',
              isActive: true,
              displayOrder: 2,
            },
            {
              id: 3,
              name: 'Gói Cao Cấp (VIP Enterprise)',
              description: 'Giải pháp toàn diện cho Phòng khám, Viện nghiên cứu và Chuyên gia hàng đầu',
              price: 1999000,
              durationDays: 90,
              maxAppointmentsPerMonth: 999,
              features: 'Không giới hạn số lượt khám,Thời hạn 90 ngày (3 tháng),Tích hợp hội chẩn từ xa,Báo cáo thống kê tiến trình điều trị AI,Chăm sóc khách hàng VIP riêng biệt',
              badge: 'Cao Cấp',
              isActive: true,
              displayOrder: 3,
            },
          ]);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  getFeaturesList(featuresStr?: string): string[] {
    if (!featuresStr) return [];
    return featuresStr.split(',').map(f => f.trim()).filter(f => f.length > 0);
  }

  openCheckoutModal(content: TemplateRef<any>, pkg: IServicePackage): void {
    this.selectedPackage.set(pkg);
    this.paymentSuccess.set(false);
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

  processMockCheckout(modal: any): void {
    const txn = this.activeTxn();
    if (!txn) return;

    this.isSubscribing.set(true);
    this.paymentService.mockCheckout(txn.transactionCode).subscribe({
      next: () => {
        this.isSubscribing.set(false);
        this.paymentSuccess.set(true);
        this.loadData();
      },
      error: () => {
        this.isSubscribing.set(false);
      },
    });
  }

  processVnPayRedirect(): void {
    const txn = this.activeTxn();
    if (!txn) return;

    this.paymentService.createVnPayUrl(txn.transactionCode).subscribe({
      next: res => {
        if (res.paymentUrl) {
          window.open(res.paymentUrl, '_blank');
        }
      },
    });
  }
}
