(ns strategy.negamax
  (:require [clojure.core.async :refer [go]]
            [game.game :refer [make-move generate-moves]]
            [game.strategy :refer [Strategy choose-next-move]]
            ; TODO get rid of this
            [pylos.board :refer :all]))

(def negamax-table (atom {}))

(defn negamax-tt-lookup [board]
  {:pre [(some? board)]}
  (if-let [entry (find @negamax-table board)]
    (val entry) nil))

(defn negamax-tt-save! [board score depth type]
  {:pre [(some? board)(some? score)(some? depth)(some? type)]}
  (let [saved-negamax-value (negamax-tt-lookup board)]
    (when (or (nil? saved-negamax-value)
              (> depth (:depth saved-negamax-value)))
      (swap! negamax-table assoc board {:depth depth :score score :type type}))))

(defn merge-and-add-stats [stats next-stats]
  (let [calculated-moves (+ (:calculated-moves stats) (:calculated-moves next-stats))
        lookup-moves     (+ (:lookup-moves stats) (:lookup-moves next-stats))]
    {:calculated-moves calculated-moves
     :lookup-moves     lookup-moves
     :total-moves      (+ calculated-moves lookup-moves)}))

;{:game-position next-game-position :move move}
(defn order-moves-tt [moves first-move]
  (if (nil? first-move) moves
    (cons first-move (remove #(= first-move %) moves))))

(defn negamax-tt-lookup-with-depth [board depth]
  (when-let [saved-negamax-value (negamax-tt-lookup board)]
    (when (>= (:depth saved-negamax-value) depth) saved-negamax-value)))

(defn negamax-tt-save-with-bounds! [board score depth alpha beta]
  (let [type (cond (<= score alpha) :lowerbound
                   (>= score beta)  :upperbound
                   :else            :exact)]
    (negamax-tt-save! board score depth type)))

(defn next-alpha-beta [saved-negamax-value alpha-beta type]
  (if (nil? saved-negamax-value)
    alpha-beta
    (let [comp-fn     (case type :lowerbound > :upperbound <)
          saved-score (:score saved-negamax-value)]
      (if (and (= type (:type saved-negamax-value))
               (comp-fn saved-score alpha-beta))
        saved-score alpha-beta))))

(declare negamax-choose-move)

(defn negamax-step [{:keys [alpha beta best-negamax-values best-game-position best-move best-principal-variation stats]}
                    game-position next-move depth score-fun principal-variation]
  (try
  (let [next-game-position      (make-move game-position next-move)
        {next-negamax-values      :negamax-values
         next-stats               :stats
         next-principal-variation :principal-variation}
        (negamax-choose-move next-game-position (- beta) (- alpha) (- depth 1) score-fun (rest principal-variation))
        next-negamax-values     (assoc next-negamax-values :best-possible-score (- (:best-possible-score next-negamax-values)))
        next-best-game-position (if (> (:best-possible-score next-negamax-values) (:best-possible-score best-negamax-values))
                                  {:game-position next-game-position   :move next-move
                                   :negamax-values next-negamax-values :principal-variation next-principal-variation}
                                  {:game-position best-game-position   :move best-move
                                   :negamax-values best-negamax-values :principal-variation best-principal-variation})
        next-alpha              (max alpha (:best-possible-score next-negamax-values))]
    (let [result {:alpha                    next-alpha
                  :beta                     beta
                  :best-game-position       (:game-position next-best-game-position)
                  :best-move                (:move next-best-game-position)
                  :best-negamax-values      (:negamax-values next-best-game-position)
                  :best-principal-variation (:principal-variation next-best-game-position)
                  :stats                    (merge-and-add-stats stats next-stats)}]
      (if (>= next-alpha beta) (reduced result) result)))))

(defn negamax-choose-move
  ([{:keys [board outcome] :as game-position} alpha beta depth score-fun principal-variation]
   "For a game, applies the negamax algorithm on the tree up to depth,
   returns an object with a :next-move value and :next-game-position that
   returns the next best game-position from which the value was calculated."
   ; first we retrieve the move from the transposition table
   (let [saved-negamax-value (negamax-tt-lookup-with-depth board depth)
         next-alpha          (next-alpha-beta saved-negamax-value alpha :lowerbound)
         next-beta           (next-alpha-beta saved-negamax-value beta :upperbound)]
     (if (and (some? saved-negamax-value)
              (or
                ; we have an exact match
                (= :exact (:type saved-negamax-value))
                ; lowerbound is bigger than upperbound
                (>= next-alpha next-beta)))

       ; we found a match and return
       {:negamax-values {:best-possible-score (:score saved-negamax-value)}
        :stats          {:calculated-moves 0 :lookup-moves 1}}

       ; we go on
       (if (or outcome (= depth 0))
         (let [score (score-fun game-position)]
           ; we save that in the transposition table
           (negamax-tt-save-with-bounds! board score depth next-alpha next-beta)
           {:negamax-values {:best-possible-score score
                             :outcome outcome}
            :stats          {:calculated-moves 1 :lookup-moves 0}})
         ; else we go on with negamax checking all the moves
         (let [next-moves                 (order-moves-tt (generate-moves game-position) (first principal-variation))
               ;test (println game-position (empty-positions (:board game-position)) (generate-moves game-position))
               negamax-best-game-position (reduce #(negamax-step %1 game-position %2 depth score-fun principal-variation)
                                                  {:alpha next-alpha
                                                   :beta next-beta
                                                   :best-negamax-values {:best-possible-score -1000}
                                                   :stats {:calculated-moves 0 :lookup-moves 0}} next-moves)
               negamax-values             (:best-negamax-values negamax-best-game-position)
               game-position-with-score   {:principal-variation (cons (:best-move negamax-best-game-position)
                                                                      (:best-principal-variation negamax-best-game-position))
                                           :next-game-position  (:best-game-position  negamax-best-game-position)
                                           :next-move           (:best-move           negamax-best-game-position)
                                           :negamax-values      negamax-values
                                           :stats               (:stats               negamax-best-game-position)}]
           (negamax-tt-save-with-bounds! board (:best-possible-score negamax-values)
                                         depth
                                         (:alpha negamax-best-game-position)
                                         (:beta negamax-best-game-position))
           game-position-with-score)))))
  ([game-position depth score-fun principal-variation]
   (negamax-choose-move game-position -1000 1000 depth score-fun principal-variation)))

(defn tt-purge-entries [number-of-balls-on-board-limit table count-map]
  (if (empty? table) count-map
    (let [[board _]                        (first table)
          current-number-of-balls-on-board (number-of-balls-on-board board)]
      (when (< current-number-of-balls-on-board (+ 2 number-of-balls-on-board-limit))
        (swap! negamax-table dissoc board))
      (recur number-of-balls-on-board-limit (rest table) (update count-map current-number-of-balls-on-board inc)))))

(defrecord NegamaxStrategy [score-fun depth]
  Strategy
  (choose-next-move [this game-position]
    (go
                                        ; TODO purge too old entries
                                        ;(println "Before purge" (count @negamax-table))
      (let [count-map (tt-purge-entries (number-of-balls-on-board (:board game-position)) @negamax-table (into [] (repeat (number-of-positions (:board game-position)) 0)))]
                                        ;(println "After purge" (count @negamax-table) count-map)
        )
      (reduce (fn [step-result current-depth]
                (let [start-time    (System/nanoTime)
                      result        (negamax-choose-move game-position current-depth score-fun
                                                         (:principal-variation (:additional-infos step-result)))
                      end-time      (System/nanoTime)
                      time-at-depth (double (/ (- end-time start-time) 1000000))]
                  (-> step-result
                      (merge result)
                      (dissoc :negamax-values)
                      (assoc :additional-infos
                             (conj (:additional-infos step-result)
                                   {:principal-variation (:principal-variation result)
                                    :depth          current-depth
                                    :negamax-values (:negamax-values result)
                                    :time           time-at-depth
                                    :moves-per-ms   (double (/ (:total-moves (:stats result)) time-at-depth))
                                    :stats          (:stats result)})))))
              {:additional-infos []}
              (range 1 (+ 1 depth)))))
  (get-input-channel [this]))

(defn negamax [score-fun depth]
  (reset! negamax-table {})
  (->NegamaxStrategy score-fun depth))
