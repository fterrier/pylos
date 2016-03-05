[![Build Status](https://travis-ci.org/fterrier/pylos.svg?branch=master)](https://travis-ci.org/fterrier/pylos)

# Board Game Engine for Pylos 

This is a board game engine and AI implementation in Clojure for 2-player games. At the moment, the board game [Pylos](https://boardgamegeek.com/boardgame/1419/pylos) in Clojure.

TODO screenshot

# Getting started

## Running the project in the REPL

```bash
git clone git@github.com:fterrier/pylos.git
cd pylos
lein repl
```

The project uses Stuart Sierra's components library. In the REPL, then use this to start (and restart) the system:

```bash
(user/go)   # start system
(user/stop) # stop system
```

Open `http://localhost:8888`. To enter a CLJS REPL connection to the browser, run:

```bash
(user/cljs)
```

You can then play around with the app running in the browser. Try for instance:

```
# TODO output reconciler app-state
```

## Running the project standalone

### With an uberjar

```
lein uberjar
java -jar target/pylos.jar
```

Open `http://localhost:8080`

### With docker

```
lein uberimage
docker run -p 4000:8080 <generated-docker-image-id>
```

Open `http://<docker-machine-ip>:4000`

# Basic concepts

The engine can be extended to support multiple strategies. For now, those strategies are implemented :

| Name | Description |
|---|---|
| NegamaxStrategy | The negamax algorithm with alpha-beta pruning and transposition tables |
| RandomStrategy  | Plays randomly |
| ChannelStrategy | Accepts move input from a channel |

More will be added in the future so we can let them play against each other.

TODO explain strategies, game and output functions.


# Useful REPL commands

TODO

In the REPL, you can call the following shortcut commands:

```bash
(-> (new-game) 
    (add-negamax 8 :white) 
    (add-negamax 8 :black) 
    (output-game pylos.pprint/print-pylos-game) 
    (start-game))

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

# TODO (incomplete)

- [] Use test.check to test the game algorithm, game and game server
- [] Handle timeouts, errors, etc...
- [] Persist game - CRDTs?
- [] Implement chess and create nicer abstractions for UI
- [] Scalability - what happens when game runner runs on more servers?

# Bugs

Open Github issue and don't hesitate to PR.

# License

Copyright © 2015 François Terrier

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
