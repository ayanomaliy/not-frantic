package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScoreCalculator}.
 *
 * Tests use {@link PlayerGameState} directly (no full {@link GameState} needed)
 * except where {@link GameState#getMaxScore()} integration is checked.
 */
class ScoreCalculatorTest {

    // =========================================================================
    // calculateRoundScores — per-round score computation
    // =========================================================================

    @Test
    void emptyHand_roundScoreIsZero() {
        PlayerGameState alice = new PlayerGameState("Alice");
        // Hand is empty by default

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(0, scores.get("Alice"));
        assertEquals(0, alice.getTotalScore());
    }

    @Test
    void handWithColorCard_roundScoreIsCardValue() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.colorCard(1, CardColor.RED, 3));

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(3, scores.get("Alice"));
    }

    @Test
    void handWithColorAndBlackCard_roundScoreIs11() {
        // RED/3 = 3, BLACK/4 = 4 × 2 = 8 → total 11
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.colorCard(1, CardColor.RED, 3));
        alice.addCard(Card.blackCard(2, 4));

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(11, scores.get("Alice"));
    }

    @Test
    void handWithSpecialFourCard_roundScoreIs20() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.specialFourCard(1, SpecialEffect.FANTASTIC));

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(20, scores.get("Alice"));
    }

    @Test
    void handWithSpecialSingleCard_roundScoreIs10() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP));

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(10, scores.get("Alice"));
    }

    @Test
    void handWithFuckYouCard_roundScoreIs69() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.fuckYouCard(124));

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(69, scores.get("Alice"));
    }

    @Test
    void calculateRoundScores_addsDeltaToPlayerTotalScore() {
        PlayerGameState alice = new PlayerGameState("Alice", 10); // starts with 10 from a prior round
        alice.addCard(Card.colorCard(1, CardColor.BLUE, 5)); // round score = 5

        ScoreCalculator.calculateRoundScores(List.of(alice));

        assertEquals(15, alice.getTotalScore()); // 10 + 5
    }

    @Test
    void calculateRoundScores_multipleRounds_accumulatesCorrectly() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.colorCard(1, CardColor.RED, 4)); // round 1 score = 4

        ScoreCalculator.calculateRoundScores(List.of(alice));
        assertEquals(4, alice.getTotalScore());

        // Clear hand, add new cards for round 2
        alice.getHand().clear();
        alice.addCard(Card.blackCard(2, 3)); // round 2 score = 6

        ScoreCalculator.calculateRoundScores(List.of(alice));
        assertEquals(10, alice.getTotalScore()); // 4 + 6
    }

    @Test
    void calculateRoundScores_multiplePlayers_returnsAllScores() {
        PlayerGameState alice = new PlayerGameState("Alice");
        PlayerGameState bob   = new PlayerGameState("Bob");
        alice.addCard(Card.colorCard(1, CardColor.RED, 7));   // 7
        bob.addCard(Card.specialSingleCard(2, CardColor.BLUE, SpecialEffect.GIFT)); // 10

        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice, bob));

        assertEquals(7,  scores.get("Alice"));
        assertEquals(10, scores.get("Bob"));
    }

    @Test
    void calculateRoundScores_returnedMapIsUnmodifiable() {
        PlayerGameState alice = new PlayerGameState("Alice");
        Map<String, Integer> scores = ScoreCalculator.calculateRoundScores(List.of(alice));

        assertThrows(UnsupportedOperationException.class,
                () -> scores.put("Intruder", 999));
    }

    // =========================================================================
    // isGameOver
    // =========================================================================

    @Test
    void isGameOver_falseWhenAllScoresBelowMax() {
        Map<String, Integer> totals = Map.of("Alice", 50, "Bob", 80, "Charlie", 100);
        assertFalse(ScoreCalculator.isGameOver(totals, 141));
    }

    @Test
    void isGameOver_trueWhenOnePlayerEqualsMax() {
        Map<String, Integer> totals = Map.of("Alice", 50, "Bob", 141);
        assertTrue(ScoreCalculator.isGameOver(totals, 141));
    }

    @Test
    void isGameOver_trueWhenOnePlayerExceedsMax() {
        Map<String, Integer> totals = Map.of("Alice", 200, "Bob", 30);
        assertTrue(ScoreCalculator.isGameOver(totals, 141));
    }

    @Test
    void isGameOver_allPlayersAtZero_false() {
        Map<String, Integer> totals = Map.of("Alice", 0, "Bob", 0);
        assertFalse(ScoreCalculator.isGameOver(totals, 141));
    }

    // =========================================================================
    // getWinner
    // =========================================================================

    @Test
    void getWinner_returnsPlayerWithLowestScore() {
        Map<String, Integer> totals = Map.of("Alice", 80, "Bob", 30, "Charlie", 120);
        assertEquals("Bob", ScoreCalculator.getWinner(totals));
    }

    @Test
    void getWinner_withTie_returnsAlphabeticallyFirst() {
        Map<String, Integer> totals = Map.of("Zelda", 50, "Alice", 50, "Bob", 80);
        assertEquals("Alice", ScoreCalculator.getWinner(totals));
    }

    @Test
    void getWinner_singlePlayer_returnsThatPlayer() {
        assertEquals("Alice", ScoreCalculator.getWinner(Map.of("Alice", 42)));
    }

    @Test
    void getWinner_emptyMap_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ScoreCalculator.getWinner(Map.of()));
    }

    // =========================================================================
    // maxScore formula — verified via GameInitializer
    // =========================================================================

    @Test
    void maxScore_twoPlayers_is144() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob"), 1, null, new Random(42));
        assertEquals(144, state.getMaxScore()); // 150 - 3*2
    }

    @Test
    void maxScore_threePlayers_is141() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        assertEquals(141, state.getMaxScore()); // 150 - 3*3
    }

    @Test
    void maxScore_fourPlayers_is138() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie", "Dave"), 1, null, new Random(42));
        assertEquals(138, state.getMaxScore()); // 150 - 3*4
    }
}
