(ns pylos.move
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  #?@(:clj
       [(:require
         [pylos.board
          :refer
          [add-ball
           can-add-position?
           can-remove-position?
           empty-positions
           has-balls-to-play
           new-full-square-position
           number-of-positions-around
           positions-under-position
           removable-candidates
           removable-candidates-under-position
           removable-positions-of-color-below
           remove-ball]])]
       :cljs
       [(:require
         [pylos.board
          :refer
          [add-ball
           can-add-position?
           can-remove-position?
           positions-under-position
           empty-positions
           has-balls-to-play
           new-full-square-position
           number-of-positions-around
           removable-candidates
           removable-candidates-under-position
           removable-positions-of-color-below
           remove-ball]])]))

(declare make-move-on-board)

(defn is-move-allowed [board {:keys [color type position low-position positions original-move] :as move}]
  (case type
    :add (can-add-position? board color position)
    :rise (and (can-remove-position? board color low-position)
               (contains? (positions-under-position board position) low-position)
               (can-add-position? (remove-ball board color low-position) color position))
    :square (and (is-move-allowed board original-move)
                 (reduce #(if-not (can-remove-position? %1 color %2) (reduced false) (remove-ball %1 color %2)) (make-move-on-board board original-move) positions))))

(defn make-move-on-board [board {:keys [color type position original-move low-position positions] :as move}]
  "Returns the board that is generated by making the given move"
  {:pre [(is-move-allowed board move)]}
  (case type
    :add (add-ball board color position)
    :rise (-> board
              (remove-ball color low-position)
              (add-ball    color position))
    :square (reduce #(remove-ball %1 color %2)
                    (make-move-on-board board original-move) positions)))

(defn move-square [original-move positions-to-remove square-position]
  (let [color (:color original-move)]
    {:type :square
     :original-move original-move
     :positions positions-to-remove
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
                                                 (concat
                                                   (map (fn [second-position] (move-square move #{position second-position} new-square-position)) new-removable-balls)
                                                   (map (fn [second-position] (move-square move [position second-position] new-square-position)) (removable-positions-of-color-below board position color))))
                                               (all-tails removable-balls))]
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
    (into [] next-moves)))

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