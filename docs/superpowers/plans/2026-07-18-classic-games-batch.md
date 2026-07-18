# Classic Games Batch (50 → 56) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 6 classic games (snake, breakout, space-invaders, flappy-bird, 2048, minesweeper) plus a `mouse-pressed?`/`MOUSE-RIGHT` toolkit bind, taking `net.b12n.rljlt` from 50 to 56 examples (`games` group 4 → 10).

**Architecture:** Each game is one namespace on the shared `net.b12n.rljlt.raylib` layer, wired through the four touchpoints. All game state is an immutable value threaded through `loop`/`recur`; all logic is pure Clojure; rendering uses the `rl/*` drawing API. Minesweeper additionally needs the new mouse bind.

**Tech Stack:** jolt (native Clojure on Chez Scheme, no JVM), `jolt.ffi`, raylib 5.5, babashka.

## Global Constraints

- **jolt ≠ JVM.** Use only confirmed-working core Clojure. This plan deliberately avoids `case` (uses `cond`) and needs no `Math/*` (games use integer/float arithmetic only). Sets/maps/vectors, `loop`/`recur`, `reduce`, `mapv`/`filterv`, `for` (single `:when`), `peek`/`pop`/`into`, `cond->`, `if-let`, `apply mapv vector`, `contains?`/`disj`/`conj` are all fine.
- **One binding layer:** every game `(:require [net.b12n.rljlt.raylib :as rl])`, calls only `rl/*`. New raw binds live in `raylib.clj`.
- **Filenames use underscores; namespaces use hyphens.**
- **Draw calls that take x/y expect ints** — coerce doubles with `(int …)`. `get-random-value` returns an int; `key-pressed?`/`mouse-pressed?` are edge-triggered (true only on the press frame).
- **Four touchpoints per game:** source ns + `deps.edn` alias + `check.clj` require + `bb.edn` registry row/task. Group = `games`.
- **Verification per game:** `joltc -M:check` prints `net.b12n.rljlt: all example namespaces compiled OK`, then `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<name>` renders the initial/animating frame (needs an awake display; gameplay correctness is validated by construction + manual `bb <name>`).
- **Stage files by explicit path; never `git add -A`/`.`/`-u`.** Remove `shot-*.png` (gitignored) before committing.

---

## Task 1: Toolkit — `mouse-pressed?` + `MOUSE-RIGHT`

**Files:**
- Modify: `src/net/b12n/rljlt/raylib.clj`

**Interfaces:**
- Produces: `(rl/mouse-pressed? button)` → boolean; `rl/MOUSE-RIGHT` = 1.

- [ ] **Step 1: Add the raw bind** next to the other input `^:private` raws (after `mouse-down-raw`)

```clojure
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
```

- [ ] **Step 2: Add the predicate** next to `mouse-down?` in the predicates block

```clojure
(defn mouse-pressed? [b] (not (zero? (bit-and (mouse-pressed-raw b) 0xff))))
```

- [ ] **Step 3: Add the const** next to `MOUSE-LEFT`

```clojure
(def ^:const MOUSE-RIGHT 1)
```

- [ ] **Step 4: Compile-check** — Run `joltc -M:check`; expect the OK line. (Visual proof arrives in Task 7, minesweeper.)
- [ ] **Step 5: Commit**

```bash
git add src/net/b12n/rljlt/raylib.clj
git commit -m "feat(raylib): add mouse-pressed? + MOUSE-RIGHT for click games"
```

---

## Task 2: `snake` (games)

