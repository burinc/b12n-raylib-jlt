# Changelog

Notable changes to raylib-jlt, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://jlt-commons.github.io/raylib-jlt/>.

## Unreleased

- **Eleven more examples, taking the suite to 108.** `clock-of-clocks`,
  `undo-redo`, `window-should-close`, `ellipse-collision`, `input-gestures`,
  `rlgl-triangle`, `random-sequence`, `camera-2d-mouse-zoom`,
  `rlgl-color-wheel`, `circle-sector-drawing` and `easings-rectangles`, each a
  port of its upstream raylib counterpart. `SetExitKey`, `KEY-NULL` and `KEY-B`
  are newly bound, and `rect-pro!` draws a rotated rectangle as an rlgl quad,
  standing in for DrawRectanglePro.
- **The drawing API coerces its int parameters.** `rect!`, `line!`, `circle!`,
  `ellipse!`, `text!` and the rest forward to C functions whose positions are
  int, and a double reaching one aborted the process on the first frame with
  `invalid foreign-procedure argument`. Callers compute positions in floating
  point constantly, so the coercion belongs at the boundary. Nothing that
  worked before behaves differently.
- **The demo gallery still shows 97.** The seven new examples have no recorded
  GIFs, so `docs/demos/` and both galleries are unchanged until `bb record`
  runs. The catalog lists them with the preview column marked rather than
  omitting them.

- **The project moved to the jlt-commons organization**, from `burinc/b12n-raylib-jlt`
  to `jlt-commons/raylib-jlt`. GitHub redirects the old URLs, so existing clones and
  links keep working.
- **The documentation site moved with it**, to
  <https://jlt-commons.github.io/raylib-jlt/>. The old address,
  `raylib-jlt.b12n.app`, ran on a private engine, a personal site repository and a
  personal AWS account, none of which the organization could take over. Nothing was
  lost in the move: the same guide, the same catalog, the same bespoke homepage.
- **Publishing is no longer a maintainer task.** `bb docs-sync` is gone, along with
  the S3 upload, the CloudFront invalidation and the wiki mirror it drove. In its
  place `.github/workflows/site.yml` builds the site on every pull request and
  deploys it from `main`, so a docs change is live on merge and a contributor can
  see their own change rendered before it lands.
- **The site generator is now a shared organization tool**,
  [jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine), pinned by
  tag. This repository keeps what belongs to it: `docs/site.edn` for configuration
  and `docs/templates/home.html` for the homepage, which used to live inside the
  private engine where nobody maintaining this project could reach it. Preview
  locally with `bb site:serve`.
- **The full-size gallery was missing the whole shaders group.**
  `docs/guide/demos.md` showed 91 of 97 demos while claiming to show every one, so
  `julia-set`, `mandelbrot-set`, `raymarching`, `rounded-rect-shader`,
  `palette-switch` and `shader-hot-reload` never appeared. All 97 are there now.
- **The suite tracks raylib 6.0**, up from 5.5, and `bb lib:check` now refuses
  anything older. macOS is `brew upgrade raylib`; Linux keeps the apt-or-source
  path with the CI pin moved to the 6.0 tag.
- **`DrawCircleGradient` takes its centre as a by-value `Vector2` in 6.0**, where
  5.5 took two ints. The C symbol name did not change, so this is the kind of
  break nothing loud catches: all 109 symbols the project binds resolve in both
  versions, and an old binding against a new library simply draws in the wrong
  place. Only a header signature diff finds it. It is the reason `lib:check`
  gates the version rather than warning.
- **jolt 0.7.23 is now a hard floor**, because that binding uses
  `[:by-value [:struct ...]]`. Older jolt fails at compile, not at runtime.
- Most of the upgrade was nothing, which is worth recording: every struct layout
  is byte-identical between 5.5 and 6.0 (`Color` 4, `Vector2` 8, `Vector3` 12,
  `Camera2D` 24, `Camera3D` 44, `Texture2D` 20), all 108 hardcoded constants
  still match, and none of the four functions 6.0 removes were bound. The packed
  `Color`, both pointer-trick cameras and every rlgl path are untouched.
- One constant is worth knowing for later: `SHADER_UNIFORM_SAMPLER2D` moved from
  8 to 12, because 6.0 inserted four UINT variants ahead of it. Nothing here uses
  it yet.
- clj-kondo learned that `jolt.ffi`'s `with-layout` / `with-alloc` / `with-out` /
  `with-c-string` bind their first symbol, so the lint gate stops reporting it as
  unresolved.

- **The suite is 97 examples, up from 75.** 16 took it to 91, which is parity
  with the JVM port: 4 textures, 7 core (window flags, monitors, clipboard,
  gamepad, touch, virtual controls, letterboxing), 4 in 3D (Lorenz attractor,
  DNA helix, yaw/pitch/roll, first-person maze) and elementary cellular
  automata.
- **Shaders, a category that was closed.** 6 more: `julia-set`,
  `mandelbrot-set`, `raymarching`, `rounded-rect-shader`, `palette-switch`,
  `shader-hot-reload`. `LoadShader` returns a `Shader` by value, which Chez's
  `foreign-procedure` cannot express - jolt 0.7.23's `[:by-value [:struct ...]]`
  is what opened it, and it **raises the project's jolt floor to 0.7.23**, the
  first hard version floor this suite has had. GLSL lives as a string in each
  namespace rather than a `.glsl` file, so an example stays self-contained.
