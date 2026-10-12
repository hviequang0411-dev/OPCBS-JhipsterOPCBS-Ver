import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { IAvailabilitySlot } from '../availability-slot.model';

@Component({
  selector: 'jhi-availability-slot-detail',
  templateUrl: './availability-slot-detail.html',
  imports: [FontAwesomeModule, Alert, AlertError, RouterLink, FormatMediumDatetimePipe],
})
export class AvailabilitySlotDetail {
  readonly availabilitySlot = input<IAvailabilitySlot | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