**Files:**
- Create: `src/net/b12n/rljlt/snake.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.snake
  "raylib [games] example — the classic snake. Arrow keys steer; eat food to grow;
  hitting a wall or yourself ends it (SPACE restarts). Grid + frame-tick movement,
  all state threaded through the loop."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def cols 32)
(def rows 18)
(def cell 25)
(def tick 6)   ; advance one cell every N frames

(defn- rand-food [snake]
  (loop []
    (let [f [(rl/get-random-value 0 (dec cols)) (rl/get-random-value 0 (dec rows))]]
      (if (some (fn [s] (= s f)) snake) (recur) f))))

(defn- new-game []
  (let [snake [[16 9] [15 9] [14 9]]]
    {:snake snake :dir [1 0] :food (rand-food snake) :dead? false}))

(defn- turn [dir]
  (let [want (cond (rl/key-pressed? rl/KEY-UP)    [0 -1]
                   (rl/key-pressed? rl/KEY-DOWN)  [0 1]
                   (rl/key-pressed? rl/KEY-LEFT)  [-1 0]
                   (rl/key-pressed? rl/KEY-RIGHT) [1 0]
                   :else nil)]
    (if (and want (not= want [(- (first dir)) (- (second dir))])) want dir)))

(defn- step [{:keys [snake dir food] :as st}]
  (let [[hc hr] (first snake)
        [dc dr] dir
        head [(+ hc dc) (+ hr dr)]
        [nc nr] head]
    (if (or (< nc 0) (>= nc cols) (< nr 0) (>= nr rows) (some (fn [s] (= s head)) snake))
      (assoc st :dead? true)
      (if (= head food)
        (let [ns (into [head] snake)]
          (assoc st :snake ns :food (rand-food ns)))
        (assoc st :snake (into [head] (pop (vec snake))))))))

(defn -main [& _]
  (rl/window! :width (* cols cell) :height (* rows cell) :title "raylib [games] example - snake")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (:dead? st)
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (let [st (assoc st :dir (turn (:dir st)))]
                     (if (zero? (mod frame tick)) (step st) st)))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (let [[fc fr] (:food st)]
            (rl/rect! :x (* fc cell) :y (* fr cell) :width cell :height cell :color rl/RED))
          (doseq [[c r] (:snake st)]
            (rl/rect! :x (+ 1 (* c cell)) :y (+ 1 (* r cell))
                      :width (- cell 2) :height (- cell 2) :color rl/LIME))
          (rl/text! (str "len " (count (:snake st))) :x 8 :y 6 :size 20 :color rl/RAYWHITE)
          (when (:dead? st)
            (rl/text! "GAME OVER - SPACE to restart" :x 150 :y 210 :size 24 :color rl/RAYWHITE))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn` (in `:aliases`, before the closing `}`):
```clojure
           :snake            {:main-opts ["-m" "net.b12n.rljlt.snake"]}            ; the classic snake
```
`check.clj` (in the `:require` block):
```clojure
            net.b12n.rljlt.snake
```
`bb.edn` — registry row in `examples` (games region) + task:
```clojure
       ["snake"             "snake" "games" "the classic snake (arrow keys, grow, don't crash)"]
```
```clojure
  snake             {:doc "▶ the classic snake (arrow keys, grow, don't crash)" :task (run-example "snake")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-snake.png joltc -M:snake`; PNG shows a green snake near center + a red food cell on black.
- [ ] **Step 5: Commit**

```bash
rm -f shot-snake.png
git add src/net/b12n/rljlt/snake.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add snake"
```

---

## Task 3: `breakout` (games)

**Files:**
- Create: `src/net/b12n/rljlt/breakout.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.breakout
  "raylib [games] example — breakout. The paddle follows the mouse; bounce the ball
  to clear every brick. Ball/wall/paddle/brick collisions computed in Clojure."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def cols 10)
(def brk-rows 6)
(def brick-w 80)
(def brick-h 24)
(def top 50)
(def paddle-w 100)
(def paddle-h 14)
(def paddle-y 420)
(def ball-r 8)
(def row-colors [rl/RED rl/ORANGE rl/GOLD rl/GREEN rl/SKYBLUE rl/VIOLET])

(defn- abs [n] (if (neg? n) (- n) n))

(defn- all-bricks [] (set (for [c (range cols) r (range brk-rows)] [c r])))
(defn- new-ball [] {:x 400.0 :y 300.0 :vx 3.0 :vy -3.0})
(defn- new-game [] {:bricks (all-bricks) :ball (new-ball) :lives 3 :over? false :won? false})

(defn- brick-at [x y]
  (let [c (quot (int x) brick-w)
        r (quot (- (int y) top) brick-h)]
    (when (and (>= (- (int y) top) 0) (< r brk-rows) (>= c 0) (< c cols)) [c r])))

(defn- step [{:keys [ball bricks lives] :as st} paddle-x]
  (let [{:keys [x y vx vy]} ball
        nx (+ x vx) ny (+ y vy)
        [nx vx] (cond (< nx ball-r) [ball-r (- vx)]
                      (> nx (- width ball-r)) [(- width ball-r) (- vx)]
                      :else [nx vx])
        [ny vy] (if (< ny ball-r) [ball-r (- vy)] [ny vy])
        on-paddle? (and (> vy 0)
                        (>= (+ ny ball-r) paddle-y)
                        (<= (+ ny ball-r) (+ paddle-y paddle-h 8))
                        (>= nx paddle-x) (<= nx (+ paddle-x paddle-w)))
        vx (if on-paddle? (+ vx (* 0.08 (- nx (+ paddle-x (/ paddle-w 2.0))))) vx)
        vy (if on-paddle? (- (abs vy)) vy)
        hit (brick-at nx ny)
        hit? (and hit (contains? bricks hit))
        bricks (if hit? (disj bricks hit) bricks)
        vy (if hit? (- vy) vy)
        lost? (> ny (+ height 20))]
    (cond
      (empty? bricks) (assoc st :won? true)
      lost? (if (<= lives 1)
              (assoc st :over? true :lives 0)
              (assoc st :lives (dec lives) :ball (new-ball)))
      :else (assoc st :bricks bricks :ball {:x nx :y ny :vx vx :vy vy}))))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [games] example - breakout")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              paddle-x (double (max 0 (min (- width paddle-w) (- mx (quot paddle-w 2)))))
              st (if (or (:over? st) (:won? st))
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st paddle-x))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[c r] (:bricks st)]
            (rl/rect! :x (+ 1 (* c brick-w)) :y (+ 1 (+ top (* r brick-h)))
                      :width (- brick-w 2) :height (- brick-h 2) :color (nth row-colors r)))
          (rl/rect! :x (int paddle-x) :y paddle-y :width paddle-w :height paddle-h :color rl/DARKGRAY)
          (let [{:keys [x y]} (:ball st)]
            (rl/circle! :x (int x) :y (int y) :radius ball-r :color rl/MAROON))
          (rl/text! (str "lives " (:lives st)) :x 8 :y 8 :size 20 :color rl/DARKGRAY)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 210 :size 28 :color rl/MAROON))
          (when (:won? st) (rl/text! "YOU WIN! - SPACE" :x 290 :y 210 :size 28 :color rl/DARKGREEN))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :breakout         {:main-opts ["-m" "net.b12n.rljlt.breakout"]}         ; paddle + ball + bricks
```
`check.clj`:
```clojure
            net.b12n.rljlt.breakout
```
`bb.edn`:
```clojure
       ["breakout"          "breakout" "games" "paddle + ball + brick grid (mouse paddle)"]
```
```clojure
  breakout          {:doc "▶ paddle + ball + brick grid (mouse paddle)" :task (run-example "breakout")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-breakout.png joltc -M:breakout`; PNG shows a colored brick grid, a paddle, and the ball.
