(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go-loop]]
            [pylos.board :refer :all]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))



; (defn broadcast [chsk]
;   ((:chsk-send! chsk) :sente/all-users-without-uid [:my-event/event "test"]))

(defn transform-board [board]
  (let [size           (size board)
        frontend-board (into [] (for [layer (range 0 size)]
                                  (into [] (for [row (range 0 (- size layer))]
                                             (into [] (for [col (range 0 (- size layer))]
                                                        (cell board (ind board [(inc layer) (inc row) (inc col)]))))))))]
    frontend-board))

(defn send-board [chsk user-id board]
  ((:chsk-send! chsk) :sente/all-users-without-uid [:pylos/board (transform-board board)]))

(defn create-broadcast-game [chsk]
  (defn broadcast-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
    (send-board chsk nil board)))

(defn event-msg-handler [test]
  (println "GOT EVENT")
  (println test))

(defroutes pylos-routes
  ;;
  (GET  "/chsk"  req ((:ring-ajax-get-or-ws-handshake (:websockets system)) req))
  (POST "/chsk"  req ((:ring-ajax-post (:websockets system)) req))
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
