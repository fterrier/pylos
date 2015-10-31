(ns pylos.init
  #?(:clj (:require [clojure.math.numeric-tower :as math])))

; Starting board creation functions
(defn calculate-all-positions [size]
  (into [] (apply concat (for [layer (range 1 (+ 1 size))]
                           (let [max-size (+ 2 (- size layer))]
                             (for [x     (range 1 max-size)
                                   y     (range 1 max-size)]
                               [layer x y]))))))

(defn calculate-positions-around [positions-map [layer row col :as position] ind]
  (remove #(nil? (positions-map %)) [[layer (+ row ind) (+ col ind)]
                                     [layer (+ row ind) col]
                                     [layer row (+ col ind)]
                                     [layer row col]]))

(defn calculate-square-positions-at-position [positions-left-up positions-right-down position]
  "Returns a list of positions that are corners of squares
  containing the given position in the given board"
  (let [positions-to-try (positions-left-up position)
        positions-to-try (filter #(= 4 (count (positions-right-down %))) positions-to-try)]
    positions-to-try))

; TODO maybe have this function return something else when calling on the top position
(defn calculate-position-on-top [position]
  (update position 0 inc))

; TODO maybe have this function return something else when calling on the bottom layer
(defn calculate-square-positions-below [position]
  "Returns the position below or nil if we are at first layer"
  (let [position-below (update position 0 dec)]
    (if (< (position-below 0) 1) nil position-below)))

(defn calculate-positions-under-position [all-positions [layer _ _ :as position]]
  (apply concat (for [layer-to-check (range 1 layer)]
                  (filter (fn [[layer _ _]] (= layer layer-to-check)) all-positions))))

(defn calculate-positions-above-first-layer [all-positions]
  (filter (fn [[layer _ _]] (> layer 1)) all-positions))

(defn calculate-empty-positions [all-positions]
  (filter (fn [[layer _ _]] (= 1 layer)) all-positions))

(defn calculate-middle-positions [positions-right-down size]
  (let [middle-positions (for [layer (range 1 (- size 1))]
                           (let [size-of-layer (- size (- layer 1))
                                 middle        #?(:clj (math/ceil  (/ size-of-layer 2)) :cljs nil)
                                 one-ball      (= 1 (mod size-of-layer 2))]
                             (if one-ball [[layer middle middle]]
                               (positions-right-down [layer middle middle]))))
        middle-positions (apply concat middle-positions)]
    middle-positions))

(defn calculate-all-positions-around [position positions-left-up positions-right-down]
  (apply conj (positions-left-up position) (positions-right-down position)))

(defn vec-from-map [m size]
  (into [] (let [ind (range 0 size)]
             (map #(m %) ind))))

; (defn set-array-from-map [m size]
;   (let [array (object-array size)]
;     (amap ^java.util.Set array idx ret (m idx))))
;
; (defn int-array-from-map [m size]
;   (let [my-array (make-array Integer/TYPE size)]
;     (amap ^ints my-array idx ret (m idx))))

; optimize data structure
(defn create-position-map [fun positions positions-map]
  (vec-from-map
    (into {} (map (fn [position] [(positions-map position) (into #{} (map positions-map (fun position)))]) positions))
    (count positions-map)))

; optimize data structure
(defn create-position-map-one [fun positions positions-map]
  (vec-from-map
    (into {} (map (fn [position] [(positions-map position) (positions-map (fun position))]) positions))
    (count positions-map)))

; optimize data structure
(defn create-position-set [positions positions-map]
  (into #{} (map positions-map positions)))
