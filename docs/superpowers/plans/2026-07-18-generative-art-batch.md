# Generative-Art Batch (56 → 63) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 generative / math-art examples in a new `generative` group, taking `net.b12n.rljlt` from 56 to 63. No toolkit changes.

**Architecture:** Each example is one namespace on the shared `net.b12n.rljlt.raylib` layer, wired through the four touchpoints. All state is threaded through `loop`/`recur`; all logic is pure Clojure; rendering uses the `rl/*` drawing API + rlgl. A new `generative` group is added to `bb.edn`'s `info` task group order.

**Tech Stack:** jolt (native Clojure on Chez Scheme, no JVM), `jolt.ffi`, raylib 5.5, babashka.

## Global Constraints

- **jolt ≠ JVM.** Verified-available: `Math/sin`, `Math/cos`, `Math/sqrt`, `Math/PI`, character literals (`\F`, `\[`, `\]`, `\+`, `\-`), `(vec "str")` → chars, char map keys, `apply str`, `iterate`/`nth`, `mapcat`, `peek`/`pop`/`into`, sets as predicates. No `case` (use `cond`); no `Math/atan2` (headings via normalized velocity + perpendicular).
- **One binding layer:** every example `(:require [net.b12n.rljlt.raylib :as rl])`.
- **Filenames underscore; namespaces hyphen.** Draw calls take int x/y — coerce with `(int …)`.
- **Alpha:** `(rl/rgba r g b a)` with `a<255` blends (raylib default BLEND_ALPHA).
- **Do NOT rely on framebuffer persistence between frames** (raylib double-buffers). For trails, keep a per-particle position history and redraw each frame after a full `clear-background` — never "skip the clear to accumulate".
- **New `generative` group:** registry rows tagged `"generative"`, and `"generative"` appended to `bb.edn`'s `info` task group-order vector (landed in Task 1).
- **Verification per example:** `joltc -M:check` → OK line, then `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<name>` renders the animating frame (needs an awake display).
- **Stage files by explicit path; never `git add -A`.** Remove `shot-*.png` before committing.

---

## Task 1: `game-of-life` (generative) + add the `generative` group

**Files:**
- Create: `src/net/b12n/rljlt/game_of_life.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn` (registry row + task **+ add `"generative"` to the `info` group list**)

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.game-of-life
  "raylib [generative] example — Conway's Game of Life. A random soup evolves by the
  classic B3/S23 rules on a toroidal grid; SPACE reseeds. State is the set of live
  cells."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def cols 80)
(def rows 45)
(def cell 10)
(def tick 6)

(defn- seed []
  (set (for [c (range cols) r (range rows)
             :when (< (rl/get-random-value 0 99) 28)]
         [c r])))

(defn- neighbors [[c r]]
  (for [dc [-1 0 1] dr [-1 0 1] :when (not (and (zero? dc) (zero? dr)))]
    [(mod (+ c dc) cols) (mod (+ r dr) rows)]))

(defn- next-gen [live]
  (let [candidates (into live (mapcat neighbors live))]
    (set (filter (fn [cell]
                   (let [n (count (filter live (neighbors cell)))]
                     (if (live cell) (or (= n 2) (= n 3)) (= n 3))))
                 candidates))))

(defn -main [& _]
  (rl/window! :width (* cols cell) :height (* rows cell)
              :title "raylib [generative] example - game of life")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           live (seed)]
      (when (rl/keep-running? deadline)
        (let [live (cond (rl/key-pressed? rl/KEY-SPACE) (seed)
                         (zero? (mod frame tick)) (next-gen live)
                         :else live)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[c r] live]
            (rl/rect! :x (* c cell) :y (* r cell) :width (- cell 1) :height (- cell 1) :color rl/LIME))
          (rl/text! (str (count live) " cells - SPACE reseeds") :x 8 :y 6 :size 18 :color rl/RAYWHITE)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) live)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints + add the group**

