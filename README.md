# Pylos

This is an attempt at providing an AI for the board game [Pylos](https://boardgamegeek.com/boardgame/1419/pylos) in Clojure.

For now, a UI is provided for the terminal.

Several strategies are implemented :

| Name | File | Description |
|---|---|---|
| NegamaxStrategy | [negamax.clj](src/strategy/negamax.clj) | The negamax algorithm with alpha-beta pruning and transposition tables |
| HumanStrategy   | [human.clj](src/pylos/human.clj) | Lets a human play |
| RandomStrategy  | [human.clj](src/strategy/random.clj) | Plays randomly (TBD) |

More will be added in the future so we can let them play against each other.

## Installation

Clone and run

```bash
lein repl
```

## Usage

In the REPL, you can call the following shortcut commands:

```bash
# Negamax plays against negamax, :white starts, negamax fixed depth is 8
(output (play-negamax 4 :white 8))

# Negamax plays against human, human is :white, :black starts, negamax fixed depth is 8
(output (play-human 4 :white :black 8))
```

You should see stuff like this:
```
====================
Board after move of :black
====================

Move: {:type :add, :position 20, :color :black}
Time: 772.270422

o o o o     - - -     - -     -
o w b o     - b -     - -
o b w o     - - -
o o o o

Balls remaining :
 - :white 13
 - :black 12

{:negamax-values {:best-possible-score 1/2, :outcome nil}, :stats {:calculated-moves 14176, :lookup-moves 16234}}
Calculated moves per ms:  18.35626432938798
```

### TODO

[] Use test.check to test the game algorithm, game and game server
[] Write more tests
[] Handle end of game, timeouts, errors, etc...
[] Persist game, join en route
[] Cleanup app.cljs make modules
[x] Handle client reconnect

### Bugs

Open Github issue and don't hesitate to PR.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
