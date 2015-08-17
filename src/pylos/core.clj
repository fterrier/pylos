(ns pylos.core
  (:gen-class)
  (:require [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.math.combinatorics :as combo]
            [io.aviso.ansi :refer :all]))

; o o o o
; o o o o
; o o o o
; o o o o
;
; - - -
; - - -
; - - -
; 
; - -
; - -
;
; -
(defn print-board [board]
  (println (meta board))
  (println)
  (doseq [layer board]
    (doseq [rows layer]
      (doseq [cell rows]
        (print (str (case cell
                      :black  (bold-red "b")
                      :white  (bold-green "w")
                      :no-acc (str "-")
                      :open   (bold-black "o")
                      "") " ")))
      (println))
    (println)))

(defn print-game [game]
  (println (dissoc game :board :next-game))
  (print-board (:board game)))

(defn cell [board [layer row col]]
  (get-in board
          [(- layer 1)
           (- row 1)
           (- col 1)]))

(defn is-in-board [board position]
  (not (nil? (cell board position))))

(defn size [board]
  (count board))

(defn empty-positions [board]
  (sort (:empty-positions (meta board))))

(defn all-positions [board layer]
  (let [max-size (+ 2 (- (size board) layer))]
    (into #{} (for [x (range 1 max-size) 
                    y (range 1 max-size)] 
                [layer x y]))))

(defn change-cell 
  ([partial-board partial-position new-cell]
   "Changes a cell to a new cell"
   (if (empty? partial-position)
     new-cell
     (let [position (- (partial-position 0) 1)]
       (assoc partial-board 
         position
         (change-cell (partial-board position) (into [] (rest partial-position)) new-cell)))))
  ([partial-board partial-position new-cell cell-to-replace]
   "Changes a cell to a new cell only if the cell contains cell-to-replace"
   (if (= cell-to-replace (cell partial-board partial-position))
     (change-cell partial-board partial-position new-cell)
     partial-board)))

(defn positions-around [[layer row col] ind]
  [[layer (+ row ind) (+ col ind)]
   [layer (+ row ind) col] 
   [layer row (+ col ind)]       
   [layer row col]])

(defn square-corners [position]
  (positions-around position 1))

(defn has-ball [board position]
  (let [cell (cell board position)]
    (or (or (= :black cell) (= :white cell)))))

