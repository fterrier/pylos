(ns ui.pylos.tracker
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.circle :refer [circle]]))

(defn tracker [balls-remaining color state]
  (let [ball (circle {:color color})
        remaining-balls
             (dom/ul {:class "tracker-circle-list"}
                     (repeat balls-remaining
                             (dom/li {:class (str "tracker-circle tracker-circle-" (name color))}
                                     circle)))
        text (dom/div {:class "tracker-text"}
                      (cond
                        (= state :tracker/winner) "Winner!"
                        (= state :tracker/own-turn) "Your turn"
                        (= state :tracker/game-over) "You lose :("
                        :else ""))]
    (dom/div
     ball
     (dom/div {:class (str "tracker-color tracker-color-" (name color))}
              [text remaining-balls]))))

(defui PlayerInfos
  static om/IQuery
  (query [this]
         '[:tracker/balls-remaining :tracker/color :tracker/state :tracker/players])
  Object
  (render [this]
          (let [{:keys [tracker/balls-remaining tracker/color tracker/players tracker/state]} (om/props this)]
            (dom/div {:class "tracker"}
                     (tracker balls-remaining color state)))))

(def player-infos (om/factory PlayerInfos))

(defui GameTracker
  static om/IQuery
  (query [this]
         '[:tracker/player-infos]))

(def game-tracker (om/factory GameTracker))


(defcard all-balls
  (player-infos
   {:tracker/color :white
    :tracker/state :tracker/own-turn ;; own-turn / other-turn / winner / game-over
    :tracker/balls-remaining 15}) {:inspect-data true})

(defcard no-balls
  (player-infos
   {:tracker/color :black
    :tracker/state :tracker/winner
    :tracker/balls-remaining 0}) {:inspect-data true})
