# Changelog

Notable changes to b12n-raylib-jlt, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://raylib-jlt.b12n.app>.

## Unreleased

- `bb docs-sync` says which kind of deploy failure it hit — unreachable
  AWS, missing or expired credentials, a 403, or a genuinely absent
  bucket — instead of blaming missing infrastructure for all four and
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

## 2026-08-20 — Public launch

Highlights:

- 75 raylib examples in [Jolt](https://github.com/jolt-lang/jolt), native
  Clojure on Chez Scheme, with no JVM anywhere: 32 shapes, 12 in 3D, 10
  games, 9 core, 7 generative pieces and 5 text.
- They call the real `libraylib` directly over its C ABI through
  `jolt.ffi` — no wrapper library, no codegen, no C shim. The bindings
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
  under Requirements in the README — it bites any example that grows a
  forward reference.

## 2026-07-18 — First cut

- Grew from 42 examples to 75 in a day: 8 assorted, then 6 classic
  games, 7 generative pieces, and two rounds of 6 shapes examples.
- Along the way the shared drawing API picked up what those examples
  needed: `sphere!`, `sector!` (an rlgl fan for filled arcs, which is
  what makes pie charts and colour wheels possible), `ring!`, `line-ex!`,
  `mouse-pressed?`, and `local-time` over libc for the clock examples.