`deps.edn` (before the `:aliases` closing `}`):
```clojure
           :game-of-life     {:main-opts ["-m" "net.b12n.rljlt.game-of-life"]}     ; Conway's Game of Life
```
`check.clj`:
```clojure
            net.b12n.rljlt.game-of-life
```
`bb.edn` — registry row + task:
```clojure
       ["game-of-life"      "game-of-life" "generative" "Conway's Game of Life (SPACE reseeds)"]
```
```clojure
  game-of-life      {:doc "▶ Conway's Game of Life (SPACE reseeds)" :task (run-example "game-of-life")}
```
`bb.edn` — **add `"generative"` to the `info` task group order.** Change:
```clojure
           (doseq [g ["games" "core" "shapes" "text" "3d"]]
```
to:
```clojure
           (doseq [g ["games" "core" "shapes" "text" "3d" "generative"]]
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-game-of-life.png joltc -M:game-of-life`; PNG shows a field of green cells on black. Also confirm `bb info` now prints a `generative (1)` section.
- [ ] **Step 5: Commit**

```bash
rm -f shot-game-of-life.png
git add src/net/b12n/rljlt/game_of_life.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add game-of-life + the generative group"
```

---

## Task 2: `boids` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/boids.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.boids
  "raylib [generative] example — Reynolds boids. ~70 agents flock via separation,
  alignment, and cohesion; each drawn as a small triangle pointing along its velocity.
  Pure vector math + rlgl triangles."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def n 70)
(def radius 40.0)
(def max-speed 3.0)

(defn- spawn []
  (vec (repeatedly n (fn [] {:x (double (rl/get-random-value 0 width))
                             :y (double (rl/get-random-value 0 height))
                             :vx (- (/ (rl/get-random-value 0 200) 100.0) 1.0)
                             :vy (- (/ (rl/get-random-value 0 200) 100.0) 1.0)}))))

(defn- limit [vx vy m]
  (let [s (Math/sqrt (+ (* vx vx) (* vy vy)))]
    (if (> s m) [(* (/ vx s) m) (* (/ vy s) m)] [vx vy])))

(defn- near-boids [b boids]
  (filter (fn [o] (let [dx (- (:x o) (:x b)) dy (- (:y o) (:y b))]
                    (< (+ (* dx dx) (* dy dy)) (* radius radius))))
          boids))

(defn- step-boid [b boids]
  (let [near (near-boids b boids)
        k (count near)
        ax (/ (reduce + (map :x near)) k)
        ay (/ (reduce + (map :y near)) k)
        avx (/ (reduce + (map :vx near)) k)
        avy (/ (reduce + (map :vy near)) k)
        sx (reduce + (map (fn [o] (- (:x b) (:x o))) near))
        sy (reduce + (map (fn [o] (- (:y b) (:y o))) near))
        vx (+ (:vx b) (* 0.0008 (- ax (:x b))) (* 0.05 (- avx (:vx b))) (* 0.0010 sx))
        vy (+ (:vy b) (* 0.0008 (- ay (:y b))) (* 0.05 (- avy (:vy b))) (* 0.0010 sy))
        [vx vy] (limit vx vy max-speed)]
    {:x (mod (+ (:x b) vx) width) :y (mod (+ (:y b) vy) height) :vx vx :vy vy}))

(defn- draw-boid [{:keys [x y vx vy]}]
  (let [s (Math/sqrt (+ (* vx vx) (* vy vy)))
        s (if (< s 0.001) 1.0 s)
        ux (/ vx s) uy (/ vy s)
        px (- uy) py ux]
    (rl/rl-color! rl/SKYBLUE)
    (rl/rl-begin rl/RL-TRIANGLES)
    (rl/rl-vertex-2f (+ x (* ux 8.0)) (+ y (* uy 8.0)))
    (rl/rl-vertex-2f (+ (- x (* ux 5.0)) (* px 4.0)) (+ (- y (* uy 5.0)) (* py 4.0)))
    (rl/rl-vertex-2f (- (- x (* ux 5.0)) (* px 4.0)) (- (- y (* uy 5.0)) (* py 4.0)))
    (rl/rl-end)))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [generative] example - boids")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           boids (spawn)]
      (when (rl/keep-running? deadline)
        (let [boids (mapv (fn [b] (step-boid b boids)) boids)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 20 20 30 255))
          (doseq [b boids] (draw-boid b))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) boids)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :boids            {:main-opts ["-m" "net.b12n.rljlt.boids"]}            ; flocking simulation
