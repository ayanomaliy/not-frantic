package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes the core turn actions: starting a turn, playing a card, drawing a
 * card, and ending a turn. Every method returns a list of {@link GameEvent}s
 * describing what happened; callers broadcast these to all clients.
 *
 * <h2>Phase contract</h2>
 * <ul>
 *   <li>{@link #startTurn} — must be called when phase is {@code TURN_START}.
 *       Transitions to {@code AWAITING_PLAY}.</li>
 *   <li>{@link #playCard} / {@link #drawCard} — require phase {@code AWAITING_PLAY}.</li>
 *   <li>{@link #endTurn} — advances to the next non-skipped player and sets
 *       phase to {@code TURN_START}. Called automatically after a plain card
 *       play; must be called explicitly after effect resolution (Phase 6).</li>
 * </ul>
 *
 * <h2>Request clearing</h2>
 * Any active color/number request is cleared when a card is successfully played,
 * before the card's own effect (if any) sets a new request in Phase 6.
 */
public class TurnEngine {

    private TurnEngine() {}

    // -------------------------------------------------------------------------
    // Start turn
    // -------------------------------------------------------------------------

    /**
     * Transitions the current player's turn from {@code TURN_START} to
     * {@code AWAITING_PLAY}. Call this once at the beginning of each turn
     * before accepting play or draw actions.
     *
     * @return A single {@link GameEvent.EventType#TURN_ADVANCED} event.
     */
    public static List<GameEvent> startTurn(GameState state) {
        state.setPhase(GamePhase.AWAITING_PLAY);
        return List.of(GameEvent.turnAdvanced(state.getCurrentPlayer().getPlayerName()));
    }

    // -------------------------------------------------------------------------
    // Play card
    // -------------------------------------------------------------------------

    /**
     * Attempts to play {@code card} on behalf of {@code playerName}.
     *
     * <p>On success, the card is removed from the player's hand and placed on
     * the discard pile. Depending on the card type:
     * <ul>
     *   <li>Plain COLOR or FUCK_YOU — {@link #endTurn} is called automatically.</li>
     *   <li>BLACK — the top event card is flipped; phase becomes
     *       {@code RESOLVING_EFFECT}.</li>
     *   <li>SPECIAL_SINGLE / SPECIAL_FOUR — the effect is pushed onto the
     *       pending-effects stack; phase becomes {@code RESOLVING_EFFECT}.</li>
     * </ul>
     * If the player's hand reaches zero after playing, the round ends
     * immediately regardless of card type.
     *
     * <p>On failure (wrong turn, wrong phase, invalid card), a single
     * {@link GameEvent.EventType#ERROR} event is returned and state is unchanged.
     *
     * @param state      Current game state.
     * @param playerName Name of the player attempting to play.
     * @param card       The card to play (must be in the player's hand).
     * @return List of events describing what happened.
     */
    public static List<GameEvent> playCard(GameState state, String playerName, Card card) {
        List<GameEvent> events = new ArrayList<>();

        // --- Guard: correct player ---
        if (!state.getCurrentPlayer().getPlayerName().equals(playerName)) {
            events.add(GameEvent.error("Not your turn"));
            return events;
        }

        // --- Guard: correct phase ---
        if (state.getPhase() != GamePhase.AWAITING_PLAY) {
            events.add(GameEvent.error("Cannot play card in phase " + state.getPhase()));
            return events;
        }

        // --- Guard: valid play ---
        Card top = state.peekDiscardPile();
        if (!CardValidator.canPlay(card, top, state)) {
            events.add(GameEvent.error("Card " + card.id() + " cannot be played on " + top.id()));
            return events;
        }

        // --- Execute: remove from hand, push to discard ---
        PlayerGameState player = state.getPlayer(playerName);
        player.removeCard(card);
        state.pushToDiscardPile(card);
        player.setHasPlayedThisTurn(true);

        // Clear any active request — effects in Phase 6 will set new ones
        state.setRequestedColor(null);
        state.setRequestedNumber(null);

        events.add(GameEvent.cardPlayed(playerName, card.id()));

        // --- Check round end: player emptied hand ---
        if (player.getHandSize() == 0) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("player_empty_hand"));
            return events;
        }

        // --- Route by card type ---
        switch (card.type()) {
            case BLACK -> {
                state.setSpecialsBlocked(false); // a new black card lifts any BLOCK_SPECIALS effect
                Card eventCard = state.drawFromEventPile();
                if (eventCard != null) {
                    state.setActiveEventCard(eventCard);
                    events.add(GameEvent.eventCardFlipped(eventCard.id()));
                }
                state.setPhase(GamePhase.RESOLVING_EFFECT);
            }
            case SPECIAL_SINGLE, SPECIAL_FOUR -> {
                state.getPendingEffects().push(card.effect());
                events.add(GameEvent.effectTriggered(card.effect()));
                state.setPhase(GamePhase.RESOLVING_EFFECT);
            }
            default -> {
                // COLOR, FUCK_YOU: no effect — auto-advance
                events.addAll(endTurn(state));
            }
        }

        return events;
    }

    // -------------------------------------------------------------------------
    // Draw card
    // -------------------------------------------------------------------------

    /**
     * Draws one card from the draw pile into {@code playerName}'s hand.
     *
     * <p>If the draw pile is empty, the round ends immediately. Otherwise the
     * phase remains {@code AWAITING_PLAY}: the player may still play a card
     * (including the one just drawn) before ending their turn.
     *
     * @return List of events describing what happened.
     */
    public static List<GameEvent> drawCard(GameState state, String playerName) {
        List<GameEvent> events = new ArrayList<>();

        // --- Guard: correct player ---
        if (!state.getCurrentPlayer().getPlayerName().equals(playerName)) {
            events.add(GameEvent.error("Not your turn"));
            return events;
        }

        // --- Guard: correct phase ---
        if (state.getPhase() != GamePhase.AWAITING_PLAY) {
            events.add(GameEvent.error("Cannot draw card in phase " + state.getPhase()));
            return events;
        }

        // --- Draw ---
        Card drawn = state.drawFromDrawPile();
        if (drawn == null) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("draw_pile_empty"));
            return events;
        }

        state.getPlayer(playerName).addCard(drawn);
        events.add(GameEvent.cardDrawn(playerName, drawn.id()));
        // Phase stays AWAITING_PLAY — player may still play

        return events;
    }

    // -------------------------------------------------------------------------
    // End turn
    // -------------------------------------------------------------------------

    /**
     * Ends the current player's turn and advances to the next non-skipped player.
     *
     * <p>If the next player in line has a skip flag set, their flag is consumed
     * and the turn passes to the player after them. A safety guard prevents an
     * infinite loop when all players are somehow skipped.
     *
     * @return A single {@link GameEvent.EventType#TURN_ADVANCED} event naming the
     *         player whose turn it now is.
     */
    public static List<GameEvent> endTurn(GameState state) {
        state.getCurrentPlayer().setHasPlayedThisTurn(false);

        int playerCount = state.getPlayerOrder().size();
        int advanceCount = 0;

        // Advance at least once, keep advancing while the next player is skipped
        do {
            state.advanceToNextPlayer();
            advanceCount++;
            PlayerGameState next = state.getCurrentPlayer();
            if (next.isSkipped()) {
                next.setSkipped(false); // consume the skip
            } else {
                break;
            }
        } while (advanceCount < playerCount);

        state.setPhase(GamePhase.TURN_START);
        return List.of(GameEvent.turnAdvanced(state.getCurrentPlayer().getPlayerName()));
    }
}
