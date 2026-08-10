#!/usr/bin/env bb
;; record_all.clj — batch-record a GIF for every example.
;;
;;   bb record                              ; record everything not up to date
;;   bb record --only bounce,starfield      ; subset (comma-separated ids)
;;   bb record --force                      ; ignore the ledger
;;   bb record --dry-run                    ; show the plan
;;
;; bb record (in bb.edn) wraps: bb scripts/record_all.clj

(ns record-all
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [examples-registry :as reg]
            [record-gif :as rec])
  (:import [java.security MessageDigest]
           [java.util.concurrent Executors TimeUnit]))

(def spec
  {:manifest {:coerce :string
              :default "scripts/demo_manifest.edn"}
   :out-dir  {:coerce :string
              :default "doc/demos"}
   :ledger   {:coerce :string
              :default "doc/demos/ledger.edn"}
   :only     {:coerce :string
              :default nil
              :desc "Comma-separated example ids"}
   :force    {:coerce :boolean
              :default false}
   :dry-run  {:coerce :boolean
              :default false}
   :pool     {:coerce :long
              :default 2
              :desc "Concurrent encoder threads"}
   :readme   {:coerce :string
              :default "doc/demos/README.md"}})

;; ---------------------------------------------------------------- helpers

(defn sha256 [path]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (fs/read-all-bytes path))
         (map #(format "%02x" %))
         (apply str))))

(defn have? [bin]
  (zero? (:exit (p/shell {:out nil
                          :err nil
                          :continue true} "which" bin))))

(def mac? (str/starts-with? (System/getProperty "os.name") "Mac"))

;; ---------------------------------------------------------------- window

(defn focus!
  "Bring the process's window frontmost so the capture isn't of an occluded window."
  [pid]
  (cond
    mac? (p/shell {:continue true
                   :out nil
                   :err nil}
                  "osascript" "-e"
                  (format "tell application \"System Events\" to set frontmost of (first process whose unix id is %d) to true" pid))
    (have? "xdotool") (p/shell {:continue true
                                :out nil
                                :err nil}
                               "xdotool" "search" "--pid" (str pid) "windowactivate")
    :else nil))

(defn wait-for-window
  "Poll the capture command until it produces a non-trivial PNG. Beats a fixed
   sleep — Chez startup varies and an early frame is a blank or missing window."
  [{:keys [capture pid timeout-ms]}]
  (let [probe (str (fs/create-temp-file {:suffix ".png"}))
        end   (+ (System/currentTimeMillis) timeout-ms)]
    (try
      (loop []
        (let [{:keys [exit]} (p/shell {:continue true
                                       :out nil
                                       :err nil}
                                      (rec/render capture {:pid pid
                                                           :file probe}))]
          (cond
            (and (zero? exit) (fs/exists? probe) (> (fs/size probe) 4096)) true
            (> (System/currentTimeMillis) end) false
            :else (do (Thread/sleep 150) (recur)))))
      (finally (fs/delete-if-exists probe)))))

;; ---------------------------------------------------------------- input

(defn synth-key!
  "Post a key chord to `pid` via cgevent. Valid chord tokens are documented
   in b12n-cgevent's keycodes.clj (arrows, space, enter, letters, etc.)."
  [pid k]
  (p/shell {:continue true
            :out nil
            :err nil} "cgevent" "key" "--pid" (str pid) k))

(defn synth-click!
  "Click at (x, y) relative to `pid`'s frontmost window (--window), matching
   the manifest's window-relative coordinates rather than global screen
   coordinates — the target window is not guaranteed to sit at (0,0)."
  [pid x y]
  (p/shell {:continue true
            :out nil
            :err nil} "cgevent" "click" "--pid" (str pid) "--window" (str x) (str y)))

(defn play-input!
  "Fire a timeline of [at-seconds :key \"left\"] / [at-seconds :click x y]
   events against `pid` on a background thread."
  [pid timeline]
  (future
    (let [t0 (System/currentTimeMillis)]
      (doseq [[at action & args] (sort-by first timeline)]
        (let [wait (- (+ t0 (long (* 1000 at))) (System/currentTimeMillis))]
          (when (pos? wait) (Thread/sleep wait))
          (case action
            :key   (synth-key! pid (first args))
            :click (let [[x y] args] (synth-click! pid x y))
            nil))))))

;; ---------------------------------------------------------------- one example

(defn record-one!
  [{:keys [id src run capture duration fps width warmup input manual out-dir]
    :as ex}]
  (let [gif    (str (fs/file out-dir (str id ".gif")))
        frames (str (fs/create-temp-dir {:prefix (str "frames-" id "-")}))
        cmd    (rec/render run {:src src
                                :id id})]
    (fs/create-dirs out-dir)
    (println (format "\n▶ %s  (%s)" id src))
    (let [proc (p/process {:out :string
                           :err :string
                           :shutdown p/destroy-tree} cmd)
          pid  (.pid (:proc proc))]
      (try
        (if-not (wait-for-window {:capture capture
                                  :pid pid
                                  :timeout-ms (long (* 1000 (or warmup 10)))})
          (do (println "  ✗ window never appeared")
              {:id id
               :status :no-window
               :stderr (some-> @(:err proc) (str/join) (subs 0 (min 400 400)))})
          (do
            (focus! pid)
            (Thread/sleep 250)
            (when (seq input) (play-input! pid input))
            (when manual
              (println (format "  ⌨  INTERACT NOW — recording %.0fs" (double duration))))
            (let [captured (rec/capture-frames {:pid pid
                                                :capture capture
                                                :duration duration
                                                :fps fps
                                                :frames-dir frames})]
              (if (< (count captured) 2)
                {:id id
                 :status :too-few-frames}
                {:id id
                 :status :captured
                 :gif gif
                 :frames frames
                 :captured captured
                 :fps fps
                 :width width
                 :duration duration
                 :sha (sha256 src)}))))
        (finally
          (p/destroy-tree proc)
          (deref proc 3000 nil))))))

