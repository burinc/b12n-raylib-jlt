# Grow `net.b12n.rljlt` 42 → 50 Examples — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 8 raylib examples (5 × 3d, 2 × shapes, 1 × text) plus two small `raylib.clj` toolkit additions (`sphere!` + char/key input binds), taking the suite from 42 to 50 examples.

**Architecture:** Each example is one namespace under `src/net/b12n/rljlt/` on top of the shared `net.b12n.rljlt.raylib` binding layer, wired through four touchpoints (source ns + `deps.edn` alias + `check.clj` require + `bb.edn` registry row/task). Two examples first need toolkit growth: a `sphere!` rlgl helper and `GetCharPressed`/`GetKeyPressed` binds.

**Tech Stack:** jolt (native Clojure on Chez Scheme, no JVM), `jolt.ffi`, raylib 5.5 (system `libraylib`), babashka task runner.

## Global Constraints

- **jolt ≠ JVM.** This is native Clojure on Chez. Verified-available interop: `Math/sin`, `Math/cos`, `Math/sqrt`, `Math/PI`, `(char <int>)`, `System/getenv`, `Integer/parseInt`, `System/currentTimeMillis`. Do NOT reach for `java.util.*` or other JVM classes.
- **One binding layer.** Every example `(:require [net.b12n.rljlt.raylib :as rl])` and calls only `rl/*`. New raw bindings live in `raylib.clj`, never in an example.
- **Filenames use underscores; namespaces use hyphens** (`rotating_cube.clj` ↔ `net.b12n.rljlt.rotating-cube`).
- **Four touchpoints per example:** `src/net/b12n/rljlt/<name>.clj`, a `deps.edn` alias, a `check.clj` require line, a `bb.edn` registry row + `bb <name>` task.
- **Draw calls that take x/y expect ints** (`draw-text`/`draw-rectangle`/`draw-line` bind `:int`); coerce doubles with `(int …)`. `circle!`/`ellipse!` coerce radius to double internally.
- **Verification per example:** `joltc -M:check` must print `net.b12n.rljlt: all example namespaces compiled OK`, then a screenshot smoke `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<alias>` (needs an awake display; raylib writes the basename into CWD).
- **Stage files by explicit path; never `git add -A`/`.`/`-u`.** Clean up `shot-*.png` (gitignored) before committing.
- **Groups** for `bb info`: `games` / `core` / `shapes` / `text` / `3d`.

---

## Task 1: Toolkit additions to `raylib.clj` (`sphere!` + input binds)

**Files:**
- Modify: `src/net/b12n/rljlt/raylib.clj` (add `sphere!` after `cube!`; add input binds near the other input binds; add `KEY-BACKSPACE`/`KEY-ENTER` near the other key consts)

**Interfaces:**
- Produces:
  - `(rl/sphere! & {:keys [pos radius rings slices color]})` — draws an rlgl sphere; must run inside `with-camera-3d`.
  - `(rl/get-char-pressed)` → int (unicode codepoint; 0 when queue empty)
  - `(rl/get-key-pressed)` → int (keycode; 0 when queue empty)
  - `rl/KEY-BACKSPACE` = 259, `rl/KEY-ENTER` = 257

- [ ] **Step 1: Add `sphere!` immediately after `cube!`** (after the `cube!` defn, before the `;; --- input ---` section)

