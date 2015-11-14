(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go chan sub unsub]]
            [game.output :refer [output-with-fn]]
            [game.game :refer [other-color]]
            [game.board :refer [serialize-board]]
            [strategy.negamax :refer [negamax]]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [ring.util.response :refer [resource-response content-type]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))

; get system stuff
(defn system-websockets []
  (:websockets system))

(defn event-channels []
  (:event-channels system))

; output
(defn send-game-infos [websockets uid board player move additional-infos time]
  (println "sending infos to " uid board (:chsk-send! websockets))
  ((:chsk-send! websockets) uid
                      [:pylos/game-infos
                       {:board (serialize-board board)
                        :next-player player
                        :move move
                        :time time
                        :additional-infos additional-infos}]))

(defn create-websocket-broadcast [websockets uid]
  (defn broadcast-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
    (send-game-infos websockets uid board player last-move additional-infos time)))

(defn output-websockets [play uid]
  (output-with-fn play (create-websocket-broadcast (system-websockets) uid)))

; communication
(defn new-game-ch [event-channels game-id]
  (println "creating game channel" game-id)
  (let [game-ch (chan)]
    (sub (:pub-ch event-channels) game-id game-ch)
    game-ch))

(defn delete-game-ch [event-channels game-id game-ch]
  (println "deleting game channel" game-id)
  (unsub (:pub-ch event-channels) game-id game-ch)
  (close! game-ch))

(defn send-to-game-ch [event-channels game-id game-infos]
  (go (>! (:event-ch event-channels) {(:topic-fn event-channels) game-id :game-infos game-infos})))


; handlers
(defmulti handle-event-msg (fn [id game-id ?data event-channels] id))

(defmethod handle-event-msg :pylos/player-move [id game-id {:keys [game-infos]} event-channels]
  (println "Got client event, sending infos to game-id" game-id "on channel" event-channels)
  (send-to-game-ch event-channels game-id game-infos))

(defmethod handle-event-msg :default ; Fallback
  [id game-id event event-channels]
  ;(println "Unhandled event:" id game-id)
  )

(defn event-msg-handler* [event-channels]
  (fn [{:as ev-msg :keys [id uid ?data event]}]
    (handle-event-msg id uid ?data event-channels)))

; websockets routes
(defroutes pylos-routes
  ;;
  (GET  "/chsk/:game-id" req ((:ring-ajax-get-or-ws-handshake (system-websockets)) req))
  (POST "/chsk/:game-id" req ((:ring-ajax-post (system-websockets)) req))
  ; (GET  "/pylos/:game-id" [game-id] (resource-response "index.html" {:root "public"}))
  (route/resources "/")
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
