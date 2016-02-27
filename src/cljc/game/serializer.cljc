(ns game.serializer)

(defprotocol GameSerializer
  (deserialize-game-position [this map])
  (serialize-game-position [this game-position]))
