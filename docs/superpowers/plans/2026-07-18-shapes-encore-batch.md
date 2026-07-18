# Shapes Encore Batch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port six raylib `examples/shapes/` demos (color-wheel, pie-chart, splines, vector-angle, easings, penrose-tiling) plus one toolkit helper (`sector!`), taking `net.b12n.rljlt` from 63 to 69 examples.

**Architecture:** Each example is one namespace on the shared `net.b12n.rljlt.raylib` binding layer, wired through four touchpoints (src ns + `deps.edn` alias + `check.clj` require + `bb.edn` registry row/task). All land in the existing `shapes` group. Filled arcs use a new pure-rlgl `sector!` helper (the by-value `DrawCircleSector` is unbindable); splines are evaluated in pure Clojure and drawn with `line!`. All examples are non-interactive-verifiable via a `RAYLIB_APP_SHOT` screenshot.

**Tech Stack:** jolt (native Clojure on Chez Scheme, no JVM) · `joltc` v0.3.3 · raylib 5.5 (system dylib) · `jolt.ffi`.

## Global Constraints

- **No new C binds.** `sector!` is pure rlgl over the existing `rl-begin`/`rl-vertex-2f`/`rl-color-4ub`/`rl-end`.
- **No textures / fonts / audio / raygui.** Parameters are driven by animation or keys, not GUI widgets.
- **jolt dialect:** no `case` (use `cond`); `Math/sin cos sqrt PI atan2 acos pow floor abs` are all verified working. Avoid `Math/toDegrees`/`toRadians` (unprobed) — multiply by `(/ 180.0 Math/PI)` / `(/ Math/PI 180.0)` instead.
- **On-screen text is ASCII only** — the raylib default font renders `°`/`—` as `?`. Use `deg` and `-`.
- **rlgl 2D triangle winding:** the front face is **negative signed area** in screen coords (proven by `shapes.clj`'s triangle and `color_wheel`'s fan). Any fan/mesh must emit that winding or it is backface-culled. `sector!` emits `rim→center→rim` with **increasing** angle (front-facing); callers must pass `start-deg < end-deg`. Arbitrary-winding meshes (penrose) normalize each triangle to negative area first.
- **Filenames** underscore (`vector_angle.clj`), **namespaces** hyphen (`vector-angle`). Window 800×450.
- **Staging:** stage files by explicit path; never `git add -A`/`.`/`-u`.
- **Screenshots** (`shot-*.png`) are gitignored scratch — delete before committing.

---

## Task 1: Toolkit helper `sector!` + winding smoke

**Files:**
- Modify: `src/net/b12n/rljlt/raylib.clj` (insert after `pixel!`, before the `;; --- smoke-test loop guards` block — currently line 330)

**Interfaces:**
- Produces: `(rl/sector! :cx :cy :radius :start-deg :end-deg :segments :color)` — draws a filled arc as an rlgl triangle fan. Consumed by Task 3 (`pie-chart`) and Task 5 (`vector-angle`).

- [ ] **Step 1: Add `sector!` to `raylib.clj`**

Insert immediately after the `pixel!` defn (line 329) and before `;; --- smoke-test loop guards`:

```clojure
(defn sector!
  "A filled circular sector (pie slice / arc) drawn as an rlgl triangle fan — the
  immediate-mode stand-in for DrawCircleSector, whose Vector2 center is by-value and
  so unbindable (see rlgl-immediate-mode.md). The fan runs from the center across
  [start-deg, end-deg] in `segments` sub-triangles, a single packed `:color`.
  0 deg points up and the angle increases clockwise (rim = (sin, -cos)); vertices are
  emitted rim -> center -> rim so the fan carries raylib's front-facing winding and is
  not backface-culled. Callers must pass start-deg < end-deg.
    :cx :cy    center
    :radius    outer radius
    :start-deg :end-deg   sweep in degrees (0 = up, clockwise, increasing)
    :segments  fan resolution (default 32)
    :color     packed Color"
  [& {:keys [cx cy radius start-deg end-deg segments color]
      :or {cx 0 cy 0 radius 10 start-deg 0 end-deg 90 segments 32 color BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        rim (fn [deg]
              (let [t (* deg d->r)]
                [(+ cx (* radius (Math/sin t)))
                 (- cy (* radius (Math/cos t)))]))]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (dotimes [k segments]
      (let [[x0 y0] (rim (+ start-deg (* span (/ (double k) segments))))
            [x1 y1] (rim (+ start-deg (* span (/ (double (inc k)) segments))))]
        (rl-vertex-2f (double x0) (double y0))
        (rl-vertex-2f (double cx) (double cy))
        (rl-vertex-2f (double x1) (double y1))))
    (rl-end)))
```

- [ ] **Step 2: Compile-check**

Run: `joltc -M:check`
Expected: `net.b12n.rljlt: all example namespaces compiled OK`

- [ ] **Step 3: Winding smoke (throwaway — proves the fan is front-facing, not culled)**

