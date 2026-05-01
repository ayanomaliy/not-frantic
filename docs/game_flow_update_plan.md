# Game flow update plan

## Game flow logic (High Level)

1. During any round, if any player has 0 cards left, the round ends.
2. After rounds ends "round points" get assign to each player based on the cards they have.
3. Those round points then get added to each players "game points" count.
4. If someones game points exceed a predefined limit the game ends and the winner (player with the least game points) is proclaimed.
5. Otherwise, the game gets reset and a new round starts. The only thing that remains from the previous round is the log of the round points and the game points.
6. All the previous games get displayed on the global leader board stored on the server

---

## Current State Analysis

### Root cause of the single-round bug

`ServerService.handleRoundEnd()` contains a deliberate shortcut: it always ends the game after
one round, ignoring `ScoreCalculator.isGameOver()`. The key line is:

```java
// End the whole game immediately after this round   ← the comment that explains the bug
String winner = ScoreCalculator.getWinner(lobby.getCumulativeScores());
```

All the multi-round infrastructure already exists and is correct:

- `ScoreCalculator.isGameOver()` and `getWinner()` — implemented and unit-tested
- `GameInitializer.initialize()` — supports round 2+ with score carryover and player reordering
- `Lobby.nextRound()` — already called in `handleStart()` (round=1)
- `FullRoundSimulationTest` — validates the full model-layer multi-round flow

### What the client does today at round end

When `ROUND_END` arrives: scores are parsed and stored in `ClientState.getFinalScoreRows()`.
The boolean `gameViewShown` stays `true`, so when the next `GAME_STATE` arrives (for round 2),
the `gameStartListener` is not re-fired — GameView stays visible and its state updates silently.
There is no "between rounds" screen.

---

## Implementation Plan

### Phase 1 — Server: Fix round transition (the critical bug)

#### Step 1.1 — Refactor `handleRoundEnd()` in `ServerService`

**File:** `src/main/java/.../service/ServerService.java`

Replace the always-end-game body with a split into two private helpers. The new logic:

```text
handleRoundEnd(lobby):
  1. calculateRoundScores (consumes DOUBLE_SCORING flag if active)
  2. persist cumulative totals into lobby.getCumulativeScores()
  3. broadcast ROUND_END | <scores payload>
  4. if isGameOver(cumulativeScores, maxScore):
       call endGame(lobby)
     else:
       call startNextRound(lobby)

endGame(lobby):
  1. getWinner(cumulativeScores)
  2. highScoreHistory.appendFinishedGame(...)
  3. broadcast GAME_END | <winnerName>
  4. state.setPhase(GAME_OVER)
  5. lobby.setGameState(null)
  6. lobby.setGameStarted(false)
  7. lobby.setLobbyStatus(FINISHED)
  8. broadcastLobbyListToAllClients()

startNextRound(lobby):
  1. nextRound = lobby.nextRound()
  2. collect playerNames from lobby.getSessions()
  3. newState = GameInitializer.initialize(playerNames, nextRound, cumulativeScores, new Random())
  4. lobby.setGameState(newState)
  5. broadcast NEXT_ROUND | <nextRound>         <- new message type (see Step 1.2)
  6. broadcastAllHands(lobby)
  7. List<GameEvent> events = TurnEngine.startTurn(newState)
  8. broadcastEvents(lobby, events)
  9. broadcastGameState(lobby)
```

Guard at the top of `handleRoundEnd`: return early if `state.getPhase() != ROUND_END`
to prevent accidental double-calls.

#### Step 1.2 — Add `NEXT_ROUND` to the message protocol

**File:** `src/main/java/.../service/Message.java`

Add `NEXT_ROUND` to the `Type` enum.

**Purpose:** Tells clients that a new round is starting (distinct from the very first game start).
This allows the client to reset UI state specifically for a new round without conflating it with
the initial game start.

**Format:** `NEXT_ROUND|<roundNumber>` — e.g. `NEXT_ROUND|2`

No payload parsing is needed server-side; the server only sends it.

