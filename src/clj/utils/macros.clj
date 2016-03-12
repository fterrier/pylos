(ns utils.macros)

(defmacro t
  [label & body]
  `(do
     (.time js/console ~label)
     (let [result# (do ~@body)]
       (.timeEnd js/console ~label)
       result#)))
