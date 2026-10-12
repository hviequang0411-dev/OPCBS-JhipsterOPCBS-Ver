import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config';
import { AlertError } from 'app/shared/alert';
import { IAvailabilitySlot } from '../availability-slot.model';
import { AvailabilitySlotService } from '../service/availability-slot.service';

@Component({
  templateUrl: './availability-slot-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class AvailabilitySlotDeleteDialog {
  availabilitySlot?: IAvailabilitySlot;

  protected readonly availabilitySlotService = inject(AvailabilitySlotService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.availabilitySlotService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
