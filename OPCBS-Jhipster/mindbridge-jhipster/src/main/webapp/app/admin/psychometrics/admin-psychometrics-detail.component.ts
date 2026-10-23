import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminPsychometricsService } from './psychometrics.service';
import { PsychometricTest } from './psychometrics.model';

@Component({
  selector: 'jhi-admin-psychometrics-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-psychometrics-detail.component.html',
})
export default class AdminPsychometricsDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly psychometricsService = inject(AdminPsychometricsService);

  readonly test = signal<PsychometricTest | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadTest(Number(idParam));
    }
  }

  loadTest(id: number): void {
    this.isLoading.set(true);
    this.psychometricsService.getTest(id).subscribe({
      next: (data: PsychometricTest) => {
        this.test.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải bài trắc nghiệm.');
        this.isLoading.set(false);
      },
    });
  }

  getCategoryBadgeClass(category?: string): string {
    switch (category) {
      case 'Depression':
        return 'bg-danger text-white';
      case 'Anxiety':
        return 'bg-warning text-dark';
      case 'Stress':
        return 'bg-info text-dark';
      case 'Sleep':
        return 'bg-purple text-white';
      case 'Somatic':
        return 'bg-orange text-white';
      default:
        return 'bg-secondary text-white';
    }
  }
}
