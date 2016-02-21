(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [system
             [figwheel :refer [figwheel-config map->Figwheel]]
             [main :refer [get-system-map]]])
  (:use [figwheel-sidecar.repl-api :as ra]))

(set! *warn-on-reflection* true)

(def system)

(defn init []
  (alter-var-root 
   #'system
   (constantly (-> (get-system-map 8888)
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
  (repl/refresh :after 'user/go))

(defn cljs [] (ra/cljs-repl "dev"))

(comment
  (reset)
  (cljs)
)