- [ ] **Step 5: Commit**

```bash
rm -f shot-breakout.png
git add src/net/b12n/rljlt/breakout.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add breakout"
```

---

## Task 4: `space-invaders` (games)

**Files:**
- Create: `src/net/b12n/rljlt/space_invaders.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.space-invaders
  "raylib [games] example — space invaders. ←/→ move, SPACE shoots; clear the
  marching alien grid before it reaches you. Formation march + AABB hits, all in
  Clojure."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def acols 8)
(def arows 4)
(def sp 60)
(def alien-w 40)
(def alien-h 26)
(def ship-y 420)
(def ship-w 50)

(defn- new-game []
  {:ship-x 375.0 :bullets []
   :aliens (set (for [c (range acols) r (range arows)] [c r]))
   :ax 40.0 :ay 40.0 :adir 1.0 :cooldown 0 :over? false :won? false :score 0})

(defn- alien-px [ax c] (+ ax (* c sp)))
(defn- alien-py [ay r] (+ ay (* r 45)))

(defn- march [{:keys [aliens ax ay adir] :as st}]
  (let [cs (map first aliens)
        minc (reduce min cs) maxc (reduce max cs)
        nax (+ ax (* adir 1.2))]
    (if (or (< (+ nax (* minc sp)) 6)
            (> (+ (alien-px nax maxc) alien-w) (- width 6)))
      (assoc st :adir (- adir) :ay (+ ay 18))
      (assoc st :ax nax))))

(defn- hit-alien [aliens bx by ax ay]
  (some (fn [[c r]]
          (let [px (alien-px ax c) py (alien-py ay r)]
            (when (and (>= bx px) (<= bx (+ px alien-w))
                       (>= by py) (<= by (+ py alien-h)))
              [c r])))
        aliens))

(defn- step [{:keys [ship-x bullets aliens ax ay cooldown score] :as st}]
  (let [ship-x (cond (rl/key-down? rl/KEY-LEFT)  (max 0.0 (- ship-x 5.0))
                     (rl/key-down? rl/KEY-RIGHT) (min (- width ship-w) (+ ship-x 5.0))
                     :else ship-x)
        shoot? (and (rl/key-pressed? rl/KEY-SPACE) (zero? cooldown))
        bullets (cond-> (mapv (fn [b] (update b :y - 8.0)) bullets)
                  shoot? (conj {:x (+ ship-x (/ ship-w 2.0)) :y ship-y}))
        bullets (filterv (fn [b] (> (:y b) -10)) bullets)
        cooldown (cond shoot? 15 (pos? cooldown) (dec cooldown) :else 0)
        result (reduce (fn [acc b]
                         (if-let [a (hit-alien (:aliens acc) (:x b) (:y b) ax ay)]
                           (-> acc (update :aliens disj a) (update :score + 10))
                           (update acc :bullets conj b)))
                       {:aliens aliens :bullets [] :score score}
                       bullets)
        st (assoc st :ship-x ship-x :bullets (:bullets result)
                  :aliens (:aliens result) :cooldown cooldown :score (:score result))
        st (if (seq (:aliens st)) (march st) st)
        lowest (if (seq (:aliens st)) (reduce max (map second (:aliens st))) -1)
        reached? (and (>= lowest 0) (>= (+ (alien-py (:ay st) lowest) alien-h) ship-y))]
    (cond (empty? (:aliens st)) (assoc st :won? true)
          reached? (assoc st :over? true)
          :else st)))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [games] example - space invaders")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (or (:over? st) (:won? st))
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[c r] (:aliens st)]
            (rl/rect! :x (int (alien-px (:ax st) c)) :y (int (alien-py (:ay st) r))
                      :width alien-w :height alien-h :color rl/LIME))
          (doseq [{:keys [x y]} (:bullets st)]
            (rl/rect! :x (int x) :y (int y) :width 4 :height 12 :color rl/GOLD))
          (rl/rect! :x (int (:ship-x st)) :y ship-y :width ship-w :height 16 :color rl/SKYBLUE)
          (rl/text! (str "score " (:score st)) :x 8 :y 8 :size 20 :color rl/RAYWHITE)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 210 :size 28 :color rl/RED))
          (when (:won? st) (rl/text! "YOU WIN! - SPACE" :x 290 :y 210 :size 28 :color rl/LIME))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :space-invaders   {:main-opts ["-m" "net.b12n.rljlt.space-invaders"]}   ; marching aliens shooter
```
`check.clj`:
```clojure
            net.b12n.rljlt.space-invaders
```
`bb.edn`:
```clojure
       ["space-invaders"    "space-invaders" "games" "marching aliens (arrows + SPACE to shoot)"]
```
```clojure
  space-invaders    {:doc "▶ marching aliens (arrows + SPACE to shoot)" :task (run-example "space-invaders")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-space-invaders.png joltc -M:space-invaders`; PNG shows a green alien grid at top + a blue ship at the bottom on black.
