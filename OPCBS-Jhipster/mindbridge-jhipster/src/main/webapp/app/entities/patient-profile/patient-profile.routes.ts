import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import PatientProfileResolve from './route/patient-profile-routing-resolve.service';

const patientProfileRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/patient-profile').then(m => m.PatientProfile),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/patient-profile-detail').then(m => m.PatientProfileDetail),
    resolve: {
      patientProfile: PatientProfileResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/patient-profile-update').then(m => m.PatientProfileUpdate),
    resolve: {
      patientProfile: PatientProfileResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/patient-profile-update').then(m => m.PatientProfileUpdate),
    resolve: {
      patientProfile: PatientProfileResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default patientProfileRoute;
