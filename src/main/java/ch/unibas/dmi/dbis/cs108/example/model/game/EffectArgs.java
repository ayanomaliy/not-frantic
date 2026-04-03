package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.List;

/**
 * Carries the optional client-supplied parameters needed to resolve a
 * {@link SpecialEffect}. Build instances through the static factory methods.
 *
 * <table border="1">
 * <caption>Which fields each effect uses</caption>
 * <tr><th>Effect</th><th>targetPlayer</th><th>chosenColor</th><th>chosenNumber</th><th>selectedCards</th></tr>
 * <tr><td>SECOND_CHANCE</td><td></td><td></td><td></td><td>card to play (0 or 1)</td></tr>
 * <tr><td>SKIP</td><td>player to skip</td><td></td><td></td><td></td></tr>
 * <tr><td>GIFT</td><td>recipient</td><td></td><td></td><td>1–2 cards from actor</td></tr>
 * <tr><td>EXCHANGE</td><td>exchange partner</td><td></td><td></td><td>2 cards from actor</td></tr>
 * <tr><td>FANTASTIC</td><td></td><td>requested</td><td>requested</td><td></td></tr>
 * <tr><td>FANTASTIC_FOUR</td><td></td><td>requested</td><td>requested</td><td></td></tr>
 * <tr><td>EQUALITY</td><td>player who draws</td><td>requested</td><td></td><td></td></tr>
 * <tr><td>COUNTERATTACK</td><td>new target</td><td></td><td></td><td></td></tr>
 * <tr><td>NICE_TRY</td><td>player who emptied hand</td><td></td><td></td><td></td></tr>
 * </table>
 */
public final class EffectArgs {

    private final String targetPlayer;
    private final CardColor chosenColor;
    private final Integer chosenNumber;
    private final List<Card> selectedCards;

    private EffectArgs(String targetPlayer, CardColor chosenColor,
                       Integer chosenNumber, List<Card> selectedCards) {
        this.targetPlayer = targetPlayer;
        this.chosenColor = chosenColor;
        this.chosenNumber = chosenNumber;
        this.selectedCards = selectedCards == null ? List.of() : List.copyOf(selectedCards);
    }

    // --- Static factories ---

    /** No arguments — used when an effect needs no client input. */
    public static EffectArgs empty() {
        return new EffectArgs(null, null, null, null);
    }

    /** SKIP / COUNTERATTACK / NICE_TRY: only a target player name is needed. */
    public static EffectArgs withTarget(String targetPlayer) {
        return new EffectArgs(targetPlayer, null, null, null);
    }

    /** FANTASTIC: request a color only. */
    public static EffectArgs withColor(CardColor color) {
        return new EffectArgs(null, color, null, null);
    }

    /** FANTASTIC / FANTASTIC_FOUR: request a color and/or number. */
    public static EffectArgs withColorAndNumber(CardColor color, Integer number) {
        return new EffectArgs(null, color, number, null);
    }

    /** EQUALITY: target player plus the color the actor wishes to request. */
    public static EffectArgs withTargetAndColor(String targetPlayer, CardColor color) {
        return new EffectArgs(targetPlayer, color, null, null);
    }

    /** GIFT: recipient plus the cards to transfer from the actor's hand. */
    public static EffectArgs withTargetAndCards(String targetPlayer, List<Card> cards) {
        return new EffectArgs(targetPlayer, null, null, cards);
    }

    /** SECOND_CHANCE: card the actor wants to play (empty list = draw instead). */
    public static EffectArgs withCards(List<Card> cards) {
        return new EffectArgs(null, null, null, cards);
    }

    /** Full constructor for cases that need all fields. */
    public static EffectArgs of(String targetPlayer, CardColor chosenColor,
                                Integer chosenNumber, List<Card> selectedCards) {
        return new EffectArgs(targetPlayer, chosenColor, chosenNumber, selectedCards);
    }

    // --- Getters ---

    public String getTargetPlayer() { return targetPlayer; }
    public CardColor getChosenColor() { return chosenColor; }
    public Integer getChosenNumber() { return chosenNumber; }
    public List<Card> getSelectedCards() { return selectedCards; }
}
