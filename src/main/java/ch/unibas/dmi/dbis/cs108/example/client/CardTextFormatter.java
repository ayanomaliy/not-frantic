package ch.unibas.dmi.dbis.cs108.example.client;

/**
 * Utility for converting internal card ids into human-readable card labels
 * for the GUI and terminal client.
 */
public final class CardTextFormatter {

    private CardTextFormatter() {
    }

    /**
     * Converts a card id into a readable display label.
     *
     * @param cardId the internal card id
     * @return the human-readable card label
     */
    public static String formatCardLabel(int cardId) {
        if (cardId >= 0 && cardId <= 71) {
            String[] colors = {"Red", "Green", "Blue", "Yellow"};
            int colorIndex = cardId / 18;
            int withinColor = cardId % 18;
            int value = (withinColor / 2) + 1;
            return colors[colorIndex] + " " + value;
        }

        if (cardId >= 72 && cardId <= 80) {
            return "Black " + (cardId - 71);
        }

        if (cardId >= 81 && cardId <= 100) {
            String[] colors = {"Red", "Green", "Blue", "Yellow"};

            int offset = cardId - 81;
            int colorIndex = offset / 5;
            int withinColor = offset % 5;

            String effect = switch (withinColor) {
                case 0 -> "Second Chance";
                case 1 -> "Skip";
                case 2 -> "Gift";
                case 3 -> "Exchange";
                case 4 -> switch (colorIndex) {
                    case 0 -> "Second Chance";
                    case 1 -> "Skip";
                    case 2 -> "Gift";
                    case 3 -> "Exchange";
                    default -> "?";
                };
                default -> "?";
            };

            return colors[colorIndex] + " " + effect;
        }

        if (cardId >= 101 && cardId <= 105) {
            return "Fantastic";
        }

        if (cardId >= 106 && cardId <= 110) {
            return "Fantastic Four";
        }

        if (cardId >= 111 && cardId <= 115) {
            return "Equality";
        }

        if (cardId >= 116 && cardId <= 119) {
            return "Counterattack";
        }

        if (cardId >= 120 && cardId <= 123) {
            return "Nice Try";
        }

        if (cardId == 124) {
            return "F%&/ U";
        }

        return "Card #" + cardId;
    }

    /**
     * Converts a card id into a human-readable label that still includes the
     * numeric card id used by legacy slash commands.
     *
     * @param cardId the internal card id
     * @return the readable card label including the id
     */
    public static String formatCardLabelWithId(int cardId) {
        return formatCardLabel(cardId) + " (#" + cardId + ")";
    }
}