/* eslint-disable @typescript-eslint/no-unsafe-return */
import { Type } from '@angular/core';

import { loadRemoteModule } from '@angular-architects/native-federation';

import NavbarItem from 'app/layouts/navbar/navbar-item.model';

export const loadNavbarItems = (service: string): Promise<NavbarItem[]> =>
  loadRemoteModule<{ default: NavbarItem[] }>(service, './entity-navbar-items').then(items => items.default);

export const loadEntityRoutes = (service: string): Promise<Type<any>> =>
  loadRemoteModule<{ default: Type<any> }>(service, './entity-routes').then(routes => routes.default);
