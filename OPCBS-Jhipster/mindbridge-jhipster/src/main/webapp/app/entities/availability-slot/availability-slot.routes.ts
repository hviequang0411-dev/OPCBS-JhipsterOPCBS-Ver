import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import AvailabilitySlotResolve from './route/availability-slot-routing-resolve.service';

const availabilitySlotRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/availability-slot').then(m => m.AvailabilitySlot),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/availability-slot-detail').then(m => m.AvailabilitySlotDetail),
    resolve: {
      availabilitySlot: AvailabilitySlotResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/availability-slot-update').then(m => m.AvailabilitySlotUpdate),
    resolve: {
      availabilitySlot: AvailabilitySlotResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/availability-slot-update').then(m => m.AvailabilitySlotUpdate),
    resolve: {
      availabilitySlot: AvailabilitySlotResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default availabilitySlotRoute;
