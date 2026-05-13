package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GameInitializerTest {


    // --- Max score ---

    @Test
    void maxScore_3Players_is141() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie"));
        assertEquals(141, state.getMaxScore());
    }

    @Test
    void maxScore_4Players_is138() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie", "Dave"));
        assertEquals(138, state.getMaxScore());
    }

    @Test
    void maxScore_2Players_is144() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertEquals(144, state.getMaxScore());
    }

    // --- Hand size ---

    @Test
    void eachPlayer_startsWithExactly7Cards() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie"));
        for (PlayerGameState player : state.getPlayerOrder()) {
            assertEquals(7, player.getHandSize(),
                    player.getPlayerName() + " should have 7 cards");
        }
    }

    // --- Draw pile size ---

    @Test
    void drawPile_hasCorrectSizeAfterDeal_3Players() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"),
                1,
                null,
                new Random(42)
        );

        assertEquals(
                99,
                state.getDrawPile().size(),
                "Draw pile should contain 99 cards after dealing 7 cards to 3 players and flipping 1 starter card"
        );
    }

    @Test
    void drawPile_hasCorrectSizeAfterDeal_4Players() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie", "Diana"),
                1,
                null,
                new Random(42)
        );

        assertEquals(
                92,
                state.getDrawPile().size(),
                "Draw pile should contain 92 cards after dealing 7 cards to 4 players and flipping 1 starter card"
        );
    }

    // --- Discard pile ---

    @Test
    void discardPile_startsWithExactly1Card() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertEquals(1, state.getDiscardPile().size());
    }

    @Test
    void discardPile_topCard_isNotNull() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertNotNull(state.peekDiscardPile());
    }

    // --- Event pile ---

    @Test
    void eventPile_hasExactly20Cards() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie"));
        assertEquals(20, state.getEventPile().size());
    }

    @Test
    void eventPile_allCardsAreEventType() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie"));
        state.getEventPile().forEach(c ->
                assertEquals(CardType.EVENT, c.type()));
    }

    // --- Player ordering: round 1 ---

    @Test
    void round1_playerOrder_isAlphabetical() {
        GameState state = GameInitializer.initialize(
                List.of("Charlie", "Alice", "Bob"), 1, null, new Random(1));
        List<String> names = getNames(state);
        assertEquals(List.of("Alice", "Bob", "Charlie"), names);
    }

    @Test
    void round1_playerOrder_isAlphabetical_alreadySorted() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(1));
        List<String> names = getNames(state);
        assertEquals(List.of("Alice", "Bob", "Charlie"), names);
    }

    // --- Player ordering: round 2+ ---

    @Test
    void round2_playerOrder_isDescendingByScore() {
        Map<String, Integer> scores = Map.of("Alice", 30, "Bob", 50, "Charlie", 10);
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 2, scores, new Random(1));
        List<String> names = getNames(state);
        assertEquals(List.of("Bob", "Alice", "Charlie"), names);
    }

    @Test
    void round2_tiedScores_brokenAlphabetically() {
        Map<String, Integer> scores = Map.of("Charlie", 50, "Alice", 50, "Bob", 10);
        GameState state = GameInitializer.initialize(
                List.of("Charlie", "Alice", "Bob"), 2, scores, new Random(1));
        List<String> names = getNames(state);
        // Alice and Charlie both have 50 — alphabetical tie-break → Alice before Charlie
        assertEquals(List.of("Alice", "Charlie", "Bob"), names);
    }

    // --- Phase ---

    @Test
    void initialPhase_isTurnStart() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    // --- Cumulative scores carried over ---

    @Test
    void round2_cumulativeScores_areCarriedIntoPlayerState() {
        Map<String, Integer> scores = Map.of("Alice", 42, "Bob", 15);
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob"), 2, scores, new Random(1));
        assertEquals(42, state.getPlayer("Alice").getTotalScore());
        assertEquals(15, state.getPlayer("Bob").getTotalScore());
    }

    @Test
    void round1_cumulativeScores_startAtZero() {
        GameState state = GameInitializer.initialize(
                List.of("Alice", "Bob"), 1, null, new Random(1));
        assertEquals(0, state.getPlayer("Alice").getTotalScore());
        assertEquals(0, state.getPlayer("Bob").getTotalScore());
    }

    // --- Initial state defaults ---

    @Test
    void initialState_currentPlayerIndex_isZero() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertEquals(0, state.getCurrentPlayerIndex());
    }

    @Test
    void initialState_noRequestedColorOrNumber() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertNull(state.getRequestedColor());
        assertNull(state.getRequestedNumber());
    }

    @Test
    void initialState_noActiveEventCard() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertNull(state.getActiveEventCard());
    }

    @Test
    void initialState_noPendingEffects() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertTrue(state.getPendingEffects().isEmpty());
    }

    // --- GameState helper methods ---

    @Test
    void getPlayer_returnsCorrectPlayer() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertEquals("Alice", state.getPlayer("Alice").getPlayerName());
    }

    @Test
    void getPlayer_unknownName_throwsException() {
        GameState state = initialize(List.of("Alice", "Bob"));
        assertThrows(IllegalArgumentException.class, () -> state.getPlayer("Nobody"));
    }

    @Test
    void drawFromDrawPile_reducesDrawPileSize() {
        GameState state = initialize(List.of("Alice", "Bob"));
        int before = state.getDrawPile().size();
        Card drawn = state.drawFromDrawPile();
        assertNotNull(drawn);
        assertEquals(before - 1, state.getDrawPile().size());
    }

    @Test
    void pushToDiscardPile_increasesDiscardPileSize() {
        GameState state = initialize(List.of("Alice", "Bob"));
        Card card = Card.colorCard(999, CardColor.RED, 5);
        int before = state.getDiscardPile().size();
        state.pushToDiscardPile(card);
        assertEquals(before + 1, state.getDiscardPile().size());
        assertEquals(card, state.peekDiscardPile());
    }

    @Test
    void advanceToNextPlayer_wrapsAround() {
        GameState state = initialize(List.of("Alice", "Bob", "Charlie"));
        assertEquals(0, state.getCurrentPlayerIndex());
        state.advanceToNextPlayer();
        assertEquals(1, state.getCurrentPlayerIndex());
        state.advanceToNextPlayer();
        assertEquals(2, state.getCurrentPlayerIndex());
        state.advanceToNextPlayer();
        assertEquals(0, state.getCurrentPlayerIndex());
    }

    @Test
    void roundNumber_isStoredOnGameState() {
        GameState round1 = GameInitializer.initialize(List.of("Alice", "Bob"), 1, null, new Random(42));
        assertEquals(1, round1.getRoundNumber());

        GameState round3 = GameInitializer.initialize(List.of("Alice", "Bob"), 3,
                Map.of("Alice", 10, "Bob", 20), new Random(42));
        assertEquals(3, round3.getRoundNumber());
    }

    // --- Helpers ---

    private GameState initialize(List<String> names) {
        return GameInitializer.initialize(names, 1, null, new Random(42));
    }

    private List<String> getNames(GameState state) {
        return state.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .collect(Collectors.toList());
    }
}
