# Game Logic Changelog

All phases were implemented server-side under
`src/main/java/ch/unibas/dmi/dbis/cs108/example/` with corresponding tests
under `src/test/java/ch/unibas/dmi/dbis/cs108/example/`.

---

## Phase 1 — Card Model

**Files created:**

- `model/game/CardColor.java` — enum: `RED, GREEN, BLUE, YELLOW, BLACK`
- `model/game/CardType.java` — enum: `COLOR, BLACK, SPECIAL_SINGLE, SPECIAL_FOUR, FUCK_YOU, EVENT`
- `model/game/SpecialEffect.java` — enum: `SECOND_CHANCE, SKIP, GIFT, EXCHANGE, FANTASTIC, FANTASTIC_FOUR, EQUALITY, COUNTERATTACK, NICE_TRY`
- `model/game/Card.java` — immutable record with fields `id, type, color, value, effect`; `scoringValue()` method; static factory methods `colorCard`, `blackCard`, `specialSingleCard`, `specialFourCard`, `fuckYouCard`, `eventCard`

**Scoring rules baked into `scoringValue()`:**

| Card type | Score |
|---|---|
| COLOR | face value |
| BLACK | value × 2 |
| SPECIAL_SINGLE | 10 |
| SPECIAL_FOUR | 20 |
| FUCK_YOU | 69 |
| EVENT | 0 |

**Tests created:** `CardTest.java` — 6 tests verifying scoring values per card type.

---

## Phase 2 — Deck Generation

**Files created:**

- `model/game/DeckFactory.java`
  - `buildMainDeck()` — produces 125 cards (72 color, 9 black, 20 single-color special, 23 four-color special, 1 Fuck You); ids 0–124
  - `buildEventDeck()` — produces 20 event cards; ids 0–19
  - `shuffle(List<Card>, Random)` — Fisher-Yates in-place shuffle using a seeded `Random`

**Tests created:** `DeckFactoryTest.java` — 9 tests covering exact card counts per type, id uniqueness, and shuffle determinism with a fixed seed.

---

## Phase 3 — Game State Model

**Files created:**

- `model/game/PlayerGameState.java` — per-player mutable state: `hand`, `totalScore`, `skipped`, `hasPlayedThisTurn`; two constructors (fresh and carry-over score)
- `model/game/GamePhase.java` — enum: `WAITING, TURN_START, AWAITING_PLAY, RESOLVING_EFFECT, ROUND_END, GAME_OVER`
- `model/game/GameState.java` — full round state: `playerOrder`, `currentPlayerIndex`, `phase`, three pile deques (`drawPile`, `discardPile`, `eventPile`), `activeEventCard`, `maxScore`, `requestedColor`, `requestedNumber`, `pendingEffects`, `pendingEffectTarget`; pile accessor methods; `getPlayer(name)` lookup
- `model/game/GameInitializer.java`
  - `initialize(playerNames, roundNumber, previousScores, rng)` — builds and shuffles both decks, computes `maxScore = 150 − (3 × playerCount)`, orders players (alphabetical for round 1; descending total score for round 2+), deals 7 cards each, flips one card to discard

**Tests created:** `GameInitializerTest.java` — 9 tests: maxScore formula, hand sizes, pile sizes, event pile size, round 1 alphabetical order, round 2 score-descending order, initial phase.

---

## Phase 4 — Card Play Validation

**Files created:**

- `model/game/CardValidator.java`
  - `canPlay(card, topOfDiscard, state)` — per-type rules matrix
  - `canPlayOutOfTurn(card, state)` — only COUNTERATTACK and NICE_TRY

**Rules:**

| Card type | Condition |
|---|---|
| COLOR | If request active: must satisfy at least one active request. Otherwise: same color or same value as top |
| BLACK | Same value as top AND top is not BLACK |
| SPECIAL_SINGLE | If color request active: card color must match. Otherwise: same color or same effect as top |
| SPECIAL_FOUR | Always playable during your turn |
| FUCK_YOU | Only when hand size is exactly 10 |
| EVENT | Never playable from hand |

**Tests created:** `CardValidatorTest.java` — 16 tests covering all card types, request overrides, and out-of-turn rules.

---

## Phase 5 — Turn Engine