```
`check.clj`:
```clojure
            net.b12n.rljlt.boids
```
`bb.edn`:
```clojure
       ["boids"             "boids" "generative" "flocking birds (separation/alignment/cohesion)"]
```
```clojure
  boids             {:doc "▶ flocking birds (separation/alignment/cohesion)" :task (run-example "boids")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-boids.png joltc -M:boids`; PNG shows sky-blue triangles scattered on the dark background.
- [ ] **Step 5: Commit**

```bash
rm -f shot-boids.png
git add src/net/b12n/rljlt/boids.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add boids"
```

---

## Task 3: `fireworks` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/fireworks.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.fireworks
  "raylib [generative] example — fireworks. Rockets rise and explode into particles
  that fall under gravity and fade out via the alpha channel."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def gravity 0.07)

(defn- palette []
  (nth [[255 80 80] [80 180 255] [255 220 80] [180 120 255] [120 255 160]]
       (rl/get-random-value 0 4)))

(defn- new-rocket []
  {:x (double (rl/get-random-value 100 700)) :y (double height)
   :vy (- (/ (rl/get-random-value 60 85) 10.0)) :color (palette)})

(defn- explode [{:keys [x y color]}]
  (vec (repeatedly 40
         (fn [] (let [a (/ (rl/get-random-value 0 628) 100.0)
                      sp (/ (rl/get-random-value 5 32) 10.0)]
                  {:x x :y y :vx (* sp (Math/cos a)) :vy (* sp (Math/sin a))
                   :life 1.0 :color color})))))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [generative] example - fireworks")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           rockets []
           parts (explode {:x 400.0 :y 170.0 :color [255 220 80]})]
      (when (rl/keep-running? deadline)
        (let [rockets (if (zero? (mod frame 35)) (conj rockets (new-rocket)) rockets)
              rockets (mapv (fn [r] (-> r (update :y + (:vy r)) (update :vy + gravity))) rockets)
              exploded (filter (fn [r] (>= (:vy r) 0)) rockets)
              rockets (filterv (fn [r] (< (:vy r) 0)) rockets)
              parts (into (mapv (fn [p] (-> p (update :x + (:vx p)) (update :y + (:vy p))
                                            (update :vy + gravity) (update :life - 0.012)))
                                parts)
                          (mapcat explode exploded))
              parts (filterv (fn [p] (> (:life p) 0)) parts)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [r rockets]
            (let [[cr cg cb] (:color r)]
              (rl/circle! :x (int (:x r)) :y (int (:y r)) :radius 3 :color (rl/rgba cr cg cb 255))))
          (doseq [p parts]
            (let [[cr cg cb] (:color p)]
              (rl/circle! :x (int (:x p)) :y (int (:y p)) :radius 2
                          :color (rl/rgba cr cg cb (int (* 255 (:life p)))))))
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) rockets parts)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :fireworks        {:main-opts ["-m" "net.b12n.rljlt.fireworks"]}        ; particle fireworks
```
`check.clj`:
```clojure
            net.b12n.rljlt.fireworks
```
`bb.edn`:
```clojure
       ["fireworks"         "fireworks" "generative" "rockets + fading particle bursts"]
```
```clojure
  fireworks         {:doc "▶ rockets + fading particle bursts" :task (run-example "fireworks")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-fireworks.png joltc -M:fireworks`; PNG shows a spread + fading burst (the seeded one) plus a rising rocket, on black.
- [ ] **Step 5: Commit**

```bash
rm -f shot-fireworks.png
git add src/net/b12n/rljlt/fireworks.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add fireworks"
```

---

## Task 4: `fourier-epicycles` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/fourier_epicycles.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.fourier-epicycles
  "raylib [generative] example — a chain of rotating circles (a Fourier series for a
  square wave) whose tip traces the wave. Classic 'drawing with epicycles'."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def cx 200.0)
(def cy 225.0)
(def n-terms 8)
(def scale 55.0)

