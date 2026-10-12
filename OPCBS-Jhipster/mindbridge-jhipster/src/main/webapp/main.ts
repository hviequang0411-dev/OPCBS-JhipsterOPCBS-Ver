import { initFederation } from '@angular-architects/native-federation';

initFederation(
  {},
  // Expose this application's own modules through federation.
  { hostRemoteEntry: { name: 'opcbs', url: './remoteEntry.json' } },
)
  .catch((err: unknown) => console.error(err)) // NOSONAR
  .then(() => import('./bootstrap'))
  .catch((err: unknown) => console.error(err)); // NOSONAR

