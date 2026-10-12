import { Route } from '@angular/router';

const registerDoctorRoute: Route = {
  path: 'register-doctor',
  loadComponent: () => import('./register-doctor'),
  title: 'Become a Doctor - MindBridge',
};

export default registerDoctorRoute;
