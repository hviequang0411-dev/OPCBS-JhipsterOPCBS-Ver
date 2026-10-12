import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PsychometricsService } from '../../service/psychometrics.service';
import { IPsychometricSubmission } from '../../psychometrics.model';

@Component({
  selector: 'jhi-test-result',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './test-result.component.html',
  styleUrls: ['./test-result.component.scss']
})
export class TestResultComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private psychometricsService = inject(PsychometricsService);
  private cdr = inject(ChangeDetectorRef);

  submission: IPsychometricSubmission | null = null;
  scoreData: any = {};
  isLoading = false;

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadSubmission(+id);
    }
  }

  loadSubmission(id: number): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.psychometricsService.getSubmissionDetails(id).subscribe({
      next: res => {
        this.submission = res;
        try {
          this.scoreData = res.scoreDataJson ? JSON.parse(res.scoreDataJson) : {};
        } catch {
          this.scoreData = {};
        }
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  getSeverityBadge(level?: string): string {
    switch (level) {
      case 'Bình thường':
      case 'Tối thiểu / Không trầm cảm':
      case 'Tối thiểu / Bình thường':
        return 'badge bg-success';
      case 'Nhẹ':
      case 'Trầm cảm nhẹ':
      case 'Lo âu nhẹ':
        return 'badge bg-info text-dark';
      case 'Vừa':
      case 'Trầm cảm vừa':
      case 'Lo âu trung bình':
        return 'badge bg-warning text-dark';
      case 'Nặng':
      case 'Trầm cảm mức độ nặng vừa':
      case 'Lo âu nặng':
        return 'badge bg-danger';
      case 'Rất nặng':
      case 'Trầm cảm nặng':
        return 'badge bg-danger fw-bold';
      default:
        return 'badge bg-secondary';
    }
  }

  printReport(): void {
    window.print();
  }
}
