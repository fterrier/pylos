(ns pylos.game.state
  (:require [cljs.core.async :as async :refer [put! >! <! chan close!]]
            [om.core :as om]
            [game.board :refer [serialize-board]]
            [pylos.game :refer [generate-all-moves]]
            [pylos.ui :refer [highlight-status move-status]]
            [pylos.board :refer [board-size ind balls-remaining]]
            [pylos.init :refer [create-board starting-board board-indexes initialize-board-meta]]))

(defn game-infos-with-meta [game-infos]
  (let [board-with-meta  (create-board (:board game-infos))
        next-player      (:next-player game-infos)
        possible-moves   (generate-all-moves {:board board-with-meta :player next-player})
        ; TODO don't do that if it's not our turn to play
        highlight-status (highlight-status board-with-meta possible-moves)
        move-status      (move-status possible-moves)
        layered-board    (board-indexes board-with-meta)
        balls-remaining  {:white (balls-remaining board-with-meta :white)
                          :black (balls-remaining board-with-meta :black)}
        game-infos       (assoc game-infos :board board-with-meta :layered-board layered-board :highlight-status highlight-status :move-status move-status :balls-remaining balls-remaining)]
    game-infos))

(def no-move {:selections [] :can-play-move false}) ; this vector contains [third-position-selected second-position-selected first-position-selected]

(def initial-state {:game-infos [(game-infos-with-meta
                                  {:size 4
                                   :board (serialize-board (starting-board 4))
                                   :next-player :white})]
                    :highlighted-position {:position nil} ; this contains the highlighted position
                    :current-move no-move
                    :current-index []})

; state
(defonce app-state
  (atom initial-state))

(defonce app-channels
  (atom {}))

; get state
(defn game-infos []
  (om/ref-cursor (:game-infos (om/root-cursor app-state))))

(defn current-index []
  (om/ref-cursor (:current-index (om/root-cursor app-state))))

(defn current-move []
  (om/ref-cursor (:current-move (om/root-cursor app-state))))

(defn highlighted-position []
  (om/ref-cursor (:highlighted-position (om/root-cursor app-state))))

(defn highlighted-info [position]
  (om/ref-cursor (position (:highlighted-positions (om/root-cursor app-state)))))

(defn current-game-infos [current-index]
  (let [game-infos (:game-infos (om/root-cursor app-state))]
    (if (empty? current-index) (last game-infos) (get game-infos (current-index 0)))))

(defn moves-by-position [current-position]
  (let [current-game-infos (current-game-infos (:current-index (om/root-cursor app-state)))]
    (get (:possible-moves current-game-infos) current-position)))

(defn- get-highlights [game-infos current-selections]
  (get (:highlight-status game-infos) current-selections))

(defn- get-moves [game-infos current-selections]
  (:moves (get (:move-status game-infos) current-selections)))

(defn- playable-move [game-infos current-selections]
  (:playable-move (get (:move-status game-infos) current-selections)))

(defn position-info [current-selections highlighted-position]
  (let [current-game-infos (current-game-infos (:current-index (om/root-cursor app-state)))]
    (merge
     ; all positions and the highlighted ones
     (get-highlights current-game-infos (conj current-selections :all))
     (get-highlights current-game-infos (conj current-selections (:position highlighted-position))))))

(defn moves-info [current-selections highlighted-position]
  (let [current-game-infos (current-game-infos (:current-index (om/root-cursor app-state)))]
    (conj (get-moves current-game-infos (conj current-selections (:position highlighted-position)))
          (playable-move current-game-infos (conj current-selections (:position highlighted-position))))))

; change state
; TODO remove app from signatures here
(defn append-game-infos [app game-infos]
  (let [game-infos-with-meta (game-infos-with-meta game-infos)]
    (om/update! app :current-move no-move)
    (om/transact! app :game-infos #(conj % game-infos-with-meta))))

(defn- change-current [app key current-index]
  (if (nil? current-index)
    (om/update! app key [])
    (om/update! app key [current-index])))

(defn change-current-index [app current-index]
  (change-current app :current-index current-index))

(defn change-highlighted-position [app current-position]
  (om/update! app [:highlighted-position :position] current-position))

(defn play-move [board move control-ch]
  (put! control-ch {:action :send-move-to-server :game-infos {:board board :size (board-size board) :move move}}))

(defn play-current-move [app control-ch]
  (let [current-game-infos (current-game-infos (:current-index (om/root-cursor app-state)))
        current-selections (:selections (:current-move (om/root-cursor app-state)))
        playable-move      (playable-move current-game-infos current-selections)]
    (when playable-move (play-move (:board current-game-infos) playable-move control-ch))))

(defn select-current-position [app control-ch current-position]
  (let [current-index      (:current-index (om/root-cursor app-state))
        current-selections (:selections (:current-move (om/root-cursor app-state)))
        current-game-infos (current-game-infos (:current-index (om/root-cursor app-state)))]
    ; TODO is it my turn to play ?
    ; this only works on the last game position
    (if-not (empty? current-index)
      (println "Not your turn to play")
      (let [new-selections (conj current-selections current-position)
            playable-move  (playable-move current-game-infos new-selections)
            possible-moves (get-moves current-game-infos new-selections)]
        (when playable-move (println "FOUND PLAYABLE MOVE" playable-move))
        (when (or playable-move (not (empty? possible-moves)))
          (om/update! app [:current-move :selections] new-selections) ; we first update the current move
          (om/update! app [:current-move :can-play-move] (some? playable-move))
          (when (and playable-move (= 1 (count possible-moves))) (play-move (:board current-game-infos) playable-move control-ch)))))))

; convenience stuff for development
(defn unselect-move []
  (swap! app-state (fn [state] (assoc state :current-move no-move))))

(defn regen-game-infos-state []
  (swap! app-state (fn [state] (update state :game-infos #(into [] (map game-infos-with-meta %))))))

(defn reset []
  (reset! app-state initial-state))