(defn- epicycles [theta]
  ;; returns {:centers [[x y]…(n+1)] :radii [r…n]}
  (loop [k 1 x cx y cy centers [[cx cy]] radii []]
    (if (> k (dec (* 2 n-terms)))
      {:centers centers :radii radii}
      (let [radius (* scale (/ 4.0 Math/PI) (/ 1.0 k))
            nx (+ x (* radius (Math/cos (* k theta))))
            ny (+ y (* radius (Math/sin (* k theta))))]
        (recur (+ k 2) nx ny (conj centers [nx ny]) (conj radii radius))))))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [generative] example - fourier epicycles")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           theta 0.0
           path []]
      (when (rl/keep-running? deadline)
        (let [{:keys [centers radii]} (epicycles theta)
              [tx ty] (last centers)
              path (vec (take 400 (cons ty path)))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (dotimes [i (count radii)]
            (let [[ox oy] (nth centers i)
                  [nx ny] (nth centers (inc i))]
              (rl/circle-lines! :x (int ox) :y (int oy) :radius (nth radii i) :color (rl/rgba 70 70 80 255))
              (rl/line! :x1 (int ox) :y1 (int oy) :x2 (int nx) :y2 (int ny) :color rl/GRAY)))
          (rl/line! :x1 (int tx) :y1 (int ty) :x2 400 :y2 (int ty) :color (rl/rgba 90 90 90 255))
          (let [pts (map-indexed (fn [i y] [(+ 400.0 (* i 1.0)) y]) path)]
            (doseq [[[x1 y1] [x2 y2]] (partition 2 1 pts)]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/GOLD)))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) (+ theta 0.05) path)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :fourier-epicycles {:main-opts ["-m" "net.b12n.rljlt.fourier-epicycles"]} ; epicycles trace a square wave
```
`check.clj`:
```clojure
            net.b12n.rljlt.fourier-epicycles
```
`bb.edn`:
```clojure
       ["fourier-epicycles" "fourier-epicycles" "generative" "rotating circles trace a square wave"]
