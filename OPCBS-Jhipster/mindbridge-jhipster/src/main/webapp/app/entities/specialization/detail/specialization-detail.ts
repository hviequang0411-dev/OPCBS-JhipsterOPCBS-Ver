import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { Alert, AlertError } from 'app/shared/alert';
import { ISpecialization } from '../specialization.model';

@Component({
  selector: 'jhi-specialization-detail',
  templateUrl: './specialization-detail.html',
  imports: [FontAwesomeModule, Alert, AlertError, RouterLink],
})
export class SpecializationDetail {
  readonly specialization = input<ISpecialization | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