#### Step 1.3 — Add round number to the `GAME_STATE` broadcast

**File:** `src/main/java/.../service/GameStateSerializer.java`

Add `round:<N>` to the public game-state payload so clients can display "Round N" in the HUD
without relying on a separate counter.

**File:** `src/main/java/.../model/game/GameState.java`

Add `private int roundNumber` field with getter, set by `GameInitializer.initialize()`.

**File:** `src/main/java/.../model/game/GameInitializer.java`

Set `state.setRoundNumber(roundNumber)` after building the state.

---

### Phase 2 — Client: Between-rounds transition

#### Step 2.1 — Reset `gameViewShown` in `FxNetworkClient` on `ROUND_END`

**File:** `src/main/java/.../client/net/FxNetworkClient.java`

In the `ROUND_END` case handler, add:

```java
gameViewShown = false;
```

This allows the `gameStartListener` to fire again when the next `GAME_STATE` arrives,
which triggers `MainController.showGameView()` — but since GameView is already shown,
the guard `!(stage.getScene().getRoot() instanceof GameView)` prevents a screen flicker.
What actually needs to happen is the round results overlay appearing, not a full view switch.

Also add a `roundEndListener` callback field and invoke it here, to decouple logic:

```java
if (roundEndListener != null) roundEndListener.run();
```

#### Step 2.2 — Handle `NEXT_ROUND` in `FxNetworkClient`

Add a `newRoundListener` callback field. In `onMessage()`:

```java
case NEXT_ROUND -> Platform.runLater(() -> {
    int roundNum = Integer.parseInt(message.content());
    state.setCurrentRound(roundNum);
    state.getGameMessages().add("[ROUND] Starting round " + roundNum + "...");
    if (newRoundListener != null) newRoundListener.run();
});
```

Add `currentRound` as an `IntegerProperty` to `ClientState`.

#### Step 2.3 — Add `currentRound` to `ClientState`

**File:** `src/main/java/.../client/ClientState.java`

```java
private final IntegerProperty currentRound = new SimpleIntegerProperty(1);
public IntegerProperty currentRoundProperty() { return currentRound; }
public int getCurrentRound() { return currentRound.get(); }
public void setCurrentRound(int round) { currentRound.set(round); }
```

#### Step 2.4 — Implement `RoundResultsOverlay`

**File (new):** `src/main/java/.../client/ui/RoundResultsOverlay.java`

A JavaFX `StackPane` that sits on top of the game board. Shows:

- "Round N complete!" heading
- A table of per-player round scores and cumulative totals (from `ClientState.getFinalScoreRows()`)
- A "Next round starting..." label that auto-hides when the new `GAME_STATE` arrives

The overlay is constructed with the score rows list and a `Runnable onDismiss`.
It should fade in on creation and fade out on dismiss.

It is **not** a separate screen — it is an overlay within `GameView`.

#### Step 2.5 — Integrate `RoundResultsOverlay` into `GameView`

**File:** `src/main/java/.../client/ui/GameView.java`

Add methods:

```java
public void showRoundResults(ObservableList<String> scoreRows) { ... }
public void hideRoundResults() { ... }
```

`showRoundResults` creates and adds a `RoundResultsOverlay` to the root StackPane of GameView.
`hideRoundResults` plays a fade-out and then removes it.

#### Step 2.6 — Wire listeners in `MainController`

**File:** `src/main/java/.../client/ui/MainController.java`

In the constructor (after setting `gameStartListener` and `gameEndListener`):

```java
networkClient.setRoundEndListener(() -> {
    if (stage.getScene().getRoot() instanceof GameView gv) {
        gv.showRoundResults(state.getFinalScoreRows());
    }
});

networkClient.setNewRoundListener(() -> {
    if (stage.getScene().getRoot() instanceof GameView gv) {
        gv.hideRoundResults();
    }
});
```

The `gameStartListener` check already guards against re-showing the GameView:

```java
if (stage.getScene() == null || !(stage.getScene().getRoot() instanceof GameView)) {
    showGameView();
}
```

