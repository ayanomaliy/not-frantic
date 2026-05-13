package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds and shuffles the card decks used in a game round.
 *
 * <h2>Main deck composition (125 cards)</h2>
 * <ul>
 *   <li>IDs 0–71: 72 color cards (4 colors × values 1–9 × 2 copies)</li>
 *   <li>IDs 72–80: 9 black cards (values 1–9)</li>
 *   <li>IDs 81–100: 20 single-color special cards (5 per color)</li>
 *   <li>IDs 101–123: 23 four-color special cards</li>
 *   <li>ID 124: 1 Fuck You card</li>
 * </ul>
 *
 * <h2>Single-color special distribution (5 per color)</h2>
 * Each color has one of each of the 4 effects plus one extra, cycling per color:
 * RED +SECOND_CHANCE, GREEN +SKIP, BLUE +GIFT, YELLOW +EXCHANGE.
 *
 * <h2>Four-color special distribution (23 cards)</h2>
 * FANTASTIC ×5, FANTASTIC_FOUR ×5, EQUALITY ×5, COUNTERATTACK ×4, NICE_TRY ×4.
 *
 * <h2>Event deck (20 cards)</h2>
 * IDs 0–19, all of type {@link CardType#EVENT}.
 */
public class DeckFactory {

    private DeckFactory() {}

    /**
     * Builds a fresh, unshuffled main deck of 125 cards.
     */
    public static List<Card> buildMainDeck() {
        List<Card> deck = new ArrayList<>(121);
        int id = 0;

        // --- 72 color cards: 4 colors × values 1–9 × 2 copies ---
        CardColor[] colors = {CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.YELLOW};
        for (CardColor color : colors) {
            for (int value = 1; value <= 9; value++) {
                deck.add(Card.colorCard(id++, color, value));
                deck.add(Card.colorCard(id++, color, value));
            }
        }
        // id == 72

        // --- 9 black cards: values 1–9 ---
        for (int value = 1; value <= 9; value++) {
            deck.add(Card.blackCard(id++, value));
        }
        // id == 81

        // --- 20 single-color special cards (5 per color) ---
        // Base: one of each effect per color (4 per color = 16 total).
        // Extra card per color cycles through effects to reach 5 per color:
        //   RED +SECOND_CHANCE, GREEN +SKIP, BLUE +GIFT, YELLOW +EXCHANGE
        SpecialEffect[] singleEffects = {
            SpecialEffect.SECOND_CHANCE, SpecialEffect.SKIP,
            SpecialEffect.GIFT, SpecialEffect.EXCHANGE
        };
        for (int c = 0; c < colors.length; c++) {
            // one of each effect
            for (SpecialEffect effect : singleEffects) {
                deck.add(Card.specialSingleCard(id++, colors[c], effect));
            }
            // extra card: the effect at index c (cycles RED→0, GREEN→1, BLUE→2, YELLOW→3)
            deck.add(Card.specialSingleCard(id++, colors[c], singleEffects[c]));
        }
        // id == 101

// --- Four-color special cards ---
// FANTASTIC ×5, FANTASTIC_FOUR ×5, EQUALITY ×5, NICE_TRY ×4
// COUNTERATTACK ×4 removed because the effect is currently unstable.
// We intentionally skip IDs 116–119 so old card-id assumptions do not shift.

        int[] fourColorCounts = {5, 5, 5};
        SpecialEffect[] fourEffects = {
                SpecialEffect.FANTASTIC,
                SpecialEffect.FANTASTIC_FOUR,
                SpecialEffect.EQUALITY
        };

        for (int i = 0; i < fourEffects.length; i++) {
            for (int j = 0; j < fourColorCounts[i]; j++) {
                deck.add(Card.specialFourCard(id++, fourEffects[i]));
            }
        }

// id == 116 here.
// Skip old COUNTERATTACK IDs 116–119.
        id += 4;

// id == 120 here.
// Add NICE_TRY ×4 with original IDs 120–123.
        for (int j = 0; j < 4; j++) {
            deck.add(Card.specialFourCard(id++, SpecialEffect.NICE_TRY));
        }

// id == 124

        // --- 1 Fuck You card ---
        deck.add(Card.fuckYouCard(id++));
        // id == 125

        return deck;
    }

    /**
     * Builds a fresh, unshuffled event deck of 20 cards (IDs 0–19).
     */
    public static List<Card> buildEventDeck() {
        List<Card> deck = new ArrayList<>(20);
        for (int id = 0; id < 20; id++) {
            deck.add(Card.eventCard(id));
        }
        return deck;
    }

    /**
     * Shuffles the given deck in place using the provided {@link Random} source.
     * Passing a seeded {@code Random} produces a deterministic result.
     */
    public static void shuffle(List<Card> deck, Random rng) {
        Collections.shuffle(deck, rng);
    }
}
