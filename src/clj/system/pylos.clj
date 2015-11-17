(ns system.pylos
  (:require [strategy.negamax :refer [negamax]]
            [clojure.core.async :as async :refer [chan go]]
            [system.app :refer [output-websockets create-websocket-broadcast event-channels new-game-ch delete-game-ch]]
            [system.strategy.websockets :refer [websockets]]
            [pylos.score :refer [score-middle-blocked]]
            [pylos.core :refer [play]]
            [game.game :refer [other-color]]))

; (defn play-websockets [size websockets-color first-player negamax-depth event-ch]
;   (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
;     (play size
;           {websockets-color (websockets event-ch)
;            (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
;           first-player)))

          ; input

(defn play-websockets [size websockets-color first-player negamax-depth game-ch close-ch]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play size
          {websockets-color (websockets game-ch close-ch)
           (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
          first-player)))

(defn play-and-output [size websockets-color first-player negamax-depth game-ch output-uid]
  (output-websockets (play-websockets size websockets-color first-player negamax-depth game-ch nil) output-uid))

; (defn play-websockets-uid [size websockets-color first-player negamax-depth game-id]
;   (play-websockets size websockets-color first-player negamax-depth (new-game-ch (event-channels) game-id)))
