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
 *   <li>{@link #endTurn} — may only succeed if the current player has already
 *       played a card or drawn a card during this turn.</li>
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
     * @param state the current game state
     * @return a single {@link GameEvent.EventType#TURN_ADVANCED} event
     */
    public static List<GameEvent> startTurn(GameState state) {
        state.getCurrentPlayer().setHasPlayedThisTurn(false);
        state.getCurrentPlayer().setHasDrawnThisTurn(false);
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
     * the discard pile. Depending on the card type:</p>
     * <ul>
     *   <li>Plain {@code COLOR} or {@code FUCK_YOU} — {@link #endTurn} is called automatically.</li>
     *   <li>{@code BLACK} — the top event card is flipped; phase becomes
     *       {@code RESOLVING_EFFECT}.</li>
     *   <li>{@code SPECIAL_SINGLE} / {@code SPECIAL_FOUR} — the effect is pushed onto the
     *       pending-effects stack; phase becomes {@code RESOLVING_EFFECT}.</li>
     * </ul>
     *
     * <p>If the player's hand reaches zero after playing, the round ends
     * immediately regardless of card type.</p>
     *
     * <p>On failure (wrong turn, wrong phase, invalid card), a single
     * {@link GameEvent.EventType#ERROR} event is returned and state is unchanged.</p>
     *
     * @param state the current game state
     * @param playerName the name of the player attempting to play
     * @param card the card to play
     * @return a list of generated game events
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
     * after drawing, but may not draw a second time in the same turn.</p>
     *
     * @param state the current game state
     * @param playerName the name of the player drawing
     * @return a list of generated game events
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

        PlayerGameState player = state.getPlayer(playerName);

        // --- Guard: only one normal draw per turn ---
        if (player.hasDrawnThisTurn()) {
            events.add(GameEvent.error("You already drew a card this turn."));
            return events;
        }

        // --- Draw ---
        Card drawn = state.drawFromDrawPile();
        if (drawn == null) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("draw_pile_empty"));
            return events;
        }

        player.addCard(drawn);
        player.setHasDrawnThisTurn(true);
        events.add(GameEvent.cardDrawn(playerName, drawn.id()));

        // Phase stays AWAITING_PLAY — player may still play one valid card
        return events;
    }

    // -------------------------------------------------------------------------
    // End turn
    // -------------------------------------------------------------------------

    /**
     * Ends the current player's turn and advances to the next non-skipped player.
     *
     * <p>The current player may only end their turn if they have already
     * played a card or drawn a card during this turn. This prevents empty
     * passes with no action.</p>
     *
     * <p>If the next player in line has a skip flag set, their flag is consumed
     * and the turn passes to the player after them. A safety guard prevents an
     * infinite loop when all players are somehow skipped.</p>
     *
     * @param state the current game state
     * @return a list containing either an error event or a turn-advanced event
     */
    public static List<GameEvent> endTurn(GameState state) {
        PlayerGameState current = state.getCurrentPlayer();

        if (!current.hasPlayedThisTurn() && !current.hasDrawnThisTurn()) {
            return List.of(GameEvent.error(
                    "You must play a card or draw a card before ending your turn."
            ));
        }

        current.setHasPlayedThisTurn(false);
        current.setHasDrawnThisTurn(false);

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