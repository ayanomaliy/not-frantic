package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * Represents the effect of a special card.
 * Single-color specials have one of the first four effects.
 * Four-color specials have one of the last five effects.
 */
public enum SpecialEffect {
    // --- Single-color special effects ---

    /** Actor must immediately play another card; if impossible, draws 1. */
    SECOND_CHANCE,
    /** Actor chooses a target player who skips their next turn. */
    SKIP,
    /** Actor gives 2 cards (or 1 if only 1 remains) to a chosen player. */
    GIFT,
    /** Actor swaps 2 cards with a chosen player (target's cards are hidden to actor). */
    EXCHANGE,

    // --- Four-color special effects ---

    /** Actor requests a color and/or number for the next play. */
    FANTASTIC,
    /** Distributes 4 cards from the draw pile among players; actor then sets a request. */
    FANTASTIC_FOUR,
    /** Target draws until their hand size equals the actor's; actor then requests a color. */
    EQUALITY,
    /** Can be played out of turn; cancels an incoming effect and redirects it to a new target. */
    COUNTERATTACK,
    /** Can be played when someone runs out of cards; forces that player to draw 3. */
    NICE_TRY
}
