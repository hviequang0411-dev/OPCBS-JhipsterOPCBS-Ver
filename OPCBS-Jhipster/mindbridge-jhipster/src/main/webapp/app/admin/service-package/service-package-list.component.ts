import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminServicePackageService } from './service-package.service';
import { ServicePackage, ServicePackageStats } from './service-package.model';

@Component({
  selector: 'jhi-service-package-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './service-package-list.component.html',
  styleUrl: './service-package-list.component.scss',
})
export default class ServicePackageListComponent implements OnInit {
  private readonly packageService = inject(AdminServicePackageService);

  readonly packages = signal<ServicePackage[]>([]);
  readonly filteredPackages = signal<ServicePackage[]>([]);
  readonly stats = signal<ServicePackageStats>({
    totalPackages: 0,
    activePackages: 0,
    inactivePackages: 0,
    averagePrice: 0,
  });

  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  searchTerm = '';
  statusFilter = 'ALL'; // 'ALL', 'ACTIVE', 'INACTIVE'

  packageToDelete: ServicePackage | null = null;
  isDeleting = false;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.packageService.getAllPackages().subscribe({
      next: (data: ServicePackage[]) => {
        this.packages.set(data);
        this.applyFilter();
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách gói dịch vụ. Vui lòng thử lại sau.');
        this.isLoading.set(false);
      },
    });

    this.packageService.getPackageStats().subscribe({
      next: (s: ServicePackageStats) => {
        this.stats.set(s);
      },
    });
  }

  applyFilter(): void {
    let result = [...this.packages()];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      result = result.filter(
        p => (p.name && p.name.toLowerCase().includes(term)) || (p.description && p.description.toLowerCase().includes(term))
      );
    }

    if (this.statusFilter === 'ACTIVE') {
      result = result.filter(p => p.isActive === true);
    } else if (this.statusFilter === 'INACTIVE') {
      result = result.filter(p => p.isActive === false);
    }

    this.filteredPackages.set(result);
  }

  onSearchChange(): void {
    this.applyFilter();
  }

  onStatusFilterChange(status: string): void {
    this.statusFilter = status;
    this.applyFilter();
  }

  toggleActive(pkg: ServicePackage): void {
    if (!pkg.id) return;
    this.packageService.toggleActive(pkg.id).subscribe({
      next: (updated: ServicePackage) => {
        pkg.isActive = updated.isActive;
        this.successMessage.set(`Đã ${pkg.isActive ? 'kích hoạt' : 'tạm dừng'} gói "${pkg.name}" thành công.`);
        setTimeout(() => this.successMessage.set(null), 3500);
        this.loadData();
      },
      error: () => {
        this.errorMessage.set('Không thể cập nhật trạng thái gói cước.');
        setTimeout(() => this.errorMessage.set(null), 3500);
      },
    });
  }

  confirmDelete(pkg: ServicePackage): void {
    this.packageToDelete = pkg;
  }

  cancelDelete(): void {
    this.packageToDelete = null;
  }

  executeDelete(): void {
    if (!this.packageToDelete?.id) return;
    this.isDeleting = true;

    this.packageService.deletePackage(this.packageToDelete.id).subscribe({
      next: () => {
        this.isDeleting = false;
        this.successMessage.set(`Đã xóa gói dịch vụ "${this.packageToDelete?.name}" thành công.`);
        this.packageToDelete = null;
        setTimeout(() => this.successMessage.set(null), 3500);
        this.loadData();
      },
      error: () => {
        this.isDeleting = false;
        this.errorMessage.set('Không thể xóa gói cước. Gói có thể đang được bác sĩ đăng ký.');
        this.packageToDelete = null;
        setTimeout(() => this.errorMessage.set(null), 4000);
      },
    });
  }

  formatPrice(price?: number): string {
    if (price === undefined || price === null) return '0 ₫';
    return price.toLocaleString('vi-VN') + ' ₫';
  }
}
