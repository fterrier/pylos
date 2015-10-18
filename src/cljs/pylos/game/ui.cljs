(ns pylos.game.ui
  (:require [cljs.core.async :as async :refer [>! <! alts! chan close!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.board :refer [board-comp]]
            [pylos.game.state :refer [app-state]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defcomponent app [app-state owner]
  (render [_]
          (om/build board-comp nil)))

(defmulti handle-control :action)

(defmethod handle-control :select-cell [control]
  (println control))

(defn main []
  (let [control-ch (chan)]
    (swap! app-state #(assoc % :control-ch control-ch))

    (go
     (while true
       (handle-control (<! control-ch))))

    (om/root app app-state
             {:target (. js/document (getElementById "main-area"))
              :shared {:control-ch control-ch}})))

(defn stop []
  (fnil close! (:control-ch app-state))
  (swap! app-state #(assoc % :control-ch nil)))
