(ns pylos.human
  (:require [game.game :refer :all]
            [clojure.string :as str]
            [pylos.game :refer :all]
            [pylos.board :refer :all]))

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
      (if (not (can-remove-positions? board player [position]))
        (do 
          (println "That ball cannot be removed")
          (recur game-position))
        (let [high-position (ask-for-position board "Please enter a position for rise [layer row col]")]
          (if (not (can-rise? board player position high-position))
            (do 
              (println "Invalid move, we start again")
              (recur game-position))
            (move-rise board player position high-position))))
      (if (not (can-add? board player position))
        (do 
          (println "Invalid move, we start again")
          (recur game-position))
        (move-add board player position)))))

(defn ask-human-to-remove-balls [game-position {:keys [board move] :as original-move} balls-removed]
  (let [old-board               (:board game-position)
        player                  (:player game-position)
        number-of-balls-removed (count balls-removed)]
    (if (not (has-new-full-square board old-board player))
      {:board board :move move}
      (if (= 2 number-of-balls-removed)
        (move-square original-move balls-removed)
        (let [position (ask-for-position 
                         board 
                         (str "Please enter a ball to remove [layer row col]" 
                              (if (not= 0 number-of-balls-removed) " or <enter> to finish" ""))
                         (not= 0 number-of-balls-removed))]
          (if (nil? position)
            (move-square original-move balls-removed)
            (if (not (can-remove-positions? board player [position]))
              (do 
                (println "Cannot remove that ball")
                (recur game-position {:board board :move move} balls-removed))
              (ask-human-to-remove-balls game-position {:board board :move move} (conj balls-removed position)))))))))

(defn ask-human-and-play [{:keys [board] :as game-position}]  
  (let [new-move               (ask-human-to-place-or-rise-ball game-position)
        new-move-without-balls (ask-human-to-remove-balls game-position new-move [])
        next-game              (next-game-position game-position new-move-without-balls)]
    {:next-game-position (:game-position next-game)
     :next-move (:move next-game)}))

(defrecord HumanStrategy []
  Strategy
  (choose-next-move [this game-position]
                    (ask-human-and-play game-position)))

(defn human []
  (->HumanStrategy))
