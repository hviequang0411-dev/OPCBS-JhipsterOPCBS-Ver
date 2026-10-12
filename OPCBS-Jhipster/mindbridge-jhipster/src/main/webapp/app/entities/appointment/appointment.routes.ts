import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import AppointmentResolve from './route/appointment-routing-resolve.service';

const appointmentRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/appointment').then(m => m.Appointment),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'book',
    loadComponent: () => import('./book/appointment-book').then(m => m.AppointmentBook),
  },
  {
    path: 'book/:doctorId',
    loadComponent: () => import('./book/appointment-book').then(m => m.AppointmentBook),
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/appointment-detail').then(m => m.AppointmentDetail),
    resolve: {
      appointment: AppointmentResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id',
    loadComponent: () => import('./detail/appointment-detail').then(m => m.AppointmentDetail),
    resolve: {
      appointment: AppointmentResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/appointment-update').then(m => m.AppointmentUpdate),
    resolve: {
      appointment: AppointmentResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/appointment-update').then(m => m.AppointmentUpdate),
    resolve: {
      appointment: AppointmentResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default appointmentRoute;

