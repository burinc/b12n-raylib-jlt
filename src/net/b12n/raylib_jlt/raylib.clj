(ns net.b12n.raylib-jlt.raylib
  "Shared jolt.ffi bindings for raylib — the surface used by the example programs
  in this project (net.b12n.raylib-jlt.core, net.b12n.raylib-jlt.input, net.b12n.raylib-jlt.bounce, net.b12n.raylib-jlt.colors, net.b12n.raylib-jlt.mouse,
  net.b12n.raylib-jlt.wheel, net.b12n.raylib-jlt.shapes, net.b12n.raylib-jlt.text, net.b12n.raylib-jlt.logo, net.b12n.raylib-jlt.gradient, net.b12n.raylib-jlt.stars, net.b12n.raylib-jlt.camera2d).

  raylib is the upstream C game library (raysan5/raylib); jolt calls it directly
  over its C ABI. The system libraylib is declared as a :jolt/native lib in
  deps.edn. Almost every call here uses raylib's scalar-argument variants, so the
  only by-value struct that crosses the FFI boundary is `Color` — a 4-byte
  {u8 r,g,b,a} packed into a :uint (see `rgba` and README.md). The one exception,
  Camera2D (24 bytes, passed by pointer), lives in net.b12n.raylib-jlt.camera2d, not here."
  (:require
   [jolt.ffi :as ffi]))

