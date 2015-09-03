(ns game.game)

(defprotocol Game
  (board [this])
  (player [this])
  (next-game-positions [this])
  (outcome [this]))

(defprotocol Strategy
  (choose-next-move [this game-position] "Chooses the next move for the given game, returns a {:next-game-position :next-move} map"))

(defn play-game [{:keys [game-position] :as game} strategies]
  (let [player        (:player game-position)
        strategy      (get strategies player)]
    (cons game
          (if (:outcome game-position) []
            (lazy-seq 
              (let [start-time  (System/nanoTime)
                    game-result (choose-next-move strategy game-position)
                    end-time    (System/nanoTime)]
                (play-game {:game-position (:next-game-position game-result) 
                            :last-move (:next-move game-result)
                            :additional-infos (:additional-infos game-result)
                            :time (- end-time start-time)} strategies)))))))