```
```clojure
  fourier-epicycles {:doc "▶ rotating circles trace a square wave" :task (run-example "fourier-epicycles")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-fourier-epicycles.png joltc -M:fourier-epicycles`; PNG shows nested gray circles at left + a gold wave trace extending right.
- [ ] **Step 5: Commit**

```bash
rm -f shot-fourier-epicycles.png
git add src/net/b12n/rljlt/fourier_epicycles.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add fourier-epicycles"
```

---

## Task 5: `spirograph` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/spirograph.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.spirograph
  "raylib [generative] example — an animated hypotrochoid (spirograph). A pen offset d
  on a wheel of radius r rolling inside a ring of radius R traces roulette curves;
  resets with new random r/d after a fixed number of points."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def cx 400.0)
(def cy 225.0)
(def big-r 170.0)

(defn- new-params []
  {:r (double (rl/get-random-value 30 95))
   :d (double (rl/get-random-value 40 130))
   :t 0.0 :points []})

(defn- pt [r d t]
  (let [k (/ (- big-r r) r)]
    [(+ cx (* (- big-r r) (Math/cos t)) (* d (Math/cos (* k t))))
     (+ cy (* (- big-r r) (Math/sin t)) (- (* d (Math/sin (* k t)))))]))

(defn- rainbow [i]
  (let [h (mod (* i 3) 360)]
    (cond (< h 60)  (rl/rgba 255 (int (* 255 (/ h 60.0))) 0 255)
          (< h 120) (rl/rgba (int (* 255 (/ (- 120 h) 60.0))) 255 0 255)
          (< h 180) (rl/rgba 0 255 (int (* 255 (/ (- h 120) 60.0))) 255)
          (< h 240) (rl/rgba 0 (int (* 255 (/ (- 240 h) 60.0))) 255 255)
          (< h 300) (rl/rgba (int (* 255 (/ (- h 240) 60.0))) 0 255 255)
          :else     (rl/rgba 255 0 (int (* 255 (/ (- 360 h) 60.0))) 255))))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [generative] example - spirograph")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-params)]
      (when (rl/keep-running? deadline)
        (let [{:keys [r d t points]} st
              new-pts (mapv (fn [i] (pt r d (+ t (* i 0.04)))) (range 8))
              points (into points new-pts)
              t (+ t 0.32)
              st (if (> (count points) 1600) (new-params) (assoc st :t t :points points))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[i [[x1 y1] [x2 y2]]] (map-indexed vector (partition 2 1 (:points st)))]
            (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color (rainbow i)))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :spirograph       {:main-opts ["-m" "net.b12n.rljlt.spirograph"]}       ; hypotrochoid roulette curves
```
`check.clj`:
```clojure
            net.b12n.rljlt.spirograph
```
`bb.edn`:
```clojure
       ["spirograph"        "spirograph" "generative" "animated hypotrochoid roulette curves"]
```
```clojure
  spirograph        {:doc "▶ animated hypotrochoid roulette curves" :task (run-example "spirograph")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-spirograph.png joltc -M:spirograph`; PNG shows a partial rainbow roulette curve centered on black.
- [ ] **Step 5: Commit**

```bash
rm -f shot-spirograph.png
git add src/net/b12n/rljlt/spirograph.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add spirograph"
```

---

## Task 6: `l-system` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/l_system.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.l-system
  "raylib [generative] example — an L-system fractal plant. A string is rewritten by
  production rules, then drawn with turtle graphics (F=forward, +/-=turn, []=branch);
  the plant reveals itself segment by segment, then regrows."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def rules {\X "F+[[X]-X]-F[-FX]+X" \F "FF"})
(def iterations 5)
(def angle-rad (* 25.0 (/ Math/PI 180.0)))
(def step-len 2.4)

(defn- expand [s] (apply str (map (fn [ch] (get rules ch (str ch))) s)))
(defn- lsystem-string [] (nth (iterate expand "X") iterations))

(defn- build-segments [s]
  (let [a0 (- (/ Math/PI 2.0))]                 ; pointing up (screen-y grows downward)
    (loop [chars (seq s) x 400.0 y 445.0 a a0 stack [] segs []]
      (if (empty? chars)
        segs
        (let [ch (first chars) more (rest chars)]
          (cond
            (= ch \F) (let [nx (+ x (* step-len (Math/cos a)))
                            ny (+ y (* step-len (Math/sin a)))]
                        (recur more nx ny a stack (conj segs [x y nx ny])))
            (= ch \+) (recur more x y (+ a angle-rad) stack segs)
            (= ch \-) (recur more x y (- a angle-rad) stack segs)
            (= ch \[) (recur more x y a (conj stack [x y a]) segs)
            (= ch \]) (let [[px py pa] (peek stack)]
                        (recur more px py pa (pop stack) segs))
            :else (recur more x y a stack segs)))))))

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [generative] example - L-system plant")
  (rl/set-target-fps 60)
  (let [segs (build-segments (lsystem-string))
        total (count segs)
        deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           shown 0]
      (when (rl/keep-running? deadline)
        (let [shown (if (>= shown total) 0 (min total (+ shown 40)))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (dotimes [i shown]
            (let [[x1 y1 x2 y2] (nth segs i)]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/LIME)))
          (rl/text! (str total " segments") :x 8 :y 6 :size 18 :color rl/GRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) shown)))))
  (rl/close-window))
```

Sizing note: iteration 5 yields ~1488 `F` segments (`fc(n+1)=3·xc(n)+2·fc(n)`, `xc(n+1)=4·xc(n)`). With `step-len 2.4` and start `(400, 445)`, the plant should fill most of the frame growing upward. **If it overflows or underfills at smoke (Step 4), tune `step-len` (smaller if it overflows, larger if it's tiny) — the only knob.**

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :l-system         {:main-opts ["-m" "net.b12n.rljlt.l-system"]}         ; L-system fractal plant
```
`check.clj`:
```clojure
            net.b12n.rljlt.l-system
```
`bb.edn`:
```clojure
       ["l-system"          "l-system" "generative" "an L-system fractal plant (grows + regrows)"]
```
```clojure
  l-system          {:doc "▶ an L-system fractal plant (grows + regrows)" :task (run-example "l-system")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-l-system.png joltc -M:l-system`; PNG shows a green fractal plant partially grown from the bottom. Tune `step-len` if it overflows/underfills, then re-smoke.
