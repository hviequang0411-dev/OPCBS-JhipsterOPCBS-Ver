import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { IAppointment } from 'app/entities/appointment/appointment.model';
import { PatientBookingService } from 'app/entities/appointment/service/patient-booking.service';

@Component({
  selector: 'jhi-patient-clinical-records',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule, NgbPagination, FormatMediumDatePipe, FormatMediumDatetimePipe],
  templateUrl: './patient-clinical-records.component.html',
  styleUrls: ['./patient-clinical-records.component.scss'],
})
export class PatientClinicalRecordsComponent implements OnInit {
  private readonly patientBookingService = inject(PatientBookingService);
  private readonly cdr = inject(ChangeDetectorRef);

  records: IAppointment[] = [];
  filteredRecords: IAppointment[] = [];
  isLoading = false;
  searchTerm = '';
  errorMessage = '';

  // Pagination state
  page = 1;
  itemsPerPage = 6;

  selectedRecord?: IAppointment;

  get totalItems(): number {
    return this.filteredRecords.length;
  }

  get paginatedRecords(): IAppointment[] {
    const startIndex = (this.page - 1) * this.itemsPerPage;
    return this.filteredRecords.slice(startIndex, startIndex + this.itemsPerPage);
  }

  getFirstItemIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.page - 1) * this.itemsPerPage + 1;
  }

  getSecondItemIndex(): number {
    return Math.min(this.page * this.itemsPerPage, this.totalItems);
  }

  ngOnInit(): void {
    this.loadRecords();
  }

  loadRecords(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.patientBookingService.getMyClinicalRecords().subscribe({
      next: data => {
        this.records = data || [];
        this.applyFilter();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.isLoading = false;
        this.records = [];
        this.filteredRecords = [];
        this.errorMessage = 'Không thể tải hồ sơ bệnh án: ' + (err?.error?.message || err?.message || 'Lỗi kết nối máy chủ');
        this.cdr.markForCheck();
      },
    });
  }

  applyFilter(): void {
    this.page = 1;
    if (!this.searchTerm.trim()) {
      this.filteredRecords = [...this.records];
      return;
    }

    const term = this.searchTerm.toLowerCase().trim();
    this.filteredRecords = this.records.filter(
      r =>
        (r.bookingCode && r.bookingCode.toLowerCase().includes(term)) ||
        (r.doctor?.fullName && r.doctor.fullName.toLowerCase().includes(term)) ||
        (r.diagnosis && r.diagnosis.toLowerCase().includes(term)) ||
        (r.consultationSummary && r.consultationSummary.toLowerCase().includes(term)) ||
        (r.recommendations && r.recommendations.toLowerCase().includes(term))
    );
  }

  onPageChange(newPage: number): void {
    this.page = newPage;
    this.cdr.markForCheck();
  }

  onItemsPerPageChange(): void {
    this.page = 1;
    this.cdr.markForCheck();
  }

  viewDetail(record: IAppointment): void {
    this.selectedRecord = record;
  }

  closeDetail(): void {
    this.selectedRecord = undefined;
  }
}
