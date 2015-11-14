(ns game.board)

(defprotocol Board
  (deserialize-board [this map] "Creates a board from a map")
  (serialize-board [this]))
