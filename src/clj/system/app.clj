(ns system.app
  (:require [clojure.core.async :refer [<! >! put! close! go-loop]]
            [ring.middleware.defaults :refer [site-defaults]]
            [system.system :refer [system]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [compojure.core :as comp :refer (defroutes GET POST)]))


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
