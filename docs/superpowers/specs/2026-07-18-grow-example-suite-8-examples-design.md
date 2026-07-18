# Design — grow `net.b12n.rljlt` from 42 → 50 examples

**Date:** 2026-07-18
**Status:** approved
**Repo:** `burinc/b12n-rljlt` (personal; in-project superpowers docs)

## Goal

Add the eight CONTINUATION_PROMPT.md "Next ideas" examples to the raylib-in-jolt
suite, plus the two small toolkit additions they require. Take the suite from 42 to
50 examples. Every example stays within the existing architecture: one namespace per
example on top of the shared `net.b12n.rljlt.raylib` binding layer, wired through the
four touchpoints (source ns + `deps.edn` alias + `check.clj` require + `bb.edn`
registry row/task).

## Non-goals

- No new external dependencies; raylib 5.5 + jolt only.
- No textures / shaders / audio (that's the "big frontier" and needs native
  by-value struct support in the jolt fork — out of scope here).
- No test framework: this is a visual example suite. Verification is
  `joltc -M:check` (headless compile) + `RAYLIB_APP_SHOT` screenshot smoke.
- x86-64 portability is not a goal (the camera examples are AArch64-only, as
  documented).

## Toolkit additions — `src/net/b12n/rljlt/raylib.clj`

Two additions; everything else reuses the current surface.

### A. `sphere!` — an rlgl-tessellated 3D sphere (sibling to `cube!`)

