#!/usr/bin/env bb
;; record_gif.clj — capture a fixed-fps screenshot sequence for one running
;; example (via cgevent) and encode it to a GIF (gifski, falling back to
;; ffmpeg's two-pass palette recipe). Required by scripts/record_all.clj.

(ns record-gif
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(defn render
  "Substitute {{key}} tokens in `tmpl` from `m`, e.g.
   (render \"cgevent screenshot {{file}} --pid {{pid}}\" {:file \"a.png\" :pid 123})
   => \"cgevent screenshot a.png --pid 123\"."
  [tmpl m]
  (reduce-kv (fn [s k v] (str/replace s (str "{{" (name k) "}}") (str v))) tmpl m))

(defn capture-frames
  "Sample `capture` (a render template needing {{pid}}/{{file}}) at `fps` for
   `duration` seconds, writing zero-padded sequential PNGs into `frames-dir`.
   Returns the vector of frame file paths that actually captured — a failed
   or slow capture is skipped, not retried, so the count can be less than
   fps*duration."
  [{:keys [pid duration fps frames-dir capture]}]
  (let [interval-ms (long (/ 1000 fps))
        n-frames    (long (* duration fps))]
    (loop [i 0 captured []]
      (if (>= i n-frames)
        captured
        (let [t0   (System/currentTimeMillis)
              file (str (fs/file frames-dir (format "%04d.png" i)))
              cmd  (render capture {:pid pid
                                    :file file})
              {:keys [exit]} (p/shell {:continue true
                                       :out nil
                                       :err nil} cmd)
              ok?  (and (zero? exit) (fs/exists? file) (pos? (fs/size file)))
              wait (- (+ t0 interval-ms) (System/currentTimeMillis))]
          (when (pos? wait) (Thread/sleep wait))
          (recur (inc i) (if ok? (conj captured file) captured)))))))

(defn encode-gifski
  "gifski takes the frame files directly as positional args, already in
   capture order (sequential zero-padded names sort correctly)."
  [{:keys [frames out fps width]}]
  (apply p/shell "gifski" "-o" out "-r" (str fps)
         (concat (when width ["-W" (str width)]) frames)))

(defn encode-ffmpeg
  "Two-pass palette encode (palettegen + paletteuse) — a naive single-pass
   ffmpeg gif bands badly on gradients/anti-aliased raylib output."
  [{:keys [frames-dir out fps width]}]
  (let [pattern (str frames-dir "/%04d.png")
        scale   (when width (str ",scale=" width ":-1:flags=lanczos"))
        vf      (str "fps=" fps scale)
        palette (str (fs/create-temp-file {:suffix ".png"}))]
    (try
      (p/shell "ffmpeg" "-y" "-framerate" (str fps) "-i" pattern
               "-vf" (str vf ",palettegen") palette)
      (p/shell "ffmpeg" "-y" "-framerate" (str fps) "-i" pattern "-i" palette
               "-lavfi" (str vf "[x];[x][1:v]paletteuse") out)
      (finally (fs/delete-if-exists palette)))))

(defn optimize!
  "Shrink the encoded GIF further with gifsicle if it's installed; a no-op
   otherwise (gifski's own output is already reasonable without it)."
  [gif]
  (when (fs/which "gifsicle")
    (p/shell {:continue true} "gifsicle" "-O3" "--batch" gif)))
