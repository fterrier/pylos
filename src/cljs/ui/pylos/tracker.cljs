(ns ui.pylos.tracker
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.circle :refer [circle]]))

(defn render-players [comp color players]
  (dom/div (if (empty? players)
             "No players have joined yet. "
             (map #(dom/div (:tracker/player-name %)) players))
           (dom/a {:href "#"
                   :on-click (fn [e] (om/transact! comp
                                                   `[(game/join-game 
                                                      {:color color})]))} 
                  (str "Join as " (name color) "."))))

(defn render-tracker [comp balls-remaining color state players]
  (let [ball (circle {:color color})
        remaining-balls
        (dom/ul {:class "circle-list collapsed"}
                (repeat balls-remaining
                        (dom/li (circle {:color color}))))
        text (cond
               (= state :tracker/winner) "You win :)"
               (= state :tracker/own-turn) "Your turn"
               (= state :tracker/game-over) "You lose :("
               :else "")]
    (dom/div {:class "tracker-infos"}
     (dom/div ball " " text)
     (render-players comp color players)
     (dom/div {:class "tracker-color"} remaining-balls))))

(defui PlayerInfos
  static om/IQuery
  (query [this]
         [:tracker/balls-remaining 
          :tracker/color 
          :tracker/state 
          :tracker/players])
  Object
  (render [this]
          (let [{:keys [tracker/balls-remaining tracker/color tracker/players tracker/state]} (om/props this)]
            (render-tracker this balls-remaining color state players))))

(def player-infos-comp (om/factory PlayerInfos))

(defui GameTracker
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query PlayerInfos)]
           [{:tracker/player-infos subquery}]))
  Object
  (render [this]
          (let [{:keys [tracker/player-infos]} (om/props this)]
            (dom/div {:class "tracker"}
             (map player-infos-comp player-infos)))))

(def game-tracker (om/factory GameTracker))

(defn player-infos [color state balls-remaining players]
  {:tracker/color color
   :tracker/state state ;; own-turn / winner / game-over
   :tracker/players players
   :tracker/balls-remaining balls-remaining})

(defcard all-balls
  (player-infos-comp (player-infos :white :tracker/own-turn 15 [])) 
  {:inspect-data true})

(defcard no-balls
  (player-infos-comp (player-infos :black :tracker/winner 0 [])) 
  {:inspect-data true})

(defcard players-card
  (player-infos-comp (player-infos :white :tracker/winner 10 
                                   [{:tracker/player-name "Franz" 
                                     :tracker/is-human true}
                                    {:tracker/player-name "Hans-Joseph Grüber"
                                     :tracker/is-human true}]))
  {:inspect-data true})

(defcard tracker-card
  (game-tracker
   {:tracker/player-infos
    [(player-infos :white :tracker/own-turn 15 
                   [{:tracker/player-name "Franz" 
                     :tracker/is-human true}
                    {:tracker/player-name "Hans-Joseph Grüber"
                     :tracker/is-human true}])
     (player-infos :black nil 14 [])]}))
