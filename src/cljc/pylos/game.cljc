(ns pylos.game
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  (:require [pylos.board :refer [square-corners number-of-positions removable-positions
                                 cell square-positions-at-position square-position-below
                                 positions-under-position position-on-top
                                 empty-positions number-of-positions-around
                                 has-ball balls-on-board can-remove-ball
                                 add-ball remove-ball removable-candidates removable-positions-of-color-below
                                 removable-candidates-under-position new-full-square-position
                                 has-balls-to-play number-of-balls-on-board]]))

; TODO remove defmulti
(defmulti make-move-on-board
  "Returns the board that is generated by making the given move"
  (fn [board move] (:type move)))

(defmethod make-move-on-board :add [board {:keys [type color position] :as move}]
  ; {:pre [(is (can-add? board color position))]}
  (add-ball board color position))

(defmethod make-move-on-board :rise [board {:keys [type color low-position position] :as move}]
  ; {:pre [(can-rise? board color low-position high-position)]}
  (-> board
      (remove-ball color low-position)
      (add-ball    color position)))

(defmethod make-move-on-board :square [board {:keys [type color positions original-move] :as move}]
  ; {:pre [(can-remove-positions? (make-move-board board original-move) (:color move) positions)]}
  (reduce #(remove-ball %1 color %2)
          (make-move-on-board board original-move) positions))

(defn move-square [original-move positions-to-remove square-position]
  (let [color (:color original-move)]
    {:type :square
     :original-move original-move
     :positions (into #{} positions-to-remove)
     :square-position square-position
     :color color}))

(defn move-add [color position]
  {:type :add
   :position position
   :color color})

(defn move-rise [color low-position high-position]
  {:type :rise
   :low-position low-position
   :position high-position
   :color color})

(defn all-tails [col]
  (map reverse (remove empty? (reductions conj [] col))))

(defn remove-balls-when-square [{:keys [position type] :as move} board color new-square-position]
  "Generates all possible ball removal possibilities given taht there is a square
  of the same color as player at the given position
  Assumes the given move has not been generated on the given board."
    (let [removable-balls              (removable-candidates board color position)
          removable-balls              (if (= :rise type) (disj removable-balls (:low-position move)) removable-balls) ; we remove the low position if rise
          moves-with-one-ball-removed  (map (fn [position] (move-square move [position] new-square-position)) removable-balls)
          moves-with-two-balls-removed (mapcat (fn [[position & new-removable-balls :as stuff]]
                                                 (let [; we add the balls that are below the first removed position if any
                                                       new-removable-balls (apply conj new-removable-balls (removable-positions-of-color-below board position color))]
                                                   (map (fn [second-position]
                                                          (move-square move [position second-position] new-square-position)) new-removable-balls))) (all-tails removable-balls))]
      (concat moves-with-two-balls-removed moves-with-one-ball-removed)))

(defn calculate-next-moves [{:keys [board player]} empty-position]
  (let [move-with-ball-added  (move-add player empty-position)
        moves-with-ball-risen (map #(move-rise player % empty-position)
                                   (removable-candidates-under-position board player empty-position))
        next-moves            (reduce (fn [all-moves {:keys [position type] :as move}]
                                        (let [new-square-position (new-full-square-position board position player)]
                                          (if (nil? new-square-position) (conj all-moves move)
                                                            (concat all-moves (remove-balls-when-square move board player new-square-position)))))
                                      [] (conj moves-with-ball-risen move-with-ball-added))]
    next-moves))

; (defn best-order [board]
;   (let [empty-positions            (empty-positions board)
;         number-of-positions-around (number-of-positions-around board)]
;     (sort-by #(-(number-of-positions-around %)) empty-positions)))

(defn compare-positions [board position1 position2]
  (let [number-of-positions-around (number-of-positions-around board)]
    (- (number-of-positions-around position2)
       (number-of-positions-around position1))))

(defn compare-moves [board]
  (fn [{type1 :type :as move1} {type2 :type :as move2}]
    (if (= type1 type2)
      ; we order by position
      (cond
        ; except if square, then we put the 2 before the ones
        (= type1 :square) (let [positions1 (:positions move1)
                                positions2 (:positions move2)]
                            (if (= (count positions1) (count positions2))
                              (compare-positions board
                                                 (:position (:original-move move1))
                                                 (:position (:original-move move2)))
                              (if (= 2 (count positions1)) -1 1)))
        :else (compare-positions board (:position move1) (:position move2)))
      ; we order by type
      (cond
        (= type1 :square) -1
        (= type2 :square) 1
        (= type1 :rise) -1
        (= type2 :rise) 1
        :else (throw (Exception. "Not possible"))))))

(defn order-moves [board moves]
  (sort (compare-moves board) moves))

(defn generate-all-moves [game-position]
  (mapcat #(calculate-next-moves game-position %) (empty-positions (:board game-position))))

(defn game-over? [board]
  (or (not (has-balls-to-play board :white))
      (not (has-balls-to-play board :black))))

(defn winner [board]
  ; {:pre [(game-over? board)]}
  (if (has-balls-to-play board :white) :white :black))
