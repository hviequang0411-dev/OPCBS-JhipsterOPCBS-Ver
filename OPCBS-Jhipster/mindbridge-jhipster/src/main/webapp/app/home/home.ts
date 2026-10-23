import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { AccountService } from 'app/core/auth';
import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { DoctorProfileService } from 'app/entities/doctor-profile/service/doctor-profile.service';
import { ISpecialization } from 'app/entities/specialization/specialization.model';
import { SpecializationService } from 'app/entities/specialization/service/specialization.service';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [CommonModule, FormsModule, RouterLink],
})
export default class Home implements OnInit {
  public readonly account = inject(AccountService).account;

  readonly specializations = signal<ISpecialization[]>([]);
  readonly featuredDoctors = signal<IDoctorProfile[]>([]);
  readonly isLoading = signal<boolean>(false);

  searchKeyword = '';
  selectedSpecializationId: number | null = null;

  private readonly router = inject(Router);
  private readonly specializationService = inject(SpecializationService);
  private readonly doctorProfileService = inject(DoctorProfileService);

  ngOnInit(): void {
    this.loadLandingData();
  }

  loadLandingData(): void {
    this.isLoading.set(true);
    // Load specializations from normalized database
    this.specializationService.query({ size: 12, sort: ['name,asc'] }).subscribe({
      next: res => {
        this.specializations.set(res.body || []);
      },
      error: () => {
        this.specializations.set([]);
      },
    });

    // Load featured verified doctors from normalized database
    this.doctorProfileService.query({ size: 4, sort: ['experienceYears,desc'] }).subscribe({
      next: res => {
        this.featuredDoctors.set(res.body || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.featuredDoctors.set([]);
        this.isLoading.set(false);
      },
    });
  }

  onSearch(): void {
    const queryParams: Record<string, any> = {};
    if (this.searchKeyword.trim()) {
      queryParams['fullName.contains'] = this.searchKeyword.trim();
    }
    if (this.selectedSpecializationId) {
      queryParams['specializationsId.equals'] = this.selectedSpecializationId;
    }
    this.router.navigate(['/doctor-profile'], { queryParams });
  }

  filterBySpecialization(specId: number): void {
    this.router.navigate(['/doctor-profile'], {
      queryParams: { 'specializationsId.equals': specId },
    });
  }

  getInitials(name?: string | null): string {
    if (!name) return 'DR';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  login(): void {
    this.router.navigate(['/login']);
  }
}