This means for round 2+, when `GAME_STATE` arrives and fires `gameStartListener`,
the screen stays as-is and only the game state updates — which is correct.

---

### Phase 3 — Robustness and edge cases

#### Step 3.1 — Player disconnect during an active game

**File:** `src/main/java/.../service/ServerService.java`

Find the `handleDisconnect()` method and add:

```text
if (lobby.isGameStarted() && lobby.getGameState() != null):
  if (lobby.getPlayerCount() < 2):
    // Not enough players to continue — abort the round
    broadcast ERROR | "Game aborted: a player disconnected."
    lobby.setGameState(null)
    lobby.setGameStarted(false)
    lobby.setLobbyStatus(FINISHED)
  else if disconnected player was current player:
    // Auto-advance the turn so the game is not stuck
    TurnEngine.endTurn(state)
    broadcastGameState(lobby)
```

This prevents the game from freezing if a player drops mid-round.

#### Step 3.2 — Draw pile exhaustion ends the round correctly

**File:** `src/main/java/.../model/game/TurnEngine.java`

Verify `drawCard()` sets phase to `ROUND_END` when the draw pile is empty.
This is already implemented — confirm in `TurnEngine` and in `handleRoundEnd()` that
the empty-pile-end path goes through the same `handleRoundEnd()` method.

#### Step 3.3 — Double scoring flag consumed before game-over check

**Already correct** — `ScoreCalculator.calculateRoundScores(players, state)` consumes the
`doubleScoringActive` flag in the same call that computes scores. But verify this is the
overload being called in `handleRoundEnd()` (with the `GameState` parameter, not without).
The current code passes `state`, so this is correct.

#### Step 3.4 — Lobby reuse after `FINISHED`

After a game ends (`status = FINISHED`), players cannot join the lobby. If the goal is to
allow a "rematch", a new lobby must be created. Document this as intentional in `Lobby.java`'s
class Javadoc. No code change needed — just make the decision explicit.

---

### Phase 4 — Tests

All new tests must be deterministic (seeded `Random`) and test a single behavior per method.

#### Step 4.1 — `ServerService` round-transition unit tests

**File:** `src/test/java/.../service/ServerServiceTest.java`

New test methods:

```text
handleRoundEnd_noGameOver_broadcastsRoundEndThenNextRoundThenGameState()
  Setup: lobby with 2 players, cumulative scores = {Alice:10, Bob:15}, maxScore=144
  Action: call handleRoundEnd(lobby)
  Assert:
    - ROUND_END message was broadcast
    - NEXT_ROUND message was broadcast with round=2
    - GAME_STATE message was broadcast (new state)
    - GAME_END was NOT broadcast
    - lobby.getCurrentRound() == 2
    - new game state has phase == TURN_START
    - each player has 7 cards in hand

handleRoundEnd_gameOver_broadcastsRoundEndThenGameEnd()
  Setup: lobby with 2 players, cumulative scores above maxScore after this round
  Action: call handleRoundEnd(lobby)
  Assert:
    - ROUND_END message was broadcast
    - GAME_END message was broadcast with correct winner (lowest score)
    - NEXT_ROUND was NOT broadcast
    - lobby.getLobbyStatus() == FINISHED
    - lobby.getGameState() == null

handleRoundEnd_doubleScoring_scoresDoubledBeforeGameOverCheck()
  Setup: round scores that would not trigger game-over without doubling,
         but do trigger game-over with doubling active
  Action: set doubleScoringActive=true, call handleRoundEnd(lobby)
  Assert:
    - game-over path taken (GAME_END broadcast)

handleRoundEnd_roundScores_cumulativeScoresCorrectlyAccumulated()
  Setup: after round 1, Alice has totalScore=30, Bob has totalScore=20
         round 2 ends: Alice hand value=10, Bob hand value=5
  Action: call handleRoundEnd(lobby) for round 2
  Assert:
    - lobby.getCumulativeScores().get("Alice") == 40
    - lobby.getCumulativeScores().get("Bob") == 25

handleRoundEnd_playerOrderInNextRound_descendingByCumulativeScore()
  Setup: after round 1 ends, Alice=50, Bob=30
  Action: handleRoundEnd starts round 2
  Assert:
    - round 2 player order is [Alice, Bob] (Alice has more penalty points, plays first)
```

