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
   [system.events :refer :all]
   [system.game :refer :all]
   [system.routes :refer :all]
   [system.pylos :refer :all]
   [system.events :refer :all]
   [system.websockets :refer :all]
   [clojure.core.async :refer [chan]]
   [com.stuartsierra.component :as component]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn init []
  (alter-var-root #'system
                  (constantly (component/system-map
                                :figwheel       (map->Figwheel figwheel-config)

                                :websockets-ch  (chan)
                                :routes         (component/using (new-server-routes) [:game-runner :websockets])
                                :web-server     (component/using (new-web-server 8080) [:routes])
                                :websockets     (component/using (new-channel-sockets sente-web-server-adapter) [:event-handler])
                                :event-handler  (component/using (new-event-handler) [:websockets-ch])
                                :game-runner    (component/using (new-game-runner) [:websockets-ch :game-output])
                                :game-output    (component/using (new-game-output) [:websockets])
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
