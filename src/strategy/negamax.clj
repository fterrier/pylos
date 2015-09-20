(ns strategy.negamax
  (:require [game.game :refer :all]))

(def negamax-table (atom {}))

(declare calculate-and-save negamax-lookup-and-save negamax-choose-move)

(defn calculate-and-save [{:keys [board player] :as game-position} alpha beta depth score-fun] 
  (let [{:keys [negamax-values stats]} (negamax-choose-move game-position alpha beta depth score-fun)]
    (swap! negamax-table assoc [board player] {:negamax-values negamax-values :depth depth})
    [negamax-values stats]))

(defn negamax-lookup-and-save [{:keys [board player] :as game-position} alpha beta depth score-fun]
  "Returns the score and save it in the transposition table along with the depth"
  ; (if (= 0 depth) (let [{:keys [negamax-values stats]} (negamax-choose-move game-position alpha beta depth score-fun)]
  ;                   [negamax-values stats])
  (if-let [e (find @negamax-table [board player])]
    (let [saved-depth (:depth (val e))]
      (if (>= saved-depth depth) 
        [(:negamax-values (val e)) {:calculated-moves 0 :lookup-moves 1}]
        (calculate-and-save game-position alpha beta depth score-fun)))
    (calculate-and-save game-position alpha beta depth score-fun)))
; )

(defn merge-and-add-stats [stats next-stats]
  (let [calculated-moves (+ (:calculated-moves stats) (:calculated-moves next-stats))
        lookup-moves     (+ (:lookup-moves stats) (:lookup-moves next-stats))]
    {:calculated-moves calculated-moves
     :lookup-moves     lookup-moves
     :total-moves      (+ calculated-moves lookup-moves)}))

(defn negamax-step [{:keys [alpha beta best-negamax-values best-game-position best-move stats]} {next-game-position :game-position, next-move :move} depth score-fun] 
  (let [[next-negamax-values next-stats] (negamax-lookup-and-save next-game-position (- beta) (- alpha) (- depth 1) score-fun)
        next-negamax-values              (assoc next-negamax-values :best-possible-score (- (:best-possible-score next-negamax-values)))
        next-best-game-position          (if (> (:best-possible-score next-negamax-values) (:best-possible-score best-negamax-values)) 
                                           {:game-position next-game-position :move next-move :negamax-values next-negamax-values}
                                           {:game-position best-game-position :move best-move :negamax-values best-negamax-values})
        next-alpha                       (max alpha (:best-possible-score next-negamax-values))]
    (let [result {:alpha next-alpha 
                  :beta beta 
                  :best-game-position  (:game-position next-best-game-position) 
                  :best-move           (:move next-best-game-position)
                  :best-negamax-values (:negamax-values next-best-game-position)
                  :stats               (merge-and-add-stats stats next-stats)}]
      (if (>= next-alpha beta) (reduced result) result))))

; (defn best-order [board]
;   (let [empty-positions  (empty-positions board)
;         middle-positions (middle-positions board)]
;     (sort-by #(not (contains? middle-positions %)) empty-positions)))

;{:game-position next-game-position :move move}
(defn order-game-positions [game-positions]
  (sort-by (fn [{{:keys [board player]} :game-position move :move}]
             (if-let [e (find @negamax-table [board player])]
               (do 
                 ;(println e)
                 (:best-possible-score (:negamax-values (val e))))
               -1000)) game-positions))

(defn negamax-choose-move
  ([{:keys [board outcome] :as game-position} alpha beta depth score-fun]
   "For a game, applies the negamax algorithm on the tree up to depth,
   returns an object with a :next-move value and :next-game-position that 
   returns the next best game-position from which the value was calculated."
   (let [score  (score-fun game-position)]
     (if (or outcome (= depth 0))
       ; else we go on with the negamax algorithm
       {:negamax-values {:best-possible-score score
                         :outcome outcome}
        :stats          {:calculated-moves 1 :lookup-moves 0}}       
       (let [next-games                 (order-game-positions (next-game-positions game-position))
             negamax-best-game-position (reduce #(negamax-step %1 %2 depth score-fun) {:alpha alpha :beta beta :best-negamax-values {:best-possible-score -1000} :stats {:calculated-moves 0 :lookup-moves 0}} next-games)
             game-position-with-score   {:next-game-position (:best-game-position  negamax-best-game-position)
                                         :next-move          (:best-move           negamax-best-game-position)
                                         :negamax-values     (:best-negamax-values negamax-best-game-position)
                                         :stats              (:stats negamax-best-game-position)}]
         game-position-with-score))))
  ([game-position depth score-fun]
   (negamax-choose-move game-position -1000 1000 depth score-fun)))

(defrecord NegamaxStrategy [score-fun depth]
  Strategy
  (choose-next-move [this game-position] 
                    (reset! negamax-table {})
                    (reduce (fn [step-result current-depth]
                              (let [start-time    (System/nanoTime)
                                    result        (negamax-choose-move game-position current-depth score-fun)
                                    end-time      (System/nanoTime)
                                    time-at-depth (double (/ (- end-time start-time) 1000000))]
                                (-> step-result
                                    (merge result)
                                    (dissoc :negamax-values)
                                    (assoc :additional-infos 
                                      (conj (:additional-infos step-result)  
                                            {:depth          current-depth
                                             :negamax-values (:negamax-values result)
                                             :time           time-at-depth
                                             :moves-per-ms   (double (/ (:total-moves (:stats result)) time-at-depth))
                                             :stats          (:stats result)}))))) {:additional-infos []} 
                            (range 1 (+ 1 depth)))))


; (defrecord NegamaxStrategy [score-fun depth]
;   Strategy
;   (choose-next-move [this game-position] 
;                     (reset! negamax-table {})
;                     (last (for [depth (range 1 10)]
;                             (do 
;                               (println "Starting negamax depth " depth)
;                               (let [start-time    (System/nanoTime)
;                                     result        (negamax-choose-move game-position depth score-fun)
;                                     end-time      (System/nanoTime)
;                                     time-at-depth (double (/ (- end-time start-time) 1000000))
;                                     total-moves   (+ (:lookup-moves (:stats result)) (:calculated-moves (:stats result)))]
;                                 (println (:stats result))
;                                 (println "Total moves          " total-moves)
;                                 (println "Moves per ms         " (double (/ total-moves time-at-depth)))
;                                 (println "Size of negamax table" (count @negamax-table))
;                                 (-> result
;                                     (dissoc :negamax-values)
;                                     (assoc :additional-infos {:negamax-values (:negamax-values result)
;                                                               :stats          (:stats result)}))))))))

(defn negamax [score-fun depth]
  (->NegamaxStrategy score-fun depth))
