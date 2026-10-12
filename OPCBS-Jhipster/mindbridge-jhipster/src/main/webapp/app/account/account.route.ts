import { Routes } from '@angular/router';

import activateRoute from './activate/activate.route';
import passwordRoute from './password/password.route';
import passwordResetFinishRoute from './password-reset/finish/password-reset-finish.route';
import passwordResetInitRoute from './password-reset/init/password-reset-init.route';
import registerRoute from './register/register.route';
import registerDoctorRoute from './register-doctor/register-doctor.route';
import settingsRoute from './settings/settings.route';

const accountRoutes: Routes = [
  activateRoute,
  passwordRoute,
  passwordResetFinishRoute,
  passwordResetInitRoute,
  registerRoute,
  registerDoctorRoute,
  {
    path: 'verify-otp',
    loadComponent: () => import('./verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent),
    title: 'Xác Thực OTP - MindBridge',
  },
  settingsRoute,
];

export default accountRoutes;