```clojure
(defn sphere!
  "Draw a sphere via rlgl immediate mode (lat/long tessellation), faces shaded
  from the packed `:color` for depth (brighter toward +y). Must be called inside
  a BeginMode3D block (see with-camera-3d). Keyword args:
    :pos    [x y z] centre        (default [0 0 0])
    :radius a number              (default 0.5)
    :rings  latitude bands        (default 12)
    :slices longitude sectors     (default 16)
    :color  a packed Color        (default BLACK)"
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0] radius 0.5 rings 12 slices 16 color BLACK}}]
  (let [[cx cy cz] pos
        two-pi (* 2.0 Math/PI)]
    (rl-begin RL-TRIANGLES)
    (dotimes [i rings]
      (let [lat0 (- (* Math/PI (/ (double i) rings)) (/ Math/PI 2.0))
            lat1 (- (* Math/PI (/ (double (inc i)) rings)) (/ Math/PI 2.0))
            y0 (Math/sin lat0) y1 (Math/sin lat1)
            r0 (Math/cos lat0) r1 (Math/cos lat1)
            brightness (+ 0.45 (* 0.55 (/ (+ y0 y1 2.0) 4.0)))
            shaded (shade-color color brightness)]
        (dotimes [j slices]
          (let [lon0 (* two-pi (/ (double j) slices))
                lon1 (* two-pi (/ (double (inc j)) slices))
                s0 (Math/sin lon0) c0 (Math/cos lon0)
                s1 (Math/sin lon1) c1 (Math/cos lon1)
                p00 [(+ cx (* radius r0 c0)) (+ cy (* radius y0)) (+ cz (* radius r0 s0))]
                p01 [(+ cx (* radius r0 c1)) (+ cy (* radius y0)) (+ cz (* radius r0 s1))]
                p10 [(+ cx (* radius r1 c0)) (+ cy (* radius y1)) (+ cz (* radius r1 s0))]
                p11 [(+ cx (* radius r1 c1)) (+ cy (* radius y1)) (+ cz (* radius r1 s1))]]
            (quad-3f shaded [p00 p10 p11 p01])))))
    (rl-end)))
```

- [ ] **Step 2: Add the input binds** in the `;; --- input ---` section (after `get-random-value`)

```clojure
(ffi/defcfn get-char-pressed "GetCharPressed" [] :int)  ; unicode codepoint; 0 = queue empty
(ffi/defcfn get-key-pressed  "GetKeyPressed"  [] :int)  ; keycode; 0 = queue empty
```

- [ ] **Step 3: Add the two key consts** in the `;; --- constants ---` section (after `MOUSE-LEFT`)

```clojure
(def ^:const KEY-BACKSPACE 259) (def ^:const KEY-ENTER 257)
```

- [ ] **Step 4: Compile-check**

Run: `joltc -M:check`
Expected: `net.b12n.rljlt: all example namespaces compiled OK`
(Visual proof of `sphere!` comes in Task 6; of the input binds in Task 8.)

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/raylib.clj
git commit -m "feat(raylib): add sphere! rlgl helper + GetCharPressed/GetKeyPressed binds"
```

---

## Task 2: `rotating-cube` (3d)

**Files:**
- Create: `src/net/b12n/rljlt/rotating_cube.clj`
- Modify: `deps.edn` (alias), `src/net/b12n/rljlt/check.clj` (require), `bb.edn` (registry row + task)

**Interfaces:**
- Consumes: `rl/with-camera-3d`, `rl/cube!`, `rl/draw-grid`, `rl/rl-push-matrix`/`rl-pop-matrix`/`rl-rotatef`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.rotating-cube
  "raylib [models] example — a single cube rotating in place via the rlgl matrix
  stack. Fixed 3D camera; the cube spins on X and Y with a frame-driven angle.
  See docs/guide/rlgl-immediate-mode.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - rotating cube")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [angle (* frame 1.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 4.0 :pos-y 4.0 :pos-z 4.0}
            (fn []
              (rl/draw-grid 10 1.0)
              (rl/rl-push-matrix)
              (rl/rl-rotatef angle 1.0 0.0 0.0)
              (rl/rl-rotatef (* angle 0.7) 0.0 1.0 0.0)
              (rl/cube! :pos [0.0 0.0 0.0] :size 2.0 :color rl/RED)
              (rl/rl-pop-matrix)))
          (rl/text! "A cube rotating via the rlgl matrix stack"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

In `deps.edn`, add to the `:aliases` map (before the closing `}}` of the last alias):
```clojure
           :rotating-cube {:main-opts ["-m" "net.b12n.rljlt.rotating-cube"]} ; single cube spinning (rlgl matrix)
```
In `src/net/b12n/rljlt/check.clj`, add to the `:require` block:
```clojure
            net.b12n.rljlt.rotating-cube
```
In `bb.edn`, add a row to the `examples` vector (in the `3d` group region):
```clojure
       ["rotating-cube"     "rotating-cube" "3d" "a single cube spinning via the rlgl matrix stack"]
```
and a task in the per-example task section:
```clojure
  rotating-cube     {:doc "▶ a single cube spinning via the rlgl matrix stack" :task (run-example "rotating-cube")}
