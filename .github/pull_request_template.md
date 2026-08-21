## What this changes

<!-- One or two sentences. If it adds an example, name it and its upstream raylib
     source (e.g. shapes/shapes_bouncing_ball) if it's a port. -->

## Gates

<!-- All three are fast and need no JVM at runtime. Please paste or confirm. -->

- [ ] `bb check` passes (headless compile-check of every example)
- [ ] `bb lint:strict` passes (clj-kondo, non-zero exit on any finding)
- [ ] `bb lsp:format-check` passes (clojure-lsp formatting, **not** cljfmt)

## If this adds an example

<!-- Skip this section otherwise. All four touchpoints are required, or the
     example won't be compile-checked or won't appear in bb info / run-all.
     See docs/guide/example-catalog.md -->

- [ ] Source namespace under `src/net/b12n/raylib_jlt/`
- [ ] `:<name>` alias in `deps.edn`
- [ ] Namespace added to the `:require` list in `check.clj`
- [ ] Registry row + `bb <name>` task in `bb.edn`
- [ ] Row added to the example table in `README.md`
- [ ] Ran it in a real window, or verified with `RAYLIB_APP_AUTO_QUIT_MS` / `RAYLIB_APP_SHOT`

## Environment you tested on

<!-- The pointer trick used by camera2d / camera-3d is AArch64-specific, so
     architecture matters for anything touching the binding layer. -->

- OS / arch (`uname -sm`):
- jolt version (`jolt --version`):

## Notes for the reviewer

<!-- Anything surprising, any deliberate deviation, anything you're unsure about. -->
