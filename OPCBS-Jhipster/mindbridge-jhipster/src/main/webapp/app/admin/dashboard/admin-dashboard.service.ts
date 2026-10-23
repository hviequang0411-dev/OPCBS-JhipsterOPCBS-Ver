import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of, catchError } from 'rxjs';
import { serverApiUrl } from 'app/config';

export interface DashboardTotalView {
  totalRevenue: number;
  unconfirmedRevenue: number;
  activeSubscriptions: number;
  totalDoctors: number;
  totalSpecializations: number;
  totalAppointments: number;
  totalTests: number;
  totalSubmissions: number;
  specializationBreakdown: Array<{
    name: string;
    doctorCount: number;
    percentage: number;
    color: string;
  }>;
  monthlyRevenueTrend: Array<{
    month: string;
    revenue: number;
    subscriptions: number;
  }>;
  appointmentStatusBreakdown: Array<{
    status: string;
    count: number;
    label: string;
    color: string;
    percentage: number;
  }>;
  recentPackages: Array<{
    id: number;
    name: string;
    price: number;
    durationDays: number;
    isActive: boolean;
  }>;
  topSpecializations: Array<{
    name: string;
    doctorCount: number;
    percentage: number;
  }>;
  // Redis Cache Metadata
  cacheHit?: boolean;
  cacheKey?: string;
  cachedAt?: string;
  executionTimeMs?: number;
  revenueNote?: string;
  dataFreshnessNote?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  private readonly http = inject(HttpClient);

  getDashboardMetrics(): Observable<DashboardTotalView> {
    const overviewUrl = `${serverApiUrl}api/admin/dashboard/overview`;

    return this.http.get<any>(overviewUrl).pipe(
      map(res => ({
        totalRevenue: res?.totalRevenue ?? 0,
        unconfirmedRevenue: res?.unconfirmedRevenue ?? 0,
        activeSubscriptions: res?.activeSubscriptions ?? 0,
        totalDoctors: res?.totalDoctors ?? 0,
        totalSpecializations: res?.totalSpecializations ?? 0,
        totalAppointments: res?.totalAppointments ?? 0,
        totalTests: res?.totalTests ?? 0,
        totalSubmissions: res?.totalSubmissions ?? 0,
        specializationBreakdown: res?.specializationBreakdown ?? [],
        monthlyRevenueTrend: res?.monthlyRevenueTrend ?? [],
        appointmentStatusBreakdown: res?.appointmentStatusBreakdown ?? [],
        recentPackages: res?.recentPackages ?? [],
        topSpecializations: (res?.specializationBreakdown ?? []).slice(0, 5),
        cacheHit: res?.cacheHit ?? false,
        cacheKey: res?.cacheKey ?? 'dashboard:admin:overview',
        cachedAt: res?.cachedAt,
        executionTimeMs: res?.executionTimeMs ?? 0,
        revenueNote: res?.revenueNote,
        dataFreshnessNote: res?.dataFreshnessNote,
      }))
    );
  }

  /**
   * Yêu cầu backend xóa Redis Cache của Admin Dashboard
   */
  evictDashboardCache(): Observable<{ success: boolean; message: string; cacheKey?: string }> {
    const evictUrl = `${serverApiUrl}api/admin/dashboard/cache/evict`;
    return this.http.post<{ success: boolean; message: string; cacheKey?: string }>(evictUrl, {});
  }
}

