(ns game.board)

(defprotocol Board
  ;; TODO this makes no sense here
  ;; probably should be moved somewhere else
  ;; probably this should go to "Game" and de-serialize game positions
  (deserialize-board [this map] "Creates a board from a map")
  (serialize-board [this]))
