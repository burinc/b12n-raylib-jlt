# Design — generative / math art batch: `net.b12n.rljlt` 56 → 63

**Date:** 2026-07-18
**Status:** approved
**Repo:** `burinc/b12n-rljlt` (personal; in-project superpowers docs)

## Goal

Add seven generative / mathematical-art examples in a new `generative` group, taking
the suite from 56 to 63. Each is one namespace on the shared `net.b12n.rljlt.raylib`
layer, wired through the four touchpoints. **No toolkit additions** — all pure reuse
of the drawing API + `Math/sin`/`cos` (verified working in jolt) + `rgba` alpha.

## Non-goals

- No new `raylib.clj` binds (the batch is deliberately pure-reuse).
- No textures/audio.
- Gameplay/interaction is minimal (SPACE reseed where relevant); verification is
  `joltc -M:check` + a `RAYLIB_APP_SHOT` screenshot of the animating frame.

## New group: `generative`

These seven form a coherent cluster distinct from the `shapes` primitives. Add a
`generative` group:
- Each registry row is tagged `"generative"`.
- `bb.edn`'s `info` task iterates a hard-coded group order
  `["games" "core" "shapes" "text" "3d"]`; append `"generative"` so the group renders
  in `bb info`. (Examples with a group not in that list still appear in `bb examples`
  and `bb run-all`, but would be invisible in `bb info` — so this one-line change is
  required.)

## The seven examples (group `generative`)

Each `-main` runs the canonical loop; all state threaded through `loop`/`recur`; all
logic pure. Filenames underscore, namespaces hyphen. Window 800×450 unless noted.

1. **`game-of-life`** — Conway's cellular automaton. State is a **set of live `[c r]`
   cells** on a `cols×rows` grid (cell ~10 px). Next generation (toroidal wrap via
   `mod`): candidate cells = live cells ∪ their 8 neighbors; a cell lives next gen if
   (live ∧ 2–3 live neighbors) ∨ (dead ∧ exactly 3). Advance every ~6 frames. Random
   seed (~28% alive via `get-random-value`); SPACE reseeds. Live cells drawn with
   `rect!`.

2. **`boids`** — Reynolds flocking. ~70 boids `{:x :y :vx :vy}`. Per frame, per boid,
   over neighbors within a radius: **separation** (steer away from close ones),
   **alignment** (match average velocity), **cohesion** (steer toward average
   position); add weighted, clamp speed, wrap edges. Each boid drawn as a small
   **rlgl triangle** pointing along its velocity (`rl-begin RL-TRIANGLES` + three
   `rl-vertex-2f` from the heading angle).

3. **`fireworks`** — Particle bursts. Rockets `{:x :y :vy}` rise under gravity; at
   apex (`vy >= 0`) each explodes into ~40 particles `{:x :y :vx :vy :life :color}`.
   Particles fall under gravity, `life` decays 1→0; drawn with `circle!` whose color
   is `(rgba r g b (int (* 255 life)))` so they **fade via the alpha channel**
   (raylib's default BLEND_ALPHA). A new rocket launches every ~35 frames; dead
   particles/rockets are filtered out.

4. **`fourier-epicycles`** — A chain of rotating circles (a Fourier series for a
   **square wave**: terms `sin((2k-1)θ)/(2k-1)`, k=1..N). From a fixed left-center,
   each epicycle adds a rotating vector; draw each circle with `circle-lines!` and the
   connecting radius with `line!`; the chain tip traces a path (accumulate ~N points,
   draw as connected `line!` segments). θ advances each frame; the path scrolls.

5. **`spirograph`** — Animated hypotrochoid. Parametric with fixed `R`, moving `r`,
   pen offset `d`: `x = (R-r)cos t + d cos((R-r)/r · t)`, `y = (R-r)sin t − d
   sin((R-r)/r · t)`. Advance `t`, accumulate points, draw as connected `line!`
   segments colored by index (rainbow). When `t` completes the closing period, reset
   with new random `r`/`d`.

6. **`l-system`** — A fractal plant via Lindenmayer rewriting. Axiom `"X"`, rules
   `X → F+[[X]-X]-F[-FX]+X`, `F → FF`, angle 25°, **N = 4** iterations (bounded — the
   segment list stays a few thousand). Interpret with a turtle: `F` = forward `line!`,
   `+`/`-` = rotate, `[`/`]` = push/pop `(pos, angle)` on a stack. Precompute the
   segment list once, then **animate the reveal** (show `K` segments, `K` grows each
   frame) for a "growing plant"; reset when fully drawn.

7. **`flow-field`** — Particles steered by a smooth field. The field angle at `(x,y)`
   is a **sine-layered pseudo-noise**: `angle = 2π·(sin(x·0.008 + t) + cos(y·0.008 −
   t))·0.5` (deterministic, smooth — no external noise). ~500 particles `{:x :y}`
   step by `(cos angle, sin angle)·speed`, wrapping edges. **Trails accumulate**: draw
   a translucent black `rect!` (`(rgba 0 0 0 18)`) over the whole window each frame
   instead of `clear-background` (clear only on frame 0), then draw particle dots
   colored by angle. `t` advances slowly.

## Mechanics per example (four touchpoints)

1. `src/net/b12n/rljlt/<name>.clj`.
2. `deps.edn` alias `:<name> {:main-opts ["-m" "net.b12n.rljlt.<name>"]}`.
3. `check.clj` — add `net.b12n.rljlt.<name>` to the `:require` block.
4. `bb.edn` — add `["<name>" "<name>" "generative" "<desc>"]` to `examples` + a
   `bb <name>` task; **and** append `"generative"` to the `info` task's group list.

## Documentation + wiki (doc-sync, after code lands)

- `README.md` — add 7 rows to the examples table (a new generative cluster).
- `docs/guide/example-catalog.md` — add a new `## generative (7)` section with the 7
  rows; bump the title "56" → "63".
- `docs/guide/index.md` — "56 raylib examples" / "tour of all 56" → "63".
- Re-mirror the changed `docs/guide/*.md` → `~/dev/b12n-wikis/b12n-rljlt/`; bump "56" →
  "63" in the wiki `README.md` row, `CLAUDE.md` quick-ref row, and `b12n-rljlt/README.md`.
  Commit `re-mirror b12n-rljlt @ <sha>`.

## Verification

- Per example: `joltc -M:check` green; screenshot smoke
  `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<name>` — the
  animating frame must render (motion-based ones show their pattern a few frames in).
- `bb info` shows a `generative (7)` group and 63 total; `bb tasks` parses.
- Clean up `shot-*.png` (gitignored) before committing.

## Sequencing

1. **The 7 examples** (order: game-of-life, boids, fireworks, fourier-epicycles,
   spirograph, l-system, flow-field), each with its four touchpoints + smoke. The
   `bb.edn` `info` group-list change lands with the first example (so `bb info` shows
   the group as examples accrue).
2. **Doc-sync + wiki re-mirror.**
3. **Arc-close**: `bb info` = 63 with `generative (7)`, screenshot sweep, push both.
