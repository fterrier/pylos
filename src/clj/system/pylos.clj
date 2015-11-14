(ns system.pylos
  (:require [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]
            [pylos.score :refer [score-middle-blocked]]
            [pylos.core :refer [play]]
            [game.game :refer [other-color]]))

(defn play-websockets [size websockets-color first-player negamax-depth event-ch]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play size
          {websockets-color (websockets event-ch)
           (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
          first-player)))
