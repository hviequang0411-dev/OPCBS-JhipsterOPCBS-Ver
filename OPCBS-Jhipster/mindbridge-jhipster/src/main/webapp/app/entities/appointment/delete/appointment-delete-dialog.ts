import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config';
import { AlertError } from 'app/shared/alert';
import { IAppointment } from '../appointment.model';
import { AppointmentService } from '../service/appointment.service';

@Component({
  templateUrl: './appointment-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class AppointmentDeleteDialog {
  appointment?: IAppointment;

  protected readonly appointmentService = inject(AppointmentService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.appointmentService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
