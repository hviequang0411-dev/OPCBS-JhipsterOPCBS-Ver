import { Route } from '@angular/router';

import Register from './register';

const registerRoute: Route = {
  path: 'register',
  loadComponent: () => import('./register'),
  title: 'Registration',
};

export default registerRoute;
