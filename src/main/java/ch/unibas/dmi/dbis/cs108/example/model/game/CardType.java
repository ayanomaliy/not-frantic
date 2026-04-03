package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * Represents the type of a card, determining its play rules and scoring.
 */
public enum CardType {
    /** Standard colored card (red/green/blue/yellow), values 1–9. */
    COLOR,
    /** Black card, values 1–9. Triggers an event card when played. */
    BLACK,
    /** Single-color special card with one of four effects. */
    SPECIAL_SINGLE,
    /** Four-color special card, playable on any card during a turn. */
    SPECIAL_FOUR,
    /** Unique card, playable only when holding exactly 10 cards. */
    FUCK_YOU,
    /** Event card drawn from the event pile when a black card is played. */
    EVENT
}
