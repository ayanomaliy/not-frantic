package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * Represents the current phase of the game state machine.
 *
 * <p>Transitions:
 * <pre>
 * WAITING → TURN_START → AWAITING_PLAY → RESOLVING_EFFECT → TURN_START
 *                                     ↘ ROUND_END → TURN_START (next round)
 *                                                 → GAME_OVER
 * </pre>
 */
public enum GamePhase {
    /** Game has not started yet; waiting for the server to initialize. */
    WAITING,
    /** Start of a player's turn; effects from the top discard card are applied. */
    TURN_START,
    /** The current player must play a card or draw one. */
    AWAITING_PLAY,
    /** A special effect is being resolved; no new plays accepted until complete. */
    RESOLVING_EFFECT,
    /** The round has ended; scores are being calculated. */
    ROUND_END,
    /** The game is over; a winner has been determined. */
    GAME_OVER
}
