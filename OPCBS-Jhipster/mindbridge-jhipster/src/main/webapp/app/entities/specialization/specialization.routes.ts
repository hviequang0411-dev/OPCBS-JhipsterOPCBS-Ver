import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import SpecializationResolve from './route/specialization-routing-resolve.service';

const specializationRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/specialization').then(m => m.Specialization),
    data: {
      defaultSort: `id,${ASC}`,
    },
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/specialization-detail').then(m => m.SpecializationDetail),
    resolve: {
      specialization: SpecializationResolve,
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./update/specialization-update').then(m => m.SpecializationUpdate),
    resolve: {
      specialization: SpecializationResolve,
    },
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/specialization-update').then(m => m.SpecializationUpdate),
    resolve: {
      specialization: SpecializationResolve,
    },
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
  },
];

export default specializationRoute;
