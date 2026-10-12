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

  todaySessionsCount: number;
  todayCompletedCount: number;
  todayUpcomingCount: number;
  upcomingSessionsCount: number;

  appointmentsThisMonth: number;
  appointmentsGrowthPercent: number;
  isAppointmentsGrowthPositive: boolean;

  newPatientsThisMonth: number;
  activePatientsCount: number;
  returningPatientsCount: number;
  returningPatientRate: number;

  revenueThisMonth: number;
  revenueGrowthPercent: number;
  isRevenueGrowthPositive: boolean;

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
  selectedPeriod = signal<string>('today');
  isEvicting = signal<boolean>(false);
  alertMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDashboard('today');
  }

  loadDashboard(period: string): void {
    this.selectedPeriod.set(period);
    this.isLoading.set(true);
    this.http.get<IDoctorDashboard>(`/api/doctor/dashboard/overview?period=${period}`).subscribe({
      next: data => {
        this.dashboard.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading doctor dashboard:', err);
        this.isLoading.set(false);
      },
    });
  }

  evictCache(): void {
    this.isEvicting.set(true);
    this.http.post<any>('/api/doctor/dashboard/cache/evict', {}).subscribe({
      next: res => {
        this.isEvicting.set(false);
        this.alertMessage.set('⚡ Đã xóa Redis Cache thành công! Lần tải tiếp theo sẽ đọc trực tiếp từ MySQL.');
        setTimeout(() => this.alertMessage.set(null), 4000);
        this.loadDashboard(this.selectedPeriod());
      },
      error: () => {
        this.isEvicting.set(false);
      },
    });
  }

  formatCurrency(value: number | undefined): string {
    if (!value) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }
}
