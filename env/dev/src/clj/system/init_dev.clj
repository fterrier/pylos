(ns system.init-dev
  (:require [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [system
             [figwheel :refer [figwheel-config map->Figwheel]]
             [system :refer [get-system-map]]]))

(set! *warn-on-reflection* true)

(def system)

(defn init []
  (alter-var-root #'system
                  (constantly (-> (get-system-map 8080)
                                  (assoc :figwheel (map->Figwheel figwheel-config))))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (repl/refresh :after 'system.init-dev/go))

(comment
  (reset)
)