Create `src/net/b12n/rljlt/sectorsmoke.clj`:

```clojure
(ns net.b12n.rljlt.sectorsmoke
  "Throwaway: prove sector! renders (winding not culled). Delete after use."
  (:require [net.b12n.rljlt.raylib :as rl]))

(defn -main [& _]
  (rl/window! :title "sector smoke")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/sector! :cx 400 :cy 225 :radius 150 :start-deg 30 :end-deg 300
                    :segments 48 :color rl/SKYBLUE)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

Add a temporary alias to `deps.edn` (right after the `:check` alias line):

```clojure
           :sectorsmoke {:main-opts ["-m" "net.b12n.rljlt.sectorsmoke"]} ; TEMP — remove
```

Run: `RAYLIB_APP_AUTO_QUIT_MS=1200 RAYLIB_APP_SHOT=shot-sector.png joltc -M:sectorsmoke`
Then: `[ -s shot-sector.png ] && echo NONEMPTY` and open `shot-sector.png`.
Expected: a solid sky-blue pie wedge (270°) — **not** blank. If blank/culled, swap the two rim vertices in `sector!` (emit `rim(k+1) → center → rim(k)`), re-run.

- [ ] **Step 4: Remove the throwaway smoke**

```bash
rm src/net/b12n/rljlt/sectorsmoke.clj shot-sector.png
```
Then delete the `:sectorsmoke` alias line from `deps.edn`. Re-run `joltc -M:check` → still green.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/raylib.clj
git commit -m "feat(raylib): add sector! (rlgl fan filled arc) for pie-chart + vector-angle"
```

---

## Task 2: `color-wheel` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/color_wheel.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `net.b12n.rljlt.raylib` (`rl-begin`, `rl-color!`, `rl-vertex-2f`, `rl-end`, `RL-TRIANGLES`, `rgba`, drawing API).

- [ ] **Step 1: Create `src/net/b12n/rljlt/color_wheel.clj`**

```clojure
(ns net.b12n.rljlt.color-wheel
  "raylib [shapes] example - rlgl color wheel. A hue ring drawn as an rlgl triangle
  fan: each slice's rim vertices carry an HSV->RGB color (s=v=1), the center is white.
  Port of shapes_rlgl_color_wheel (minus the raygui value slider); the hue offset
  rotates slowly so the wheel animates."
  (:require [net.b12n.rljlt.raylib :as rl]))

(defn- hsv->color
  "HSV -> packed Color for s=1, v=1. h in degrees (wrapped)."
  [h]
  (let [h' (/ (mod h 360.0) 60.0)
        i  (int (Math/floor h'))
        f  (- h' i)
        q  (- 1.0 f)
        [r g b] (cond
                  (= i 0) [1.0 f 0.0]
                  (= i 1) [q 1.0 0.0]
                  (= i 2) [0.0 1.0 f]
                  (= i 3) [0.0 q 1.0]
                  (= i 4) [f 0.0 1.0]
                  :else   [1.0 0.0 q])]
    (rl/rgba (int (* 255 r)) (int (* 255 g)) (int (* 255 b)) 255)))

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - rlgl color wheel")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        n 64 cx 400 cy 235 radius 165
        two-pi (* 2.0 Math/PI)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background (rl/rgba 20 20 28 255))
        (let [hue-off (* frame 0.6)]
          (rl/rl-begin rl/RL-TRIANGLES)
          (dotimes [i n]
            (let [a0 (* two-pi (/ (double i) n))
                  a1 (* two-pi (/ (double (inc i)) n))
                  h0 (+ hue-off (* 360.0 (/ (double i) n)))
                  h1 (+ hue-off (* 360.0 (/ (double (inc i)) n)))
                  x0 (+ cx (* radius (Math/sin a0)))
                  y0 (- cy (* radius (Math/cos a0)))
                  x1 (+ cx (* radius (Math/sin a1)))
                  y1 (- cy (* radius (Math/cos a1)))]
              (rl/rl-color! (hsv->color h0))
              (rl/rl-vertex-2f (double x0) (double y0))
              (rl/rl-color! rl/WHITE)
              (rl/rl-vertex-2f (double cx) (double cy))
              (rl/rl-color! (hsv->color h1))
              (rl/rl-vertex-2f (double x1) (double y1))))
          (rl/rl-end))
        (rl/text! "HSV color wheel (rlgl triangle fan)" :x 10 :y 10 :size 20 :color rl/RAYWHITE)
        (rl/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn` — change the `:flow-field` line's trailing `}}` to `}` and append (keep the aliases map closing `}` on the last new line):

```clojure
           :color-wheel      {:main-opts ["-m" "net.b12n.rljlt.color-wheel"]}      ; HSV hue ring (rlgl fan)
```

`src/net/b12n/rljlt/check.clj` — change `net.b12n.rljlt.flow-field))` to `net.b12n.rljlt.flow-field` and append before the closing `))`:

```clojure
            net.b12n.rljlt.color-wheel))
```

