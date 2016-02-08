(ns chess.board
  (:require [game.board :refer [Board]]))






(defn init-board [board]
  "Inits a chess board given in a 2-D vector of positions.
   This creates maps for a given position:
     - all horizontal positions, in order of closeness
     - all vertical positions, in order of closeness
     - all diagonal positions, in order of closeness
     - all knight positions"
  )


(defn starting-board [board]
  (init-board 
   [[[:black :tower] [:black :knight] [:black :bishop] [:black :queen] 
     [:black :king] [:black :bishop] [:black :knight] [:black :tower]]
    [[:black :pawn] [:black :pawn] [:black :pawn] [:black :pawn] 
     [:black :pawn] [:black :pawn] [:black :pawn] [:black :pawn]]
    [[:open] [:open] [:open] [:open] [:open] [:open] [:open] [:open]]
    [[:open] [:open] [:open] [:open] [:open] [:open] [:open] [:open]]
    [[:open] [:open] [:open] [:open] [:open] [:open] [:open] [:open]]
    [[:open] [:open] [:open] [:open] [:open] [:open] [:open] [:open]]
    [[:white :pawn] [:white :pawn] [:white :pawn] [:white :pawn]  
     [:white :pawn] [:white :pawn] [:white :pawn] [:white :pawn]]
    [[:white :tower] [:white :knight] [:white :bishop] [:white :queen] 
     [:white :king] [:white :bishop] [:white :knight] [:white :tower]]]))


(parse-board [])