```

- [ ] **Step 3: Compile-check**

Run: `joltc -M:check`
Expected: `net.b12n.rljlt: all example namespaces compiled OK`

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-rotating-cube.png joltc -M:rotating-cube`
Expected: a red cube on a grid; view the PNG to confirm it renders (rotation won't show in a still, but the cube + grid + text must).

- [ ] **Step 5: Commit**

```bash
rm -f shot-rotating-cube.png
git add src/net/b12n/rljlt/rotating_cube.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add rotating-cube (3d)"
```

---

## Task 3: `spinning-cubes` (3d)

**Files:**
- Create: `src/net/b12n/rljlt/spinning_cubes.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/with-camera-3d`, `rl/cube!`, `rl/draw-grid`, matrix stack, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.spinning-cubes
  "raylib [models] example — a row of cubes each spinning in place with a
  per-index phase offset (distinct from waving-cubes, which translates a grid).
  See docs/guide/rlgl-immediate-mode.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def palette [rl/RED rl/ORANGE rl/GREEN rl/BLUE rl/VIOLET])

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - spinning cubes")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/with-camera-3d {:pos-x 0.0 :pos-y 6.0 :pos-z 10.0}
          (fn []
            (rl/draw-grid 12 1.0)
            (dotimes [i 5]
              (let [x (- (* i 2.0) 4.0)
                    angle (+ (* frame 2.0) (* i 30.0))]
                (rl/rl-push-matrix)
                (rl/rl-translatef x 0.5 0.0)
                (rl/rl-rotatef angle 0.3 1.0 0.0)
                (rl/cube! :pos [0.0 0.0 0.0] :size 1.0 :color (nth palette i))
                (rl/rl-pop-matrix)))))
        (rl/text! "Five cubes spinning with a phase offset"
                  :x 10 :y 10 :size 20 :color rl/DARKGRAY)
        (rl/maybe-screenshot! frame 10)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :spinning-cubes {:main-opts ["-m" "net.b12n.rljlt.spinning-cubes"]} ; row of cubes spinning in place
```
`check.clj`:
```clojure
            net.b12n.rljlt.spinning-cubes
```
`bb.edn` registry row + task:
```clojure
       ["spinning-cubes"    "spinning-cubes" "3d" "a row of cubes each spinning with a phase offset"]
```
```clojure
  spinning-cubes    {:doc "▶ a row of cubes each spinning with a phase offset" :task (run-example "spinning-cubes")}
```

- [ ] **Step 3: Compile-check** — Run `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — Run `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-spinning-cubes.png joltc -M:spinning-cubes`; PNG shows 5 colored cubes on a grid.
- [ ] **Step 5: Commit**

```bash
rm -f shot-spinning-cubes.png
git add src/net/b12n/rljlt/spinning_cubes.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add spinning-cubes (3d)"
```

---

## Task 4: `orthographic-projection` (3d)

**Files:**
- Create: `src/net/b12n/rljlt/orthographic_projection.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/with-camera-3d` (`:projection`, `:fovy`), `rl/cube!`, `rl/draw-grid`, `rl/key-pressed?`, `rl/KEY-SPACE`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.orthographic-projection
  "raylib [core] example — the same 3D scene under perspective vs orthographic
  projection. Press SPACE to toggle with-camera-3d's :projection (0/1). For
  orthographic, fovy is the view height (raylib convention). See
  docs/guide/struct-by-value-pointer-trick.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [core] example - orthographic projection")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           ortho? false]
      (when (rl/keep-running? deadline)
        (let [ortho? (if (rl/key-pressed? rl/KEY-SPACE) (not ortho?) ortho?)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 5.0 :pos-y 5.0 :pos-z 5.0
                              :fovy (if ortho? 12.0 45.0)
                              :projection (if ortho? 1 0)}
            (fn []
              (rl/draw-grid 10 1.0)
              (rl/cube! :pos [-1.5 0.5 0.0] :size 1.0 :color rl/RED)
              (rl/cube! :pos [0.0 0.5 0.0]  :size 1.0 :color rl/GREEN)
              (rl/cube! :pos [1.5 0.5 0.0]  :size 1.0 :color rl/BLUE)))
          (rl/text! (if ortho? "ORTHOGRAPHIC  (SPACE to toggle)" "PERSPECTIVE  (SPACE to toggle)")
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) ortho?)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :orthographic-projection {:main-opts ["-m" "net.b12n.rljlt.orthographic-projection"]} ; perspective/ortho toggle
```
`check.clj`:
```clojure
            net.b12n.rljlt.orthographic-projection