**Files created:**

- `model/game/GameEvent.java` — immutable record `(EventType type, String detail)`; event types: `CARD_PLAYED, CARD_DRAWN, TURN_ADVANCED, EFFECT_TRIGGERED, EVENT_CARD_FLIPPED, ROUND_ENDED, GAME_OVER, ERROR`; static convenience factories
- `model/game/TurnEngine.java`
  - `startTurn(state)` — transitions `TURN_START → AWAITING_PLAY`
  - `playCard(state, playerName, card)` — validates turn/phase/legality; removes card from hand, pushes to discard; routes by card type: COLOR/FUCK_YOU auto-calls `endTurn`; BLACK flips event card and sets `RESOLVING_EFFECT`; SPECIAL_SINGLE/FOUR pushes effect and sets `RESOLVING_EFFECT`; checks for empty-hand round-end before routing
  - `drawCard(state, playerName)` — draws one card; triggers `ROUND_END` if draw pile is empty; phase stays `AWAITING_PLAY`
  - `endTurn(state)` — advances to next non-skipped player (consuming skip flags); sets `TURN_START`

**Tests created:** `TurnEngineTest.java` — 9 tests: valid play, invalid play, turn advance, draw, empty-draw-pile round end, black-card event flip, skip consumption, Fuck You size guard, empty-hand round end.

---

## Phase 6 — Special Effect Resolution

**Files created:**

- `model/game/EffectArgs.java` — immutable parameter bag for effect resolution; fields: `targetPlayer`, `chosenColor`, `chosenNumber`, `selectedCards`; static factories: `empty()`, `withTarget()`, `withColor()`, `withColorAndNumber()`, `withTargetAndColor()`, `withTargetAndCards()`, `withCards()`, `of()`
- `model/game/EffectResolver.java` — `resolve(effect, state, actingPlayer, args)` dispatching to per-effect handlers:

| Effect | Behaviour |
|---|---|
| SKIP | Sets `args.targetPlayer.skipped = true`; calls `endTurn` |
| GIFT | Transfers 1–2 cards from actor to target; calls `endTurn` |
| EXCHANGE | Swaps two cards between actor and target; calls `endTurn` |
| FANTASTIC | Sets `requestedColor` and/or `requestedNumber`; calls `endTurn` |
| FANTASTIC_FOUR | Deals 4 cards from draw pile round-robin starting from next player; sets request; calls `endTurn` |
| EQUALITY | Target draws until hand size equals actor's; sets `requestedColor`; calls `endTurn` |
| COUNTERATTACK | Pops top pending effect; sets new `pendingEffectTarget`; does NOT call `endTurn` (phase stays `RESOLVING_EFFECT`) |
| NICE_TRY | Target draws 3 cards; calls `endTurn` (un-ROUND_ENDs) |
| SECOND_CHANCE | If `selectedCards` non-empty: replays first card directly onto discard. Otherwise: draws 1 (penalty). Calls `endTurn` |

- `model/game/EventEffect.java` *(Phase 9 stub, created here)* — enum of 20 constants (ordinals 0–19 = event card ids); `fromCardId(int)` lookup
- `model/game/EventResolver.java` *(Phase 9 stub, created here)* — `resolve(eventCard, state)` dispatching to 20 stub handlers each returning `EVENT_STUB:<name>` marker events; `activeEventCard` always cleared after dispatch

**Tests created:**

- `EffectResolverTest.java` — 22 tests covering all 9 effects including edge cases (GIFT with 1 card, EQUALITY draws, COUNTERATTACK chain, NICE_TRY un-round-end, SECOND_CHANCE with and without card)
- `EventResolverTest.java` *(stub smoke tests)* — parametrized: all 20 event ids dispatch without exception, clear `activeEventCard`, return non-empty event list; `fromCardId` range guards; stub-marker verification

**Bug fixed during testing:** `skip_marksTargetAsSkipped` — when the immediate-next player is targeted, `endTurn` consumes their skip and lands on the player after. Fix: test targets a non-adjacent player; separate test `skip_immediateNextPlayer_skipConsumedByEndTurn` covers the consumed-flag case.

---

## Phase 7 — Round & Score Calculation

**Files created:**

