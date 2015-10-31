(ns pylos.board
  (:require [pylos.init :refer [calculate-positions-around calculate-all-positions
                                calculate-positions-above-first-layer
                                create-position-map create-position-set create-position-map-one
                                calculate-square-positions-below calculate-square-positions-at-position
                                calculate-positions-under-position calculate-position-on-top
                                calculate-all-positions-around calculate-empty-positions]]))

(defrecord HelperMetaBoard [size number-of-positions
                            positions-right-down-map
                            positions-left-up-map
                            position-on-top-map
                            square-positions-below-map
                            square-positions-at-position-map
                            number-of-positions-around
                            positions-above-first-layer
                            positions-under-position-map
                            positions-map all-positions])

(defrecord MetaBoard [helper-meta-board empty-positions balls-on-board removable-positions])


(defn cell [board position]
  (get board position))

(defn empty-positions [board]
  (:empty-positions (meta board)))

(defn number-of-balls-on-board [board]
  (apply + (map #(count (second %)) (:balls-on-board (meta board)))))

(defn removable-positions [board]
  (:removable-positions (meta board)))

(defn ind [board position]
  ((:positions-map (:helper-meta-board (meta board))) position))

(defn size [board]
    (:size (:helper-meta-board (meta board))))

(defn number-of-positions-around [board]
  (:number-of-positions-around (:helper-meta-board (meta board))))

(defn positions-around [board position direction]
  {:pre [(or (= :left-up direction) (= :right-down direction))]}
  (let [meta-infos           (:helper-meta-board (meta board))
        positions-around-map (case direction
                               :left-up (:positions-left-up-map meta-infos)
                               :right-down (:positions-right-down-map meta-infos))]
    (positions-around-map position)))

(defn position-on-top [board position]
  ((:position-on-top-map (:helper-meta-board (meta board))) position))

(defn positions-under-position [board position]
  ((:positions-under-position-map (:helper-meta-board (meta board))) position))

(defn square-position-below [board position]
  ((:square-positions-below-map (:helper-meta-board (meta board))) position))

(defn positions-above-first-layer [board]
  (:positions-above-first-layer (:helper-meta-board (meta board))))

(defn square-corners [board position]
  (positions-around board position :right-down))

(defn square-positions-at-position [board position]
  "Returns a list of positions that are corners of squares
  containing the given position in the given board"
  ((:square-positions-at-position-map (:helper-meta-board (meta board))) position))

(defn number-of-positions [board]
  (:number-of-positions (:helper-meta-board (meta board))))

; Starting functions that access the state
(defn balls-on-board [board color]
  (color (:balls-on-board (meta board))))

(defn balls-remaining [board color]
  (let [number-of-balls (/ (number-of-positions board) 2)]
    (- number-of-balls (count (balls-on-board board color)))))

(defn has-ball [board position]
  (let [cell (cell board position)]
    (or (= :black cell) (= :white cell))))

(defn can-remove-ball [board position]
  (contains? (removable-positions board) position))

(defn helper-meta-board [size]
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

(defn retrieve-empty-positions [board all-positions]
  (into #{} (filter #(= :open (cell board %)) all-positions)))

(defn retrieve-balls-on-board [board color all-positions]
  (into #{} (filter #(= color (cell board %)) all-positions)))

(defn can-remove-ball-no-meta [board position]
  (and (has-ball board position)
       (every? #(not (has-ball board (position-on-top board %))) (positions-around board position :left-up))))

(defn retrieve-removable-positions [board all-positions]
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
