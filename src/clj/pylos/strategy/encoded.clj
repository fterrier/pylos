(ns pylos.strategy.encoded
  (:require [clojure.core.async :refer [<! chan go-loop close!]]
            [clojure.tools.logging :as log]
            [game.strategy :refer [Strategy]]
            [pylos
             [move :refer [generate-all-moves]]
             [ui :refer [move-status]]]))

(defn choose-move-with-context [game-ch game-position]
  (go-loop [[game-ch {:keys [board selected-positions player] :as game-position}]
            [game-ch game-position]]
    (let [selected-positions (or selected-positions [])
          user-input         (<! game-ch)
          move-status        (move-status game-position (generate-all-moves game-position))
          current-move       (get move-status selected-positions)]
      (log/debug "Got user input" user-input)
      (let [user-input (if (sequential? user-input) user-input [user-input])
            is-done    (= :done (last user-input))
            user-input (if is-done (butlast user-input) user-input)]
        (log/debug "Sanitized user input, is done" user-input is-done)
        (let [new-selected-positions (into [] (concat selected-positions user-input))
              new-move               (get move-status new-selected-positions)]
          (log/debug "Found new move" user-input new-move)
          (if (nil? new-move)
            ;; we try to get a valid move
            (recur [game-ch game-position selected-positions])
            (if (and (or is-done (= 1 (count (:moves new-move))))
                     (:playable-move new-move))
              ;; we play this 1 playable move that we found
              {:next-move (:playable-move new-move)}
              ;; we send that game position again with the move selected
              {:next-game-position 
               (assoc game-position 
                      :selected-positions new-selected-positions
                      :intermediate-board ((:intermediate-board new-move)))})))))))

(defrecord EncodedStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position] 
    (choose-move-with-context game-ch game-position))
  (notify-end-game [this]
    (close! game-ch))
  (get-input-channel [this] game-ch))

;; (send-user-input [this input])
;; (notify-end-game [this)

(defn encoded []
  (->EncodedStrategy (chan)))
