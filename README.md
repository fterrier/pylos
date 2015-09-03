# Pylos

This is an attempt at providing an AI for the board game [Pylos](https://boardgamegeek.com/boardgame/1419/pylos) in Clojure.

For now, the game is played on the terminal only ...

Several strategies are implemented :

| Name | File | Description |
|---|---|---|
| NegamaxStrategy | [negamax.clj](src/strategy/negamax.clj) | The negamax algorithm with alpha-beta pruning and transposition tables |
| HumanStrategy | [human.clj](src/pylos/human.clj) | Lets a human play |

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

### Bugs

Open Github issue and don't hesitate to PR.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
