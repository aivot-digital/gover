# Frontend assets

This document describes how frontend files are built, loaded and served. The
executable configuration is authoritative: [package.json](../app/package.json),
the [customer](../app/vite.config.customer.js) and
[staff](../app/vite.config.staff.js) Vite configurations,
[Containerfile](../Containerfile) and [Nginx configuration](../container/nginx.conf).
Keep their paths, output naming and delivery rules aligned when changing them.

## Build outputs and asset ownership

The frontend has separate customer and staff builds. Both use `app/index.html`;
`VITE_APP_MODE` selects the corresponding TypeScript entry point.

| Build | Output directory | Container directory | URL base |
| --- | --- | --- | --- |
| Customer | `app/build/customer/` | `/app/www/` | `/` |
| Staff | `app/build/staff/` | `/app/www/staff/` | `/staff/` |

Import assets from application source when Vite should process them, rewrite
references and generate content-hashed filenames. Files in `app/public/` are
copied into each build without content hashing. Use public files for resources
that need stable URLs, and do not assume that a file is versioned simply because
it resides under `assets/`. Avoid hand-written URLs to generated chunks; let Vite
resolve imports and worker references for the appropriate shell base.

The backend supplies runtime configuration through
`/api/public/system/app-config.js`, which the HTML loads before the frontend
entry. It is not a generated Vite asset. The container also publishes one SBOM
bundle under `/sbom/`, independently of the two frontend builds.

## Loading optional functionality

Use dynamic imports at the point where optional functionality is needed. Keep
runtime imports behind that boundary; use type-only imports in shared interfaces
so an optional dependency is not pulled into the initial JavaScript graph.
Moving code into a chunk defers its download and execution; it does not remove
its size from the complete application.

Share concurrent initialization and reuse successful loads. A failed download
must allow a later attempt. Keep errors local to the affected surface and preserve
other user inputs. Discard asynchronous results when their input changes or the
surface closes. Do not automatically reload a page to recover a missing chunk.

Use the shared `withAsyncWrapper` utility for minimum loading durations. Apply
visible results after it resolves, not in its `after` callback, which runs before
the minimum duration has elapsed. Rejected operations bypass the wrapper's delay;
when an error transition also needs the minimum duration, settle the operation
inside `main` and handle its outcome after the wrapper resolves. Keep presentation
delays out of repeated calculations on an already initialized runtime.

Monaco and ELK have additional initialization requirements:

- Monaco's local loader configuration and language workers belong to its deferred
  runtime. Configuration must run before an editor mounts. Retain editor values,
  read-only state and model behavior through loading and rerendering.
  Vite remembers failed stylesheet preloads; retrying the import can skip those
  styles. The loader retains such a failure until a manual page reload to avoid
  mounting an unstyled editor. Review this guard when updating Vite.
- Process flows and department diagrams retain separate ELK instances and layout
  options. Runtime download retries must restore the diagram's measurement and
  viewport sequence. The team diagram uses its own layout without ELK.

## HTTP delivery

Nginx serves static files and falls back to the corresponding HTML entry for SPA
routes. Requests for missing assets must return 404, rather than HTML that the
browser could mistake for JavaScript or CSS.

Cache policy depends on whether a URL changes when its content changes:

| Resource | Policy |
| --- | --- |
| Content-hashed JS and CSS under either shell's `assets/` directory | Long-lived, immutable cache |
| Other files under those asset directories | Revalidate before reuse |
| HTML entries and the SBOM manifest | No-store |

The immutable rule matches Vite's eight-character content hash. Review the rule
when changing output naming or adding file types; do not extend it to arbitrary
public files or runtime configuration. A cached hashed URL must always refer to
the same content. The Nginx configuration defines the exact lifetimes and headers.

The npm build commands run `scripts/compress-assets.mjs` as a postbuild step.
It creates Brotli sidecars (`.br`) for content-hashed JavaScript and CSS, including
workers, using Node's built-in compressor. Quality 11 runs at build time with
bounded concurrency; requests do not perform Brotli compression. Originals remain
available, and sidecars are omitted if compression would increase the file size.
Keep the script's filename rule aligned with the Nginx immutable rule. Invoke the
npm build commands when preparing deployments: running `vite build` directly
does not run npm's postbuild step.

The container installs Alpine's `nginx-mod-http-brotli` alongside Nginx. Its static
module serves the sidecar at the original asset URL when the client accepts
`br`. The response retains the original MIME type and includes `Content-Encoding:
br` and `Vary: Accept-Encoding`. Gzip-only clients use the configured gzip
compression; clients without encoding support receive the original. A missing
sidecar still permits an uncompressed response. Deploy originals and sidecars
together, and do not rewrite application imports to point to `.br` URLs.

Verify content negotiation, MIME types and cache headers through the deployed
reverse proxy, not only at the container boundary. Proxy cache keys must respect
`Vary: Accept-Encoding` so compressed bytes never reach an incompatible client.

## Deployments and open tabs

Deploy HTML and its referenced assets consistently. Long-lived tabs can still
request chunks from an older build after a release. Browser caching cannot supply
an old chunk that the browser has never downloaded.

The application container contains the current build only. Deployments that must
support old tabs without reloading need to retain and serve previous hashed
assets for a suitable transition period, including during rolling updates. A
retry cannot recover a file that is no longer available. If recovery requires a
manual reload, users must have an opportunity to save their inputs first.

## Verification and diagnosis

Run frontend checks from `app/`:

```sh
npm test
npm run test:assets
npm run typecheck
npm run build:staff
npm run build:customer
```

Check both shells when changing shared imports, asset paths or build settings.
Measure initial JavaScript using the HTML entry and its recursive static imports;
report dynamic chunks, workers and CSS separately. Distinguish minified size,
compressed transfer size and browser execution time. Keep release-specific size
comparisons in change descriptions or performance reports.

Run the finite Nginx HTTP regression check from the repository root:

```sh
docker build -f container/Containerfile.nginx-test -t prosuna-nginx-test .
docker run --rm --network none prosuna-nginx-test
```

This script creates fixtures and starts Nginx only inside the disposable
container. The image uses Alpine's Nginx and Brotli packages, matching the
production Containerfile. It checks Brotli/gzip negotiation, lossless content,
MIME types, cache headers, missing sidecars and assets, SPA routes and SBOM
headers. Do not run the script on a persistent application host. Keep the test
image's Alpine release aligned with the production runtime. CI runs the build
script tests and these HTTP checks with the frontend tests.

Use production builds to inspect chunk requests and delivery headers; a Vite
development server does not reproduce production bundling or Nginx caching.
Check cold and warm caches, throttled or failed downloads, retries, navigation
while loading, and tabs left open across a deployment. Verify that optional
runtimes load when needed and that worker URLs stay local to the application.
For editor or diagram changes, also check input preservation, read-only behavior,
selection and viewport restoration on the supported shell viewports.
