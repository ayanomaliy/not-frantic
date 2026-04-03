package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * Represents a single card in the game.
 *
 * <p>Use the static factory methods to construct cards of each type:
 * {@link #colorCard}, {@link #blackCard}, {@link #specialSingleCard},
 * {@link #specialFourCard}, {@link #fuckYouCard}, {@link #eventCard}.
 *
 * @param id     Unique identifier for this card instance (0–124 for the main deck).
 * @param type   The category of the card.
 * @param color  The card's color, or {@code null} for four-color specials and event cards.
 * @param value  Numeric value 1–9 for color and black cards; 0 for all other types.
 * @param effect The special effect, or {@code null} for non-special cards.
 */
public record Card(int id, CardType type, CardColor color, int value, SpecialEffect effect) {

    /**
     * Returns the scoring value of this card as counted at round end.
     * <ul>
     *   <li>Color card: face value</li>
     *   <li>Black card: face value × 2</li>
     *   <li>Single-color special: 10</li>
     *   <li>Four-color special: 20</li>
     *   <li>Fuck You card: 69</li>
     *   <li>Event card: 0 (never in a player's hand)</li>
     * </ul>
     */
    public int scoringValue() {
        return switch (type) {
            case COLOR -> value;
            case BLACK -> value * 2;
            case SPECIAL_SINGLE -> 10;
            case SPECIAL_FOUR -> 20;
            case FUCK_YOU -> 69;
            case EVENT -> 0;
        };
    }

    // --- Static factory methods ---

    /** Creates a standard color card. */
    public static Card colorCard(int id, CardColor color, int value) {
        return new Card(id, CardType.COLOR, color, value, null);
    }

    /** Creates a black card. */
    public static Card blackCard(int id, int value) {
        return new Card(id, CardType.BLACK, CardColor.BLACK, value, null);
    }

    /** Creates a single-color special card. */
    public static Card specialSingleCard(int id, CardColor color, SpecialEffect effect) {
        return new Card(id, CardType.SPECIAL_SINGLE, color, 0, effect);
    }

    /** Creates a four-color special card. */
    public static Card specialFourCard(int id, SpecialEffect effect) {
        return new Card(id, CardType.SPECIAL_FOUR, null, 0, effect);
    }

    /** Creates the unique Fuck You card. */
    public static Card fuckYouCard(int id) {
        return new Card(id, CardType.FUCK_YOU, null, 0, null);
    }

    /** Creates an event card. */
    public static Card eventCard(int id) {
        return new Card(id, CardType.EVENT, null, 0, null);
    }
}
