(ns ui.pylos.history
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [ui.pylos.circle :refer [circle]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]))

(defn history-controls [comp selected-index max-index]
  (let [current-index (if (nil? selected-index) max-index selected-index)
        on-backward (fn [e]
                      (.preventDefault e)
                      (when-not (= 0 current-index)
                        (om/transact! comp `[(game/select-history ~{:index (dec current-index)}) 
                                             :app/current-game])))
        on-forward (fn [e]
                     (.preventDefault e)
                     (when-not (>= current-index (dec max-index))
                       (om/transact! comp `[(game/select-history ~{:index (inc current-index)}) 
                                            :app/current-game])))
        on-play (fn [e]
                  (.preventDefault e)
                  (when-not (nil? selected-index)
                    (om/transact! comp `[(game/select-history ~{:index nil}) 
                                         :app/current-game])))]
    (dom/div {:class "history-controls"}
             (dom/i {:class (str "fa fa-backward " (when (= 0 current-index) "is-disabled"))
                     :on-click on-backward})
             (dom/i {:class (str "fa fa-forward " (when (>= current-index (dec max-index)) "is-disabled"))
                     :on-click on-forward})
             (dom/i {:class (str "fa fa-play " (when (nil? selected-index) "is-disabled"))
                     :on-click on-play}))))

(defn history-list [comp merged-game-infos selected-index highlighted-index]
  (let [highlight-fn (fn [index] 
                       (om/set-state! comp {:highlight index}))
        highlight-out-fn (fn []
                           (om/set-state! comp {:highlight nil}))
        find-index (fn [e]
                     (let [touches (.-changedTouches e)]
                       (when (= 1 (.-length touches))
                         (let [x (.-pageX (aget touches 0))
                               y (.-pageY (aget touches 0))
                               element (.elementFromPoint js/document x y)
                               index (js/parseInt (.getAttribute element "data-index"))]
                           (when-not (js/isNaN index) index)))))
        select-fn (fn [index] (om/transact! comp `[(game/select-history ~{:index index})
                                                   :app/current-game]))]
    (dom/ul {:class (str "history-list " (when highlighted-index "is-highlighted"))
             :on-touch-start (fn [e] 
                               (when-let [index (find-index e)]
                                 (highlight-fn index)))
             :on-touch-move (fn [e]
                              (when-let [index (find-index e)]
                                (highlight-fn index)))
             :on-touch-end (fn [e]
                             (when-let [index (find-index e)]
                               (println index)
                               (select-fn index)))
             :on-touch-cancel (fn [e]
                                (highlight-out-fn))}
            (map #(dom/li
                   {:data-index (:index %)
                    :class (str "history-item " 
                                (when (= (:index %) selected-index) "history-selected ")
                                (when (= (:index %) highlighted-index) "is-highlighted"))
                    :on-mouse-over (fn [e] 
                                     (.preventDefault e)
                                     (highlight-fn (:index %)))
                    :on-mouse-out (fn [e]
                                    (.preventDefault e)
                                    (highlight-out-fn))}
                   (dom/figure {:data-index (:index %)
                                :class (str "circle circle-" (name (:player %)))
                                :on-click (fn [e] 
                                            (println "click" (:index %))
                                            (select-fn (:index %)))}
                               (:index %))) merged-game-infos))))

(defui GameHistory
  static om/IQuery
  (query [this]
         '[:app/selected-index 
           :app/merged-game-infos])
  Object
  (render [this]
          (let [{:keys [app/merged-game-infos app/selected-index]} (om/props this)
                highlighted-index (om/get-state this :highlight)]
            (dom/div {:class "history"}
                     (history-controls this selected-index (count merged-game-infos))
                     (history-list this merged-game-infos selected-index highlighted-index)))))

(def game-history (om/factory GameHistory))

(defcard test-game-history
  (game-history {:app/selected-index 10
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                         {:index 2 :player :white}
                                         {:index 3 :player :black} 
                                         {:index 4 :player :white}
                                         {:index 5 :player :black} 
                                         {:index 6 :player :white}
                                         {:index 7 :player :black} 
                                         {:index 8 :player :white}
                                         {:index 9 :player :black} 
                                         {:index 10 :player :white}
                                         {:index 11 :player :black} 
                                         {:index 12 :player :white}
                                         {:index 13 :player :black} 
                                         {:index 14 :player :white}
                                         {:index 15 :player :black} 
                                         {:index 16 :player :white}
                                         {:index 17 :player :black} 
                                         {:index 18 :player :white}
                                         {:index 19 :player :black} 
                                         {:index 20 :player :white}
                                         {:index 21 :player :black} 
                                         {:index 22 :player :white}
                                        ]}))

(defcard test-game-history-long
  (game-history {:app/selected-index 10
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                         {:index 2 :player :white}
                                         {:index 3 :player :black} 
                                         {:index 4 :player :white}
                                         {:index 5 :player :black} 
                                         {:index 6 :player :white}
                                         {:index 7 :player :black} 
                                         {:index 8 :player :white}
                                         {:index 9 :player :black} 
                                         {:index 10 :player :white}
                                         {:index 11 :player :black} 
                                         {:index 12 :player :white}
                                         {:index 13 :player :black} 
                                         {:index 14 :player :white}
                                         {:index 15 :player :black} 
                                         {:index 16 :player :white}
                                         {:index 17 :player :black} 
                                         {:index 18 :player :white}
                                         {:index 19 :player :black} 
                                         {:index 20 :player :white}
                                         {:index 21 :player :black} 
                                         {:index 22 :player :white}
                                         {:index 23 :player :black}
                                         {:index 24 :player :white}
                                         {:index 25 :player :black}
                                         {:index 26 :player :white}
                                         {:index 27 :player :black}
                                         {:index 28 :player :white}
                                         {:index 29 :player :black}
                                         {:index 30 :player :white}
                                         {:index 31 :player :black}
                                         {:index 32 :player :white}
                                         {:index 33 :player :black}
                                         {:index 34 :player :white}
                                         {:index 35 :player :black}
                                        ]})
  nil
  {:classname "devcard-short"})

(defcard test-game-history
  (game-history {:app/selected-index 1
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                        ]}))

(defcard test-game-history
  (game-history {:app/selected-index nil
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                        ]}))

(defcard test-game-history
  (game-history {:app/selected-index 0
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                        ]}))
