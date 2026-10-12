import { mappingsFromWorkspace, share, shareAll, withNativeFederation } from '@angular-architects/native-federation/config';

const singleton = { singleton: true, strictVersion: true, requiredVersion: 'auto', includeSecondaries: false };

// The application imports the dayjs esm entry points, which are not covered by sharing the dayjs package.
// They must be shared explicitly so the shared mappings (app/shared/date, app/shared/language) use the same dayjs instance
// the plugins and locales are registered in (app/config/dayjs). Plugins and locales are only used by app/config/dayjs,
// which the unused dependencies scan (started from the exposed modules) does not reach, so they opt out of it.
const shareDayjs = (locales: string[]) =>
  share(
    Object.fromEntries(
      ['dayjs/esm', 'dayjs/esm/plugin/customParseFormat', 'dayjs/esm/plugin/duration', 'dayjs/esm/plugin/relativeTime']
        .concat(locales.map(locale => `dayjs/esm/locale/${locale}`))
        .map(entryPoint => [entryPoint, { ...singleton, includeSecondaries: { keepAll: true } }]),
    ),
  );

export default withNativeFederation({
  name: 'opcbs',

  exposes: {
    './entity-navbar-items': './src/main/webapp/app/entities/entity-navbar-items.ts',
    './entity-routes': './src/main/webapp/app/entities/entity.routes.ts',
    // jhipster-needle-expose
  },

  shared: {
    ...shareAll(
      { singleton: true, strictVersion: true, requiredVersion: 'auto' },
      {
        overrides: {
          // includeSecondaries is an opt-out of ignoreUnusedDeps, so all of @angular/core is shared to prevent mismatches.
          '@angular/core': { singleton: true, strictVersion: true, requiredVersion: 'auto', includeSecondaries: { keepAll: true } },
        },
      },
    ),
    ...shareDayjs(['en']),
  },

  // Application modules shared as singletons across the gateway and the microfrontends, see tsconfig.json paths.
  sharedMappings: mappingsFromWorkspace({ singleton: true, strictVersion: false })
    .filter([
      'app/config',
      'app/core/auth',
      'app/core/request',
      'app/core/util',
      'app/shared/alert',
      'app/shared/auth',
      'app/shared/date',
      'app/shared/pagination',
      'app/shared/sort',
    ])
    .get(),

  skip: [
    '@angular/localize',
    'es-module-shims',
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket',
    // Add further packages you don't need at runtime
  ],

  features: {
    // Application modules are not versioned libraries.
    mappingVersion: false,
    denseChunking: true,
  },
});
