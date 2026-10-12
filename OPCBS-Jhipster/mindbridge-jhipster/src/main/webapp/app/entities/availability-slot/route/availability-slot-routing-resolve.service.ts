import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IAvailabilitySlot } from '../availability-slot.model';
import { AvailabilitySlotService } from '../service/availability-slot.service';

const availabilitySlotResolve = (route: ActivatedRouteSnapshot): Observable<null | IAvailabilitySlot> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(AvailabilitySlotService);
    return service.find(id).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          router.navigate(['404']);
        } else {
          router.navigate(['error']);
        }
        return EMPTY;
      }),
    );
  }

  return of(null);
};

export default availabilitySlotResolve;
