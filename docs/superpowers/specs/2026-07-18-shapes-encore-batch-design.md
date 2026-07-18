# Design — shapes encore batch (upstream ports): `net.b12n.rljlt` 63 → 69

**Date:** 2026-07-18
**Status:** approved
**Repo:** `burinc/b12n-rljlt` (personal; in-project superpowers docs)

## Goal

Port six of raylib's own `examples/shapes/` demos that we skipped, taking the suite
from 63 to 69. All land in the **existing `shapes` group** (20 → 26) — no new group.
One small toolkit helper lands first (`sector!`), then the six examples. Each example
is one namespace on the shared `net.b12n.rljlt.raylib` layer, wired through the four
touchpoints.

These are direct 1:1 ports from `~/dev/github--raysan5--raylib/examples/shapes/`:
`shapes_rlgl_color_wheel`, `shapes_pie_chart`, `shapes_splines_drawing`,
`shapes_vector_angle`, `shapes_easings_*`, and a Penrose rhombus tiling (the
showpiece, in the spirit of `shapes_penrose_tile`).

## Feasibility notes (probed this session)

- **All `Math/*` used here work in jolt** — `sin`, `cos`, `sqrt`, `PI` (already
  in-tree) **plus `atan2`, `acos`, `pow`, `floor`, `abs`** (probed 2026-07-18 with a
  throwaway `mathprobe` ns under `joltc -M:probe` — every one returned correctly).
  This retires the earlier "avoid `Math/atan2`" caution: `atan2` is safe.
  `vector-angle` uses `atan2`; `easings` uses `pow`.
- **`DrawCircleSector` / `DrawRing` / `DrawSpline*` are NOT bindable** — they take
  `Vector2` (and `Rectangle`) **by value**, which go in FP registers on AArch64 and
  the pointer trick can't fake (the same wall documented in `rlgl-immediate-mode.md`).
  So filled arcs are drawn as **rlgl triangle fans**, and splines are **evaluated in
  pure Clojure** and drawn with `line!`. No new C binds are required for any example.
- **rlgl 2D triangle winding / backface culling** (the gotcha that forced `boids` off
  rlgl triangles): a fan of consistent-winding triangles is culled-or-not as a whole
  (unlike a *rotating* single triangle, which flips winding as it spins). The proven
  front-face order is `shapes_rlgl_color_wheel`'s: per sub-triangle emit
  `rim(θ) → center → rim(θ+dθ)` with **θ increasing**. `sector!` and both fan-drawing
  examples use exactly this order. The `sector!` toolkit task carries its own
  screenshot smoke, so the winding is verified empirically before any example
  consumes it.

## Non-goals

- No textures / fonts / audio (out of scope — needs by-value struct returns in the
  jolt fork).
- No new C binds — `sector!` is pure rlgl built on the existing `rl-begin` /
  `rl-vertex-2f` / `rl-color-4ub` / `rl-end`.
- No raygui (upstream `color_wheel`/`splines` use raygui sliders for interactivity —
  we drop the GUI chrome and drive parameters by animation / keys instead).
- Interaction is minimal; verification is `joltc -M:check` + a `RAYLIB_APP_SHOT`
  screenshot of a representative frame.

## Toolkit addition (task 1): `sector!`

Add one pure-rlgl helper to `raylib.clj` (no new C bind), documented as the filled-arc
counterpart to the by-value-blocked `DrawCircleSector`:

```
(sector! :cx cx :cy cy :radius r :start-deg a0 :end-deg a1 :segments n :color c)
```

- Builds an `n`-triangle fan from the center over `[a0, a1]` degrees.
- Emits each sub-triangle as `rim(k) → center → rim(k+1)` (the color_wheel winding).
- `rim(k)` = `(cx + r·sin θ, cy − r·cos θ)` where `θ = radians(a0 + (a1−a0)·k/n)`
  — so `0°` points up and angle increases clockwise, matching color_wheel.
- Single `color` (packed) via `rl-color!` before the vertices; one `rl-begin
  RL-TRIANGLES` / `rl-end` around the whole fan.

Used by `pie-chart` and `vector-angle`. (`color-wheel` needs a per-vertex hue
gradient, so it draws its own fan with `rl-color-4ub` per vertex rather than
`sector!`.)

**Task-1 smoke:** a throwaway one-sector render proves the winding is front-facing
(non-empty screenshot) before the examples are written. If culled, swap the two `rim`
vertices (one-line) — but color_wheel's order is expected to be correct.

## The six examples (group `shapes`)

Canonical loop; all state threaded through `loop`/`recur`; logic pure. Filenames
underscore, namespaces hyphen. Window 800×450 unless noted.

1. **`color-wheel`** — HSV hue ring as an rlgl triangle fan (port of
   `shapes_rlgl_color_wheel`). `N=64` slices; per slice, `hue = 360·i/N`, rim vertex
   at `center + radius·(sin θ, −cos θ)`. Each triangle: `rim(i)` colored
   `hsv→rgb(hue,1,1)`, `center` gray, `rim(i+1)` colored `hsv→rgb(hue+dhue,1,1)`, via
   `rl-color-4ub` per vertex. `hsv→rgb` implemented in Clojure (h∈[0,360), s=v=1). A
   slow hue rotation (offset by `frame`) keeps it alive. Labels via `text!`.

2. **`pie-chart`** — labelled slices (port of `shapes_pie_chart`). Fixed data (e.g.
   5 categories with values); each slice is a `sector!` in a distinct palette color
   spanning its share of 360°, plus a legend column (colored `rect!` swatch + `text!`
   label + percent). Optional slow whole-chart rotation. Title via `text!`.

