(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go-loop]]
            [pylos.board :refer :all]
            [pylos.game :refer :all]
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

(defn send-game-infos [chsk user-id board player move additional-infos time]
  ((:chsk-send! chsk) :sente/all-users-without-uid
                      [:pylos/game-infos
                       {:board (transform-board board)
                        :balls-remaining {:white (balls-remaining board :white)
                                          :black (balls-remaining board :black)}
                        :next-player player
                        :move move
                        :time time
                        :additional-infos additional-infos}]))

(defn create-broadcast-game [chsk]
  (defn broadcast-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
    (send-game-infos chsk nil board player last-move additional-infos time)))

(defn event-msg-handler [test]
  ;(println "TODO got event")
  )

(defroutes pylos-routes
  ;;
  (GET  "/chsk"  req ((:ring-ajax-get-or-ws-handshake (:websockets system)) req))
  (POST "/chsk"  req ((:ring-ajax-post (:websockets system)) req))
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(def pylos-app
  (ring.middleware.defaults/wrap-defaults pylos-routes site-defaults))
