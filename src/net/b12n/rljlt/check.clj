(ns net.b12n.rljlt.check
  "Headless compile-check (`joltc -M:check`).

  Requires every example namespace, which compiles each one (macro-expansion,
  var resolution, arity checks) WITHOUT opening a window — so the whole suite can
  be verified with no display attached. It does not exercise rendering; that needs
  a real window (each example's own `RAYLIB_APP_AUTO_QUIT_MS` + `RAYLIB_APP_SHOT`
  smoke does that)."
  (:require net.b12n.rljlt.raylib
            net.b12n.rljlt.core
            net.b12n.rljlt.input
            net.b12n.rljlt.bounce
            net.b12n.rljlt.colors
            net.b12n.rljlt.mouse
            net.b12n.rljlt.wheel
            net.b12n.rljlt.shapes
            net.b12n.rljlt.gradient
            net.b12n.rljlt.text
            net.b12n.rljlt.logo
            net.b12n.rljlt.stars
            net.b12n.rljlt.eyes
            net.b12n.rljlt.camera2d
            net.b12n.rljlt.mouse-trail
            net.b12n.rljlt.recursive-tree
            net.b12n.rljlt.math-sine-cosine
            net.b12n.rljlt.bullet-hell
            net.b12n.rljlt.triangle-strip
            net.b12n.rljlt.delta-time
            net.b12n.rljlt.scissor-test
            net.b12n.rljlt.writing-anim
            net.b12n.rljlt.format-text
            net.b12n.rljlt.collision-area
            net.b12n.rljlt.dashed-line
            net.b12n.rljlt.double-pendulum
            net.b12n.rljlt.kaleidoscope
            net.b12n.rljlt.hilbert-curve
            net.b12n.rljlt.math-angle-rotation
            net.b12n.rljlt.basic-screen-manager
            net.b12n.rljlt.random-values
            net.b12n.rljlt.words-alignment
            net.b12n.rljlt.camera-3d
            net.b12n.rljlt.waving-cubes
            net.b12n.rljlt.camera-3d-first-person
            net.b12n.rljlt.rlgl-solar-system
            net.b12n.rljlt.box-collisions
            net.b12n.rljlt.asteroids
            net.b12n.rljlt.tetris
            net.b12n.rljlt.pong
            net.b12n.rljlt.tesseract-view
            net.b12n.rljlt.vampire-survivors
            net.b12n.rljlt.wireframe-shapes
            net.b12n.rljlt.rotating-cube
            net.b12n.rljlt.spinning-cubes
            net.b12n.rljlt.orthographic-projection
            net.b12n.rljlt.point-cloud
            net.b12n.rljlt.bouncing-spheres
            net.b12n.rljlt.ball-physics
            net.b12n.rljlt.lines-bezier
            net.b12n.rljlt.input-box
            net.b12n.rljlt.snake
            net.b12n.rljlt.breakout
            net.b12n.rljlt.space-invaders
            net.b12n.rljlt.flappy-bird
            net.b12n.rljlt.game-2048
            net.b12n.rljlt.minesweeper
            net.b12n.rljlt.game-of-life
            net.b12n.rljlt.boids
            net.b12n.rljlt.fireworks
            net.b12n.rljlt.fourier-epicycles
            net.b12n.rljlt.spirograph
            net.b12n.rljlt.l-system
            net.b12n.rljlt.flow-field
            net.b12n.rljlt.color-wheel
            net.b12n.rljlt.pie-chart
            net.b12n.rljlt.splines))

(defn -main [& _]
  (println "net.b12n.rljlt: all example namespaces compiled OK"))
