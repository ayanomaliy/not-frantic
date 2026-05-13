package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DeckFactoryTest {

    private List<Card> mainDeck;
    private List<Card> eventDeck;

    @BeforeEach
    void setUp() {
        mainDeck = DeckFactory.buildMainDeck();
        eventDeck = DeckFactory.buildEventDeck();
    }

    // --- Main deck size ---

    @Test
    void mainDeck_hasExactly121Cards() {
        assertEquals(121, mainDeck.size());
    }

    // --- Color cards ---

    @Test
    void mainDeck_hasExactly72ColorCards() {
        long count = mainDeck.stream()
                .filter(c -> c.type() == CardType.COLOR)
                .count();

        assertEquals(72, count);
    }

    @Test
    void mainDeck_colorCards_eachValueAppearsExactlyTwicePerColor() {
        CardColor[] colors = {
                CardColor.RED,
                CardColor.GREEN,
                CardColor.BLUE,
                CardColor.YELLOW
        };

        for (CardColor color : colors) {
            for (int value = 1; value <= 9; value++) {
                final int v = value;

                long count = mainDeck.stream()
                        .filter(c -> c.type() == CardType.COLOR)
                        .filter(c -> c.color() == color)
                        .filter(c -> c.value() == v)
                        .count();

                assertEquals(
                        2,
                        count,
                        "Expected 2 copies of " + color + "/" + value + " but found " + count
                );
            }
        }
    }

    @Test
    void mainDeck_colorCards_valuesAreInRange1To9() {
        mainDeck.stream()
                .filter(c -> c.type() == CardType.COLOR)
                .forEach(c -> assertTrue(
                        c.value() >= 1 && c.value() <= 9,
                        "Color card value out of range: " + c.value()
                ));
    }

    // --- Black cards ---

    @Test
    void mainDeck_hasExactly9BlackCards() {
        long count = mainDeck.stream()
                .filter(c -> c.type() == CardType.BLACK)
                .count();

        assertEquals(9, count);
    }

    @Test
    void mainDeck_blackCards_valuesAre1To9WithNoDuplicates() {
        List<Integer> values = mainDeck.stream()
                .filter(c -> c.type() == CardType.BLACK)
                .map(Card::value)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), values);
    }

    @Test
    void mainDeck_blackCards_allHaveBlackColor() {
        mainDeck.stream()
                .filter(c -> c.type() == CardType.BLACK)
                .forEach(c -> assertEquals(CardColor.BLACK, c.color()));
    }

    // --- Single-color special cards ---

    @Test
    void mainDeck_hasExactly20SpecialSingleCards() {
        long count = mainDeck.stream()
                .filter(c -> c.type() == CardType.SPECIAL_SINGLE)
                .count();

        assertEquals(20, count);
    }

    @Test
    void mainDeck_specialSingleCards_exactly5PerColor() {
        CardColor[] colors = {
                CardColor.RED,
                CardColor.GREEN,
                CardColor.BLUE,
                CardColor.YELLOW
        };

        for (CardColor color : colors) {
            long count = mainDeck.stream()
                    .filter(c -> c.type() == CardType.SPECIAL_SINGLE)
                    .filter(c -> c.color() == color)
                    .count();

            assertEquals(
                    5,
                    count,
                    "Expected 5 SPECIAL_SINGLE cards for color " + color
            );
        }
    }

    @Test
    void mainDeck_specialSingleCards_onlyHaveSingleColorEffects() {
        Set<SpecialEffect> singleEffects = Set.of(
                SpecialEffect.SECOND_CHANCE,
                SpecialEffect.SKIP,
                SpecialEffect.GIFT,
                SpecialEffect.EXCHANGE
        );

        mainDeck.stream()
                .filter(c -> c.type() == CardType.SPECIAL_SINGLE)
                .forEach(c -> assertTrue(
                        singleEffects.contains(c.effect()),
                        "Unexpected effect on SPECIAL_SINGLE card: " + c.effect()
                ));
    }

    // --- Four-color special cards ---

    @Test
    void mainDeck_hasExactly19SpecialFourCards() {
        long count = mainDeck.stream()
                .filter(c -> c.type() == CardType.SPECIAL_FOUR)
                .count();

        assertEquals(19, count);
    }

    @Test
    void mainDeck_specialFourCards_allHaveNullColor() {
        mainDeck.stream()
                .filter(c -> c.type() == CardType.SPECIAL_FOUR)
                .forEach(c -> assertNull(c.color()));
    }

    @Test
    void mainDeck_specialFourCards_countPerEffect() {
        assertEquals(5, countEffect(mainDeck, SpecialEffect.FANTASTIC));
        assertEquals(5, countEffect(mainDeck, SpecialEffect.FANTASTIC_FOUR));
        assertEquals(5, countEffect(mainDeck, SpecialEffect.EQUALITY));
        assertEquals(4, countEffect(mainDeck, SpecialEffect.NICE_TRY));
    }

    // --- Fuck You card ---

    @Test
    void mainDeck_hasExactly1FuckYouCard() {
        long count = mainDeck.stream()
                .filter(c -> c.type() == CardType.FUCK_YOU)
                .count();

        assertEquals(1, count);
    }

    @Test
    void mainDeck_fuckYouCard_exists() {
        assertTrue(
                mainDeck.stream().anyMatch(c -> c.type() == CardType.FUCK_YOU),
                "Expected exactly one Fuck You card in the deck"
        );
    }

    // --- Unique IDs ---

    @Test
    void mainDeck_allCardIdsAreUnique() {
        Set<Integer> ids = new HashSet<>();

        for (Card card : mainDeck) {
            assertTrue(ids.add(card.id()), "Duplicate card id: " + card.id());
        }
    }

    @Test
    void mainDeck_cardIdsAreInRange0To124() {
        mainDeck.forEach(c -> assertTrue(
                c.id() >= 0 && c.id() <= 124,
                "Card id out of range: " + c.id()
        ));
    }

    // --- Event deck ---

    @Test
    void eventDeck_hasExactly20Cards() {
        assertEquals(20, eventDeck.size());
    }

    @Test
    void eventDeck_allCardsAreEventType() {
        eventDeck.forEach(c -> assertEquals(CardType.EVENT, c.type()));
    }

    @Test
    void eventDeck_allCardIdsAreUnique() {
        Set<Integer> ids = eventDeck.stream()
                .map(Card::id)
                .collect(Collectors.toSet());

        assertEquals(20, ids.size());
    }

    // --- Shuffle ---

    @Test
    void shuffle_producesDifferentOrderFromOriginal() {
        List<Card> deck = DeckFactory.buildMainDeck();
        List<Integer> originalIds = deck.stream()
                .map(Card::id)
                .collect(Collectors.toList());

        DeckFactory.shuffle(deck, new Random(42));

        List<Integer> shuffledIds = deck.stream()
                .map(Card::id)
                .collect(Collectors.toList());

        assertNotEquals(
                originalIds,
                shuffledIds,
                "Shuffled deck should differ from original order"
        );
    }

    @Test
    void shuffle_preservesAllCards() {
        List<Card> deck = DeckFactory.buildMainDeck();

        Set<Integer> before = deck.stream()
                .map(Card::id)
                .collect(Collectors.toSet());

        DeckFactory.shuffle(deck, new Random(99));

        Set<Integer> after = deck.stream()
                .map(Card::id)
                .collect(Collectors.toSet());

        assertEquals(before, after, "Shuffle must not add or remove cards");
    }

    @Test
    void shuffle_withSameSeed_producesSameOrder() {
        List<Card> deck1 = DeckFactory.buildMainDeck();
        List<Card> deck2 = DeckFactory.buildMainDeck();

        DeckFactory.shuffle(deck1, new Random(1234));
        DeckFactory.shuffle(deck2, new Random(1234));

        List<Integer> ids1 = deck1.stream()
                .map(Card::id)
                .collect(Collectors.toList());

        List<Integer> ids2 = deck2.stream()
                .map(Card::id)
                .collect(Collectors.toList());

        assertEquals(ids1, ids2, "Same seed must produce same shuffle order");
    }

    // --- Helper ---

    private long countEffect(List<Card> deck, SpecialEffect effect) {
        return deck.stream()
                .filter(c -> c.effect() == effect)
                .count();
    }
}