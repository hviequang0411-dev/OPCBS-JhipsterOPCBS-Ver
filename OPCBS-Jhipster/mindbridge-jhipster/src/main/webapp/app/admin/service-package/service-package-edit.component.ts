import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminServicePackageService } from './service-package.service';
import { ServicePackage } from './service-package.model';

@Component({
  selector: 'jhi-service-package-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './service-package-edit.component.html',
  styleUrl: './service-package-edit.component.scss',
})
export default class ServicePackageEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly packageService = inject(AdminServicePackageService);

  isEditMode = false;
  packageId: number | null = null;

  pkg: ServicePackage = {
    name: '',
    description: '',
    price: 0,
    durationDays: 30,
    maxDailySlotsCapacity: undefined,
    featuresList: '',
    isActive: true,
    displayOrder: 1,
  };

  readonly isSaving = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.packageId = Number(idParam);
      this.isEditMode = true;
      this.loadPackage(this.packageId);
    }
  }

  loadPackage(id: number): void {
    this.isLoading.set(true);
    this.packageService.getPackage(id).subscribe({
      next: (data: ServicePackage) => {
        this.pkg = { ...data };
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải thông tin gói dịch vụ. Vui lòng kiểm tra lại ID.');
        this.isLoading.set(false);
      },
    });
  }

  save(): void {
    if (!this.pkg.name?.trim()) {
      this.errorMessage.set('Vui lòng nhập tên gói dịch vụ.');
      return;
    }
    if (this.pkg.price === undefined || this.pkg.price < 0) {
      this.errorMessage.set('Giá tiền không hợp lệ.');
      return;
    }
    if (!this.pkg.durationDays || this.pkg.durationDays <= 0) {
      this.errorMessage.set('Thời hạn gói phải lớn hơn 0 ngày.');
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    if (this.isEditMode && this.packageId) {
      this.packageService.updatePackage(this.packageId, this.pkg).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.router.navigate(['/admin/service-package']);
        },
        error: (err: any) => {
          this.isSaving.set(false);
          this.errorMessage.set(err?.error?.message || 'Lỗi khi cập nhật gói dịch vụ.');
        },
      });
    } else {
      this.packageService.createPackage(this.pkg).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.router.navigate(['/admin/service-package']);
        },
        error: (err: any) => {
          this.isSaving.set(false);
          this.errorMessage.set(err?.error?.message || 'Lỗi khi tạo gói dịch vụ mới.');
        },
      });
    }
  }

  getFeatureArray(): string[] {
    if (!this.pkg.featuresList) return [];
    return this.pkg.featuresList
      .split('\n')
      .map(f => f.trim())
      .filter(f => f.length > 0);
  }

  formatPrice(price?: number): string {
    if (price === undefined || price === null) return '0 ₫';
    return price.toLocaleString('vi-VN') + ' ₫';
  }
}
