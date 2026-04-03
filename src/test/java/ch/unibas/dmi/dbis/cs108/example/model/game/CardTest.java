package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    // --- Color cards ---

    @Test
    void colorCard_scoringValueEqualsValue() {
        Card card = Card.colorCard(0, CardColor.RED, 5);
        assertEquals(5, card.scoringValue());
    }

    @Test
    void colorCard_scoringValueEqualsValue_minBoundary() {
        Card card = Card.colorCard(0, CardColor.GREEN, 1);
        assertEquals(1, card.scoringValue());
    }

    @Test
    void colorCard_scoringValueEqualsValue_maxBoundary() {
        Card card = Card.colorCard(0, CardColor.BLUE, 9);
        assertEquals(9, card.scoringValue());
    }

    @Test
    void colorCard_hasCorrectTypeAndColor() {
        Card card = Card.colorCard(0, CardColor.YELLOW, 7);
        assertEquals(CardType.COLOR, card.type());
        assertEquals(CardColor.YELLOW, card.color());
        assertEquals(7, card.value());
        assertNull(card.effect());
    }

    // --- Black cards ---

    @Test
    void blackCard_scoringValueIsDoubleValue() {
        Card card = Card.blackCard(72, 3);
        assertEquals(6, card.scoringValue());
    }

    @Test
    void blackCard_scoringValueIsDoubleValue_maxBoundary() {
        Card card = Card.blackCard(80, 9);
        assertEquals(18, card.scoringValue());
    }

    @Test
    void blackCard_hasCorrectTypeAndColor() {
        Card card = Card.blackCard(72, 4);
        assertEquals(CardType.BLACK, card.type());
        assertEquals(CardColor.BLACK, card.color());
        assertEquals(4, card.value());
        assertNull(card.effect());
    }

    // --- Single-color special cards ---

    @Test
    void specialSingleCard_scoringValueIsTen() {
        Card card = Card.specialSingleCard(81, CardColor.RED, SpecialEffect.SKIP);
        assertEquals(10, card.scoringValue());
    }

    @Test
    void specialSingleCard_allEffects_scoringValueIsTen() {
        for (SpecialEffect effect : new SpecialEffect[]{
                SpecialEffect.SECOND_CHANCE, SpecialEffect.SKIP,
                SpecialEffect.GIFT, SpecialEffect.EXCHANGE}) {
            Card card = Card.specialSingleCard(0, CardColor.BLUE, effect);
            assertEquals(10, card.scoringValue(),
                    "Expected scoring value 10 for SPECIAL_SINGLE with effect " + effect);
        }
    }

    @Test
    void specialSingleCard_hasCorrectFields() {
        Card card = Card.specialSingleCard(85, CardColor.GREEN, SpecialEffect.GIFT);
        assertEquals(CardType.SPECIAL_SINGLE, card.type());
        assertEquals(CardColor.GREEN, card.color());
        assertEquals(0, card.value());
        assertEquals(SpecialEffect.GIFT, card.effect());
    }

    // --- Four-color special cards ---

    @Test
    void specialFourCard_scoringValueIsTwenty() {
        Card card = Card.specialFourCard(101, SpecialEffect.FANTASTIC);
        assertEquals(20, card.scoringValue());
    }

    @Test
    void specialFourCard_allEffects_scoringValueIsTwenty() {
        for (SpecialEffect effect : new SpecialEffect[]{
                SpecialEffect.FANTASTIC, SpecialEffect.FANTASTIC_FOUR,
                SpecialEffect.EQUALITY, SpecialEffect.COUNTERATTACK,
                SpecialEffect.NICE_TRY}) {
            Card card = Card.specialFourCard(0, effect);
            assertEquals(20, card.scoringValue(),
                    "Expected scoring value 20 for SPECIAL_FOUR with effect " + effect);
        }
    }

    @Test
    void specialFourCard_hasNullColor() {
        Card card = Card.specialFourCard(101, SpecialEffect.EQUALITY);
        assertEquals(CardType.SPECIAL_FOUR, card.type());
        assertNull(card.color());
        assertEquals(0, card.value());
    }

    // --- Fuck You card ---

    @Test
    void fuckYouCard_scoringValueIsSixtyNine() {
        Card card = Card.fuckYouCard(124);
        assertEquals(69, card.scoringValue());
    }

    @Test
    void fuckYouCard_hasCorrectFields() {
        Card card = Card.fuckYouCard(124);
        assertEquals(CardType.FUCK_YOU, card.type());
        assertNull(card.color());
        assertEquals(0, card.value());
        assertNull(card.effect());
    }

    // --- Event cards ---

    @Test
    void eventCard_scoringValueIsZero() {
        Card card = Card.eventCard(0);
        assertEquals(0, card.scoringValue());
    }

    @Test
    void eventCard_hasCorrectFields() {
        Card card = Card.eventCard(5);
        assertEquals(CardType.EVENT, card.type());
        assertNull(card.color());
        assertEquals(0, card.value());
        assertNull(card.effect());
    }

    // --- Identity ---

    @Test
    void card_idIsPreserved() {
        Card card = Card.colorCard(42, CardColor.RED, 3);
        assertEquals(42, card.id());
    }

    @Test
    void cards_withSameFieldsAreEqual() {
        Card a = Card.colorCard(7, CardColor.RED, 5);
        Card b = Card.colorCard(7, CardColor.RED, 5);
        assertEquals(a, b);
    }

    @Test
    void cards_withDifferentIdsAreNotEqual() {
        Card a = Card.colorCard(1, CardColor.RED, 5);
        Card b = Card.colorCard(2, CardColor.RED, 5);
        assertNotEquals(a, b);
    }
}
