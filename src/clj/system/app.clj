(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go chan sub unsub]]
            [game.game :refer [other-color]]
            [game.board :refer [serialize-board]]
            [strategy.negamax :refer [negamax]]
            [system.system :refer [system]]
            [ring.util.response :refer [resource-response content-type]]
            [component.compojure :as ccompojure]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))

; TODO remove this get system stuff
(defn system-websockets []
  (:websockets system))

; handlers
(defmulti handle-event-msg (fn [id game-id ?data game-channels] id))

(defmethod handle-event-msg :pylos/new-game [id uid {:keys [first-player negamax-depth websockets-color]} websockets-ch]
  (println "Websockets got new game message" uid)
  (go (>! websockets-ch {:type :new-game :game-id "test" :first-player first-player :negamax-depth negamax-depth :websockets-color websockets-color})))

(defmethod handle-event-msg :pylos/player-move [id uid {:keys [game-infos]} websockets-ch]
  (println "Websockets got player move, sending to game runner" uid)
  (go (>! websockets-ch {:type :player-move :game-id uid :game-infos game-infos})))

(defmethod handle-event-msg :default ; Fallback
  [id uid event game-channels]
  (println "Unhandled event:" id uid)
  )

(defn event-msg-handler* [websockets-ch]
  (fn [{:as ev-msg :keys [id uid ?data event]}]
    (handle-event-msg id uid ?data websockets-ch)))

; websockets routes
(ccompojure/defroutes ServerRoutes [game-runner]
    ;;
    (GET  "/chsk/:game-id" req (
                                (try
                                  (:ring-ajax-get-or-ws-handshake (system-websockets))
                                  (catch Exception e
                                  ; do nothing
                                  )) req))
    (POST "/chsk/:game-id" req ((:ring-ajax-post (system-websockets)) req))
    (GET "/test"
                   [:as request
                    :as {deps :system-deps}
                    :as {{game-runner :game-runner} :system-deps}]
                   (println game-runner))
    ; (GET  "/pylos/:game-id" [game-id] (resource-response "index.html" {:root "public"}))
    (route/resources "/")
    ;;
    (route/not-found "<h1>Page not found</h1>"))

(defn new-server-routes []
  (map->ServerRoutes {}))
