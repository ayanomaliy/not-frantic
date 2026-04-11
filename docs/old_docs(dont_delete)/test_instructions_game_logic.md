# Game Logic Manual Test Instructions

## Overview

These instructions walk through a full 3-player game session using raw protocol
messages sent over TCP. Every test scenario is self-contained — you send the
exact message shown, and the expected server response is listed immediately after.

**Tool:** Any TCP client — e.g. `netcat`, PuTTY (raw mode), or the project CLI client.

**Server port:** default `4444` (check your run configuration).

All messages follow the `TYPE|payload` protocol. Commands with no payload must
still include the pipe character: `DRAW_CARD|`, `END_TURN|`, `START|`.

---

## 1 — Start the Server

```
./gradlew run
```

Wait for the `[SERVER]` ready log line before connecting clients.

---

## 2 — Connect Three Clients

Open **three separate terminal windows** and connect each one:

```bash
nc localhost 4444
```

Each client receives immediately:

```
INFO|Connected to server. Use /create <lobby> or /join <lobby>.
```

### 2.1 — Set Player Names

**Client 1** → Alice:
```
NAME|Alice
```

**Client 2** → Bob:
```
NAME|Bob
```

**Client 3** → Charlie:
```
NAME|Charlie
```

---

## 3 — Create and Join a Lobby

### 3.1 — Alice creates the lobby

**Alice sends:**
```
CREATE|game1
```

**Alice receives:**
```
INFO|Joined lobby: game1
PLAYERS|Alice
```

### 3.2 — Bob joins

**Bob sends:**
```
JOIN|game1
```

**All lobby members receive:**
```
INFO|Bob joined the lobby.
PLAYERS|Alice,Bob
```

### 3.3 — Charlie joins

**Charlie sends:**
```
JOIN|game1
```

**All lobby members receive:**
```
INFO|Charlie joined the lobby.
PLAYERS|Alice,Bob,Charlie
```

---

## 4 — Start the Game

Any player may send `START|`. Alice does it:

**Alice sends:**
```
START|
```

All three clients receive the following in order:

**1. Public game state (same for all three):**
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:<id>,drawPileSize:103,players:Alice:7:0,Bob:7:0,Charlie:7:0
```
- `discardTop` — id of the first card flipped onto the discard pile.
- `drawPileSize` — starts at `125 − (7×3) − 1 = 103`.
- Round 1 player order is alphabetical: Alice → Bob → Charlie.

**2. Private hand (each player receives only their own):**
```
HAND_UPDATE|<id1>,<id2>,<id3>,<id4>,<id5>,<id6>,<id7>
```
Note down your seven card ids — you will need them for subsequent commands.

**3. Turn-start state update (same for all three):**
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,...
```

> **Verify:** Each client sees `phase:AWAITING_PLAY`, `currentPlayer:Alice`, and
> exactly 7 ids in their private `HAND_UPDATE`.

---

## 5 — Basic Turn: Play a Color Card

It is Alice's turn. Color card ids are in the range **0–71**.

A color card is playable if it matches the `discardTop` by color **or** by value
(when no color/number request is active).

**Alice sends** (use a valid id from her hand):
```
PLAY_CARD|<colorCardId>
```

**On success** — all three clients receive:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Bob,discardTop:<colorCardId>,drawPileSize:103,players:Alice:6:0,Bob:7:0,Charlie:7:0
```

Alice now has 6 cards, the discard top is her played card, and it is Bob's turn.

**On failure** (wrong color and wrong value) — only Alice receives:
```
ERROR|Card <id> cannot be played on <topId>
```

State is **unchanged**. Alice must try a different card or draw.

---

## 6 — Draw a Card, Then End Turn Without Playing

Bob's turn. Bob draws instead of playing.

**Bob sends:**
```
DRAW_CARD|
```

Bob receives (private):
```
HAND_UPDATE|<id1>,<id2>,...,<newCardId>
```

All clients receive:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Bob,...,players:Alice:6:0,Bob:8:0,Charlie:7:0
```

Phase stays `AWAITING_PLAY` — Bob may still play the drawn card or any other.
Bob decides to pass:

**Bob sends:**
```
END_TURN|
```