- [ ] **Step 5: Commit**

```bash
rm -f shot-space-invaders.png
git add src/net/b12n/rljlt/space_invaders.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add space-invaders"
```

---

## Task 5: `flappy-bird` (games)

**Files:**
- Create: `src/net/b12n/rljlt/flappy_bird.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.flappy-bird
  "raylib [games] example — flappy bird. SPACE to flap; fly through the pipe gaps.
  Gravity + scrolling pipes + AABB/circle collision, all in Clojure."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def width 800)
(def height 450)
(def bird-x 150)
(def bird-r 14)
(def gravity 0.4)
(def flap -7.0)
(def pipe-w 70)
(def gap-h 140)
(def scroll 3.0)
(def spacing 300)

(defn- rand-gap [] (rl/get-random-value 80 (- height 80 gap-h)))

(defn- new-game []
  {:y 225.0 :vy 0.0 :score 0 :over? false
   :pipes (mapv (fn [i] {:x (+ 500 (* i spacing)) :gap (rand-gap) :scored false})
                (range 3))})

(defn- step [{:keys [y vy pipes score] :as st}]
  (let [vy (+ vy gravity)
        vy (if (rl/key-pressed? rl/KEY-SPACE) flap vy)
        ny (+ y vy)
        pipes (mapv (fn [p]
                      (let [nx (- (:x p) scroll)]
                        (if (< nx (- pipe-w))
                          {:x (+ nx (* 3 spacing)) :gap (rand-gap) :scored false}
                          (assoc p :x nx))))
                    pipes)
        passed (count (filter (fn [p] (and (not (:scored p)) (< (+ (:x p) pipe-w) bird-x))) pipes))
        pipes (mapv (fn [p] (if (and (not (:scored p)) (< (+ (:x p) pipe-w) bird-x))
                              (assoc p :scored true) p)) pipes)
        hit-pipe? (some (fn [p]
                          (and (< (- bird-x bird-r) (+ (:x p) pipe-w))
                               (> (+ bird-x bird-r) (:x p))
                               (or (< (- ny bird-r) (:gap p))
                                   (> (+ ny bird-r) (+ (:gap p) gap-h)))))
                        pipes)
        oob? (or (< (- ny bird-r) 0) (> (+ ny bird-r) height))]
    (if (or hit-pipe? oob?)
      (assoc st :over? true :y ny :vy vy)
      (assoc st :y ny :vy vy :pipes pipes :score (+ score passed)))))

(defn -main [& _]
  (rl/window! :width width :height height :title "raylib [games] example - flappy bird")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (:over? st)
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st))]
          (rl/begin-drawing)
          (rl/clear-background rl/SKYBLUE)
          (doseq [p (:pipes st)]
            (rl/rect! :x (int (:x p)) :y 0 :width pipe-w :height (int (:gap p)) :color rl/DARKGREEN)
            (rl/rect! :x (int (:x p)) :y (int (+ (:gap p) gap-h)) :width pipe-w
                      :height (- height (int (+ (:gap p) gap-h))) :color rl/DARKGREEN))
          (rl/circle! :x bird-x :y (int (:y st)) :radius bird-r :color rl/GOLD)
          (rl/text! (str "score " (:score st)) :x 8 :y 8 :size 20 :color rl/DARKBLUE)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 200 :size 28 :color rl/MAROON))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :flappy-bird      {:main-opts ["-m" "net.b12n.rljlt.flappy-bird"]}      ; flap through the pipes
```
`check.clj`:
```clojure
            net.b12n.rljlt.flappy-bird
```
`bb.edn`:
```clojure
       ["flappy-bird"       "flappy-bird" "games" "flap through the pipe gaps (SPACE)"]
```
```clojure
  flappy-bird       {:doc "▶ flap through the pipe gaps (SPACE)" :task (run-example "flappy-bird")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-flappy-bird.png joltc -M:flappy-bird`; PNG shows the gold bird + green pipes scrolling in on sky blue.
