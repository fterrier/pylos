(ns pylos.game.game-state
  (:require [om.core :as om]
            [pylos.ui :refer [move-status highlight-status game-infos-with-meta]]))

; TODO redo this
(def initial-state-game {:game-infos []
                         :highlighted-position {:position nil} ; this contains the highlighted position
                         :current-move {:selections [] :playable-move nil :must-play-move false} 
                         :current-index []
                         :game-id nil})

(defn game-infos [game]
  (om/ref-cursor (:game-infos game)))

(defn current-index [game]
  (om/ref-cursor (:current-index game)))

(defn current-move [game]
  (om/ref-cursor (:current-move game)))

(defn highlighted-position [game]
  (om/ref-cursor (:highlighted-position game)))

(defn current-game-infos [game current-index]
  (let [game-infos (:game-infos game)]
    (if (empty? current-index) (last game-infos) (get game-infos (current-index 0)))))

(defn- get-highlights [game-infos current-selections]
  (get (:highlight-status game-infos) current-selections))

(defn- get-moves [game-infos current-selections]
  (:moves (get (:move-status game-infos) current-selections)))

(defn- playable-move [game-infos current-selections]
  (:playable-move (get (:move-status game-infos) current-selections)))

(defn position-info [game current-selections highlighted-position]
  (let [current-game-infos (current-game-infos game (:current-index game))]
    (merge
     ; all positions and the highlighted ones
     (get-highlights current-game-infos (conj current-selections :all))
     (get-highlights current-game-infos (conj current-selections (:position highlighted-position))))))

(defn moves-info [game current-selections highlighted-position]
  (let [current-game-infos (current-game-infos game (:current-index game))]
    (conj (get-moves current-game-infos (conj current-selections (:position highlighted-position)))
          (playable-move current-game-infos (conj current-selections (:position highlighted-position))))))

; change state
(defn append-game-infos [game game-infos]

  (let [game-infos-with-meta (game-infos-with-meta game-infos)]
    (println "Appending game infos with meta" game-infos-with-meta)
    (-> game 
        (assoc :current-move {:selections [] :playable-move nil :must-play-move false})
        (update :game-infos conj game-infos-with-meta))))

(defn append-past-game-infos [game past-game-infos]

  (let [past-game-infos-with-meta (into [] (map game-infos-with-meta past-game-infos))]
    (println past-game-infos-with-meta)
    (-> game
        (assoc :current-move {:selections [] :playable-move nil :must-play-move false})
        (update :game-infos #(into past-game-infos-with-meta %)))))

(defn- change-current [game key current-index]
  (if (nil? current-index)
    (assoc game key [])
    (assoc game key [current-index])))

(defn change-current-index [game current-index]
  (change-current game :current-index current-index))

(defn change-highlighted-position [game current-position]
  (assoc-in game [:highlighted-position :position] current-position))

(defn join-game [game game-id]
  (assoc initial-state-game :game-id game-id))

(defn select-current-position [game current-position]
  (let [current-index      (:current-index game)
        current-selections (:selections (:current-move game))
        current-game-infos (current-game-infos game (:current-index game))]
    ; TODO is it my turn to play ?
    ; this only works on the last game position
    (if-not (empty? current-index) game
      (let [new-selections (conj current-selections current-position)
            playable-move  (playable-move current-game-infos new-selections)
            possible-moves (get-moves current-game-infos new-selections)]
        (if (or playable-move (not (empty? possible-moves)))
          (-> game
              ; first update the current move
              (assoc-in [:current-move :selections] new-selections)
              (assoc-in [:current-move :must-play-move] (and playable-move (= 1 (count possible-moves))))
              (assoc-in [:current-move :playable-move] playable-move))
          game)))))