- [ ] **Step 5: Commit**

```bash
rm -f shot-l-system.png
git add src/net/b12n/rljlt/l_system.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add l-system"
```

---

## Task 7: `flow-field` (generative)

**Files:**
- Create: `src/net/b12n/rljlt/flow_field.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.flow-field
  "raylib [generative] example — particles steered by a smooth sine-layered field,
  each leaving a short trail. The field angle is a pure function of position + time;
  trails are per-particle position history (double-buffer-safe), redrawn each frame."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def n 500)
(def speed 1.7)
(def trail-len 16)

(defn- field-angle [x y t]
  (* 2.0 Math/PI (* 0.5 (+ (Math/sin (+ (* x 0.008) t)) (Math/cos (- (* y 0.008) t))))))

(defn- spawn []
  (vec (repeatedly n (fn [] (let [x (double (rl/get-random-value 0 width))
                                  y (double (rl/get-random-value 0 height))]
                              {:x x :y y :trail [[x y]]})))))

(defn- step-part [{:keys [x y trail]} t]
  (let [a (field-angle x y t)
        nx (+ x (* speed (Math/cos a)))
        ny (+ y (* speed (Math/sin a)))
        [nx w1] (cond (< nx 0) [(+ nx width) true] (>= nx width) [(- nx width) true] :else [nx false])
        [ny w2] (cond (< ny 0) [(+ ny height) true] (>= ny height) [(- ny height) true] :else [ny false])
        trail (if (or w1 w2) [[nx ny]] (vec (take trail-len (cons [nx ny] trail))))]
    {:x nx :y ny :trail trail}))

(defn- trail-color [a] (rl/rgba (int (+ 128 (* 100 (Math/cos a)))) 120
                                (int (+ 160 (* 90 (Math/sin a)))) 200))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [generative] example - flow field")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           t 0.0
           parts (spawn)]
      (when (rl/keep-running? deadline)
        (let [parts (mapv (fn [p] (step-part p t)) parts)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [{:keys [x y trail]} parts]
            (let [col (trail-color (field-angle x y t))]
              (doseq [[[x1 y1] [x2 y2]] (partition 2 1 trail)]
                (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color col))))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) (+ t 0.005) parts)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :flow-field       {:main-opts ["-m" "net.b12n.rljlt.flow-field"]}       ; particles in a noise flow field
```
`check.clj`:
```clojure
            net.b12n.rljlt.flow-field
```
`bb.edn`:
```clojure
       ["flow-field"        "flow-field" "generative" "particles steered by a flow field (trails)"]
```
```clojure
  flow-field        {:doc "▶ particles steered by a flow field (trails)" :task (run-example "flow-field")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-flow-field.png joltc -M:flow-field`; PNG shows streaks of colored particle trails flowing across black.
- [ ] **Step 5: Commit**

```bash
rm -f shot-flow-field.png
git add src/net/b12n/rljlt/flow_field.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(generative): add flow-field"
```

---

## Task 8: Doc-sync — README + guide pages

**Files:**
- Modify: `README.md`, `docs/guide/example-catalog.md`, `docs/guide/index.md`

- [ ] **Step 1: Add 7 rows to the README examples table** (after the last games row / at the end)

```markdown
| `game-of-life` | (showcase) | Conway's Game of Life on a toroidal grid (SPACE reseeds) |
| `boids` | (showcase) | Reynolds flocking — separation / alignment / cohesion |
| `fireworks` | (showcase) | rockets + gravity-fading particle bursts (alpha channel) |
| `fourier-epicycles` | (showcase) | a chain of rotating circles traces a square wave |
| `spirograph` | (showcase) | animated hypotrochoid roulette curves |
| `l-system` | (showcase) | an L-system fractal plant (rewrite + turtle graphics) |
| `flow-field` | (showcase) | particles steered by a sine-layered flow field |
```

- [ ] **Step 2: Add a `## generative (7)` section to `docs/guide/example-catalog.md`** (after the `## games` table, before `## core`), and bump the title "56" → "63":

