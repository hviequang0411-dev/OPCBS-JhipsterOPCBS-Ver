import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import DoctorProfileResolve from './route/doctor-profile-routing-resolve.service';

const doctorProfileRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/doctor-profile').then(m => m.DoctorProfile),
    data: {
      defaultSort: `id,${ASC}`,
    },
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/doctor-profile-detail').then(m => m.DoctorProfileDetail),
    resolve: {
      doctorProfile: DoctorProfileResolve,
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./update/doctor-profile-update').then(m => m.DoctorProfileUpdate),
    resolve: {
      doctorProfile: DoctorProfileResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'verification',
    loadComponent: () => import('./verification/doctor-verification.component').then(m => m.DoctorVerificationComponent),
    title: 'Doctor Account Verification',
  },
  {
    path: 'patients',
    loadComponent: () => import('./patients/doctor-patients.component').then(m => m.DoctorPatientsComponent),
    title: 'Doctor Patients',
  },
  {
    path: 'patients/:id',
    loadComponent: () => import('./patients/detail/doctor-patient-detail.component').then(m => m.DoctorPatientDetailComponent),
    title: 'Patient Clinical History',
  },
  {
    path: 'patients/:id/view',
    loadComponent: () => import('./patients/detail/doctor-patient-detail.component').then(m => m.DoctorPatientDetailComponent),
    title: 'Patient Clinical History',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/doctor-profile-update').then(m => m.DoctorProfileUpdate),
    resolve: {
      doctorProfile: DoctorProfileResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default doctorProfileRoute;
