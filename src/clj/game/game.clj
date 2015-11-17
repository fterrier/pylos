(ns game.game
  (:require [clojure.core.async :refer [<! >! go go-loop chan close!]]))

(defprotocol Game
  (board [this])
  (player [this])
  (outcome [this])
  (generate-moves [this])
  (make-move [this move]))

; (defprotocol PrettyPrint
;   (print-game [this]))

(defprotocol Strategy
  (choose-next-move [this game-position] "Returns a channel where the next move for the given game will be put, returns a {:next-move :additional-infos :next-game-position (optional)} object"))

(defn other-color [color]
  (if (= color :white) :black :white))

(defn play-game [{:keys [game-position] :as game} strategies]
  "Returns a channel where the consumer can ask for the next move"
  (let [player    (:player game-position)
        strategy  (get strategies player)
        result-ch (chan)]
      (go-loop [game-position    game-position
                time             0
                additional-infos nil
                last-move        nil]
                (if (:outcome game-position)
                  (close! result-ch)
                  (do
                    (>! result-ch
                        {:game-position game-position
                         :last-move last-move
                         :additional-infos additional-infos
                         :time time})
                    (let [start-time  (System/nanoTime)
                          game-result (<! (choose-next-move strategy game-position))
                          end-time    (System/nanoTime)
                          next-game-position (or (:next-game-position game-result) (make-move game-position (:next-move game-result)))]
                    (recur next-game-position (- end-time start-time) (:additional-infos game-result) (:next-move game-result))))))
      result-ch))
