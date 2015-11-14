(ns system.init-dev
  (:require
   ; those 2 deps are only here for convenience
   [game.output :refer :all]
   [game.game :refer :all]
   [pylos.core :refer :all]
   [clojure.tools.namespace.repl :as repl]
   [system.figwheel :refer :all]
   [system.server :refer :all]
   [system.system :refer :all]
   [system.app :refer :all]
   [system.pylos :refer :all]
   [system.events :refer :all]
   [system.websockets :refer :all]
   [com.stuartsierra.component :as component]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn init []
  (alter-var-root #'system
                  (constantly (component/system-map
                                ; :app-server (jetty-server {:app {:handler handler}, :port 3000})
                                :figwheel      (map->Figwheel figwheel-config)
                                :websockets    (component/using (new-channel-sockets sente-web-server-adapter)
                                                                [:event-handler])
                                :event-handler (new-event-handler)
                                ;:web-server (new-web-server 8080 pylos-app)
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