- `model/game/ScoreCalculator.java`
  - `calculateRoundScores(players)` — sums `card.scoringValue()` per player, adds delta to `totalScore`, returns unmodifiable `LinkedHashMap` preserving player order
  - `isGameOver(totalScores, maxScore)` — `true` if any score `>= maxScore`
  - `getWinner(totalScores)` — lowest total score; alphabetical tie-breaking

**Tests created:** `ScoreCalculatorTest.java` — 20 tests: per-card-type scoring, accumulation across rounds, multi-player map, unmodifiable return, isGameOver at/above/below threshold, getWinner with clear winner, tie-break, single player, empty map, maxScore formula for 2/3/4 players.

---

## Phase 8 — Protocol Integration

**Files modified:**

- `service/Message.java`
  - Added 9 new `Type` enum values: `PLAY_CARD`, `DRAW_CARD`, `END_TURN`, `EFFECT_RESPONSE` (client → server); `GAME_STATE`, `HAND_UPDATE`, `EFFECT_REQUEST`, `ROUND_END`, `GAME_END` (server → client)
  - Updated `parseType`, `expectsContent`, `hasValidStructure` to cover all new types
  - `DRAW_CARD` and `END_TURN` require blank content; server-only types return an error if sent by a client

- `model/Lobby.java`
  - Added fields: `gameState` (live `GameState`), `currentRound` (1-based), `cumulativeScores` (`Map<String, Integer>`)
  - Added accessors: `getGameState/setGameState`, `getCurrentRound`, `nextRound()` (increments and returns), `getCumulativeScores()` (live map)

- `service/ServerService.java`
  - `handleStart` — real implementation: guards (must be in lobby, not started, ≥ 2 players), calls `GameInitializer.initialize`, stores state, broadcasts `GAME_STATE` + private `HAND_UPDATE` per player, calls `TurnEngine.startTurn`, broadcasts updated `GAME_STATE`
  - `handlePlayCard` — parses card id, finds card in hand, calls `TurnEngine.playCard`, broadcasts events; calls `handleRoundEnd` on `ROUND_END` phase
  - `handleDrawCard` — calls `TurnEngine.drawCard`, broadcasts events; calls `handleRoundEnd` on `ROUND_END` phase
  - `handleEndTurn` — calls `TurnEngine.endTurn`, broadcasts updated state
  - `handleEffectResponse` — parses via `GameMessageParser.parseEffectResponse`, calls `EffectResolver.resolve`, broadcasts events; calls `handleRoundEnd` on `ROUND_END` phase
  - `handleRoundEnd` — calls `ScoreCalculator.calculateRoundScores`, broadcasts `ROUND_END`; if game over broadcasts `GAME_END` and resets lobby; otherwise initialises next round
  - `broadcastGameState`, `broadcastAllHands`, `broadcastEvents` — broadcast helpers

**Files created:**

- `service/GameStateSerializer.java` — four pure serialization methods:
  - `serializePublicState(state)` → `phase:X,currentPlayer:Y,discardTop:Z,drawPileSize:N,players:A:handSize:total,...`
  - `serializeHand(player)` → comma-separated card ids
  - `serializeRoundEnd(roundScores, players)` → `name:roundScore:totalScore,...`
  - `serializeEffectRequest(effectName, actingPlayer)` → `EFFECT_NAME:playerName`

- `service/GameMessageParser.java` — two pure parse methods:
  - `parsePlayCard(payload)` → card id integer or `-1`
  - `parseEffectResponse(payload, state, actingPlayer)` → `[effectName, EffectArgs]` or `null`; handles all 9 `SpecialEffect` variants

**Tests created:** `ProtocolIntegrationTest.java` — 38 tests: `Message.parse` for all 9 new types, `hasValidStructure`, encode/parse round-trips, `parsePlayCard` (valid, whitespace, non-numeric, empty), `parseEffectResponse` for all handled effects, `GameStateSerializer` field presence and format, game-flow pipeline (valid/invalid play, draw), score accumulation, game-over detection, winner selection.

**Bug fixed during testing:** `roundEnd_scoresAccumulateAcrossRounds` — test forgot to clear Alice's hand between rounds (mirroring what `GameInitializer` does). Fixed by adding `alice.getHand().clear()` between the two round calculations.

