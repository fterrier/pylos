(ns system.routes
  (:require [hiccup.page :refer [include-css html5]]
            [game.view :refer [include-javascript]]
            [compojure
             [core :as comp :refer [GET POST]]
             [route :as route]]
            [component.compojure :as ccompojure]
            [ring.middleware.json :refer [wrap-json-response]]
            [system.game :refer [channel-stats]]))

(defn- content-page [content]
  (let [description ""
        lang        "en"
        title       "Pylos!"]
    (html5 {:lang lang}
           [:head {:profile "http://www.w3.org/2005/10/profile"}
            [:meta {:charset "UTF-8"}]
            [:meta {:http-equiv "Content-Type"    :content "text/html;charset=utf-8"}]
            [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
            [:meta {:name "language" :content lang}]
            [:meta {:name "og:locale" :content lang}]

            [:meta {:name "description" :content description}]
            [:meta {:name "og:description" :content description}]

            [:title "Pylos!"]
            [:meta {:name "og:title" :content title}]
            ;[:meta {:name "og:url" :content "http://www.timeforcoffee.ch/"}]

            [:link {:rel "icon" :type "image/png" :href "/favicon.png"}]

            (include-css "/css/styles.css")]
           (into
             [:body content]
             (include-javascript)))))

(defn index-page []
  (content-page [:div {:id "main-area"}]))

; websockets routes
(ccompojure/defroutes ServerRoutes [game-runner websockets]
  ;;
  (GET  "/chsk" [:as request
                 :as {{:keys [websockets]} :system-deps}] (
                     (try
                       (:ring-ajax-get-or-ws-handshake websockets)
                       (catch Exception e
                       ; do nothing
                           )) request))
  (POST "/chsk" [:as request
                 :as {{:keys [websockets]} :system-deps}] ((:ring-ajax-post websockets) request))
  (wrap-json-response
    (GET "/test"   [:as request
                    :as {{:keys [game-runner]} :system-deps}]
                   {:body (channel-stats game-runner)}))
  ; (GET  "/pylos/:game-id" [game-id] (resource-response "index.html" {:root "public"}))

  (GET "/"      [] (index-page))
  (route/resources "/")
  ;;
  (route/not-found "<h1>Page not found</h1>"))

(defn new-server-routes []
  (map->ServerRoutes {}))
