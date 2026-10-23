import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { ServicePackage, ServicePackageStats } from './service-package.model';

@Injectable({ providedIn: 'root' })
export class AdminServicePackageService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = `${serverApiUrl}api/admin/service-packages`;
  private readonly publicUrl = `${serverApiUrl}api/service-packages`;

  getAllPackages(): Observable<ServicePackage[]> {
    return this.http.get<ServicePackage[]>(this.resourceUrl).pipe(
      catchError(() => this.http.get<ServicePackage[]>(this.publicUrl).pipe(catchError(() => of([]))))
    );
  }

  getPackageStats(): Observable<ServicePackageStats> {
    return this.http.get<ServicePackageStats>(`${this.resourceUrl}/stats`).pipe(
      catchError(() =>
        of({
          totalPackages: 0,
          activePackages: 0,
          inactivePackages: 0,
          averagePrice: 0,
        })
      )
    );
  }

  getPackage(id: number): Observable<ServicePackage> {
    return this.http.get<ServicePackage>(`${this.publicUrl}/${id}`);
  }

  createPackage(pkg: ServicePackage): Observable<ServicePackage> {
    return this.http.post<ServicePackage>(this.resourceUrl, pkg);
  }

  updatePackage(id: number, pkg: ServicePackage): Observable<ServicePackage> {
    return this.http.put<ServicePackage>(`${this.resourceUrl}/${id}`, pkg);
  }

  toggleActive(id: number): Observable<ServicePackage> {
    return this.http.patch<ServicePackage>(`${this.resourceUrl}/${id}/toggle-active`, {});
  }

  deletePackage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }
}
