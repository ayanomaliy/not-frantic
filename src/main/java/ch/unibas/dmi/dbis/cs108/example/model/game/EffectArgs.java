package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.List;

/**
 * Carries the optional client-supplied parameters needed to resolve a
 * {@link SpecialEffect}. Build instances through the static factory methods.
 *
 * <table border="1">
 * <caption>Which fields each effect uses</caption>
 * <tr>
 *   <th>Effect</th>
 *   <th>targetPlayer</th>
 *   <th>chosenColor</th>
 *   <th>chosenNumber</th>
 *   <th>selectedCards</th>
 *   <th>targetPlayers</th>
 * </tr>
 * <tr><td>SECOND_CHANCE</td><td></td><td></td><td></td><td>card to play (0 or 1)</td><td></td></tr>
 * <tr><td>SKIP</td><td>player to skip</td><td></td><td></td><td></td><td></td></tr>
 * <tr><td>GIFT</td><td>recipient</td><td></td><td></td><td>1–2 cards from actor</td><td></td></tr>
 * <tr><td>EXCHANGE</td><td>exchange partner</td><td></td><td></td><td>2 cards from actor</td><td></td></tr>
 * <tr><td>FANTASTIC</td><td></td><td>requested</td><td>requested</td><td></td><td></td></tr>
 * <tr><td>FANTASTIC_FOUR</td><td></td><td>requested</td><td>requested</td><td></td><td>exactly 4 recipients, repeats allowed</td></tr>
 * <tr><td>EQUALITY</td><td>player who draws</td><td>requested</td><td></td><td></td><td></td></tr>
 * <tr><td>COUNTERATTACK</td><td>new target</td><td>requested</td><td></td><td></td><td></td></tr>
 * <tr><td>NICE_TRY</td><td>player who emptied hand</td><td></td><td></td><td></td><td></td></tr>
 * </table>
 */
public final class EffectArgs {

    private final String targetPlayer;
    private final CardColor chosenColor;
    private final Integer chosenNumber;
    private final List<Card> selectedCards;
    private final List<String> targetPlayers;

    /**
     * Creates a new immutable effect-argument bundle.
     *
     * @param targetPlayer the single target player, or {@code null}
     * @param chosenColor the requested color, or {@code null}
     * @param chosenNumber the requested number, or {@code null}
     * @param selectedCards the selected cards, or {@code null}
     * @param targetPlayers the list of target players, or {@code null}
     */
    private EffectArgs(String targetPlayer,
                       CardColor chosenColor,
                       Integer chosenNumber,
                       List<Card> selectedCards,
                       List<String> targetPlayers) {
        this.targetPlayer = targetPlayer;
        this.chosenColor = chosenColor;
        this.chosenNumber = chosenNumber;
        this.selectedCards = selectedCards == null ? List.of() : List.copyOf(selectedCards);
        this.targetPlayers = targetPlayers == null ? List.of() : List.copyOf(targetPlayers);
    }

    // --- Static factories ---

    /** No arguments — used when an effect needs no client input. */
    public static EffectArgs empty() {
        return new EffectArgs(null, null, null, null, null);
    }

    /** SKIP / NICE_TRY: only a target player name is needed. */
    public static EffectArgs withTarget(String targetPlayer) {
        return new EffectArgs(targetPlayer, null, null, null, null);
    }

    /** FANTASTIC: request a color only. */
    public static EffectArgs withColor(CardColor color) {
        return new EffectArgs(null, color, null, null, null);
    }

    /** FANTASTIC: request a color or a number. */
    public static EffectArgs withColorAndNumber(CardColor color, Integer number) {
        return new EffectArgs(null, color, number, null, null);
    }

    /**
     * FANTASTIC_FOUR: request a color or number and specify the four
     * recipients of the distributed cards. Repeated names are allowed.
     */
    public static EffectArgs withTargetsAndColorOrNumber(List<String> targets,
                                                         CardColor color,
                                                         Integer number) {
        return new EffectArgs(null, color, number, null, targets);
    }

    /** EQUALITY: target player plus the color the actor wishes to request. */
    public static EffectArgs withTargetAndColor(String targetPlayer, CardColor color) {
        return new EffectArgs(targetPlayer, color, null, null, null);
    }

    /** GIFT: recipient plus the cards to transfer from the actor's hand. */
    public static EffectArgs withTargetAndCards(String targetPlayer, List<Card> cards) {
        return new EffectArgs(targetPlayer, null, null, cards, null);
    }

    /** SECOND_CHANCE: card the actor wants to play (empty list = draw instead). */
    public static EffectArgs withCards(List<Card> cards) {
        return new EffectArgs(null, null, null, cards, null);
    }

    /** Full constructor for cases that need all fields except targetPlayers. */
    public static EffectArgs of(String targetPlayer,
                                CardColor chosenColor,
                                Integer chosenNumber,
                                List<Card> selectedCards) {
        return new EffectArgs(targetPlayer, chosenColor, chosenNumber, selectedCards, null);
    }

    /** Full constructor for cases that may also need multiple target players. */
    public static EffectArgs of(String targetPlayer,
                                CardColor chosenColor,
                                Integer chosenNumber,
                                List<Card> selectedCards,
                                List<String> targetPlayers) {
        return new EffectArgs(targetPlayer, chosenColor, chosenNumber, selectedCards, targetPlayers);
    }

    // --- Getters ---

    /** Returns the single target player, or {@code null} if none is used. */
    public String getTargetPlayer() {
        return targetPlayer;
    }

    /** Returns the chosen color, or {@code null} if none was chosen. */
    public CardColor getChosenColor() {
        return chosenColor;
    }

    /** Returns the chosen number, or {@code null} if none was chosen. */
    public Integer getChosenNumber() {
        return chosenNumber;
    }

    /** Returns the selected cards, or an empty list if none were supplied. */
    public List<Card> getSelectedCards() {
        return selectedCards;
    }

    /**
     * Returns the list of target players, or an empty list if none were supplied.
     *
     * <p>This is mainly used for effects such as {@code FANTASTIC_FOUR} where
     * multiple recipients must be specified.</p>
     */
    public List<String> getTargetPlayers() {
        return targetPlayers;
    }
}