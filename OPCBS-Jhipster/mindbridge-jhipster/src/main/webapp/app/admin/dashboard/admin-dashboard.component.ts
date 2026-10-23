import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminDashboardService, DashboardTotalView } from './admin-dashboard.service';

@Component({
  selector: 'jhi-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export default class AdminDashboardComponent implements OnInit {
  private readonly dashboardService = inject(AdminDashboardService);

  readonly data = signal<DashboardTotalView>({
    totalRevenue: 0,
    unconfirmedRevenue: 0,
    activeSubscriptions: 0,
    totalDoctors: 0,
    totalSpecializations: 0,
    totalAppointments: 0,
    totalTests: 0,
    totalSubmissions: 0,
    specializationBreakdown: [],
    monthlyRevenueTrend: [],
    appointmentStatusBreakdown: [],
    recentPackages: [],
    topSpecializations: [],
    cacheHit: false,
    cacheKey: 'dashboard:admin:overview',
    executionTimeMs: 0,
  });

  readonly isLoading = signal<boolean>(true);
  readonly isEvicting = signal<boolean>(false);
  readonly alertMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly hoveredPoint = signal<{ month: string; revenue: number; subscriptions: number; x: number; y: number } | null>(null);
  readonly hoveredSpec = signal<{ name: string; doctorCount: number; percentage: number; color: string } | null>(null);
  readonly hoveredStatus = signal<{ status: string; label: string; count: number; percentage: number; color: string } | null>(null);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.dashboardService.getDashboardMetrics().subscribe({
      next: (metrics: DashboardTotalView) => {
        this.data.set(metrics);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Lỗi khi tải số liệu Admin Dashboard:', err);
        this.errorMessage.set('Không thể tải dữ liệu Dashboard từ máy chủ. Vui lòng thử lại sau.');
        this.isLoading.set(false);
      },
    });
  }

  evictCache(): void {
    this.isEvicting.set(true);
    this.dashboardService.evictDashboardCache().subscribe({
      next: res => {
        this.isEvicting.set(false);
        if (res && res.success) {
          this.alertMessage.set('⚡ ' + (res.message || 'Đã làm mới Redis Cache thành công! Lần tải tiếp theo sẽ đồng bộ trực tiếp từ Database.'));
        } else {
          this.alertMessage.set('⚠️ ' + (res?.message || 'Khóa cache không tồn tại hoặc Redis tạm thời không phản hồi.'));
        }
        setTimeout(() => this.alertMessage.set(null), 4000);
        this.loadData();
      },
      error: () => {
        this.isEvicting.set(false);
        this.alertMessage.set('⚠️ Không thể kết nối với dịch vụ cache. Đang tải lại dữ liệu trực tiếp từ Database...');
        setTimeout(() => this.alertMessage.set(null), 4000);
        this.loadData();
      },
    });
  }

  formatVnd(amount: number | undefined): string {
    if (!amount) return '0 ₫';
    return amount.toLocaleString('vi-VN') + ' ₫';
  }

  formatShortVnd(amount: number): string {
    if (!amount || amount === 0) return '0 ₫';
    if (amount >= 1000000000) {
      return (amount / 1000000000).toFixed(1) + ' tỷ';
    }
    if (amount >= 1000000) {
      return (amount / 1000000).toFixed(1) + ' tr';
    }
    if (amount >= 1000) {
      return (amount / 1000).toFixed(0) + ' k';
    }
    return amount.toLocaleString('vi-VN') + ' ₫';
  }

  hasRevenueData(): boolean {
    const trend = this.data().monthlyRevenueTrend;
    return !!trend && trend.some(t => t.revenue > 0 || t.subscriptions > 0);
  }

  hasSpecializationData(): boolean {
    const specs = this.data().specializationBreakdown;
    return !!specs && specs.length > 0 && specs.some(s => s.doctorCount > 0);
  }

  hasAppointmentData(): boolean {
    const appts = this.data().appointmentStatusBreakdown;
    return !!appts && appts.length > 0 && appts.some(a => a.count > 0);
  }

  getMaxRevenue(): number {
    const trend = this.data().monthlyRevenueTrend;
    if (!trend || trend.length === 0) return 1000000;
    const max = Math.max(...trend.map(t => t.revenue));
    return max > 0 ? max * 1.15 : 1000000;
  }

  // Helpers for SVG Area & Bezier Line Chart
  getChartPoints(): Array<{ month: string; revenue: number; subscriptions: number; x: number; y: number }> {
    const trend = this.data().monthlyRevenueTrend;
    if (!trend || trend.length === 0) return [];

    const maxRev = this.getMaxRevenue();
    const width = 600;
    const height = 180;
    const paddingX = 45;
    const paddingY = 25;

    const usableWidth = width - paddingX * 2;
    const usableHeight = height - paddingY * 2;
    const stepX = trend.length > 1 ? usableWidth / (trend.length - 1) : usableWidth;

    return trend.map((item, idx) => {
      const x = paddingX + idx * stepX;
      const normalizedY = (item.revenue / maxRev) * usableHeight;
      const y = height - paddingY - normalizedY;
      return {
        month: item.month,
        revenue: item.revenue,
        subscriptions: item.subscriptions,
        x,
        y,
      };
    });
  }

  getChartPathD(): string {
    const pts = this.getChartPoints();
    if (pts.length === 0) return '';
    if (pts.length === 1) return `M ${pts[0].x} ${pts[0].y}`;

    let d = `M ${pts[0].x} ${pts[0].y}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const current = pts[i];
      const next = pts[i + 1];
      const controlX = (current.x + next.x) / 2;
      d += ` C ${controlX} ${current.y}, ${controlX} ${next.y}, ${next.x} ${next.y}`;
    }
    return d;
  }

  getChartAreaD(): string {
    const pts = this.getChartPoints();
    if (pts.length === 0) return '';
    const linePath = this.getChartPathD();
    const last = pts[pts.length - 1];
    const first = pts[0];
    const bottomY = 155;
    return `${linePath} L ${last.x} ${bottomY} L ${first.x} ${bottomY} Z`;
  }

  onPointHover(pt: { month: string; revenue: number; subscriptions: number; x: number; y: number }): void {
    this.hoveredPoint.set(pt);
  }

  onPointLeave(): void {
    this.hoveredPoint.set(null);
  }

  onSpecHover(spec: { name: string; doctorCount: number; percentage: number; color: string } | null): void {
    this.hoveredSpec.set(spec);
  }

  onStatusHover(st: { status: string; label: string; count: number; percentage: number; color: string } | null): void {
    this.hoveredStatus.set(st);
  }

  // Helpers for SVG Donut Diagram
  getDonutSegments(): Array<{
    name: string;
    doctorCount: number;
    percentage: number;
    color: string;
    strokeDasharray: string;
    strokeDashoffset: number;
  }> {
    const specs = this.data().specializationBreakdown;
    if (!specs || specs.length === 0) return [];

    const circumference = 2 * Math.PI * 70; // radius = 70 => ~439.82
    let accumulatedPercent = 0;

    return specs.map(item => {
      const segLength = (item.percentage / 100) * circumference;
      const dashoffset = -((accumulatedPercent / 100) * circumference);
      accumulatedPercent += item.percentage;

      return {
        name: item.name,
        doctorCount: item.doctorCount,
        percentage: item.percentage,
        color: item.color,
        strokeDasharray: `${segLength} ${circumference - segLength}`,
        strokeDashoffset: dashoffset,
      };
    });
  }
}
