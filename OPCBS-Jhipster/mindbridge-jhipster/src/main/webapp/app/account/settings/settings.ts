import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

import { AccountService } from 'app/core/auth';
import { AlertError } from 'app/shared/alert';
import { Gender } from 'app/entities/enumerations/gender.model';

@Component({
  selector: 'jhi-settings',
  standalone: true,
  imports: [CommonModule, AlertError, ReactiveFormsModule],
  templateUrl: './settings.html',
})
export default class Settings implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly http = inject(HttpClient);

  readonly success = signal(false);
  readonly currentRole = signal<string>('USER');
  readonly genders = Object.values(Gender);

  avatarUrl = signal<string | null>(null);
  isUploadingAvatar = signal<boolean>(false);
  avatarSuccess = signal<boolean>(false);
  uploadError = signal<string | null>(null);

  settingsForm = new FormGroup({
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    login: new FormControl({ value: '', disabled: true }),

    // Patient & general details
    fullName: new FormControl(''),
    phone: new FormControl(''),
    dateOfBirth: new FormControl(''),
    gender: new FormControl<Gender | null>(null),
    address: new FormControl(''),
    medicalHistory: new FormControl(''),

    // Doctor specific fields
    title: new FormControl(''),
    bio: new FormControl(''),
    experienceYears: new FormControl<number | null>(null),
    consultationFee: new FormControl<number | null>(null),
  });

  private rawProfile: any = null;

  ngOnInit(): void {
    this.accountService.getProfile().subscribe({
      next: profile => {
        if (profile) {
          this.rawProfile = profile;
          this.currentRole.set(profile.primaryRole ?? 'USER');
          this.avatarUrl.set(profile.imageUrl ?? null);

          const patient = profile.patientProfile ?? {};
          const doctor = profile.doctorProfile ?? {};

          const fallbackFullName = `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim();
          const patientName = patient.fullName;
          const doctorName = doctor.fullName;
          const resolvedName = (patientName && patientName.length > 0) ? patientName : ((doctorName && doctorName.length > 0) ? doctorName : fallbackFullName);

          this.settingsForm.patchValue({
            login: profile.login,
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            email: profile.email ?? '',
            fullName: resolvedName,
            phone: patient.phone ?? doctor.phone ?? '',
            dateOfBirth: patient.dateOfBirth ?? '',
            gender: patient.gender ?? doctor.gender ?? null,
            address: patient.address ?? '',
            medicalHistory: patient.medicalHistory ?? '',
            title: doctor.title ?? '',
            bio: doctor.bio ?? '',
            experienceYears: doctor.experienceYears ?? null,
            consultationFee: doctor.consultationFee ?? null,
          });
        }
      },
      error: () => {
        // Fallback to basic identity
        this.accountService.identity().subscribe(account => {
          if (account) {
            this.avatarUrl.set(account.imageUrl ?? null);
            this.settingsForm.patchValue({
              login: account.login,
              firstName: account.firstName ?? '',
              lastName: account.lastName ?? '',
              email: account.email,
            });
          }
        });
      },
    });
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.isUploadingAvatar.set(true);
    this.uploadError.set(null);
    this.avatarSuccess.set(false);

    const formData = new FormData();
    formData.append('file', file);

    this.http.post<{ imageUrl: string; url: string }>('/api/doctor/upload-avatar', formData).subscribe({
      next: res => {
        const cloudUrl = res.imageUrl || res.url;
        this.avatarUrl.set(cloudUrl);
        this.isUploadingAvatar.set(false);
        this.avatarSuccess.set(true);

        if (this.rawProfile) {
          this.rawProfile.imageUrl = cloudUrl;
        }

        // Auto persist new avatar into user profile
        this.save();
        setTimeout(() => this.avatarSuccess.set(false), 4000);
      },
      error: err => {
        this.isUploadingAvatar.set(false);
        this.uploadError.set('Không thể tải ảnh lên máy chủ Cloudinary. Vui lòng thử lại.');
      },
    });
  }

  save(): void {
    this.success.set(false);
    const formValues = this.settingsForm.getRawValue();

    const payload: any = {
      ...(this.rawProfile ?? {}),
      firstName: formValues.firstName,
      lastName: formValues.lastName,
      email: formValues.email,
      imageUrl: this.avatarUrl(),
    };

    if (this.currentRole() === 'PATIENT' || this.rawProfile?.patientProfile) {
      const defaultName = `${formValues.firstName} ${formValues.lastName}`.trim();
      payload.patientProfile = {
        ...(this.rawProfile?.patientProfile ?? {}),
        fullName: (formValues.fullName && formValues.fullName.length > 0) ? formValues.fullName : defaultName,
        phone: formValues.phone ?? '',
        dateOfBirth: formValues.dateOfBirth ?? null,
        gender: formValues.gender ?? null,
        address: formValues.address ?? '',
        medicalHistory: formValues.medicalHistory ?? '',
      };
    }

    if (this.currentRole() === 'DOCTOR' || this.rawProfile?.doctorProfile) {
      const defaultDoctorName = `${formValues.firstName} ${formValues.lastName}`.trim();
      payload.doctorProfile = {
        ...(this.rawProfile?.doctorProfile ?? {}),
        fullName: (formValues.fullName && formValues.fullName.length > 0) ? formValues.fullName : defaultDoctorName,
        phone: formValues.phone ?? '',
        gender: formValues.gender ?? null,
        title: formValues.title ?? '',
        bio: formValues.bio ?? '',
        experienceYears: formValues.experienceYears,
        consultationFee: formValues.consultationFee,
      };
    }

    this.accountService.updateProfile(payload).subscribe({
      next: updated => {
        this.success.set(true);
        this.rawProfile = updated;
        if (updated) {
          this.accountService.authenticate({
            firstName: updated.firstName,
            lastName: updated.lastName,
            email: updated.email,
            imageUrl: updated.imageUrl,
            activated: updated.activated,
            langKey: updated.langKey,
            login: updated.login,
            authorities: Array.from(updated.authorities ?? []),
          });
        }
      },
      error() {
        // Error handled by interceptor
      },
    });
  }

  userInitials(): string {
    const fullName = this.settingsForm.get('fullName')?.value ?? this.settingsForm.get('firstName')?.value ?? 'U';
    const parts = fullName.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return (parts[0]?.[0] ?? 'U').toUpperCase();
  }
}

