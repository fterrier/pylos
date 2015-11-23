(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go chan sub unsub]]
            [game.game :refer [other-color]]
            [game.board :refer [serialize-board]]
            [strategy.negamax :refer [negamax]]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [ring.util.response :refer [resource-response content-type]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))

; TODO remove this get system stuff
(defn system-websockets []
  (:websockets system))

; handlers
(defmulti handle-event-msg (fn [id game-id ?data game-channels] id))

; (defmethod handle-event-msg :pylos/new-game [id game-id {:keys [game-infos]} websockets-ch]
;   (println "Got client event, sending infos to game-id" game-id)
;   (go (>! websockets-ch {:type :player-move :game-id game-id :game-infos game-infos})))

(defmethod handle-event-msg :pylos/player-move [id game-id {:keys [game-infos]} websockets-ch]
  (println "Websockets got player move, sending to game runner" game-id)
  (go (>! websockets-ch {:type :player-move :game-id game-id :game-infos game-infos})))

(defmethod handle-event-msg :default ; Fallback
  [id game-id event game-channels]
  ;(println "Unhandled event:" id game-id)
  )

(defn event-msg-handler* [websockets-ch]
  (fn [{:as ev-msg :keys [id uid ?data event]}]
    (handle-event-msg id uid ?data websockets-ch)))

; websockets routes
(defroutes pylos-routes
  ;;
  (GET  "/chsk/:game-id" req (
                              (try
                                (:ring-ajax-get-or-ws-handshake (system-websockets))
                                (catch Exception e
                                ; do nothing
                                )) req))
  (POST "/chsk/:game-id" req ((:ring-ajax-post (system-websockets)) req))
  ; (GET  "/pylos/:game-id" [game-id] (resource-response "index.html" {:root "public"}))
  (route/resources "/")
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