`DrawSphere*` all take a `Vector3` by value (unbindable — see the FFI guides), so a
sphere is built from `rl-vertex-3f` like `cube!`. Lat/long tessellation: `rings`
latitude bands × `slices` longitude sectors; each cell is a quad = 2 triangles.
Faces shaded from `:color` via the existing `shade-color`, brighter toward +y, so it
reads 3D without a lighting pass (same intent as `cube!`'s per-face shading). Must be
called inside a `BeginMode3D` block (`with-camera-3d`).

```clojure
(defn sphere!
  "Draw a sphere via rlgl immediate mode (lat/long tessellation), faces shaded from
  the packed :color for depth. Must be called inside a BeginMode3D block (see
  with-camera-3d). Keyword args:
    :pos    [x y z] centre       (default [0 0 0])
    :radius number               (default 0.5)
    :rings  latitude bands       (default 12)
    :slices longitude sectors    (default 16)
    :color  a packed Color       (default BLACK)"
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0] radius 0.5 rings 12 slices 16 color BLACK}}]
  ;; for i in 0..rings-1 (lat0/lat1 in [-π/2, π/2]),
  ;;     j in 0..slices-1 (lon0/lon1 in [0, 2π]):
  ;;   compute the 4 unit-sphere corners, scale by radius, offset by pos,
  ;;   shade the quad by its mid-latitude via shade-color (~0.4..1.0),
  ;;   emit 2 triangles with rl-vertex-3f  (reuse quad-3f).
  )
```

Implementation note: reuse the existing private `quad-3f` (it already emits two
triangles for `[a b c d]` corners with a shaded color) so `sphere!` only computes
corner coordinates + a per-quad brightness. Keep the brightness derivation simple and
deterministic (a function of latitude), matching `cube!`'s look.

### B. Character/key input binds (for `input-box`)

Both return `int` (a queued value; 0 when the queue is empty), so they bind directly:

```clojure
(ffi/defcfn get-char-pressed "GetCharPressed" [] :int)  ; unicode codepoint, 0 = empty
(ffi/defcfn get-key-pressed  "GetKeyPressed"  [] :int)  ; keycode, 0 = empty
(def ^:const KEY-BACKSPACE 259)
(def ^:const KEY-ENTER     257)
```

No kwarg wrappers needed — these are simple scalar-return binds like `get-mouse-x`.

## The eight examples

Each is `src/net/b12n/rljlt/<name>.clj` with a `-main` running the canonical loop
(`window!` → `set-target-fps` → `auto-quit-deadline` → `keep-running?` loop with
`begin-drawing`/`clear-background`/…/`maybe-screenshot!`/`end-drawing` → `close-window`).
Filenames use underscores; namespaces use hyphens.

### 3D group (adds 5 → group 7→12)

1. **`rotating-cube`** (alias `rotating-cube`) — a fixed `with-camera-3d`, `draw-grid`,
   and one `cube!` at the origin wrapped in `rl-push-matrix` + `rl-rotatef`
   (frame-driven angle on X and Y) + `rl-pop-matrix`. The minimal "first 3D
   transform" demo.

2. **`spinning-cubes`** (alias `spinning-cubes`) — a short row (e.g. 5) of cubes,
   each spinning **in place** with a per-index phase offset. Deliberately distinct
   from `waving-cubes` (which translates a grid in a ripple); this one rotates.

3. **`orthographic-projection`** (alias `orthographic-projection`) — a scene of a few
   cubes + grid rendered under perspective vs orthographic; SPACE toggles
   `with-camera-3d`'s `:projection` (0 = perspective, 1 = orthographic). For ortho,
   `fovy` is the ortho view height (raylib convention) so use a fitted value
   (~10) vs ~45 for perspective. On-screen label shows the current mode.

4. **`point-cloud`** (alias `point-cloud`) — ~2000 random 3D points (seeded via
   `get-random-value`) rendered as tiny `cube!`s (size ~0.05), the whole cloud slowly
   rotating via the matrix stack, each point colored by its position (xyz → rgb).
   (rlgl has no `RL_POINTS` mode, so tiny cubes are the point primitive.)

5. **`bouncing-spheres`** (alias `bouncing-spheres`) — several spheres in a 3D box
   `[-w..w]³`, gravity on y, bouncing off all six walls with restitution; drawn with
   the new `sphere!` inside `with-camera-3d` over a `draw-grid` floor. The showcase
   for `sphere!`.

### shapes group (adds 2 → group 18→20)

6. **`ball-physics`** (alias `ball-physics`) — N 2D balls `{pos vel radius color}`;
   each frame `vel.y += gravity`, integrate, reflect off window edges with
   restitution; drawn with `circle!`. SPACE resets / adds a ball.

7. **`lines-bezier`** (alias `lines-bezier`) — a cubic Bézier whose endpoint follows
   the mouse. `DrawLineBezier` takes `Vector2` by value (unbindable), so sample the
   curve in Clojure — `B(t) = (1-t)³p0 + 3(1-t)²t p1 + 3(1-t)t² p2 + t³ p3` over ~30
   `t` steps — and draw consecutive samples with `line!`; mark the four control
   points with `circle!`.

### text group (adds 1 → group 4→5)

8. **`input-box`** (alias `input-box`) — a text field. Each frame: drain
   `get-char-pressed` while > 0, appending printable chars (32–125) up to a max length
   (~20); `key-pressed? KEY-BACKSPACE` drops the last char; a blinking cursor (toggle
   on a frame counter); `ENTER` clears. Draw the box with `rect-lines!`, the text with
   `text!`, and a `N/20` char counter. Mirrors raylib's `text_input_box`.

> Group-count note: `input-box` is the only new `text` example (4→**5**), placed there
> per raylib's own `text/` categorization of `text_input_box`. The other seven split
> 5×3d + 2×shapes, for 8 total (42 → 50).

## Mechanics per example (the four touchpoints)

1. `src/net/b12n/rljlt/<name>.clj` — new namespace + `-main`.
2. `deps.edn` — `:<alias> {:main-opts ["-m" "net.b12n.rljlt.<name>"]}`.
3. `check.clj` — add `net.b12n.rljlt.<name>` to the `:require` block.
4. `bb.edn` — add `["<name>" "<alias>" "<group>" "<desc>"]` to the `examples`
   registry vector **and** a matching `bb <name>` task.

## Documentation + wiki (doc-sync)

Done after the code lands (per the hold-doc-sync-until-after rule):

- `README.md` — add 8 rows to the examples table; the Layout section's example
  count is prose-free so no count edit needed there.
- `docs/guide/example-catalog.md` — add the 8 rows to their groups; update the group
  headings (3d 7→12, shapes 18→20, text 4→5) and any "42" total → "50".
- `docs/guide/rlgl-immediate-mode.md` — one line noting `sphere!` joins `cube!` as an
  rlgl 3D primitive.
- `docs/guide/headless-smoke-testing.md` / kwarg page — mention the new input binds
  only if a natural spot exists (optional; don't force it).
- Re-mirror `docs/guide/*.md` → `~/dev/b12n-wikis/b12n-rljlt/`, and refresh the "42
  examples" mentions to "50" in the wiki `README.md` row, `CLAUDE.md` quick-ref row,
  and `PATTERNS.md` if present. Commit with `re-mirror b12n-rljlt @ <sha>`.

## Verification

- After each example: `joltc -M:check` stays green ("net.b12n.rljlt: all example
  namespaces compiled OK").
- Screenshot smoke per windowed example:
  `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<alias>`,
  then eyeball the PNG. (Needs an awake display.)
- `bb info` lists 50 examples grouped; `bb tasks` parses.
- Clean up `shot-*.png` (gitignored) before committing.

## Sequencing

Toolkit first (so dependents compile), then examples, then docs:

1. **Toolkit**: add `sphere!` + the input binds/consts to `raylib.clj`; `-M:check`.
2. **Pure-reuse examples** (6): rotating-cube, spinning-cubes, orthographic-projection,
   point-cloud, ball-physics, lines-bezier — each with its 4 touchpoints + smoke.
3. **Toolkit-dependent examples** (2): bouncing-spheres (`sphere!`), input-box (binds).
4. **Doc-sync + wiki re-mirror.**
5. **Arc-close verification**: `bb info` shows 50, full screenshot sweep, commit(s).
