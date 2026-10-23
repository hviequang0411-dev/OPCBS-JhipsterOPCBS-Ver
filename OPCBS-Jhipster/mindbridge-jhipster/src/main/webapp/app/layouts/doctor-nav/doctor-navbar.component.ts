import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'jhi-doctor-navbar',
  standalone: true,
  templateUrl: './doctor-navbar.component.html',
  styleUrl: './doctor-navbar.component.scss',
  imports: [CommonModule],
})
export default class DoctorNavbarComponent {
  private readonly router = inject(Router);
  currentUrl = '';

  constructor() {
    this.currentUrl = this.router.url;
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: any) => {
      this.currentUrl = event.urlAfterRedirects || event.url || '';
    });
  }

  getModuleTitle(): string {
    const url = (this.currentUrl || '').toLowerCase();
    if (url.includes('/doctor/dashboard')) {
      return 'Tổng Quan Lâm Sàng & Hiệu Suất Phòng Khám';
    }
    if (url.includes('/doctor/treatment-packages')) {
      return 'Quản Lý Phác Đồ Trị Liệu & Gói Can Thiệp';
    }
    if (url.includes('/doctor/psychometrics')) {
      return 'Ngân Hàng Trắc Nghiệm Tâm Lý & Báo Cáo Sàng Lọc';
    }
    if (url.includes('/doctor/subscriptions') || url.includes('/doctor/subscription')) {
      return 'Gói Hội Viên Bác Sĩ & Cổng Thanh Toán VNPay';
    }
    if (url.includes('/doctor/patients')) {
      return 'Danh Sách & Hồ Sơ Bệnh Nhân Lâm Sàng';
    }
    if (url.includes('/doctor/verification')) {
      return 'Chứng Chỉ Hành Nghề & Xác Minh Y Khoa';
    }
    if (url.includes('/appointment')) {
      return 'Quản Lý Ca Khám & Lịch Hẹn Trị Liệu';
    }
    if (url.includes('/availability-slot')) {
      return 'Thiết Lập Khung Giờ & Lịch Trực Phòng Khám';
    }
    return 'Cổng Y Tế Lâm Sàng Bác Sĩ';
  }

  getModuleIcon(): string {
    const url = (this.currentUrl || '').toLowerCase();
    if (url.includes('/doctor/dashboard')) return 'bi bi-speedometer2 text-teal';
    if (url.includes('/doctor/treatment-packages')) return 'bi bi-box-seam text-teal';
    if (url.includes('/doctor/psychometrics')) return 'bi bi-clipboard2-pulse text-teal';
    if (url.includes('/doctor/subscriptions') || url.includes('/doctor/subscription')) return 'bi bi-credit-card text-teal';
    if (url.includes('/doctor/patients')) return 'bi bi-people text-teal';
    if (url.includes('/doctor/verification')) return 'bi bi-shield-check text-teal';
    if (url.includes('/appointment')) return 'bi bi-calendar-check text-teal';
    if (url.includes('/availability-slot')) return 'bi bi-clock-history text-teal';
    return 'bi bi-hospital text-teal';
  }
}
