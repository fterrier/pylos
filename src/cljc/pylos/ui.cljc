(ns pylos.ui
  #?@(:clj
       [(:require
         [game.serializer :refer [deserialize-game-position]]
         [pylos.serializer :refer [new-pylos-serializer]]
         [pylos.board
          :refer
          [add-ball balls-remaining has-ball remove-ball square-corners]]
         [pylos.move :refer [generate-all-moves]])]
       :cljs
       [(:require
         [game.serializer :refer [deserialize-game-position]]
         [pylos.serializer :refer [new-pylos-serializer]]
         [pylos.board
          :refer
          [add-ball balls-remaining has-ball remove-ball square-corners]]
         [pylos.move :refer [generate-all-moves]])]))

(defn- all-permutations [positions]
  "Returns a vector of [permutation complete]"
  (if (vector? positions)
    (if (= 2 (count positions))
      [[[(get positions 0)] false] [positions true]]
      [[positions true]])
    (let [position-vec (into [] positions)]
      (if (= 2 (count position-vec))
        [[[(get position-vec 0) (get position-vec 1)] true]
         [[(get position-vec 1) (get position-vec 0)] true]
         [[(get position-vec 0)] false]
         [[(get position-vec 1)] false]]
        [[position-vec true]]))))

(defn- info-map [info positions]
  (into {} (map (fn [position] [position info]) positions)))

; TODO maybe get rid of board here ?
; TODO make this nicer to read
(defn- highlight-infos [board {:keys [type position low-position original-move positions] :as move}]
  "Generates a map of {[<current-selections> highlighted-position] {<position1> <status> <position2> <status>}}"
  (case type
    :add    [{[position] {position {:addable true}}}]
    :rise   [{[low-position] {position {:addable true} low-position {:risable true}}}
             {[low-position :all] {position {:addable true}}}
             ;; TODO do we display low positions when selecting high ?
             #_{[position] {:highlights {position {:addable true} low-position {:risable true}}}}]

    :square (concat
              (highlight-infos board original-move)
              ;; highlight in square positions
              (map (fn [position] {[(:position original-move)] {position {:in-square true}}}) (square-corners board (:square-position move)))
              (let [prefix (case (:type original-move)
                             :add  [(:position original-move)]
                             :rise [(:low-position original-move) (:position original-move)])]
                (concat
                 ;; whenever original move is made, highlight all removable positions
                 [{(conj prefix :all) (info-map {:removable true} positions)}]
                 ;; whenever a removable move is selected display the rest as removable
                 (map (fn [[[first & rest] _]] {(conj prefix first :all) (info-map {:removable true} rest)}) (all-permutations positions)))))))

(defn- move-infos [board {:keys [type position low-position original-move positions color] :as move} move-to-save playable-move current-selection]
  "Generates a map of {[current-selections] :moves ... :playable-move ...}"
  (case type
    :add    {(into [] (cons position current-selection))
             {:moves [move-to-save] :playable-move playable-move}}
    :rise   (let [full-move-info
                  {(into [] (concat [low-position position] current-selection))
                   {:moves [move-to-save] :playable-move playable-move}}]
              (if (empty? current-selection)
                (assoc full-move-info
                       (into [] (cons low-position current-selection))
                       {:moves [move-to-save]})
                full-move-info))
    :square (merge
             (move-infos board original-move move nil [])
             (apply merge
                    (map (fn [[removed-balls complete]]
                           (move-infos board original-move move
                                       (if complete move nil) removed-balls))
                         (all-permutations positions))))))

(defn- merge-move-infos [{move-1 :moves play-1 :playable-move} {move-2 :moves play-2 :playable-move}]
  {:moves (concat move-1 move-2)
   :playable-move (if play-1 play-1 play-2)})

(defn highlight-status [{:keys [board] :as game-position} moves]
  "we highlight different stuff if we are in the middle of a rise or square
  also the actual highlight position does not need to be saved, just the
  state of each balls in a map {position <state>} where <state> is
  {:addable :risable :in-square)} and position is [<current-selections> <highlighted-position>]"
  (reduce #(apply merge-with merge %1 (highlight-infos board %2)) {} moves))

(defn- intermediate-board [current-board color selections]
  (reduce (fn [board selection]
            (if (has-ball board selection)
              (remove-ball board color selection)
              (add-ball board color selection))) current-board selections))

(defn move-status [{:keys [board player]} moves]
  "we generate all possible path to moves"
  (into {} (map (fn [[selections move-info]]
                  [selections (assoc move-info :intermediate-board 
                                     (fn []
                                       (intermediate-board board player selections)))])
                (reduce #(merge-with merge-move-infos %1 (move-infos board %2 %2 %2 [])) {} moves))))

;; TODO move these 2 to CLJS ?
(defn game-infos-with-meta [{:keys [game-position] :as game-infos}]
  (let [possible-moves   (generate-all-moves game-position)
        highlight-status (highlight-status game-position possible-moves)
        game-infos       (assoc game-infos :game-position game-position :highlight-status highlight-status)]
    game-infos))

(defn score [{:keys [board]}]
  {:white (balls-remaining board :white) :black (balls-remaining board :black)})
