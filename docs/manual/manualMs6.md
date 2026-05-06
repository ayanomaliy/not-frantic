
# Frantic^-1 Manual

**Frantic^-1** is a chaotic multiplayer card game.  
Players try to get rid of all cards in their hand while using special cards, black-card events and player effects to disrupt each other.

This manual explains the most important things needed to play the game through the **GUI**.

---

## 1. Starting the Game

Frantic^-1 uses a server-client system.

One person starts the server.  
Every player starts a GUI client and connects to that server.

### Start the Server

```bash
java -jar not-frantic.jar server
````

This starts the server on the default port `5555`.

To use a specific port:

```bash
java -jar not-frantic.jar server 5555
```

Keep the server running while playing.

### Start the GUI Client

Each player starts the graphical client with:

```bash
java -jar not-frantic.jar client
```

You can also prefill host, port and username:

```bash
java -jar not-frantic.jar client localhost:5555 Alice
```

---

## 2. Connecting

In the connect screen, enter:

* **Host**
* **Port**
* **Username**

For a local game on the same computer:

```text
Host: localhost
Port: 5555
```

Then click **Connect**.

---

## 3. Starting a Match

After connecting, you enter the lobby screen.

To start a match:

1. One player clicks **Create New Lobby**.
2. The other players select that lobby and click **Join Selected Lobby**.
3. When everyone is in the lobby, click **Start Game**.

You can leave a lobby with **Leave Lobby**.

---

## 4. Game Screen

The game screen shows:

* the draw pile
* the discard pile
* the current player
* the current phase
* your hand
* other players and their card counts
* the Game / Info log
* chat

Your cards are shown at the bottom.

Click a card to play it.
Click the draw pile to draw.
Click **End Turn** to end your turn when allowed.

---

## 5. Goal of the Game

The goal is to get rid of all cards in your hand.

A round ends when one player has no cards left.

At the end of a round, players receive penalty points for cards still in their hand.

The full match ends when the score limit is reached.

The player with the **lowest total score** wins.

---

## 6. How a Turn Works

On your turn, you normally do one of these:

1. Play a legal card.
2. Draw a card.
3. End your turn when the game allows it.

You usually cannot end your turn without first playing or drawing.

If something is not allowed, the Game / Info log shows an error.

---

## 7. Normal Cards

Normal cards have:

* a color
* a number

Colors:

* Red
* Yellow
* Green
* Blue

Numbers:

```text
1 to 9
```

A normal card can be played if it matches:

* the current color
* or the current number
* or the active requested color/number

Normal cards score their face value.

---

## 8. Black Cards

Black cards have a number but no normal color.

A black card can be played when its number matches the current top card or active number request.

When a black card is played, it automatically triggers an event card.

Black cards score double their face value.

---

## 9. Special Cards

Special cards do more than just match a number or color.

Most special cards open a GUI overlay.
When that happens, follow the overlay instructions and click **Done** or **Next**.

---

### 9.1 Second Chance

Lets the player immediately play another card.

If another card is played:

* it is placed on the discard pile
* it may trigger another effect
* it may end the round if the player has no cards left

If no card is played, the player draws instead.

Score value: **10 points**

---

### 9.2 Skip

Choose one player.

That player's next turn is skipped.

Score value: **10 points**

---

### 9.3 Gift

Choose cards from your hand and give them to another player.

Score value: **10 points**

---

### 9.4 Exchange

Choose cards from your hand and choose another player.

Your selected cards are exchanged with cards from that player's hand.

Score value: **10 points**

---

### 9.5 Fantastic

Choose either:

* one color
* or one number

That choice becomes the active request for the next play.

Score value: **20 points**

---

### 9.6 Fantastic Four

Choose either a requested color or number.

Then assign four drawn cards to players.

The same player can receive multiple cards.

Score value: **20 points**

---

### 9.7 Equality

Choose a player and a color.

The chosen player draws until their hand size reaches your hand size.

The chosen color becomes the active request.

Score value: **20 points**

---

### 9.8 Counterattack

Choose a color.

The chosen color becomes the active request.

Counterattack can also redirect a pending effect in some situations.

Score value: **20 points**

---

### 9.9 Nice Try

Stops a player from safely ending the round.

The chosen player draws up to three cards, so the round can continue.

Score value: **20 points**

---

### 9.10 Fuck You

A unique special card.

It can only be played when the current player has exactly **10 cards** in hand.

Score value: **69 points**

---

## 10. Event Cards

Event cards are triggered automatically when a black card is played.

Players do not choose them manually.

Current event cards:

| Event               | Effect                                                    |
| ------------------- | --------------------------------------------------------- |
| All Draw Two        | Every player draws up to two cards.                       |
| All Draw One        | Every player draws up to one card.                        |
| All Skip            | Every player except the triggering player is skipped.     |
| Instant Round End   | The round ends immediately.                               |
| Reverse Order       | The turn order is reversed.                               |
| Steal From Next     | The triggering player steals from the next player.        |
| Steal From Previous | The triggering player steals from the previous player.    |
| Discard Highest     | Every player discards their highest-scoring card.         |
| Discard Color       | Players discard cards matching the current discard color. |
| Swap Hands          | The triggering player swaps hands with the next player.   |
| Block Specials      | Special cards are blocked until the next black card.      |
| Gift Chain          | Each player passes one card to the next player.           |
| Hand Reset          | All players discard their hands and draw new cards.       |
| Lucky Draw          | The triggering player draws up to three cards.            |
| Penalty Draw        | The player with the most cards draws extra cards.         |
| Equalize            | Players draw until they match the largest hand size.      |
| Wild Request        | A requested color is set.                                 |
| Cancel Effects      | Pending effects are cancelled.                            |
| Bonus Play          | The triggering player gets another play opportunity.      |
| Double Scoring      | Round scores are doubled.                                 |

Event cards are not held in player hands and score **0 points**.

---

## 11. Scoring

At the end of a round, every card still in your hand gives penalty points.

Lower score is better.

| Card type      |         Points |
| -------------- | -------------: |
| Normal card    |     face value |
| Black card     | face value × 2 |
| Second Chance  |             10 |
| Skip           |             10 |
| Gift           |             10 |
| Exchange       |             10 |
| Fantastic      |             20 |
| Fantastic Four |             20 |
| Equality       |             20 |
| Counterattack  |             20 |
| Nice Try       |             20 |
| Fuck You       |             69 |
| Event card     |              0 |

If the **Double Scoring** event happened during the round, the round score is doubled.

---

## 12. Match End

The match ends when at least one player reaches or exceeds the score limit.

The score limit is:

```text
150 - (3 × number of players)
```

The player with the **lowest total score** wins.

---

## 13. Chat

The GUI supports:

* Global chat
* Lobby chat
* Whisper chat

Use the chat mode button to switch modes.

In whisper mode, write:

```text
PlayerName: Message
```

---

## 14. Command Field

The game is intended to be played through the GUI.

The command field is mainly a fallback for testing, debugging or advanced use.

Useful commands include:

```text
/lobbies
/players
/allplayers
/create <lobby>
/join <lobby>
/leave
/start
/hand
/gamestate
/play <cardId>
/draw
/end
```

