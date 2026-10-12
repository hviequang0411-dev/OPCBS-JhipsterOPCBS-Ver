import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config';
import { AlertError } from 'app/shared/alert';
import { IPatientProfile } from '../patient-profile.model';
import { PatientProfileService } from '../service/patient-profile.service';

@Component({
  templateUrl: './patient-profile-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class PatientProfileDeleteDialog {
  patientProfile?: IPatientProfile;

  protected readonly patientProfileService = inject(PatientProfileService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.patientProfileService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
