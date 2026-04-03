package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CardValidator}.
 *
 * Each test builds a minimal but real {@link GameState} and mutates only
 * the fields relevant to the scenario under test (discard top, requests,
 * hand size), avoiding any reliance on mocking.
 */
class CardValidatorTest {

    private GameState state;
    private PlayerGameState currentPlayer;

    @BeforeEach
    void setUp() {
        // Real game state with two players; current player is first alphabetically.
        state = GameInitializer.initialize(
                List.of("Alice", "Bob"), 1, null, new Random(42));
        currentPlayer = state.getCurrentPlayer();
        // Clear the real hand so tests control hand size precisely.
        currentPlayer.getHand().clear();
    }

    // =========================================================================
    // COLOR cards
    // =========================================================================

    @Test
    void colorCard_sameColor_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        assertTrue(canPlay(Card.colorCard(1, CardColor.RED, 5)));
    }

    @Test
    void colorCard_sameNumber_isValid() {
        setTop(Card.colorCard(0, CardColor.BLUE, 5));
        assertTrue(canPlay(Card.colorCard(1, CardColor.RED, 5)));
    }

    @Test
    void colorCard_differentColorAndNumber_isInvalid() {
        setTop(Card.colorCard(0, CardColor.BLUE, 3));
        assertFalse(canPlay(Card.colorCard(1, CardColor.RED, 5)));
    }

    // --- Requests override normal matching ---

    @Test
    void colorCard_withRequestedColor_mustMatchRequestedColor() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.BLUE);
        // RED/5 would normally match by color, but request says BLUE
        assertFalse(canPlay(Card.colorCard(1, CardColor.RED, 5)));
    }

    @Test
    void colorCard_matchesRequestedColor_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.BLUE);
        assertTrue(canPlay(Card.colorCard(1, CardColor.BLUE, 7)));
    }

    @Test
    void colorCard_matchesRequestedNumber_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedNumber(7);
        assertTrue(canPlay(Card.colorCard(1, CardColor.GREEN, 7)));
    }

    @Test
    void colorCard_matchesNeitherRequest_isInvalid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.BLUE);
        state.setRequestedNumber(7);
        assertFalse(canPlay(Card.colorCard(1, CardColor.RED, 5)));
    }

    @Test
    void colorCard_matchesOneOfTwoRequests_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.BLUE);
        state.setRequestedNumber(7);
        // Matches number request (7), not color request
        assertTrue(canPlay(Card.colorCard(1, CardColor.GREEN, 7)));
    }

    // =========================================================================
    // BLACK cards
    // =========================================================================

    @Test
    void blackCard_sameNumberOnColorCard_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 4));
        assertTrue(canPlay(Card.blackCard(1, 4)));
    }

    @Test
    void blackCard_differentNumber_isInvalid() {
        setTop(Card.colorCard(0, CardColor.RED, 4));
        assertFalse(canPlay(Card.blackCard(1, 5)));
    }

    @Test
    void blackCard_onBlackCard_isInvalid() {
        setTop(Card.blackCard(0, 4));
        assertFalse(canPlay(Card.blackCard(1, 4)));
    }

    @Test
    void blackCard_requestedColor_doesNotOverrideBlackRules() {
        // Requests do not affect black card eligibility
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.BLUE);
        // BLACK/3 matches the number but requests don't apply to black cards
        assertTrue(canPlay(Card.blackCard(1, 3)));
    }

    @Test
    void blackCard_sameNumberOnSpecialSingle_isInvalid() {
        // SPECIAL_SINGLE has value 0; BLACK/0 doesn't exist, but value match on 0 would pass
        // More importantly: black cannot be played on a special that has a non-matching value
        setTop(Card.specialSingleCard(0, CardColor.RED, SpecialEffect.SKIP));
        // top.type() == SPECIAL_SINGLE, not BLACK — so the only guard is "not black on black"
        // BLACK/0 does not exist in real deck, but the rule only blocks BLACK-on-BLACK
        // top value is 0, black card value must be 0 to match → BLACK cards have values 1-9
        assertFalse(canPlay(Card.blackCard(1, 5)));
    }

    // =========================================================================
    // SPECIAL_SINGLE cards
    // =========================================================================

    @Test
    void specialSingle_sameColor_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 7));
        assertTrue(canPlay(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP)));
    }

    @Test
    void specialSingle_sameSymbol_isValid() {
        setTop(Card.specialSingleCard(0, CardColor.GREEN, SpecialEffect.SKIP));
        assertTrue(canPlay(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP)));
    }

    @Test
    void specialSingle_differentColorAndSymbol_isInvalid() {
        setTop(Card.colorCard(0, CardColor.BLUE, 3));
        assertFalse(canPlay(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP)));
    }

    @Test
    void specialSingle_colorCardOnTop_noSymbolMatch_differentColor_isInvalid() {
        // Top is a color card (no effect), so symbol matching can't apply
        setTop(Card.colorCard(0, CardColor.BLUE, 3));
        assertFalse(canPlay(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.GIFT)));
    }

    @Test
    void specialSingle_withRequestedColor_mustMatchRequestedColor() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.GREEN);
        // RED SKIP would normally match color, but request says GREEN
        assertFalse(canPlay(Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP)));
    }

    @Test
    void specialSingle_matchesRequestedColor_isValid() {
        setTop(Card.colorCard(0, CardColor.RED, 3));
        state.setRequestedColor(CardColor.GREEN);
        assertTrue(canPlay(Card.specialSingleCard(1, CardColor.GREEN, SpecialEffect.SKIP)));
    }

    @Test
    void specialSingle_allSingleEffects_sameColor_areValid() {
        setTop(Card.colorCard(0, CardColor.BLUE, 3));
        for (SpecialEffect effect : new SpecialEffect[]{
                SpecialEffect.SECOND_CHANCE, SpecialEffect.SKIP,
                SpecialEffect.GIFT, SpecialEffect.EXCHANGE}) {
            assertTrue(canPlay(Card.specialSingleCard(1, CardColor.BLUE, effect)),
                    "SPECIAL_SINGLE/" + effect + " on same color should be valid");
        }
    }

    // =========================================================================
    // SPECIAL_FOUR cards
    // =========================================================================

    @Test
    void specialFour_alwaysValid_onColorCard() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        assertTrue(canPlay(Card.specialFourCard(1, SpecialEffect.FANTASTIC)));
    }

    @Test
    void specialFour_alwaysValid_onBlackCard() {
        setTop(Card.blackCard(0, 5));
        assertTrue(canPlay(Card.specialFourCard(1, SpecialEffect.EQUALITY)));
    }

    @Test
    void specialFour_alwaysValid_onSpecialFour() {
        setTop(Card.specialFourCard(0, SpecialEffect.FANTASTIC));
        assertTrue(canPlay(Card.specialFourCard(1, SpecialEffect.FANTASTIC_FOUR)));
    }

    @Test
    void specialFour_allFourColorEffects_areAlwaysValid() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        for (SpecialEffect effect : new SpecialEffect[]{
                SpecialEffect.FANTASTIC, SpecialEffect.FANTASTIC_FOUR,
                SpecialEffect.EQUALITY, SpecialEffect.COUNTERATTACK,
                SpecialEffect.NICE_TRY}) {
            assertTrue(canPlay(Card.specialFourCard(1, effect)),
                    "SPECIAL_FOUR/" + effect + " should always be valid during turn");
        }
    }

    // =========================================================================
    // FUCK_YOU card
    // =========================================================================

    @Test
    void fuckYouCard_validWhenHandSizeIsExactly10() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        setHandSize(10);
        assertTrue(canPlay(Card.fuckYouCard(1)));
    }

    @Test
    void fuckYouCard_invalidWhenHandSizeIs9() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        setHandSize(9);
        assertFalse(canPlay(Card.fuckYouCard(1)));
    }

    @Test
    void fuckYouCard_invalidWhenHandSizeIs11() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        setHandSize(11);
        assertFalse(canPlay(Card.fuckYouCard(1)));
    }

    @Test
    void fuckYouCard_invalidWhenHandIsEmpty() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        // hand was already cleared in setUp
        assertFalse(canPlay(Card.fuckYouCard(1)));
    }

    // =========================================================================
    // EVENT cards
    // =========================================================================

    @Test
    void eventCard_neverValid() {
        setTop(Card.colorCard(0, CardColor.RED, 5));
        assertFalse(canPlay(Card.eventCard(0)));
    }

    // =========================================================================
    // Out-of-turn play
    // =========================================================================

    @Test
    void counterattack_canBePlayedOutOfTurn() {
        assertTrue(CardValidator.canPlayOutOfTurn(
                Card.specialFourCard(1, SpecialEffect.COUNTERATTACK), state));
    }

    @Test
    void niceTry_canBePlayedOutOfTurn() {
        assertTrue(CardValidator.canPlayOutOfTurn(
                Card.specialFourCard(1, SpecialEffect.NICE_TRY), state));
    }

    @Test
    void fantastic_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(
                Card.specialFourCard(1, SpecialEffect.FANTASTIC), state));
    }

    @Test
    void fantasticFour_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(
                Card.specialFourCard(1, SpecialEffect.FANTASTIC_FOUR), state));
    }

    @Test
    void equality_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(
                Card.specialFourCard(1, SpecialEffect.EQUALITY), state));
    }

    @Test
    void colorCard_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(
                Card.colorCard(1, CardColor.RED, 5), state));
    }

    @Test
    void specialSingle_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(
                Card.specialSingleCard(1, CardColor.RED, SpecialEffect.SKIP), state));
    }

    @Test
    void blackCard_cannotBePlayedOutOfTurn() {
        assertFalse(CardValidator.canPlayOutOfTurn(Card.blackCard(1, 5), state));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Replaces the top of the discard pile with the given card. */
    private void setTop(Card card) {
        state.getDiscardPile().clear();
        state.pushToDiscardPile(card);
    }

    /** Fills the current player's hand with dummy color cards to reach the target size. */
    private void setHandSize(int size) {
        currentPlayer.getHand().clear();
        for (int i = 0; i < size; i++) {
            currentPlayer.addCard(Card.colorCard(200 + i, CardColor.GREEN, 1));
        }
    }

    private boolean canPlay(Card card) {
        return CardValidator.canPlay(card, state.peekDiscardPile(), state);
    }
}