;; --- Color -------------------------------------------------------------------
;; Defined first: every drawing binding below takes a packed Color :uint, and
;; shade-color / cube! / sphere! reference `rgba` and the palette. Since jolt 0.4.0
;; ("unresolved symbols are compile errors") a symbol must be defined before its
;; first use in the file — in a fn body and in an :or destructuring default just as
;; much as at top level. Keep this section above its first use.
(defn rgba
  "Pack an RGBA color into the little-endian uint32 that raylib's `Color` struct
  is (r | g<<8 | b<<16 | a<<24), so it can cross the FFI boundary as a :uint."
  [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

;; raylib's named color palette (values from src/raylib.h).
(def LIGHTGRAY (rgba 200 200 200 255))   (def GRAY       (rgba 130 130 130 255))
(def DARKGRAY  (rgba 80 80 80 255))      (def YELLOW     (rgba 253 249 0 255))
(def GOLD      (rgba 255 203 0 255))     (def ORANGE     (rgba 255 161 0 255))
(def PINK      (rgba 255 109 194 255))   (def RED        (rgba 230 41 55 255))
(def MAROON    (rgba 190 33 55 255))     (def GREEN      (rgba 0 228 48 255))
(def LIME      (rgba 0 158 47 255))      (def DARKGREEN  (rgba 0 117 44 255))
(def SKYBLUE   (rgba 102 191 255 255))   (def BLUE       (rgba 0 121 241 255))
(def DARKBLUE  (rgba 0 82 172 255))      (def PURPLE     (rgba 200 122 255 255))
(def VIOLET    (rgba 135 60 190 255))    (def DARKPURPLE (rgba 112 31 126 255))
(def BEIGE     (rgba 211 176 131 255))   (def BROWN      (rgba 127 106 79 255))
(def DARKBROWN (rgba 76 63 47 255))      (def WHITE      (rgba 255 255 255 255))
(def BLACK     (rgba 0 0 0 255))         (def MAGENTA    (rgba 255 0 255 255))
(def RAYWHITE  (rgba 245 245 245 255))

;; --- window / lifecycle ------------------------------------------------------
(ffi/defcfn init-window    "InitWindow"   [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
(ffi/defcfn close-window   "CloseWindow"  [] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)

;; --- frame -------------------------------------------------------------------
(ffi/defcfn begin-drawing      "BeginDrawing"     [] :void)
(ffi/defcfn end-drawing        "EndDrawing"       [] :void)
(ffi/defcfn clear-background   "ClearBackground"  [:uint] :void)       ; Color
(ffi/defcfn get-frame-time     "GetFrameTime"     [] :float)           ; seconds since last frame
(ffi/defcfn begin-scissor-mode "BeginScissorMode" [:int :int :int :int] :void)
(ffi/defcfn end-scissor-mode   "EndScissorMode"   [] :void)

;; --- 2D shapes + text (scalar variants; Color is the only by-value struct) ---
(ffi/defcfn draw-text            "DrawText"            [:string :int :int :int :uint] :void)
(ffi/defcfn draw-fps             "DrawFPS"             [:int :int] :void)
(ffi/defcfn measure-text         "MeasureText"         [:string :int] :int)
(ffi/defcfn draw-pixel           "DrawPixel"           [:int :int :uint] :void)
(ffi/defcfn draw-line            "DrawLine"            [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle       "DrawRectangle"       [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle-lines "DrawRectangleLines"  [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle-grad-v "DrawRectangleGradientV" [:int :int :int :int :uint :uint] :void)
(ffi/defcfn draw-circle          "DrawCircle"          [:int :int :float :uint] :void)
(ffi/defcfn draw-circle-lines    "DrawCircleLines"     [:int :int :float :uint] :void)
(ffi/defcfn draw-ellipse         "DrawEllipse"         [:int :int :float :float :uint] :void)

;; --- rlgl immediate mode (all scalar) — for triangles / points ---------------
(ffi/defcfn rl-begin     "rlBegin"     [:int] :void)   ; RL-LINES / RL-TRIANGLES
(ffi/defcfn rl-end       "rlEnd"       [] :void)
(ffi/defcfn rl-vertex-2f "rlVertex2f"  [:float :float] :void)
(ffi/defcfn rl-color-4ub "rlColor4ub"  [:int :int :int :int] :void)  ; u8 args
(def ^:const RL-LINES 1)
(def ^:const RL-TRIANGLES 4)

(defn rl-color!
  "rlColor4ub from a packed rgba Color, so rlgl immediate mode can use the same
  Color values as the rest of the API."
  [color]
  (rl-color-4ub (bit-and color 0xff)
                (bit-and (bit-shift-right color 8) 0xff)
                (bit-and (bit-shift-right color 16) 0xff)
                (bit-and (bit-shift-right color 24) 0xff)))

;; --- Camera2D: a struct passed BY VALUE (the one non-Color by-value struct) ---
;; raylib's BeginMode2D(Camera2D) takes {Vector2 offset; Vector2 target; float
;; rotation; float zoom} — 24 bytes — by value. On the AArch64 (Apple) ABI a
;; composite larger than 16 bytes is passed INDIRECTLY: the caller allocates a
;; copy and passes a POINTER to it, so the binding is [:pointer] and we build the
;; struct (six little-endian floats) in native memory. NOTE: this is AArch64-
;; specific — on the x86-64 SysV ABI the 24 bytes are passed on the stack, which
;; a [:pointer] binding does NOT do (see README). For a portable alternative,
;; apply the same transform with the scalar rlgl matrix ops instead.
(ffi/defcfn ^:private begin-mode-2d-ptr "BeginMode2D" [:pointer] :void)
(ffi/defcfn end-mode-2d "EndMode2D" [] :void)

(defn with-camera-2d
  "Run (f) with a Camera2D active. Allocates the 24-byte struct, writes the six
  floats (offset.x, offset.y, target.x, target.y, rotation, zoom), passes a
  pointer to BeginMode2D, runs f, then EndMode2D and frees. See the ABI note above."
  [{:keys [offset-x offset-y target-x target-y rotation zoom]
    :or {offset-x 0
         offset-y 0
         target-x 0
         target-y 0
         rotation 0
         zoom 1.0}} f]
  (let [p (ffi/alloc 24)]
    (try
      (ffi/write p :float 0  (double offset-x))
      (ffi/write p :float 4  (double offset-y))
      (ffi/write p :float 8  (double target-x))
      (ffi/write p :float 12 (double target-y))
      (ffi/write p :float 16 (double rotation))
      (ffi/write p :float 20 (double zoom))
      (begin-mode-2d-ptr p)
      (f)
      (end-mode-2d)
      (finally (ffi/free p)))))

;; --- Camera3D + 3D geometry --------------------------------------------------
;; Camera3D is 44 bytes (three Vector3 + a float + an int), passed BY VALUE to
;; BeginMode3D — the same >16-byte-struct-by-pointer approach as Camera2D. 3D
;; shape helpers like DrawCube take a Vector3 BY VALUE (a 12-byte float struct
;; passed in FP registers, which the pointer trick does NOT cover), so draw 3D
;; geometry with rlgl immediate mode (rl-vertex-3f) instead. DrawGrid is scalar.
(ffi/defcfn draw-grid    "DrawGrid"    [:int :float] :void)
(ffi/defcfn rl-vertex-3f "rlVertex3f"  [:float :float :float] :void)
(ffi/defcfn ^:private begin-mode-3d-ptr "BeginMode3D" [:pointer] :void)
(ffi/defcfn end-mode-3d "EndMode3D" [] :void)

;; rlgl matrix stack — nested transforms for immediate-mode geometry. rlgl applies
;; the current transform to each rlVertex* at submit time, so push/rotate/translate
;; around a cube! call moves it (used by rlgl-solar-system).
(ffi/defcfn rl-push-matrix "rlPushMatrix" [] :void)
(ffi/defcfn rl-pop-matrix  "rlPopMatrix"  [] :void)
(ffi/defcfn rl-translatef  "rlTranslatef" [:float :float :float] :void)
(ffi/defcfn rl-rotatef     "rlRotatef"    [:float :float :float :float] :void)
(ffi/defcfn rl-scalef      "rlScalef"     [:float :float :float] :void)

(defn with-camera-3d
  "Run (f) with a Camera3D active (BeginMode3D → f → EndMode3D). Builds the
  44-byte struct in native memory (nine floats + fovy + projection int) and passes
  a pointer. Keys: :pos-x/y/z :target-x/y/z :up-x/y/z :fovy :projection (0 =
  perspective). See the ABI note above."
  [{:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
    :or {pos-x 0
         pos-y 0
         pos-z 0
         target-x 0
         target-y 0
         target-z 0
         up-x 0
         up-y 1
         up-z 0
         fovy 45
         projection 0}} f]
  (let [p (ffi/alloc 44)]
    (try
      (ffi/write p :float 0  (double pos-x))
      (ffi/write p :float 4  (double pos-y))
      (ffi/write p :float 8  (double pos-z))
      (ffi/write p :float 12 (double target-x))
      (ffi/write p :float 16 (double target-y))
      (ffi/write p :float 20 (double target-z))
      (ffi/write p :float 24 (double up-x))
      (ffi/write p :float 28 (double up-y))
      (ffi/write p :float 32 (double up-z))
      (ffi/write p :float 36 (double fovy))
      (ffi/write p :int   40 (int projection))
      (begin-mode-3d-ptr p)
      (f)
      (end-mode-3d)
      (finally (ffi/free p)))))

(defn- shade-color
  "Darken a packed Color by factor f (fakes lighting so cube faces read as 3D)."
  [color f]
  (rgba (int (* f (bit-and color 0xff)))
        (int (* f (bit-and (bit-shift-right color 8) 0xff)))
        (int (* f (bit-and (bit-shift-right color 16) 0xff)))
        255))

(defn- quad-3f
  "Two rlgl triangles for a quad, given a shaded color and a vector of its four
  [x y z] corners in a→b→c→d winding order."
  [color [a b c d]]
  (rl-color! color)
  (let [[ax ay az] a [bx by bz] b [cx cy cz] c [dx dy dz] d]
    (rl-vertex-3f ax ay az) (rl-vertex-3f bx by bz) (rl-vertex-3f cx cy cz)
    (rl-vertex-3f ax ay az) (rl-vertex-3f cx cy cz) (rl-vertex-3f dx dy dz)))

(defn cube!
  "Draw an axis-aligned box via rlgl immediate mode, its faces shaded from the
  packed `:color` for depth. Must be called inside a BeginMode3D block (see
  with-camera-3d). Keyword args:
    :pos   [x y z] centre           (default [0 0 0])
    :size  a number for a uniform cube, or [sx sy sz]  (default 1)
    :color a packed Color           (default BLACK)"
  [& {:keys [pos size color]
      :or {pos [0.0 0.0 0.0]
           size 1.0
           color BLACK}}]
  (let [[cx cy cz] pos
        [sx sy sz] (if (number? size) [size size size] size)
        hx (/ sx 2.0) hy (/ sy 2.0) hz (/ sz 2.0)
        x0 (- cx hx) x1 (+ cx hx) y0 (- cy hy) y1 (+ cy hy) z0 (- cz hz) z1 (+ cz hz)
        ;; the eight corners, named a<x><y><z> by which extreme each axis takes
        a000 [x0 y0 z0] a100 [x1 y0 z0] a010 [x0 y1 z0] a110 [x1 y1 z0]
        a001 [x0 y0 z1] a101 [x1 y0 z1] a011 [x0 y1 z1] a111 [x1 y1 z1]]
    (rl-begin RL-TRIANGLES)
    (quad-3f (shade-color color 1.0)  [a001 a101 a111 a011])   ; front  +z
    (quad-3f (shade-color color 0.5)  [a100 a000 a010 a110])   ; back   -z
    (quad-3f (shade-color color 0.7)  [a000 a001 a011 a010])   ; left   -x
    (quad-3f (shade-color color 0.85) [a101 a100 a110 a111])   ; right  +x
    (quad-3f (shade-color color 1.0)  [a011 a111 a110 a010])   ; top    +y
    (quad-3f (shade-color color 0.4)  [a000 a100 a101 a001])   ; bottom -y
    (rl-end)))

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
      :or {pos [0.0 0.0 0.0]
           radius 0.5
           rings 12
           slices 16
           color BLACK}}]
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

;; --- input -------------------------------------------------------------------
(ffi/defcfn ^:private key-down-raw     "IsKeyDown"          [:int] :int)
(ffi/defcfn ^:private key-pressed-raw  "IsKeyPressed"       [:int] :int)
(ffi/defcfn ^:private mouse-down-raw   "IsMouseButtonDown"  [:int] :int)
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
(ffi/defcfn get-mouse-x      "GetMouseX"         [] :int)
(ffi/defcfn get-mouse-y      "GetMouseY"         [] :int)
(ffi/defcfn get-mouse-wheel  "GetMouseWheelMove" [] :float)
(ffi/defcfn get-random-value "GetRandomValue"    [:int :int] :int)
(ffi/defcfn get-char-pressed "GetCharPressed"    [] :int)   ; unicode codepoint; 0 = queue empty
(ffi/defcfn get-key-pressed  "GetKeyPressed"     [] :int)   ; keycode; 0 = queue empty

;; --- libc time (the one NON-raylib FFI) --------------------------------------
;; time()/localtime() live in libc (always loaded); jolt.ffi resolves them exactly
;; like raylib's symbols. localtime returns a pointer to a struct tm whose first
;; three ints are tm_sec, tm_min, tm_hour (offsets 0/4/8 on Darwin and glibc). This
;; is the repo's only non-raylib FFI call — proof jolt binds any C ABI symbol.
(ffi/defcfn ^:private c-time      "time"      [:pointer] :long)
(ffi/defcfn ^:private c-localtime "localtime" [:pointer] :pointer)

(defn local-time
  "Current wall-clock local time as [hour minute second] via libc time()/localtime()."
  []
  (let [buf (ffi/alloc 8)]
    (try
      (c-time buf)
      (let [tm (c-localtime buf)]
        [(ffi/read tm :int 8) (ffi/read tm :int 4) (ffi/read tm :int 0)])
      (finally (ffi/free buf)))))

;; --- screenshot hook plumbing (headless smoke tests) -------------------------
(ffi/defcfn take-screenshot       "TakeScreenshot"          [:string] :void)
(ffi/defcfn ^:private flush-batch "rlDrawRenderBatchActive" [] :void)

;; C-bool returns arrive in the low byte; mask so only 0/1 counts.
(defn window-should-close?
  []
  (not (zero? (bit-and (should-close-raw) 0xff))))

(defn key-down?
  [k]
  (not (zero? (bit-and (key-down-raw k) 0xff))))

(defn key-pressed?
  [k]
  (not (zero? (bit-and (key-pressed-raw k) 0xff))))

(defn mouse-down?
  [b]
  (not (zero? (bit-and (mouse-down-raw b) 0xff))))

(defn mouse-pressed?
  [b]
  (not (zero? (bit-and (mouse-pressed-raw b) 0xff))))

;; --- constants (raylib KeyboardKey / MouseButton) ----------------------------
(def ^:const KEY-SPACE 32)  (def ^:const KEY-R     82)
(def ^:const KEY-W     87)  (def ^:const KEY-A     65)
(def ^:const KEY-S     83)  (def ^:const KEY-D     68)
(def ^:const KEY-RIGHT 262) (def ^:const KEY-LEFT  263)
(def ^:const KEY-DOWN  264) (def ^:const KEY-UP    265)
(def ^:const MOUSE-LEFT 0)
(def ^:const MOUSE-RIGHT 1)
(def ^:const KEY-BACKSPACE 259) (def ^:const KEY-ENTER 257)

;; --- ergonomic keyword-argument drawing API ----------------------------------
;; raylib's C functions are positional; these wrappers take keyword arguments so
;; example code reads self-descriptively — e.g. (rl/text! "hi" :x 10 :y 20
;; :color rl/RED) instead of (draw-text "hi" 10 20 20 rl/RED). The raw bindings
;; above remain the FFI boundary; these just name the arguments.

(defn window!
  "InitWindow with keyword args. :width :height :title."
  [& {:keys [width height title]
      :or {width 800
           height 450
           title "raylib"}}]
  (init-window width height title))

(defn text!
  "DrawText. :x :y :size :color."
  [s & {:keys [x y size color]
        :or {x 0
             y 0
             size 20
             color BLACK}}]
  (draw-text s x y size color))

(defn text-width
  "MeasureText. :size."
  [s & {:keys [size]
        :or {size 20}}]
  (measure-text s size))

(defn fps!
  "DrawFPS. :x :y."
  [& {:keys [x y]
      :or {x 10
           y 10}}]
  (draw-fps x y))

(defn rect!
  "DrawRectangle. :x :y :width :height :color."
  [& {:keys [x y width height color]
      :or {x 0
           y 0
           width 10
           height 10
           color BLACK}}]
  (draw-rectangle x y width height color))

(defn rect-lines!
  "DrawRectangleLines. :x :y :width :height :color."
  [& {:keys [x y width height color]
      :or {x 0
           y 0
           width 10
           height 10
           color BLACK}}]
  (draw-rectangle-lines x y width height color))

(defn rect-gradient!
  "DrawRectangleGradientV (top->bottom). :x :y :width :height :top :bottom."
  [& {:keys [x y width height top bottom]
      :or {x 0
           y 0
           width 10
           height 10
           top WHITE
           bottom BLACK}}]
  (draw-rectangle-grad-v x y width height top bottom))

(defn circle!
  "DrawCircle. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color BLACK}}]
  (draw-circle x y (double radius) color))

(defn circle-lines!
  "DrawCircleLines. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color BLACK}}]
  (draw-circle-lines x y (double radius) color))

(defn ellipse!
  "DrawEllipse. :x :y :rx :ry :color."
  [& {:keys [x y rx ry color]
      :or {x 0
           y 0
           rx 10
           ry 6
           color BLACK}}]
  (draw-ellipse x y (double rx) (double ry) color))

(defn line!
  "DrawLine. :x1 :y1 :x2 :y2 :color."
  [& {:keys [x1 y1 x2 y2 color]
      :or {x1 0
           y1 0
           x2 0
           y2 0
           color BLACK}}]
  (draw-line x1 y1 x2 y2 color))

(defn pixel!
  "DrawPixel. :x :y :color."
  [& {:keys [x y color]
      :or {x 0
           y 0
           color BLACK}}]
  (draw-pixel x y color))

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
      :or {cx 0
           cy 0
           radius 10
           start-deg 0
           end-deg 90
           segments 32
           color BLACK}}]
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

(defn ring!
  "A filled annulus (donut sector) as an rlgl quad strip between :inner and :outer
  radius over [start-deg, end-deg] — the immediate-mode stand-in for DrawRing (Vector2
  center by value). Same angle convention as sector! (0 deg up, clockwise, increasing).
  Each segment is two front-wound triangles.
    :cx :cy    center
    :inner :outer   radii
    :start-deg :end-deg   sweep in degrees (increasing)
    :segments  resolution (default 48)
    :color     packed Color"
  [& {:keys [cx cy inner outer start-deg end-deg segments color]
      :or {cx 0
           cy 0
           inner 20
           outer 40
           start-deg 0
           end-deg 360
           segments 48
           color BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        pt (fn [deg r]
             (let [t (* deg d->r)]
               [(+ cx (* r (Math/sin t))) (- cy (* r (Math/cos t)))]))]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (dotimes [k segments]
      (let [d0 (+ start-deg (* span (/ (double k) segments)))
            d1 (+ start-deg (* span (/ (double (inc k)) segments)))
            [ix0 iy0] (pt d0 inner) [ox0 oy0] (pt d0 outer)
            [ix1 iy1] (pt d1 inner) [ox1 oy1] (pt d1 outer)]
        (rl-vertex-2f (double ox0) (double oy0))
        (rl-vertex-2f (double ix0) (double iy0))
        (rl-vertex-2f (double ix1) (double iy1))
        (rl-vertex-2f (double ox0) (double oy0))
        (rl-vertex-2f (double ix1) (double iy1))
        (rl-vertex-2f (double ox1) (double oy1))))
    (rl-end)))

(defn line-ex!
  "A thick line (rlgl quad) from (x1,y1) to (x2,y2), :thick pixels wide — the
  immediate-mode stand-in for DrawLineEx (Vector2 endpoints by value). The quad is
  front-wound at every line direction (perpendicular = (dy,-dx)/len).
    :x1 :y1 :x2 :y2   endpoints
    :thick   width in px (default 2)
    :color   packed Color"
  [& {:keys [x1 y1 x2 y2 thick color]
      :or {x1 0
           y1 0
           x2 0
           y2 0
           thick 2
           color BLACK}}]
  (let [dx (- x2 x1) dy (- y2 y1)
        len (Math/sqrt (+ (* dx dx) (* dy dy)))
        len (if (zero? len) 1.0 len)
        h  (/ thick 2.0)
        px (* (/ dy len) h)        ; perpendicular (dy,-dx) * half-thick
        py (* (/ (- dx) len) h)
        ax (+ x1 px) ay (+ y1 py)
        bx (- x1 px) by (- y1 py)
        cx (- x2 px) cy (- y2 py)
        ex (+ x2 px) ey (+ y2 py)]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (rl-vertex-2f (double ax) (double ay))
    (rl-vertex-2f (double bx) (double by))
    (rl-vertex-2f (double cx) (double cy))
    (rl-vertex-2f (double ax) (double ay))
    (rl-vertex-2f (double cx) (double cy))
    (rl-vertex-2f (double ex) (double ey))
    (rl-end)))

;; --- smoke-test loop guards --------------------------------------------------
(defn auto-quit-deadline
  "RAYLIB_APP_AUTO_QUIT_MS=<n> ends the loop after n ms, so a window example is
  smoke-testable with no person at the keyboard. Returns an absolute ms deadline
  or nil."
  []
  (when-let [v (System/getenv "RAYLIB_APP_AUTO_QUIT_MS")]
    (try (let [ms (Integer/parseInt v)]
           (when (pos? ms) (+ (System/currentTimeMillis) ms)))
         (catch Exception _ nil))))

(defn keep-running?
  "True while the window is open and any RAYLIB_APP_AUTO_QUIT_MS deadline is unmet."
  [deadline]
  (and (not (window-should-close?))
       (or (nil? deadline) (< (System/currentTimeMillis) deadline))))

(def ^:private shot-path (System/getenv "RAYLIB_APP_SHOT"))

(defn maybe-screenshot!
  "RAYLIB_APP_SHOT=/path dumps one PNG on frame `at` — headless visual proof a
  frame rendered. Flushes raylib's batched geometry first (DrawText etc. is
  deferred until EndDrawing, so a mid-frame TakeScreenshot would miss it). raylib
  writes the file's basename into the current working directory."
  [frame at]
  (when (and shot-path (= frame at))
    (flush-batch)
    (take-screenshot shot-path)
    (binding [*out* *err*] (println "[net.b12n.raylib-jlt] SHOT" shot-path))))
