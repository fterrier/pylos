(ns ui.pylos.history
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [ui.pylos.circle :refer [circle]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]))

(defn element-before-and-after [merged-game-infos selected-index]
  (let [[list-before _ list-after] (into [] (partition-by #(= (:index %) selected-index)) 
                                         (cons nil merged-game-infos))]
    [(last list-before) (first list-after)]))

(defn history-controls [comp merged-game-infos selected-index]
  "Displays a forward, a backward and a play button with this logic
  - backward enabled when there is an element before in the list
  - forward enabled when there is an element after in the list
  - play enabled when the selected element is not the last, clicking on it jumps to the last element"
  (let [[element-before element-after] (element-before-and-after merged-game-infos selected-index)
        last-element (last merged-game-infos)
        play-enabled (not= (:index last-element) selected-index)
        on-backward (fn [e]
                      (.preventDefault e)
                      (when element-before
                        (om/transact! comp `[(game/select-history ~{:index (:index element-before)})
                                             :app/current-game])))
        on-forward (fn [e]
                     (.preventDefault e)
                     (when element-after 
                       (om/transact! comp `[(game/select-history ~{:index (:index element-after)}) 
                                            :app/current-game])))
        on-play (fn [e]
                  (.preventDefault e)
                  (when play-enabled
                    (om/transact! comp `[(game/select-history ~{:index (:index last-element)})
                                         :app/current-game])))]
    (dom/div {:class "history-controls"}
             (dom/i {:class (str "fa fa-backward " (when-not element-before "is-disabled"))
                     :on-click on-backward})
             (dom/i {:class (str "fa fa-forward " (when-not element-after "is-disabled"))
                     :on-click on-forward})
             (dom/i {:class (str "fa fa-play " (when-not play-enabled "is-disabled"))
                     :on-click on-play}))))

;; TODO this could be one component
(defn history-list [comp merged-game-infos selected-index]
  (let [highlighted-idx (om/get-state comp :highlight) ;; this is the idx in the merged-game-infos vector
        highlight-fn (fn [idx]
                       (om/set-state! comp {:highlight idx}))
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
    (dom/ul {:class (str "history-list " 
                         ;; we mark as is highlighted if an item is selected and it is not the last one
                         (when (and highlighted-idx
                                    (not= highlighted-idx (dec (count merged-game-infos)))) "is-highlighted"))
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
            (map-indexed 
             (fn [idx game-infos] (dom/li
                     {:data-index (:index game-infos)
                      :class (str "history-item " 
                                  (when (= (:index game-infos) selected-index) "history-selected ")
                                  (when (= idx highlighted-idx) "is-highlighted"))
                      :on-mouse-over (fn [e] 
                                       (.preventDefault e)
                                       (highlight-fn idx))
                      :on-mouse-out (fn [e]
                                      (.preventDefault e)
                                      (highlight-out-fn))}
                     (dom/figure {:data-index (:index game-infos)
                                  :class (str "circle " (if (:last-player game-infos) 
                                                          (str "circle-" (name (:last-player game-infos)))
                                                          "circle-open"))
                                  :on-click (fn [e] 
                                              (println "click" (:index game-infos))
                                              (select-fn (:index game-infos)))}
                                 (when (> (:index game-infos) 0) (:index game-infos))))) merged-game-infos))))

(defui GameHistory
  static om/IQuery
  (query [this]
         '[:history/selected-index
           :history/merged-game-infos])
  Object
  (render [this]
          (let [{:keys [history/merged-game-infos history/selected-index]} (om/props this)]
            (dom/div {:class "history"}
                     (history-controls this merged-game-infos selected-index)
                     (history-list this merged-game-infos selected-index)))))

(def game-history (om/factory GameHistory))

(defcard test-game-history
  (game-history {:history/selected-index 10
                 :history/merged-game-infos [{:index 0}
                                         {:index 1 :last-player :black} 
                                         {:index 2 :last-player :white}
                                         {:index 3 :last-player :black} 
                                         {:index 4 :last-player :white}
                                         {:index 5 :last-player :black} 
                                         {:index 6 :last-player :white}
                                         {:index 7 :last-player :black} 
                                         {:index 8 :last-player :white}
                                         {:index 9 :last-player :black} 
                                         {:index 10 :last-player :white}
                                         {:index 11 :last-player :black} 
                                         {:index 12 :last-player :white}
                                         {:index 13 :last-player :black} 
                                         {:index 14 :last-player :white}
                                         {:index 15 :last-player :black} 
                                         {:index 16 :last-player :white}
                                         {:index 17 :last-player :black} 
                                         {:index 18 :last-player :white}
                                         {:index 19 :last-player :black} 
                                         {:index 20 :last-player :white}
                                         {:index 21 :last-player :black} 
                                         {:index 22 :last-player :white}
                                         {:index :current :last-player :black}
                                        ]}))

(defcard test-game-history-long
  (game-history {:history/selected-index 10
                 :history/merged-game-infos [{:index 0}
                                         {:index 1 :last-player :black} 
                                         {:index 2 :last-player :white}
                                         {:index 3 :last-player :black} 
                                         {:index 4 :last-player :white}
                                         {:index 5 :last-player :black} 
                                         {:index 6 :last-player :white}
                                         {:index 7 :last-player :black} 
                                         {:index 8 :last-player :white}
                                         {:index 9 :last-player :black} 
                                         {:index 10 :last-player :white}
                                         {:index 11 :last-player :black} 
                                         {:index 12 :last-player :white}
                                         {:index 13 :last-player :black} 
                                         {:index 14 :last-player :white}
                                         {:index 15 :last-player :black} 
                                         {:index 16 :last-player :white}
                                         {:index 17 :last-player :black} 
                                         {:index 18 :last-player :white}
                                         {:index 19 :last-player :black} 
                                         {:index 20 :last-player :white}
                                         {:index 21 :last-player :black} 
                                         {:index 22 :last-player :white}
                                         {:index 23 :last-player :black}
                                         {:index 24 :last-player :white}
                                         {:index 25 :last-player :black}
                                         {:index 26 :last-player :white}
                                         {:index 27 :last-player :black}
                                         {:index 28 :last-player :white}
                                         {:index 29 :last-player :black}
                                         {:index 30 :last-player :white}
                                         {:index 31 :last-player :black}
                                         {:index 32 :last-player :white}
                                         {:index 33 :last-player :black}
                                         {:index 34 :last-player :white}
                                         {:index 35 :last-player :black}
                                         {:index :current :outcome :white}
                                        ]})
  nil
  {:classname "devcard-short"})

(defcard test-game-history-no-backward
  (game-history {:history/selected-index 0
                 :history/merged-game-infos [{:index 0}
                                         {:index 1 :last-player :white} 
                                         {:index 2 :last-player :black}
                                         {:index :current :last-player :white}]}))

(defcard test-game-history-no-forward
  (game-history {:history/selected-index :current
                 :history/merged-game-infos [{:index 0}
                                         {:index 1 :last-player :white}
                                         {:index 2 :last-player :black}
                                         {:index :current :last-player :white}]}))

(defcard test-game-history
  (game-history {:history/selected-index 1
                 :history/merged-game-infos [{:index 0}
                                         {:index 1 :last-player :white} 
                                         {:index 2 :last-player :black}
                                         {:index :current :last-player :white}]}))
