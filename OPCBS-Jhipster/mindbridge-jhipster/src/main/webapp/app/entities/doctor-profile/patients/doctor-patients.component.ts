import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import dayjs from 'dayjs/esm';
import { FormatMediumDatePipe } from 'app/shared/date';
import { DoctorPatientService, IDoctorPatientSummary } from 'app/entities/doctor-profile/service/doctor-patient.service';

@Component({
  selector: 'jhi-doctor-patients',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FontAwesomeModule, FormatMediumDatePipe],
  templateUrl: './doctor-patients.component.html',
  styleUrls: ['./doctor-patients.component.scss'],
})
export class DoctorPatientsComponent implements OnInit {
  private readonly doctorPatientService = inject(DoctorPatientService);

  readonly patients = signal<IDoctorPatientSummary[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly searchTerm = signal<string>('');
  readonly filterTab = signal<'ALL' | 'REGISTERED' | 'GUEST' | 'ACTIVE' | 'COMPLETED'>('ALL');

  // Computed Stats
  readonly totalPatientsCount = computed(() => this.patients().length);
  readonly registeredPatientsCount = computed(() => this.patients().filter(p => !p.isGuest).length);
  readonly guestPatientsCount = computed(() => this.patients().filter(p => p.isGuest).length);
  readonly activeTreatmentsCount = computed(() => this.patients().filter(p => p.upcomingAppointments > 0).length);
  readonly completedConsultationsCount = computed(() =>
    this.patients().reduce((sum, p) => sum + (p.completedAppointments || 0), 0)
  );

  // Computed Filtered List
  readonly filteredPatients = computed<IDoctorPatientSummary[]>(() => {
    let list = this.patients();
    const tab = this.filterTab();
    const term = this.searchTerm().toLowerCase().trim();

    if (tab === 'REGISTERED') {
      list = list.filter(p => !p.isGuest);
    } else if (tab === 'GUEST') {
      list = list.filter(p => p.isGuest);
    } else if (tab === 'ACTIVE') {
      list = list.filter(p => p.upcomingAppointments > 0);
    } else if (tab === 'COMPLETED') {
      list = list.filter(p => p.completedAppointments > 0 && p.upcomingAppointments === 0);
    }

    if (term) {
      list = list.filter(
        p =>
          (p.fullName && p.fullName.toLowerCase().includes(term)) ||
          (p.phone && p.phone.toLowerCase().includes(term)) ||
          (p.email && p.email.toLowerCase().includes(term)) ||
          (p.lastDiagnosis && p.lastDiagnosis.toLowerCase().includes(term)) ||
          (p.medicalHistory && p.medicalHistory.toLowerCase().includes(term))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.isLoading.set(true);
    this.doctorPatientService.getMyPatients().subscribe({
      next: data => {
        this.patients.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.patients.set([]);
        this.isLoading.set(false);
      },
    });
  }

  onTabChange(tab: 'ALL' | 'REGISTERED' | 'GUEST' | 'ACTIVE' | 'COMPLETED'): void {
    this.filterTab.set(tab);
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  readonly exportingPatientId = signal<number | null>(null);

  exportPatientPdf(patient: IDoctorPatientSummary, event: Event): void {
    event.stopPropagation();
    if (!patient.patientId) return;

    this.exportingPatientId.set(patient.patientId);

    this.doctorPatientService.exportPatientPdf(patient.patientId).subscribe({
      next: (blob: Blob) => {
        this.exportingPatientId.set(null);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const patientName = patient.fullName?.trim().replace(/\s+/g, '_') || `patient_${patient.patientId}`;
        a.download = `Benh_An_${patientName}_${dayjs().format('YYYYMMDD')}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.exportingPatientId.set(null);
      },
    });
  }

  getAge(dob?: dayjs.Dayjs | null): number | string {
    if (!dob) return '--';
    return dayjs().diff(dayjs(dob), 'year');
  }

  getInitials(name?: string | null): string {
    if (!name) return 'BN';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 0) return 'BN';
    if (parts.length === 1) return parts[0].substring(0, Math.min(2, parts[0].length)).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
}
