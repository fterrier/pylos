(ns game.strategy)

(defprotocol Strategy
  (choose-next-move [this game-position] "Returns a channel where the next move for the given game will be put, returns a {:next-move :additional-infos :next-game-position (optional)} object"))
