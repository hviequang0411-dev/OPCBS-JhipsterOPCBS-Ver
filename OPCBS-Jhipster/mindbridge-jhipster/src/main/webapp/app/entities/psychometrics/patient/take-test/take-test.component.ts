import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PsychometricsService } from '../../service/psychometrics.service';
import { IPsychometricTest, IPsychometricQuestion } from '../../psychometrics.model';

@Component({
  selector: 'jhi-take-test',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './take-test.component.html',
  styleUrls: ['./take-test.component.scss']
})
export class TakeTestComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private psychometricsService = inject(PsychometricsService);
  private cdr = inject(ChangeDetectorRef);

  test: IPsychometricTest | null = null;
  questions: IPsychometricQuestion[] = [];
  answers: { [questionId: number]: number } = {};
  isLoading = false;
  isSubmitting = false;
  errorMessage = '';

  // Standard Likert 0-3 scale options for DASS-21, PHQ-9, GAD-7
  scaleOptions = [
    { value: 0, label: '0 - Không bao giờ / Không đúng với tôi' },
    { value: 1, label: '1 - Thỉnh thoảng / Đúng một phần' },
    { value: 2, label: '2 - Khá thường xuyên / Đúng nhiều' },
    { value: 3, label: '3 - Hầu như luôn luôn / Hoàn toàn đúng' }
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadTest(+id);
    }
  }

  loadTest(id: number): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.psychometricsService.getTestWithQuestions(id).subscribe({
      next: res => {
        this.test = res;
        this.questions = res.questions || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Không thể tải thông tin bài trắc nghiệm!';
        this.cdr.markForCheck();
      }
    });
  }

  get answeredCount(): number {
    return Object.keys(this.answers).length;
  }

  get progressPercentage(): number {
    if (this.questions.length === 0) return 0;
    return Math.round((this.answeredCount / this.questions.length) * 100);
  }

  selectScore(questionId: number, score: number): void {
    this.answers[questionId] = score;
  }

  submit(): void {
    if (this.answeredCount < this.questions.length) {
      this.errorMessage = `Bạn mới trả lời ${this.answeredCount}/${this.questions.length} câu hỏi. Vui lòng hoàn thành toàn bộ câu hỏi trước khi nộp bài!`;
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    if (!this.test?.id) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const payload = {
      answers: this.questions.map(q => ({
        questionId: q.id!,
        score: this.answers[q.id!] !== undefined ? this.answers[q.id!] : 0
      }))
    };

    this.psychometricsService.submitTest(this.test.id, payload).subscribe({
      next: res => {
        this.isSubmitting = false;
        this.router.navigate(['/patient/psychometrics/result', res.id]);
      },
      error: err => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi nộp bài trắc nghiệm!';
      }
    });
  }
}
