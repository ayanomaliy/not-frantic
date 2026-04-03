package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * An immutable, tagged event produced by the {@link TurnEngine} to describe
 * what happened during a game action. Events are collected into lists and
 * broadcast to all clients after each action.
 *
 * @param type   The category of the event.
 * @param detail A human-readable string carrying extra context (player name,
 *               card id, error message, etc.). Never {@code null}.
 */
public record GameEvent(EventType type, String detail) {

    public enum EventType {
        /** A card was successfully played onto the discard pile. */
        CARD_PLAYED,
        /** A card was drawn from the draw pile into a player's hand. */
        CARD_DRAWN,
        /** The turn has advanced to the next player. */
        TURN_ADVANCED,
        /** A special effect has been queued for resolution. */
        EFFECT_TRIGGERED,
        /** An event card was flipped from the event pile. */
        EVENT_CARD_FLIPPED,
        /** The round has ended (player emptied hand or draw pile exhausted). */
        ROUND_ENDED,
        /** The game is over and a winner has been determined. */
        GAME_OVER,
        /** The requested action was illegal; state is unchanged. */
        ERROR
    }

    // --- Convenience factories ---

    public static GameEvent cardPlayed(String playerName, int cardId) {
        return new GameEvent(EventType.CARD_PLAYED, playerName + ":" + cardId);
    }

    public static GameEvent cardDrawn(String playerName, int cardId) {
        return new GameEvent(EventType.CARD_DRAWN, playerName + ":" + cardId);
    }

    public static GameEvent turnAdvanced(String nextPlayerName) {
        return new GameEvent(EventType.TURN_ADVANCED, nextPlayerName);
    }

    public static GameEvent effectTriggered(SpecialEffect effect) {
        return new GameEvent(EventType.EFFECT_TRIGGERED, effect.name());
    }

    public static GameEvent eventCardFlipped(int eventCardId) {
        return new GameEvent(EventType.EVENT_CARD_FLIPPED, String.valueOf(eventCardId));
    }

    public static GameEvent roundEnded(String reason) {
        return new GameEvent(EventType.ROUND_ENDED, reason);
    }

    public static GameEvent error(String message) {
        return new GameEvent(EventType.ERROR, message);
    }

    /** Returns {@code true} if this event represents a failed action. */
    public boolean isError() {
        return type == EventType.ERROR;
    }
}