- These six have no demo GIFs yet, so the catalog lists them without previews.
- **raylib's textures are reachable now**, which they were not before.
  `LoadTexture` returns a 20-byte `Texture2D` by value, which AArch64
  hands back through the `x8` indirect-result register and Chez's
  `foreign-procedure` cannot express at all, so the whole texture, image,
  font, shader, model and audio half of raylib was off the table. rlgl's
  layer underneath is entirely scalar, so a texture here is just the GL
  id `rlLoadTexture` returns: an int, no struct anywhere. Textures are
  built pixel by pixel in native memory rather than decoded from a file,
  which is the part of `LoadTexture` that does not come back.
  [`textures-via-rlgl.md`](docs/guide/textures-via-rlgl.md) is the new
  guide page.
- Render textures too: `rl/render-texture` and `rl/with-render-texture`
  spell out `BeginTextureMode`/`EndTextureMode` in scalar rlgl calls,
  including the HiDPI screen-scale matrix raylib reestablishes across
  `EndTextureMode` and the following `BeginDrawing`. Leaving that at
  identity draws every later frame at half size in the corner.
- New scalar bindings for the rest: window state and config flags,
  monitors, clipboard, gamepad, touch and gestures, key and mouse release
  predicates, `DrawCircleGradient`, `DrawRectangleGradientH` and blend
  modes.
- The registry moved to `scripts/examples_registry.clj` some time ago but
  the catalog still described it as a `bb.edn` row; adding an example is
  five touchpoints, not four, and the guide now says which.

- `bb docs-sync` says which kind of deploy failure it hit, unreachable
  AWS, missing or expired credentials, a 403, or a genuinely absent
  bucket. Instead of blaming missing infrastructure for all four and
  recommending `tofu:apply`. On a restricted laptop `HTTPS_PROXY` is the
  usual cause, and it now detects that and prints the unset-and-retry
  line.
- `bb docs-sync` exits non-zero when the deploy does not happen. It
  printed its complaint and exited 0 before, so nothing chaining off it
  could tell a publish from a no-op.
- Em-dashes removed from the docs.

## 2026-08-21

- `bb docs-sync` could never push a change that was already committed:
  it looked only at its own run's result, so a commit left behind by an
  earlier run stayed local forever.

## 2026-08-20: Public launch

Highlights:

- 75 raylib examples in [Jolt](https://github.com/jolt-lang/jolt), native
  Clojure on Chez Scheme, with no JVM anywhere: 32 shapes, 12 in 3D, 10
  games, 9 core, 7 generative pieces and 5 text.
- They call the real `libraylib` directly over its C ABI through
  `jolt.ffi`, no wrapper library, no codegen, no C shim. The bindings
  and a keyword-argument drawing API live in one shared namespace,
  `net.b12n.raylib-jlt.raylib`, and each example is a small namespace on
  top of it.
- The interesting part is the ABI. raylib passes structs *by value*
  everywhere and Chez's `foreign-procedure` cannot, so each struct gets
  the treatment its size earns: `Color` rides in one register as a
  packed `:uint`, `Camera2D`/`Camera3D` go by pointer, and
  `Vector2`/`Vector3` geometry is drawn through rlgl's scalar immediate
  mode.
- An animated GIF for every one of the 75, in [`docs/demos/`](docs/demos),
  with a guide and full gallery published at <https://raylib-jlt.b12n.app>.
- A CI workflow that falls back to building raylib from source when the
  distro package is absent.

Other changes:

- `bb record` regenerates the demo GIFs from
  [`scripts/demo_manifest.edn`](scripts/demo_manifest.edn) via
  [screen-grab](https://github.com/burinc/b12n-screen-grab). Maintainer-only.
- `bb lint` / `bb lint:fix`, with clj-kondo taught about `jolt.ffi/defcfn`
  so its bindings stop reading as unresolved symbols. Formatting moved
  from cljfmt to clojure-lsp.
- `bb lib:check` / `bb lib:install` resolve a cross-platform `libraylib`.
- The jolt launcher is resolved rather than hardcoded to `joltc`.
- Third-party attribution split out of `LICENSE` into `NOTICE`, and the
  zlib header added for the vendored raylib material.

## 2026-08-14

- **Breaking:** the namespace root moved from `net.b12n.rljlt` to
  `net.b12n.raylib-jlt`. Anything requiring the old root needs updating.
- Demo recording migrated to [screen-grab](https://github.com/burinc/b12n-screen-grab),
  replacing the repo's own hand-rolled batch capture script.

## 2026-08-03

- Jolt 0.4.0 resolves strictly in definition order, so the `Color`
  section had to move above its first use. The requirement is noted
  under Requirements in the README. It bites any example that grows a
  forward reference.

## 2026-07-18: First cut

- Grew from 42 examples to 75 in a day: 8 assorted, then 6 classic
  games, 7 generative pieces, and two rounds of 6 shapes examples.
- Along the way the shared drawing API picked up what those examples
  needed: `sphere!`, `sector!` (an rlgl fan for filled arcs, which is
  what makes pie charts and colour wheels possible), `ring!`, `line-ex!`,
  `mouse-pressed?`, and `local-time` over libc for the clock examples.
