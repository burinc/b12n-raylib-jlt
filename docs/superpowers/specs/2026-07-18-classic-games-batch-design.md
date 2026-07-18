# Design — classic games batch: `net.b12n.rljlt` 50 → 56

**Date:** 2026-07-18
**Status:** approved
**Repo:** `burinc/b12n-rljlt` (personal; in-project superpowers docs)

## Goal

Add six classic games to the suite (`games` group grows 4 → 10), plus one small
toolkit addition they collectively need. Each game is one namespace on top of the
shared `net.b12n.rljlt.raylib` layer, wired through the four touchpoints (source ns +
`deps.edn` alias + `check.clj` require + `bb.edn` registry row/task).

## Non-goals

- No textures / audio / high-score persistence (the by-value-struct-return frontier
  stays out of scope).
- **Gameplay logic is not headless-testable.** Verification is `joltc -M:check`
  (compile) + a `RAYLIB_APP_SHOT` screenshot (renders + runs clean). Interactive
  correctness (win/lose, merges, flood-fill) is validated by construction + manual
  `bb <game>` play, not automated.
- No AI opponents beyond simple rules (pong already covers a tracking CPU).

## Toolkit addition — `src/net/b12n/rljlt/raylib.clj`

The suite has `key-pressed?` (edge-triggered) but no mouse equivalent, and only
`MOUSE-LEFT`. Minesweeper needs both. Add:

```clojure
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
;; … in the predicates block, next to mouse-down?:
(defn mouse-pressed? [b] (not (zero? (bit-and (mouse-pressed-raw b) 0xff))))
;; … in the constants block, next to MOUSE-LEFT:
(def ^:const MOUSE-RIGHT 1)
```

`IsMouseButtonPressed` returns a C bool in the low byte (verified in `raylib.h`);
mask with `0xff` exactly like `key-pressed?` / `mouse-down?`. `MOUSE_BUTTON_RIGHT = 1`.

## The six games (group `games`)

Each `-main` runs the canonical loop (`window!` → `set-target-fps 60` →
`auto-quit-deadline` → `keep-running?` loop → `maybe-screenshot! frame 10` →
`close-window`). All game state is threaded through the `loop`/`recur` (immutable
value updated each frame); all logic is pure Clojure; rendering uses the drawing API.
Filenames use underscores, namespaces hyphens.

1. **`snake`** (arrows) — a grid (e.g. 20×15 cells of 24 px). The snake is a vector
   of `[col row]` cells; a direction `[dc dr]`; food at a random empty cell. Advance
   one cell every ~8 frames (frame-tick, not per-frame). Eating food grows the tail
   and respawns food; hitting a wall or self ends the game (show "GAME OVER, SPACE to
   restart"). Arrow keys set direction (no 180° reversal). Draw cells with `rect!`,
   score via `text!`.

2. **`breakout`** (mouse-x paddle) — a brick grid (rows × cols of colored `rect!`),
   a paddle following `get-mouse-x` (clamped), a ball with velocity. Ball reflects off
   walls, paddle (with x-offset english), and bricks (AABB, computed in Clojure —
   remove the hit brick, flip vy). Lose life if the ball passes the paddle; clear all
   bricks to win. No new bind (mouse position is already available).

3. **`space-invaders`** (←/→ + SPACE) — a player ship (`rect!`) at the bottom, a grid
   of aliens marching side-to-side and stepping down at edges, player bullets (SPACE,
   rate-limited by a frame cooldown). Bullet-alien AABB removes the alien; aliens
   reaching the bottom (or touching the ship) ends the game. Score per alien.

4. **`flappy-bird`** (SPACE) — a bird (`circle!`) with gravity; SPACE sets upward
   velocity. Pipes (`rect!` pairs with a gap) scroll left and recycle on the right at
   a random gap-y. Passing a pipe scores; touching a pipe or the ground/ceiling ends
   the game (SPACE restarts). Frame-based scroll.

5. **`2048`** (arrows) — a 4×4 board (vector of 16 ints, 0 = empty). An arrow slides +
   merges each row/column (classic 2048 rules: compress, merge equal adjacent once,
   compress again); if the board changed, spawn a `2` (or `4`) at a random empty cell.
   Tiles drawn as `rect!` colored by value (a value→color map) with the number via
   `text!`; score accumulates merges. "You win" at 2048; "game over" when no move
   changes the board. The slide/merge is the core pure function; implement + reuse it
   for all four directions via row extraction/rotation.

6. **`minesweeper`** (mouse L reveal / R flag) — a grid (e.g. 12×12) of cells with N
   mines placed at start (seeded via `get-random-value`, avoiding the first area).
   Each cell knows: mine?, revealed?, flagged?, adjacent-count. **Left-click**
   (`mouse-pressed? MOUSE-LEFT`) reveals; revealing a 0-cell flood-fills neighbors;
   revealing a mine loses. **Right-click** (`mouse-pressed? MOUSE-RIGHT`) toggles a
   flag. Win when all non-mine cells are revealed. Numbers via `text!`, cells via
   `rect!`/`rect-lines!`. **This is the example that exercises the new toolkit bind.**

## Mechanics per game (the four touchpoints)

1. `src/net/b12n/rljlt/<name>.clj`.
2. `deps.edn` alias `:<name> {:main-opts ["-m" "net.b12n.rljlt.<name>"]}`.
3. `check.clj` — add `net.b12n.rljlt.<name>` to the `:require` block.
4. `bb.edn` — add `["<name>" "<name>" "games" "<desc>"]` to `examples` + a `bb <name>`
   task.

## Documentation + wiki (doc-sync, after code lands)

- `README.md` — add 6 rows to the examples table.
- `docs/guide/example-catalog.md` — add 6 rows under `## games`, change its heading
  `## games (4)` → `## games (10)`, update the title/intro "50" → "56".
- `docs/guide/index.md` — "50 raylib examples" / "tour of all 50" → "56".
- Mention `mouse-pressed?` where input binds are noted (headless-smoke-testing.md's
  C-truthiness footnote is the natural spot, or leave if it doesn't fit cleanly).
- Re-mirror the changed `docs/guide/*.md` → `~/dev/b12n-wikis/b12n-rljlt/`; bump the
  "50" mentions to "56" in the wiki `README.md` row, `CLAUDE.md` quick-ref row, and
  `b12n-rljlt/README.md`. Commit `re-mirror b12n-rljlt @ <sha>`.

## Verification

- Per game: `joltc -M:check` green; screenshot smoke
  `RAYLIB_APP_AUTO_QUIT_MS=1500 RAYLIB_APP_SHOT=shot-<name>.png joltc -M:<name>` — the
  initial/animating frame must render (self-moving games show motion; grid games show
  the starting board). Games needing input to progress (2048, minesweeper) are smoked
  at their initial state.
- `bb info` shows `games (10)` and 56 total; `bb tasks` parses.
- Clean up `shot-*.png` (gitignored) before committing.

## Sequencing

1. **Toolkit**: `mouse-pressed?` + `MOUSE-RIGHT` → `-M:check`.
2. **Keyboard/mouse-position games** (no new-bind dependency): snake, breakout,
   space-invaders, flappy-bird, 2048.
3. **Mouse-click game**: minesweeper (depends on the toolkit bind).
4. **Doc-sync + wiki re-mirror.**
5. **Arc-close**: `bb info` = 56, screenshot sweep, push both repos.
