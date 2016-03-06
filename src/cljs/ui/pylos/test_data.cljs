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

(defn game-infos [game-position index]
  (assoc (game-infos-with-meta {:game-position game-position})
         :index index))

(def game-position-init (init-game-position))
(def game-position-init-2 (make-move game-position-init (move-add :white 4)))

(def game-infos-1 (game-infos game-position-init 0))
(def game-infos-2-black (game-infos (game-position-1 :black) 0))
(def game-infos-2-white (game-infos (game-position-1 :white) 0))

(def state-1 {:root {:current-game nil}})

(def normalized-state-1 {:games {"LYlHISli" {:id "LYlHISli" 
                                             :past-game-infos [(game-infos game-position-init   0)
                                                               (game-infos game-position-init-2 1)]
                                             :current-game-infos (game-infos game-position-init 0)}}
                         :root {:current-game {:game [:games "LYlHISli"]
                                               :current-selections []
                                               :selected-index nil}}})