#### Step 4.2 — `ScoreCalculator` edge case tests

**File:** `src/test/java/.../model/game/ScoreCalculatorTest.java`

Add tests for missing edge cases:

```text
isGameOver_exactlyAtMaxScore_returnsTrue()
isGameOver_oneBelow_returnsFalse()
isGameOver_emptyScoreMap_returnsFalse()
getWinner_tie_returnsAlphabeticalFirst()
  Players "Bob" and "Alice" tied at 50 -> winner is "Alice"
calculateRoundScores_doubleScoringFlagConsumedAfterCall()
  Assert state.isDoubleScoringActive() == false after the call
calculateRoundScores_doubleScoring_totalScoreDoubled()
  Hand value = 10, doubleScoringActive=true -> totalScore += 20
```

#### Step 4.3 — `GameInitializer` multi-round tests

**File:** `src/test/java/.../model/game/GameInitializerTest.java`

Add tests for:

```text
round2_playerOrderByDescendingCumulativeScore()
  Alice=100, Bob=50, Charlie=75 -> order: [Alice, Charlie, Bob]
round2_tieInScore_alphabeticalTieBreak()
  Alice=50, Bob=50 -> order: [Alice, Bob] (alphabetical among tied)
round2_cumulativeScoresPreloadedIntoPlayerState()
  previousScores = {Alice:30, Bob:20}
  -> state.getPlayer("Alice").getTotalScore() == 30
round2_eachPlayerHas7CardsInHand()
roundNumber_storedInGameState()
  Initialize with roundNumber=3 -> state.getRoundNumber() == 3
```

#### Step 4.4 — `ServerService` multi-round integration test

**File (new):** `src/test/java/.../service/ServerServiceMultiRoundTest.java`

A higher-level integration test that wires `ServerService` with mock `ClientSession` objects,
captures all messages sent, and drives a minimal game to completion over multiple rounds.
Use a scenario where max scores are reached quickly (small starting hands with high-value cards).

```text
fullGame_messageSequence_roundEndBeforeEachNewRound()
  Setup: 2-player game, dev scenario that makes rounds end quickly
  Action: simulate play until GAME_END is received
  Assert:
    - For each round except the last: ROUND_END -> NEXT_ROUND -> HAND_UPDATE -> GAME_STATE
    - For the final round: ROUND_END -> GAME_END
    - GAME_END payload is the correct winner name
    - The number of ROUND_END messages == the number of rounds played

fullGame_winner_isPlayerWithLowestTotalScore()
```

#### Step 4.5 — Client-side `FxNetworkClient` round-handling tests

**File:** `src/test/java/.../client/net/FxNetworkClientTest.java`

Add:

```text
onMessage_ROUND_END_resetsGameViewShownFlag()
  After ROUND_END: gameViewShown field == false

onMessage_ROUND_END_firesRoundEndListener()

onMessage_NEXT_ROUND_updatesClientStateCurrentRound()
  NEXT_ROUND|3 -> state.getCurrentRound() == 3

onMessage_NEXT_ROUND_firesNewRoundListener()

onMessage_GAME_STATE_afterRound_firesGameStartListenerOnlyIfGameViewNotShown()
  Simulate: GAME_STATE (round 1) -> ROUND_END -> GAME_STATE (round 2)
  Assert gameStartListener fired exactly once (for round 1 only,
  because showGameView's instanceof guard prevents re-show in round 2)
```

---

### Execution Order

Implement in this order to keep the main branch green at every step:

