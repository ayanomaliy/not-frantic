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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end integration test.
 *
 * <p>Simulates a deterministic multi-round game with three players entirely
 * in-process. The simulation always makes the simplest legal move available.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullRoundSimulationTest {

    private static final List<String> PLAYER_NAMES = List.of("Alice", "Bob", "Charlie");

    private static final long SEED = 42L;

    /**
     * Important:
     * The second GameInitializer argument is maxScore, not round number.
     * Keep this high enough so round 1 does not immediately end the whole game.
     */
    private static final int MAX_SCORE = 500;

    private static final int MAX_STEPS_PER_ROUND = 5_000;
    private static final int MAX_ROUNDS = 50;

    private GameState round1FinalState;
    private Map<String, Integer> round1RoundScores;
    private Map<String, Integer> totalScoresAfterRound1;

    private GameState round2State;
    private List<String> round2OrderedPlayerNames;
    private Map<String, Integer> round2StartingTotalScores;
    private Map<String, Integer> round2InitialHandSizes;

    private GameState finalGameState;
    private Map<String, Integer> finalTotalScores;
    private String declaredWinner;
    private int totalRoundsPlayed;

    private record RoundTransition(Map<String, Integer> roundScores,
                                   Map<String, Integer> cumulativeScores,
                                   GameState nextRoundState) {
    }

    @BeforeAll
    void runFullGame() {
        GameState r1 = GameInitializer.initialize(
                PLAYER_NAMES,
                MAX_SCORE,
                null,
                new Random(SEED)
        );

        simulateRound(r1);
        round1FinalState = r1;

        RoundTransition afterRound1 = transitionToNextRound(r1, SEED + 1);

        round1RoundScores = afterRound1.roundScores();
        totalScoresAfterRound1 = afterRound1.cumulativeScores();
        round2State = afterRound1.nextRoundState();

        if (round2State != null) {
            round2OrderedPlayerNames = round2State.getPlayerOrder().stream()
                    .map(PlayerGameState::getPlayerName)
                    .collect(Collectors.toList());

            round2StartingTotalScores = buildTotalScoreMap(round2State);

            round2InitialHandSizes = round2State.getPlayerOrder().stream()
                    .collect(Collectors.toMap(
                            PlayerGameState::getPlayerName,
                            PlayerGameState::getHandSize
                    ));
        } else {
            round2OrderedPlayerNames = List.of();
            round2StartingTotalScores = Map.of();
            round2InitialHandSizes = Map.of();
        }

        if (round2State == null) {
            finalGameState = r1;
            finalTotalScores = totalScoresAfterRound1;
            finalGameState.setPhase(GamePhase.GAME_OVER);
            declaredWinner = ScoreCalculator.getWinner(finalTotalScores);
            totalRoundsPlayed = 1;
            return;
        }

        GameState current = round2State;
        int round = 2;

        while (true) {
            simulateRound(current);

            RoundTransition transition = transitionToNextRound(
                    current,
                    SEED + round + 1
            );

            Map<String, Integer> cumulativeScores = transition.cumulativeScores();

            if (transition.nextRoundState() == null) {
                finalGameState = current;
                finalTotalScores = cumulativeScores;
                finalGameState.setPhase(GamePhase.GAME_OVER);
                totalRoundsPlayed = round;
                break;
            }

            if (round >= MAX_ROUNDS) {
                fail("Game did not end within " + MAX_ROUNDS
                        + " rounds — simulation may be stuck.");
            }

            round++;
            current = transition.nextRoundState();
        }

        declaredWinner = ScoreCalculator.getWinner(finalTotalScores);
    }

    @Test
    void round1_endsWithRoundEndOrGameOverPhase() {
        assertTrue(
                round1FinalState.getPhase() == GamePhase.ROUND_END
                        || round1FinalState.getPhase() == GamePhase.GAME_OVER,
                "Round 1 must terminate with ROUND_END, or GAME_OVER if the game ended after round 1"
        );
    }

    @Test
    void round1_allPlayersHaveNonNegativeRoundScore() {
        for (String name : PLAYER_NAMES) {
            assertTrue(
                    round1RoundScores.get(name) >= 0,
                    name + "'s round-1 score must be non-negative"
            );
        }
    }

    @Test
    void round1_roundScoresAddedToTotalScores() {
        for (String name : PLAYER_NAMES) {
            assertEquals(
                    round1RoundScores.get(name),
                    totalScoresAfterRound1.get(name),
                    name + "'s total after round 1 should equal their round-1 score"
            );
        }
    }

    @Test
    void round1_allPlayersHaveNonNegativeTotalScore() {
        for (String name : PLAYER_NAMES) {
            assertTrue(
                    totalScoresAfterRound1.get(name) >= 0,
                    name + " must have non-negative cumulative score after round 1"
            );
        }
    }

    @Test
    void round2_allPlayersPresent() {
        assumeTrue(round2State != null, "Game ended after round 1, so there is no round 2.");

        assertEquals(
                Set.of("Alice", "Bob", "Charlie"),
                Set.copyOf(round2OrderedPlayerNames),
                "All 3 players must be present at the start of round 2"
        );
    }

    @Test
    void round2_isCreatedByExplicitRoundTransitionStep() {
        assumeTrue(round2State != null, "Game ended after round 1, so there is no round 2.");

        assertNotSame(
                round1FinalState,
                round2State,
                "Round 2 must be a freshly initialized state, not the finished round-1 object."
        );
    }

    @Test
    void round2_playerOrderIsDescendingByScore() {
        assumeTrue(round2State != null, "Game ended after round 1, so there is no round 2.");

        for (int i = 0; i < round2OrderedPlayerNames.size() - 1; i++) {
            String a = round2OrderedPlayerNames.get(i);
            String b = round2OrderedPlayerNames.get(i + 1);

            int scoreA = totalScoresAfterRound1.get(a);
            int scoreB = totalScoresAfterRound1.get(b);

            assertTrue(
                    scoreA >= scoreB,
                    a + " (score=" + scoreA + ") must come before "
                            + b + " (score=" + scoreB + ") in round-2 order"
            );
        }
    }

    @Test
    void round2_eachPlayerStartsWith7Cards() {
        assumeTrue(round2State != null, "Game ended after round 1, so there is no round 2.");

        for (String name : PLAYER_NAMES) {
            assertEquals(
                    7,
                    round2InitialHandSizes.get(name),
                    name + " should start round 2 with exactly 7 cards"
            );
        }
    }

    @Test
    void gameOver_atLeastOnePlayerReachedMaxScore() {
        int maxScore = finalGameState.getMaxScore();

        boolean anyReached = finalTotalScores.values().stream()
                .anyMatch(score -> score >= maxScore);

        assertTrue(
                anyReached,
                "At least one player must have reached or exceeded maxScore="
                        + maxScore + "; actual totals: " + finalTotalScores
        );
    }

    @Test
    void gameOver_winnerIsOneOfThePlayers() {
        assertTrue(
                PLAYER_NAMES.contains(declaredWinner),
                "Winner must be one of the 3 players, got: " + declaredWinner
        );
    }

    @Test
    void gameOver_winnerHasLowestTotalScore() {
        int winnerScore = finalTotalScores.get(declaredWinner);

        for (Map.Entry<String, Integer> entry : finalTotalScores.entrySet()) {
            assertTrue(
                    winnerScore <= entry.getValue(),
                    "Winner " + declaredWinner + " (score=" + winnerScore
                            + ") must have the lowest score, but "
                            + entry.getKey() + " has " + entry.getValue()
            );
        }
    }

    @Test
    void gameOver_phaseIsGameOver() {
        assertEquals(
                GamePhase.GAME_OVER,
                finalGameState.getPhase(),
                "Phase must be GAME_OVER at the end of the game"
        );
    }

    @Test
    void gameOver_gameFinishedInReasonableRounds() {
        assertTrue(
                totalRoundsPlayed >= 1 && totalRoundsPlayed <= MAX_ROUNDS,
                "Game should finish in 1–" + MAX_ROUNDS + " rounds; took "
                        + totalRoundsPlayed
        );
    }

    @Test
    void gameOver_allPlayersHaveNonNegativeFinalScores() {
        for (String name : PLAYER_NAMES) {
            assertTrue(
                    finalTotalScores.get(name) >= 0,
                    name + " must have a non-negative final score"
            );
        }
    }

    private void simulateRound(GameState state) {
        for (int step = 0; step < MAX_STEPS_PER_ROUND; step++) {
            if (state.getPhase() == GamePhase.ROUND_END) {
                return;
            }

            simulateStep(state);
        }

        fail("Round did not reach ROUND_END within " + MAX_STEPS_PER_ROUND
                + " steps. Phase=" + state.getPhase()
                + ", drawPile=" + state.getDrawPile().size());
    }

    private RoundTransition transitionToNextRound(GameState finishedRound, long nextSeed) {
        assertEquals(
                GamePhase.ROUND_END,
                finishedRound.getPhase(),
                "A next round may only be initialized after the current round has ended."
        );

        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(
                        finishedRound.getPlayerOrder(),
                        finishedRound
                );

        Map<String, Integer> cumulativeScores = buildTotalScoreMap(finishedRound);

        if (ScoreCalculator.isGameOver(cumulativeScores, finishedRound.getMaxScore())) {
            return new RoundTransition(roundScores, cumulativeScores, null);
        }

        GameState nextRoundState = GameInitializer.initialize(
                PLAYER_NAMES,
                MAX_SCORE,
                cumulativeScores,
                new Random(nextSeed)
        );

        return new RoundTransition(roundScores, cumulativeScores, nextRoundState);
    }

    private void simulateStep(GameState state) {
        switch (state.getPhase()) {
            case TURN_START -> TurnEngine.startTurn(state);
            case AWAITING_PLAY -> simulatePlayOrDraw(state);
            case RESOLVING_EFFECT -> simulateEffectResolution(state);
            default -> {
                // ROUND_END / GAME_OVER are handled outside this method.
            }
        }
    }

    private void simulatePlayOrDraw(GameState state) {
        PlayerGameState current = state.getCurrentPlayer();
        String playerName = current.getPlayerName();
        Card top = state.peekDiscardPile();

        Card toPlay = pickPreferredCard(current, top, state);

        if (toPlay != null) {
            TurnEngine.playCard(state, playerName, toPlay);
            return;
        }

        TurnEngine.drawCard(state, playerName);

        if (state.getPhase() == GamePhase.ROUND_END) {
            return;
        }

        Card newTop = state.peekDiscardPile();
        Card afterDraw = pickPreferredCard(current, newTop, state);

        if (afterDraw != null) {
            TurnEngine.playCard(state, playerName, afterDraw);
        } else {
            TurnEngine.endTurn(state);
        }
    }

    private Card pickPreferredCard(PlayerGameState player, Card top, GameState state) {
        Optional<Card> color = player.getHand().stream()
                .filter(card -> card.type() == CardType.COLOR
                        && CardValidator.canPlay(card, top, state))
                .findFirst();

        if (color.isPresent()) {
            return color.get();
        }

        return player.getHand().stream()
                .filter(card -> CardValidator.canPlay(card, top, state))
                .findFirst()
                .orElse(null);
    }

    private void simulateEffectResolution(GameState state) {
        String actingPlayer = state.getCurrentPlayer().getPlayerName();

        if (state.getActiveEventCard() != null) {
            Card eventCard = state.getActiveEventCard();
            EventResolver.resolve(eventCard, state);

            if (state.getPhase() == GamePhase.RESOLVING_EFFECT) {
                TurnEngine.endTurn(state);
            }

            return;
        }

        if (!state.getPendingEffects().isEmpty()) {
            SpecialEffect effect = state.getPendingEffects().peek();
            EffectArgs args = buildArgs(effect, state, actingPlayer);
            EffectResolver.resolve(effect, state, actingPlayer, args);
            return;
        }

        TurnEngine.endTurn(state);
    }

    private EffectArgs buildArgs(SpecialEffect effect, GameState state, String actingPlayer) {
        String pendingTarget = state.getPendingEffectTarget();
        String nextPlayer = firstOtherPlayer(state, actingPlayer);
        String target = pendingTarget != null ? pendingTarget : nextPlayer;

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

            case FANTASTIC -> EffectArgs.withColor(CardColor.RED);

            case FANTASTIC_FOUR -> EffectArgs.withColor(CardColor.RED);

            case EQUALITY -> EffectArgs.withTargetAndColor(nextPlayer, CardColor.RED);

            case COUNTERATTACK -> {
                String redirectTo = state.getPlayerOrder().stream()
                        .map(PlayerGameState::getPlayerName)
                        .filter(name -> !name.equals(actingPlayer))
                        .filter(name -> !name.equals(pendingTarget))
                        .findFirst()
                        .orElse(nextPlayer);

                yield EffectArgs.withTarget(redirectTo);
            }

            case NICE_TRY -> {
                String emptyHandPlayer = state.getPlayerOrder().stream()
                        .filter(player -> player.getHandSize() == 0)
                        .map(PlayerGameState::getPlayerName)
                        .findFirst()
                        .orElse(nextPlayer);

                yield EffectArgs.withTarget(emptyHandPlayer);
            }

            case SECOND_CHANCE -> {
                PlayerGameState actor = state.getPlayer(actingPlayer);
                Card top = state.peekDiscardPile();

                List<Card> playable = actor.getHand().stream()
                        .filter(card -> card.type() == CardType.COLOR
                                && CardValidator.canPlay(card, top, state))
                        .limit(1)
                        .collect(Collectors.toList());

                if (playable.isEmpty()) {
                    playable = actor.getHand().stream()
                            .filter(card -> CardValidator.canPlay(card, top, state))
                            .limit(1)
                            .collect(Collectors.toList());
                }

                yield EffectArgs.withCards(playable);
            }
        };
    }

    private String firstOtherPlayer(GameState state, String self) {
        return state.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .filter(name -> !name.equals(self))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No other player found — need at least 2 players"
                ));
    }

    private Map<String, Integer> buildTotalScoreMap(GameState state) {
        Map<String, Integer> map = new HashMap<>();

        for (PlayerGameState player : state.getPlayerOrder()) {
            map.put(player.getPlayerName(), player.getTotalScore());
        }

        return map;
    }
}