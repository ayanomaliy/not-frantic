# Game Logic Implementation Plan

## Overview

The networking layer is complete. Everything needed is server-side in `ServerService` and new model classes under `model/`. The protocol uses `TYPE|content` messages — game actions will extend this with new `Message.Type` variants.

Base path for all files: `src/main/java/ch/unibas/dmi/dbis/cs108/example/`
Test path: `src/test/java/ch/unibas/dmi/dbis/cs108/example/`

---

## Phase 1 — Card Model

**Goal:** Define all card types as an immutable data model.

### Step 1.1 — `CardColor` enum
Create `model/game/CardColor.java`:
```
RED, GREEN, BLUE, YELLOW, BLACK
```

### Step 1.2 — `CardType` enum
Create `model/game/CardType.java`:
```
COLOR, BLACK, SPECIAL_SINGLE, SPECIAL_FOUR, FUCK_YOU, EVENT
```

### Step 1.3 — `SpecialEffect` enum
Create `model/game/SpecialEffect.java`:
```
// Single-color effects:
SECOND_CHANCE, SKIP, GIFT, EXCHANGE
// Four-color effects:
FANTASTIC, FANTASTIC_FOUR, EQUALITY, COUNTERATTACK, NICE_TRY
```

### Step 1.4 — `Card` record
Create `model/game/Card.java`:
```java
record Card(
    int id,             // unique card id (0..124 for main deck)
    CardType type,
    CardColor color,    // null for four-color specials / event cards
    int value,          // 1-9 for color/black; 0 for specials
    SpecialEffect effect,  // null for non-special cards
    int scoringValue    // precomputed from rules
)
```

**Scoring rules baked in at construction:**
- Color card → `value`
- Black card → `value * 2`
- Single-color special → `10`
- Four-color special → `20`
- Fuck You → `69`
- Event card → `0`

### Intermediary Tests — `CardTest.java`
- [ ] Color card RED/5 has `scoringValue == 5`
- [ ] Black card value 3 has `scoringValue == 6`
- [ ] Single-color SKIP card has `scoringValue == 10`
- [ ] Four-color FANTASTIC card has `scoringValue == 20`
- [ ] Fuck You card has `scoringValue == 69`
- [ ] Event card has `scoringValue == 0`

---

## Phase 2 — Deck Generation

**Goal:** Produce the correct 125-card main deck and 20-card event deck.

### Step 2.1 — `DeckFactory` class
Create `model/game/DeckFactory.java` with static methods:
```java
static List<Card> buildMainDeck()   // 125 cards
static List<Card> buildEventDeck()  // 20 cards
static void shuffle(List<Card> deck, Random rng)
```

**Main deck composition:**
- 72 color cards: 4 colors × values 1–9 × 2 copies → IDs 0–71
- 9 black cards: values 1–9 → IDs 72–80
- 20 single-color special cards (5 per color × 4 effects) → IDs 81–100
- 23 four-color special cards (distributed among 5 effects) → IDs 101–123
- 1 Fuck You card → ID 124

> **Note:** Agree on the exact distribution of the 23 four-color specials among the 5 effect types
> (e.g., 5 Fantastic, 5 Fantastic Four, 5 Equality, 4 Counterattack, 4 Nice Try = 23).

### Intermediary Tests — `DeckFactoryTest.java`
- [ ] `buildMainDeck()` returns exactly 125 cards
- [ ] Exactly 72 `COLOR` type cards
- [ ] Exactly 9 `BLACK` type cards with values 1–9 (no duplicates)
- [ ] Exactly 20 `SPECIAL_SINGLE` cards (5 per color)
- [ ] Exactly 23 `SPECIAL_FOUR` cards
- [ ] Exactly 1 `FUCK_YOU` card
- [ ] All card IDs in main deck are unique (0–124)
- [ ] `buildEventDeck()` returns exactly 20 cards, all of type `EVENT`
- [ ] After `shuffle()`, same cards exist but in different order (seed test)

---

## Phase 3 — Game State Model

**Goal:** A single `GameState` object that owns all mutable game state.

### Step 3.1 — `PlayerGameState` class
Create `model/game/PlayerGameState.java`:
```java
class PlayerGameState {
    String playerName;
    List<Card> hand;
    int totalScore;
    boolean skipped;           // skip-next-turn flag
    boolean hasPlayedThisTurn;
}
```

### Step 3.2 — `GamePhase` enum
Create `model/game/GamePhase.java`:
```
WAITING, TURN_START, AWAITING_PLAY, RESOLVING_EFFECT, ROUND_END, GAME_OVER
```

