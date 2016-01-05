(ns game.strategy)


(defprotocol Strategy
  (choose-next-move [this game-position] "Returns a channel where the next move for the given game will be put, returns a {:next-move :additional-infos :next-game-position (optional)} object")
  (get-input-channel [this])
  (notify-end-game [this] "Called when a game is over, gives a chance for the strategy to cleanup channels and other resources"))
