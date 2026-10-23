import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminPsychometricsService } from './psychometrics.service';
import { PsychometricTest, PsychometricQuestion } from './psychometrics.model';

@Component({
  selector: 'jhi-admin-psychometrics-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-psychometrics-edit.component.html',
  styleUrl: './admin-psychometrics-edit.component.scss',
})
export default class AdminPsychometricsEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly psychometricsService = inject(AdminPsychometricsService);

  isEditMode = false;
  testId: number | null = null;

  test: PsychometricTest = {
    title: '',
    testType: 'CUSTOM',
    category: 'General Wellbeing',
    description: '',
    purpose: '',
    sourceUrl: '',
    isActive: true,
    questions: [],
  };

  readonly isSaving = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successNotice = signal<string | null>(null);

  // File Upload Modal State
  showFileUploadModal = false;
  selectedFile: File | null = null;
  isFileParsing = false;
  parsedQuestionsPreview: PsychometricQuestion[] = [];
  parsedFileTitle = '';

  // Link Import Modal State
  showLinkImportModal = false;
  importUrl = '';
  isLinkFetching = false;
  linkImportError = '';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.testId = Number(idParam);
      this.isEditMode = true;
      this.loadTest(this.testId);
    } else {
      // Default to 1 empty question for convenience
      this.addQuestion('', 'General');
    }
  }

  loadTest(id: number): void {
    this.isLoading.set(true);
    this.psychometricsService.getTest(id).subscribe({
      next: (data: PsychometricTest) => {
        this.test = { ...data };
        if (!this.test.questions || this.test.questions.length === 0) {
          this.test.questions = [];
        }
        this.reindexQuestions();
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải bài trắc nghiệm. Vui lòng kiểm tra lại ID.');
        this.isLoading.set(false);
      },
    });
  }

  addQuestion(text = '', category = 'General'): void {
    if (!this.test.questions) {
      this.test.questions = [];
    }
    const num = this.test.questions.length + 1;
    this.test.questions.push({
      questionNumber: num,
      questionText: text,
      category: category,
      questionType: 'Rating0To3',
    });
  }

  removeQuestion(index: number): void {
    if (!this.test.questions) return;
    if (this.test.questions.length <= 1) {
      alert('Đề trắc nghiệm tâm lý cần ít nhất một câu hỏi.');
      return;
    }
    this.test.questions.splice(index, 1);
    this.reindexQuestions();
  }

  reindexQuestions(): void {
    if (!this.test.questions) return;
    this.test.questions.forEach((q, idx) => {
      q.questionNumber = idx + 1;
    });
  }

  loadTemplate(type: 'DASS21' | 'GAD7' | 'PHQ9' | 'CLEAR'): void {
    if (type === 'CLEAR') {
      this.test.questions = [];
      this.addQuestion('', 'General');
      return;
    }

    if (type === 'DASS21') {
      this.test.title = 'Depression, Anxiety and Stress Scale (DASS-21)';
      this.test.testType = 'DASS21';
      this.test.category = 'General Wellbeing';
      this.test.sourceUrl = 'http://www2.psy.unsw.edu.au/dass/';
      this.test.description = 'Thang đo DASS-21 là bộ công cụ lâm sàng gồm 21 câu hỏi định lượng 3 trạng thái cảm xúc: Trầm cảm, Lo âu và Căng thẳng (Stress).';
      this.test.purpose = 'Sàng lọc và phân loại mức độ tổn thương tâm lý theo 5 cấp độ (Bình thường, Nhẹ, Vừa, Nặng, Rất nặng).';
      this.test.questions = this.psychometricsService.getDass21Questions();
    } else if (type === 'GAD7') {
      this.test.title = 'GAD-7 Generalized Anxiety Disorder Scale';
      this.test.testType = 'GAD7';
      this.test.category = 'Anxiety';
      this.test.sourceUrl = '';
      this.test.description = 'Thang đo GAD-7 gồm 7 câu hỏi tiêu chuẩn đo lường và sàng lọc mức độ rối loạn lo âu lan tỏa trong 2 tuần gần nhất.';
      this.test.purpose = 'Lượng giá mức độ lo âu từ Tối thiểu (0-4), Nhẹ (5-9), Trung bình (10-14) đến Nặng (15-21).';
      this.test.questions = this.psychometricsService.getGad7Questions();
    } else if (type === 'PHQ9') {
      this.test.title = 'PHQ-9 Patient Health Questionnaire';
      this.test.testType = 'PHQ9';
      this.test.category = 'Depression';
      this.test.sourceUrl = '';
      this.test.description = 'Bảng câu hỏi sức khỏe bệnh nhân PHQ-9 là công cụ 9 câu hỏi sàng lọc, chẩn đoán và theo dõi mức độ nghiêm trọng của bệnh trầm cảm.';
      this.test.purpose = 'Phân loại mức độ trầm cảm từ Không trầm cảm (0-4), Nhẹ (5-9), Vừa (10-14), Nặng vừa (15-19) đến Nặng (20-27).';
      this.test.questions = this.psychometricsService.getPhq9Questions();
    }

    this.reindexQuestions();
    this.successNotice.set(`Đã tải mẫu y khoa tiêu chuẩn ${type} thành công (${this.test.questions?.length} câu hỏi).`);
    setTimeout(() => this.successNotice.set(null), 3500);
  }

  // File Upload Feature
  openFileUploadModal(): void {
    this.selectedFile = null;
    this.parsedQuestionsPreview = [];
    this.parsedFileTitle = '';
    this.showFileUploadModal = true;
  }

  closeFileUploadModal(): void {
    this.showFileUploadModal = false;
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (!file) return;
    this.selectedFile = file;
    this.isFileParsing = true;

    this.psychometricsService
      .parseQuestionsFromFile(file)
      .then(res => {
        this.parsedQuestionsPreview = res.questions;
        this.parsedFileTitle = res.title || file.name.replace(/\.[^/.]+$/, '');
        this.isFileParsing = false;
      })
      .catch(() => {
        this.isFileParsing = false;
        alert('Không thể đọc file. Vui lòng kiểm tra định dạng file (Excel, Word, CSV, JSON, TXT).');
      });
  }

  applyParsedFile(): void {
    if (this.parsedQuestionsPreview.length === 0) return;

    if (!this.test.title && this.parsedFileTitle) {
      this.test.title = this.parsedFileTitle;
    }
    this.test.questions = [...this.parsedQuestionsPreview];
    this.reindexQuestions();
    this.closeFileUploadModal();

    this.successNotice.set(`Đã nhập thành công ${this.parsedQuestionsPreview.length} câu hỏi từ file "${this.selectedFile?.name}".`);
    setTimeout(() => this.successNotice.set(null), 4000);
  }

  // Link Import Feature (Khuyến khích)
  openLinkImportModal(): void {
    this.importUrl = '';
    this.linkImportError = '';
    this.showLinkImportModal = true;
  }

  closeLinkImportModal(): void {
    this.showLinkImportModal = false;
  }

  executeLinkImport(): void {
    if (!this.importUrl.trim()) {
      this.linkImportError = 'Vui lòng dán hoặc nhập đường dẫn URL hợp lệ.';
      return;
    }

    this.isLinkFetching = true;
    this.linkImportError = '';

    this.psychometricsService.importQuestionsFromLink(this.importUrl.trim()).subscribe({
      next: res => {
        this.isLinkFetching = false;
        if (res.title && !this.test.title) {
          this.test.title = res.title;
        }
        if (res.description && !this.test.description) {
          this.test.description = res.description;
        }
        this.test.sourceUrl = res.sourceUrl || this.importUrl.trim();

        if (res.questions && res.questions.length > 0) {
          this.test.questions = [...res.questions];
          this.reindexQuestions();
          this.closeLinkImportModal();
          this.successNotice.set(`Đã nhập thành công ${res.questions.length} câu hỏi từ liên kết trực tuyến!`);
          setTimeout(() => this.successNotice.set(null), 4000);
        } else {
          this.linkImportError = 'Không trích xuất được câu hỏi từ link này. Vui lòng kiểm tra cấu trúc URL.';
        }
      },
      error: () => {
        this.isLinkFetching = false;
        this.linkImportError = 'Lỗi kết nối khi trích xuất từ link. Vui lòng kiểm tra URL.';
      },
    });
  }

  save(): void {
    if (!this.test.title?.trim()) {
      this.errorMessage.set('Vui lòng nhập tiêu đề bài trắc nghiệm.');
      return;
    }
    if (!this.test.testType?.trim()) {
      this.errorMessage.set('Vui lòng chọn hoặc nhập mã loại thang đo (e.g. GAD7, DASS21).');
      return;
    }
    if (!this.test.questions || this.test.questions.length === 0) {
      this.errorMessage.set('Đề trắc nghiệm phải có ít nhất 1 câu hỏi.');
      return;
    }

    // Validate that questions are non-empty
    const emptyQ = this.test.questions.find(q => !q.questionText?.trim());
    if (emptyQ) {
      this.errorMessage.set(`Câu hỏi #${emptyQ.questionNumber} đang để trống nội dung.`);
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    if (this.isEditMode && this.testId) {
      this.psychometricsService.updateTest(this.testId, this.test).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.router.navigate(['/admin/psychometrics']);
        },
        error: (err: any) => {
          this.isSaving.set(false);
          this.errorMessage.set(err?.error?.message || 'Lỗi khi cập nhật bài trắc nghiệm.');
        },
      });
    } else {
      this.psychometricsService.createTest(this.test).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.router.navigate(['/admin/psychometrics']);
        },
        error: (err: any) => {
          this.isSaving.set(false);
          this.errorMessage.set(err?.error?.message || 'Lỗi khi tạo mới bài trắc nghiệm.');
        },
      });
    }
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
