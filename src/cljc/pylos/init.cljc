(ns pylos.init
  #?(:cljs (:require [pylos.board :refer [map->MetaBoard map->HelperMetaBoard size
                                          cell has-ball position-on-top positions-around]]))
  #?(:clj (:require [clojure.math.numeric-tower :as math]
                    [pylos.board :refer :all])))

; Starting board creation functions
(defn calculate-all-positions [size]
  (into [] (apply concat (for [layer (range 1 (+ 1 size))]
                           (let [max-size (+ 2 (- size layer))]
                             (for [x     (range 1 max-size)
                                   y     (range 1 max-size)]
                               [layer x y]))))))

(defn- calculate-positions-around [positions-map [layer row col :as position] ind]
  (remove #(nil? (positions-map %)) [[layer (+ row ind) (+ col ind)]
                                     [layer (+ row ind) col]
                                     [layer row (+ col ind)]
                                     [layer row col]]))

(defn- calculate-square-positions-at-position [positions-left-up positions-right-down position]
  "Returns a list of positions that are corners of squares
  containing the given position in the given board"
  (let [positions-to-try (positions-left-up position)
        positions-to-try (filter #(= 4 (count (positions-right-down %))) positions-to-try)]
    positions-to-try))

; TODO maybe have this function return something else when calling on the top position
(defn- calculate-position-on-top [position]
  (update position 0 inc))

; TODO maybe have this function return something else when calling on the bottom layer
(defn- calculate-square-positions-below [position]
  "Returns the position below or nil if we are at first layer"
  (let [position-below (update position 0 dec)]
    (if (< (position-below 0) 1) nil position-below)))

(defn- calculate-positions-under-position [all-positions [layer _ _ :as position]]
  (apply concat (for [layer-to-check (range 1 layer)]
                  (filter (fn [[layer _ _]] (= layer layer-to-check)) all-positions))))

(defn- calculate-positions-above-first-layer [all-positions]
  (filter (fn [[layer _ _]] (> layer 1)) all-positions))

(defn- calculate-empty-positions [all-positions]
  (filter (fn [[layer _ _]] (= 1 layer)) all-positions))

(defn- calculate-middle-positions [positions-right-down size]
  (let [middle-positions (for [layer (range 1 (- size 1))]
                           (let [size-of-layer (- size (- layer 1))
                                 middle        #?(:clj (math/ceil  (/ size-of-layer 2)) :cljs nil)
                                 one-ball      (= 1 (mod size-of-layer 2))]
                             (if one-ball [[layer middle middle]]
                               (positions-right-down [layer middle middle]))))
        middle-positions (apply concat middle-positions)]
    middle-positions))

(defn- calculate-all-positions-around [position positions-left-up positions-right-down]
  (apply conj (positions-left-up position) (positions-right-down position)))

(defn- vec-from-map [m size]
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
(defn- create-position-map [fun positions positions-map]
  (vec-from-map
    (into {} (map (fn [position] [(positions-map position) (into #{} (map positions-map (fun position)))]) positions))
    (count positions-map)))

; optimize data structure
(defn- create-position-map-one [fun positions positions-map]
  (vec-from-map
    (into {} (map (fn [position] [(positions-map position) (positions-map (fun position))]) positions))
    (count positions-map)))

; optimize data structure
(defn- create-position-set [positions positions-map]
  (into #{} (map positions-map positions)))


(defn- helper-meta-board [size]
  (let [all-positions                (calculate-all-positions size)
        number-of-positions          (count all-positions)
        positions-map                (into {} (map (fn [ind] [(all-positions ind) ind]) (range number-of-positions)))

        positions-above-first-layer-no-ind (calculate-positions-above-first-layer all-positions)
        positions-right-down-no-ind  (into {} (map (fn [position] [position (calculate-positions-around positions-map position 1)]) all-positions))
        positions-left-up-no-ind     (into {} (map (fn [position] [position (calculate-positions-around positions-map position -1)]) all-positions))

        positions-above-first-layer  (create-position-set positions-above-first-layer-no-ind positions-map)
        positions-right-down-map     (create-position-map #(calculate-positions-around positions-map % 1) all-positions positions-map)
        positions-left-up-map        (create-position-map #(calculate-positions-around positions-map % -1) all-positions positions-map)
        square-positions-below-map   (create-position-map-one #(calculate-square-positions-below %) all-positions positions-map)
        square-positions-at-position-map (create-position-map #(calculate-square-positions-at-position positions-left-up-no-ind positions-right-down-no-ind %) all-positions positions-map)
        positions-under-position-map (create-position-map #(calculate-positions-under-position all-positions %) positions-above-first-layer-no-ind positions-map)
        position-on-top-map          (create-position-map-one #(calculate-position-on-top %) all-positions positions-map)
        positions-around             (create-position-map #(calculate-all-positions-around % positions-left-up-no-ind positions-right-down-no-ind)
                                                          all-positions positions-map)]
    {:number-of-positions number-of-positions
     :size size
     :positions-right-down-map positions-right-down-map
     :positions-left-up-map positions-left-up-map
     :position-on-top-map position-on-top-map
     :square-positions-below-map square-positions-below-map
     :square-positions-at-position-map square-positions-at-position-map
     :positions-above-first-layer positions-above-first-layer
     :positions-under-position-map positions-under-position-map
     :number-of-positions-around (into [] (map (fn [positions-around] (count positions-around)) positions-around))
     ; this map is only used to translate a position in coordinates into the integer
     :positions-map positions-map
     :all-positions all-positions}))

(defn starting-board [size]
  {:pre [(even? (count (calculate-all-positions size)))]}
  (let [helper-meta-board   (helper-meta-board size)
        number-of-positions (:number-of-positions helper-meta-board)
        positions-map       (:positions-map helper-meta-board)
        all-positions       (:all-positions helper-meta-board)
        ; TODO optimize data structure
        board               (into [] (map (fn [ind] (if (< ind (* size size)) :open :no-acc)) (range number-of-positions)))
        empty-positions     (create-position-set (calculate-empty-positions all-positions) positions-map)]
    (with-meta board (map->MetaBoard
                       {:helper-meta-board (map->HelperMetaBoard helper-meta-board)
                        :empty-positions empty-positions
                        :removable-positions #{}
                        :balls-on-board {:black #{} :white #{}}}))))

; re-init functions from here on
(defn- retrieve-empty-positions [board all-positions]
  (into #{} (filter #(= :open (cell board %)) all-positions)))

(defn- retrieve-balls-on-board [board color all-positions]
  (into #{} (filter #(= color (cell board %)) all-positions)))

(defn- can-remove-ball-no-meta [board position]
  (and (has-ball board position)
       (every? #(not (has-ball board (position-on-top board %))) (positions-around board position :left-up))))

(defn- retrieve-removable-positions [board all-positions]
  (into #{} (filter #(can-remove-ball-no-meta board %) all-positions)))

(defn initialize-board-meta [board size]
  "Create all meta data for a board and attach to it"
  (let [size              4
        helper-meta-board (helper-meta-board size)
        all-positions     (range 0 (count board))
        board             (with-meta board {:helper-meta-board helper-meta-board})
        board             (with-meta board
                            (map->MetaBoard
                             {:helper-meta-board   helper-meta-board
                              ; TODO maybe send those 3 datastructures also from the server
                              :empty-positions     (retrieve-empty-positions board all-positions)
                              :balls-on-board      {:black (retrieve-balls-on-board board :black all-positions)
                                                    :white (retrieve-balls-on-board board :white all-positions)}
                              :removable-positions (retrieve-removable-positions board all-positions)}))]
    board))

(defn board-indexes [board]
  (let [size           (size board)
        positions-map  (:positions-map (:helper-meta-board (meta board)))
        frontend-board (into [] (for [layer (range 0 size)]
                                (into [] (for [row (range 0 (- size layer))]
                                           (into [] (for [col (range 0 (- size layer))]
                                                      (get positions-map [(inc layer) (inc row) (inc col)])))))))]
  frontend-board))