### Step 3.3 — `GameState` class
Create `model/game/GameState.java`:
```java
class GameState {
    List<PlayerGameState> playerOrder;   // ordered, wraps around
    int currentPlayerIndex;
    GamePhase phase;

    Deque<Card> drawPile;
    Deque<Card> discardPile;
    Deque<Card> eventPile;

    Card activeEventCard;      // null if no active event
    int maxScore;

    // Request state (set by Fantastic / Fantastic Four / Equality)
    CardColor requestedColor;
    Integer requestedNumber;

    // Pending effects (for counterattack chains, etc.)
    Deque<SpecialEffect> pendingEffects;
    String pendingEffectTarget;  // player name
}
```

### Step 3.4 — `GameInitializer` class
Create `model/game/GameInitializer.java`:
```java
static GameState initialize(
    List<String> playerNames,
    int roundNumber,
    Map<String, Integer> previousScores,
    Random rng
)
```

**Initialization logic:**
1. Build + shuffle both decks
2. Compute `maxScore = 150 - (3 * playerCount)`
3. Order players: alphabetical (round 1) or by previous score descending (round 2+)
4. Deal 7 cards to each player from draw pile
5. Dealer (server) flips top card from draw pile to discard pile
6. Set `phase = TURN_START`, `currentPlayerIndex = 0`

### Intermediary Tests — `GameInitializerTest.java`
- [ ] With 3 players, `maxScore == 141`
- [ ] With 4 players, `maxScore == 138`
- [ ] Each player starts with exactly 7 cards
- [ ] Draw pile starts with `125 - (7 * playerCount) - 1` cards
- [ ] Discard pile has exactly 1 card
- [ ] Event pile has 20 cards
- [ ] Round 1 player order is alphabetical
- [ ] Round 2 order is by descending score (highest score plays first)
- [ ] Phase is `TURN_START`

---

## Phase 4 — Card Play Validation

**Goal:** Determine whether a given card is legally playable given the current state.

### Step 4.1 — `CardValidator` class
Create `model/game/CardValidator.java`:
```java
static boolean canPlay(Card card, Card topOfDiscard, GameState state)
static boolean canPlayOutOfTurn(Card card, GameState state)
```

**Rules matrix:**

| Card type        | Allowed when                                                                 |
|------------------|------------------------------------------------------------------------------|
| `COLOR`          | Same color as top OR same number as top OR matches `requestedColor`/`requestedNumber` |
| `BLACK`          | Same number as top AND top is NOT a black card                               |
| `SPECIAL_SINGLE` | Same color as top OR same symbol (effect) OR matches `requestedColor`        |
| `SPECIAL_FOUR`   | Always during your turn                                                      |
| `FUCK_YOU`       | Player has **exactly** 10 cards                                              |

**Additional constraints:**
- If `requestedColor` is set, color cards must match it (overrides color-on-color)
- If `requestedNumber` is set, color cards must match it
- `canPlayOutOfTurn` returns `true` only for `COUNTERATTACK` and `NICE_TRY` under correct conditions

### Intermediary Tests — `CardValidatorTest.java`
- [ ] RED/5 is valid on RED/3 (same color)
- [ ] RED/5 is valid on BLUE/5 (same number)
- [ ] RED/5 is invalid on BLUE/3
- [ ] BLACK/4 valid on RED/4 (same number, top is not black)
- [ ] BLACK/4 invalid on BLACK/4 (cannot play black on black)
- [ ] Single-color RED SKIP valid on RED/7 (same color)
- [ ] Single-color RED SKIP valid on GREEN SKIP (same symbol)
- [ ] Single-color RED SKIP invalid on BLUE/3
- [ ] Four-color FANTASTIC always valid during turn
- [ ] FUCK_YOU valid only when hand size is exactly 10
- [ ] FUCK_YOU invalid when hand size is 9 or 11
- [ ] If `requestedColor = BLUE`, RED/5 is invalid even on RED/3
- [ ] If `requestedColor = BLUE`, BLUE/3 is valid
- [ ] COUNTERATTACK can be played out-of-turn
- [ ] NICE_TRY can be played out-of-turn
- [ ] FANTASTIC cannot be played out-of-turn

---

## Phase 5 — Turn Engine

**Goal:** Execute a single turn, handling draw, play, and effect trigger.

### Step 5.1 — `GameEvent` class
Create `model/game/GameEvent.java` — a tagged event used for broadcasting:
```
// Types: CARD_PLAYED, CARD_DRAWN, TURN_ADVANCED, EFFECT_TRIGGERED,
//        ROUND_ENDED, GAME_OVER, EVENT_CARD_FLIPPED
```

