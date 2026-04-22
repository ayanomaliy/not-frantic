package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * Stateless validator that determines whether a card may legally be played
 * given the current top of the discard pile and the game state.
 *
 * <h2>Play rules summary</h2>
 * <ul>
 *   <li><b>COLOR</b> – If a color or number request is active, the card must
 *       satisfy at least one active request. Otherwise: same color OR same
 *       number as the top of the discard pile.</li>
 *   <li><b>BLACK</b> – Same number as top AND top is not a black card.
 *       Requests do not override black-card rules.</li>
 *   <li><b>SPECIAL_SINGLE</b> – If a color request is active, card color must
 *       match. Otherwise: same color as top OR same symbol (effect) as top.</li>
 *   <li><b>SPECIAL_FOUR</b> – Always playable during the current player's turn.</li>
 *   <li><b>FUCK_YOU</b> – Playable only when the current player holds exactly
 *       10 cards.</li>
 *   <li><b>EVENT</b> – Never played from hand; always {@code false}.</li>
 * </ul>
 *
 * <h2>Out-of-turn play</h2>
 * Only {@link SpecialEffect#COUNTERATTACK} and {@link SpecialEffect#NICE_TRY}
 * may be played outside the current player's turn.
 */
public class CardValidator {

    private CardValidator() {}

    /**
     * Returns {@code true} if {@code card} may be played on top of
     * {@code topOfDiscard} given the current {@code state}.
     *
     * @param card         The card the current player wants to play.
     * @param topOfDiscard The card currently on top of the discard pile.
     * @param state        The full game state (used for requests and hand size).
     */
    public static boolean canPlay(Card card, Card topOfDiscard, GameState state) {
        return switch (card.type()) {
            case COLOR -> canPlayColorCard(card, topOfDiscard, state);
            case BLACK -> canPlayBlackCard(card, topOfDiscard, state);
            case SPECIAL_SINGLE -> !state.isSpecialsBlocked() && canPlaySpecialSingle(card, topOfDiscard, state);
            case SPECIAL_FOUR -> !state.isSpecialsBlocked();
            case FUCK_YOU -> state.getCurrentPlayer().getHandSize() == 10;
            case EVENT -> false;
        };
    }

    /**
     * Returns {@code true} if {@code card} may be played outside the current
     * player's turn (e.g. as a reaction to another player's action).
     */
    public static boolean canPlayOutOfTurn(Card card, GameState state) {
        if (card.type() != CardType.SPECIAL_FOUR) {
            return false;
        }
        return card.effect() == SpecialEffect.COUNTERATTACK
                || card.effect() == SpecialEffect.NICE_TRY;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * COLOR card rules:
     * If a request is active, the card must satisfy at least one active request.
     * Otherwise: same color OR same number as the top of discard.
     */
    private static boolean canPlayColorCard(Card card, Card top, GameState state) {
        CardColor reqColor = state.getRequestedColor();
        Integer reqNumber = state.getRequestedNumber();

        if (reqColor != null || reqNumber != null) {
            boolean matchesColor = reqColor != null && card.color() == reqColor;
            boolean matchesNumber = reqNumber != null && card.value() == reqNumber;
            return matchesColor || matchesNumber;
        }

        return card.color() == top.color() || card.value() == top.value();
    }

    /**
     * BLACK card rules:
     * Same number as top AND top must not be a black card.
     * Active requests do not affect black card play eligibility.
     */
    private static boolean canPlayBlackCard(Card card, Card top, GameState state) {
        Integer reqNumber = state.getRequestedNumber();

        if (reqNumber != null) {
            return top.type() != CardType.BLACK && card.value() == reqNumber;
        }

        return top.type() != CardType.BLACK && card.value() == top.value();
    }

    /**
     * SPECIAL_SINGLE card rules:
     * If a color request is active, the card's color must match it.
     * If a number request is active, single-color special cards are not valid,
     * because they do not satisfy a numeric request.
     * Otherwise: same color as top OR same symbol (same effect) as top.
     */
    private static boolean canPlaySpecialSingle(Card card, Card top, GameState state) {
        CardColor reqColor = state.getRequestedColor();
        Integer reqNumber = state.getRequestedNumber();

        if (reqColor != null) {
            return card.color() == reqColor;
        }

        if (reqNumber != null) {
            return false;
        }

        boolean sameColor = card.color() == top.color();
        boolean sameSymbol = top.effect() != null && card.effect() == top.effect();
        return sameColor || sameSymbol;
    }
}
