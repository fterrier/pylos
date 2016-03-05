(ns pylos.serializer
  (:require [game.serializer :refer [GameSerializer]]
            [pylos.game :refer [map->PylosGamePosition]]
            [pylos.board :refer [board-size new-pylos-board]]))

;; TODO this is only used in websockets.clj, maybe no need for a protocol ?
(defrecord PylosSerializer []
  GameSerializer
  (deserialize-game-position [_ {:keys [size board player outcome 
                                        intermediate-board selected-positions]}]
    (let [game-position (map->PylosGamePosition {:board (new-pylos-board board size)
                                                 :player player
                                                 :outcome outcome})]
      (if (and intermediate-board selected-positions)
        (assoc game-position 
               :selected-positions selected-positions
               :intermediate-board (new-pylos-board intermediate-board size))
        game-position)))
  (serialize-game-position [_ {:keys [board player outcome 
                                      selected-positions intermediate-board]}]
    {:board (:board board) 
     :size (board-size board) 
     :player player 
     :outcome outcome
     :intermediate-board (:board intermediate-board)
     :selected-positions selected-positions}))

(defn new-pylos-serializer []
  (->PylosSerializer))
