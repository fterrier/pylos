(ns pylos.repl
  (:require [pylos.pprint :refer [print-pylos-game]]
            [pylos.strategy.human :refer [human]]
            [strategy.negamax :refer [negamax]]
            [strategy.random :refer [random]]
            [game.output :refer [output-with-fn]]
            [game.play :refer [play]]
            [game.game :refer [other-color]]
            [pylos.game :refer [new-pylos-game]]
            [pylos.score :refer [score-middle-blocked]]
            [pylos.pprint :refer [print-pylos-game]]))

(defn output [play]
  (output-with-fn play print-pylos-game))

(defn play-negamax [game first-player negamax-depth]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play game {:black negamax-strategy :white negamax-strategy} first-player)))

(defn play-human [game human-color first-player negamax-depth]
  (play game {human-color (human) (other-color human-color) (negamax score-middle-blocked negamax-depth)} first-player))


(comment
  (output (play (new-pylos-game 4) {:white (negamax score-middle-blocked 6) :black (random)} :white)))

(comment
  (output (play-human (new-pylos-game 4) :white :black 8)))