`bb.edn` — append a registry row after the `flow-field` row (line 83; change its `]])` to `]` then add rows, close with `]])` on the last):

```clojure
       ["color-wheel"       "color-wheel" "shapes" "an HSV color wheel (rlgl triangle fan)"]
```

`bb.edn` — add a task after the `flow-field` task (line 163):

```clojure
  color-wheel       {:doc "▶ an HSV color wheel (rlgl triangle fan)"          :task (run-example "color-wheel")}
```

- [ ] **Step 3: Compile-check**

Run: `joltc -M:check`
Expected: `net.b12n.rljlt: all example namespaces compiled OK`

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-color-wheel.png joltc -M:color-wheel`
Open `shot-color-wheel.png`. Expected: a full-spectrum color wheel (red/green/blue/yellow/etc.), white toward center, on a dark background. Then `rm shot-color-wheel.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/color_wheel.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add color-wheel (HSV rlgl fan)"
```

---

## Task 3: `pie-chart` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/pie_chart.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/sector!` (Task 1), drawing API.

- [ ] **Step 1: Create `src/net/b12n/rljlt/pie_chart.clj`**

```clojure
(ns net.b12n.rljlt.pie-chart
  "raylib [shapes] example - pie chart. Fixed category data drawn as filled slices
  via rl/sector! (an rlgl triangle fan), with a legend column. The whole chart
  rotates slowly. Port of shapes_pie_chart."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def ^:private slices
  ;; [label value color]
  [["alpha"   30 rl/RED]
   ["beta"    24 rl/SKYBLUE]
   ["gamma"   18 rl/LIME]
   ["delta"   16 rl/GOLD]
   ["epsilon" 12 rl/VIOLET]])

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - pie chart")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx 270 cy 235 radius 165
        total (double (reduce + (map second slices)))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "pie chart (rl/sector! fan)" :x 10 :y 10 :size 20 :color rl/DARKGRAY)
        (let [base (* frame 0.25)]
          ;; slices
          (loop [acc 0.0 items slices]
            (when (seq items)
              (let [[_ val color] (first items)
                    span (* 360.0 (/ (double val) total))]
                (rl/sector! :cx cx :cy cy :radius radius
                            :start-deg (+ base acc) :end-deg (+ base acc span)
                            :segments 60 :color color)
                (recur (+ acc span) (rest items))))))
        ;; legend
        (dotimes [i (count slices)]
          (let [[label val color] (nth slices i)
                pct (int (Math/round (* 100.0 (/ (double val) total))))
                ly (+ 90 (* i 44))]
            (rl/rect! :x 540 :y ly :width 26 :height 26 :color color)
            (rl/text! (format "%s  %d%%" label pct)
                      :x 576 :y (+ ly 4) :size 20 :color rl/DARKGRAY)))
        (rl/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn` (append alias, keep map-closing `}`):
```clojure
           :pie-chart        {:main-opts ["-m" "net.b12n.rljlt.pie-chart"]}        ; labelled pie slices (sector!)
```
`check.clj` (append require before closing `))`):
```clojure
            net.b12n.rljlt.pie-chart))
```
`bb.edn` registry row:
```clojure
       ["pie-chart"         "pie-chart" "shapes" "labelled pie slices via rl/sector!"]
```
`bb.edn` task:
```clojure
  pie-chart         {:doc "▶ labelled pie slices via rl/sector!"               :task (run-example "pie-chart")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check` → all compiled OK.

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-pie.png joltc -M:pie-chart`
Open `shot-pie.png`. Expected: five colored wedges forming a full disk on the left, a legend with swatches + percentages on the right. Then `rm shot-pie.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/pie_chart.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add pie-chart (sector! slices + legend)"
```

---

## Task 4: `splines` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/splines.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: drawing API (`line!`, `circle!`, `circle-lines!`, `text!`), `key-pressed?`, `KEY-SPACE`.

- [ ] **Step 1: Create `src/net/b12n/rljlt/splines.clj`**

```clojure
(ns net.b12n.rljlt.splines
  "raylib [shapes] example - spline drawing. Five control points bob vertically; a
  spline is evaluated in pure Clojure (raylib's DrawSpline* take Vector2 arrays by
  value, unbindable) and drawn as a line! polyline. SPACE cycles Catmull-Rom /
  cubic Bezier / uniform B-spline. Port of shapes_splines_drawing (minus raygui)."
  (:require [net.b12n.rljlt.raylib :as rl]))

;; scalar spline bases: (f a b c d t) over 4 successive control values, t in [0,1]
(defn- cr [a b c d t]
  (let [t2 (* t t) t3 (* t2 t)]
    (* 0.5 (+ (* 2.0 b)
              (* (+ (- a) c) t)
              (* (+ (* 2.0 a) (* -5.0 b) (* 4.0 c) (- d)) t2)
              (* (+ (- a) (* 3.0 b) (* -3.0 c) d) t3)))))

(defn- bez [a b c d t]
  (let [u (- 1.0 t)]
    (+ (* u u u a) (* 3.0 u u t b) (* 3.0 u t t c) (* t t t d))))

