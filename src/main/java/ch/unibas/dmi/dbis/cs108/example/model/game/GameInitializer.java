package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Produces a fully initialized {@link GameState} ready for the first turn.
 *
 * <p>Initialization sequence:
 * <ol>
 *   <li>Build and shuffle main deck and event deck.</li>
 *   <li>Compute {@code maxScore = 150 - (3 × playerCount)}.</li>
 *   <li>Order players: alphabetically for round 1; by descending cumulative
 *       score for subsequent rounds (highest score plays first).</li>
 *   <li>Deal 7 cards to each player from the draw pile.</li>
 *   <li>Flip the top card of the draw pile onto the discard pile.</li>
 *   <li>Set {@code phase = TURN_START}.</li>
 * </ol>
 */
public class GameInitializer {

    private static final int STARTING_HAND_SIZE = 7;

    private GameInitializer() {}

    /**
     * Initializes a new round of the game.
     *
     * @param playerNames     Ordered or unordered list of player names joining this round.
     * @param roundNumber     1 for the first round, 2+ for subsequent rounds.
     * @param previousScores  Map of playerName → cumulative score from prior rounds.
     *                        May be empty or null for round 1.
     * @param rng             Random source used for shuffling; pass a seeded instance
     *                        for deterministic results in tests.
     * @return A fully initialized {@link GameState}.
     */
    public static GameState initialize(List<String> playerNames,
                                       int roundNumber,
                                       Map<String, Integer> previousScores,
                                       Random rng) {
        // 1. Build and shuffle decks
        List<Card> mainDeckList = DeckFactory.buildMainDeck();
        List<Card> eventDeckList = DeckFactory.buildEventDeck();
        DeckFactory.shuffle(mainDeckList, rng);
        DeckFactory.shuffle(eventDeckList, rng);

        Deque<Card> drawPile = new ArrayDeque<>(mainDeckList);
        Deque<Card> eventPile = new ArrayDeque<>(eventDeckList);

        // 2. Compute max score
        int maxScore = 150 - (3 * playerNames.size());

        // 3. Order players
        List<String> ordered = orderPlayers(playerNames, roundNumber, previousScores);

        // 4. Create player states, carrying over cumulative scores
        List<PlayerGameState> players = new ArrayList<>();
        for (String name : ordered) {
            int carried = (previousScores != null && previousScores.containsKey(name))
                    ? previousScores.get(name)
                    : 0;
            players.add(new PlayerGameState(name, carried));
        }

        // 5. Deal 7 cards to each player
        for (int i = 0; i < STARTING_HAND_SIZE; i++) {
            for (PlayerGameState player : players) {
                player.addCard(drawPile.pop());
            }
        }

        // 6. Flip a valid starter card from draw pile to discard pile
        Deque<Card> discardPile = new ArrayDeque<>();
        discardPile.push(drawValidStarterCard(drawPile));

        // 7. Build and return GameState (phase defaults to TURN_START)
        return new GameState(players, drawPile, discardPile, eventPile, maxScore, roundNumber);
    }

    /**
     * Determines turn order for the round.
     * Round 1: alphabetical by name.
     * Round 2+: descending by cumulative score (highest score plays first).
     * Ties in score are broken alphabetically.
     */
    private static List<String> orderPlayers(List<String> playerNames,
                                              int roundNumber,
                                              Map<String, Integer> previousScores) {
        List<String> ordered = new ArrayList<>(playerNames);
        if (roundNumber == 1 || previousScores == null || previousScores.isEmpty()) {
            ordered.sort(Comparator.naturalOrder());
        } else {
            ordered.sort(Comparator
                    .comparingInt((String name) -> previousScores.getOrDefault(name, 0))
                    .reversed()
                    .thenComparing(Comparator.naturalOrder()));
        }
        return ordered;
    }

    /**
     * Draws the first valid starter card from the draw pile.
     *
     * <p>The initial discard card should not be a special card that creates an
     * ambiguous or unplayable opening state. Therefore, only normal color cards
     * are allowed as starter cards.</p>
     *
     * <p>Cards that are skipped while searching are placed back at the bottom of
     * the draw pile in the same order in which they were encountered.</p>
     *
     * @param drawPile the shuffled draw pile
     * @return the chosen starter card
     * @throws IllegalStateException if no valid starter card can be found
     */
    private static Card drawValidStarterCard(Deque<Card> drawPile) {
        List<Card> skippedCards = new ArrayList<>();

        while (!drawPile.isEmpty()) {
            Card candidate = drawPile.pop();

            if (isValidStarterCard(candidate)) {
                for (int i = 0; i < skippedCards.size(); i++) {
                    drawPile.addLast(skippedCards.get(i));
                }
                return candidate;
            }

            skippedCards.add(candidate);
        }

        throw new IllegalStateException("Could not find a valid starter card.");
    }

    /**
     * Returns whether the given card is allowed as the initial discard card.
     *
     * @param card the candidate starter card
     * @return {@code true} if the card is a valid starter card
     */
    private static boolean isValidStarterCard(Card card) {
        return card.type() == CardType.COLOR;
    }
}
