# net.b12n.rljlt — Guide

User-facing documentation for `net.b12n.rljlt`: a suite of **[raylib](https://github.com/raysan5/raylib)
examples written in [jolt](https://github.com/jolt-lang)** (native Clojure on Chez
Scheme, no JVM) over `jolt.ffi`. Each page below covers one FFI pattern or drawing
convention, with citations to source files and cross-references to sibling projects
in the [b12n umbrella wiki](https://github.com/burinc/b12n-wikis).

## Why this exists

When `net.b12n.rljlt` is mirrored into
[`b12n-wikis`](https://github.com/burinc/b12n-wikis), each page below becomes an
entry under `b12n-wikis/b12n-rljlt/`, and the wiki's `PATTERNS.md` cross-project
index cites them for any Jolt-FFI-distinctive pattern.

## What net.b12n.rljlt is

A community suite of 63 raylib examples — the classic core/shapes/text demos, a
handful of games (asteroids, tetris, pong, vampire-survivors), and a 3D set
(orbiting cameras, waving cubes, an rlgl solar system) — each a small Clojure
namespace on top of one shared binding layer, `net.b12n.rljlt.raylib`.

It is the **graphics sibling** of
[`b12n-tsj`](https://github.com/burinc/b12n-tsj) (tree-sitter from Jolt). Both bind
a real external C library directly over its C ABI with `jolt.ffi` — Chez
`foreign-procedure` under the hood. They differ on how hard the library leans on
**by-value structs**, and that difference is the whole story of the FFI pages here:

> **raylib hits the *mild* version of struct-by-value — its hot path is `Color`,
> a 4-byte struct that reduces to a `uint32`, and its only large structs
> (`Camera2D`/`Camera3D`) are by-value *arguments* it can fake behind a pointer on
> AArch64. tree-sitter hits the *severe* version — a 32-byte `TSNode` passed AND
> returned by value — so it needs a full C shim. raylib needs none.**

Three ABI facts drive every distinctive decision in this repo:

1. **`Color` packs into a `:uint`** — a 4-byte all-integer struct travels in one
   general-purpose register, so every draw call passes color as an int, no struct
   marshaling. ([`color-by-value.md`](color-by-value.md))
2. **`Camera2D`/`Camera3D` go by pointer** — a >16-byte composite is passed
   indirectly on AArch64, so a `[:pointer]` binding + a hand-built native struct
   works (with an x86-64 caveat). ([`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md))
3. **`Vector2`/`Vector3` geometry uses rlgl** — small float structs go in FP
   registers, which the pointer trick does *not* cover, so shapes and 3D cubes are
   drawn with rlgl's scalar immediate mode instead.
   ([`rlgl-immediate-mode.md`](rlgl-immediate-mode.md))

## Capability pages

### The FFI core (the reason this repo is interesting)

- ✅ [`color-by-value.md`](color-by-value.md) — why raylib's `Color` crosses the
  FFI boundary as a packed `:uint` and not a struct, the little-endian `rgba`
  packing, and the two-by-value-Colors-in-one-call case (`DrawRectangleGradientV`).
  Source: `src/net/b12n/rljlt/raylib.clj` (`rgba`, `clear-background`, the palette).
- ✅ [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md) — how a
  24-byte `Camera2D` / 44-byte `Camera3D` is passed by value on AArch64 by
  allocating the struct in native memory and binding `BeginMode2D`/`BeginMode3D` as
  `[:pointer]`. **The x86-64 non-portability caveat is here.** Source:
  `raylib.clj` (`with-camera-2d`, `with-camera-3d`).
- ✅ [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md) — the fallback for by-value
  `Vector2`/`Vector3` args the pointer trick can't fake: rlgl scalar immediate mode
  (`rlBegin`/`rlVertex2f`/`rlVertex3f`/`rlColor4ub`) for the 2D triangle and the 3D
  `cube!`, plus the rlgl matrix stack for nested transforms (the solar-system demo).
  Source: `raylib.clj` (`cube!`, `quad-3f`, the `rl-*` binds).

### The drawing API

- ✅ [`kwarg-drawing-api.md`](kwarg-drawing-api.md) — the two-layer design: raw
  positional `ffi/defcfn` binds at the boundary (mirroring C), ergonomic
  keyword-argument wrappers (`text!`/`rect!`/`circle!`/…) on top, and the
  ">3 arguments → keyword args" style convention. Source: `raylib.clj`.

### Verifying without a display

- ✅ [`headless-smoke-testing.md`](headless-smoke-testing.md) — how a windowed
  example proves itself with no person at the keyboard: `RAYLIB_APP_AUTO_QUIT_MS`
  (auto-close), `RAYLIB_APP_SHOT` (dump one PNG), and the batched-geometry flush
  that makes the screenshot non-empty. Source: `raylib.clj` (`auto-quit-deadline`,
  `keep-running?`, `maybe-screenshot!`).

### Orientation

- ✅ [`example-catalog.md`](example-catalog.md) — a tour of all 63 examples grouped
  games / core / shapes / text / 3d / generative, what each demonstrates, and the four-touchpoint
  recipe for adding one (source ns + `deps.edn` alias + `check.clj` require +
  `bb.edn` registry row). Read this for the map; the FFI pages for the mechanics.

## See also

- [`b12n-tsj`](https://github.com/burinc/b12n-tsj) — the Jolt sibling that binds a
  by-value-*returning* C API (tree-sitter) and therefore needs the full C shim this
  repo avoids. The [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md)
  page is the delta between "fake it with a pointer" (raylib arguments) and "you
  can't, write a shim" (tree-sitter returns).
