(ns system.init-dev
  (:require
    [clojure.tools.namespace.repl :as repl]
    [system.figwheel :refer :all]
    [system.server :refer :all]
    [com.stuartsierra.component :as component]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (component/system-map
                                ; :app-server (jetty-server {:app {:handler handler}, :port 3000})
                                :figwheel   (map->Figwheel figwheel-config)
                                :web-server (new-web-server 8080)
                                ))))

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
