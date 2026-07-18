# Continuation prompt — build the `net.b12n.rljlt` raylib example suite

Paste this as the first message of a fresh Claude Code session opened in
`~/dev/b12n-rljlt`.

---

You're starting work in `~/dev/b12n-rljlt`, a **new standalone home** for a suite of
**raylib examples written in jolt** (native Clojure — no JVM), meant to be shared
with the community. The examples already exist and are battle-tested in another
repo; your job is to **replicate them here and rename the package** to
`net.b12n.rljlt`, then keep growing the suite.

## Source of truth

`~/dev/github--jolt-lang--examples/raylib-app/` — a working **42-example** raylib
suite. Contents:
- `deps.edn` — declares the system `libraylib` as a `:jolt/native` lib + one alias
  per example (short names: `:run`, `:asteroids`, …).
- `bb.edn` — a Babashka runner: `bb <name>`, `bb run <name>`, `bb run-all [secs]`,
  `bb examples`, `bb info` (grouped: games/core/shapes/text/3d), `bb check`.
- `README.md`, `.gitignore`
- `src/app/*.clj` — a shared `app.raylib` (ALL `jolt.ffi` bindings + a
  keyword-argument drawing API + color palette + loop guards), `app.check`
  (headless compile-check), and 42 example namespaces.

## Task 1 — replicate + rename `app.*` → `net.b12n.rljlt.*`

Move `src/app/` to `src/net/b12n/rljlt/` and rename the package everywhere. `app.`
appears ONLY as namespace references (verified), so replacing the token `app.` with
`net.b12n.rljlt.` in `.clj` files + `deps.edn` is safe. Turnkey recipe (macOS/BSD sed):

```bash
SRC=~/dev/github--jolt-lang--examples/raylib-app
DST=~/dev/b12n-rljlt
cp "$SRC"/deps.edn "$SRC"/bb.edn "$SRC"/README.md "$SRC"/.gitignore "$DST"/
mkdir -p "$DST"/src/net/b12n/rljlt
cp "$SRC"/src/app/*.clj "$DST"/src/net/b12n/rljlt/
cd "$DST"
find src -name '*.clj' -exec sed -i '' 's/app\./net.b12n.rljlt./g' {} +
sed -i '' 's/app\./net.b12n.rljlt./g' deps.edn
sed -i '' -e 's/app\./net.b12n.rljlt./g' -e 's#src/app/#src/net/b12n/rljlt/#g' README.md
```

Notes:
- Keep the short `deps.edn` alias names (`:run`, `:asteroids`, …) and the
  `:jolt/native` libraylib entry unchanged — only the `-m` targets change.
- `bb.edn` has no `app.` refs (it invokes `joltc` aliases) — it copies as-is.
- Filenames keep underscores (`basic_screen_manager.clj`); the ns uses hyphens
  (`net.b12n.rljlt.basic-screen-manager`) — that's Clojure's normal mapping.
- After copying, **reword `README.md`'s intro** so it reads as a standalone
  community project (not "one of the jolt examples"), and give it a title/desc for
  `net.b12n.rljlt`.

## Task 2 — verify

Requirements: `brew install raylib` (macOS), `joltc` on PATH.
- Headless compile-check MUST pass: `joltc -M:check` → "all example namespaces
  compiled OK". (This works with no display.)
- `bb tasks` parses; `bb info` lists all 42 examples grouped.
- Spot-check a few windowed — needs an ACTIVE display (raylib/GLFW crashes with
  "Failed to determine Monitor" → "invalid memory reference" if the Mac display has
  slept): `RAYLIB_APP_AUTO_QUIT_MS=2000 RAYLIB_APP_SHOT=shot.png joltc -M:<alias>`,
  then view the PNG (raylib writes the basename into CWD).

## Task 3 — repo + commit

`git init` if needed. **Ask me for the git remote before pushing** (this is going to
be community-shared, so the remote/owner is my call). Then commit the suite.

## Key context (architecture, FFI, conventions)

- **Architecture:** one shared `net.b12n.rljlt.raylib` ns = ALL `jolt.ffi` bindings
  + a keyword-argument drawing API (`text!`/`rect!`/`circle!`/`cube!`/…) + color
  palette + loop guards (`auto-quit-deadline`, `keep-running?`, `maybe-screenshot!`).
  One ns per example. Adding an example = new `src/net/b12n/rljlt/<name>.clj` + a
  `deps.edn` alias + a `net.b12n.rljlt.check` require + a `bb.edn` registry row & task.
- **FFI facts proven:** Color-by-value as a packed `:uint`; two by-value Colors in
  one call; float returns (`GetMouseWheelMove`/`GetFrameTime`); `ffi/write :int`;
  rlgl immediate mode 2D **and** 3D (`rl-vertex-2f`/`rl-vertex-3f`, RL_TRIANGLES/
  RL_LINES); the rlgl matrix stack (`rlPushMatrix`/`rlRotatef`/`rlTranslatef`/
  `rlScalef`, applied per-vertex at submit time); **struct >16 B by value via a
  `[:pointer]` binding on AArch64** (Camera2D 24 B, Camera3D 44 B). `Vector2`/
  `Vector3`-by-value functions (`DrawCube`/`DrawSphere`, ≤16 B float HFAs) can't use
  the pointer trick — use rlgl immediate mode instead. `with-camera-3d` + `rl/cube!`
  are the 3D building blocks.
- **Style convention:** functions with **more than 3 arguments use keyword args**
  (or group scalars into vectors to reach ≤3). Raw `ffi/defcfn` bindings stay
  positional (they mirror C); recursive math kernels may stay positional with
  grouped-vector args.
- **What's built:** games (asteroids, tetris, pong, vampire-survivors); 3d
  (camera-3d, waving-cubes, camera-3d-first-person, rlgl-solar-system,
  box-collisions, tesseract-view, wireframe-shapes); the full 2D core/shapes/text
  set (input, timing, shapes, text, trig, fractals, …).
- **Next ideas** (all within the current toolkit): more 3D —
  `orthographic-projection` (toggle `:projection 1`), `point-cloud`,
  `bouncing-spheres`, `spinning-cubes`, `rotating-cube`; more 2D — `ball-physics`,
  `lines-bezier`, `input-box`. **Big frontier:** textures / shaders / audio — large
  by-value struct returns (`Texture2D`/`Model`/`Sound`); where native struct support
  in the jolt fork (`~/dev/github--jolt-lang--jolt`, my private fork) would unlock a
  whole category. Idea source: `~/dev/raylib-clojure-playground-mvp` (an earlier
  Clojure raylib port with pong/asteroids/tetris/vampire-survivors + many 3D demos).

**Start with Task 1 (replicate + rename), then Task 2 (verify). Ask me for the git
remote before pushing.**