```
`bb.edn` registry row + task:
```clojure
       ["orthographic-projection" "orthographic-projection" "3d" "perspective vs orthographic (SPACE toggles)"]
```
```clojure
  orthographic-projection {:doc "▶ perspective vs orthographic (SPACE toggles)" :task (run-example "orthographic-projection")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-ortho.png joltc -M:orthographic-projection`; PNG shows three cubes + grid + "PERSPECTIVE" label.
- [ ] **Step 5: Commit**

```bash
rm -f shot-ortho.png
git add src/net/b12n/rljlt/orthographic_projection.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add orthographic-projection (3d)"
```

---

## Task 5: `point-cloud` (3d)

**Files:**
- Create: `src/net/b12n/rljlt/point_cloud.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/with-camera-3d`, `rl/cube!`, matrix stack, `rl/get-random-value`, `rl/rgba`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.point-cloud
  "raylib [models] example — a cloud of ~1500 points, each a tiny rlgl cube,
  colored by position and slowly rotating via the matrix stack. (rlgl has no
  RL_POINTS mode, so points are drawn as small cubes.) See
  docs/guide/rlgl-immediate-mode.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def n-points 1500)

(defn- make-points []
  (vec (repeatedly n-points
         (fn []
           (let [x (/ (rl/get-random-value -50 50) 10.0)
                 y (/ (rl/get-random-value -50 50) 10.0)
                 z (/ (rl/get-random-value -50 50) 10.0)
                 col (rl/rgba (int (+ 128 (* 25 x)))
                              (int (+ 128 (* 25 y)))
                              (int (+ 128 (* 25 z))) 255)]
             [x y z col])))))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - point cloud")
  (rl/set-target-fps 60)
  (let [points (make-points)
        deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/BLACK)
        (rl/with-camera-3d {:pos-x 0.0 :pos-y 0.0 :pos-z 12.0}
          (fn []
            (rl/rl-push-matrix)
            (rl/rl-rotatef (* frame 0.3) 0.0 1.0 0.0)
            (doseq [[x y z col] points]
              (rl/cube! :pos [x y z] :size 0.06 :color col))
            (rl/rl-pop-matrix)))
        (rl/text! (str n-points " points, each a tiny rlgl cube")
                  :x 10 :y 10 :size 20 :color rl/RAYWHITE)
        (rl/maybe-screenshot! frame 10)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :point-cloud {:main-opts ["-m" "net.b12n.rljlt.point-cloud"]} ; rotating cloud of tiny cubes
```
`check.clj`:
```clojure
            net.b12n.rljlt.point-cloud
```
`bb.edn` registry row + task:
```clojure
       ["point-cloud"       "point-cloud" "3d" "~1500 points as tiny rlgl cubes, rotating"]
```
```clojure
  point-cloud       {:doc "▶ ~1500 points as tiny rlgl cubes, rotating" :task (run-example "point-cloud")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-point-cloud.png joltc -M:point-cloud`; PNG shows a colored point cloud on black. If the frame rate is very low, reduce `n-points` to 1000.
- [ ] **Step 5: Commit**

```bash
rm -f shot-point-cloud.png
git add src/net/b12n/rljlt/point_cloud.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add point-cloud (3d)"
```

---

## Task 6: `bouncing-spheres` (3d) — first `sphere!` user

**Files:**
- Create: `src/net/b12n/rljlt/bouncing_spheres.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/sphere!` (Task 1), `rl/with-camera-3d`, `rl/draw-grid`, `rl/get-random-value`, `rl/key-pressed?`, `rl/KEY-SPACE`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.bouncing-spheres
  "raylib [models] example — spheres bouncing inside a 3D box under gravity, with
  wall restitution. Uses the rlgl-tessellated rl/sphere! helper (DrawSphere takes
  Vector3 by value, so it's built from rlgl triangles). SPACE respawns. See
  docs/guide/rlgl-immediate-mode.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def bound 4.0)
(def gravity 0.01)
(def restitution 0.9)
(def palette [rl/RED rl/ORANGE rl/GREEN rl/SKYBLUE rl/VIOLET rl/GOLD])

(defn- spawn []
  (vec (for [i (range 6)]
         {:x (/ (rl/get-random-value -30 30) 10.0)
          :y (/ (rl/get-random-value 10 40) 10.0)
          :z (/ (rl/get-random-value -30 30) 10.0)
          :vx (/ (rl/get-random-value -10 10) 100.0)
          :vy 0.0
          :vz (/ (rl/get-random-value -10 10) 100.0)
          :r (/ (rl/get-random-value 3 6) 10.0)
          :color (nth palette i)})))

(defn- reflect [p v r]
  (cond (< (- p r) (- bound)) [(+ (- bound) r) (* (- v) restitution)]
        (> (+ p r) bound)     [(- bound r) (* (- v) restitution)]
        :else [p v]))

(defn- step [{:keys [x y z vx vy vz r] :as b}]
  (let [vy (- vy gravity)
        [nx vx] (reflect (+ x vx) vx r)
        [ny vy] (reflect (+ y vy) vy r)
        [nz vz] (reflect (+ z vz) vz r)]
    (assoc b :x nx :y ny :z nz :vx vx :vy vy :vz vz)))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - bouncing spheres")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           spheres (spawn)]
      (when (rl/keep-running? deadline)
        (let [spheres (if (rl/key-pressed? rl/KEY-SPACE) (spawn) (mapv step spheres))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 10.0 :pos-y 8.0 :pos-z 10.0}
            (fn []
              (rl/draw-grid 10 1.0)
              (doseq [{:keys [x y z r color]} spheres]
                (rl/sphere! :pos [x y z] :radius r :rings 10 :slices 14 :color color))))
          (rl/text! "Spheres bouncing in a 3D box — SPACE respawns"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) spheres)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :bouncing-spheres {:main-opts ["-m" "net.b12n.rljlt.bouncing-spheres"]} ; spheres bouncing (rl/sphere!)
```
`check.clj`:
```clojure
            net.b12n.rljlt.bouncing-spheres
```
`bb.edn` registry row + task:
```clojure
       ["bouncing-spheres"  "bouncing-spheres" "3d" "spheres bouncing in a 3D box (rl/sphere!)"]
```
```clojure
  bouncing-spheres  {:doc "▶ spheres bouncing in a 3D box (rl/sphere!)" :task (run-example "bouncing-spheres")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke (validates `sphere!`)** — Run `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-spheres.png joltc -M:bouncing-spheres`. The PNG must show **solid, lit spheres** (lighter on top, darker underneath) on a grid. **If a sphere renders inside-out / hollow (you see its far inner surface, it looks dark or missing faces), the quad winding in `sphere!` is backwards — in `raylib.clj`'s `sphere!`, change `(quad-3f shaded [p00 p10 p11 p01])` to `(quad-3f shaded [p00 p01 p11 p10])` and re-smoke.** Amend Task 1's commit if you change it (`git add src/net/b12n/rljlt/raylib.clj && git commit --amend --no-edit` before committing this task).
- [ ] **Step 5: Commit**

```bash
rm -f shot-spheres.png
git add src/net/b12n/rljlt/bouncing_spheres.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add bouncing-spheres (3d, first sphere! user)"
```

---

## Task 7: `ball-physics` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/ball_physics.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/circle!`, `rl/get-random-value`, `rl/key-pressed?`, `rl/KEY-SPACE`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.ball-physics
  "raylib [shapes] example — 2D balls under gravity, bouncing off the window
  edges with restitution. SPACE respawns a fresh set. Pure scalar math + circle!."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def gravity 0.4)
(def restitution 0.8)
(def palette [rl/RED rl/ORANGE rl/GOLD rl/GREEN rl/SKYBLUE rl/VIOLET])

(defn- spawn-balls []
  (vec (for [i (range 6)]
         {:x (rl/get-random-value 100 700)
          :y (rl/get-random-value 50 200)
          :vx (/ (rl/get-random-value -40 40) 10.0)
          :vy 0.0
          :r (rl/get-random-value 15 35)
          :color (nth palette i)})))

(defn- step [{:keys [x y vx vy r] :as b}]
  (let [vy (+ vy gravity)
        [nx vx] (cond (< (- (+ x vx) r) 0)      [r (* (- vx) restitution)]
                      (> (+ (+ x vx) r) width)  [(- width r) (* (- vx) restitution)]
                      :else [(+ x vx) vx])
        [ny vy] (if (> (+ (+ y vy) r) height)
                  [(- height r) (* (- vy) restitution)]
                  [(+ y vy) vy])]
    (assoc b :x nx :y ny :vx vx :vy vy)))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [shapes] example - ball physics")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           balls (spawn-balls)]
      (when (rl/keep-running? deadline)
        (let [balls (if (rl/key-pressed? rl/KEY-SPACE) (spawn-balls) (mapv step balls))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [{:keys [x y r color]} balls]
            (rl/circle! :x (int x) :y (int y) :radius r :color color))
          (rl/text! "Balls under gravity — SPACE respawns"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) balls)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :ball-physics {:main-opts ["-m" "net.b12n.rljlt.ball-physics"]} ; 2D balls under gravity
```
`check.clj`:
```clojure
            net.b12n.rljlt.ball-physics
```
`bb.edn` registry row (in the `shapes` group region) + task:
```clojure
       ["ball-physics"      "ball-physics" "shapes" "2D balls under gravity, SPACE respawns"]
```
```clojure
  ball-physics      {:doc "▶ 2D balls under gravity, SPACE respawns" :task (run-example "ball-physics")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-ball-physics.png joltc -M:ball-physics`; PNG shows several colored circles part-way down the window.
- [ ] **Step 5: Commit**

```bash
rm -f shot-ball-physics.png
git add src/net/b12n/rljlt/ball_physics.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add ball-physics (shapes)"
```

---

## Task 8: `lines-bezier` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/lines_bezier.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/line!`, `rl/circle!`, `rl/get-mouse-x`/`get-mouse-y`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.lines-bezier
  "raylib [shapes] example — a cubic Bézier curve whose end point follows the
  mouse. DrawLineBezier takes Vector2 by value (unbindable), so the curve is
  sampled in Clojure and drawn as line! segments. See
  docs/guide/rlgl-immediate-mode.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def segments 30)

(defn- bezier-point [t [p0x p0y] [p1x p1y] [p2x p2y] [p3x p3y]]
  (let [u (- 1.0 t)
        a (* u u u)
        b (* 3.0 u u t)
        c (* 3.0 u t t)
        d (* t t t)]
    [(+ (* a p0x) (* b p1x) (* c p2x) (* d p3x))
     (+ (* a p0y) (* b p1y) (* c p2y) (* d p3y))]))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [shapes] example - lines bezier")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [p0 [100.0 225.0]
              p1 [300.0 60.0]
              p2 [500.0 390.0]
              p3 [(double (rl/get-mouse-x)) (double (rl/get-mouse-y))]
              pts (mapv (fn [i] (bezier-point (/ i (double segments)) p0 p1 p2 p3))
                        (range (inc segments)))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[[x1 y1] [x2 y2]] (partition 2 1 pts)]
            (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/BLUE))
          (doseq [[px py] [p0 p1 p2 p3]]
            (rl/circle! :x (int px) :y (int py) :radius 5 :color rl/RED))
          (rl/text! "Cubic Bézier — move the mouse (end point follows)"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :lines-bezier {:main-opts ["-m" "net.b12n.rljlt.lines-bezier"]} ; cubic Bézier follows the mouse
```
`check.clj`:
```clojure
            net.b12n.rljlt.lines-bezier
```
`bb.edn` registry row + task:
```clojure
       ["lines-bezier"      "lines-bezier" "shapes" "a cubic Bézier that follows the mouse"]
```
```clojure
  lines-bezier      {:doc "▶ a cubic Bézier that follows the mouse" :task (run-example "lines-bezier")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-lines-bezier.png joltc -M:lines-bezier`; PNG shows a blue curve with four red control dots.
- [ ] **Step 5: Commit**

```bash
rm -f shot-lines-bezier.png
git add src/net/b12n/rljlt/lines_bezier.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add lines-bezier (shapes)"
```

---

## Task 9: `input-box` (text) — first input-binds user

**Files:**
- Create: `src/net/b12n/rljlt/input_box.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/get-char-pressed` (Task 1), `rl/key-pressed?`, `rl/KEY-BACKSPACE`/`rl/KEY-ENTER` (Task 1), `rl/rect-lines!`, `rl/text!`, loop guards.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.input-box
  "raylib [text] example — a text input box. Type printable characters
  (GetCharPressed), backspace deletes, a blinking cursor blinks, ENTER clears.
  Uses the get-char-pressed / get-key-pressed binds. See README.md."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def max-len 20)

(defn- drain-chars [s]
  (loop [s s]
    (let [c (rl/get-char-pressed)]
      (if (pos? c)
        (recur (if (and (< (count s) max-len) (>= c 32) (<= c 125))
                 (str s (char c))
                 s))
        s))))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [text] example - input box")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           text ""]
      (when (rl/keep-running? deadline)
        (let [text (drain-chars text)
              text (if (rl/key-pressed? rl/KEY-BACKSPACE)
                     (subs text 0 (max 0 (dec (count text))))
                     text)
              text (if (rl/key-pressed? rl/KEY-ENTER) "" text)
              cursor? (even? (quot frame 30))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "Type something:" :x 200 :y 140 :size 20 :color rl/DARKGRAY)
          (rl/rect-lines! :x 200 :y 180 :width 400 :height 50 :color rl/DARKGRAY)
          (rl/text! (str text (if cursor? "_" "")) :x 210 :y 192 :size 30 :color rl/MAROON)
          (rl/text! (str (count text) "/" max-len) :x 200 :y 245 :size 20 :color rl/GRAY)
          (rl/text! "BACKSPACE deletes · ENTER clears" :x 200 :y 275 :size 16 :color rl/GRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) text)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :input-box {:main-opts ["-m" "net.b12n.rljlt.input-box"]} ; type into a text box (GetCharPressed)
```
`check.clj`:
```clojure
            net.b12n.rljlt.input-box
```
`bb.edn` registry row (in the `text` group region) + task:
```clojure
       ["input-box"         "input-box" "text" "type into a text box (GetCharPressed)"]
```
```clojure
  input-box         {:doc "▶ type into a text box (GetCharPressed)" :task (run-example "input-box")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke (validates input binds compile + run)** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-input-box.png joltc -M:input-box`; PNG shows the labeled box with a blinking cursor and `0/20` counter. (Typing isn't exercised headless; the compile + clean run confirm the binds resolve.)
- [ ] **Step 5: Commit**

```bash
rm -f shot-input-box.png
git add src/net/b12n/rljlt/input_box.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(examples): add input-box (text, first GetCharPressed user)"
```

---

## Task 10: Doc-sync — README + guide pages

**Files:**
- Modify: `README.md`, `docs/guide/example-catalog.md`, `docs/guide/rlgl-immediate-mode.md`

**Interfaces:**
- Consumes: nothing (documentation only). Run after all 8 examples land.

- [ ] **Step 1: Add 8 rows to the README examples table**

In `README.md`'s `## The examples` table, add these rows (after the existing 3d rows / at the end of the relevant sections). Match the existing `| alias | raylib source | Shows |` column shape:
```markdown
| `rotating-cube` | models | one cube spinning via the rlgl matrix stack |
| `spinning-cubes` | models | a row of cubes each spinning with a phase offset |
| `orthographic-projection` | core/core_3d_camera | perspective vs orthographic (`:projection` toggle) |
| `point-cloud` | (showcase) | ~1500 points as tiny rlgl cubes, rotating |
| `bouncing-spheres` | models | spheres bouncing in a 3D box (`rl/sphere!`) |
| `ball-physics` | shapes | 2D balls under gravity + restitution |
| `lines-bezier` | shapes/shapes_lines_bezier | a cubic Bézier sampled in Clojure, follows the mouse |
| `input-box` | text/text_input_box | a text field via `GetCharPressed` (blinking cursor) |
```

- [ ] **Step 2: Update `docs/guide/example-catalog.md`**

Update the intro line "A map of the whole suite" area and the group headings/rows:
- Change the `## 3d (7)` heading to `## 3d (12)` and add rows for `rotating-cube`, `spinning-cubes`, `orthographic-projection`, `point-cloud`, `bouncing-spheres`.
- Change `## shapes (18)` to `## shapes (20)` and add rows for `ball-physics`, `lines-bezier`.
- Change `## text (4)` to `## text (5)` and add a row for `input-box`.

Example rows (match the existing `| \`bb\` name | shows |` shape):
```markdown
| `rotating-cube` | a single cube spinning via the rlgl matrix stack |
| `spinning-cubes` | a row of cubes each spinning with a phase offset |
| `orthographic-projection` | perspective vs orthographic (SPACE toggles) |
| `point-cloud` | ~1500 points as tiny rlgl cubes, rotating |
| `bouncing-spheres` | spheres bouncing in a 3D box (rl/sphere!) |
| `ball-physics` | 2D balls under gravity, SPACE respawns |
| `lines-bezier` | a cubic Bézier that follows the mouse |
| `input-box` | type into a text box (GetCharPressed) |
```

- [ ] **Step 3: Note `sphere!` in `docs/guide/rlgl-immediate-mode.md`**

In the "3D: `cube!` is 12 rlgl triangles" section, after the paragraph mentioning `with-camera-3d` + `cube!` are the 3D building blocks, add one sentence:
```markdown
`sphere!` is the same idea for a ball — lat/long rings of `RL_TRIANGLES`, faces
shaded by latitude — and drives the `bouncing-spheres` example.
```

- [ ] **Step 4: Verify the catalog counts sum to 50**

Run: `bb info`
Expected: group counts games 4 / core 9 / shapes 20 / text 5 / 3d 12 = **50**, matching the catalog headings you just edited.

- [ ] **Step 5: Commit**

```bash
git add README.md docs/guide/example-catalog.md docs/guide/rlgl-immediate-mode.md
git commit -m "docs: sync README + guide for the 8 new examples (42 → 50)"
```

---

## Task 11: Re-mirror to `b12n-wikis` + arc-close verification

**Files:**
- Modify (in `~/dev/b12n-wikis`): `b12n-rljlt/example-catalog.md`, `b12n-rljlt/rlgl-immediate-mode.md`, `README.md`, `CLAUDE.md`, `PATTERNS.md` (only the "42 examples" mentions)

- [ ] **Step 1: Arc-close verification in the project repo**

```bash
cd ~/dev/b12n-rljlt
joltc -M:check          # expect: net.b12n.rljlt: all example namespaces compiled OK
bb info                 # expect: 50 examples, groups 4/9/20/5/12
bb tasks | head -3      # expect: parses cleanly
git status --short      # expect: clean (all committed)
```

- [ ] **Step 2: Re-mirror the changed guide pages**

```bash
cp ~/dev/b12n-rljlt/docs/guide/example-catalog.md ~/dev/b12n-rljlt/docs/guide/rlgl-immediate-mode.md ~/dev/b12n-wikis/b12n-rljlt/
```

- [ ] **Step 3: Refresh the "42 examples" → "50 examples" mentions in the wiki index files**

In `~/dev/b12n-wikis/README.md` (the b12n-rljlt Projects row), `~/dev/b12n-wikis/CLAUDE.md` (the b12n-rljlt quick-ref row), and `~/dev/b12n-wikis/b12n-rljlt/README.md`, change "42 raylib examples" / "42 examples" → "50 raylib examples" / "50 examples". (PATTERNS.md doesn't state a count — leave it.)

- [ ] **Step 4: Commit + push the wiki**

```bash
cd ~/dev/b12n-wikis
git add b12n-rljlt/ README.md CLAUDE.md
SHA=$(git -C ~/dev/b12n-rljlt rev-parse --short HEAD)
git commit -m "re-mirror b12n-rljlt @ ${SHA}: 8 new examples (42 → 50), sphere! note"
git push origin main
```

- [ ] **Step 5: Push the project repo**

```bash
cd ~/dev/b12n-rljlt
git push origin main
git rev-list --left-right --count main...origin/main   # expect: 0  0
```

---

## Self-Review (checked against the spec)

- **Spec coverage:** toolkit (`sphere!` + input binds) → Task 1; the 8 examples → Tasks 2–9 (5×3d, 2×shapes, 1×text); doc-sync → Task 10; wiki re-mirror + arc-close verify → Task 11. All spec sections mapped.
- **Placeholder scan:** every code step has complete code; no TBD/TODO. The `sphere!` winding has an explicit empirical fix path in Task 6 Step 4 (not a placeholder — a named, testable contingency).
- **Type/name consistency:** `sphere!`/`get-char-pressed`/`get-key-pressed`/`KEY-BACKSPACE`/`KEY-ENTER` defined in Task 1 and consumed by Tasks 6 & 9 with matching names; every example consumes only `rl/*` symbols that exist in `raylib.clj` (verified against source this session).
- **jolt-interop check:** `Math/sin`/`cos`/`sqrt`/`PI` and `(char n)` confirmed available via `joltc -e` probe; no JVM-only interop used.
- **Group-count arithmetic:** 3d 7→12 (+5), shapes 18→20 (+2), text 4→5 (+1) = +8 → 50 total; games 4 + core 9 + shapes 20 + text 5 + 3d 12 = 50. ✓
```