### Step 5.2 — `TurnEngine` class
Create `model/game/TurnEngine.java`:
```java
// Returns list of GameEvents describing what happened (for broadcasting)
static List<GameEvent> playCard(GameState state, String playerName, Card card)
static List<GameEvent> drawCard(GameState state, String playerName)
static List<GameEvent> endTurn(GameState state)
```

**`playCard` logic:**
1. Validate it is `playerName`'s turn and `phase == AWAITING_PLAY`
2. Call `CardValidator.canPlay()` — reject if invalid
3. Remove card from player's hand
4. Add to discard pile
5. If card is `BLACK` → flip top event card, set `activeEventCard`, resolve event
6. Trigger card's special effect if applicable
7. Check round-end conditions
8. If not ended → advance to next non-skipped player

**`drawCard` logic:**
1. Draw 1 card from draw pile into player's hand
2. If draw pile is empty → round ends immediately
3. After draw, player may still play if drawn card is valid (do NOT auto-advance)

**`endTurn` logic:**
1. Clear `hasPlayedThisTurn`
2. Advance `currentPlayerIndex` (skip skipped players, clear their flag)
3. Set `phase = TURN_START`

### Intermediary Tests — `TurnEngineTest.java`
- [ ] Playing a valid card removes it from hand and places it on discard
- [ ] Playing an invalid card returns an error event and leaves state unchanged
- [ ] After playing, it is not the same player's turn (unless SECOND_CHANCE)
- [ ] Drawing a card adds it to the player's hand
- [ ] Drawing when draw pile is empty triggers `ROUND_ENDED` event
- [ ] Playing a BLACK card triggers event card flip
- [ ] Skipped player is skipped automatically on `endTurn`
- [ ] FUCK_YOU card can only be played with exactly 10 cards in hand
- [ ] Playing last card triggers `ROUND_ENDED` event

---

## Phase 6 — Special Effect Resolution

**Goal:** Fully resolve each special card effect.

### Step 6.1 — `EffectArgs` class
Create `model/game/EffectArgs.java` to carry optional parameters:
```java
class EffectArgs {
    String targetPlayer;
    CardColor chosenColor;
    Integer chosenNumber;
    List<Card> selectedCards;  // for Gift / Exchange
}
```

### Step 6.2 — `EffectResolver` class
Create `model/game/EffectResolver.java`:
```java
static List<GameEvent> resolve(
    SpecialEffect effect,
    GameState state,
    String actingPlayer,
    EffectArgs args
)
```

**Effect implementations:**

| Effect          | Logic                                                                                      |
|-----------------|--------------------------------------------------------------------------------------------|
| `SECOND_CHANCE` | Actor must play another card immediately; if impossible, draw 1                            |
| `SKIP`          | Actor names a target; target's `skipped = true`                                            |
| `GIFT`          | Give 2 cards (or 1 if only 1 remains) from hand to named player                           |
| `EXCHANGE`      | Swap 2 named cards with target; target's card identities are hidden to the actor           |
| `FANTASTIC`     | Set `requestedColor` and/or `requestedNumber`                                              |
| `FANTASTIC_FOUR`| Distribute 4 cards from draw pile among players; then set request                         |
| `EQUALITY`      | Target draws until hand size equals actor's size; then actor sets `requestedColor`         |
| `COUNTERATTACK` | Cancel the top `pendingEffect`; re-apply it to a new target                               |
| `NICE_TRY`      | Force the player who just emptied their hand to draw 3 new cards                          |

### Intermediary Tests — `EffectResolverTest.java`
- [ ] SKIP marks target player as `skipped = true`
- [ ] GIFT transfers 2 cards from actor to target; hand sizes change correctly
- [ ] GIFT with only 1 card in hand transfers just 1
- [ ] EXCHANGE swaps exactly 2 cards between players
- [ ] FANTASTIC sets `requestedColor`
- [ ] FANTASTIC_FOUR distributes 4 cards and sets request
- [ ] EQUALITY: target draws until equal hand size
- [ ] COUNTERATTACK cancels pending effect and redirects to new target
- [ ] NICE_TRY: player who just played last card draws 3 (no longer at 0)
- [ ] SECOND_CHANCE: actor must play again; if impossible, draws 1
- [ ] Chain: two COUNTERATTACKs cancel each other and re-apply to original target

---

## Phase 7 — Round & Score Calculation

**Goal:** Compute scores at round end and determine game end.

### Step 7.1 — `ScoreCalculator` class
Create `model/game/ScoreCalculator.java`:
```java
static Map<String, Integer> calculateRoundScores(List<PlayerGameState> players)
static boolean isGameOver(Map<String, Integer> totalScores, int maxScore)
static String getWinner(Map<String, Integer> totalScores)
```

