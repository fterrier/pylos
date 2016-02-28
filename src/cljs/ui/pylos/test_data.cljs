(ns ui.pylos.test-data
  (:require 
   [game.game :refer [make-move]]
   [pylos.game :refer [map->PylosGamePosition]]
   [pylos.board :refer [new-pylos-board]]
   [pylos.move :refer [move-add]]
   [pylos.ui :refer [game-infos-with-meta]]))

(defn init-game-position []
  (map->PylosGamePosition {:board   (new-pylos-board 4)
                           :player  :white
                           :outcome nil}))

(defn game-position-1 [player]
  (map->PylosGamePosition 
   {:board  (new-pylos-board [:black :white :black :open
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
    :outcome nil}))

(defn game-infos [game-position]
  (game-infos-with-meta {:game-position game-position}))

(def game-position-init (init-game-position))
(def game-position-init-2 (make-move game-position-init (move-add :white 4)))

(def game-infos-1 (game-infos game-position-init))
(def game-infos-2-black (game-infos (game-position-1 :black)))
(def game-infos-2-white (game-infos (game-position-1 :white)))

(def state-1 {:games {"LYlHISli" {:id "LYlHISli" 
                                  :past-game-infos [(game-infos game-position-init)
                                                    (game-infos game-position-init-2)]}}
              :current-game [:games "LYlHISli"]})