1. **Step 1.1** — Fix `handleRoundEnd()` (the single most impactful change; server now works end-to-end for multi-round games without any client changes)
2. **Step 4.1** — Write `ServerServiceTest` round-transition tests (lock in the correct behavior)
3. **Step 4.2** — Add `ScoreCalculator` edge case tests
4. **Step 4.3** — Add `GameInitializer` multi-round tests
5. **Step 1.2** — Add `NEXT_ROUND` message type to `Message.Type` and server broadcast
6. **Step 1.3** — Add `roundNumber` to `GameState` and `GAME_STATE` serialization
7. **Step 2.3** — Add `currentRound` to `ClientState`
8. **Step 2.2** — Handle `NEXT_ROUND` in `FxNetworkClient`
9. **Step 2.1** — Reset `gameViewShown` on `ROUND_END`, add listener hooks
10. **Step 2.6** — Wire `roundEndListener` / `newRoundListener` in `MainController`
11. **Step 2.4 + 2.5** — Implement `RoundResultsOverlay` and integrate into `GameView`
12. **Step 4.5** — Add `FxNetworkClient` round-handling tests
13. **Step 4.4** — Write `ServerServiceMultiRoundTest` integration test
14. **Step 3.1** — Handle player disconnect during active game
15. **Step 3.4** — Document lobby reuse decision in Javadoc

---

### Architecture invariants to preserve

- `TurnEngine`, `EffectResolver`, `EventResolver`, `ScoreCalculator`, `GameInitializer` remain
  stateless pure functions — no `ServerService` or `Lobby` references.
- `GameState` holds only one round's worth of state. Cumulative scores live in `Lobby`.
- The `handleRoundEnd()` → `startNextRound()` → `broadcastGameState()` path is the single
  authoritative transition point. No other code path should call `GameInitializer.initialize()`
  outside of `handleStart()` and `startNextRound()`.
- All server state mutations happen inside `synchronized` methods in `ServerService`
  (the existing synchronization model is sufficient — do not introduce per-lobby locks).

## Review

### What was changed and why

#### `ServerService.java` — `handleRoundEnd()` refactored

The method previously called `ScoreCalculator.getWinner()` unconditionally after every round,
ending the game after round 1 regardless of cumulative scores.

It was split into three private methods:

- **`handleRoundEnd(lobby)`** — computes and broadcasts `ROUND_END` scores, then checks
  `ScoreCalculator.isGameOver()`. Routes to `endGame` or waits for a `START_NEXT_ROUND` request.
- **`endGame(lobby)`** — broadcasts `GAME_END`, writes to the high-score history, resets
  lobby state to `FINISHED`.
- **`startNextRound(lobby)`** — calls `GameInitializer.initialize()` with the carried-over
  cumulative scores, broadcasts `NEXT_ROUND|<n>`, sends updated hands and game state.

Why: the game was always ending after one round because the end-game path was unconditional.

#### `ServerService.java` — `unregisterClient()` extended

After removing the disconnecting player from the lobby, the method now checks whether an
active game has been disrupted:

- If fewer than 2 players remain → broadcasts an `ERROR` message, sets the game state to
  `null`, and marks the lobby `FINISHED`.
- If the disconnected player was the current player → sets `hasDrawnThisTurn` to bypass
  `TurnEngine`'s "must have acted" guard, calls `TurnEngine.endTurn()`, then either triggers
  `handleRoundEnd()` or starts the next player's turn.

Why: without this, the game would freeze waiting for input from a player who is no longer connected.

#### `Message.java` — `NEXT_ROUND` and `START_NEXT_ROUND` added to `Type`

`NEXT_ROUND` is sent server → client to signal a new round is starting, carrying the round
number as payload. `START_NEXT_ROUND` is sent client → server by any lobby member to trigger
the transition after the round-results overlay is dismissed.

Why: the client needed a way to distinguish "first game start" from "subsequent round start"
to avoid re-rendering the game view, and players needed an explicit trigger to begin the next
round rather than it starting automatically.

#### `GameState.java` — `roundNumber` field added

A `private int roundNumber` field with getter and setter was added to `GameState`.
`GameInitializer.initialize()` sets it from the `roundNumber` parameter.