(defn- bspl [a b c d t]
  (let [t2 (* t t) t3 (* t2 t)]
    (/ (+ (* (+ (- a) (* 3.0 b) (* -3.0 c) d) t3)
          (* (+ (* 3.0 a) (* -6.0 b) (* 3.0 c)) t2)
          (* (+ (* -3.0 a) (* 3.0 c)) t)
          (+ a (* 4.0 b) c))
       6.0)))

(def ^:private modes
  [[:catmull "Catmull-Rom" cr]
   [:bezier  "cubic Bezier" bez]
   [:bspline "uniform B-spline" bspl]])

(defn- control-points [frame]
  ;; five points across the window, each bobbing vertically with a per-point phase
  (vec (for [i (range 5)]
         (let [x (+ 90 (* i 155))
               y (+ 225 (* 70.0 (Math/sin (+ (* frame 0.03) (* i 1.3)))))]
           [x y]))))

(defn- polyline [f pts]
  ;; pad ends so the curve spans all points, then sample each 4-window between its
  ;; middle two control points
  (let [padded (vec (concat [(first pts)] pts [(last pts)]))
        steps 20]
    (loop [i 0 out []]
      (if (<= (+ i 3) (dec (count padded)))
        (let [[ax ay] (nth padded i)
              [bx by] (nth padded (+ i 1))
              [cx cy] (nth padded (+ i 2))
              [dx dy] (nth padded (+ i 3))
              seg (vec (for [s (range (inc steps))]
                         (let [t (/ (double s) steps)]
                           [(f ax bx cx dx t) (f ay by cy dy t)])))]
          (recur (inc i) (into out seg)))
        out))))

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - splines drawing")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 mode-idx 0]
      (when (rl/keep-running? deadline)
        (let [mode-idx (if (rl/key-pressed? rl/KEY-SPACE)
                         (mod (inc mode-idx) (count modes))
                         mode-idx)
              [_ label f] (nth modes mode-idx)
              pts (control-points frame)
              line-pts (polyline f pts)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; control polygon
          (dotimes [i (dec (count pts))]
            (let [[x1 y1] (nth pts i) [x2 y2] (nth pts (inc i))]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/LIGHTGRAY)))
          ;; the spline
          (dotimes [i (dec (count line-pts))]
            (let [[x1 y1] (nth line-pts i) [x2 y2] (nth line-pts (inc i))]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/RED)))
          ;; control point handles
          (doseq [[x y] pts]
            (rl/circle-lines! :x (int x) :y (int y) :radius 8 :color rl/DARKBLUE))
          (rl/text! (format "%s  (SPACE cycles)" label) :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) mode-idx)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :splines          {:main-opts ["-m" "net.b12n.rljlt.splines"]}          ; Catmull-Rom / Bezier / B-spline
```
`check.clj`:
```clojure
            net.b12n.rljlt.splines))
```
`bb.edn` registry row:
```clojure
       ["splines"           "splines" "shapes" "Catmull-Rom / Bezier / B-spline (SPACE cycles)"]
