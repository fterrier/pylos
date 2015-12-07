(ns game.play
  (:require [clojure.core.async :refer [<! >! go go-loop chan close!]]
            [game.strategy :refer [choose-next-move]]
            [game.game :refer [make-move initial-game-position]]))

(defn- play-game
  ([game strategies]
    "Returns a channel where the consumer can ask for the next move"
    (let [result-ch (chan)]
        (play-game game strategies result-ch)
        result-ch))
  ([{:keys [game-position] :as game} strategies result-ch]
   (go-loop [game-position    game-position
             time             0
             additional-infos nil
             last-move        nil]
     (let [player    (:player game-position)
           strategy  (get strategies player)]
       (>! result-ch
           {:game-position game-position
            :last-move last-move
            :additional-infos additional-infos
            :time time})
       (if (:outcome game-position)
         ; TODO do not close this ?
         (close! result-ch)
         (let [start-time  (System/nanoTime)
               ; TODO provide a way to exit from this
               game-result (<! (choose-next-move strategy game-position))
               end-time    (System/nanoTime)
               next-game-position (or (:next-game-position game-result) (make-move game-position (:next-move game-result)))]
           (recur next-game-position (- end-time start-time) (:additional-infos game-result) (:next-move game-result))))))))

(defn play
  ([game {:keys [white black] :as strategies} first-player result-ch]
    (play-game {:game-position (initial-game-position game first-player)} strategies result-ch))
  ([game {:keys [white black] :as strategies} first-player]
    (play-game {:game-position (initial-game-position game first-player)} strategies)))
