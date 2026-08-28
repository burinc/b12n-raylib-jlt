(ns registration
  "The offline half of the four-touchpoint check.

  CONTRIBUTING.md and the example catalog list four places one example
  touches: the source namespace, a deps.edn alias, a check.clj require, and a
  registry row that bb.edn turns into a task.

  The check.clj require is the one that needs a gate. `bb check` compiles
  what check.clj requires, so an example left out of that list is never
  compiled and the run still prints \"all example namespaces compiled OK\"
  and exits 0. Measured 2026-08-29 on a scratch copy: a namespace carrying a
  deliberate unresolved symbol fails `bb check` with its require present and
  passes with the require removed. The compile gate cannot guard its own
  registration, because what it would have to notice is the thing it never
  loaded.

  The registry's alias is NOT the display name and the namespace is not
  derivable from either. `[\"basic-window\" \"run\" ...]` reaches
  net.b12n.raylib-jlt.core through deps.edn's :run alias, and 12 of the rows
  differ this way. So deps.edn is the source of truth for which namespace a
  row means, and everything else is checked against that rather than against
  a name built by string surgery."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private ns-prefix "net.b12n.raylib-jlt.")

(defn ns->source
  "The file a namespace must live in. Clojure's underscore rule applies."
  [root ns-sym]
  (let [tail (subs (str ns-sym) (count ns-prefix))]
    (str (fs/path root "src" "net" "b12n" "raylib_jlt"
                  (str (str/replace tail "-" "_") ".clj")))))

(defn alias-targets
  "deps.edn's :aliases, as {alias-name main-namespace-string}."
  [root]
  (->> (:aliases (edn/read-string (slurp (str (fs/path root "deps.edn")))))
       (map (fn [[k v]] [(name k) (second (:main-opts v))]))
       (into {})))

(defn check-requires
  "The namespaces check.clj requires, as a set of strings.

  Read to EOF rather than with `read-string`. The ns form happens to come
  first today, so a single read would work and would quietly stop working
  the moment check.clj grew a second form worth reading."
  [root]
  (let [path (str (fs/path root "src" "net" "b12n" "raylib_jlt" "check.clj"))
        r (java.io.PushbackReader. (java.io.StringReader. (slurp path)))
        forms (loop [acc []]
                (let [f (read {:read-cond :allow
                               :eof ::eof} r)]
                  (if (= f ::eof) acc (recur (conj acc f)))))
        ns-form (first (filter (fn [f] (and (seq? f) (= 'ns (first f)))) forms))]
    (->> ns-form
         (filter (fn [x] (and (seq? x) (= :require (first x)))))
         first rest
         (map (fn [x] (str (first x))))
         set)))

(defn bb-task-names
  "The task keys bb.edn declares. bb.edn is EDN, so it reads back as data."
  [root]
  (set (map name (keys (:tasks (edn/read-string (slurp (str (fs/path root "bb.edn")))))))))

(defn problems
  "Every registration gap, as [name explanation] pairs. Empty means clean."
  [root examples]
  (let [aliases  (alias-targets root)
        requires (check-requires root)
        tasks    (bb-task-names root)
        gaps
        (mapcat
         (fn [row]
           (let [nm (nth row 0)
                 al (nth row 1)
                 target (get aliases al)]
             (cond-> []
               (nil? target)
               (conj [nm (str "no :" al " alias in deps.edn, so jolt -M:" al
                              " cannot run it")])

               (and target (not (fs/exists? (ns->source root target))))
               (conj [nm (str "deps.edn alias :" al " names " target
                              ", which has no source file")])

               (and target (not (contains? requires target)))
               (conj [nm (str target " missing from check.clj :require, so "
                              "bb check never compiles it and still reports success")])

               (not (contains? tasks nm))
               (conj [nm (str "no `bb " nm "` task in bb.edn")]))))
         examples)
        ;; A runnable source with no registry row is invisible: no task, and
        ;; nothing lists it. `-main` separates examples from the shared
        ;; binding layer exactly, an idea borrowed from the sibling
        ;; b12n-raylib-jnk's bb/check.clj, which is better than maintaining a
        ;; list of names to skip.
        reachable (set (keep (fn [row] (get aliases (nth row 1))) examples))
        orphans
        (->> (fs/glob (fs/path root "src") "**/*.clj")
             (map str)
             (remove (fn [f] (contains? reachable
                                        (str ns-prefix
                                             (str/replace
                                              (str/replace (str (fs/file-name f)) ".clj" "")
                                              "_" "-")))))
             (filter (fn [f] (re-find #"\(defn -main" (slurp f))))
             (remove (fn [f] (= "check.clj" (str (fs/file-name f)))))
             (mapv (fn [f] [(str (fs/file-name f))
                            "defines -main but no registry row reaches it"])))]
    (concat gaps orphans)))
