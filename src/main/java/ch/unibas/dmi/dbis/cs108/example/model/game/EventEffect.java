package ch.unibas.dmi.dbis.cs108.example.model.game;

/**
 * The 20 distinct effects that can be triggered by playing a {@link CardType#BLACK} card.
 *
 * <p><b>Phase 9 placeholder — team must assign final definitions before
 * implementing {@link EventResolver}.</b>
 *
 * <p>Each constant maps 1-to-1 to an event card ID (0–19) via its
 * {@link #ordinal()}.  The brief descriptions below are working hypotheses;
 * the team should replace them with the agreed-upon rules before Phase 9.
 *
 * <ul>
 *   <li>Ordinal 0  – {@link #ALL_DRAW_TWO}           – all players draw 2 cards</li>
 *   <li>Ordinal 1  – {@link #ALL_DRAW_ONE}            – all players draw 1 card</li>
 *   <li>Ordinal 2  – {@link #ALL_SKIP}                – all other players skip next turn</li>
 *   <li>Ordinal 3  – {@link #INSTANT_ROUND_END}       – round ends immediately</li>
 *   <li>Ordinal 4  – {@link #REVERSE_ORDER}           – reverse play order for rest of round</li>
 *   <li>Ordinal 5  – {@link #STEAL_FROM_NEXT}         – take 1 card from next player</li>
 *   <li>Ordinal 6  – {@link #STEAL_FROM_PREV}         – take 1 card from previous player</li>
 *   <li>Ordinal 7  – {@link #DISCARD_HIGHEST}         – everyone discards their highest-value card</li>
 *   <li>Ordinal 8  – {@link #DISCARD_COLOR}           – everyone discards cards matching discard top color</li>
 *   <li>Ordinal 9  – {@link #SWAP_HANDS}              – triggering player swaps hand with a chosen player</li>
 *   <li>Ordinal 10 – {@link #BLOCK_SPECIALS}          – special cards cannot be played until next black card</li>
 *   <li>Ordinal 11 – {@link #GIFT_CHAIN}              – each player gives 1 card to the next in order</li>
 *   <li>Ordinal 12 – {@link #HAND_RESET}              – everyone discards hand and redraws to 7 cards</li>
 *   <li>Ordinal 13 – {@link #LUCKY_DRAW}              – triggering player draws 3 extra cards</li>
 *   <li>Ordinal 14 – {@link #PENALTY_DRAW}            – player with the most cards draws 2 more</li>
 *   <li>Ordinal 15 – {@link #EQUALIZE}                – all players draw or discard to match the median hand size</li>
 *   <li>Ordinal 16 – {@link #WILD_REQUEST}            – server requests any color for the next play</li>
 *   <li>Ordinal 17 – {@link #CANCEL_EFFECTS}          – all pending special effects are cancelled</li>
 *   <li>Ordinal 18 – {@link #BONUS_PLAY}              – triggering player may play one additional card</li>
 *   <li>Ordinal 19 – {@link #DOUBLE_SCORING}          – card scoring values are doubled for this round</li>
 * </ul>
 */
public enum EventEffect {

    // Ordinal 0
    /** All players draw 2 cards from the draw pile. */
    ALL_DRAW_TWO,

    // Ordinal 1
    /** All players draw 1 card from the draw pile. */
    ALL_DRAW_ONE,

    // Ordinal 2
    /** All players other than the triggering player must skip their next turn. */
    ALL_SKIP,

    // Ordinal 3
    /** The round ends immediately; scores are calculated now. */
    INSTANT_ROUND_END,

    // Ordinal 4
    /** Play order is reversed for the remainder of the round. */
    REVERSE_ORDER,

    // Ordinal 5
    /** The triggering player takes 1 card from the next player in turn order. */
    STEAL_FROM_NEXT,

    // Ordinal 6
    /** The triggering player takes 1 card from the previous player in turn order. */
    STEAL_FROM_PREV,

    // Ordinal 7
    /** Every player discards the single highest-scoring card in their hand. */
    DISCARD_HIGHEST,

    // Ordinal 8
    /** Every player discards all cards whose color matches the current discard-pile top. */
    DISCARD_COLOR,

    // Ordinal 9
    /** The triggering player swaps their entire hand with a player of their choice. */
    SWAP_HANDS,

    // Ordinal 10
    /** Special cards (SPECIAL_SINGLE and SPECIAL_FOUR) cannot be played until the next black card is played. */
    BLOCK_SPECIALS,

    // Ordinal 11
    /** Each player simultaneously passes 1 card to the next player in turn order. */
    GIFT_CHAIN,

    // Ordinal 12
    /** All players discard their entire hand and draw 7 new cards. */
    HAND_RESET,

    // Ordinal 13
    /** The triggering player draws 3 extra cards from the draw pile. */
    LUCKY_DRAW,

    // Ordinal 14
    /** The player currently holding the most cards must draw 2 more. */
    PENALTY_DRAW,

    // Ordinal 15
    /** All players draw cards (or the server discards from their hands) until everyone has the same count. */
    EQUALIZE,

    // Ordinal 16
    /** The server sets a color request for the next play (chosen randomly or by rule). */
    WILD_REQUEST,

    // Ordinal 17
    /** All pending special effects are cancelled; pendingEffects is cleared. */
    CANCEL_EFFECTS,

    // Ordinal 18
    /** The triggering player is allowed to play one additional card this turn. */
    BONUS_PLAY,

    // Ordinal 19
    /** Card scoring values are doubled when calculating scores at the end of this round. */
    DOUBLE_SCORING;

    /**
     * Returns the {@link EventEffect} whose {@link #ordinal()} equals {@code eventCardId}.
     * Event card IDs are 0–19 (matching the indices of this enum).
     *
     * @throws IllegalArgumentException if {@code eventCardId} is out of range.
     */
    public static EventEffect fromCardId(int eventCardId) {
        EventEffect[] values = values();
        if (eventCardId < 0 || eventCardId >= values.length) {
            throw new IllegalArgumentException(
                    "No EventEffect for card id " + eventCardId
                            + " (valid range 0–" + (values.length - 1) + ")");
        }
        return values[eventCardId];
    }
}
