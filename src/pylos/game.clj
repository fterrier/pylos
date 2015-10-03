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

(defn has-ball [board position]
  (let [cell (cell board position)]
    (or (= :black cell) (= :white cell))))

(defn can-remove-ball [board position]
  (contains? (:removable-positions (meta board)) position))

(defn removable-candidates 
  "Gives all candidates below the given position that can be removed"
  ([board color position]
   (let [balls-of-player      (balls-on-board board color)
         balls-to-check       (if (nil? position) balls-of-player
                                (filter #(contains? balls-of-player %) (positions-under-position board position)))
         removable-candidates (filter #(contains? (:removable-positions (meta board)) %) balls-to-check)]
     (into #{} removable-candidates)))
  ([board color] (removable-candidates board color nil)))

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

(defn is-full-square [board position color]
  (every? #(= color (cell board %)) (square-corners board position)))

(defn full-squares [board color]
  (->> board
       meta
       :full-squares
       color))

(defn add-ball [board color position]
  ; {:pre [(= :open (cell board position))
  ;        (has-balls-to-play board color)]}
  (let [meta-infos                    (meta board)
        board-with-ball               (change-cell board position color)
        new-square-positions          (squares-at-position board-with-ball position)
        new-full-squares              (filter #(is-full-square board-with-ball % color) new-square-positions)
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
        :full-squares   (update (:full-squares meta-infos) color #(apply conj % new-full-squares))
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
                                     (filter #(can-remove-ball board-without-ball %) (square-corners board-without-ball position-below)) [])]
    (with-meta
      (reduce #(change-cell %1 %2 :no-acc :open) board-without-ball open-positions-to-remove)
      (assoc meta-infos
        :empty-positions new-empty-positions
        :removable-positions (-> (apply conj (:removable-positions meta-infos) new-removable-positions)
                                 (disj position))
        :full-squares   (update (:full-squares meta-infos) color #(apply disj % square-positions-to-remove))
        :balls-on-board (update (:balls-on-board meta-infos) color #(disj % position))))))

(defn other-color [color]
  (if (= color :white) :black :white))

(defn has-new-full-square [board old-board color]
  (let [squares          (full-squares board color)
        old-squares      (full-squares old-board color)
        new-full-squares (apply disj squares old-squares)]
    (not (empty? new-full-squares))))

(defn can-remove-positions? [board color positions]
  (every? #(and (can-remove-ball board %)
                (= color (cell board %))) positions))

(defn can-add? [board color position]
  (= :open (cell board position)))

(defn can-rise? [board color low-position high-position]
  (and (can-remove-positions? board color [low-position])
       (contains? (positions-under-position board high-position) low-position)
       (can-add? board color high-position)
       (not= high-position (position-on-top board low-position))))

(defrecord BoardMove [board move])

; TODO test this
(defn move-square [{:keys [board move]} positions]
  ; {:pre [(can-remove-positions? board (:color move) positions)]}
  (let [color (:color move)]
    {:board (reduce #(remove-ball %1 color %2) board positions)
     :move {:type :square 
            :original-move move 
            :positions (into #{} positions)
            :color color}}))

; TODO test this
(defn move-add [board color position]
  ; {:pre [(is (can-add? board color position))]}
  {:board (add-ball board color position)
   :move  {:type :add 
           :position position
           :color color}})

; TODO test this
(defn move-rise [board color low-position high-position]
  ; {:pre [(can-rise? board color low-position high-position)]}
  {:board (-> board
              (remove-ball color low-position)
              (add-ball    color high-position))
   :move  {:type :rise 
           :low-position low-position 
           :high-position high-position 
           :color color}})

; TODO test this
(defn move-rise-no-add [board color low-position high-position]
  ; {:pre [(can-rise? board color low-position high-position)]}
  {:board (-> board
              (remove-ball color low-position))
   :move {:type :rise
          :low-position low-position
          :high-position high-position
          :color color}})

(defn remove-balls-if-whole-square [{:keys [board move] :as original-move} old-board color]
  "Generates all possible ball removal possibilities if there is a square
  of the same color as player"
  (if-not (has-new-full-square board old-board color) [{:board board :move move}]
    (let [removable-balls (removable-candidates board color)
          combinations    (combo/combinations removable-balls 2)]
      (concat (map (fn [positions] (move-square original-move positions)) combinations)
              (map (fn [position] (move-square original-move [position])) removable-balls)))))

(defn calculate-next-move [{:keys [board player]} position]
  (let [{new-board :board {high-position :position} :move
         :as new-move-with-added-ball} (move-add board player position)
        new-moves-with-risen-ball      (map #(move-rise-no-add new-board player % high-position) 
                                            (removable-candidates new-board player position))]
    (mapcat #(remove-balls-if-whole-square % board player) (conj new-moves-with-risen-ball new-move-with-added-ball))))

(defn best-order [board]
  (let [empty-positions            (empty-positions board)
        number-of-positions-around (number-of-positions-around board)]
    (sort-by #(-(number-of-positions-around %)) empty-positions)))

(defn board-move-map [game-position]
  (mapcat #(calculate-next-move game-position %) (best-order (:board game-position))))

; (defn board-move-map [game-position]
;   (mapcat #(calculate-next-move game-position %) (empty-positions (:board game-position))))

(defn game-over? [board]
  (or (not (has-balls-to-play board :white))
      (not (has-balls-to-play board :black))))

(defn winner [board]
  ; {:pre [(game-over? board)]}
  (if (has-balls-to-play board :white) :white :black))

(declare next-game-position)

(defrecord GamePosition [board player outcome]
  Game
  (next-game-positions 
    [game-position]
    (map (fn [new-move]
           (next-game-position game-position new-move)) (board-move-map game-position))))

(defn next-game-position [{:keys [player] :as game-position} {:keys [move board]}]  
  (let [game-over?         (game-over? board)
        next-game-position (map->GamePosition {:board board
                                               :player (other-color player)
                                               :outcome (if game-over? (winner board) nil)})]
    {:game-position next-game-position :move move}))
