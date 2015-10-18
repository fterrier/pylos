(ns pylos.game
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  (:require [game.game :refer :all]
            [pylos.board :refer :all]
            [clojure.math.combinatorics :as combo]
            [clojure.test :refer (is)]))

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
  (contains? (:removable-positions (meta board)) position))

(defn has-new-full-square-at-square-position [board square-position position color]
  (let [square-corners                  (square-corners board square-position)
        square-corners-without-position (disj square-corners position)]
    (every? #(= color (cell board %)) square-corners-without-position)))

(defn has-new-full-square [board position color]
  "Checks whether the board would have a now full square if the
  board would be filled with a ball of the given color at the given position"
  (let [square-positions (square-positions-at-position board position)]
    (some? (some #(has-new-full-square-at-square-position board % position color) square-positions))))

(defn removable-balls [board position]
  "Gives all balls that can be removed assuming the given position is filled."
  (let [removable-positions   (:removable-positions (meta board))
        square-position-below (square-position-below board position)]
    (if (nil? square-position-below)
      removable-positions
      (apply disj removable-positions (square-corners board square-position-below)))))

(defn removable-candidates-under-position
  "Gives all candidates under the given position that can be removed,
  assuming the given position would be filled by the given color on the board."
  [board color position]
  (let [removable-balls (removable-balls board position)
        positions-under (positions-under-position board position)]
    (into #{} (filter #(and (contains? removable-balls %) (contains? positions-under %))
                      (balls-on-board board color)))))

(defn removable-candidates
  "Gives all the candidates on the board that can be removed,
  assuming the given position would be filled by the given color on the board."
  [board color position]
  (let [removable-balls (removable-balls board position)]
    (into #{} (filter #(contains? removable-balls %) (balls-on-board board color)))))

(defn change-cell
  ([board position new-cell]
   "Changes a cell to a new cell"
   (assoc board position new-cell))
  ([partial-board partial-position new-cell cell-to-replace]
   "Changes a cell to a new cell only if the cell contains cell-to-replace"
   (if (= cell-to-replace (cell partial-board partial-position))
     (change-cell partial-board partial-position new-cell)
     partial-board)))

(defn has-square [board position]
  (let [positions-to-check (square-corners board position)]
    (and (not (empty? positions-to-check))
         (every? #(has-ball board %) positions-to-check))))

(defn squares-at-position [board position]
  (let [positions-to-try (square-positions-at-position board position)]
    (filter #(has-square board %) positions-to-try)))

(defn has-balls-to-play [board color]
  (let [balls-per-player       (/ (number-of-positions board) 2)
        balls-of-color-in-play (count (balls-on-board board color))]
    (not (= balls-per-player balls-of-color-in-play))))

(defn add-ball [board color position]
  ; {:pre [(= :open (cell board position))
  ;        (has-balls-to-play board color)]}
  (let [meta-infos                    (meta board)
        board-with-ball               (change-cell board position color)
        new-square-positions          (squares-at-position board-with-ball position)
        new-open-positions            (map #(position-on-top board %) new-square-positions)
        new-empty-positions           (-> (apply conj (:empty-positions meta-infos) new-open-positions)
                                          (disj position))
        position-below                (square-position-below board position)
        removable-positions-to-remove (if-not (nil? position-below) (square-corners board-with-ball position-below) [])]
    (with-meta
      (reduce #(change-cell %1 %2 :open :no-acc) board-with-ball new-open-positions)
      (assoc meta-infos
        :empty-positions new-empty-positions
        :removable-positions (-> (apply disj (:removable-positions meta-infos) removable-positions-to-remove)
                                 (conj position))
        :balls-on-board (update (:balls-on-board meta-infos) color #(conj % position))))))


(defn remove-ball [board color position]
  ; {:pre [(= color (cell board position))
  ;        (can-remove-ball board position)]}
  (let [meta-infos                 (meta board)
        board-without-ball         (change-cell board position :open)
        square-positions-to-remove (squares-at-position board position)
        open-positions-to-remove   (map #(position-on-top board %) square-positions-to-remove)
        new-empty-positions        (-> (apply disj (:empty-positions meta-infos) open-positions-to-remove)
                                       (conj position))
        position-below             (square-position-below board position)
        new-removable-positions    (if-not (nil? position-below)
                                     (remove (fn [position]
                                               (let [positions-to-try (square-positions-at-position board-without-ball position)]
                                                 (some #(has-ball board-without-ball (position-on-top board-without-ball %))
                                                       positions-to-try)))
                                             (square-corners board-without-ball position-below)) [])]
    (with-meta
      (reduce #(change-cell %1 %2 :no-acc :open) board-without-ball open-positions-to-remove)
      (assoc meta-infos
        :empty-positions new-empty-positions
        :removable-positions (-> (apply conj (:removable-positions meta-infos) new-removable-positions)
                                 (disj position))
        :balls-on-board (update (:balls-on-board meta-infos) color #(disj % position))))))

(defn other-color [color]
  (if (= color :white) :black :white))

(defn can-remove-position? [board color position]
  (and (can-remove-ball board position)
       (= color (cell board position))))

(defn can-add? [board color position]
  "Checks if the given add can be done, given that the move has not been
  generated on the board."
  (= :open (cell board position)))

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

(defn move-square [original-move positions-to-remove]
  (let [color (:color original-move)]
    {:type :square
     :original-move original-move
     :positions (into #{} positions-to-remove)
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

(defn remove-balls-if-whole-square [{:keys [position type] :as move} board color]
  "Generates all possible ball removal possibilities if there is a square
  of the same color as player.
  Assumes the given move has not been generated on the given board."
  (if-not (has-new-full-square board position color) [move]
    (let [removable-balls (removable-candidates board color position)
          ; we remove the low position if it is a rise
          removable-balls (if (= :rise type) (disj removable-balls (:low-position move)) removable-balls)
          combinations    (combo/combinations removable-balls 2)]
      (concat (map (fn [positions] (move-square move positions)) combinations)
              (map (fn [position] (move-square move [position])) removable-balls)))))

(defn calculate-next-move [{:keys [board player]} position]
  (let [move-with-ball-added  (move-add player position)
        moves-with-ball-risen (map #(move-rise player % position)
                                   (removable-candidates-under-position board player position))]
    (mapcat #(remove-balls-if-whole-square % board player) (conj moves-with-ball-risen move-with-ball-added))))

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
  (mapcat #(calculate-next-move game-position %) (empty-positions (:board game-position))))

(defn game-over? [board]
  (or (not (has-balls-to-play board :white))
      (not (has-balls-to-play board :black))))

(defn winner [board]
  ; {:pre [(game-over? board)]}
  (if (has-balls-to-play board :white) :white :black))

(declare next-game-position)

(defrecord GamePosition [board player outcome]
  Game
  (generate-moves [game-position]
                  (order-moves board (generate-all-moves game-position)))
  (make-move [game-position move]
             (next-game-position game-position move (make-move-on-board board move))))


(defn next-game-position [{:keys [player] :as game-position} move board]
  {:pre [(= player (:color move))]}
  (let [game-over?         (game-over? board)
        next-game-position (map->GamePosition {:board board
                                               :player (other-color player)
                                               :outcome (if game-over? (winner board) nil)})]
    next-game-position))
