package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless utility that handles all score-related computations at round and
 * game end.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>{@link #calculateRoundScores} — sums each player's hand at round end,
 *       applies the delta to their running total, and returns the per-round
 *       breakdown for broadcasting.</li>
 *   <li>{@link #isGameOver} — returns {@code true} once any player's cumulative
 *       score reaches or exceeds {@code maxScore}.</li>
 *   <li>{@link #getWinner} — returns the name of the player with the lowest
 *       total score (alphabetical tie-breaking).</li>
 * </ul>
 */
public class ScoreCalculator {

    private ScoreCalculator() {}

    /**
     * Calculates each player's round score (sum of
     * {@link Card#scoringValue()} for every card still in their hand),
     * adds it to their {@link PlayerGameState#addToTotalScore running total},
     * and returns a snapshot of the per-round scores.
     *
     * <p>The returned map preserves the order of {@code players}.
     *
     * @param players All players in the finished round.
     * @return An unmodifiable map from player name → round score for this round.
     */
    public static Map<String, Integer> calculateRoundScores(List<PlayerGameState> players) {
        Map<String, Integer> roundScores = new LinkedHashMap<>();
        for (PlayerGameState player : players) {
            int roundScore = player.getHand().stream()
                    .mapToInt(Card::scoringValue)
                    .sum();
            roundScores.put(player.getPlayerName(), roundScore);
            player.addToTotalScore(roundScore);
        }
        return Collections.unmodifiableMap(roundScores);
    }

    /**
     * Variant of {@link #calculateRoundScores(java.util.List)} that respects the
     * {@code DOUBLE_SCORING} event flag on {@code state}.
     *
     * <p>If {@link GameState#isDoubleScoringActive()} is {@code true}, every
     * player's round score is multiplied by 2 before being added to their total.
     * The flag is consumed (set to {@code false}) after this call.
     *
     * @param players All players in the finished round.
     * @param state   Current game state (read for {@code doubleScoringActive}).
     * @return An unmodifiable map from player name → round score for this round
     *         (after the doubling multiplier, if any).
     */
    public static Map<String, Integer> calculateRoundScores(List<PlayerGameState> players,
                                                             GameState state) {
        int multiplier = state.isDoubleScoringActive() ? 2 : 1;
        state.setDoubleScoringActive(false);
        Map<String, Integer> roundScores = new LinkedHashMap<>();
        for (PlayerGameState player : players) {
            int roundScore = player.getHand().stream()
                    .mapToInt(Card::scoringValue)
                    .sum() * multiplier;
            roundScores.put(player.getPlayerName(), roundScore);
            player.addToTotalScore(roundScore);
        }
        return Collections.unmodifiableMap(roundScores);
    }

    /**
     * Returns {@code true} if any entry in {@code totalScores} is greater than
     * or equal to {@code maxScore}, signalling that the game is over.
     *
     * @param totalScores Map from player name → cumulative score.
     * @param maxScore    The threshold at which the game ends (typically
     *                    {@code 150 - 3 × playerCount}).
     */
    public static boolean isGameOver(Map<String, Integer> totalScores, int maxScore) {
        return totalScores.values().stream().anyMatch(s -> s >= maxScore);
    }

    /**
     * Returns the name of the player with the <em>lowest</em> cumulative score.
     * In case of a tie the player whose name comes first alphabetically wins.
     *
     * @param totalScores Map from player name → cumulative score.
     * @return The winner's name.
     * @throws IllegalArgumentException if {@code totalScores} is empty.
     */
    public static String getWinner(Map<String, Integer> totalScores) {
        return totalScores.entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry<String, Integer>::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot determine a winner from an empty score map"));
    }
}
