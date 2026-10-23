import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminPsychometricsService } from './psychometrics.service';
import { PsychometricTest, PsychometricsStats } from './psychometrics.model';

@Component({
  selector: 'jhi-admin-psychometrics-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-psychometrics-list.component.html',
  styleUrl: './admin-psychometrics-list.component.scss',
})
export default class AdminPsychometricsListComponent implements OnInit {
  private readonly psychometricsService = inject(AdminPsychometricsService);

  readonly tests = signal<PsychometricTest[]>([]);
  readonly filteredTests = signal<PsychometricTest[]>([]);
  readonly stats = signal<PsychometricsStats>({
    totalTests: 0,
    activeTests: 0,
    inactiveTests: 0,
    totalQuestions: 0,
    totalSubmissions: 0,
  });

  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  searchTerm = '';
  typeFilter = 'ALL'; // 'ALL', 'DASS21', 'GAD7', 'PHQ9', 'CUSTOM'

  testToDelete: PsychometricTest | null = null;
  isDeleting = false;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.psychometricsService.getAllTests().subscribe({
      next: (data: PsychometricTest[]) => {
        this.tests.set(data);
        this.applyFilter();
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách đề trắc nghiệm tâm lý.');
        this.isLoading.set(false);
      },
    });

    this.psychometricsService.getStats().subscribe({
      next: (s: PsychometricsStats) => {
        this.stats.set(s);
      },
    });
  }

  applyFilter(): void {
    let result = [...this.tests()];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      result = result.filter(
        t =>
          (t.title && t.title.toLowerCase().includes(term)) ||
          (t.testType && t.testType.toLowerCase().includes(term)) ||
          (t.category && t.category.toLowerCase().includes(term)) ||
          (t.description && t.description.toLowerCase().includes(term))
      );
    }

    if (this.typeFilter !== 'ALL') {
      result = result.filter(t => (t.testType || '').toUpperCase().includes(this.typeFilter.toUpperCase()));
    }

    this.filteredTests.set(result);
  }

  onSearchChange(): void {
    this.applyFilter();
  }

  onTypeFilterChange(type: string): void {
    this.typeFilter = type;
    this.applyFilter();
  }

  toggleActive(test: PsychometricTest): void {
    if (!test.id) return;
    this.psychometricsService.toggleActive(test.id).subscribe({
      next: (updated: PsychometricTest) => {
        test.isActive = updated.isActive;
        this.successMessage.set(`Đã ${test.isActive ? 'kích hoạt' : 'tạm dừng'} đề "${test.title}" thành công.`);
        setTimeout(() => this.successMessage.set(null), 3500);
        this.loadData();
      },
      error: () => {
        this.errorMessage.set('Không thể cập nhật trạng thái đề thi.');
        setTimeout(() => this.errorMessage.set(null), 3500);
      },
    });
  }

  confirmDelete(test: PsychometricTest): void {
    this.testToDelete = test;
  }

  cancelDelete(): void {
    this.testToDelete = null;
  }

  executeDelete(): void {
    if (!this.testToDelete?.id) return;
    this.isDeleting = true;

    this.psychometricsService.deleteTest(this.testToDelete.id).subscribe({
      next: () => {
        this.isDeleting = false;
        this.successMessage.set(`Đã xóa đề trắc nghiệm "${this.testToDelete?.title}" thành công.`);
        this.testToDelete = null;
        setTimeout(() => this.successMessage.set(null), 3500);
        this.loadData();
      },
      error: () => {
        this.isDeleting = false;
        this.errorMessage.set('Không thể xóa đề trắc nghiệm này do đã có dữ liệu làm bài của bệnh nhân.');
        this.testToDelete = null;
        setTimeout(() => this.errorMessage.set(null), 4000);
      },
    });
  }

  getQuestionCount(test: PsychometricTest): number {
    return test.questions ? test.questions.length : 0;
  }
}
