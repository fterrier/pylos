(ns ui.pylos.tracker
  (:require
   [om.next :as om :refer-macros [defui ui]]
   [om-tools.dom :as dom :include-macros true]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))


(defn tracker [balls-remaining current player outcome]
  (let [counter (get balls-remaining current)
        circle (dom/ul {:class "tracker-circle-list"}
                       (repeat counter
                               (dom/li {:class (str "tracker-circle tracker-circle-" (name current))}
                                       (dom/figure {:class (str "circle circle-" (name current))}
                                                   counter))))
        text   (dom/div {:class "tracker-text"} 
                        (cond
                          (= outcome current) "Winner!"
                          (= player current) "Your turn"
                          :else ""))]
    (dom/div {:class (str "tracker-color tracker-color-" (name current))}
             (if (= :white current) 
               [text circle]
               [circle text]))))

(defui GameTracker
  static om/IQuery
  (query [this]
         '[:tracker/balls-remaining :tracker/player :tracker/outcome])
  Object
  (render [this]
          (let [{:keys [tracker/balls-remaining tracker/player tracker/outcome]} (om/props this)]
            (dom/div {:class "tracker"}
                     (tracker balls-remaining :white player outcome)
                     (tracker balls-remaining :black player outcome)))))

(def game-tracker (om/factory GameTracker))

(defcard last-white-ball
  (game-tracker
   {:tracker/player :white
    :tracker/balls-remaining {:white 15 :black 15}}) {:inspect-data true})

(defcard last-black-ball
  (game-tracker
   {:tracker/player :black
    :tracker/balls-remaining {:black 2 :white 9}}) {:inspect-data true})
