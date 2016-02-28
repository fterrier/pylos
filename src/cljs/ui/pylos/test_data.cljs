(ns ui.pylos.test-data
  (:require [pylos.init :refer [starting-board]]
            [pylos.ui :refer [game-infos-with-meta]]
            [pylos.init :refer [initialize-board-meta]]))

(defn init-game-position []
  {:board   (starting-board 4)
   :player  :white
   :outcome nil})

(defn game-position-1 [player]
  {:board  (initialize-board-meta [:black :white :black :open
                                   :open :white :white :open
                                   :open :white :open :open
                                   :open :open :black :open
                                   :no-acc :open :no-acc
                                   :no-acc :no-acc :no-acc
                                   :no-acc :no-acc :no-acc
                                   :no-acc :no-acc
                                   :no-acc :no-acc
                                   :no-acc] 4)
   :player player
   :outcome nil})

(def game-infos-1 (game-infos-with-meta {:game-position (init-game-position)}))
(def game-infos-2-black (game-infos-with-meta {:game-position (game-position-1 :black)}))
(def game-infos-2-white (game-infos-with-meta {:game-position (game-position-1 :white)}))

(def state-1 {:games {"aNJQakPq" {:id "aNJQakPq" :past-game-infos [game-infos-1]}}
              :current-game [:games "aNJQakPq"]})






