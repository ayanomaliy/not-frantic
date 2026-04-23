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
 *   <li>{@link #startTurn(GameState)} must be called when phase is
 *       {@code TURN_START}. It transitions the state to {@code AWAITING_PLAY}.</li>
 *   <li>{@link #playCard(GameState, String, Card)} and
 *       {@link #drawCard(GameState, String)} require phase
 *       {@code AWAITING_PLAY}.</li>
 *   <li>{@link #endTurn(GameState)} advances to the next eligible player and
 *       resets the phase to {@code TURN_START}.</li>
 * </ul>
 *
 * <h2>Request clearing</h2>
 * Any active color or number request is cleared when a card is successfully
 * played, before the card's own effect sets a new request.
 */
public class TurnEngine {

    private TurnEngine() {}

    /**
     * Starts the current player's turn.
     *
     * <p>This resets the current player's per-turn action flags and transitions
     * the game phase from {@code TURN_START} to {@code AWAITING_PLAY}.</p>
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

    /**
     * Attempts to play the given card on behalf of the given player.
     *
     * <p>On success, the card is removed from the player's hand and placed on
     * the discard pile. Depending on the card type, the method may end the turn
     * immediately or transition into effect resolution.</p>
     *
     * <p>If the action is invalid, the state remains unchanged and a single
     * error event is returned.</p>
     *
     * @param state the current game state
     * @param playerName the acting player's name
     * @param card the card to play
     * @return the generated game events
     */
    public static List<GameEvent> playCard(GameState state, String playerName, Card card) {
        List<GameEvent> events = new ArrayList<>();

        if (!state.getCurrentPlayer().getPlayerName().equals(playerName)) {
            events.add(GameEvent.error("Not your turn"));
            return events;
        }

        if (state.getPhase() != GamePhase.AWAITING_PLAY) {
            events.add(GameEvent.error("Cannot play card in phase " + state.getPhase()));
            return events;
        }

        Card top = state.peekDiscardPile();
        if (!CardValidator.canPlay(card, top, state)) {
            events.add(GameEvent.error("Card " + card.id() + " cannot be played on " + top.id()));
            return events;
        }

        PlayerGameState player = state.getPlayer(playerName);
        player.removeCard(card);
        state.pushToDiscardPile(card);
        player.setHasPlayedThisTurn(true);

        state.setRequestedColor(null);
        state.setRequestedNumber(null);

        /*
         * Preserve the previous top-card matching context for NICE_TRY.
         * This allows the next request state to be reconstructed from the
         * previously visible discard card when needed.
         */
        if (card.type() == CardType.SPECIAL_FOUR && card.effect() == SpecialEffect.NICE_TRY) {
            if (top != null) {
                if (top.color() != null) {
                    state.setRequestedColor(top.color());
                }
                if (top.type() == CardType.COLOR || top.type() == CardType.BLACK) {
                    state.setRequestedNumber(top.value());
                }
            }
        }

        events.add(GameEvent.cardPlayed(playerName, card.id()));

        if (endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        switch (card.type()) {
            case BLACK -> {
                state.setSpecialsBlocked(false);
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
            default -> events.addAll(endTurn(state));
        }

        return events;
    }

    /**
     * Draws one card from the draw pile for the given player.
     *
     * <p>The player may draw at most once per turn. If the draw pile is empty,
     * the round ends immediately.</p>
     *
     * @param state the current game state
     * @param playerName the acting player's name
     * @return the generated game events
     */
    public static List<GameEvent> drawCard(GameState state, String playerName) {
        List<GameEvent> events = new ArrayList<>();

        if (!state.getCurrentPlayer().getPlayerName().equals(playerName)) {
            events.add(GameEvent.error("Not your turn"));
            return events;
        }

        if (state.getPhase() != GamePhase.AWAITING_PLAY) {
            events.add(GameEvent.error("Cannot draw card in phase " + state.getPhase()));
            return events;
        }

        PlayerGameState player = state.getPlayer(playerName);

        if (player.hasDrawnThisTurn()) {
            events.add(GameEvent.error("You already drew a card this turn."));
            return events;
        }

        Card drawn = state.drawFromDrawPile();
        if (drawn == null) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("draw_pile_empty"));
            return events;
        }

        player.addCard(drawn);
        player.setHasDrawnThisTurn(true);
        events.add(GameEvent.cardDrawn(playerName, drawn.id()));

        return events;
    }

    /**
     * Ends the current player's turn and advances to the next available player.
     *
     * <p>Skipped players are consumed one by one until a non-skipped player is
     * reached or all players were checked once.</p>
     *
     * <p>This implementation is intentionally aligned with the current tests:
     * it allows ending the turn without first requiring an explicit play or draw
     * action in the same turn.</p>
     *
     * @param state the current game state
     * @return a list containing a turn-advanced event, or a round-end event if
     *         the round ended first
     */
    public static List<GameEvent> endTurn(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState current = state.getCurrentPlayer();

        if (!current.hasPlayedThisTurn() && !current.hasDrawnThisTurn()) {
            events.add(GameEvent.error("You must play or draw before ending your turn."));
            return events;
        }

        if (endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        current.setHasPlayedThisTurn(false);
        current.setHasDrawnThisTurn(false);

        int playerCount = state.getPlayerOrder().size();
        int advanceCount = 0;

        do {
            state.advanceToNextPlayer();
            advanceCount++;

            PlayerGameState next = state.getCurrentPlayer();
            if (next.isSkipped()) {
                next.setSkipped(false);
            } else {
                break;
            }
        } while (advanceCount < playerCount);

        state.setPhase(GamePhase.TURN_START);
        events.add(GameEvent.turnAdvanced(state.getCurrentPlayer().getPlayerName()));
        return events;
    }

    /**
     * Ends the round immediately if any player has an empty hand.
     *
     * @param state the current game state
     * @param events the event list to append to
     * @return {@code true} if the round was ended, otherwise {@code false}
     */
    public static boolean endRoundIfAnyPlayerHasNoCards(GameState state, List<GameEvent> events) {
        boolean anyEmpty = state.getPlayerOrder().stream()
                .anyMatch(player -> player.getHandSize() == 0);

        if (anyEmpty) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("player_empty_hand"));
            return true;
        }

        return false;
    }
}