3. **`splines`** — Catmull-Rom / cubic-Bézier / uniform-B-spline through animated
   control points (port of `shapes_splines_drawing`, minus raygui). 5 control points
   bobbing vertically with per-point sine phase. Spline **evaluated in pure Clojure**
   and drawn as a `line!` polyline (≥16 samples/segment). SPACE cycles the active type;
   the active type is labelled; control polygon drawn in gray with `circle-lines!` at
   each point. Formulas (all pure, `t∈[0,1]`, points p0..p3 per segment):
   - Catmull-Rom: `0.5·(2p1 + (−p0+p2)t + (2p0−5p1+4p2−p3)t² + (−p0+3p1−3p2+p3)t³)`
   - Cubic Bézier: `(1−t)³p0 + 3(1−t)²t·p1 + 3(1−t)t²·p2 + t³p3`
   - Uniform B-spline: `⅙·((−p0+3p1−3p2+p3)t³ + (3p0−6p1+3p2)t² + (−3p0+3p2)t +
     (p0+4p1+p2))`

4. **`vector-angle`** — the angle between two vectors (port of
   `shapes_vector_angle`). Shared origin near center; vector **A** fixed, vector **B**
   rotates over time (parametric, `frame`-driven). Both drawn with `line!`; each
   endpoint angle via `atan2(−dy, dx)`; the signed angle between them drawn as a filled
   arc via `sector!` at a small radius; the angle in degrees shown via `text!`
   (`format "%.1f deg"`). ASCII text only (`deg`, not `°` — the default font lacks the
   glyph).

5. **`easings`** — a grid of tracks, each animating a ball with a different easing
   function (spirit of `shapes_easings_*`). 12 lanes (4 rows × 3 cols); a normalized
   `t` ping-pongs 0→1→0 (triangle wave from `frame`); each lane maps `t` through a
   distinct easing: linear, in/out/inOut Quad, in/out Sine, inOut Cubic, in Expo, out
   Elastic, out Bounce, in Back, inOut Quart. Each lane: a track `line!`, a `circle!`
   ball at the eased x, and a `text!` label. Easing fns pure (use `pow`, `sin`, `cos`
   — all probed OK).

6. **`penrose-tiling`** — P3 (rhombus) Penrose tiling by deflation — the showpiece.
   Robinson triangles `{:kind 0|1 :a [x y] :b [x y] :c [x y]}` (0 = thin/36°, 1 =
   thick). Seed = 10 triangles around the center forming a decagon "sun"; **deflate
   N=5 times** using golden-ratio `φ=(1+√5)/2` lerps (Preshing's subdivision rules):
   - kind 0 `(A,B,C)`: `P = lerp(A,B,1/φ)` → `[(0,C,P,B), (1,P,C,A)]`
   - kind 1 `(A,B,C)`: `Q = lerp(B,A,1/φ)`, `R = lerp(B,C,1/φ)` →
     `[(1,R,C,A), (1,Q,R,B), (0,R,Q,A)]`

   (`lerp(u,v,s) = u + (v−u)·s`.) After deflation (~a few thousand triangles), scale to
   fit the window and draw: fill each triangle as an rlgl fan / triangle (two palette
   colors by `:kind`) and stroke the two rhombus-interior edges with `line!`. Static
   (or a very slow rotation). Title via `text!`.

## Mechanics per example (four touchpoints)

1. `src/net/b12n/rljlt/<name>.clj`.
2. `deps.edn` alias `:<name> {:main-opts ["-m" "net.b12n.rljlt.<name>"]}`.
3. `check.clj` — add `net.b12n.rljlt.<name>` to the `:require` block.
4. `bb.edn` — add `["<name>" "<name>" "shapes" "<desc>"]` to `examples` + a
   `bb <name>` task. **No `info` group-list change** — `shapes` is already in the list.

The toolkit task (`sector!`) touches only `raylib.clj` (+ a throwaway smoke), no
touchpoints.

## Documentation + wiki (doc-sync, after code lands)

- `README.md` — add 6 rows to the examples table under the shapes cluster; bump the
  "63" counts.
- `docs/guide/example-catalog.md` — add the 6 rows to the `shapes` section; bump title
  "63" → "69" and the shapes-group count.
- `docs/guide/index.md` — "63 raylib examples" / "tour of all 63" → "69".
- `docs/guide/rlgl-immediate-mode.md` — add `sector!` as a documented rlgl-fan helper
  (the filled-arc counterpart to by-value-blocked `DrawCircleSector`).
- Re-mirror changed `docs/guide/*.md` → `~/dev/b12n-wikis/b12n-rljlt/`; bump "63" →
  "69" in the wiki `README.md` row, `CLAUDE.md` quick-ref row, and
  `b12n-rljlt/README.md`. Commit `re-mirror b12n-rljlt @ <sha>`.

## Verification

- Per example: `joltc -M:check` green; screenshot smoke
  `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<name>` — the
  representative frame must render its pattern.
- `sector!` task: throwaway one-sector smoke renders non-empty (winding proven).
- `bb examples` = 69 total; `bb info` shows `shapes (26)`; `bb tasks` parses.
- Clean up `shot-*.png` (gitignored) before committing.

## Sequencing

1. **Toolkit `sector!`** in `raylib.clj` + winding smoke (own commit).
2. **The six examples** (order: color-wheel, pie-chart, splines, vector-angle,
   easings, penrose-tiling), each with its four touchpoints + smoke.
3. **Doc-sync + wiki re-mirror.**
4. **Arc-close**: `bb examples` = 69, `bb info` = `shapes (26)`, screenshot sweep,
   push both repos.
