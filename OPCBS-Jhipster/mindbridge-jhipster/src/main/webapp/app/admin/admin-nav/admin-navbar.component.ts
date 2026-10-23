import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'jhi-admin-navbar',
  standalone: true,
  templateUrl: './admin-navbar.component.html',
  styleUrl: './admin-navbar.component.scss',
  imports: [CommonModule, RouterLink, RouterLinkActive],
})
export default class AdminNavbarComponent {
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
    if (url.includes('/admin/dashboard')) {
      return 'Tổng Quan Quản Trị & Hiệu Suất Kinh Doanh';
    }
    if (url.includes('/admin/service-package')) {
      if (url.includes('/new')) return 'Tạo Mới Gói Dịch Vụ Nền Tảng';
      if (url.includes('/edit')) return 'Chỉnh Sửa Gói Dịch Vụ Nền Tảng';
      if (url.includes('/view')) return 'Chi Tiết Gói Dịch Vụ Nền Tảng';
      return 'Quản Lý Gói Dịch Vụ Nền Tảng (Service Packages)';
    }
    if (url.includes('/admin/psychometrics')) {
      if (url.includes('/new')) return 'Tạo / Nhập Đề Trắc Nghiệm Tâm Lý';
      if (url.includes('/edit')) return 'Chỉnh Sửa Đề Trắc Nghiệm Tâm Lý';
      if (url.includes('/view')) return 'Chi Tiết Bộ Đề Khảo Sát Tâm Lý';
      return 'Ngân Hàng Đề Trắc Nghiệm Tâm Lý (Psychometrics)';
    }
    if (url.includes('/admin/health')) return 'Giám Sát Trạng Thái Hệ Thống';
    if (url.includes('/admin/metrics')) return 'Chỉ Số Hiệu Năng Ứng Dụng';
    if (url.includes('/admin/configuration')) return 'Cấu Hình Hệ Thống';
    if (url.includes('/user-management')) return 'Quản Lý Tài Khoản Người Dùng';
    if (url.includes('/doctor-profile')) return 'Quản Lý Hồ Sơ Chuyên Gia Bác Sĩ';
    return 'Trung Tâm Chỉ Huy Quản Trị Hệ Thống';
  }

  getModuleIcon(): string {
    const url = (this.currentUrl || '').toLowerCase();
    if (url.includes('/admin/dashboard')) return 'bi bi-speedometer2 text-teal';
    if (url.includes('/admin/service-package')) return 'bi bi-gem text-teal';
    if (url.includes('/admin/psychometrics')) return 'bi bi-clipboard2-pulse text-teal';
    if (url.includes('/user-management')) return 'bi bi-people text-teal';
    if (url.includes('/doctor-profile')) return 'bi bi-person-badge text-teal';
    if (url.includes('/admin/health')) return 'bi bi-activity text-teal';
    return 'bi bi-briefcase text-teal';
  }
}
