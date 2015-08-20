(ns pylos.core
  (:gen-class)
  (:require [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.math.combinatorics :as combo]
            [io.aviso.ansi :refer :all]
            [clojure.string :as str]))

(defn cell [board [layer row col]]
  (get-in board
          [(- layer 1)
           (- row 1)
           (- col 1)]))

(defn size [board]
  (count board))

(defn is-in-board [board position]
  (not (nil? (cell board position))))

(defn positions-around [[layer row col] ind]
  [[layer (+ row ind) (+ col ind)]
   [layer (+ row ind) col] 
   [layer row (+ col ind)]       
   [layer row col]])

(defn position-on-top [position]
  (update position 0 inc))

(defn has-ball [board position]
  (let [cell (cell board position)]
    (or (or (= :black cell) (= :white cell)))))

(defn bold-positions-from-move [last-move]
  (case (:type last-move)
    :rise   [(:low-position last-move) (:high-position last-move)]
    :add    [(:position last-move)]
    :square [concat (:positions last-move) (bold-positions-from-move (:original-move last-move))]
    []))

(defn can-remove-ball [board position]
  (let [positions-to-check (map position-on-top (positions-around position -1))]
    (not (some #(has-ball board %) positions-to-check))))

(defn print-cell [board position last-move]
  (let [cell            (cell board position)
        bold-positions  (into #{} (bold-positions-from-move last-move))
        bold?           (contains? bold-positions position)
        blocked?        (not (can-remove-ball board position))]
    (print (str (case cell
                  :black  (if bold? (inverse (red "b")) (if blocked? (red "b") (str csi 4 sgr (red "b") reset-font)))
                  :white  (if bold? (inverse (green "w")) (if blocked? (green "w") (str csi 4 sgr (green "w") reset-font)))
                  :no-acc (str "-")
                  :open   (if bold? (inverse (black "o")) (black "o"))
                  "") " "))))

(defn print-board [board last-move]
  (println)
  (doseq [row (range 1 (+ 1 (size board)))]
    (doseq [layer (range 1 (+ 1 (size board)))]
      (doseq [col (range 1 (+ 2 (- (size board) layer)))]
        (let [position [layer row col]]
          (when (is-in-board board position) (print-cell board position last-move))))
      (print "    "))
    (println))
  (println))

(defn print-game [game]
  (println "====================")
  (if-not (empty? (:past-moves game))
    (let [last-move (last (:past-moves game))]
      (println "Board after move of" (:color last-move))
      (println "====================")
      (println)
      (println "Move:" last-move))
    (do
      (println "Initial board")
      (println "====================")))
  (print-board (:board game) (last (:past-moves game)))
  (println "Next move is for" (:player game))
  (println))

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

(defn square-corners [position]
  (positions-around position 1))

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

(defn next-player [board player]
  (let [other-player (other-color player)]
    (if (not (has-balls-to-play board other-player)) player other-player)))

(defn has-new-full-square [board old-board color]
  (let [squares          (full-squares board color)
        old-squares      (full-squares old-board color)
        new-full-squares (apply disj squares old-squares)]
    (not (empty? new-full-squares))))

(defn move-add [] nil)
(defn move-rise [] nil)

(defn move-square [board player original-move positions]
  {:board (reduce #(remove-ball %1 player %2) board positions)
   :move {:type :square 
          :original-move original-move 
          :positions (into #{} positions)
          :color player}})


(defn remove-balls-if-whole-square [{:keys [board move]} old-board color]
  "Generates all possible ball removal possibilities if there is a square
  of the same color as player"
  (if-not (has-new-full-square board old-board color) [{:board board :move move}]
    (let [removable-balls (removable-candidates board color)
          combinations    (combo/combinations removable-balls 2)]
      (concat (map (fn [position] (move-square board color move [position])) removable-balls)
              (map (fn [positions] (move-square board color move positions)) combinations)))))

(defn create-next-game [game move new-board]
  (let [next-player (next-player new-board (:player game))]
    {:board new-board
     :player next-player
     :past-moves (conj (:past-moves game) move)}))

; TODO write TESTs for this !
(defn moves [game]  
  (let [board       (:board game)
        player      (:player game)
        new-moves-with-new-ball      (map (fn [position] {:board (add-ball board player position)
                                                          :move  {:type :add 
                                                                  :position position
                                                                  :color player}}) (empty-positions board))
        ; TODO we could optimize this and start from new-boards-with-new-ball and remove the possible lows
        new-moves-with-risen-ball    (map (fn [candidate] {:board (-> board
                                                                      (remove-ball player (:low-position candidate))
                                                                      (add-ball    player (:high-position candidate)))
                                                           :move  {:type :rise 
                                                                   :low-position (:low-position candidate) 
                                                                   :high-position (:high-position candidate) 
                                                                   :color player}})
                                          (rise-candidates board player))
        new-moves                    (concat new-moves-with-new-ball new-moves-with-risen-ball)
        new-moves-with-removed-balls (apply concat (map (fn [move] (remove-balls-if-whole-square move board player)) new-moves))]
    (map (fn [new-move] (create-next-game game (:move new-move) (:board new-move))) new-moves-with-removed-balls)))

; TODO change this to "check if one player does not have ball"
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
   :player first-player
   :past-moves []})

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
  ; (if (game-over? board)
  ;   (if (= (winner board) player) 100 -100)
  (let [player-balls       (balls-on-board board player)
        other-player-balls (balls-on-board board (other-color player))
        ball-difference    (- other-player-balls player-balls)]  
    ball-difference)
  ; )
  )

; (defn game-decided? [board]
;   (let [balls-per-player (/ (:number-of-positions (meta board)) 2)]
;     (or (= balls-per-player (balls-on-board board :white))
;         (= balls-per-player (balls-on-board board :black)))))

(defn negamax 
  ([game alpha beta depth]
   "For a game, applies the negamax algorithm on the tree up to depth,
   returns an object with a :next-best-score value and :next-game or :outcome value that 
   returns the next best game from which the value was calculated 
   or an :outcome value if the game is won."
   (when 
     (nil? (:outcome game))
     (let [board  (:board game)
           player (:player game)
           score  (score-for-player board player)]
       (if (game-over? board)
         ; TODO factor this out in "winner-if-done"
         {:next-best-score score :outcome (winner board)}
         ; else we go on with the negamax algorithm
         (if (= depth 0)
           {:next-best-score score}
           (let [negamax-best-game (reduce (fn [{:keys [alpha beta best-game best-score]} game] 
                                             (let [next-player     (:player game)
                                                   child-negamax   (if (not= next-player player) 
                                                                     (negamax game (- beta) (- alpha) (- depth 1))
                                                                     (negamax game alpha beta (- depth 1)))
                                                   child-score     (if (not= next-player player) (- (:next-best-score child-negamax)) (:next-best-score child-negamax))
                                                   next-best-game  (if (> child-score best-score) 
                                                                     {:game game      :score child-score}
                                                                     {:game best-game :score best-score})
                                                   next-alpha      (max alpha child-score)]
                                               (let [result {:alpha next-alpha 
                                                             :beta beta 
                                                             :best-game (:game next-best-game) 
                                                             :best-score (:score next-best-game)}]
                                                 (if (>= next-alpha beta) (reduced result) result)
                                                 ; result
                                                 )))
                                           {:alpha alpha :beta beta :best-score -1000} (moves game))
                 game-with-score    {:next-best-score (:best-score negamax-best-game) 
                                     :next-game       (:best-game  negamax-best-game)}]
             game-with-score))))))
  ([game depth]
   (negamax game -1000 1000 depth)))

(defn to-int [array]
  (into [] (map #(try (Integer/parseInt %) (catch Exception e -1)) array)))

(defn ask-for-position 
  ([board text allow-enter]
   (println text)
   (let [position-string (read-line)]
     (if (and allow-enter (= position-string "")) nil
       (let [position-array  (str/split position-string #" +")
             position        (to-int position-array)]
         (if (or (not (= 3 (count position)))
                 (not (is-in-board board position)))
           (recur board text allow-enter)
           position)))))
  ([board text]
   (ask-for-position board text false)))

(defn ask-human-to-place-or-rise-ball [game]
  (let [board    (:board game)
        player   (:player game)
        position (ask-for-position board "Please enter a valid position [layer row col]")]
    (if (has-ball board position)
      (if (or (not (can-remove-ball board position))
              (not= player (cell board position)))
        (do 
          (println "That ball cannot be removed")
          (recur game))
        (let [high-position (ask-for-position board "Please enter a position for rise [layer row col]")]
          (if (or (<= (high-position 0) (position 0))
                  (not= :open (cell board high-position))
                  ; TODO one condition is missing - not rise a ball on top of itself
                  )
            (do 
              (println "Invalid move, we start again")
              (recur game))
            {:board (-> board 
                        (remove-ball player position)
                        (add-ball player high-position))
             ; TODO factor create-next-game out and create 3 functions for each move
             ; with is-valid-<move> preconditions
             :move {:type :rise
                    :low-position position
                    :high-position high-position
                    :color player}})))
      (if (not= :open (cell board position))
        (do 
          (println "Invalid move, we start again")
          (recur game))
        {:board (add-ball board player position)
         ; TODO refactor this and create move-<move> and is-valid-<move> functions
         :move {:type :add 
                :position position
                :color player}}))))

(defn ask-human-to-remove-balls [game {:keys [board move]} balls-removed]
  (let [old-board               (:board game)
        player                  (:player game)
        number-of-balls-removed (count balls-removed)]
    (if (not (has-new-full-square board old-board player))
      {:board board :move move}
      (if (= 2 number-of-balls-removed)
        (move-square board player move balls-removed)
        (let [position (ask-for-position board 
                                         (str "Please enter a ball to remove [layer row col]" 
                                              (if (not= 0 number-of-balls-removed) "or <enter> to finish" ""))
                                         (not= 0 number-of-balls-removed))]
          (if (nil? position)
            (move-square board player move balls-removed)
            ; TODO factor this check out (there is the same above)
            (if (or (not (= player (cell board position)))
                    (not (can-remove-ball board position)))
              (do 
                (println "Cannot remove that ball")
                (recur game {:board board :move move} balls-removed))
              (ask-human-to-remove-balls game {:board board :move move} (conj balls-removed position)))))))))

(defn ask-human-and-play [game]
  ; TODO factor this out in "winner-if-done"
  (let [board (:board game)]
    (if (game-over? board) 
      {:outcome (winner board)}
      (let [new-move               (ask-human-to-place-or-rise-ball game)
            new-move-without-balls (ask-human-to-remove-balls game new-move [])]
        {:next-game (create-next-game game (:move new-move-without-balls) (:board new-move-without-balls))}))))

(defn play-human-game [game human-color negamax-depth]
  (let [player        (:player game)
        human?        (= human-color player)
        play-function (if human? ask-human-and-play negamax)
        parameters    (if human? [game] [game negamax-depth])]
    (cons game
          (lazy-seq (let [game-result (apply play-function parameters)]
                      (if (:outcome game-result) []
                        (play-human-game (:next-game game-result) human-color negamax-depth)))))))

(defn play-human [size human-color first-player negamax-depth]
  (play-human-game (initial-game size first-player) human-color negamax-depth))

(defn play-negamax-game [game negamax-depth]
  (let [negamax-result (negamax game negamax-depth)]
    (cons game 
          (if (:outcome negamax-result) []
            (lazy-seq (play-negamax-game (:next-game negamax-result) negamax-depth))))))

(defn play-negamax [size first-player negamax-depth]
  (play-negamax-game (initial-game size first-player) negamax-depth))

(defn output [play]
  (into [] (map print-game play)))



