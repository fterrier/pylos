(ns system.routes
  (:require [system.game :refer [channel-stats]]
            [ring.util.response :refer [resource-response content-type]]
            [component.compojure :as ccompojure]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response]]
            [compojure.core :as comp :refer (defroutes GET POST)]))

; websockets routes
(ccompojure/defroutes ServerRoutes [game-runner websockets]
  ;;
  (GET  "/chsk/:game-id" [:as request
                          :as {{:keys [websockets]} :system-deps}] (
                              (try
                                (:ring-ajax-get-or-ws-handshake websockets)
                                (catch Exception e
                                ; do nothing
                                )) request))
  (POST "/chsk/:game-id" [:as request
                          :as {{:keys [websockets]} :system-deps}] ((:ring-ajax-post websockets) request))
  (wrap-json-response
    (GET "/test"   [:as request
                    :as {{:keys [game-runner]} :system-deps}]
                   {:body (channel-stats game-runner)}))
  ; (GET  "/pylos/:game-id" [game-id] (resource-response "index.html" {:root "public"}))
  (route/resources "/")
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(defn new-server-routes []
  (map->ServerRoutes {}))