All clients receive:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Charlie,...
```

> **Verify:** `currentPlayer` has advanced to Charlie. Bob still has 8 cards.

---

## 7 — Special Effect: SKIP

SKIP cards are **single-color specials** (ids **81–100**, effect = SKIP).

**Current player (Charlie) sends:**
```
PLAY_CARD|<skipCardId>
```

All clients receive:
```
GAME_STATE|phase:RESOLVING_EFFECT,currentPlayer:Charlie,...
```

Only Charlie receives (the acting player must supply arguments):
```
EFFECT_REQUEST|SKIP:Charlie
```

**Charlie names a target:**
```
EFFECT_RESPONSE|SKIP|Alice
```

All clients receive:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Bob,...
```

> **Verify:** Alice's turn was skipped — after Charlie the turn jumped straight to
> Bob, not Alice. Alice's `skipped` flag was consumed automatically.

---

## 8 — Special Effect: FANTASTIC (Four-Color Special)

FANTASTIC cards are **four-color specials** (ids **101–123**, effect = FANTASTIC).
They are always playable during the current player's turn.

**Current player sends:**
```
PLAY_CARD|<fantasticCardId>
```

All clients receive:
```
GAME_STATE|phase:RESOLVING_EFFECT,...
```

Only the acting player receives:
```
EFFECT_REQUEST|FANTASTIC:<playerName>
```

**Respond with a color only:**
```
EFFECT_RESPONSE|FANTASTIC|BLUE
```

**Or with a color and a number:**
```
EFFECT_RESPONSE|FANTASTIC|RED|7
```

