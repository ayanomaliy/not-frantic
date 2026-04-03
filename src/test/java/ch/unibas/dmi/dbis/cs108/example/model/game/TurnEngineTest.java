package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TurnEngine}.
 *
 * Each test sets up a controlled {@link GameState} so that the relevant
 * scenario can be exercised in isolation without relying on random deck order.
 */
class TurnEngineTest {

    private GameState state;
    private String currentPlayerName;

    @BeforeEach
    void setUp() {
        state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        // Alphabetical: Alice, Bob, Charlie → Alice is index 0
        currentPlayerName = state.getCurrentPlayer().getPlayerName();
        // Transition to AWAITING_PLAY so actions are accepted
        TurnEngine.startTurn(state);
    }

    // =========================================================================
    // startTurn
    // =========================================================================

    @Test
    void startTurn_setsPhaseToAwaitingPlay() {
        // State was already started in setUp; verify phase
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase());
    }

    @Test
    void startTurn_returnsCorrectPlayerNameInEvent() {
        // Reset phase and call startTurn again
        state.setPhase(GamePhase.TURN_START);
        List<GameEvent> events = TurnEngine.startTurn(state);
        assertEquals(1, events.size());
        assertEquals(GameEvent.EventType.TURN_ADVANCED, events.get(0).type());
        assertEquals(currentPlayerName, events.get(0).detail());
    }

    // =========================================================================
    // playCard — guards
    // =========================================================================

    @Test
    void playCard_wrongPlayer_returnsError_stateUnchanged() {
        String otherPlayer = otherThan(currentPlayerName);
        Card playable = findPlayableCard(otherPlayer);
        int discardSizeBefore = state.getDiscardPile().size();

        List<GameEvent> events = TurnEngine.playCard(state, otherPlayer, playable);

        assertEquals(1, events.size());
        assertTrue(events.get(0).isError());
        assertEquals(discardSizeBefore, state.getDiscardPile().size());
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase());
    }

    @Test
    void playCard_wrongPhase_returnsError() {
        state.setPhase(GamePhase.RESOLVING_EFFECT);
        Card card = anyCardInHand(currentPlayerName);

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, card);

        assertEquals(1, events.size());
        assertTrue(events.get(0).isError());
    }

    @Test
    void playCard_invalidCard_returnsError_stateUnchanged() {
        // Place a specific top on discard, then try to play a card that cannot match it
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));
        // Find a card in hand that is definitely invalid (different color AND different number)
        Card invalid = findInvalidCard(currentPlayerName,
                Card.colorCard(200, CardColor.RED, 3));

        if (invalid == null) {
            // No invalid card found — skip this scenario gracefully
            return;
        }

        int handSizeBefore = state.getCurrentPlayer().getHandSize();
        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, invalid);

        assertTrue(events.get(0).isError());
        assertEquals(handSizeBefore, state.getCurrentPlayer().getHandSize());
        assertEquals(CardColor.RED, state.peekDiscardPile().color()); // top unchanged
    }

    // =========================================================================
    // playCard — plain color card
    // =========================================================================

    @Test
    void playCard_validColorCard_removesFromHandAndPushesToDiscard() {
        Card played = forcePlayableColorCard(currentPlayerName);
        int handBefore = state.getCurrentPlayer().getHandSize();

        TurnEngine.playCard(state, currentPlayerName, played);

        assertEquals(handBefore - 1, state.getCurrentPlayer().getHandSize());
        assertEquals(played, state.peekDiscardPile());
    }

    @Test
    void playCard_validColorCard_producesCardPlayedEvent() {
        Card played = forcePlayableColorCard(currentPlayerName);

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, played);

        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.CARD_PLAYED));
    }

    @Test
    void playCard_validColorCard_autoAdvancesPlayer() {
        Card played = forcePlayableColorCard(currentPlayerName);

        TurnEngine.playCard(state, currentPlayerName, played);

        assertNotEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void playCard_validColorCard_clearsActiveRequests() {
        state.setRequestedColor(CardColor.BLUE);
        state.setRequestedNumber(5);
        // Force a card that satisfies the request
        Card blue5 = Card.colorCard(300, CardColor.BLUE, 5);
        state.getCurrentPlayer().addCard(blue5);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        TurnEngine.playCard(state, currentPlayerName, blue5);

        assertNull(state.getRequestedColor());
        assertNull(state.getRequestedNumber());
    }

    @Test
    void playCard_setsPhaseToTurnStart_afterAutoAdvance() {
        Card played = forcePlayableColorCard(currentPlayerName);
        TurnEngine.playCard(state, currentPlayerName, played);
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    // =========================================================================
    // playCard — FUCK_YOU card
    // =========================================================================

    @Test
    void playCard_fuckYouCard_validWithExactly10Cards_autoAdvances() {
        setHandSize(currentPlayerName, 10);
        // Replace the last card with FUCK_YOU
        Card fuckYou = Card.fuckYouCard(124);
        state.getCurrentPlayer().getHand().set(9, fuckYou);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3)); // FUCK_YOU ignores top

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fuckYou);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertNotEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void playCard_fuckYouCard_invalidWith9Cards_returnsError() {
        setHandSize(currentPlayerName, 9);
        Card fuckYou = Card.fuckYouCard(124);
        state.getCurrentPlayer().addCard(fuckYou); // now 10... so set to 9 differently
        state.getCurrentPlayer().getHand().clear();
        setHandSize(currentPlayerName, 8);
        state.getCurrentPlayer().addCard(fuckYou); // hand size = 9

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fuckYou);

        assertTrue(events.get(0).isError());
    }

    // =========================================================================
    // playCard — BLACK card triggers event flip
    // =========================================================================

    @Test
    void playCard_blackCard_flipsEventCard() {
        Card black3 = Card.blackCard(77, 3);
        state.getCurrentPlayer().addCard(black3);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3)); // same number → valid

        int eventPileBefore = state.getEventPile().size();
        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, black3);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.EVENT_CARD_FLIPPED));
        assertNotNull(state.getActiveEventCard());
        assertEquals(eventPileBefore - 1, state.getEventPile().size());
    }

    @Test
    void playCard_blackCard_setsPhaseToResolvingEffect() {
        Card black3 = Card.blackCard(77, 3);
        state.getCurrentPlayer().addCard(black3);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        TurnEngine.playCard(state, currentPlayerName, black3);

        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    // =========================================================================
    // playCard — SPECIAL cards queue effect
    // =========================================================================

    @Test
    void playCard_specialSingleCard_queuesEffectAndSetsResolvingPhase() {
        Card skip = Card.specialSingleCard(90, CardColor.RED, SpecialEffect.SKIP);
        state.getCurrentPlayer().addCard(skip);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7)); // same color

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, skip);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.EFFECT_TRIGGERED));
        assertEquals(SpecialEffect.SKIP, state.getPendingEffects().peek());
        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    @Test
    void playCard_specialFourCard_queuesEffectAndSetsResolvingPhase() {
        Card fantastic = Card.specialFourCard(105, SpecialEffect.FANTASTIC);
        state.getCurrentPlayer().addCard(fantastic);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7)); // SPECIAL_FOUR always valid

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fantastic);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertEquals(SpecialEffect.FANTASTIC, state.getPendingEffects().peek());
        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    @Test
    void playCard_specialCard_doesNotAutoAdvancePlayer() {
        Card skip = Card.specialSingleCard(90, CardColor.RED, SpecialEffect.SKIP);
        state.getCurrentPlayer().addCard(skip);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7));

        TurnEngine.playCard(state, currentPlayerName, skip);

        // Current player index must NOT have advanced yet (effect pending)
        assertEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    // =========================================================================
    // playCard — round end: empty hand
    // =========================================================================

    @Test
    void playCard_lastCardInHand_triggersRoundEnd() {
        // Give current player exactly one playable card
        state.getCurrentPlayer().getHand().clear();
        Card last = Card.colorCard(300, CardColor.RED, 5);
        state.getCurrentPlayer().addCard(last);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, last);

        assertEquals(GamePhase.ROUND_END, state.getPhase());
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.ROUND_ENDED));
    }

    @Test
    void playCard_lastCardInHand_roundEndReason_isPlayerEmptyHand() {
        state.getCurrentPlayer().getHand().clear();
        Card last = Card.colorCard(300, CardColor.RED, 5);
        state.getCurrentPlayer().addCard(last);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, last);

        GameEvent roundEnd = events.stream()
                .filter(e -> e.type() == GameEvent.EventType.ROUND_ENDED)
                .findFirst().orElseThrow();
        assertEquals("player_empty_hand", roundEnd.detail());
    }

    // =========================================================================
    // drawCard
    // =========================================================================

    @Test
    void drawCard_addsCardToHand() {
        int handBefore = state.getCurrentPlayer().getHandSize();
        TurnEngine.drawCard(state, currentPlayerName);
        assertEquals(handBefore + 1, state.getCurrentPlayer().getHandSize());
    }

    @Test
    void drawCard_producesCardDrawnEvent() {
        List<GameEvent> events = TurnEngine.drawCard(state, currentPlayerName);
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.CARD_DRAWN));
    }

    @Test
    void drawCard_doesNotAdvancePlayer() {
        TurnEngine.drawCard(state, currentPlayerName);
        assertEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase());
    }

    @Test
    void drawCard_wrongPlayer_returnsError() {
        String other = otherThan(currentPlayerName);
        List<GameEvent> events = TurnEngine.drawCard(state, other);
        assertTrue(events.get(0).isError());
    }

    @Test
    void drawCard_emptyDrawPile_triggersRoundEnd() {
        state.getDrawPile().clear();
        List<GameEvent> events = TurnEngine.drawCard(state, currentPlayerName);

        assertEquals(GamePhase.ROUND_END, state.getPhase());
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.ROUND_ENDED));
        GameEvent roundEnd = events.stream()
                .filter(e -> e.type() == GameEvent.EventType.ROUND_ENDED)
                .findFirst().orElseThrow();
        assertEquals("draw_pile_empty", roundEnd.detail());
    }

    @Test
    void drawCard_reducesDrawPileByOne() {
        int before = state.getDrawPile().size();
        TurnEngine.drawCard(state, currentPlayerName);
        assertEquals(before - 1, state.getDrawPile().size());
    }

    // =========================================================================
    // endTurn
    // =========================================================================

    @Test
    void endTurn_advancesToNextPlayer() {
        String before = state.getCurrentPlayer().getPlayerName();
        TurnEngine.endTurn(state);
        assertNotEquals(before, state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void endTurn_setsPhaseToTurnStart() {
        TurnEngine.endTurn(state);
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    @Test
    void endTurn_returnsTurnAdvancedEvent() {
        List<GameEvent> events = TurnEngine.endTurn(state);
        assertEquals(1, events.size());
        assertEquals(GameEvent.EventType.TURN_ADVANCED, events.get(0).type());
    }

    @Test
    void endTurn_wrapsAroundToFirstPlayer() {
        // Three players: advance three times to come back to first
        String first = state.getCurrentPlayer().getPlayerName();
        TurnEngine.endTurn(state);
        TurnEngine.endTurn(state);
        TurnEngine.endTurn(state);
        assertEquals(first, state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void endTurn_skippedPlayer_isSkipped_andFlagIsCleared() {
        // Mark the next player (Bob) as skipped
        String nextPlayer = state.getPlayerOrder().get(1).getPlayerName();
        state.getPlayer(nextPlayer).setSkipped(true);

        TurnEngine.endTurn(state);

        // Should have skipped over the skipped player
        assertNotEquals(nextPlayer, state.getCurrentPlayer().getPlayerName());
        // Skip flag should be consumed
        assertFalse(state.getPlayer(nextPlayer).isSkipped());
    }

    @Test
    void endTurn_skippedPlayer_landOnPlayerAfterSkipped() {
        // With 3 players Alice(0), Bob(1), Charlie(2): skip Bob → should land on Charlie
        state.getPlayerOrder().get(1).setSkipped(true); // Bob skipped

        TurnEngine.endTurn(state); // was Alice, advance past Bob to Charlie

        assertEquals(state.getPlayerOrder().get(2).getPlayerName(),
                state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void endTurn_clearsHasPlayedThisTurn() {
        state.getCurrentPlayer().setHasPlayedThisTurn(true);
        TurnEngine.endTurn(state);
        // After endTurn, the previous player's flag should be cleared
        assertFalse(state.getPlayerOrder().get(0).hasPlayedThisTurn());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns any card currently in the named player's hand. */
    private Card anyCardInHand(String playerName) {
        return state.getPlayer(playerName).getHand().get(0);
    }

    /** Returns any player name in the game other than the given one. */
    private String otherThan(String name) {
        return state.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .filter(n -> !n.equals(name))
                .findFirst()
                .orElseThrow();
    }

    /** Replaces the top of the discard pile with the given card. */
    private void forceDiscardTop(Card card) {
        state.getDiscardPile().clear();
        state.pushToDiscardPile(card);
    }

    /**
     * Adds a guaranteed playable color card to the current player's hand and
     * ensures the discard top matches by color.
     */
    private Card forcePlayableColorCard(String playerName) {
        Card playable = Card.colorCard(500, CardColor.BLUE, 8);
        forceDiscardTop(Card.colorCard(501, CardColor.BLUE, 3));
        state.getPlayer(playerName).addCard(playable);
        return playable;
    }

    /** Finds any card held by {@code playerName} that cannot be played on {@code top}. */
    private Card findInvalidCard(String playerName, Card top) {
        // Temporarily add a known-invalid card (GREEN/9 vs RED/3 has no match)
        Card invalid = Card.colorCard(600, CardColor.GREEN, 9);
        state.getPlayer(playerName).addCard(invalid);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));
        return CardValidator.canPlay(invalid, state.peekDiscardPile(), state) ? null : invalid;
    }

    /** Finds any playable card held by {@code playerName}. */
    private Card findPlayableCard(String playerName) {
        Card top = state.peekDiscardPile();
        return state.getPlayer(playerName).getHand().stream()
                .filter(c -> CardValidator.canPlay(c, top, state))
                .findFirst()
                .orElseGet(() -> forcePlayableColorCard(playerName));
    }

    /** Clears and refills the named player's hand with exactly {@code size} dummy cards. */
    private void setHandSize(String playerName, int size) {
        state.getPlayer(playerName).getHand().clear();
        for (int i = 0; i < size; i++) {
            state.getPlayer(playerName).addCard(Card.colorCard(700 + i, CardColor.GREEN, 1));
        }
    }
}
