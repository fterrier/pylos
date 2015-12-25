(ns system.server
  (:require
   [org.httpkit.server :refer [run-server]]
   [clojure.tools.logging :as log]
   [ring.middleware.defaults :refer [site-defaults api-defaults]]
   [com.stuartsierra.component :as component]))

(defn get-routes [routes]
  (ring.middleware.defaults/wrap-defaults routes api-defaults))

(defrecord WebServer [port routes]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (log/info ";; Starting server on port" port)
    ;; In the 'start' method, initialize this component
    ;; and start it running. For example, connect to a
    ;; database, create thread pools, or initialize shared
    ;; state.
    (assoc component :server (run-server
                              (get-routes (:routes routes))
                              {:port port :join? false})))

  (stop [component]
    (log/info ";; Stopping server")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    ((:server component) :timeout 100)
    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (assoc component :server nil)))

; (def allowed-opts
;   [:ip :port :thread :worker-name-prefix :queue-size :max-body :max-line])

(defn new-web-server
  ([port] (map->WebServer {:port port}))
  ([port routes]
   (map->WebServer {:port port :routes routes})))
