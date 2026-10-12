import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { EmotionJournalService } from '../service/emotion-journal.service';
import { IEmotionJournal, IJournalStats } from '../emotion-journal.model';

@Component({
  selector: 'jhi-patient-journal',
  standalone: true,
  imports: [CommonModule, FormsModule, NgbModule],
  templateUrl: './patient-journal.component.html',
  styleUrls: ['./patient-journal.component.scss']
})
export class PatientJournalComponent implements OnInit {
  private journalService = inject(EmotionJournalService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  journals: IEmotionJournal[] = [];
  stats: IJournalStats | null = null;
  isLoading = false;

  // New / Edit Journal Entry Form
  currentJournal: IEmotionJournal = this.getEmptyJournal();
  isEditMode = false;
  isSaving = false;
  errorMessage = '';

  moodOptions = [
    { value: 1, label: 'Rất tệ', emoji: '😭', colorClass: 'btn-outline-danger' },
    { value: 2, label: 'Tệ / Lo âu', emoji: '🙁', colorClass: 'btn-outline-warning' },
    { value: 3, label: 'Bình thường', emoji: '😐', colorClass: 'btn-outline-secondary' },
    { value: 4, label: 'Tốt / Ổn', emoji: '🙂', colorClass: 'btn-outline-info' },
    { value: 5, label: 'Rất tốt / Vui vẻ', emoji: '😄', colorClass: 'btn-outline-success' }
  ];

  ngOnInit(): void {
    this.loadJournals();
    this.loadStats();
  }

  loadJournals(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.journalService.getMyJournals().subscribe({
      next: res => {
        this.journals = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadStats(): void {
    this.journalService.getStats().subscribe({
      next: res => {
        this.stats = res;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cdr.markForCheck();
      }
    });
  }

  selectMood(value: number): void {
    this.currentJournal.moodScale = value;
  }

  openCreateModal(content: any): void {
    this.isEditMode = false;
    this.currentJournal = this.getEmptyJournal();
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  openEditModal(content: any, journal: IEmotionJournal): void {
    this.isEditMode = true;
    this.currentJournal = { ...journal };
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static' });
  }

  saveJournal(modal: any): void {
    if (!this.currentJournal.title?.trim()) {
      this.errorMessage = 'Vui lòng nhập tiêu đề cho nhật ký hôm nay!';
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';

    if (this.isEditMode && this.currentJournal.id) {
      this.journalService.updateJournal(this.currentJournal.id, this.currentJournal).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadJournals();
          this.loadStats();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi cập nhật nhật ký!';
        }
      });
    } else {
      this.journalService.createJournal(this.currentJournal).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadJournals();
          this.loadStats();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi lưu nhật ký!';
        }
      });
    }
  }

  deleteJournal(journal: IEmotionJournal): void {
    if (!journal.id) return;
    if (confirm('Bạn có chắc chắn muốn xóa mục nhật ký này không?')) {
      this.journalService.deleteJournal(journal.id).subscribe({
        next: () => {
          this.loadJournals();
          this.loadStats();
        }
      });
    }
  }

  getMoodEmoji(scale?: number | null): string {
    switch (scale) {
      case 1: return '😭';
      case 2: return '🙁';
      case 3: return '😐';
      case 4: return '🙂';
      case 5: return '😄';
      default: return '😐';
    }
  }

  getMoodLabel(scale?: number | null): string {
    switch (scale) {
      case 1: return 'Rất tệ';
      case 2: return 'Tệ / Lo âu';
      case 3: return 'Bình thường';
      case 4: return 'Tốt / Ổn';
      case 5: return 'Rất tốt / Vui';
      default: return 'Bình thường';
    }
  }

  getStressLabel(scale?: number | null): string {
    switch (scale) {
      case 1: return 'Rất thấp / Thư giãn';
      case 2: return 'Thấp';
      case 3: return 'Trung bình';
      case 4: return 'Cao';
      case 5: return 'Rất cao / Kiệt sức';
      default: return 'Trung bình';
    }
  }

  private getEmptyJournal(): IEmotionJournal {
    return {
      title: 'Nhật ký cảm xúc ngày ' + new Date().toLocaleDateString('vi-VN'),
      content: '',
      moodScale: 4,
      stressScale: 2,
      sleepHours: 7.0,
      depressionScale: 1,
      isShared: true
    };
  }
}
