import { Component, OnInit, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { AccountService } from 'app/core/auth';
import { DataUtils } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { type BlobType } from 'app/shared/jhipster/data-utils';
import { IAvailabilitySlot } from 'app/entities/availability-slot/availability-slot.model';
import { AvailabilitySlotService } from 'app/entities/availability-slot/service/availability-slot.service';
import { IDoctorProfile } from '../doctor-profile.model';

@Component({
  selector: 'jhi-doctor-profile-detail',
  templateUrl: './doctor-profile-detail.html',
  styleUrl: './doctor-profile-detail.scss',
  imports: [CommonModule, FormsModule, FontAwesomeModule, Alert, AlertError, RouterLink],
})
export class DoctorProfileDetail implements OnInit {
  readonly doctorProfile = input<IDoctorProfile | null>(null);

  readonly availableSlots = signal<IAvailabilitySlot[]>([]);
  readonly isLoadingSlots = signal<boolean>(false);
  readonly selectedSlot = signal<IAvailabilitySlot | null>(null);

  readonly account = inject(AccountService).account;
  protected dataUtils = inject(DataUtils);
  private readonly router = inject(Router);
  private readonly slotService = inject(AvailabilitySlotService);

  constructor() {
    effect(() => {
      const doc = this.doctorProfile();
      if (doc?.id) {
        this.loadDoctorSlots(doc.id);
      }
    });
  }

  ngOnInit(): void {
    const doc = this.doctorProfile();
    if (doc?.id) {
      this.loadDoctorSlots(doc.id);
    }
  }

  loadDoctorSlots(doctorId: number): void {
    this.isLoadingSlots.set(true);
    this.slotService
      .query({
        'doctorId.equals': doctorId,
        'status.equals': 'AVAILABLE',
        sort: ['startTime,asc'],
      })
      .subscribe({
        next: res => {
          if (res.body) {
            this.availableSlots.set(res.body);
          }
          this.isLoadingSlots.set(false);
        },
        error: () => {
          this.isLoadingSlots.set(false);
        },
      });
  }

  getInitials(name?: string | null): string {
    if (!name) return 'DR';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  isDoctor(): boolean {
    return this.account()?.authorities?.includes('ROLE_DOCTOR') ?? false;
  }

  selectSlot(slot: IAvailabilitySlot): void {
    this.selectedSlot.set(slot);
  }

  bookAppointment(slot?: IAvailabilitySlot): void {
    const doc = this.doctorProfile();
    if (!doc) return;
    const targetSlot = slot ?? this.selectedSlot();
    const queryParams: Record<string, any> = { doctorId: doc.id };
    if (targetSlot?.id) {
      queryParams['slotId'] = targetSlot.id;
    }
    this.router.navigate(['/appointment/book', doc.id], { queryParams });
  }

  formatSlotDisplay(slot: IAvailabilitySlot): string {
    if (!slot.startTime || !slot.endTime) return '';
    return `${slot.startTime.format('ddd, MMM D, YYYY, h:mm A')} - ${slot.endTime.format('h:mm A')}`;
  }

  formatSelectedSlotFull(slot: IAvailabilitySlot): string {
    if (!slot.startTime || !slot.endTime) return '';
    return `${slot.startTime.format('dddd, MMMM D, YYYY, h:mm A')} — ${slot.endTime.format('h:mm A')}`;
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined, blobType?: BlobType): void {
    this.dataUtils.openFile(base64String, contentType, blobType);
  }

  previousState(): void {
    globalThis.history.back();
  }
}