```
`bb.edn` task:
```clojure
  splines           {:doc "▶ Catmull-Rom / Bezier / B-spline (SPACE cycles)"    :task (run-example "splines")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check` → all compiled OK.

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-splines.png joltc -M:splines`
Open `shot-splines.png`. Expected: a smooth red curve threading five blue-ringed control points, a gray control polygon behind it, "Catmull-Rom (SPACE cycles)" label. Then `rm shot-splines.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/splines.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add splines (Catmull-Rom / Bezier / B-spline)"
```

---

## Task 5: `vector-angle` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/vector_angle.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/sector!` (Task 1), `line!`, `circle!`, `text!`, `rgba`. Uses `Math/atan2` (verified).

- [ ] **Step 1: Create `src/net/b12n/rljlt/vector_angle.clj`**

```clojure
(ns net.b12n.rljlt.vector-angle
  "raylib [shapes] example - vector angle. Two vectors share an origin; vector A is
  fixed, vector B rotates. The signed angle between them is filled as an arc via
  rl/sector! and read out in degrees. Port of shapes_vector_angle (B is time-driven
  rather than mouse-driven). Screen-space clockwise-from-up angle uses atan2(vx,-vy)."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def ^:private r->d (/ 180.0 Math/PI))

(defn- screen-deg
  "Clockwise-from-up angle (deg) of a screen vector (vx, vy) with y pointing down."
  [vx vy]
  (* r->d (Math/atan2 vx (- vy))))

(defn- norm180 [d]
  (cond (> d 180.0) (- d 360.0)
        (< d -180.0) (+ d 360.0)
        :else d))

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - vector angle")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        ox 400 oy 235 len 150.0
        ;; A fixed, pointing up-right
        a-deg 35.0
        ad (* a-deg (/ Math/PI 180.0))
        ax (* len (Math/sin ad)) ay (- (* len (Math/cos ad)))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [b-rad (* frame 0.02)
              bx (* len (Math/sin b-rad)) by (- (* len (Math/cos b-rad)))
              da (screen-deg ax ay)
              db (screen-deg bx by)
              delta (norm180 (- db da))
              lo (min da (+ da delta))
              hi (max da (+ da delta))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; the angle arc (translucent) between the two vectors
          (rl/sector! :cx ox :cy oy :radius 70 :start-deg lo :end-deg hi
                      :segments 48 :color (rl/rgba 255 200 0 110))
          ;; vectors
          (rl/line! :x1 ox :y1 oy :x2 (int (+ ox ax)) :y2 (int (+ oy ay)) :color rl/RED)
          (rl/line! :x1 ox :y1 oy :x2 (int (+ ox bx)) :y2 (int (+ oy by)) :color rl/BLUE)
          (rl/circle! :x (int (+ ox ax)) :y (int (+ oy ay)) :radius 5 :color rl/RED)
          (rl/circle! :x (int (+ ox bx)) :y (int (+ oy by)) :radius 5 :color rl/BLUE)
          (rl/circle! :x ox :y oy :radius 4 :color rl/DARKGRAY)
          (rl/text! "A" :x (int (+ ox ax 8)) :y (int (+ oy ay -6)) :size 20 :color rl/RED)
          (rl/text! "B" :x (int (+ ox bx 8)) :y (int (+ oy by -6)) :size 20 :color rl/BLUE)
          (rl/text! (format "angle: %.1f deg" (Math/abs delta))
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :vector-angle     {:main-opts ["-m" "net.b12n.rljlt.vector-angle"]}     ; angle between two vectors (atan2)
```
`check.clj`:
```clojure
            net.b12n.rljlt.vector-angle))
```
`bb.edn` registry row:
```clojure
       ["vector-angle"      "vector-angle" "shapes" "the angle between two vectors (arc + readout)"]
```
`bb.edn` task:
```clojure
  vector-angle      {:doc "▶ the angle between two vectors (arc + readout)"     :task (run-example "vector-angle")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check` → all compiled OK.

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-vecangle.png joltc -M:vector-angle`
Open `shot-vecangle.png`. Expected: a red vector "A" and a blue vector "B" from a shared origin, a translucent yellow arc filling the angle between them, and an "angle: NN.N deg" readout. Then `rm shot-vecangle.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/vector_angle.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add vector-angle (atan2 arc + readout)"
```

---

## Task 6: `easings` (shapes)

**Files:**
- Create: `src/net/b12n/rljlt/easings.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: drawing API (`line!`, `circle!`, `text!`, `rgba`). Uses `Math/pow`, `Math/sin`, `Math/cos` (verified).

- [ ] **Step 1: Create `src/net/b12n/rljlt/easings.clj`**

```clojure
(ns net.b12n.rljlt.easings
  "raylib [shapes] example - easings. A 3x4 grid of tracks, each animating a ball
  across the lane with a different easing function of a ping-ponging t in [0,1].
  Spirit of shapes_easings_*; all easings are pure math (pow / sin / cos)."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def ^:private PI Math/PI)

(defn- out-bounce [t]
  (let [n1 7.5625 d1 2.75]
    (cond
      (< t (/ 1.0 d1)) (* n1 t t)
      (< t (/ 2.0 d1)) (let [t (- t (/ 1.5 d1))] (+ (* n1 t t) 0.75))
      (< t (/ 2.5 d1)) (let [t (- t (/ 2.25 d1))] (+ (* n1 t t) 0.9375))
      :else            (let [t (- t (/ 2.625 d1))] (+ (* n1 t t) 0.984375)))))

(def ^:private easings
  [["linear"     (fn [t] t)]
   ["inQuad"     (fn [t] (* t t))]
   ["outQuad"    (fn [t] (- 1.0 (* (- 1.0 t) (- 1.0 t))))]
   ["inOutCubic" (fn [t] (if (< t 0.5)
                           (* 4.0 t t t)
                           (- 1.0 (/ (Math/pow (+ (* -2.0 t) 2.0) 3.0) 2.0))))]
   ["inSine"     (fn [t] (- 1.0 (Math/cos (/ (* t PI) 2.0))))]
   ["outSine"    (fn [t] (Math/sin (/ (* t PI) 2.0)))]
   ["inOutSine"  (fn [t] (/ (- (Math/cos (* PI t)) 1.0) -2.0))]
   ["inExpo"     (fn [t] (if (<= t 0.0) 0.0 (Math/pow 2.0 (- (* 10.0 t) 10.0))))]
   ["outElastic" (fn [t] (cond (<= t 0.0) 0.0 (>= t 1.0) 1.0
                               :else (+ 1.0 (* (Math/pow 2.0 (* -10.0 t))
                                               (Math/sin (* (- (* 10.0 t) 0.75) (/ (* 2.0 PI) 3.0)))))))]
   ["outBounce"  out-bounce]
   ["inBack"     (fn [t] (let [c1 1.70158 c3 (+ c1 1.0)] (- (* c3 t t t) (* c1 t t))))]
   ["inOutQuart" (fn [t] (if (< t 0.5)
                           (* 8.0 t t t t)
                           (- 1.0 (/ (Math/pow (+ (* -2.0 t) 2.0) 4.0) 2.0))))]])

(def ^:private lane-colors
  [rl/RED rl/ORANGE rl/GOLD rl/LIME rl/GREEN rl/SKYBLUE
   rl/BLUE rl/VIOLET rl/PURPLE rl/PINK rl/MAROON rl/DARKBLUE])

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - easings")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cols 3 track-w 190 x-pad 30]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [p (/ (double (mod frame 240)) 120.0)   ; 0..2
              t (if (<= p 1.0) p (- 2.0 p))]         ; ping-pong 0..1..0
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "easing functions" :x 10 :y 8 :size 20 :color rl/DARKGRAY)
          (dotimes [i (count easings)]
            (let [[label f] (nth easings i)
                  col (mod i cols)
                  row (quot i cols)
                  x0 (+ x-pad (* col (+ track-w 70)))
                  y  (+ 70 (* row 92))
                  eased (f t)
                  bx (+ x0 (* eased track-w))]
              (rl/text! label :x x0 :y (- y 22) :size 14 :color rl/DARKGRAY)
              (rl/line! :x1 x0 :y1 y :x2 (+ x0 track-w) :y2 y :color rl/LIGHTGRAY)
              (rl/circle! :x (int bx) :y y :radius 9 :color (nth lane-colors i))))
          (rl/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :easings          {:main-opts ["-m" "net.b12n.rljlt.easings"]}          ; a grid of easing-curve balls
```
`check.clj`:
```clojure
            net.b12n.rljlt.easings))
```
`bb.edn` registry row:
```clojure
       ["easings"           "easings" "shapes" "a grid of balls, each on a different easing curve"]
```
`bb.edn` task:
```clojure
  easings           {:doc "▶ a grid of balls on different easing curves"        :task (run-example "easings")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check` → all compiled OK.

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-easings.png joltc -M:easings`
Open `shot-easings.png`. Expected: a 3×4 grid of labelled tracks, each with a colored ball at a **different** horizontal position (the whole point — the easings diverge). Then `rm shot-easings.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/easings.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add easings (12-curve ball grid)"
```

---

## Task 7: `penrose-tiling` (shapes) — showpiece

**Files:**
- Create: `src/net/b12n/rljlt/penrose_tiling.clj`
- Modify: `deps.edn`, `src/net/b12n/rljlt/check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl-begin`, `rl-color!`, `rl-vertex-2f`, `rl-end`, `RL-TRIANGLES`, `line!`, `rgba`. Uses `Math/sqrt`, `Math/sin`, `Math/cos`.

- [ ] **Step 1: Create `src/net/b12n/rljlt/penrose_tiling.clj`**

Note the `front` winding-normalizer: deflation produces mixed windings; without normalizing to negative signed area, ~half the fill triangles would be backface-culled (the boids gotcha). Filled in one `rl-begin`/`rl-end` batch, colored per `:kind`.

```clojure
(ns net.b12n.rljlt.penrose-tiling
  "raylib [shapes] example - Penrose (P3 rhombus) tiling by deflation of Robinson
  triangles. A 10-triangle 'sun' seed is subdivided N times using golden-ratio lerps
  (Preshing's rules); triangles are filled (two colors by kind) as an rlgl batch and
  their edges stroked. Each fill triangle is winding-normalized to the front face so
  none are backface-culled. In the spirit of shapes_penrose_tile."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def ^:private phi (/ (+ 1.0 (Math/sqrt 5.0)) 2.0))
(def ^:private inv (/ 1.0 phi))

(defn- lerp [[ax ay] [bx by] s]
  [(+ ax (* (- bx ax) s)) (+ ay (* (- by ay) s))])

(defn- wheel
  "Seed: 10 Robinson triangles (kind 0) around (cx,cy) forming a decagon."
  [cx cy radius]
  (vec (for [i (range 10)]
         (let [ba (/ (* (- (* 2 i) 1) Math/PI) 10.0)
               ca (/ (* (+ (* 2 i) 1) Math/PI) 10.0)
               b [(+ cx (* radius (Math/cos ba))) (+ cy (* radius (Math/sin ba)))]
               c [(+ cx (* radius (Math/cos ca))) (+ cy (* radius (Math/sin ca)))]
               a [cx cy]]
           (if (even? i) [0 a c b] [0 a b c])))))

(defn- subdivide [tris]
  (vec (mapcat (fn [[k a b c]]
                 (if (zero? k)
                   (let [p (lerp a b inv)]
                     [[0 c p b] [1 p c a]])
                   (let [q (lerp b a inv)
                         r (lerp b c inv)]
                     [[1 r c a] [1 q r b] [0 r q a]])))
               tris)))

(defn- deflate [seed n]
  (loop [tris seed i 0]
    (if (< i n) (recur (subdivide tris) (inc i)) tris)))

(defn- front
  "Normalize a triangle's winding to the front face (negative signed area in screen
  coords), swapping b and c if needed, so the rlgl fill isn't backface-culled."
  [[k a b c]]
  (let [[ax ay] a [bx by] b [cx cy] c
        area (- (* (- bx ax) (- cy ay)) (* (- cx ax) (- by ay)))]
    (if (> area 0.0) [k a c b] [k a b c])))

(def ^:private col0 (rl/rgba 235 130 60 255))   ; thin rhombus halves
(def ^:private col1 (rl/rgba 70 130 200 255))    ; thick rhombus halves
(def ^:private edge (rl/rgba 30 30 40 130))

(defn -main [& _]
  (rl/window! :title "raylib [shapes] example - penrose tiling")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        tris (mapv front (deflate (wheel 400 230 235.0) 5))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background (rl/rgba 18 18 24 255))
        ;; fills (one rlgl batch)
        (rl/rl-begin rl/RL-TRIANGLES)
        (doseq [[k [ax ay] [bx by] [cx cy]] tris]
          (rl/rl-color! (if (zero? k) col0 col1))
          (rl/rl-vertex-2f (double ax) (double ay))
          (rl/rl-vertex-2f (double bx) (double by))
          (rl/rl-vertex-2f (double cx) (double cy)))
        (rl/rl-end)
        ;; edges
        (doseq [[_ [ax ay] [bx by] [cx cy]] tris]
          (rl/line! :x1 (int ax) :y1 (int ay) :x2 (int bx) :y2 (int by) :color edge)
          (rl/line! :x1 (int bx) :y1 (int by) :x2 (int cx) :y2 (int cy) :color edge)
          (rl/line! :x1 (int cx) :y1 (int cy) :x2 (int ax) :y2 (int ay) :color edge))
        (rl/text! "Penrose P3 tiling (deflation)" :x 10 :y 10 :size 20 :color rl/RAYWHITE)
        (rl/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn` (this is the LAST alias — its line closes the aliases map with `}}`):
```clojure
           :penrose-tiling   {:main-opts ["-m" "net.b12n.rljlt.penrose-tiling"]}}  ; P3 rhombus tiling (deflation)