- [ ] **Step 5: Commit**

```bash
rm -f shot-flappy-bird.png
git add src/net/b12n/rljlt/flappy_bird.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add flappy-bird"
```

---

## Task 6: 2048 (games) — handle `game-2048`

Note: neither a Clojure symbol nor a babashka task name may start with a digit — `bb 2048` is unreachable (verified: `bb` reads `2048` as a Long → "No such task", and it doesn't even list). So the **handle is `game-2048` everywhere** (ns `net.b12n.rljlt.game-2048`, file `game_2048.clj`, deps alias `:game-2048`, registry name + bb task `game-2048`); the **window title, README, and description carry the "2048" branding**. Commands: `bb game-2048` / `joltc -M:game-2048` / `bb run game-2048`.

**Files:**
- Create: `src/net/b12n/rljlt/game_2048.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.game-2048
  "raylib [games] example — 2048. Arrow keys slide + merge tiles on a 4x4 board;
  reach 2048. Slide/merge is one pure function reused for all four directions via
  row reversal / transpose."
  (:require [net.b12n.rljlt.raylib :as rl]))

(defn- compress [row] (vec (remove zero? row)))

(defn- merge-row [row]                       ; row is compressed (no zeros)
  (loop [in row out [] score 0]
    (cond
      (empty? in) [out score]
      (= (count in) 1) [(conj out (first in)) score]
      (= (first in) (second in)) (recur (drop 2 in) (conj out (* 2 (first in)))
                                        (+ score (* 2 (first in))))
      :else (recur (rest in) (conj out (first in)) score))))

(defn- slide-left [row]
  (let [[merged score] (merge-row (compress row))]
    [(vec (take 4 (concat merged (repeat 0)))) score]))

(defn- rows [board] (mapv vec (partition 4 board)))
(defn- from-rows [rs] (vec (apply concat rs)))
(defn- transpose [rs] (apply mapv vector rs))
(defn- revrows [rs] (mapv (fn [r] (vec (reverse r))) rs))

(defn- slide-board [rs]
  (let [results (mapv slide-left rs)]
    [(mapv first results) (reduce + (mapv second results))]))

(defn- move [board dir]
  (let [rs (rows board)]
    (cond
      (= dir :left)  (let [[nr g] (slide-board rs)] [(from-rows nr) g])
      (= dir :right) (let [[nr g] (slide-board (revrows rs))] [(from-rows (revrows nr)) g])
      (= dir :up)    (let [[nr g] (slide-board (transpose rs))] [(from-rows (transpose nr)) g])
      (= dir :down)  (let [[nr g] (slide-board (revrows (transpose rs)))]
                       [(from-rows (transpose (revrows nr))) g]))))

(defn- spawn [board]
  (let [empties (filterv (fn [i] (zero? (nth board i))) (range 16))]
    (if (empty? empties)
      board
      (let [i (nth empties (rl/get-random-value 0 (dec (count empties))))
            v (if (< (rl/get-random-value 0 9) 9) 2 4)]
        (assoc board i v)))))

(defn- new-board [] (spawn (spawn (vec (repeat 16 0)))))

(defn- try-move [{:keys [board score] :as st} dir]
  (let [[nb g] (move board dir)]
    (if (= nb board) st (assoc st :board (spawn nb) :score (+ score g)))))

(defn- stuck? [board]
  (every? (fn [dir] (= board (first (move board dir)))) [:left :right :up :down]))

(def tile-colors
  {0 (rl/rgba 205 193 180 255)  2 (rl/rgba 238 228 218 255)
   4 (rl/rgba 237 224 200 255)  8 (rl/rgba 242 177 121 255)
   16 (rl/rgba 245 149 99 255)  32 (rl/rgba 246 124 95 255)
   64 (rl/rgba 246 94 59 255)   128 (rl/rgba 237 207 114 255)
   256 (rl/rgba 237 204 97 255) 512 (rl/rgba 237 200 80 255)
   1024 (rl/rgba 237 197 63 255) 2048 (rl/rgba 237 194 46 255)})

(defn -main [& _]
  (rl/window! :width 800 :height 450 :title "raylib [games] example - 2048")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st {:board (new-board) :score 0}]
      (when (rl/keep-running? deadline)
        (let [over? (stuck? (:board st))
              st (cond
                   over? (if (rl/key-pressed? rl/KEY-SPACE) {:board (new-board) :score 0} st)
                   (rl/key-pressed? rl/KEY-LEFT)  (try-move st :left)
                   (rl/key-pressed? rl/KEY-RIGHT) (try-move st :right)
                   (rl/key-pressed? rl/KEY-UP)    (try-move st :up)
                   (rl/key-pressed? rl/KEY-DOWN)  (try-move st :down)
                   :else st)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 187 173 160 255))
          (dotimes [i 16]
            (let [c (mod i 4) r (quot i 4)
                  v (nth (:board st) i)
                  x (+ 210 (* c 95)) y (+ 40 (* r 95))]
              (rl/rect! :x x :y y :width 88 :height 88
                        :color (get tile-colors v (get tile-colors 2048)))
              (when (pos? v)
                (rl/text! (str v) :x (+ x 8) :y (+ y 28) :size 32 :color rl/DARKGRAY))))
          (rl/text! (str "score " (:score st)) :x 20 :y 20 :size 24 :color rl/RAYWHITE)
          (when over? (rl/text! "GAME OVER - SPACE" :x 250 :y 415 :size 24 :color rl/RAYWHITE))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints** (note the `game-2048` ns behind the `2048` alias)

`deps.edn`:
```clojure
           :game-2048        {:main-opts ["-m" "net.b12n.rljlt.game-2048"]}        ; 2048 tile-merge puzzle
```
`check.clj`:
```clojure
            net.b12n.rljlt.game-2048
```
`bb.edn`:
```clojure
       ["game-2048"         "game-2048" "games" "2048: 4x4 tile-merge puzzle (arrow keys)"]
```
```clojure
  game-2048         {:doc "▶ 2048: 4x4 tile-merge puzzle (arrow keys)" :task (run-example "game-2048")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-game-2048.png joltc -M:game-2048`; PNG shows the 4×4 board with two starting tiles (a `2` and a `2`/`4`).
- [ ] **Step 5: Commit**

```bash
rm -f shot-game-2048.png
git add src/net/b12n/rljlt/game_2048.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add 2048 (handle game-2048)"
```

---

## Task 7: `minesweeper` (games) — first `mouse-pressed?` user

**Files:**
- Create: `src/net/b12n/rljlt/minesweeper.clj`
- Modify: `deps.edn`, `check.clj`, `bb.edn`

**Interfaces:**
- Consumes: `rl/mouse-pressed?`, `rl/MOUSE-LEFT`, `rl/MOUSE-RIGHT` (Task 1), `rl/get-mouse-x/y`.

- [ ] **Step 1: Create the source ns**

```clojure
(ns net.b12n.rljlt.minesweeper
  "raylib [games] example — minesweeper. Left-click reveals (0-cells flood-fill),
  right-click flags; find every safe cell without hitting a mine (SPACE restarts).
  Exercises the mouse-pressed? / MOUSE-RIGHT toolkit binds."
  (:require [net.b12n.rljlt.raylib :as rl]))

(def cols 16)
(def rows 12)
(def cell 30)
(def n-mines 30)
(def top 40)

(defn- place-mines []
  (loop [mines #{}]
    (if (>= (count mines) n-mines)
      mines
      (recur (conj mines [(rl/get-random-value 0 (dec cols))
                          (rl/get-random-value 0 (dec rows))])))))

(defn- new-game [] {:mines (place-mines) :revealed #{} :flagged #{} :over? false :won? false})

(defn- neighbors [[c r]]
  (filterv (fn [[nc nr]] (and (>= nc 0) (< nc cols) (>= nr 0) (< nr rows)))
           (for [dc [-1 0 1] dr [-1 0 1] :when (not (and (zero? dc) (zero? dr)))]
             [(+ c dc) (+ r dr)])))

(defn- mine-count [mines cell] (count (filter mines (neighbors cell))))

(defn- reveal [{:keys [mines revealed] :as st} cell]
  (cond
    (revealed cell) st
    (contains? mines cell) (assoc st :over? true)
    :else
    (loop [stack [cell] rev revealed]
      (if (empty? stack)
        (assoc st :revealed rev)
        (let [c (peek stack) stack (pop stack)]
          (if (rev c)
            (recur stack rev)
            (let [rev (conj rev c)]
              (if (zero? (mine-count mines c))
                (recur (into stack (remove rev (neighbors c))) rev)
                (recur stack rev)))))))))

(defn- toggle-flag [{:keys [flagged] :as st} cell]
  (assoc st :flagged (if (flagged cell) (disj flagged cell) (conj flagged cell))))

(defn- won? [{:keys [mines revealed]}]
  (= (count revealed) (- (* cols rows) (count mines))))

(defn- cell-at [mx my]
  (let [c (quot mx cell) r (quot (- my top) cell)]
    (when (and (>= (- my top) 0) (>= c 0) (< c cols) (>= r 0) (< r rows)) [c r])))

(defn -main [& _]
  (rl/window! :width (* cols cell) :height (+ top (* rows cell))
              :title "raylib [games] example - minesweeper")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [done? (or (:over? st) (:won? st))
              st (cond
                   done? (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (rl/mouse-pressed? rl/MOUSE-LEFT)
                   (if-let [cl (cell-at (rl/get-mouse-x) (rl/get-mouse-y))]
                     (let [st2 (reveal st cl)] (if (won? st2) (assoc st2 :won? true) st2))
                     st)
                   (rl/mouse-pressed? rl/MOUSE-RIGHT)
                   (if-let [cl (cell-at (rl/get-mouse-x) (rl/get-mouse-y))]
                     (toggle-flag st cl) st)
                   :else st)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (dotimes [i (* cols rows)]
            (let [c (mod i cols) r (quot i cols)
                  cl [c r]
                  x (* c cell) y (+ top (* r cell))
                  revealed? (contains? (:revealed st) cl)
                  flagged? (contains? (:flagged st) cl)
                  mine? (contains? (:mines st) cl)]
              (cond
                (and revealed? mine?)
                (rl/rect! :x x :y y :width cell :height cell :color rl/RED)
                revealed?
                (let [n (mine-count (:mines st) cl)]
                  (rl/rect! :x x :y y :width cell :height cell :color rl/LIGHTGRAY)
                  (when (pos? n)
                    (rl/text! (str n) :x (+ x 9) :y (+ y 5) :size 22 :color rl/DARKBLUE)))
                :else
                (rl/rect! :x x :y y :width cell :height cell :color rl/GRAY))
              (rl/rect-lines! :x x :y y :width cell :height cell :color rl/DARKGRAY)
              (when (and flagged? (not revealed?))
                (rl/rect! :x (+ x 8) :y (+ y 8) :width (- cell 16) :height (- cell 16) :color rl/ORANGE))
              (when (and (:over? st) mine? (not revealed?))
                (rl/circle! :x (+ x (quot cell 2)) :y (+ y (quot cell 2)) :radius 6 :color rl/BLACK))))
          (rl/text! (cond (:won? st) "YOU WIN! - SPACE"
                          (:over? st) "BOOM! - SPACE"
                          :else "L: reveal   R: flag")
                    :x 8 :y 10 :size 20 :color (if (:won? st) rl/DARKGREEN rl/MAROON))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))
```

- [ ] **Step 2: Wire the four touchpoints**

`deps.edn`:
```clojure
           :minesweeper      {:main-opts ["-m" "net.b12n.rljlt.minesweeper"]}      ; reveal/flag grid (mouse L/R)
```
`check.clj`:
```clojure
            net.b12n.rljlt.minesweeper
```
`bb.edn`:
```clojure
       ["minesweeper"       "minesweeper" "games" "reveal/flag grid (mouse L reveal, R flag)"]
```
```clojure
  minesweeper       {:doc "▶ reveal/flag grid (mouse L reveal, R flag)" :task (run-example "minesweeper")}
```

- [ ] **Step 3: Compile-check** — `joltc -M:check`; expect the OK line.
- [ ] **Step 4: Screenshot smoke (validates the new binds)** — `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-minesweeper.png joltc -M:minesweeper`; PNG shows a 16×12 grid of gray unrevealed cells + the "L: reveal  R: flag" header. (Runs clean = `mouse-pressed?` resolved.)
- [ ] **Step 5: Commit**

```bash
rm -f shot-minesweeper.png
git add src/net/b12n/rljlt/minesweeper.clj deps.edn bb.edn src/net/b12n/rljlt/check.clj
git commit -m "feat(games): add minesweeper (first mouse-pressed? user)"
```

---

## Task 8: Doc-sync — README + guide pages

**Files:**
- Modify: `README.md`, `docs/guide/example-catalog.md`, `docs/guide/index.md`

- [ ] **Step 1: Add 6 rows to the README examples table** (after the `vampire-survivors` row / end of the games block)

```markdown
| `snake` | (game) | the classic snake — arrow keys, grow, don't crash |
| `breakout` | (game) | paddle (mouse) + ball + brick grid, clear to win |
| `space-invaders` | (game) | marching alien grid, shoot up (arrows + SPACE) |
| `flappy-bird` | (game) | flap through scrolling pipe gaps (SPACE) |
| `game-2048` | (game) | **2048** — 4×4 tile-merge puzzle (arrow keys) |
| `minesweeper` | (game) | reveal/flag grid, flood-fill (mouse L/R) — new `mouse-pressed?` |
```

- [ ] **Step 2: Update `docs/guide/example-catalog.md`** — change `## games (4)` → `## games (10)`, add the 6 rows to the games table, and change the title "50" → "56":

```markdown
| `snake` | the classic snake (arrow keys, grow, don't crash) |
| `breakout` | paddle + ball + brick grid (mouse paddle) |
| `space-invaders` | marching aliens (arrows + SPACE to shoot) |
| `flappy-bird` | flap through the pipe gaps (SPACE) |
| `game-2048` | 2048: 4x4 tile-merge puzzle (arrow keys) |
| `minesweeper` | reveal/flag grid (mouse L reveal, R flag) |
```
Title line: `# The example catalog — 50 raylib demos in jolt` → `56`.

- [ ] **Step 3: Update `docs/guide/index.md`** — "A community suite of 50 raylib examples" → "56", and "a tour of all 50 examples grouped" → "56".

- [ ] **Step 4: Verify counts** — Run `bb info`; expect `games (10)` and 56 total (games 10 + core 9 + shapes 20 + text 5 + 3d 12).

- [ ] **Step 5: Commit**

```bash
git add README.md docs/guide/example-catalog.md docs/guide/index.md
git commit -m "docs: sync README + guide for the 6 new games (50 → 56)"
```

---

## Task 9: Re-mirror to `b12n-wikis` + arc-close verification

**Files:**
- Modify (in `~/dev/b12n-wikis`): `b12n-rljlt/example-catalog.md`, `b12n-rljlt/index.md`, `README.md`, `CLAUDE.md`, `b12n-rljlt/README.md`

- [ ] **Step 1: Arc-close verification in the project repo**

```bash
cd ~/dev/b12n-rljlt
joltc -M:check                          # expect the OK line
bb info | grep -E '^=== '               # expect games (10), 56 total
git status --short                      # expect clean
```

- [ ] **Step 2: Re-mirror the changed guide pages**

```bash
cp ~/dev/b12n-rljlt/docs/guide/example-catalog.md ~/dev/b12n-rljlt/docs/guide/index.md ~/dev/b12n-wikis/b12n-rljlt/
```

- [ ] **Step 3: Bump "50" → "56" in the wiki index files** — in `~/dev/b12n-wikis/README.md` (the b12n-rljlt Projects row), `~/dev/b12n-wikis/CLAUDE.md` (the b12n-rljlt quick-ref row), and `~/dev/b12n-wikis/b12n-rljlt/README.md` (two mentions), change "50 raylib examples" / "50 examples" → "56".

- [ ] **Step 4: Commit + push the wiki**

```bash
cd ~/dev/b12n-wikis
git pull --ff-only origin main
git add b12n-rljlt/ README.md CLAUDE.md
SHA=$(git -C ~/dev/b12n-rljlt rev-parse --short HEAD)
git commit -m "re-mirror b12n-rljlt @ ${SHA}: 6 classic games (50 → 56)"
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

- **Spec coverage:** toolkit (`mouse-pressed?`/`MOUSE-RIGHT`) → Task 1; the 6 games → Tasks 2–7 (snake, breakout, space-invaders, flappy-bird, 2048, minesweeper); doc-sync → Task 8; wiki re-mirror + arc-close → Task 9. All spec sections mapped.
- **Placeholder scan:** every code step has complete code; no TBD/TODO.
- **Type/name consistency:** `mouse-pressed?`/`MOUSE-RIGHT` defined in Task 1, consumed by Task 7. Every game consumes only `rl/*` symbols that exist in `raylib.clj` (verified this session). The 2048 digit-name trap is handled by using the `game-2048` handle everywhere (verified `bb 2048` is unreachable — babashka reads `2048` as a Long); "2048" lives in the title/description branding only.
- **jolt-construct check:** no `case` (uses `cond`), no `Math/*` needed; `for` uses a single `:when`; sets/`peek`/`pop`/`into`/`apply mapv vector`/`cond->` are core. Compile-check per task is the backstop.
- **Group-count arithmetic:** games 4→10 (+6) → 56 total; games 10 + core 9 + shapes 20 + text 5 + 3d 12 = 56. ✓
```