```markdown
## generative (7)

| `bb` name | shows |
|---|---|
| `game-of-life` | Conway's Game of Life (SPACE reseeds) |
| `boids` | flocking birds (separation/alignment/cohesion) |
| `fireworks` | rockets + fading particle bursts |
| `fourier-epicycles` | rotating circles trace a square wave |
| `spirograph` | animated hypotrochoid roulette curves |
| `l-system` | an L-system fractal plant (grows + regrows) |
| `flow-field` | particles steered by a flow field (trails) |
```
Title line: `# The example catalog — 56 raylib demos in jolt` → `63`.

- [ ] **Step 3: Update `docs/guide/index.md`** — "A community suite of 56 raylib examples" → "63", "a tour of all 56 examples grouped" → "63". Also update the grouping line "games / core / shapes / text / 3d" wherever it lists groups, to include `generative`.

- [ ] **Step 4: Verify counts** — Run `bb info`; expect a `generative (7)` group and 63 total (games 10 + core 9 + shapes 20 + text 5 + 3d 12 + generative 7).

- [ ] **Step 5: Commit**

```bash
git add README.md docs/guide/example-catalog.md docs/guide/index.md
git commit -m "docs: sync README + guide for the 7 generative examples (56 → 63)"
```

---

## Task 9: Re-mirror to `b12n-wikis` + arc-close verification

**Files:**
- Modify (in `~/dev/b12n-wikis`): `b12n-rljlt/example-catalog.md`, `b12n-rljlt/index.md`, `README.md`, `CLAUDE.md`, `b12n-rljlt/README.md`

- [ ] **Step 1: Arc-close verification in the project repo**

```bash
cd ~/dev/b12n-rljlt
joltc -M:check                          # expect the OK line
bb info | grep -E '^=== '               # expect generative (7), 63 total
git status --short                      # expect clean
```

- [ ] **Step 2: Re-mirror the changed guide pages**

```bash
cp ~/dev/b12n-rljlt/docs/guide/example-catalog.md ~/dev/b12n-rljlt/docs/guide/index.md ~/dev/b12n-wikis/b12n-rljlt/
```

- [ ] **Step 3: Bump "56" → "63" in the wiki index files** — in `~/dev/b12n-wikis/README.md` (the b12n-rljlt Projects row: both the "56 … examples" and the "56 examples shipped" status), `~/dev/b12n-wikis/CLAUDE.md` (the b12n-rljlt quick-ref row), and `~/dev/b12n-wikis/b12n-rljlt/README.md` (the "56 raylib examples" intro + "tour of all 56"). Optionally note the generative group in the descriptions.

- [ ] **Step 4: Commit + push the wiki**

```bash
cd ~/dev/b12n-wikis
git pull --ff-only origin main
git add b12n-rljlt/ README.md CLAUDE.md
SHA=$(git -C ~/dev/b12n-rljlt rev-parse --short HEAD)
git commit -m "re-mirror b12n-rljlt @ ${SHA}: 7 generative examples (56 → 63)"
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

- **Spec coverage:** the 7 examples → Tasks 1–7 (game-of-life carries the `generative` group + `info` list change); doc-sync → Task 8; wiki re-mirror + arc-close → Task 9. No toolkit task (spec says none). All mapped.
- **Placeholder scan:** every code step has complete code; no TBD/TODO. The `l-system` step-len and the `fireworks` screenshot-frame are named, tunable specifics, not placeholders.
- **jolt-construct check:** char literals / string→chars / char map keys / `apply str` / `iterate` verified via `joltc -e`; no `case`, no `Math/atan2`; `mapcat`/`peek`/`pop`/sets-as-predicates are core. Compile-check per task backstops.
- **Type/name consistency:** every example consumes only existing `rl/*` symbols (drawing API + rlgl + `rgba` + input) — no new binds introduced or referenced. The new `generative` group string is used consistently in each registry row and added once to the `info` list.
- **Group-count arithmetic:** generative 0→7 → 63 total; games 10 + core 9 + shapes 20 + text 5 + 3d 12 + generative 7 = 63. ✓
```
