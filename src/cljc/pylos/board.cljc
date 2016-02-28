(ns pylos.board
  (:require [pylos.static :refer [create-static-board-helpers]]))

;; ================
;; Pylos board protocols 

(defrecord PylosBoard [board meta-board static-helpers])
(defrecord PylosMetaBoard [empty-positions balls-on-board removable-positions])

;; =================
;; private stuff
(declare square-corners cell removable-positions)

;; TODO move some of these to move.cljc ?
(defn- square-position-below [board position]
  ((:square-positions-below-map (:static-helpers board)) position))

(defn- positions-above-first-layer [board]
  (:positions-above-first-layer (:static-helpers board)))

(defn- square-positions-at-position [board position]
  "Returns a list of positions that are corners of squares
  containing the given position in the given board"
  ((:square-positions-at-position-map (:static-helpers board)) position))

(defn- has-new-full-square-at-square-position [board square-position position color]
  (let [square-corners                  (square-corners board square-position)
        square-corners-without-position (disj square-corners position)]
    (every? #(= color (cell board %)) square-corners-without-position)))

(defn- removable-balls [board position]
  "Gives all balls that can be removed assuming the given position is filled."
  (let [removable-positions   (removable-positions board)
        ; we re-add the given position since it can be removed
        removable-positions   (conj removable-positions position)
        square-position-below (square-position-below board position)]
    (if (nil? square-position-below)
      removable-positions
      (apply disj removable-positions (square-corners board square-position-below)))))

;; ================
;; public pylos board stuff

(defn board-size [board]
  (:size (:static-helpers board)))

(defn cell [board position]
  (get (:board board) position))

(defn empty-positions [board]
  (:empty-positions (:meta-board board)))

(defn number-of-balls-on-board [board]
  (apply + (map #(count (second %)) (:balls-on-board (:meta-board board)))))

(defn removable-positions [board]
  (:removable-positions (:meta-board board)))

(defn ind [board position]
  ((:positions-map (:static-helpers board)) position))

(defn number-of-positions-around [board]
  (:number-of-positions-around (:static-helpers board)))

(defn positions-around [board position direction]
  {:pre [(or (= :left-up direction) (= :right-down direction))]}
  (let [static-helpers       (:static-helpers board)
        positions-around-map (case direction
                               :left-up (:positions-left-up-map static-helpers)
                               :right-down (:positions-right-down-map static-helpers))]
    (positions-around-map position)))

(defn position-on-top [board position]
  ((:position-on-top-map (:static-helpers board)) position))

(defn positions-under-position [board position]
  ((:positions-under-position-map (:static-helpers board)) position))

(defn square-corners [board position]
  (positions-around board position :right-down))

(defn number-of-positions [board]
  (:number-of-positions (:static-helpers board)))

; Starting functions that access the state
(defn balls-on-board [board color]
  (color (:balls-on-board (:meta-board board))))

(defn balls-remaining [board color]
  (let [number-of-balls (/ (number-of-positions board) 2)]
    (- number-of-balls (count (balls-on-board board color)))))

(defn has-ball [board position]
  (let [cell (cell board position)]
    (or (= :black cell) (= :white cell))))

(defn can-remove-ball [board position]
  (contains? (removable-positions board) position))

(defn new-full-square-position [board position color]
  "Checks whether the board would have a now full square if the
  board would be filled with a ball of the given color at the given position"
  (let [square-positions (square-positions-at-position board position)]
    (some #(when (has-new-full-square-at-square-position board % position color) %) square-positions)))

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
  [board color position-with-ball]
  (let [removable-balls (removable-balls board position-with-ball)
        removable-balls (into #{} (filter #(contains? removable-balls %) (balls-on-board board color)))]
    ; we conj the given position since it can be removed
    (conj removable-balls position-with-ball)))

(defn change-cell
  ([board position new-cell]
   "Changes a cell to a new cell"
   (assoc-in board [:board position] new-cell))
  ([partial-board partial-position new-cell cell-to-replace]
   "Changes a cell to a new cell only if the cell contains cell-to-replace"
   (if (= cell-to-replace (cell partial-board partial-position))
     (change-cell partial-board partial-position new-cell)
     partial-board)))

(defn has-square [board position]
  (let [positions-to-check (square-corners board position)]
    (and (= 4 (count positions-to-check))
         (every? #(has-ball board %) positions-to-check))))

(defn squares-at-position [board position]
  (let [positions-to-try (square-positions-at-position board position)]
    (into #{} (filter #(has-square board %) positions-to-try))))

(defn has-balls-to-play [board color]
  (let [balls-per-player       (/ (number-of-positions board) 2)
        balls-of-color-in-play (count (balls-on-board board color))]
    (not (= balls-per-player balls-of-color-in-play))))

(defn add-ball [board color position]
  {:pre [(= :open (cell board position))
         (has-balls-to-play board color)]}
  (let [meta-infos                    (:meta-board board)
        board-with-ball               (change-cell board position color)
        new-square-positions          (squares-at-position board-with-ball position)
        new-open-positions            (map #(position-on-top board %) new-square-positions)
        new-empty-positions           (-> (apply conj (:empty-positions meta-infos) new-open-positions)
                                          (disj position))
        position-below                (square-position-below board position)
        removable-positions-to-remove (if-not (nil? position-below) (square-corners board-with-ball position-below) [])]
    (assoc (reduce #(change-cell %1 %2 :open :no-acc) board-with-ball new-open-positions)
           :meta-board (assoc meta-infos
                              :empty-positions new-empty-positions
                              :removable-positions (-> (apply disj (:removable-positions meta-infos) removable-positions-to-remove)
                                                       (conj position))
                              :balls-on-board (update (:balls-on-board meta-infos) color #(conj % position))))))

(defn removable-positions-below [board position]
  "Gives the positions immediately below the given positions that can be removed
  given that the given position is not any more filled on the board"
  (let [position-below             (square-position-below board position)
        new-removable-positions    (if-not (nil? position-below)
                                     (remove (fn [position]
                                               (let [positions-to-try (square-positions-at-position board position)]
                                                 (some #(has-ball board (position-on-top board %)) positions-to-try)))
                                             (square-corners board position-below)) [])]
    new-removable-positions))

(defn removable-positions-of-color-below [board position color]
  (let [removable-positions-below (removable-positions-below board position)
        balls-of-color            (balls-on-board board color)]
    (filter #(contains? balls-of-color %) removable-positions-below)))

(defn remove-ball [board color position]
  {:pre [(= color (cell board position))
         (can-remove-ball board position)]}
  (let [meta-infos                 (:meta-board board)
        board-without-ball         (change-cell board position :open)
        square-positions-to-remove (squares-at-position board position)
        open-positions-to-remove   (map #(position-on-top board %) square-positions-to-remove)
        new-empty-positions        (-> (apply disj (:empty-positions meta-infos) open-positions-to-remove)
                                       (conj position))
        new-removable-positions    (removable-positions-below board-without-ball position)]
    (assoc (reduce #(change-cell %1 %2 :no-acc :open) board-without-ball open-positions-to-remove)
           :meta-board (assoc meta-infos
                              :empty-positions new-empty-positions
                              :removable-positions (-> (apply conj (:removable-positions meta-infos) new-removable-positions)
                                                        (disj position))
                              :balls-on-board (update (:balls-on-board meta-infos) color #(disj % position))))))

(defn can-remove-position? [board color position]
  (and (can-remove-ball board position)
       (= color (cell board position))))

(defn can-add-position? [board color position]
  "Checks if the given add can be done, given that the move has not been
  generated on the board."
  (= :open (cell board position)))

;; =======================
;; Pylos board creation and visitors - PRIVATE

(defn- retrieve-empty-positions [board]
  (into #{} (filter #(= :open (cell board %)) (range 0 (count (:board board))))))

(defn- retrieve-balls-on-board [board color]
  (into #{} (filter #(= color (cell board %)) (range 0 (count (:board board))))))

(defn- can-remove-ball-no-meta [board position]
  (and (has-ball board position)
       (every? #(not (has-ball board (position-on-top board %))) (positions-around board position :left-up))))

(defn- retrieve-removable-positions [board]
  (into #{} (filter #(can-remove-ball-no-meta board %) (range 0 (count (:board board))))))

(defn- create-meta-board [board]
  {:empty-positions (retrieve-empty-positions board)
   :balls-on-board  {:black (retrieve-balls-on-board board :black)
                     :white (retrieve-balls-on-board board :white)}
   :removable-positions (retrieve-removable-positions board)})

(defn- create-board 
  ([board size static-board-helpers]
   (let [board-with-static {:board board :static-helpers static-board-helpers}
         meta-board        (create-meta-board board-with-static)]
     (map->PylosBoard {:static-helpers static-board-helpers
                       :board board
                       :meta-board meta-board}))))

(defn- create-initial-board-vector [size number-of-positions]
 (into [] (map (fn [ind] (if (< ind (* size size)) :open :no-acc)) 
               (range number-of-positions))))

;; ========================
;; Pylos board creation and visitors public interface

(defn new-pylos-board
  ([board size]
   "Create all meta data for a board and attach to it"
   (create-board board size (create-static-board-helpers size)))
  ([size]
   "Creates a new board with initial position"
   (let [static-board-helpers (create-static-board-helpers size)
         board (create-initial-board-vector 
                size (:number-of-positions static-board-helpers))]
     (create-board board size static-board-helpers))))

(defn visit-board [board visit-fn]
  (let [size           (board-size board)
        positions-map  (:positions-map (:static-helpers board))]
    (into [] 
          (for [layer (range 0 size)]
            (into [] 
                  (for [row (range 0 (- size layer))]
                    (into [] 
                          (for [col (range 0 (- size layer))]
                            (visit-fn [layer row col] (get positions-map [(inc layer) (inc row) (inc col)]))))))))))
