(ns game.ui)

;; TODO this might become an abstraction once we start implementing chess or other games
;; unused for now

(defprotocol UIGamePosition
  (score [this])
  (move-status [this])
  (highlight-status [this]))