Why: provides a single authoritative source for the current round number that travels with
the game state rather than being inferred separately on the client.

#### `GameStateSerializer.java` — `round:<N>` added to `GAME_STATE` payload

The serialized `GAME_STATE` message now includes `round:<N>` so the client HUD can display
the current round number without maintaining a separate counter.

#### `GameInitializer.java` — round number and cumulative score carryover

`initialize()` now accepts `roundNumber` and `previousScores` parameters. It preloads each
player's `totalScore` from `previousScores` and orders players by descending cumulative score
(highest score plays first in subsequent rounds). It also sets `state.setRoundNumber()`.

Why: the model layer already supported this; the initializer just wasn't wired up to use it.

#### `ClientState.java` — `currentRound` IntegerProperty added

A `SimpleIntegerProperty currentRound` was added with `currentRoundProperty()`,
`getCurrentRound()`, and `setCurrentRound()`. It is updated from both `GAME_STATE` and
`NEXT_ROUND` messages in `FxNetworkClient`.

Why: provides a bindable property so the HUD can display the round number reactively.

#### `FxNetworkClient.java` — `ROUND_END` and `NEXT_ROUND` handling added

- `ROUND_END` handler: resets `gameViewShown = false` so the next `GAME_STATE` can re-trigger
  `gameStartListener` if needed, then fires `roundEndListener`.
- `NEXT_ROUND` handler: updates `state.currentRound`, logs a message, fires `nextRoundListener`.
- `sendStartNextRound()` method added: sends a `START_NEXT_ROUND` message to the server.
- `setRoundEndListener()` and `setNextRoundListener()` setters added.

Why: the client had no handling for these two new message types and no callbacks for the
controller to hook into.

#### `RoundResultsOverlay.java` — new class

A `StackPane` overlay displayed over `GameView` at round end. Shows a "Round Complete!"
title, a `ListView` of per-player round and cumulative scores, and two buttons: "Leave Lobby"
(removes player from lobby and navigates back) and "Next Round" (sends `START_NEXT_ROUND`).
The overlay fades in on creation and fades out when `dismiss(Runnable)` is called.

Why: replaced the immediate game-over screen with an intermediate between-rounds screen that
gives players visibility into scores before deciding to continue or leave.

#### `GameView.java` — `showRoundResults()` and `hideRoundResults()` added

`showRoundResults(scoreRows, onStartNextRound, onLeaveLobby)` creates a `RoundResultsOverlay`,
binds its size to the root `StackPane`, and adds it on top of the game board.
`hideRoundResults()` plays the fade-out animation and then removes the overlay from the scene.

#### `MainController.java` — `roundEndListener` and `nextRoundListener` wired

`roundEndListener` calls `gv.showRoundResults(...)` with `networkClient::sendStartNextRound`
and `this::leaveCurrentLobbyAndShowLobbyView` as button callbacks.
`nextRoundListener` calls `gv.hideRoundResults()` so the overlay disappears when the new
round's `GAME_STATE` arrives.

#### `Lobby.java` — class Javadoc updated

Added a paragraph documenting that `FINISHED` is a terminal lifecycle state and that a new
lobby must be created for a subsequent game.

---

### Tests added or extended

| File | Tests added |
| --- | --- |
| `ServerServiceTest.java` | Round-transition tests: no-game-over path, game-over path, double-scoring, cumulative score accumulation, player order in next round |
| `ScoreCalculatorTest.java` | Edge cases: exact max score, one below, empty map, tie winner, double-scoring flag consumed, doubled total score |
| `GameInitializerTest.java` | Multi-round: player order by descending score, alphabetical tie-break, cumulative score preload, 7-card hand, round number stored |
| `GameStateSerializerTest.java` | `round:<N>` present in `GAME_STATE` payload |
| `FxNetworkClientTest.java` | `ROUND_END` resets `gameViewShown`, fires listener; `NEXT_ROUND` updates `currentRound`, fires listener |
| `ServerServiceMultiRoundTest.java` | Full 2-round integration: message ordering, winner determination |
