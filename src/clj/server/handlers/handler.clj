(ns server.handlers.handler)

(defprotocol Handler
  (start-handler [this gamerunner-ch])
  (stop-handler [this])
  (get-routes [this]))
