import { runBuilder } from '@angular-architects/native-federation/internal';
import { createBuilder } from '@angular-devkit/architect';

import defineEsbuild from './define-esbuild.ts';

// Native federation delegates to the Angular application builder, the custom esbuild plugins are injected here.
const plugins = [defineEsbuild];

export default createBuilder(async function* nativeFederationBuilder(options, context) {
  yield* runBuilder({ ...options, plugins }, context);
});