(defn encode! [{:keys [gif frames captured fps width]
                :as r}]
  (let [ctx {:frames captured
             :frames-dir frames
             :out gif
             :fps fps
             :width width}]
    (try
      (if (have? "gifski") (rec/encode-gifski ctx) (rec/encode-ffmpeg ctx))
      (rec/optimize! gif)
      (fs/delete-tree frames)
      (println (format "  ✓ %s (%.1f MB)" gif (/ (fs/size gif) 1048576.0)))
      (assoc r :status :done :bytes (fs/size gif))
      (catch Exception e
        (println "  ✗ encode failed:" (ex-message e))
        (assoc r :status :encode-failed)))))

;; ---------------------------------------------------------------- manifest

(defn load-manifest
  "Build the full per-example spec list from examples-registry/examples (the
   single source of truth also used by bb.edn) merged with demo_manifest.edn's
   :defaults and any per-id :overrides."
  [manifest-path]
  (let [{:keys [defaults overrides]} (edn/read-string (slurp manifest-path))]
    (for [[id alias group desc] reg/examples]
      (merge defaults
             {:id id
              :group group
              :desc desc
              :src (reg/src-path alias)
              :run (str "joltc -M:" alias)}
             (get overrides id)))))

(defn up-to-date? [ledger {:keys [id src out-dir duration fps width]}]
  (let [prev (get ledger id)
        gif  (fs/file out-dir (str id ".gif"))]
    (and prev (fs/exists? gif)
         (= (:sha prev) (sha256 src))
         (= (:settings prev) [duration fps width]))))

(defn write-readme!
  "Grouped catalog (games/core/shapes/text/3d/generative, mirroring `bb
   info`'s ordering), one heading + GIF per successfully recorded example."
  [path results out-dir]
  (fs/create-dirs (fs/parent path))
  (let [done     (filter #(= :done (:status %)) results)
        group-of (fn [id] (nth (get reg/by-name id) 2))]
    (spit path
          (str "# Examples\n\n"
               (str/join "\n"
                         (for [group ["games" "core" "shapes" "text" "3d" "generative"]
                               :let [rows (filter #(= group (group-of (:id %))) done)]
                               :when (seq rows)]
                           (str (format "## %s\n\n" group)
                                (str/join "\n"
                                          (for [{:keys [id]} rows]
                                            (format "### %s\n\n![%s](%s)\n" id id
                                                    (str (fs/file-name (fs/file out-dir (str id ".gif"))))))))))
               "\n")))
  (println "\nWrote" path))

;; ---------------------------------------------------------------- main

(defn -main [& args]
  (let [{:keys [manifest out-dir ledger only force dry-run pool readme]}
        (cli/parse-opts args {:spec spec})
        ledger-data (if (fs/exists? ledger) (edn/read-string (slurp ledger)) {})
        wanted      (when only (set (str/split only #",")))
        examples    (cond->> (map #(assoc % :out-dir out-dir) (load-manifest manifest))
                      wanted (filter (comp wanted :id)))
        todo        (if force examples (remove #(up-to-date? ledger-data %) examples))]
    (println (format "%d examples, %d to record, %d up to date."
                     (count examples) (count todo) (- (count examples) (count todo))))
    (when dry-run
      (doseq [e todo] (println "  -" (:id e) (if (:manual e) "[manual]" "[scripted]")))
      (System/exit 0))
    (let [ex-pool (Executors/newFixedThreadPool pool)
          pending (atom [])]
      ;; Capture is strictly serial — only one window can be frontmost, and
      ;; encoding in the background keeps the capture loop from stalling.
      (doseq [ex todo]
        (let [r (record-one! ex)]
          (if (= :captured (:status r))
            (swap! pending conj (.submit ex-pool (reify java.util.concurrent.Callable (call [_] (encode! r)))))
            (swap! pending conj (reify java.util.concurrent.Future
                                  (get [_] r) (isDone [_] true)
                                  (cancel [_ _] false) (isCancelled [_] false))))))
      (.shutdown ex-pool)
      (.awaitTermination ex-pool 30 TimeUnit/MINUTES)
      (let [results (mapv #(.get %) @pending)
            updated (reduce (fn [m {:keys [id status sha duration fps width]
                                    :as r}]
                              (if (= :done status)
                                (assoc m id {:sha sha
                                             :settings [(:duration r) fps width]
                                             :bytes (:bytes r)
                                             :at (str (java.time.Instant/now))})
                                m))
                            ledger-data results)]
        (fs/create-dirs (fs/parent ledger))
        (spit ledger (pr-str updated))
        (write-readme! readme results out-dir)
        (let [failed (remove #(= :done (:status %)) results)]
          (when (seq failed)
            (println "\nFailed:")
            (doseq [f failed] (println "  " (:id f) (:status f)))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
