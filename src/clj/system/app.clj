(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go-loop]]
            [pylos.board :refer :all]
            [pylos.game :refer :all]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))


(defn send-game-infos [chsk user-id board player move additional-infos time]
  ((:chsk-send! chsk) :sente/all-users-without-uid
                      [:pylos/game-infos
                       {:board board
                        :size (size board)
                        :next-player player
                        :move move
                        :time time
                        :additional-infos additional-infos}]))

(defn create-websocket-broadcast [chsk]
  (defn broadcast-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
    (send-game-infos chsk nil board player last-move additional-infos time)))

(defmulti handle-event-msg (fn [id ?data] id))

(defmethod handle-event-msg :pylos/player-move [id data]
  (println data))

(defmethod handle-event-msg :default ; Fallback
  [id event]
  (println "Unhandled event:" event))

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (println "Event:" ev-msg)
  (handle-event-msg id ?data))

(defroutes pylos-routes
  ;;
  (GET  "/chsk"  req ((:ring-ajax-get-or-ws-handshake (:websockets system)) req))
  (POST "/chsk"  req ((:ring-ajax-post (:websockets system)) req))
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
