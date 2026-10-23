import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

export interface IDoctorDashboard {
  doctorId?: number;
  doctorName?: string;
  professionalTitle?: string;
  avatarUrl?: string;
  phone?: string;
  email?: string;
  isVerified?: boolean;
  verificationStatus?: string;
  greeting?: string;
  period?: string;
  periodLabel?: string;

  todaySessionsCount: number;
  todayCompletedCount: number;
  todayUpcomingCount: number;
  upcomingSessionsCount: number;

  appointmentsThisMonth: number;
  appointmentsGrowthPercent?: number | null;
  isAppointmentsGrowthPositive: boolean;

  newPatientsThisMonth: number;
  activePatientsCount: number;
  returningPatientsCount: number;
  returningPatientRate: number;

  revenueThisMonth: number;
  revenueGrowthPercent?: number | null;
  isRevenueGrowthPositive: boolean;
  revenueNote?: string;
  retentionNote?: string;

  pendingApprovalsCount: number;
  pendingNotesCount: number;
  inProgressAppointmentsCount: number;

  todaySessions: Array<{
    id: number;
    bookingCode: string;
    patientName: string;
    patientPhone: string;
    timeRange: string;
    consultationMode: string;
    status: string;
    reason?: string;
  }>;

  upcomingAppointments: Array<{
    id: number;
    bookingCode: string;
    patientName: string;
    patientPhone: string;
    timeRange: string;
    consultationMode: string;
    status: string;
  }>;

  weeklyChartData: Array<{
    dayOfWeek: string;
    sessionsCount: number;
    revenue: number;
  }>;

  singleVisitPercent: number;
  repeat2To3Percent: number;
  loyal4PlusPercent: number;

  cacheHit: boolean;
  cacheKey?: string;
  cachedAt?: string;
  executionTimeMs: number;
}

@Component({
  selector: 'jhi-doctor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './doctor-dashboard.component.html',
  styleUrls: ['./doctor-dashboard.component.scss'],
})
export class DoctorDashboardComponent implements OnInit {
  private http = inject(HttpClient);

  dashboard = signal<IDoctorDashboard | null>(null);
  isLoading = signal<boolean>(true);
  isNotFound = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  selectedPeriod = signal<string>('today');
  isEvicting = signal<boolean>(false);
  alertMessage = signal<string | null>(null);

  readonly hoveredBar = signal<{ dayOfWeek: string; sessionsCount: number; revenue: number } | null>(null);

  ngOnInit(): void {
    this.loadDashboard('today');
  }

  loadDashboard(period: string): void {
    this.selectedPeriod.set(period);
    this.isLoading.set(true);
    this.isNotFound.set(false);
    this.errorMessage.set(null);

    this.http.get<IDoctorDashboard>(`/api/doctor/dashboard/overview?period=${period}`).subscribe({
      next: data => {
        this.dashboard.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Lỗi khi tải Doctor Dashboard:', err);
        this.isLoading.set(false);
        if (err.status === 404) {
          this.isNotFound.set(true);
          this.errorMessage.set(err.error?.message || 'Không tìm thấy hồ sơ bác sĩ cho tài khoản hiện tại.');
        } else {
          this.errorMessage.set('Không thể kết nối đến máy chủ để tải dữ liệu lịch hẹn của bạn. Vui lòng thử lại sau.');
        }
      },
    });
  }

  evictCache(): void {
    this.isEvicting.set(true);
    this.http.post<any>('/api/doctor/dashboard/cache/evict', {}).subscribe({
      next: () => {
        this.isEvicting.set(false);
        this.alertMessage.set('⚡ Đã làm mới Redis Cache thành công! Số liệu được tổng hợp trực tiếp từ cơ sở dữ liệu.');
        setTimeout(() => this.alertMessage.set(null), 4000);
        this.loadDashboard(this.selectedPeriod());
      },
      error: () => {
        this.isEvicting.set(false);
        this.alertMessage.set('⚠️ Không thể xóa cache hoặc Redis đang bận. Đang làm mới dữ liệu...');
        setTimeout(() => this.alertMessage.set(null), 3000);
        this.loadDashboard(this.selectedPeriod());
      },
    });
  }

  formatCurrency(value: number | undefined): string {
    if (!value) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  getMaxWeeklySessions(): number {
    const data = this.dashboard()?.weeklyChartData;
    if (!data || data.length === 0) return 5;
    const max = Math.max(...data.map(d => d.sessionsCount));
    return max > 0 ? max : 5;
  }

  getBarHeightPercent(count: number): number {
    const max = this.getMaxWeeklySessions();
    if (count === 0) return 6; // Độ cao tối thiểu hiển thị thanh mờ
    return Math.max(12, Math.round((count / max) * 100));
  }

  hasWeeklyData(): boolean {
    const data = this.dashboard()?.weeklyChartData;
    return !!data && data.some(d => d.sessionsCount > 0);
  }

  onBarHover(point: { dayOfWeek: string; sessionsCount: number; revenue: number }): void {
    this.hoveredBar.set(point);
  }

  onBarLeave(): void {
    this.hoveredBar.set(null);
  }
}