(defn has-square [board position]
  (let [positions-to-check (square-corners position)]
    (every? #(has-ball board %) positions-to-check)))

(defn squares-at-position [board position]
  "Returns a list of positions that are corners of squares
  containing the given position in the given board"
  (let [positions-to-try (positions-around position -1)]
    (filter #(has-square board %) positions-to-try)))

(defn top-position [board]
  [(size board) 1 1])

(defn position-on-top [position]
  (update position 0 inc))

(defn balls-on-board [board color]
  (count (color (:balls-on-board (meta board)))))

(defn has-balls-to-play [board color]
  (let [balls-per-player       (/ (:number-of-positions (meta board)) 2)
        balls-of-color-in-play (balls-on-board board color)]
    (not (= balls-per-player balls-of-color-in-play))))

(defn is-full-square [board position color]
  (every? #(= color (cell board %)) (square-corners position)))

(defn full-squares [board color]
  (->> board
       meta
       :full-squares
       color))

(defn add-ball [board color [layer row col :as position]]
  {:pre [(= :open (cell board position))
         (has-balls-to-play board color)]}
  (let [meta-infos           (meta board)
        board-with-ball      (change-cell board position color)
        new-square-positions (squares-at-position board-with-ball position)
        new-full-squares     (filter #(is-full-square board-with-ball % color) new-square-positions)
        new-open-positions   (map position-on-top new-square-positions)
        new-empty-positions  (-> (apply conj (:empty-positions meta-infos) new-open-positions)
                                 (disj position))]
    (with-meta 
      (reduce #(change-cell %1 %2 :open :no-acc) board-with-ball new-open-positions)
      (assoc meta-infos
        :square-positions (apply conj (:square-positions meta-infos) new-square-positions)
        :empty-positions new-empty-positions
        :full-squares   (update (:full-squares meta-infos) color #(apply conj % new-full-squares))
        :balls-on-board (update (:balls-on-board meta-infos) color #(conj % position))))))

(defn can-remove-ball [board position]
  (let [positions-to-check (map position-on-top (positions-around position -1))]
    (not (some #(has-ball board %) positions-to-check))))

(defn remove-ball [board color [layer row col :as position]]
  {:pre [(= color (cell board position))
         (can-remove-ball board position)]}
  (let [meta-infos                 (meta board)
        board-without-ball         (change-cell board position :open)
        square-positions-to-remove (squares-at-position board position)
        open-positions-to-remove   (map position-on-top square-positions-to-remove)
        new-empty-positions        (-> (apply disj (:empty-positions meta-infos) open-positions-to-remove)
                                       (conj position))]
    (with-meta
      (reduce #(change-cell %1 %2 :no-acc :open) board-without-ball open-positions-to-remove)
      (assoc meta-infos
        :square-positions (apply disj (:square-positions meta-infos) square-positions-to-remove)
        :empty-positions new-empty-positions
        :full-squares   (update (:full-squares meta-infos) color #(apply disj % square-positions-to-remove))
        :balls-on-board (update (:balls-on-board meta-infos) color #(disj % position))))))

(defn removable-candidates 
  ([board color layers]
   (let [removable-candidates           (color (:balls-on-board (meta board)))
         removable-candidates-in-layers (if (nil? layers) removable-candidates 
                                          (filter (fn [[layer _ _]] (contains? layers layer)) 
                                                  removable-candidates))
         ; TODO we could optimize this and not have to check all (prepare meta-data on position)
         candidates                     (filter #(can-remove-ball board %) removable-candidates-in-layers)]
     (into #{} candidates)))
  ([board color] (removable-candidates board color nil)))

(defn low-candidates-for-square-position [board color [layer row col :as position]]
  "Given a board, a color and a square position (*not* the position on top), returns
  a list of candidates that can be used to rise to the position on top of that square"
  {:pre [(has-square board position)]}
  (let [lower-layers         (into #{} (range 1 (+ layer 1)))
        removable-candidates (removable-candidates board color lower-layers)
        candidates           (apply disj removable-candidates (square-corners position))]
    candidates))

(defn rise-candidates [board color]
  "Returns a vector of {:low-position x, :high-position y} objects
  that gives the potential candidates for rising a ball"
  (let [square-positions       (:square-positions (meta board))
        ; TODO we could optimize this and not have to check all (prepare meta-data on position)
        empty-square-positions (remove #(has-ball board (position-on-top %)) square-positions)
        low-positions-map      (map (fn [position] {:low-positions (low-candidates-for-square-position board color position) 
                                                    :high-position (position-on-top position)}) empty-square-positions)
        low-high-position-map  (reduce #(concat %1 (map (fn [low-position] {:low-position low-position
                                                                            :high-position (:high-position %2)}) 
                                                        (:low-positions %2))) [] low-positions-map)]
    (into #{} low-high-position-map)))

(defn other-color [color]
  (if (= color :white) :black :white))

(defn next-player [game]
  (let [board        (:board game)
        player       (:player game)
        other-player (other-color player)]
    (if (not (has-balls-to-play board other-player)) player other-player)))

(defn remove-balls-if-whole-square [board old-board color]
  "Generates all possible ball removal possibilities if there is a square
  of the same color as player"
  (let [squares          (full-squares board color)
        old-squares      (full-squares old-board color)
        new-full-squares (apply disj squares old-squares)]
    (if (empty? new-full-squares) [board]
      (let [removable-balls (removable-candidates board color)
            combinations    (combo/combinations removable-balls 2)]
        (concat (map #(remove-ball board color %) removable-balls)
                (map (fn [[x y]] (-> board
                                     (remove-ball color x)
                                     (remove-ball color y))) combinations))))))

(defn moves [game]  
  (let [board       (:board game)
        player      (:player game)
        next-player (next-player game)
        new-boards-with-new-ball      (map #(add-ball board player %)
                                           (empty-positions board))
        ; TODO we could optimize this and start from new-boards-with-new-ball and remove the possible lows
        new-boards-with-risen-ball    (map #(add-ball (remove-ball board player (:low-position %)) 
                                                      player (:high-position %))
                                           (rise-candidates board player))
        new-boards                    (concat new-boards-with-new-ball new-boards-with-risen-ball)
        new-boards-with-removed-balls (apply concat (map #(remove-balls-if-whole-square % board player) new-boards))]
    (map (fn [new-board] {:board new-board 
                          :player next-player}) new-boards-with-removed-balls)))

(defn game-over? [board]
  (empty? (empty-positions board)))

(defn winner [board]
  {:pre [(game-over? board)]}
  (cell board (top-position board)))

(defn number-of-positions [size]
  (let [layers (range 1 (+ 1 size))]
    (reduce + (map #(* % %) layers))))

(defn starting-board [size]
  {:pre [(even? (number-of-positions size))]}
  (let [board (into [] (for [x (range size 0 -1)]
                         (into [] (repeat x 
                                          (into [] (repeat x (if (= size x) :open :no-acc)))))))]
    (with-meta board 
               {:number-of-positions (number-of-positions size)
                :square-positions #{}
                :empty-positions (all-positions board 1)
                :full-squares   {:black #{} :white #{}}
                :balls-on-board {:black #{} :white #{}}})))

(defn initial-game [size first-player]
  {:board (starting-board size) 
   :player first-player})

(def four (starting-board 4))
(def four-square (-> four 
                     (add-ball :black [1 1 1]) 
                     (add-ball :white [1 2 1]) 
                     (add-ball :white [1 1 2]) 
                     (add-ball :black [1 2 2])
                     (add-ball :white [1 2 3])))
(def three (starting-board 3))

(def game-four (initial-game 4 :white))

; (defn initial-game-tree [size first-player]
;   (tree-seq #(not (game-over? (:board %))) moves 
;             (initial-game size first-player)))
; (def tree-game-four (initial-game-tree 4 :white))

(defn score-for-player [board player]
  (if (game-over? board)
    (if (= (winner board) player) 100 -100)
    (let [player-balls       (balls-on-board board player)
          other-player-balls (balls-on-board board (other-color player))
          ball-difference    (- other-player-balls player-balls)]  
      ball-difference)))

(defn negamax 
  ([game alpha beta depth]
  "For a game, applies the negamax algorithm on the tree up to depth,
  returns a game with a :score value and :next-game or :outcome value that 
  returns the next best game from which the value was calculated 
  or an :outcome value if the game is won."
  (when 
    (nil? (:outcome game))
    (let [board  (:board game)
          player (:player game)
          score  (score-for-player board player)]
      (if (game-over? board)
        (assoc game :score score :outcome (winner board))
        ; else we go on with the negamax algorithm
        (if (= depth 0)
          (assoc game :score score)
          (let [children-negamax  (map #(negamax %  (- depth 1)) (moves game))
                negamax-best-game (reduce (fn [{:keys [alpha beta best-game best-score]} game] 
                                            (let [child-negamax   (negamax game (- beta) (- alpha) (- depth 1))
                                                  child-score     (- (:score child-negamax))
                                                  next-best-game  (if (> child-score best-score) 
                                                                    {:game child-negamax :score child-score}
                                                                    {:game best-game     :score best-score})
                                                  next-alpha      (max alpha child-score)]
                                              (let [result {:alpha next-alpha 
                                                            :beta beta 
                                                            :best-game (:game next-best-game) 
                                                            :best-score (:score next-best-game)}]
                                                (if (>= next-alpha beta) (reduced result) result)
                                                ;result
                                                )))
                                          {:alpha alpha :beta beta :best-score -1000} (moves game))]
            (assoc game 
              :score     (:best-score negamax-best-game) 
              :next-game (:best-game  negamax-best-game))))))))
  ([game depth]
   (negamax game -1000 1000 depth)))

(defn negamax-game [game negamax-depth] 
  (iterate #(:next-game (negamax % negamax-depth)) game))

(defn play-negamax [size first-player negamax-depth]
  (into [] (map print-game (take-while (complement nil?) (negamax-game (initial-game size first-player) negamax-depth)))))




