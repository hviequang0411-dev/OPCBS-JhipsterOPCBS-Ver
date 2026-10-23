import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminServicePackageService } from './service-package.service';
import { ServicePackage } from './service-package.model';

@Component({
  selector: 'jhi-service-package-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './service-package-detail.component.html',
})
export default class ServicePackageDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly packageService = inject(AdminServicePackageService);

  readonly pkg = signal<ServicePackage | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadPackage(Number(idParam));
    }
  }

  loadPackage(id: number): void {
    this.isLoading.set(true);
    this.packageService.getPackage(id).subscribe({
      next: (data: ServicePackage) => {
        this.pkg.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải thông tin gói dịch vụ.');
        this.isLoading.set(false);
      },
    });
  }

  getFeatureArray(): string[] {
    const features = this.pkg()?.featuresList;
    if (!features) return [];
    return features
      .split('\n')
      .map(f => f.trim())
      .filter(f => f.length > 0);
  }

  formatPrice(price?: number): string {
    if (price === undefined || price === null) return '0 ₫';
    return price.toLocaleString('vi-VN') + ' ₫';
  }
}