---

## Phase 9 — Event Cards

**Files modified:**

- `model/game/GameState.java`
  - Added `specialsBlocked` flag (set by BLOCK_SPECIALS; cleared when a BLACK card is played)
  - Added `doubleScoringActive` flag (set by DOUBLE_SCORING; consumed by `ScoreCalculator`)
  - Added getters/setters for both flags

- `model/game/CardValidator.java`
  - `SPECIAL_SINGLE` and `SPECIAL_FOUR` cases now check `state.isSpecialsBlocked()`; returns `false` when blocked

- `model/game/TurnEngine.java`
  - Playing a BLACK card calls `state.setSpecialsBlocked(false)` before flipping the event card

- `model/game/ScoreCalculator.java`
  - New overload `calculateRoundScores(players, state)` — reads `doubleScoringActive`; multiplies every round score by 2 if set; clears the flag afterwards

- `model/game/EventEffect.java` *(promoted from stub to final)*
  - All 20 constants confirmed with agreed-upon rules

- `model/game/EventResolver.java` *(all 20 stub handlers replaced)*

| Id | Effect | Implementation |
|---|---|---|
| 0 | ALL_DRAW_TWO | Every player draws 2 from draw pile |
| 1 | ALL_DRAW_ONE | Every player draws 1 from draw pile |
| 2 | ALL_SKIP | All players except current get `skipped = true` |
| 3 | INSTANT_ROUND_END | `state.setPhase(ROUND_END)`; emits `ROUND_ENDED` event |
| 4 | REVERSE_ORDER | `Collections.reverse(playerOrder)`; updates `currentPlayerIndex` so the same player stays current |
| 5 | STEAL_FROM_NEXT | Current player takes last card from next player's hand |
| 6 | STEAL_FROM_PREV | Current player takes last card from previous player's hand |
| 7 | DISCARD_HIGHEST | Each player discards their highest-scoring card to discard pile; triggers ROUND_END if any hand becomes empty |
| 8 | DISCARD_COLOR | Each player discards all cards matching discard-top color; triggers ROUND_END if any hand becomes empty |
| 9 | SWAP_HANDS | Current player swaps entire hand with next player |
| 10 | BLOCK_SPECIALS | `state.setSpecialsBlocked(true)` |
| 11 | GIFT_CHAIN | Each player simultaneously passes their last hand card to the next player; total card count unchanged |
| 12 | HAND_RESET | All hands discarded to discard pile; everyone redraws 7 cards from draw pile |
| 13 | LUCKY_DRAW | Current player draws 3 extra cards |
| 14 | PENALTY_DRAW | Player with most cards draws 2 more |
| 15 | EQUALIZE | All players draw until hand size matches the current maximum hand size |
| 16 | WILD_REQUEST | Sets `requestedColor` to the color of the current discard top (falls back to RED if top has no color) |
| 17 | CANCEL_EFFECTS | Clears `pendingEffects` deque and `pendingEffectTarget` |
| 18 | BONUS_PLAY | Sets `phase = AWAITING_PLAY` — same player may play one additional card |
| 19 | DOUBLE_SCORING | Sets `state.setDoubleScoringActive(true)`; multiplier applied at round-end scoring |

**Tests created/replaced:** `EventResolverTest.java` — old stub-marker test removed; 31 new behaviour tests plus the preserved parametrized smoke tests (60 in 3×20 parametrized set); total 60+ assertions covering all 20 effects, flag interactions, and the `ScoreCalculator` doubling overload.

---

## Test Count Summary

| Phase | Test class | Tests |
|---|---|---|
| 1 | CardTest | 6 |
| 2 | DeckFactoryTest | 9 |
| 3 | GameInitializerTest | 9 |
| 4 | CardValidatorTest | 16 |
| 5 | TurnEngineTest | 9 |
| 6 | EffectResolverTest | 22 |
| 6 | EventResolverTest (stubs) | 23 |
| 7 | ScoreCalculatorTest | 20 |
| 8 | ProtocolIntegrationTest | 38 |
| 9 | EventResolverTest (real) | 60+ |
| **Total** | | **313** |

All 313 tests pass with `./gradlew test`.
