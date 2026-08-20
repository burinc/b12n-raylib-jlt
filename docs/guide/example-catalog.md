# The example catalog — 75 raylib demos in jolt

A map of the whole suite. Each example is one namespace under
`src/net/b12n/raylib_jlt/`, runnable by a friendly `bb <name>` task or the underlying
`jolt -M:<alias>`. `bb info` prints this grouping live; this page adds the "how it's
wired" recipe at the end.

Run one, list them, or reel through all of them:

```sh
bb <name>          # e.g. bb asteroids   (opens a window)
bb examples        # flat list with descriptions
bb info            # the grouped cheat-sheet below
bb run-all [secs]  # every example, N seconds each (unattended)
```

## games (10)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/asteroids.gif" width="80">](demos.md#asteroids) | `asteroids` | the classic vector shooter — rotate / thrust / fire, splitting asteroids |
| [<img src="../demos/tetris.gif" width="80">](demos.md#tetris) | `tetris` | 10×20 well, 7 tetrominoes, rotation, line-clearing, levels |
| [<img src="../demos/pong.gif" width="80">](demos.md#pong) | `pong` | two-paddle classic, you (W/S) vs a ball-tracking CPU |
| [<img src="../demos/vampire-survivors.gif" width="80">](demos.md#vampire-survivors) | `vampire-survivors` | auto-firing survivors-like — chasing waves, XP gems, leveling |
| [<img src="../demos/snake.gif" width="80">](demos.md#snake) | `snake` | the classic snake (arrow keys, grow, don't crash) |
| [<img src="../demos/breakout.gif" width="80">](demos.md#breakout) | `breakout` | paddle + ball + brick grid (mouse paddle) |
| [<img src="../demos/space-invaders.gif" width="80">](demos.md#space-invaders) | `space-invaders` | marching aliens (arrows + SPACE to shoot) |
| [<img src="../demos/flappy-bird.gif" width="80">](demos.md#flappy-bird) | `flappy-bird` | flap through the pipe gaps (SPACE) |
| [<img src="../demos/game-2048.gif" width="80">](demos.md#game-2048) | `game-2048` | 2048: 4x4 tile-merge puzzle (arrow keys) |
| [<img src="../demos/minesweeper.gif" width="80">](demos.md#minesweeper) | `minesweeper` | reveal/flag grid (mouse L reveal, R flag) |

## core (9)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/basic-window.gif" width="80">](demos.md#basic-window) | `basic-window` | the minimal window + text (the default, `jolt -M:run`) |
| [<img src="../demos/input-keys.gif" width="80">](demos.md#input-keys) | `input-keys` | `IsKeyDown`, steer a ball with the arrow keys |
| [<img src="../demos/input-mouse.gif" width="80">](demos.md#input-mouse) | `input-mouse` | `GetMouseX/Y`, `IsMouseButtonDown`, click to recolor |
| [<img src="../demos/input-mouse-wheel.gif" width="80">](demos.md#input-mouse-wheel) | `input-mouse-wheel` | `GetMouseWheelMove` (a float return) scrolls a box |
| [<img src="../demos/camera-2d.gif" width="80">](demos.md#camera-2d) | `camera-2d` | a 2D camera over a skyline — struct-by-value (see below) |
| [<img src="../demos/delta-time.gif" width="80">](demos.md#delta-time) | `delta-time` | per-frame vs `GetFrameTime` movement |
| [<img src="../demos/scissor-test.gif" width="80">](demos.md#scissor-test) | `scissor-test` | `BeginScissorMode` clips a grid |
| [<img src="../demos/basic-screen-manager.gif" width="80">](demos.md#basic-screen-manager) | `basic-screen-manager` | a LOGO / TITLE / GAMEPLAY / ENDING state flow |
| [<img src="../demos/random-values.gif" width="80">](demos.md#random-values) | `random-values` | `GetRandomValue`, a new value every 2s |

## shapes (32)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/bouncing-ball.gif" width="80">](demos.md#bouncing-ball) | `bouncing-ball` | animation, `IsKeyPressed` pause, `DrawFPS` |
| [<img src="../demos/basic-shapes.gif" width="80">](demos.md#basic-shapes) | `basic-shapes` | rect / circle / ellipse / line + an rlgl triangle |
| [<img src="../demos/colors-palette.gif" width="80">](demos.md#colors-palette) | `colors-palette` | every named raylib color in a grid |
| [<img src="../demos/gradient.gif" width="80">](demos.md#gradient) | `gradient` | `DrawRectangleGradientV` (two by-value Colors) |
| [<img src="../demos/following-eyes.gif" width="80">](demos.md#following-eyes) | `following-eyes` | pupils track the mouse (scalar trig) |
| [<img src="../demos/starfield.gif" width="80">](demos.md#starfield) | `starfield` | `GetRandomValue` + bulk draw, per-star twinkle |
| [<img src="../demos/logo-raylib.gif" width="80">](demos.md#logo-raylib) | `logo-raylib` | rectangles + text, positioned with `MeasureText` |
| [<img src="../demos/mouse-trail.gif" width="80">](demos.md#mouse-trail) | `mouse-trail` | a fading cursor trail (alpha) |
| [<img src="../demos/recursive-tree.gif" width="80">](demos.md#recursive-tree) | `recursive-tree` | a binary fractal tree (lines + trig) |
| [<img src="../demos/math-sine-cosine.gif" width="80">](demos.md#math-sine-cosine) | `math-sine-cosine` | a live unit-circle sine/cosine visualization |
| [<img src="../demos/bullet-hell.gif" width="80">](demos.md#bullet-hell) | `bullet-hell` | a rotating bullet spiral |
| [<img src="../demos/triangle-strip.gif" width="80">](demos.md#triangle-strip) | `triangle-strip` | a rainbow strip via rlgl immediate mode |
| [<img src="../demos/collision-area.gif" width="80">](demos.md#collision-area) | `collision-area` | AABB collision between two boxes (computed in Clojure) |
| [<img src="../demos/dashed-line.gif" width="80">](demos.md#dashed-line) | `dashed-line` | a dashed line follows the mouse |
| [<img src="../demos/double-pendulum.gif" width="80">](demos.md#double-pendulum) | `double-pendulum` | chaotic double-pendulum motion + trail |
| [<img src="../demos/kaleidoscope.gif" width="80">](demos.md#kaleidoscope) | `kaleidoscope` | strokes mirrored with 6-fold symmetry |
| [<img src="../demos/hilbert-curve.gif" width="80">](demos.md#hilbert-curve) | `hilbert-curve` | a rainbow Hilbert space-filling curve |
| [<img src="../demos/math-angle-rotation.gif" width="80">](demos.md#math-angle-rotation) | `math-angle-rotation` | fixed spokes + a spinning line |
| [<img src="../demos/ball-physics.gif" width="80">](demos.md#ball-physics) | `ball-physics` | 2D balls under gravity, SPACE respawns |
| [<img src="../demos/lines-bezier.gif" width="80">](demos.md#lines-bezier) | `lines-bezier` | a cubic Bézier that follows the mouse |
| [<img src="../demos/color-wheel.gif" width="80">](demos.md#color-wheel) | `color-wheel` | an HSV color wheel drawn as an rlgl triangle fan (per-vertex hue) |
| [<img src="../demos/pie-chart.gif" width="80">](demos.md#pie-chart) | `pie-chart` | labelled pie slices via `rl/sector!` + a legend |
| [<img src="../demos/splines.gif" width="80">](demos.md#splines) | `splines` | Catmull-Rom / cubic-Bézier / uniform-B-spline through animated points (SPACE cycles) |
| [<img src="../demos/vector-angle.gif" width="80">](demos.md#vector-angle) | `vector-angle` | the signed angle between two vectors, filled arc + degrees readout (`atan2`) |
| [<img src="../demos/easings.gif" width="80">](demos.md#easings) | `easings` | a 3×4 grid of balls, each animating on a different easing curve |
| [<img src="../demos/penrose-tiling.gif" width="80">](demos.md#penrose-tiling) | `penrose-tiling` | a P3 Penrose rhombus tiling built by golden-ratio deflation |
| [<img src="../demos/analog-clock.gif" width="80">](demos.md#analog-clock) | `analog-clock` | a live analog clock (bezel `ring!`, ticks + hands `line-ex!`, libc local time) |
| [<img src="../demos/digital-clock.gif" width="80">](demos.md#digital-clock) | `digital-clock` | a seven-segment `HH:MM:SS` display (libc local time) |
| [<img src="../demos/ring-drawing.gif" width="80">](demos.md#ring-drawing) | `ring-drawing` | an animated annulus via `rl/ring!` + a stroked outline |
| [<img src="../demos/rounded-rectangle.gif" width="80">](demos.md#rounded-rectangle) | `rounded-rectangle` | rounded rectangles built from `sector!` corners + rects |
| [<img src="../demos/rectangle-scaling.gif" width="80">](demos.md#rectangle-scaling) | `rectangle-scaling` | drag the bottom-right corner handle to resize a rectangle |
| [<img src="../demos/lines-drawing.gif" width="80">](demos.md#lines-drawing) | `lines-drawing` | a rotating fan of thick lines via `rl/line-ex!` (rlgl quads) |

## text (5)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/font-sizes.gif" width="80">](demos.md#font-sizes) | `font-sizes` | font sizes + `MeasureText` centering |
| [<img src="../demos/writing-anim.gif" width="80">](demos.md#writing-anim) | `writing-anim` | a self-typing message |
| [<img src="../demos/format-text.gif" width="80">](demos.md#format-text) | `format-text` | `format` padded score + MM:SS timer |
| [<img src="../demos/words-alignment.gif" width="80">](demos.md#words-alignment) | `words-alignment` | align a word inside a box with `MeasureText` |
| [<img src="../demos/input-box.gif" width="80">](demos.md#input-box) | `input-box` | type into a text box (GetCharPressed) |

## 3d (12)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/camera-3d.gif" width="80">](demos.md#camera-3d) | `camera-3d` | an orbiting 3D camera — `Camera3D` by value + rlgl cube |
| [<img src="../demos/waving-cubes.gif" width="80">](demos.md#waving-cubes) | `waving-cubes` | 196 cubes rippling in 3D (shared `rl/cube!`) |
| [<img src="../demos/camera-3d-first-person.gif" width="80">](demos.md#camera-3d-first-person) | `camera-3d-first-person` | WASD + mouse-look walk through columns |
| [<img src="../demos/tesseract-view.gif" width="80">](demos.md#tesseract-view) | `tesseract-view` | a rotating 4D hypercube projected 4D→3D→2D |
| [<img src="../demos/wireframe-shapes.gif" width="80">](demos.md#wireframe-shapes) | `wireframe-shapes` | pyramid / octahedron / torus / helix as rlgl 3D lines |
| [<img src="../demos/rlgl-solar-system.gif" width="80">](demos.md#rlgl-solar-system) | `rlgl-solar-system` | Sun / Earth / Moon via the rlgl matrix stack |
| [<img src="../demos/box-collisions.gif" width="80">](demos.md#box-collisions) | `box-collisions` | a player cube vs. boxes, 3D AABB highlight |
| [<img src="../demos/rotating-cube.gif" width="80">](demos.md#rotating-cube) | `rotating-cube` | a single cube spinning via the rlgl matrix stack |
| [<img src="../demos/spinning-cubes.gif" width="80">](demos.md#spinning-cubes) | `spinning-cubes` | a row of cubes each spinning with a phase offset |
| [<img src="../demos/orthographic-projection.gif" width="80">](demos.md#orthographic-projection) | `orthographic-projection` | perspective vs orthographic (SPACE toggles) |
| [<img src="../demos/point-cloud.gif" width="80">](demos.md#point-cloud) | `point-cloud` | ~1500 points as tiny rlgl cubes, rotating |
| [<img src="../demos/bouncing-spheres.gif" width="80">](demos.md#bouncing-spheres) | `bouncing-spheres` | spheres bouncing in a 3D box (rl/sphere!) |

The 3D set stands entirely on two building blocks from
[`rlgl-immediate-mode.md`](rlgl-immediate-mode.md) and
[`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md): `with-camera-3d`
(the camera, by pointer) and `cube!` (the geometry, by rlgl vertices).

## generative (7)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/game-of-life.gif" width="80">](demos.md#game-of-life) | `game-of-life` | Conway's Game of Life (SPACE reseeds) |
| [<img src="../demos/boids.gif" width="80">](demos.md#boids) | `boids` | flocking birds (separation/alignment/cohesion) |
| [<img src="../demos/fireworks.gif" width="80">](demos.md#fireworks) | `fireworks` | rockets + fading particle bursts |
| [<img src="../demos/fourier-epicycles.gif" width="80">](demos.md#fourier-epicycles) | `fourier-epicycles` | rotating circles trace a square wave |
| [<img src="../demos/spirograph.gif" width="80">](demos.md#spirograph) | `spirograph` | animated hypotrochoid roulette curves |
| [<img src="../demos/l-system.gif" width="80">](demos.md#l-system) | `l-system` | an L-system fractal plant (grows + regrows) |
| [<img src="../demos/flow-field.gif" width="80">](demos.md#flow-field) | `flow-field` | particles steered by a flow field (trails) |

All seven are pure math + the drawing API — no new bindings. They showcase the
suite as a generative-art canvas: cellular automata, agent flocking, particle
systems, rotating-vector Fourier series, parametric roulette curves, string-rewrite
fractals, and noise-steered flow fields.

## Adding an example — the four touchpoints

The suite is deliberately mechanical to grow. One new example touches four places:

1. **Source** — `src/net/b12n/raylib_jlt/<name>.clj`, a namespace with a `-main` that
   runs the canonical loop (see [`headless-smoke-testing.md`](headless-smoke-testing.md))
   against the `net.b12n.raylib-jlt.raylib` API.
2. **`deps.edn` alias** — `:<name> {:main-opts ["-m" "net.b12n.raylib-jlt.<name>"]}` so
   `jolt -M:<name>` works.
3. **`check.clj` require** — add `net.b12n.raylib-jlt.<name>` to the `:require` list in
   `net.b12n.raylib-jlt.check`, so the headless compile-check covers it.
4. **`bb.edn` registry row** — add `["<display-name>" "<alias>" "<group>" "<desc>"]`
   to the `examples` vector and a matching `bb <name>` task, so it shows in
   `bb info` / `bb examples` / `bb run-all`.

```mermaid
flowchart LR
  src["src/…/<name>.clj<br/>the example"] --> deps["deps.edn<br/>:<name> alias"]
  src --> chk["check.clj<br/>require (headless compile)"]
  src --> bb["bb.edn<br/>registry row + task"]
```

Filenames use underscores (`basic_screen_manager.clj`); the namespace uses hyphens
(`net.b12n.raylib-jlt.basic-screen-manager`) — Clojure's standard file↔ns mapping.

One ordering rule applies inside every file: since **jolt 0.4.0** an unresolved
symbol is a compile error rather than a late-bound reference, so a definition must
appear before its first use — in a fn body and in an `:or` destructuring default
just as much as at top level. It bites hardest in the shared `raylib.clj` binding
layer, where one misordered symbol stops *every* example from loading and only the
first offender is reported. `jolt -M:check` is the quick confirmation; see the
[jolt note in the README](../../README.md#requirements).

## See also

- [`kwarg-drawing-api.md`](kwarg-drawing-api.md) — the API every example is written
  against.
- [`headless-smoke-testing.md`](headless-smoke-testing.md) — the loop shape and how
  `bb run-all` / `bb check` verify the catalog.
