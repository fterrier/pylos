(ns game.game)

(defprotocol Game
  (board [this])
  (player [this])
  (outcome [this])
  (generate-moves [this])
  (make-move [this move]))

(defprotocol PrettyPrint
  (print-game [this]))

(defprotocol Strategy
  (choose-next-move [this game-position] "Chooses the next move for the given game, returns a {:next-move :additional-infos :next-game-position (optional)} object"))

(defn other-color [color]
  (if (= color :white) :black :white))

(defn play-game [{:keys [game-position] :as game} strategies]
  (let [player        (:player game-position)
        strategy      (get strategies player)]
    (cons game
          (if (:outcome game-position) []
            (lazy-seq
              (let [start-time         (System/nanoTime)
                    print              (println "Player" player "to move")
                    game-result        (choose-next-move strategy game-position)
                    next-game-position (or (:next-game-position game-result) (make-move game-position (:next-move game-result)))
                    end-time           (System/nanoTime)]
                (play-game {:game-position next-game-position
                            :last-move (:next-move game-result)
                            :additional-infos (:additional-infos game-result)
                            :time (- end-time start-time)} strategies)))))))
