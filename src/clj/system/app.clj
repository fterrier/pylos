(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go]]
            ; TODO remove these deps
            [game.output :refer [output-with-fn]]
            [game.game :refer [other-color]]
            [game.board :refer [serialize-board]]
            [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))

; output
(defn send-game-infos [chsk user-id board player move additional-infos time]
  ((:chsk-send! chsk) :sente/all-users-without-uid
                      [:pylos/game-infos
                       {:board (serialize-board board)
                        :next-player player
                        :move move
                        :time time
                        :additional-infos additional-infos}]))

(defn create-websocket-broadcast [chsk]
  (defn broadcast-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
    (send-game-infos chsk nil board player last-move additional-infos time)))

(defn output-websockets [play]
  (output-with-fn play (create-websocket-broadcast (:websockets system))))

; ; returns a channel that contains all events to that user-id
; (defn attach-websocket-ch [user-id]
;   (chan))

; handlers
(defmulti handle-event-msg (fn [id ?data control-ch] id))

(defmethod handle-event-msg :pylos/player-move [id data control-ch]
  (println data)
  (go (>! control-ch (:game-infos data))))

(defmethod handle-event-msg :default ; Fallback
  [id event control-ch]
  ;(println "Unhandled event:" id)
  )

(defn event-msg-handler* [control-ch]
  (fn [{:as ev-msg :keys [id ?data event]}]
    (handle-event-msg id ?data control-ch)))

; websockets routes
(defroutes pylos-routes
  ;;
  (GET  "/chsk"  req ((:ring-ajax-get-or-ws-handshake (:websockets system)) req))
  (POST "/chsk"  req ((:ring-ajax-post (:websockets system)) req))
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
