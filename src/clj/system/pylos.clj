(ns system.pylos
  (:require [strategy.negamax :refer [negamax]]
            [clojure.core.async :as async :refer [chan go close!]]
            [system.strategy.websockets :refer [websockets]]
            [pylos.score :refer [score-middle-blocked]]
            [pylos.core :refer [play]]
            [game.output :refer [output-with-fn]]
            [game.game :refer [other-color]]))

; (defn play-websockets [size websockets-color first-player negamax-depth event-ch]
;   (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
;     (play size
;           {websockets-color (websockets event-ch)
;            (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
;           first-player)))

          ; input

; (defn output-websockets [play uid]
;   (output-with-fn play (create-websocket-broadcast (system-websockets) uid)))
;
; (defn play-websockets [size websockets-color first-player negamax-depth game-ch close-ch]
;   (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
;     (play size
;           {websockets-color (websockets game-ch close-ch)
;            (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
;           first-player)))

; (defn play-and-output [size websockets-color first-player negamax-depth uid]
;   (let [game-ch (new-game-ch (game-channels) uid)]
;     (output-websockets (play-websockets size websockets-color first-player negamax-depth game-ch nil) uid)
;     (close! game-ch)))

; (defn play-websockets-uid [size websockets-color first-player negamax-depth game-id]
;   (play-websockets size websockets-color first-player negamax-depth (new-game-ch (game-channels) game-id)))
