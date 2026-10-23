import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PsychometricsService } from '../../service/psychometrics.service';
import { IPsychometricTest, IPsychometricSubmission } from '../../psychometrics.model';

@Component({
  selector: 'jhi-psychometrics-catalog',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './psychometrics-catalog.component.html',
  styleUrls: ['./psychometrics-catalog.component.scss']
})
export class PsychometricsCatalogComponent implements OnInit {
  private psychometricsService = inject(PsychometricsService);
  private cdr = inject(ChangeDetectorRef);

  tests: IPsychometricTest[] = [];
  mySubmissions: IPsychometricSubmission[] = [];
  assignedSubmissions: IPsychometricSubmission[] = [];
  completedSubmissions: IPsychometricSubmission[] = [];
  isLoading = false;
  activeTab: 'assigned' | 'catalog' | 'history' = 'catalog';

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.psychometricsService.getAllActiveTests().subscribe({
      next: res => {
        this.tests = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });

    this.psychometricsService.getMySubmissions().subscribe({
      next: res => {
        this.mySubmissions = res || [];
        this.assignedSubmissions = this.mySubmissions.filter(s => s.status === 'ASSIGNED');
        this.completedSubmissions = this.mySubmissions.filter(s => s.status === 'COMPLETED');
        if (this.assignedSubmissions.length > 0) {
          this.activeTab = 'assigned';
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.cdr.markForCheck();
      }
    });
  }

  getBadgeForTest(testType?: string | null): string {
    switch (testType) {
      case 'DASS21':
        return 'badge bg-primary';
      case 'PHQ9':
        return 'badge bg-purple text-white';
      case 'GAD7':
        return 'badge bg-teal text-white';
      default:
        return 'badge bg-secondary';
    }
  }
}
