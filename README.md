# net.b12n.rljlt — raylib examples in jolt

A community suite of [raylib](https://github.com/raysan5/raylib) examples written in
**jolt** (native Clojure — no JVM). These call a **real external C library**: raylib is
the upstream C game library (raysan5/raylib), and jolt binds it directly over its C ABI
with `jolt.ffi` — no wrapper library, no codegen, just the shared `libraylib` loaded at
runtime and called through the FFI.

All the FFI bindings and an ergonomic keyword-argument drawing API live in one
shared namespace, `net.b12n.rljlt.raylib`; each example is a small namespace on top of it.

## Requirements

The system `libraylib` shared library must be installed:

```sh
brew install raylib          # macOS
# or your distro's raylib package on Linux
```

`deps.edn` points jolt at it via a `:jolt/native` entry (Homebrew's
`/opt/homebrew/lib/libraylib.dylib` first, then the bare name on the loader path;
`libraylib.so` on Linux).

## Running

### With Babashka (friendliest)

With [babashka](https://babashka.org) installed, `bb.edn` gives every example a task:

```sh
bb info                 # grouped cheat-sheet of every example
bb examples             # flat list with descriptions
bb bouncing-ball        # run one example (opens a window)
bb run following-eyes   # …or run one by argument
bb run-all [secs]       # demo reel / smoke test: every example, N seconds each (default 15)
bb check                # headless compile-check of every example (no window)
bb tasks                # raw babashka task list
```

The `bb` names are friendly aliases; each maps to a `joltc` alias below.

### With joltc directly

Each example is also a `joltc` alias:

```sh
joltc -M:run        # basic window (default)      joltc -M:mouse     # circle follows cursor
joltc -M:input      # move a circle, arrow keys   joltc -M:wheel     # move a box, mouse wheel
joltc -M:bounce     # bouncing ball, SPACE pauses  joltc -M:shapes    # shape primitives + triangle
joltc -M:colors     # named-color palette grid     joltc -M:gradient  # vertical color gradient
joltc -M:text       # font sizes + MeasureText     joltc -M:logo      # the raylib logo
joltc -M:stars      # twinkling starfield          joltc -M:eyes      # eyes follow the mouse
joltc -M:camera2d   # 2D camera (see note below)
```

Headless compile-check of the whole suite (no window needed):

```sh
joltc -M:check      # requires every example namespace; prints "compiled OK"
```

### Environment variables (used by every example)

- `RAYLIB_APP_AUTO_QUIT_MS=<n>` — close the window after `n` milliseconds, so an
  example is smoke-testable with no person at the keyboard.
- `RAYLIB_APP_SHOT=<name>` — dump one frame as a PNG (headless visual proof). raylib
  writes the file's **basename into the current working directory** (it ignores
  directory components), and the batched text is flushed first so it appears.

## The examples

| Alias | raylib source | Shows |
|---|---|---|
| `run` | core/core_basic_window | window, clear, text |
| `input` | core/core_input_keys | `IsKeyDown`, move a circle |
| `bounce` | shapes/shapes_bouncing_ball | animation, `IsKeyPressed` pause, `DrawFPS` |
| `colors` | (showcase) | the whole named-color palette as swatches |
| `mouse` | core/core_input_mouse | `GetMouseX/Y`, `IsMouseButtonDown` |
| `wheel` | core/core_input_mouse_wheel | `GetMouseWheelMove` (a float return) |
| `shapes` | shapes/shapes_basic_shapes | rect/circle/ellipse/line + an rlgl triangle |
| `gradient` | shapes | `DrawRectangleGradientV` (two by-value Colors) |
| `text` | text | font sizes/colors + `MeasureText` centering |
| `logo` | (raylib logo) | rectangles + text, positioned with `MeasureText` |
| `stars` | (showcase) | `GetRandomValue` + bulk draw, per-star twinkle |
| `eyes` | shapes/shapes_following_eyes | pupils track the mouse (scalar trig) |
| `camera2d` | core/core_2d_camera | 2D camera — struct-by-value (see note) |
| `delta-time` | core/core_delta_time | per-frame vs `GetFrameTime` movement |
| `scissor-test` | core/core_scissor_test | `BeginScissorMode` clips a grid |
| `mouse-trail` | shapes/shapes_mouse_trail | fading cursor trail (alpha) |
| `recursive-tree` | shapes | a binary fractal tree (lines + trig) |
| `math-sine-cosine` | shapes/shapes_math_sine_cosine | unit-circle sine/cosine viz |
| `bullet-hell` | shapes/shapes_bullet_hell | a rotating bullet spiral |
| `triangle-strip` | shapes/shapes_triangle_strip | rainbow strip via rlgl immediate mode |
| `writing-anim` | text/text_writing_anim | a self-typing message |
| `format-text` | text/text_format_text | `format` padded score + MM:SS timer |
| `basic-screen-manager` | core/core_basic_screen_manager | LOGO/TITLE/GAMEPLAY/ENDING state flow |
| `random-values` | core/core_random_values | a new random value every 2s |
| `collision-area` | shapes/shapes_collision_area | AABB collision (computed in Clojure) |
| `dashed-line` | shapes/shapes_dashed_line | a dashed line follows the mouse |
| `double-pendulum` | shapes | a chaotic double pendulum + trail |
| `kaleidoscope` | shapes | strokes mirrored with 6-fold symmetry |
| `hilbert-curve` | shapes | a rainbow Hilbert space-filling curve |
| `math-angle-rotation` | shapes/shapes_math_angle_rotation | fixed spokes + a spinning line |
| `words-alignment` | text/text_words_alignment | align a word with `MeasureText` |
| `camera-3d` | core/core_3d_camera | orbiting 3D camera — `Camera3D` by value + rlgl cube |
| `waving-cubes` | models/models_waving_cubes | 196 cubes rippling in 3D (shared `rl/cube!`) |
| `camera-3d-first-person` | core/core_3d_camera_first_person | WASD + mouse-look walk through columns |
| `tesseract-view` | (4D) | a rotating 4D hypercube projected 4D→3D→2D |
| `wireframe-shapes` | models | pyramid/octahedron/torus/helix as rlgl 3D lines |
| `rlgl-solar-system` | models/models_rlgl_solar_system | Sun/Earth/Moon via the rlgl matrix stack |
| `box-collisions` | models/models_box_collisions | player cube vs. boxes, 3D AABB highlight |
| `rotating-cube` | models | one cube spinning via the rlgl matrix stack |
| `spinning-cubes` | models | a row of cubes each spinning with a phase offset |
| `orthographic-projection` | core/core_3d_camera | perspective vs orthographic (`:projection` toggle) |
| `point-cloud` | (showcase) | ~1500 points as tiny rlgl cubes, rotating |
| `bouncing-spheres` | models | spheres bouncing in a 3D box (`rl/sphere!`) |
| `ball-physics` | shapes | 2D balls under gravity + restitution |
| `lines-bezier` | shapes/shapes_lines_bezier | a cubic Bézier sampled in Clojure, follows the mouse |
| `input-box` | text/text_input_box | a text field via `GetCharPressed` (blinking cursor) |
| `asteroids` | (game) | the classic vector shooter — rotate/thrust/fire, splitting asteroids |
| `tetris` | (game) | 10×20 well, 7 tetrominoes, rotation, line-clearing, levels |
| `pong` | (game) | two-paddle classic, you (W/S) vs a ball-tracking CPU |
| `vampire-survivors` | (game) | auto-firing survivors-like — chasing waves, XP gems, leveling |
| `snake` | (game) | the classic snake — arrow keys, grow, don't crash |
| `breakout` | (game) | paddle (mouse) + ball + brick grid, clear to win |
| `space-invaders` | (game) | marching alien grid, shoot up (arrows + SPACE) |
| `flappy-bird` | (game) | flap through scrolling pipe gaps (SPACE) |
| `game-2048` | (game) | **2048** — 4×4 tile-merge puzzle (arrow keys) |
| `minesweeper` | (game) | reveal/flag grid, flood-fill (mouse L/R) — new `mouse-pressed?` |

### Verification status

Every example has been run in a real window (clean launch → render → exit; most
inspected via `RAYLIB_APP_SHOT`). That includes `camera2d`, which confirmed the
**`Camera2D` struct-by-value spike works**: passing a 24-byte struct by value to a
C function via a pointer renders correctly on AArch64 (Apple silicon). See the
Camera2D note below for the x86-64 caveat.

## How the FFI works

### `Color` passed by value (every draw call)

raylib's `Color` is a 4-byte struct `{u8 r,g,b,a}` passed **by value**. On the
AArch64 and x86-64 ABIs a 4-byte all-integer struct travels in a single
general-purpose register, exactly like a `uint32`, so `net.b12n.rljlt.raylib/rgba` packs RGBA
little-endian into an int and each `Color` parameter is bound as `:uint`:

```clojure
(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
```

### Keyword-argument drawing API

raylib's C functions are positional; `net.b12n.rljlt.raylib` wraps the multi-argument draw
calls so examples read self-descriptively:

```clojure
(rl/text! "hi" :x 10 :y 20 :size 20 :color rl/RED)     ; DrawText
(rl/rect! :x 60 :y 80 :width 120 :height 90 :color rl/BLUE)
(rl/circle! :x 400 :y 225 :radius 50 :color rl/MAROON)
```

### rlgl immediate mode (for shapes with by-value `Vector2` args)

A few raylib calls (e.g. `DrawTriangle`) take `Vector2` by value, which does **not**
reduce to the `Color` trick (a 2-float struct goes in floating-point registers). The
`shapes` example draws its triangle with rlgl's scalar immediate mode instead —
`rlBegin` / `rlColor4ub` / `rlVertex2f` / `rlEnd`.

### `Camera2D` struct by value (the `camera2d` example)

raylib's `BeginMode2D(Camera2D)` takes a 24-byte struct by value. On the AArch64
(Apple) ABI a composite larger than 16 bytes is passed **indirectly** — the caller
allocates a copy and passes a pointer — so `net.b12n.rljlt.raylib/with-camera-2d` builds the
struct in native memory (`ffi/alloc` + six `ffi/write :float`s) and binds
`BeginMode2D` as `[:pointer]`.

**This is AArch64-specific.** On the x86-64 SysV ABI those 24 bytes are passed on
the stack, which a `[:pointer]` binding does not do — so this example is not portable
as written. The portable alternative is to apply the same transform with rlgl's
scalar matrix ops (`rlPushMatrix` / `rlTranslatef` / `rlRotatef` / `rlScalef`,
flushing the batch before `rlPopMatrix`), which is exactly what `BeginMode2D` does
internally. If `camera2d` crashes with an "invalid memory reference", the struct
passing is the cause — switch to the rlgl-matrix approach.

### 3D: `Camera3D` by value + rlgl geometry (the `camera-3d` example)

The same pointer approach scales to 3D: `BeginMode3D(Camera3D)` takes a 44-byte
struct (three `Vector3` + `fovy` + `projection`), which is `>16` bytes so it goes
by pointer too — `net.b12n.rljlt.raylib/with-camera-3d` builds it (`ffi/alloc` 44 +
`ffi/write` nine floats + an `:int`). But 3D shape helpers (`DrawCube`,
`DrawSphere`, `DrawLine3D`) take a `Vector3` **by value** — a 12-byte float struct
passed in FP registers, which the pointer trick does **not** cover. So `camera-3d`
draws its cube with rlgl immediate mode (`rlVertex3f`, scalar) inside `BeginMode3D`;
`DrawGrid` is scalar and used directly. That combination — `Camera3D` by pointer +
rlgl for geometry — is the path for any 3D example here until jolt gains native
by-value struct support for `Vector3`-taking functions.

## Documentation

- [`docs/guide/`](docs/guide/index.md) — patterns & pitfalls, each with source
  citations (mirrored to the [b12n umbrella wiki](https://github.com/burinc/b12n-wikis)):
  - [`color-by-value.md`](docs/guide/color-by-value.md) — why `Color` crosses the FFI as a packed `:uint`
  - [`struct-by-value-pointer-trick.md`](docs/guide/struct-by-value-pointer-trick.md) — `Camera2D`/`Camera3D` by pointer on AArch64 (+ the x86-64 caveat)
  - [`rlgl-immediate-mode.md`](docs/guide/rlgl-immediate-mode.md) — the fallback for by-value `Vector2`/`Vector3` geometry, + the matrix stack
  - [`kwarg-drawing-api.md`](docs/guide/kwarg-drawing-api.md) — the positional-binds / keyword-wrappers two-layer design
  - [`headless-smoke-testing.md`](docs/guide/headless-smoke-testing.md) — `RAYLIB_APP_AUTO_QUIT_MS` + `RAYLIB_APP_SHOT` proof without a person
  - [`example-catalog.md`](docs/guide/example-catalog.md) — a tour of all 50, and the four-touchpoint recipe for adding one

Sibling project: [`b12n-tsj`](https://github.com/burinc/b12n-tsj) — tree-sitter from
Jolt, the same `jolt.ffi` mechanism applied to a by-value-*returning* C API that
needs a full C shim this repo avoids.

## Layout

```
b12n-rljlt/
├── deps.edn                 ; libraylib :jolt/native + one alias per example
└── src/net/b12n/rljlt/
    ├── raylib.clj           ; ALL bindings + the kwarg API + Color palette + guards
    ├── check.clj            ; headless compile-check of every example
    ├── core.clj             ; basic window (the default, joltc -M:run)
    ├── input.clj  bounce.clj  colors.clj   mouse.clj  wheel.clj
    ├── shapes.clj gradient.clj text.clj    logo.clj   stars.clj
    ├── eyes.clj   camera2d.clj
    └── …
```

Adding an example: write `src/net/b12n/rljlt/<name>.clj` using the `net.b12n.rljlt.raylib` API, add a
`:<name>` alias to `deps.edn`, and add the namespace to `net.b12n.rljlt.check`.