**Logic:**
- Each player's round score = sum of `card.scoringValue()` for cards remaining in hand
- Add to `totalScore` in `PlayerGameState`
- Game over if any player's `totalScore >= maxScore`
- Winner = player with **lowest** total score

### Intermediary Tests — `ScoreCalculatorTest.java`
- [ ] Empty hand = 0 score
- [ ] Hand with RED/3, BLACK/4 = 3 + 8 = 11
- [ ] Hand with one FANTASTIC_FOUR card = 20
- [ ] Game not over when all scores below maxScore
- [ ] Game over when one player reaches or exceeds maxScore
- [ ] Winner is player with lowest total score
- [ ] With 4 players, maxScore is 138

---

## Phase 8 — Protocol Integration

**Goal:** Wire game actions to the server's message protocol.

### Step 8.1 — New `Message.Type` values
Add to the `Type` enum in `service/Message.java`:
```
PLAY_CARD, DRAW_CARD, END_TURN,
GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, EFFECT_RESPONSE,
ROUND_END, GAME_END
```

**Protocol format examples:**
```
PLAY_CARD|<cardId>
DRAW_CARD|
EFFECT_RESPONSE|SKIP|targetPlayerName
EFFECT_RESPONSE|FANTASTIC|BLUE|5
GAME_STATE|<delimited-state-payload>
HAND_UPDATE|cardId1,cardId2,...
```

### Step 8.2 — Wire into `ServerService`
In `service/ServerService.java`:
1. Add `Map<String, GameState> lobbyGameStates` field
2. In `handleStart()` — call `GameInitializer.initialize()`, store `GameState`
3. Add `handlePlayCard()`, `handleDrawCard()`, `handleEffectResponse()` methods
4. After each action, call `broadcastGameState()` to all players in the lobby

### Step 8.3 — `Lobby` model extension
Extend `model/Lobby.java`:
```java
GameState gameState;                      // null until game starts
int currentRound;
Map<String, Integer> cumulativeScores;
```

### Intermediary Tests — `ProtocolIntegrationTest.java`
- [ ] `PLAY_CARD|42` parses to correct card id
- [ ] `EFFECT_RESPONSE|SKIP|alice` parses to effect + target
- [ ] Playing a card server-side updates `GameState` and broadcasts `GAME_STATE` to all clients
- [ ] Invalid play returns `ERROR` message to acting client only
- [ ] After round end, `ROUND_END` broadcast contains all player scores
- [ ] After game end, `GAME_END` broadcast contains winner name

---

## Phase 9 — Event Cards

**Goal:** Implement the 20 event cards triggered by black cards.

> **Note:** The game design document does not specify the 20 event effects by name.
> The team must define all event card effects before beginning this phase
> (e.g., "all players draw 2", "reverse direction", "instant round end", etc.).

### Step 9.1 — `EventEffect` enum
Create `model/game/EventEffect.java` with all 20 defined event types.

### Step 9.2 — `EventResolver` class
Create `model/game/EventResolver.java`:
```java
static List<GameEvent> resolve(Card eventCard, GameState state)
```

### Intermediary Tests — `EventResolverTest.java`
- [ ] Each event type resolves without exception
- [ ] Events that affect multiple players do so correctly
- [ ] Round-ending events set `phase = ROUND_END`
- [ ] `activeEventCard` is cleared after resolution
- [ ] Black card always triggers event card flip

---

## Phase 10 — End-to-End Integration Test

**Goal:** Simulate a full game programmatically to verify all phases interact correctly.

### `FullRoundSimulationTest.java`
- [ ] Initialize a 3-player game
- [ ] Simulate turns until round end condition is met
- [ ] Verify scores are calculated and added to totals
- [ ] Start round 2 with correct player order (by descending score)
- [ ] Simulate until a player reaches maxScore
- [ ] Verify correct winner is declared (lowest total score)
- [ ] Verify `phase == GAME_OVER` at the end

---

## Implementation Order

```
Phase 1  →  Phase 2  →  Phase 3
                             |
                        Phase 4 (validation)
                             |
                        Phase 5 (turn engine)
                             |
                        Phase 6 (effects)   <-- Phase 9 (events, parallel)
                             |
                        Phase 7 (scoring)
                             |
                        Phase 8 (protocol)
                             |
                        Phase 10 (E2E test)
```

Phases 4–7 are pure game logic with no networking dependency and can be tested fully in isolation before touching `ServerService`. Phase 8 is the only place where game logic meets the protocol layer.
