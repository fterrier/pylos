(ns pylos.strategy.human
  (:require [game.game :refer :all]
            [clojure.string :as str]
            [pylos.board :refer :all]
            [pylos.game :refer :all]))

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
                 (nil? (ind board position)))
           (recur board text allow-enter)
           (ind board position))))))
  ([board text]
   (ask-for-position board text false)))

(defn ask-human-to-place-or-rise-ball [{:keys [board player] :as game-position}]
  (let [position (ask-for-position board "Please enter a valid position [layer row col]")]
    (if (has-ball board position)
      (if (not (can-remove-position? board player position))
        (do
          (println "That ball cannot be removed")
          (recur game-position))
        (let [high-position (ask-for-position board "Please enter a position for rise [layer row col]")]
          (if (not (and (can-remove-position? (add-ball board player high-position) player position)
                        (contains? (positions-under-position board high-position) position)))
            (do
              (println "Invalid move, we start again")
              (recur game-position))
            (move-rise player position high-position))))
      (if (not (can-add? board player position))
        (do
          (println "Invalid move, we start again")
          (recur game-position))
        (move-add player position)))))

(defn ask-human-to-remove-balls [{:keys [board player] :as game-position} {:keys [position] :as original-move} balls-removed new-board]
  (let [number-of-balls-removed (count balls-removed)]
    (if (nil? (new-full-square-position board position player))
      original-move
      (if (= 2 number-of-balls-removed)
        ; TODO fix
        (move-square original-move balls-removed )
        (let [position-to-remove (ask-for-position
                                   board
                                   (str "Please enter a ball to remove [layer row col]"
                                        (if (not= 0 number-of-balls-removed) " or <enter> to finish" ""))
                                   (not= 0 number-of-balls-removed))]
          (if (nil? position-to-remove)
            ; TODO fix
            (move-square original-move balls-removed)
            (if (not (can-remove-position? new-board player position-to-remove))
              (do
                (println "Cannot remove that ball")
                (recur game-position original-move balls-removed new-board))
              (ask-human-to-remove-balls game-position original-move
                                         (conj balls-removed position-to-remove)
                                         (remove-ball new-board player position-to-remove)))))))))

(defn ask-human-and-play [{:keys [board] :as game-position}]
  (let [new-move               (ask-human-to-place-or-rise-ball game-position)
        new-move-without-balls (ask-human-to-remove-balls game-position new-move [] (make-move-on-board board new-move))]
    {:next-move new-move-without-balls}))

(defrecord HumanStrategy []
  Strategy
  (choose-next-move [this game-position]
                    (ask-human-and-play game-position)))

(defn human []
  (->HumanStrategy))
