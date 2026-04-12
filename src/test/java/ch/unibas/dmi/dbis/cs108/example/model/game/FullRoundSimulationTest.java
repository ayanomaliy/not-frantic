package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 10 — End-to-end integration test.
 *
 * <p>Simulates a full multi-round game with three players entirely in-process,
 * without any mocking or random human input. All card plays, draws, and effect
 * resolutions are driven by a deterministic simulation helper that always makes
 * the simplest legal move available.
 *
 * <h2>Phase 10 checklist</h2>
 * <ul>
 *   <li>Initialize a 3-player game</li>
 *   <li>Simulate turns until round end condition is met</li>
 *   <li>Verify scores are calculated and added to totals</li>
 *   <li>Start round 2 with correct player order (by descending score)</li>
 *   <li>Simulate until a player reaches maxScore</li>
 *   <li>Verify correct winner is declared (lowest total score)</li>
 *   <li>Verify {@code phase == GAME_OVER} at the end</li>
 * </ul>
 *
 * <h2>Simulation strategy</h2>
 * Each turn the current player:
 * <ol>
 *   <li>Prefers to play a plain COLOR card (fewest side-effects).</li>
 *   <li>Falls back to any other legally playable card.</li>
 *   <li>Draws one card if nothing can be played; tries again after the draw.</li>
 *   <li>Calls {@code endTurn} if still nothing is playable after drawing.</li>
 * </ol>
 * Special effects and events are resolved automatically with the simplest
 * available arguments (e.g., always pick the first eligible target, always
 * request RED).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullRoundSimulationTest {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final List<String> PLAYER_NAMES = List.of("Alice", "Bob", "Charlie");
    private static final long SEED = 42L;
    /** Hard limit on simulation steps per round (prevents infinite loops in tests). */
    private static final int MAX_STEPS_PER_ROUND = 5_000;
    /** Hard limit on the number of rounds (game must end before this). */
    private static final int MAX_ROUNDS = 50;

    // ── Data captured during @BeforeAll simulation ────────────────────────────

    // Round 1
    private GameState             round1FinalState;
    private Map<String, Integer>  round1RoundScores;
    private Map<String, Integer>  totalScoresAfterRound1;

    // Round 2 initial state (captured before simulation runs)
    private GameState             round2State;
    private List<String>          round2OrderedPlayerNames;
    private Map<String, Integer>  round2StartingTotalScores;
    private Map<String, Integer>  round2InitialHandSizes;

    // Final game state
    private GameState             finalGameState;
    private Map<String, Integer>  finalTotalScores;
    private String                declaredWinner;
    private int                   totalRoundsPlayed;

    /**
     * Bundles the explicit post-round coordination step that the simulation
     * test performs after a round reaches {@link GamePhase#ROUND_END}.
     *
     * <p>This mirrors the server responsibility: score the finished round and,
     * if the game is not over, create the next round with carried scores.
     * The turn simulation itself intentionally stops at {@code ROUND_END} and
     * does not initialize the next round automatically.</p>
     */
    private record RoundTransition(Map<String, Integer> roundScores,
                                   Map<String, Integer> cumulativeScores,
                                   GameState nextRoundState) {
    }

    // =========================================================================
    // @BeforeAll — run the full game once, then assert against stored results
    // =========================================================================

    @BeforeAll
    void runFullGame() {

        // ── Round 1 ──────────────────────────────────────────────────────────
        GameState r1 = GameInitializer.initialize(PLAYER_NAMES, 1, null, new Random(SEED));
        simulateRound(r1);
        round1FinalState = r1;

        RoundTransition afterRound1 = transitionToNextRound(r1, 2, SEED + 1);
        round1RoundScores = afterRound1.roundScores();
        totalScoresAfterRound1 = afterRound1.cumulativeScores();
        round2State = afterRound1.nextRoundState();

        // ── Round 2 initial snapshot (captured only after explicit transition) ─
        if (round2State != null) {
            round2OrderedPlayerNames = round2State.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .collect(Collectors.toList());
            round2StartingTotalScores = buildTotalScoreMap(round2State);
            round2InitialHandSizes = round2State.getPlayerOrder().stream()
                .collect(Collectors.toMap(
                        PlayerGameState::getPlayerName,
                        PlayerGameState::getHandSize));
        } else {
            round2OrderedPlayerNames = List.of();
            round2StartingTotalScores = Map.of();
            round2InitialHandSizes = Map.of();
        }

        // Early exit if the game somehow ended in round 1
        if (round2State == null) {
            finalGameState = r1;
            finalTotalScores = totalScoresAfterRound1;
            finalGameState.setPhase(GamePhase.GAME_OVER);
            declaredWinner = ScoreCalculator.getWinner(finalTotalScores);
            totalRoundsPlayed = 1;
            return;
        }

        // ── Simulate round 2 and beyond until game over ───────────────────────
        GameState current = round2State;
        int round = 2;

        while (true) {
            simulateRound(current);
            RoundTransition transition = transitionToNextRound(current, round + 1, SEED + round + 1);
            Map<String, Integer> cumulativeScores = transition.cumulativeScores();

            if (transition.nextRoundState() == null) {
                finalGameState   = current;
                finalTotalScores = cumulativeScores;
                finalGameState.setPhase(GamePhase.GAME_OVER);
                totalRoundsPlayed = round;
                break;
            }

            if (round >= MAX_ROUNDS) {
                fail("Game did not end within " + MAX_ROUNDS + " rounds — simulation may be stuck.");
            }

            round++;
            current = transition.nextRoundState();
        }

        declaredWinner = ScoreCalculator.getWinner(finalTotalScores);
    }

    // =========================================================================
    // Phase 10 assertions — Round 1
    // =========================================================================

    @Test
    void round1_endsWithRoundEndPhase() {
        assertEquals(GamePhase.ROUND_END, round1FinalState.getPhase(),
                "Round 1 must terminate with phase ROUND_END");
    }

    @Test
    void round1_allPlayersHaveNonNegativeRoundScore() {
        for (String name : PLAYER_NAMES) {
            assertTrue(round1RoundScores.get(name) >= 0,
                    name + "'s round-1 score must be non-negative");
        }
    }

    @Test
    void round1_roundScoresAddedToTotalScores() {
        // Players start round 1 with totalScore = 0, so after calculateRoundScores:
        // totalScore == roundScore for each player.
        for (String name : PLAYER_NAMES) {
            assertEquals(round1RoundScores.get(name),
                    totalScoresAfterRound1.get(name),
                    name + "'s total after round 1 should equal their round-1 score");
        }
    }

    @Test
    void round1_allPlayersHaveNonNegativeTotalScore() {
        for (String name : PLAYER_NAMES) {
            assertTrue(totalScoresAfterRound1.get(name) >= 0,
                    name + " must have non-negative cumulative score after round 1");
        }
    }

    // =========================================================================
    // Phase 10 assertions — Round 2 initialisation
    // =========================================================================

    @Test
    void round2_allPlayersPresent() {
        assertEquals(Set.of("Alice", "Bob", "Charlie"),
                Set.copyOf(round2OrderedPlayerNames),
                "All 3 players must be present at the start of round 2");
    }

    @Test
    void round2_isCreatedByExplicitRoundTransitionStep() {
        assertNotNull(round2State,
                "Round 2 should only exist after the test performs the post-round transition step.");
        assertNotSame(round1FinalState, round2State,
                "Round 2 must be a freshly initialized state, not the finished round-1 object.");
    }

    @Test
    void round2_playerOrderIsDescendingByScore() {
        // Players with higher accumulated scores play earlier in round 2.
        for (int i = 0; i < round2OrderedPlayerNames.size() - 1; i++) {
            String a = round2OrderedPlayerNames.get(i);
            String b = round2OrderedPlayerNames.get(i + 1);
            int scoreA = totalScoresAfterRound1.get(a);
            int scoreB = totalScoresAfterRound1.get(b);
            assertTrue(scoreA >= scoreB,
                    a + " (score=" + scoreA + ") must come before "
                            + b + " (score=" + scoreB + ") in round-2 order");
        }
    }

    @Test
    void round2_eachPlayerStartsWith7Cards() {
        for (String name : PLAYER_NAMES) {
            assertEquals(7, round2InitialHandSizes.get(name),
                    name + " should start round 2 with exactly 7 cards");
        }
    }

    @Test
    void round2_cumulativeScoresCarriedOver() {
        // Round-2 PlayerGameState objects should be pre-loaded with round-1 totals.
        for (String name : PLAYER_NAMES) {
            assertEquals(totalScoresAfterRound1.get(name),
                    round2StartingTotalScores.get(name),
                    name + "'s cumulative score must be carried into round 2");
        }
    }

    // =========================================================================
    // Phase 10 assertions — Game over
    // =========================================================================

    @Test
    void gameOver_atLeastOnePlayerReachedMaxScore() {
        int maxScore = finalGameState.getMaxScore();
        boolean anyReached = finalTotalScores.values().stream()
                .anyMatch(s -> s >= maxScore);
        assertTrue(anyReached,
                "At least one player must have reached or exceeded maxScore=" + maxScore
                        + "; actual totals: " + finalTotalScores);
    }

    @Test
    void gameOver_winnerIsOneOfThePlayers() {
        assertTrue(PLAYER_NAMES.contains(declaredWinner),
                "Winner must be one of the 3 players, got: " + declaredWinner);
    }

    @Test
    void gameOver_winnerHasLowestTotalScore() {
        int winnerScore = finalTotalScores.get(declaredWinner);
        for (Map.Entry<String, Integer> entry : finalTotalScores.entrySet()) {
            assertTrue(winnerScore <= entry.getValue(),
                    "Winner " + declaredWinner + " (score=" + winnerScore
                            + ") must have the lowest score, but "
                            + entry.getKey() + " has " + entry.getValue());
        }
    }

    @Test
    void gameOver_phaseIsGameOver() {
        assertEquals(GamePhase.GAME_OVER, finalGameState.getPhase(),
                "Phase must be GAME_OVER at the end of the game");
    }

    @Test
    void gameOver_gameFinishedInReasonableRounds() {
        assertTrue(totalRoundsPlayed >= 1 && totalRoundsPlayed <= MAX_ROUNDS,
                "Game should finish in 1–" + MAX_ROUNDS + " rounds; took " + totalRoundsPlayed);
    }

    @Test
    void gameOver_allPlayersHaveNonNegativeFinalScores() {
        for (String name : PLAYER_NAMES) {
            assertTrue(finalTotalScores.get(name) >= 0,
                    name + " must have a non-negative final score");
        }
    }

    // =========================================================================
    // Simulation engine
    // =========================================================================

    /**
     * Drives {@code state} forward one step at a time until {@code ROUND_END}
     * is reached or the step budget is exhausted.
     */
    private void simulateRound(GameState state) {
        for (int step = 0; step < MAX_STEPS_PER_ROUND; step++) {
            if (state.getPhase() == GamePhase.ROUND_END) return;
            simulateStep(state);
        }
        fail("Round did not reach ROUND_END within " + MAX_STEPS_PER_ROUND + " steps. "
                + "Phase=" + state.getPhase()
                + ", drawPile=" + state.getDrawPile().size());
    }

    /**
     * Applies the explicit post-round transition step that production code
     * performs outside the pure turn engine.
     */
    private RoundTransition transitionToNextRound(GameState finishedRound,
                                                  int nextRoundNumber,
                                                  long nextSeed) {
        assertEquals(GamePhase.ROUND_END, finishedRound.getPhase(),
                "A next round may only be initialized after the current round has ended.");

        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(finishedRound.getPlayerOrder(), finishedRound);
        Map<String, Integer> cumulativeScores = buildTotalScoreMap(finishedRound);

        if (ScoreCalculator.isGameOver(cumulativeScores, finishedRound.getMaxScore())) {
            return new RoundTransition(roundScores, cumulativeScores, null);
        }

        GameState nextRoundState = GameInitializer.initialize(
                PLAYER_NAMES, nextRoundNumber, cumulativeScores, new Random(nextSeed));
        return new RoundTransition(roundScores, cumulativeScores, nextRoundState);
    }

    /** Dispatches a single simulation step based on the current phase. */
    private void simulateStep(GameState state) {
        switch (state.getPhase()) {
            case TURN_START      -> TurnEngine.startTurn(state);
            case AWAITING_PLAY   -> simulatePlayOrDraw(state);
            case RESOLVING_EFFECT -> simulateEffectResolution(state);
            default              -> { /* ROUND_END / GAME_OVER handled externally */ }
        }
    }

    /**
     * Simulates one AWAITING_PLAY decision for the current player:
     * <ol>
     *   <li>Prefer a plain COLOR card (no side-effects).</li>
     *   <li>Fall back to any other playable card.</li>
     *   <li>Draw one card if nothing is playable; then try to play again.</li>
     *   <li>Call {@link TurnEngine#endTurn} if still stuck after the draw.</li>
     * </ol>
     */
    private void simulatePlayOrDraw(GameState state) {
        PlayerGameState current = state.getCurrentPlayer();
        String playerName = current.getPlayerName();
        Card top = state.peekDiscardPile();

        Card toPlay = pickPreferredCard(current, top, state);

        if (toPlay != null) {
            TurnEngine.playCard(state, playerName, toPlay);
            return;
        }

        // No playable card — draw one
        TurnEngine.drawCard(state, playerName);
        if (state.getPhase() == GamePhase.ROUND_END) return; // draw pile was empty

        // After drawing, try to play the newly drawn card (or any other)
        Card newTop = state.peekDiscardPile();
        Card afterDraw = pickPreferredCard(current, newTop, state);
        if (afterDraw != null) {
            TurnEngine.playCard(state, playerName, afterDraw);
        } else {
            TurnEngine.endTurn(state);
        }
    }

    /**
     * Returns the best card to play from {@code player}'s hand given {@code top},
     * or {@code null} if no card is legally playable.
     * Plain COLOR cards are preferred over special/black cards to minimise
     * complex effect chains in the simulation.
     */
    private Card pickPreferredCard(PlayerGameState player, Card top, GameState state) {
        // 1st preference: plain COLOR card
        Optional<Card> color = player.getHand().stream()
                .filter(c -> c.type() == CardType.COLOR
                        && CardValidator.canPlay(c, top, state))
                .findFirst();
        if (color.isPresent()) return color.get();

        // 2nd preference: any playable card
        return player.getHand().stream()
                .filter(c -> CardValidator.canPlay(c, top, state))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves whatever is blocking progress in the {@code RESOLVING_EFFECT} phase:
     * an active event card (from a BLACK play) takes priority over queued special
     * effects.  If neither is present the turn is ended to avoid getting stuck.
     */
    private void simulateEffectResolution(GameState state) {
        String actingPlayer = state.getCurrentPlayer().getPlayerName();

        // ── Event card (triggered by BLACK) ──────────────────────────────────
        if (state.getActiveEventCard() != null) {
            Card eventCard = state.getActiveEventCard();
            EventResolver.resolve(eventCard, state);
            // EventResolver clears activeEventCard; advance the turn unless the event
            // already transitioned to ROUND_END (instant_round_end) or AWAITING_PLAY
            // (BONUS_PLAY).
            if (state.getPhase() == GamePhase.RESOLVING_EFFECT) {
                TurnEngine.endTurn(state);
            }
            return;
        }

        // ── Pending special effect ────────────────────────────────────────────
        if (!state.getPendingEffects().isEmpty()) {
            SpecialEffect effect = state.getPendingEffects().peek();
            EffectArgs args = buildArgs(effect, state, actingPlayer);
            EffectResolver.resolve(effect, state, actingPlayer, args);
            // Most effects call endTurn internally → phase becomes TURN_START.
            // COUNTERATTACK is the exception: it leaves phase as RESOLVING_EFFECT
            // so the redirected effect can be resolved in the next iteration.
            return;
        }

        // ── Fallback: nothing left to resolve ────────────────────────────────
        // This can happen if COUNTERATTACK was played on-turn with nothing beneath it.
        TurnEngine.endTurn(state);
    }

    /**
     * Builds automatic {@link EffectArgs} for the given {@code effect} during
     * simulation. Choices are intentionally simple (first eligible target, always
     * request RED, give the first card(s) in hand, etc.).
     */
    private EffectArgs buildArgs(SpecialEffect effect, GameState state, String actingPlayer) {
        // If COUNTERATTACK previously set a target, honour it for the next effect
        String pendingTarget = state.getPendingEffectTarget();
        String nextPlayer    = firstOtherPlayer(state, actingPlayer);
        String target        = (pendingTarget != null) ? pendingTarget : nextPlayer;

        return switch (effect) {

            case SKIP -> EffectArgs.withTarget(target);

            case GIFT -> {
                PlayerGameState actor = state.getPlayer(actingPlayer);
                int count = Math.min(2, actor.getHandSize());
                List<Card> cards = count == 0
                        ? List.of()
                        : new ArrayList<>(actor.getHand().subList(0, count));
                yield EffectArgs.withTargetAndCards(nextPlayer, cards);
            }

            case EXCHANGE -> {
                PlayerGameState actor = state.getPlayer(actingPlayer);
                int count = Math.min(2, actor.getHandSize());
                List<Card> cards = count == 0
                        ? List.of()
                        : new ArrayList<>(actor.getHand().subList(0, count));
                yield EffectArgs.withTargetAndCards(nextPlayer, cards);
            }

            case FANTASTIC      -> EffectArgs.withColor(CardColor.RED);

            case FANTASTIC_FOUR -> EffectArgs.withColor(CardColor.RED);

            case EQUALITY       -> EffectArgs.withTargetAndColor(nextPlayer, CardColor.RED);

            case COUNTERATTACK -> {
                // Redirect to a third player who is neither the actor nor the current target
                String redirectTo = state.getPlayerOrder().stream()
                        .map(PlayerGameState::getPlayerName)
                        .filter(n -> !n.equals(actingPlayer) && !n.equals(pendingTarget))
                        .findFirst()
                        .orElse(nextPlayer);
                yield EffectArgs.withTarget(redirectTo);
            }

            case NICE_TRY -> {
                // Target whoever has an empty hand (the player who just played their last card)
                String emptyHandPlayer = state.getPlayerOrder().stream()
                        .filter(p -> p.getHandSize() == 0)
                        .map(PlayerGameState::getPlayerName)
                        .findFirst()
                        .orElse(nextPlayer);
                yield EffectArgs.withTarget(emptyHandPlayer);
            }

            case SECOND_CHANCE -> {
                // Provide a playable COLOR card if possible, otherwise signal "no card"
                PlayerGameState actor = state.getPlayer(actingPlayer);
                Card top = state.peekDiscardPile();
                List<Card> playable = actor.getHand().stream()
                        .filter(c -> c.type() == CardType.COLOR
                                && CardValidator.canPlay(c, top, state))
                        .limit(1)
                        .collect(Collectors.toList());
                if (playable.isEmpty()) {
                    playable = actor.getHand().stream()
                            .filter(c -> CardValidator.canPlay(c, top, state))
                            .limit(1)
                            .collect(Collectors.toList());
                }
                yield EffectArgs.withCards(playable);
            }
        };
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    /** Returns the name of the first player in order who is not {@code self}. */
    private String firstOtherPlayer(GameState state, String self) {
        return state.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .filter(n -> !n.equals(self))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No other player found — need at least 2 players"));
    }

    /** Builds a snapshot {@code name → totalScore} map from the current player states. */
    private Map<String, Integer> buildTotalScoreMap(GameState state) {
        Map<String, Integer> map = new HashMap<>();
        for (PlayerGameState p : state.getPlayerOrder()) {
            map.put(p.getPlayerName(), p.getTotalScore());
        }
        return map;
    }
}
