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
    // Load specializations
    this.specializationService.query({ size: 12, sort: ['name,asc'] }).subscribe({
      next: res => {
        if (res.body) {
          this.specializations.set(res.body);
        }
      },
      error: () => {
        // Fallback placeholder data if DB is empty
        this.specializations.set([
          { id: 1, name: 'Anxiety & Depression', description: 'Specialized support for mood management' },
          { id: 2, name: 'Couples & Family Therapy', description: 'Relationship guidance and counseling' },
          { id: 3, name: 'Stress & Burnout', description: 'Workplace stress and mindfulness therapy' },
          { id: 4, name: 'Child & Adolescent', description: 'Developmental and youth mental care' },
          { id: 5, name: 'Trauma & PTSD', description: 'Evidence-based recovery counseling' },
          { id: 6, name: 'Career & Life Coaching', description: 'Navigating life changes and goals' },
        ]);
      },
    });

    // Load featured doctors
    this.doctorProfileService.query({ size: 4, sort: ['experienceYears,desc'] }).subscribe({
      next: res => {
        if (res.body && res.body.length > 0) {
          this.featuredDoctors.set(res.body);
        } else {
          // Fallback sample doctors
          this.featuredDoctors.set([
            {
              id: 1,
              fullName: 'Dr. Sarah Jenkins',
              title: 'Clinical Psychologist, Ph.D',
              bio: 'Specialist in Cognitive Behavioral Therapy (CBT) with over 12 years supporting adults facing chronic anxiety.',
              experienceYears: 12,
              consultationFee: 850000,
              phone: '0901234567',
              isActive: true,
            },
            {
              id: 2,
              fullName: 'Dr. Michael Chen',
              title: 'Family & Marriage Counselor, M.D.',
              bio: 'Dedicated to strengthening relationship bonds and personal resilience through empathetic psychotherapy.',
              experienceYears: 9,
              consultationFee: 750000,
              phone: '0912345678',
              isActive: true,
            },
            {
              id: 3,
              fullName: 'Dr. Elena Rostova',
              title: 'Trauma & PTSD Specialist',
              bio: 'Compassionate holistic therapist applying mindfulness and somatic experiencing for post-trauma healing.',
              experienceYears: 15,
              consultationFee: 1100000,
              phone: '0923456789',
              isActive: true,
            },
            {
              id: 4,
              fullName: 'Dr. Nguyen Van An',
              title: 'Neuropsychiatrist & Wellness Coach',
              bio: 'Pioneering evidence-based stress reduction techniques and adult ADHD behavioral strategies.',
              experienceYears: 8,
              consultationFee: 650000,
              phone: '0934567890',
              isActive: true,
            },
          ]);
        }
        this.isLoading.set(false);
      },
      error: () => {
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
