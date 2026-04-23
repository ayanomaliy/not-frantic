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

    /**
     * Creates a fresh three-player game state and starts the first turn.
     *
     * The player order is alphabetical:
     * Alice, Bob, Charlie.
     */
    @BeforeEach
    void setUp() {
        state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        currentPlayerName = state.getCurrentPlayer().getPlayerName();
        TurnEngine.startTurn(state);
    }

    // =========================================================================
    // startTurn
    // =========================================================================

    /**
     * Verifies that starting a turn sets the phase to AWAITING_PLAY.
     */
    @Test
    void startTurn_setsPhaseToAwaitingPlay() {
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase());
    }

    /**
     * Verifies that startTurn returns a TURN_ADVANCED event containing
     * the current player's name.
     */
    @Test
    void startTurn_returnsCorrectPlayerNameInEvent() {
        state.setPhase(GamePhase.TURN_START);
        List<GameEvent> events = TurnEngine.startTurn(state);

        assertEquals(1, events.size());
        assertEquals(GameEvent.EventType.TURN_ADVANCED, events.get(0).type());
        assertEquals(currentPlayerName, events.get(0).detail());
    }

    // =========================================================================
    // playCard — guards
    // =========================================================================

    /**
     * Verifies that a player who is not the current player cannot play a card
     * and that the state remains unchanged.
     */
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

    /**
     * Verifies that playing a card in the wrong phase returns an error.
     */
    @Test
    void playCard_wrongPhase_returnsError() {
        state.setPhase(GamePhase.RESOLVING_EFFECT);
        Card card = anyCardInHand(currentPlayerName);

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, card);

        assertEquals(1, events.size());
        assertTrue(events.get(0).isError());
    }

    /**
     * Verifies that an invalid card cannot be played and does not change
     * the player's hand or the discard pile.
     */
    @Test
    void playCard_invalidCard_returnsError_stateUnchanged() {
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));
        Card invalid = findInvalidCard(currentPlayerName,
                Card.colorCard(200, CardColor.RED, 3));

        if (invalid == null) {
            return;
        }

        int handSizeBefore = state.getCurrentPlayer().getHandSize();
        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, invalid);

        assertTrue(events.get(0).isError());
        assertEquals(handSizeBefore, state.getCurrentPlayer().getHandSize());
        assertEquals(CardColor.RED, state.peekDiscardPile().color());
    }

    // =========================================================================
    // playCard — plain color card
    // =========================================================================

    /**
     * Verifies that playing a valid color card removes it from the hand
     * and places it on top of the discard pile.
     */
    @Test
    void playCard_validColorCard_removesFromHandAndPushesToDiscard() {
        Card played = forcePlayableColorCard(currentPlayerName);
        int handBefore = state.getCurrentPlayer().getHandSize();

        TurnEngine.playCard(state, currentPlayerName, played);

        assertEquals(handBefore - 1, state.getCurrentPlayer().getHandSize());
        assertEquals(played, state.peekDiscardPile());
    }

    /**
     * Verifies that playing a valid color card produces a CARD_PLAYED event.
     */
    @Test
    void playCard_validColorCard_producesCardPlayedEvent() {
        Card played = forcePlayableColorCard(currentPlayerName);

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, played);

        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.CARD_PLAYED));
    }

    /**
     * Verifies that playing a normal valid color card automatically advances
     * the turn to the next player.
     */
    @Test
    void playCard_validColorCard_autoAdvancesPlayer() {
        Card played = forcePlayableColorCard(currentPlayerName);

        TurnEngine.playCard(state, currentPlayerName, played);

        assertNotEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    /**
     * Verifies that any active color or number request is cleared after a
     * successful normal card play.
     */
    @Test
    void playCard_validColorCard_clearsActiveRequests() {
        state.setRequestedColor(CardColor.BLUE);
        state.setRequestedNumber(5);

        Card blue5 = Card.colorCard(300, CardColor.BLUE, 5);
        state.getCurrentPlayer().addCard(blue5);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        TurnEngine.playCard(state, currentPlayerName, blue5);

        assertNull(state.getRequestedColor());
        assertNull(state.getRequestedNumber());
    }

    /**
     * Verifies that after a normal successful card play the phase becomes
     * TURN_START because the turn is auto-advanced.
     */
    @Test
    void playCard_setsPhaseToTurnStart_afterAutoAdvance() {
        Card played = forcePlayableColorCard(currentPlayerName);

        TurnEngine.playCard(state, currentPlayerName, played);

        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    // =========================================================================
    // playCard — FUCK_YOU card
    // =========================================================================

    /**
     * Verifies that the FUCK_YOU card can be played when the player has exactly
     * ten cards and that the turn advances successfully.
     */
    @Test
    void playCard_fuckYouCard_validWithExactly10Cards_autoAdvances() {
        setHandSize(currentPlayerName, 10);
        Card fuckYou = Card.fuckYouCard(124);
        state.getCurrentPlayer().getHand().set(9, fuckYou);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fuckYou);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertNotEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    /**
     * Verifies that the FUCK_YOU card cannot be played if the player has fewer
     * than ten cards.
     */
    @Test
    void playCard_fuckYouCard_invalidWith9Cards_returnsError() {
        setHandSize(currentPlayerName, 9);
        Card fuckYou = Card.fuckYouCard(124);
        state.getCurrentPlayer().addCard(fuckYou);
        state.getCurrentPlayer().getHand().clear();
        setHandSize(currentPlayerName, 8);
        state.getCurrentPlayer().addCard(fuckYou);

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fuckYou);

        assertTrue(events.get(0).isError());
    }

    // =========================================================================
    // playCard — BLACK card triggers event flip
    // =========================================================================

    /**
     * Verifies that playing a valid black card flips an event card and reduces
     * the event pile size by one.
     */
    @Test
    void playCard_blackCard_flipsEventCard() {
        Card black3 = Card.blackCard(77, 3);
        state.getCurrentPlayer().addCard(black3);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        int eventPileBefore = state.getEventPile().size();
        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, black3);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.EVENT_CARD_FLIPPED));
        assertNotNull(state.getActiveEventCard());
        assertEquals(eventPileBefore - 1, state.getEventPile().size());
    }

    /**
     * Verifies that playing a black card sets the phase to RESOLVING_EFFECT.
     */
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

    /**
     * Verifies that playing a special single-effect card queues the effect
     * and switches the phase to RESOLVING_EFFECT.
     */
    @Test
    void playCard_specialSingleCard_queuesEffectAndSetsResolvingPhase() {
        Card skip = Card.specialSingleCard(90, CardColor.RED, SpecialEffect.SKIP);
        state.getCurrentPlayer().addCard(skip);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, skip);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.EFFECT_TRIGGERED));
        assertEquals(SpecialEffect.SKIP, state.getPendingEffects().peek());
        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    /**
     * Verifies that playing a special four-effect card queues the effect
     * and switches the phase to RESOLVING_EFFECT.
     */
    @Test
    void playCard_specialFourCard_queuesEffectAndSetsResolvingPhase() {
        Card fantastic = Card.specialFourCard(105, SpecialEffect.FANTASTIC);
        state.getCurrentPlayer().addCard(fantastic);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, fantastic);

        assertFalse(events.stream().anyMatch(GameEvent::isError));
        assertEquals(SpecialEffect.FANTASTIC, state.getPendingEffects().peek());
        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    /**
     * Verifies that playing a special card does not automatically advance
     * the player because the effect still has to be resolved.
     */
    @Test
    void playCard_specialCard_doesNotAutoAdvancePlayer() {
        Card skip = Card.specialSingleCard(90, CardColor.RED, SpecialEffect.SKIP);
        state.getCurrentPlayer().addCard(skip);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 7));

        TurnEngine.playCard(state, currentPlayerName, skip);

        assertEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
    }

    // =========================================================================
    // playCard — round end: empty hand
    // =========================================================================

    /**
     * Verifies that playing the last card in hand ends the round.
     */
    @Test
    void playCard_lastCardInHand_triggersRoundEnd() {
        state.getCurrentPlayer().getHand().clear();
        Card last = Card.colorCard(300, CardColor.RED, 5);
        state.getCurrentPlayer().addCard(last);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));

        List<GameEvent> events = TurnEngine.playCard(state, currentPlayerName, last);

        assertEquals(GamePhase.ROUND_END, state.getPhase());
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.ROUND_ENDED));
    }

    /**
     * Verifies that the round-end reason is reported as player_empty_hand
     * when the last card is played.
     */
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

    /**
     * Verifies that drawing a card increases the current player's hand size by one.
     */
    @Test
    void drawCard_addsCardToHand() {
        int handBefore = state.getCurrentPlayer().getHandSize();

        TurnEngine.drawCard(state, currentPlayerName);

        assertEquals(handBefore + 1, state.getCurrentPlayer().getHandSize());
    }

    /**
     * Verifies that drawing a card produces a CARD_DRAWN event.
     */
    @Test
    void drawCard_producesCardDrawnEvent() {
        List<GameEvent> events = TurnEngine.drawCard(state, currentPlayerName);

        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.CARD_DRAWN));
    }

    /**
     * Verifies that drawing a card does not automatically advance the turn.
     */
    @Test
    void drawCard_doesNotAdvancePlayer() {
        TurnEngine.drawCard(state, currentPlayerName);

        assertEquals(currentPlayerName, state.getCurrentPlayer().getPlayerName());
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase());
    }

    /**
     * Verifies that a non-current player cannot draw a card.
     */
    @Test
    void drawCard_wrongPlayer_returnsError() {
        String other = otherThan(currentPlayerName);

        List<GameEvent> events = TurnEngine.drawCard(state, other);

        assertTrue(events.get(0).isError());
    }

    /**
     * Verifies that an empty draw pile ends the round immediately.
     */
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

    /**
     * Verifies that drawing a card reduces the draw pile size by one.
     */
    @Test
    void drawCard_reducesDrawPileByOne() {
        int before = state.getDrawPile().size();

        TurnEngine.drawCard(state, currentPlayerName);

        assertEquals(before - 1, state.getDrawPile().size());
    }

    // =========================================================================
    // endTurn
    // =========================================================================

    /**
     * Verifies that ending a turn advances to the next player
     * after a valid turn action has been recorded.
     */
    @Test
    void endTurn_advancesToNextPlayer() {
        String before = state.getCurrentPlayer().getPlayerName();
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        TurnEngine.endTurn(state);

        assertNotEquals(before, state.getCurrentPlayer().getPlayerName());
    }

    /**
     * Verifies that ending a turn sets the phase back to TURN_START
     * after a valid turn action has been recorded.
     */
    @Test
    void endTurn_setsPhaseToTurnStart() {
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        TurnEngine.endTurn(state);

        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    /**
     * Verifies that ending a turn returns a TURN_ADVANCED event
     * after a valid turn action has been recorded.
     */
    @Test
    void endTurn_returnsTurnAdvancedEvent() {
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        List<GameEvent> events = TurnEngine.endTurn(state);

        assertEquals(1, events.size());
        assertEquals(GameEvent.EventType.TURN_ADVANCED, events.get(0).type());
    }

    /**
     * Verifies that repeated turn endings wrap around to the first player
     * in player order.
     */
    @Test
    void endTurn_wrapsAroundToFirstPlayer() {
        String first = state.getCurrentPlayer().getPlayerName();

        state.getCurrentPlayer().setHasPlayedThisTurn(true);
        TurnEngine.endTurn(state);

        state.getCurrentPlayer().setHasPlayedThisTurn(true);
        TurnEngine.endTurn(state);

        state.getCurrentPlayer().setHasPlayedThisTurn(true);
        TurnEngine.endTurn(state);

        assertEquals(first, state.getCurrentPlayer().getPlayerName());
    }

    /**
     * Verifies that when the next player is marked as skipped,
     * the skip is consumed and the turn advances beyond that player.
     */
    @Test
    void endTurn_skippedPlayer_isSkipped_andFlagIsCleared() {
        String nextPlayer = state.getPlayerOrder().get(1).getPlayerName();
        state.getPlayer(nextPlayer).setSkipped(true);
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        TurnEngine.endTurn(state);

        assertNotEquals(nextPlayer, state.getCurrentPlayer().getPlayerName());
        assertFalse(state.getPlayer(nextPlayer).isSkipped());
    }

    /**
     * Verifies that when Bob is skipped in a three-player game,
     * ending Alice's turn lands on Charlie.
     */
    @Test
    void endTurn_skippedPlayer_landOnPlayerAfterSkipped() {
        state.getPlayerOrder().get(1).setSkipped(true);
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        TurnEngine.endTurn(state);

        assertEquals(state.getPlayerOrder().get(2).getPlayerName(),
                state.getCurrentPlayer().getPlayerName());
    }

    /**
     * Verifies that the previous player's hasPlayedThisTurn flag
     * is cleared when the turn ends.
     */
    @Test
    void endTurn_clearsHasPlayedThisTurn() {
        state.getCurrentPlayer().setHasPlayedThisTurn(true);

        TurnEngine.endTurn(state);

        assertFalse(state.getPlayerOrder().get(0).hasPlayedThisTurn());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns any card currently in the named player's hand.
     *
     * @param playerName the player whose hand is inspected
     * @return one card from that player's hand
     */
    private Card anyCardInHand(String playerName) {
        return state.getPlayer(playerName).getHand().get(0);
    }

    /**
     * Returns any player name in the game other than the given one.
     *
     * @param name the player name to exclude
     * @return a different player name
     */
    private String otherThan(String name) {
        return state.getPlayerOrder().stream()
                .map(PlayerGameState::getPlayerName)
                .filter(n -> !n.equals(name))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Replaces the top of the discard pile with the given card.
     *
     * @param card the card to place on top of the discard pile
     */
    private void forceDiscardTop(Card card) {
        state.getDiscardPile().clear();
        state.pushToDiscardPile(card);
    }

    /**
     * Adds a guaranteed playable color card to the current player's hand
     * and ensures that the discard top matches by color.
     *
     * @param playerName the acting player
     * @return the added playable card
     */
    private Card forcePlayableColorCard(String playerName) {
        Card playable = Card.colorCard(500, CardColor.BLUE, 8);
        forceDiscardTop(Card.colorCard(501, CardColor.BLUE, 3));
        state.getPlayer(playerName).addCard(playable);
        return playable;
    }

    /**
     * Finds any card held by the given player that cannot be played on the
     * current discard top.
     *
     * @param playerName the player whose hand is inspected
     * @param top the discard-top card context
     * @return an invalid card, or null if the constructed card is unexpectedly valid
     */
    private Card findInvalidCard(String playerName, Card top) {
        Card invalid = Card.colorCard(600, CardColor.GREEN, 9);
        state.getPlayer(playerName).addCard(invalid);
        forceDiscardTop(Card.colorCard(200, CardColor.RED, 3));
        return CardValidator.canPlay(invalid, state.peekDiscardPile(), state) ? null : invalid;
    }

    /**
     * Finds any playable card in the given player's hand.
     *
     * If none is found, a guaranteed playable card is added and returned.
     *
     * @param playerName the player whose hand is inspected
     * @return a playable card
     */
    private Card findPlayableCard(String playerName) {
        Card top = state.peekDiscardPile();
        return state.getPlayer(playerName).getHand().stream()
                .filter(c -> CardValidator.canPlay(c, top, state))
                .findFirst()
                .orElseGet(() -> forcePlayableColorCard(playerName));
    }

    /**
     * Clears and refills the named player's hand with exactly the given number
     * of dummy cards.
     *
     * @param playerName the player whose hand is rebuilt
     * @param size the desired hand size
     */
    private void setHandSize(String playerName, int size) {
        state.getPlayer(playerName).getHand().clear();
        for (int i = 0; i < size; i++) {
            state.getPlayer(playerName).addCard(Card.colorCard(700 + i, CardColor.GREEN, 1));
        }
    }
}