All clients receive the state update with the request now active:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:<next>,...
```

> **Verify:** The next player attempts to play a card that does **not** match the
> requested color — they should receive `ERROR|Card ... cannot be played`. Then
> play a matching card — it should succeed.

---

## 9 — Special Effect: GIFT

Current player plays a single-color GIFT card (ids 81–100, effect = GIFT).

Only the acting player receives:
```
EFFECT_REQUEST|GIFT:<playerName>
```

**Acting player responds** with a target and one or two card ids from their own hand:
```
EFFECT_RESPONSE|GIFT|Bob|<cardId1>,<cardId2>
```

Both affected players receive updated `HAND_UPDATE` messages. All clients receive
the public state.

> **Verify:** Acting player's `handSize` in `GAME_STATE` decreased by 1 or 2;
> Bob's increased by the same amount.

---

## 10 — Special Effect: EXCHANGE

Acting player plays a single-color EXCHANGE card.

Only the acting player receives:
```
EFFECT_REQUEST|EXCHANGE:<playerName>
```

**Acting player responds** with a target and exactly two card ids (one from each player — the acting player provides their own card ids; the server picks from the target's hand):
```
EFFECT_RESPONSE|EXCHANGE|Bob|<actorCardId1>,<actorCardId2>
```

> **Verify:** Both players' hand sizes are unchanged, but the specific card ids
> in their `HAND_UPDATE` differ from before.

---

## 11 — Special Effect: EQUALITY

Acting player plays EQUALITY.

Only the acting player receives:
```
EFFECT_REQUEST|EQUALITY:<playerName>
```

**Respond with a target and a color:**
```
EFFECT_RESPONSE|EQUALITY|Charlie|GREEN
```

> **Verify:** Charlie draws cards until their `handSize` matches the acting
> player's. The public state shows an updated `drawPileSize`. The next card
> played must match GREEN.

---

## 12 — Special Effect: COUNTERATTACK (Out-of-Turn)

Player A plays a SKIP targeting Player B. Before the skip resolves, Player B
plays a COUNTERATTACK card (a four-color special; effect = COUNTERATTACK).

**Player B sends immediately (out-of-turn):**
```
PLAY_CARD|<counterattackCardId>
```

Only Player B receives:
```
EFFECT_REQUEST|COUNTERATTACK:Bob
```

**Player B redirects the skip to Charlie:**
```
EFFECT_RESPONSE|COUNTERATTACK|Charlie
```

All clients receive the updated state.

> **Verify:** Charlie is now skipped instead of Bob. Bob's turn proceeds normally.

---

## 13 — Special Effect: NICE_TRY (Out-of-Turn)

When a player plays their last card and the round would end, any other player
may immediately play NICE_TRY (a four-color special; effect = NICE_TRY).

The target (who just emptied their hand) draws 3 cards and the round continues.

**Any other player sends (out-of-turn):**
```
PLAY_CARD|<niceTryCardId>
```

Only that player receives:
```
EFFECT_REQUEST|NICE_TRY:<playerName>
```

**Respond with the target (the player who just emptied their hand):**
```
EFFECT_RESPONSE|NICE_TRY|Alice
```

> **Verify:** Alice now has 3 cards, phase is back to `AWAITING_PLAY`,
> and the round has not ended.

---

## 14 — Special Effect: SECOND_CHANCE

Acting player plays a SECOND_CHANCE card.

Only the acting player receives:
```
EFFECT_REQUEST|SECOND_CHANCE:<playerName>
```

**Option A — play another card from hand (provide its id):**
```
EFFECT_RESPONSE|SECOND_CHANCE|<cardId>
```

**Option B — cannot or does not want to play (empty payload = draw penalty):**
```
EFFECT_RESPONSE|SECOND_CHANCE|
```

> **Verify (A):** The supplied card appears as the new `discardTop` in
> `GAME_STATE`. Hand shrank by 1 (the second-chance card was already removed
> when played, so the hand shrinks by 1 more for the replayed card).
>
> **Verify (B):** Acting player's `handSize` increased by 1 (draw penalty).

---

## 15 — Playing a Black Card (Event Card Trigger)

Black cards have ids **72–80**. A black card is playable when the discard top
has the **same numeric value** and is **not itself a black card**.

**Current player sends:**
```
PLAY_CARD|<blackCardId>
```

All clients receive:
```
GAME_STATE|phase:RESOLVING_EFFECT,...
```

And the event flip notification:
```
GAME|EVENT_CARD_FLIPPED:<eventCardId>
```

The event resolves **automatically** (no player input required for most effects).
Afterwards all clients receive the updated public state. The table below lists
observable outcomes per event card id:

| Event id | Effect name | What to observe |
|---|---|---|
| 0 | ALL_DRAW_TWO | All `handSize` values increase by 2 |
| 1 | ALL_DRAW_ONE | All `handSize` values increase by 1 |
| 2 | ALL_SKIP | All players except the black-card player have `skipped=true`; next two turns are skipped |
| 3 | INSTANT_ROUND_END | `phase:ROUND_END` immediately; `ROUND_END` broadcast follows |
| 4 | REVERSE_ORDER | Player order in subsequent `GAME_STATE` broadcasts is reversed |
| 5 | STEAL_FROM_NEXT | Current player `handSize` +1, next player `handSize` −1 |
| 6 | STEAL_FROM_PREV | Current player `handSize` +1, previous player `handSize` −1 |
| 7 | DISCARD_HIGHEST | Each player's `handSize` decreases by 1; `discardTop` changes |
| 8 | DISCARD_COLOR | All cards matching discard-top color removed from all hands |
| 9 | SWAP_HANDS | Current player and next player swap `handSize` values |
| 10 | BLOCK_SPECIALS | Special cards now return `ERROR` when played (until next black card) |
| 11 | GIFT_CHAIN | Each player's `handSize` is unchanged; card ids in hands rotate |
| 12 | HAND_RESET | All hands cleared and redrawn to 7 cards each |
| 13 | LUCKY_DRAW | Current player `handSize` +3 |
| 14 | PENALTY_DRAW | Player with most cards gets `handSize` +2 |
| 15 | EQUALIZE | All players draw until `handSize` matches the largest hand |
| 16 | WILD_REQUEST | `requestedColor` is set (visible in `GAME_STATE`); next play must match |
| 17 | CANCEL_EFFECTS | Any pending effect chain is cleared |
| 18 | BONUS_PLAY | `phase:AWAITING_PLAY` remains with the same player — they play again |
| 19 | DOUBLE_SCORING | No immediate change; round-end scores are doubled |

---

## 16 — BLOCK_SPECIALS Verification

1. Trigger event card id **10** (play a black card whose value matches the discard top; hope it flips event 10, or arrange a test environment).
2. With BLOCK_SPECIALS active, have any player attempt a special card:
   ```
   PLAY_CARD|<specialSingleOrFourCardId>
   ```
   Expected:
   ```
   ERROR|Card <id> cannot be played on <topId>
   ```
3. Play any **black card**. After it resolves, retry the same special card — it should now succeed (the flag was cleared when the black card was played).

---

## 17 — DOUBLE_SCORING Verification

1. Trigger event card id **19** during a round.
2. Note each player's remaining hand composition at round end.
3. After `ROUND_END`, compare announced `roundScore` values:
   - Example: Alice holds RED/3 (score 3) + GREEN/7 (score 7) → expected round score `(3+7)×2 = 20`.
   - Bob holds BLACK/4 (score `4×2=8`) → expected `8×2 = 16`.

---

## 18 — Round End: Player Empties Hand

When a player plays their last card, all clients receive:

```
GAME_STATE|phase:ROUND_END,...
```

Followed immediately by:

```
ROUND_END|Alice:<roundScore>:<totalScore>,Bob:<roundScore>:<totalScore>,Charlie:<roundScore>:<totalScore>
```

- The player who emptied their hand has `roundScore:0`.
- All others are scored by the sum of `scoringValue()` of cards still in hand:
  - Color card → face value; Black card → value × 2; Single special → 10; Four-color special → 20; Fuck You → 69.

**If no player's `totalScore >= 141`** (maxScore for 3 players = `150 − 3×3`):
A new round starts automatically:
```
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:<name>,...  ← public state, all hands fresh
HAND_UPDATE|...                                          ← each player gets 7 new cards
GAME_STATE|phase:AWAITING_PLAY,...                       ← turn-start update
```

> **Verify round 2 player order:** The player with the **highest** total score
> plays first in round 2, not alphabetically.

---

## 19 — Round End: Draw Pile Exhausted

If any player calls `DRAW_CARD|` when the draw pile is empty, all clients receive:

```
GAME_STATE|phase:ROUND_END,...
ROUND_END|...
```

Round-end scoring and next-round setup follow the same flow as §18.

---

## 20 — Game Over

When any player's `totalScore >= 141` after round-end scoring, all clients receive:

```
ROUND_END|Alice:<roundScore>:<totalScore>,Bob:<roundScore>:<totalScore>,Charlie:<roundScore>:<totalScore>
GAME_END|<winnerName>
```

- `winnerName` = player with the **lowest** cumulative score.
- Alphabetical tie-breaking: if two players have equal lowest scores, the
  alphabetically-first name wins.

After `GAME_END` the lobby resets: game state is cleared, `gameStarted = false`.
Players remain in the lobby and may issue `START|` again to begin a fresh game
with round 1 alphabetical order and all scores at 0.

---

## 21 — Error Conditions

| Scenario | Message to send | Expected response |
|---|---|---|
| Play a card when it is not your turn | `PLAY_CARD|<id>` | `ERROR|Not your turn` |
| Draw a card in wrong phase | `DRAW_CARD|` outside `AWAITING_PLAY` | `ERROR|Cannot draw card in phase ...` |
| Play an invalid card (wrong color/value) | `PLAY_CARD|<id>` | `ERROR|Card <id> cannot be played on <topId>` |
| Play a card not in your hand | `PLAY_CARD|9999` | `ERROR|...` (card not found) |
| Start game with fewer than 2 players | `START|` alone in lobby | `ERROR|Need at least 2 players` |
| Start game already in progress | `START|` during active game | `ERROR|Game already started` |
| Send unknown message type | `BOGUS|hello` | `ERROR|Malformed protocol message.` |
| Send `GAME_STATE` (server-only type) | `GAME_STATE|anything` | `ERROR|...` |
| Send `PLAY_CARD` without content | `PLAY_CARD|` | Treated as card id `""` → `ERROR|Card -1 ...` |

---

## 22 — Chat Commands (Sanity Check During Game)

Verify pre-existing chat features still work while a game is active:

**Global chat (all connected clients, all lobbies):**
```
GLOBALCHAT|Hello everyone!
```
All clients receive:
```
GLOBALCHAT|Alice|Hello everyone!
```

**Lobby chat (only lobby members):**
```
LOBBYCHAT|Good luck!
```
Lobby members receive:
```
LOBBYCHAT|Alice|Good luck!
```

**Whisper (private):**
```
WHISPERCHAT|Bob|Nice hand!
```
Bob receives:
```
WHISPERCHAT|FROM|Alice|Nice hand!
```
Alice receives:
```
WHISPERCHAT|TO|Bob|Nice hand!
```

---

## 23 — Full Happy-Path Quick Reference

```
# ── Setup (run once) ──────────────────────────────────────────────────
# Client 1        Client 2        Client 3
NAME|Alice         NAME|Bob        NAME|Charlie
CREATE|game1       JOIN|game1      JOIN|game1
START|             (any client)

# All receive: GAME_STATE + HAND_UPDATE (×3, private) + GAME_STATE

# ── Round play (Alice → Bob → Charlie → Alice → …) ───────────────────
PLAY_CARD|<id>     # Alice plays;  turn → Bob
                   DRAW_CARD|      # Bob draws
                   END_TURN|       # Bob passes; turn → Charlie
                                   PLAY_CARD|<id>   # Charlie; turn → Alice
# … repeat until someone empties their hand …

# All receive: GAME_STATE(phase:ROUND_END) + ROUND_END|...

# If game not over → new round starts automatically
# If game over     → GAME_END|<winnerName>
```
