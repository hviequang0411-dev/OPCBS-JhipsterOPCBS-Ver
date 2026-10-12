import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config';
import { AlertError } from 'app/shared/alert';
import { SpecializationService } from '../service/specialization.service';
import { ISpecialization } from '../specialization.model';

@Component({
  templateUrl: './specialization-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class SpecializationDeleteDialog {
  specialization?: ISpecialization;

  protected readonly specializationService = inject(SpecializationService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.specializationService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
