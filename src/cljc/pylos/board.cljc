(ns pylos.board)

(defn board-size [board]
  (:size (:helper-meta-board (meta board))))

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

(defn- has-new-full-square-at-square-position [board square-position position color]
  (let [square-corners                  (square-corners board square-position)
        square-corners-without-position (disj square-corners position)]
    (every? #(= color (cell board %)) square-corners-without-position)))

(defn new-full-square-position [board position color]
  "Checks whether the board would have a now full square if the
  board would be filled with a ball of the given color at the given position"
  (let [square-positions (square-positions-at-position board position)]
    (some #(when (has-new-full-square-at-square-position board % position color) %) square-positions)))

(defn removable-balls [board position]
  "Gives all balls that can be removed assuming the given position is filled."
  (let [removable-positions   (removable-positions board)
        ; we re-add the given position since it can be removed
        removable-positions   (conj removable-positions position)
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
  [board color position-with-ball]
  (let [removable-balls (removable-balls board position-with-ball)
        removable-balls (into #{} (filter #(contains? removable-balls %) (balls-on-board board color)))]
    ; we conj the given position since it can be removed
    (conj removable-balls position-with-ball)))

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
  (let [meta-infos                 (meta board)
        board-without-ball         (change-cell board position :open)
        square-positions-to-remove (squares-at-position board position)
        open-positions-to-remove   (map #(position-on-top board %) square-positions-to-remove)
        new-empty-positions        (-> (apply disj (:empty-positions meta-infos) open-positions-to-remove)
                                       (conj position))
        new-removable-positions    (removable-positions-below board-without-ball position)]
    (with-meta
      (reduce #(change-cell %1 %2 :no-acc :open) board-without-ball open-positions-to-remove)
      (assoc meta-infos
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
