(ns hooks.jolt-ffi
  "clj-kondo hook for jolt.ffi/defcfn.

  `defcfn` binds a C symbol to a Clojure var:

      (ffi/defcfn draw-text \"DrawText\" [:string :int :int :int :uint] :void)

  clj-kondo cannot see through the macro, so without this hook every bound name
  is an `Unresolved symbol` inside raylib.clj and an `Unresolved var: rl/…` at
  each of the ~460 call sites in the examples — enough noise to make the linter
  useless as a gate.

  The hook rewrites the form into a `defn` of the same name whose parameter count
  matches the C argument-type vector, and whose body is a literal of the declared
  C return type. That buys three things clj-kondo could not otherwise know:

    * the var exists (kills the false positives),
    * its arity — passing the wrong number of arguments to a binding is exactly
      the FFI mistake that otherwise surfaces only as a native crash,
    * its return type, so `(+ 1 (rl/measure-text …))` type-checks.

  Return-type mapping is deliberately conservative: numeric C types become a
  number, `:string` a string, and everything else (`:void`, `:pointer`) nil.
  `:pointer` is an opaque handle that only ever gets passed back into other
  `ffi/*` calls, which are themselves untyped here, so calling it nil costs
  nothing today. If a pointer ever flows somewhere type-checked, widen this map
  rather than deleting the hook."
  (:require [clj-kondo.hooks-api :as api]))

(def ^:private numeric-ret
  #{:int :uint :long :ulong :short :ushort :byte :ubyte :float :double :size-t})

(defn- ret-node
  "A literal whose inferred type matches the declared C return type."
  [ret]
  (let [k (when ret (api/sexpr ret))]
    (cond
      (contains? numeric-ret k) (api/token-node 0)
      (= :string k)             (api/string-node "")
      :else                     (api/token-node nil))))

(defn defcfn
  [{:keys [node]}]
  (let [[_defcfn name-node _c-symbol arg-types ret] (:children node)]
    ;; Only rewrite the shape we understand; anything else falls through to the
    ;; default analysis rather than silently interning a wrong var.
    (if (and name-node arg-types (api/vector-node? arg-types))
      (let [params (map-indexed (fn [i _] (api/token-node (symbol (str "_arg" i))))
                                (:children arg-types))
            expanded (api/list-node
                      [(api/token-node 'clojure.core/defn)
                       name-node
                       (api/vector-node (vec params))
                       (ret-node ret)])]
        {:node (with-meta expanded (meta node))})
      {:node node})))