```
`check.clj` (this is the LAST require — closes the require vector + ns form with `))`):
```clojure
            net.b12n.rljlt.penrose-tiling))
```
`bb.edn` registry row (LAST row — closes the vector with `]])`):
```clojure
       ["penrose-tiling"    "penrose-tiling" "shapes" "a P3 Penrose rhombus tiling (deflation)"]
```
`bb.edn` task:
```clojure
  penrose-tiling    {:doc "▶ a P3 Penrose rhombus tiling (deflation)"           :task (run-example "penrose-tiling")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check` → all compiled OK.

- [ ] **Step 4: Screenshot smoke**

Run: `RAYLIB_APP_AUTO_QUIT_MS=1800 RAYLIB_APP_SHOT=shot-penrose.png joltc -M:penrose-tiling`
Open `shot-penrose.png`. Expected: a dense aperiodic tiling of orange + blue rhombus-halves radiating from the center with dark edges — a filled decagon, **no** large blank (culled) wedges. If large sectors are missing, the `front` normalizer's sign is inverted — flip the `(> area 0.0)` test to `(< area 0.0)` and re-run. Then `rm shot-penrose.png`.

- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/penrose_tiling.clj deps.edn src/net/b12n/rljlt/check.clj bb.edn
git commit -m "feat(shapes): add penrose-tiling (P3 deflation, rlgl fill)"
```

---

## Task 8: Doc-sync — README + guide pages

Runs **after** all six examples land and render. Bumps counts 63 → 69 and documents `sector!`.

**Files:**
- Modify: `README.md`, `docs/guide/example-catalog.md`, `docs/guide/index.md`, `docs/guide/rlgl-immediate-mode.md`

- [ ] **Step 1: `docs/guide/example-catalog.md`** — bump the title/count "63" → "69" (and the shapes-group count to 26), then add these six rows to the shapes section:

```markdown
| `color-wheel` | an HSV color wheel drawn as an rlgl triangle fan (per-vertex hue) |
| `pie-chart` | labelled pie slices via `rl/sector!` + a legend |
| `splines` | Catmull-Rom / cubic-Bézier / uniform-B-spline through animated points (SPACE cycles) |
| `vector-angle` | the signed angle between two vectors, filled arc + degrees readout (`atan2`) |
| `easings` | a 3×4 grid of balls, each animating on a different easing curve |
| `penrose-tiling` | a P3 Penrose rhombus tiling built by golden-ratio deflation |
```

- [ ] **Step 2: `docs/guide/index.md`** — replace "63 raylib examples" and "tour of all 63" with "69"; leave the group list ("games / core / shapes / text / 3d / generative") unchanged.

- [ ] **Step 3: `docs/guide/rlgl-immediate-mode.md`** — add a short subsection documenting `sector!` as the filled-arc rlgl helper (the immediate-mode stand-in for the by-value-blocked `DrawCircleSector`), noting the front-face winding requirement.

- [ ] **Step 4: `README.md`** — add the six examples to the examples table under the shapes cluster and bump the "63" counts to "69" (and the shapes count). Grep first: `grep -n "63" README.md` and update each count line.

- [ ] **Step 5: Verify counts + parse**

```bash
bb examples | wc -l      # expect 69
bb info | grep shapes    # expect "shapes (26)"
bb tasks >/dev/null && echo "bb.edn parses"
grep -rn "63" README.md docs/guide/*.md   # expect no stale "63 examples" counts
```

- [ ] **Step 6: Commit**

```bash
git add README.md docs/guide/example-catalog.md docs/guide/index.md docs/guide/rlgl-immediate-mode.md
git commit -m "docs: sync README + guide for the 6 shapes-encore examples (63 -> 69)"
```

---

## Task 9: Re-mirror to `b12n-wikis` + arc-close verification

**Files:**
- Modify (in `~/dev/b12n-wikis/`): `b12n-rljlt/*.md` (mirrored guides), `b12n-rljlt/README.md`, top-level `README.md`, `CLAUDE.md`

- [ ] **Step 1: Re-mirror the changed guide pages**

```bash
cp ~/dev/b12n-rljlt/docs/guide/example-catalog.md ~/dev/b12n-wikis/b12n-rljlt/example-catalog.md
cp ~/dev/b12n-rljlt/docs/guide/index.md ~/dev/b12n-wikis/b12n-rljlt/index.md
cp ~/dev/b12n-rljlt/docs/guide/rlgl-immediate-mode.md ~/dev/b12n-wikis/b12n-rljlt/rlgl-immediate-mode.md
```

- [ ] **Step 2: Bump "63" → "69" in the wiki wrapper files**

- `~/dev/b12n-wikis/b12n-rljlt/README.md` — the "63 raylib examples" description; add the shapes-encore set to the prose (color wheel, pie chart, splines, vector angle, easings, Penrose tiling).
- `~/dev/b12n-wikis/README.md` — the Projects-table row for b12n-rljlt (example count).
- `~/dev/b12n-wikis/CLAUDE.md` — the quick-ref row for b12n-rljlt (example count).

Verify: `grep -rn "63" ~/dev/b12n-wikis/b12n-rljlt/ ~/dev/b12n-wikis/README.md ~/dev/b12n-wikis/CLAUDE.md` → no stale counts.

- [ ] **Step 3: Commit the wiki (separate repo)**

```bash
cd ~/dev/b12n-wikis
git add b12n-rljlt/example-catalog.md b12n-rljlt/index.md b12n-rljlt/rlgl-immediate-mode.md b12n-rljlt/README.md README.md CLAUDE.md
git commit -m "re-mirror b12n-rljlt @ <sha>"   # <sha> = the b12n-rljlt doc-sync commit
```

- [ ] **Step 4: Arc-close verification**

```bash
cd ~/dev/b12n-rljlt
joltc -M:check                        # all example namespaces compiled OK
bb examples | wc -l                   # 69
bb info | grep -E "shapes|generative" # shapes (26), generative (7)
git status --short                    # clean (no stray shot-*.png)
```

- [ ] **Step 5: Push both repos**

```bash
cd ~/dev/b12n-rljlt && git push
cd ~/dev/b12n-wikis && git push
```

---

## Self-Review (checked against the spec)

**Spec coverage:**
- `sector!` toolkit helper → Task 1 ✓ (with winding smoke)
- color-wheel, pie-chart, splines, vector-angle, easings, penrose-tiling → Tasks 2–7 ✓
- Four touchpoints per example, no `info` group-list change (shapes already listed) ✓
- Doc-sync (README, example-catalog, index, rlgl-immediate-mode) → Task 8 ✓
- Wiki re-mirror + count bumps + push both → Task 9 ✓
- Verification: `joltc -M:check` + `RAYLIB_APP_SHOT` per example ✓

**Placeholder scan:** no TBD/TODO; every step has concrete code or exact commands. The one `<sha>` in Task 9's wiki commit message is a deliberate fill-in (the doc-sync commit SHA), not a placeholder for code.

**Type/name consistency:** `sector!` signature (`:cx :cy :radius :start-deg :end-deg :segments :color`) is identical in Task 1 (def), Task 3, and Task 5 (calls). All namespaces are hyphen / files underscore. Registry rows and `deps.edn` aliases and `check.clj` requires use matching names across all tasks. `front`/`lerp`/`subdivide`/`wheel` are all defined and used within Task 7.

**Winding correctness:** front face = negative signed area (proven by `shapes.clj` + `color_wheel`); `sector!` emits increasing-angle `rim→center→rim` (front); `vector-angle` passes `lo`/`hi` (increasing); `penrose` normalizes each fill via `front`. All three fan/mesh consumers covered.
