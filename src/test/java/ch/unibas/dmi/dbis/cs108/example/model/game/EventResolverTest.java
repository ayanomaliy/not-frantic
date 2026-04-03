package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EventResolver} — all 20 event card effects.
 *
 * The parametrized smoke tests (no-throw, clears activeEventCard, non-empty list)
 * are preserved; the old stub-marker test is replaced by per-effect behaviour tests.
 */
class EventResolverTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        // 3 players, round 1, seed 42 → [Alice, Bob, Charlie] alphabetically,
        // each with 7 cards, draw pile ~103 cards.
        state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
    }

    // =========================================================================
    // EventEffect.fromCardId — mapping correctness
    // =========================================================================

    @Test
    void fromCardId_maps0to19_toCorrectOrdinals() {
        EventEffect[] values = EventEffect.values();
        assertEquals(20, values.length, "There must be exactly 20 EventEffect constants");
        for (int id = 0; id < 20; id++) {
            EventEffect effect = EventEffect.fromCardId(id);
            assertEquals(id, effect.ordinal(),
                    "EventEffect.fromCardId(" + id + ") should return ordinal " + id);
        }
    }

    @Test
    void fromCardId_outOfRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> EventEffect.fromCardId(-1));
        assertThrows(IllegalArgumentException.class, () -> EventEffect.fromCardId(20));
    }

    // =========================================================================
    // Parametrized smoke tests — all 20 cards must not throw, clear the card,
    // and return at least one event
    // =========================================================================

    static Stream<Integer> allEventCardIds() {
        return Stream.iterate(0, i -> i + 1).limit(20);
    }

    @ParameterizedTest(name = "event card id={0} resolves without exception")
    @MethodSource("allEventCardIds")
    void resolve_allEventCards_doNotThrow(int cardId) {
        // Re-init each time to avoid cross-effect contamination
        GameState s = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        Card eventCard = Card.eventCard(cardId);
        s.setActiveEventCard(eventCard);
        assertDoesNotThrow(() -> EventResolver.resolve(eventCard, s));
    }

    @ParameterizedTest(name = "event card id={0} clears activeEventCard")
    @MethodSource("allEventCardIds")
    void resolve_allEventCards_clearActiveEventCard(int cardId) {
        GameState s = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        Card eventCard = Card.eventCard(cardId);
        s.setActiveEventCard(eventCard);
        EventResolver.resolve(eventCard, s);
        assertNull(s.getActiveEventCard(),
                "activeEventCard must be cleared after resolution");
    }

    @ParameterizedTest(name = "event card id={0} returns non-empty event list")
    @MethodSource("allEventCardIds")
    void resolve_allEventCards_returnAtLeastOneEvent(int cardId) {
        GameState s = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        Card eventCard = Card.eventCard(cardId);
        s.setActiveEventCard(eventCard);
        List<GameEvent> events = EventResolver.resolve(eventCard, s);
        assertFalse(events.isEmpty(),
                "EventResolver must return at least one event for card id=" + cardId);
    }

    // =========================================================================
    // Per-effect behaviour tests
    // =========================================================================

    // --- ALL_DRAW_TWO (id=0) ---

    @Test
    void allDrawTwo_eachPlayerReceivesTwoCards() {
        Card ec = Card.eventCard(0);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // All 3 players should now have 7 + 2 = 9 cards
        for (PlayerGameState p : state.getPlayerOrder()) {
            assertEquals(9, p.getHandSize(),
                    p.getPlayerName() + " should have 9 cards after ALL_DRAW_TWO");
        }
    }

    @Test
    void allDrawTwo_eventListContainsTriggerEvent() {
        Card ec = Card.eventCard(0);
        state.setActiveEventCard(ec);
        List<GameEvent> events = EventResolver.resolve(ec, state);
        assertTrue(events.stream()
                        .anyMatch(e -> e.type() == GameEvent.EventType.EFFECT_TRIGGERED
                                && e.detail().equals("ALL_DRAW_TWO")),
                "Events must include EFFECT_TRIGGERED/ALL_DRAW_TWO");
    }

    // --- ALL_DRAW_ONE (id=1) ---

    @Test
    void allDrawOne_eachPlayerReceivesOneCard() {
        Card ec = Card.eventCard(1);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        for (PlayerGameState p : state.getPlayerOrder()) {
            assertEquals(8, p.getHandSize(),
                    p.getPlayerName() + " should have 8 cards after ALL_DRAW_ONE");
        }
    }

    // --- ALL_SKIP (id=2) ---

    @Test
    void allSkip_allOtherPlayersMarkedSkipped() {
        Card ec = Card.eventCard(2);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // Current player (Alice, index 0) must NOT be skipped
        assertFalse(state.getPlayerOrder().get(0).isSkipped(), "Alice must not be skipped");
        // Bob and Charlie must be skipped
        assertTrue(state.getPlayerOrder().get(1).isSkipped(), "Bob must be skipped");
        assertTrue(state.getPlayerOrder().get(2).isSkipped(), "Charlie must be skipped");
    }

    // --- INSTANT_ROUND_END (id=3) ---

    @Test
    void instantRoundEnd_setsPhaseToRoundEnd() {
        Card ec = Card.eventCard(3);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertEquals(GamePhase.ROUND_END, state.getPhase());
    }

    @Test
    void instantRoundEnd_returnsRoundEndedEvent() {
        Card ec = Card.eventCard(3);
        state.setActiveEventCard(ec);
        List<GameEvent> events = EventResolver.resolve(ec, state);
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.ROUND_ENDED),
                "INSTANT_ROUND_END must emit a ROUND_ENDED event");
    }

    // --- REVERSE_ORDER (id=4) ---

    @Test
    void reverseOrder_reversesPlayerList() {
        // Before: [Alice, Bob, Charlie]
        Card ec = Card.eventCard(4);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // After reverse: [Charlie, Bob, Alice]
        List<PlayerGameState> order = state.getPlayerOrder();
        assertEquals("Charlie", order.get(0).getPlayerName());
        assertEquals("Bob",     order.get(1).getPlayerName());
        assertEquals("Alice",   order.get(2).getPlayerName());
    }

    @Test
    void reverseOrder_currentPlayerRemainsTheSame() {
        // Alice is current (index 0) before reverse
        Card ec = Card.eventCard(4);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // After reverse Alice should still be current (now at index 2)
        assertEquals("Alice", state.getCurrentPlayer().getPlayerName());
    }

    // --- STEAL_FROM_NEXT (id=5) ---

    @Test
    void stealFromNext_currentPlayerGainsCard_nextPlayerLosesCard() {
        Card ec = Card.eventCard(5);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // Alice (current) should have 8, Bob (next) should have 6
        assertEquals(8, state.getPlayer("Alice").getHandSize(), "Alice should gain 1 card");
        assertEquals(6, state.getPlayer("Bob").getHandSize(),   "Bob should lose 1 card");
    }

    // --- STEAL_FROM_PREV (id=6) ---

    @Test
    void stealFromPrev_currentPlayerGainsCard_prevPlayerLosesCard() {
        Card ec = Card.eventCard(6);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // Alice (current, index 0) steals from Charlie (prev, index 2)
        assertEquals(8, state.getPlayer("Alice").getHandSize(),   "Alice should gain 1 card");
        assertEquals(6, state.getPlayer("Charlie").getHandSize(), "Charlie should lose 1 card");
    }

    // --- DISCARD_HIGHEST (id=7) ---

    @Test
    void discardHighest_eachPlayerLosesOneCard() {
        Card ec = Card.eventCard(7);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        for (PlayerGameState p : state.getPlayerOrder()) {
            // Each player had 7; DISCARD_HIGHEST removes exactly 1 (unless hand empty)
            assertTrue(p.getHandSize() <= 6,
                    p.getPlayerName() + " should have at most 6 cards after DISCARD_HIGHEST");
        }
    }

    @Test
    void discardHighest_discardedCardIsOnDiscardPile() {
        // The discard pile grows by the number of players (3) after the effect
        int discardBefore = state.getDiscardPile().size();
        Card ec = Card.eventCard(7);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        // 3 cards added to discard pile
        assertEquals(discardBefore + 3, state.getDiscardPile().size());
    }

    // --- DISCARD_COLOR (id=8) ---

    @Test
    void discardColor_playerHandsDoNotContainMatchingColor() {
        Card ec = Card.eventCard(8);
        Card top = state.peekDiscardPile();
        CardColor topColor = (top != null) ? top.color() : null;
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        if (topColor != null) {
            for (PlayerGameState p : state.getPlayerOrder()) {
                CardColor finalTopColor = topColor;
                assertTrue(p.getHand().stream().noneMatch(c -> c.color() == finalTopColor),
                        p.getPlayerName() + " must have no cards of color " + topColor);
            }
        }
    }

    // --- SWAP_HANDS (id=9) ---

    @Test
    void swapHands_currentAndNextExchangeHands() {
        List<Card> aliceHandBefore = List.copyOf(state.getPlayer("Alice").getHand());
        List<Card> bobHandBefore   = List.copyOf(state.getPlayer("Bob").getHand());

        Card ec = Card.eventCard(9);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        // Alice now has Bob's old hand and vice-versa
        assertEquals(bobHandBefore,   state.getPlayer("Alice").getHand());
        assertEquals(aliceHandBefore, state.getPlayer("Bob").getHand());
    }

    @Test
    void swapHands_charlieHandUnchanged() {
        List<Card> charlieHandBefore = List.copyOf(state.getPlayer("Charlie").getHand());
        Card ec = Card.eventCard(9);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertEquals(charlieHandBefore, state.getPlayer("Charlie").getHand());
    }

    // --- BLOCK_SPECIALS (id=10) ---

    @Test
    void blockSpecials_specialsBlockedIsTrue() {
        Card ec = Card.eventCard(10);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertTrue(state.isSpecialsBlocked());
    }

    @Test
    void blockSpecials_specialCardsCannotBePlayedWhileBlocked() {
        Card ec = Card.eventCard(10);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        // A SPECIAL_FOUR card should not be playable while blocked
        Card specialFour = Card.specialFourCard(200, SpecialEffect.FANTASTIC);
        assertFalse(CardValidator.canPlay(specialFour, state.peekDiscardPile(), state),
                "SPECIAL_FOUR must not be playable while specials are blocked");
    }

    @Test
    void blockSpecials_clearedWhenBlackCardPlayed() {
        state.setSpecialsBlocked(true);
        // Simulate playing a black card (TurnEngine.playCard clears the flag)
        TurnEngine.startTurn(state);
        String currentName = state.getCurrentPlayer().getPlayerName();
        // Place a black card in the current player's hand that can be played
        Card blackCard = Card.blackCard(500, 5);
        state.getDiscardPile().clear();
        state.pushToDiscardPile(Card.colorCard(0, CardColor.RED, 5)); // value 5 on top
        state.getCurrentPlayer().getHand().add(0, blackCard);
        TurnEngine.playCard(state, currentName, blackCard);
        assertFalse(state.isSpecialsBlocked(),
                "specialsBlocked must be cleared when a BLACK card is played");
    }

    // --- GIFT_CHAIN (id=11) ---

    @Test
    void giftChain_totalCardCountUnchanged() {
        int totalBefore = state.getPlayerOrder().stream()
                .mapToInt(PlayerGameState::getHandSize).sum();
        Card ec = Card.eventCard(11);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        int totalAfter = state.getPlayerOrder().stream()
                .mapToInt(PlayerGameState::getHandSize).sum();
        assertEquals(totalBefore, totalAfter,
                "GIFT_CHAIN must not change the total number of cards across all hands");
    }

    @Test
    void giftChain_eachPlayerHandSizeUnchanged() {
        // With 3 players each at 7 cards: each gives 1 and receives 1 → still 7
        Card ec = Card.eventCard(11);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        for (PlayerGameState p : state.getPlayerOrder()) {
            assertEquals(7, p.getHandSize(),
                    p.getPlayerName() + " should still have 7 cards after GIFT_CHAIN");
        }
    }

    // --- HAND_RESET (id=12) ---

    @Test
    void handReset_allPlayersHaveSevenCards() {
        Card ec = Card.eventCard(12);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        for (PlayerGameState p : state.getPlayerOrder()) {
            assertEquals(7, p.getHandSize(),
                    p.getPlayerName() + " should have 7 cards after HAND_RESET");
        }
    }

    // --- LUCKY_DRAW (id=13) ---

    @Test
    void luckyDraw_currentPlayerDrawsThreeCards() {
        Card ec = Card.eventCard(13);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertEquals(10, state.getCurrentPlayer().getHandSize(),
                "Current player should have 7 + 3 = 10 cards after LUCKY_DRAW");
    }

    @Test
    void luckyDraw_otherPlayersUnchanged() {
        Card ec = Card.eventCard(13);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        String currentName = state.getCurrentPlayer().getPlayerName();
        for (PlayerGameState p : state.getPlayerOrder()) {
            if (!p.getPlayerName().equals(currentName)) {
                assertEquals(7, p.getHandSize(),
                        p.getPlayerName() + " should be unaffected by LUCKY_DRAW");
            }
        }
    }

    // --- PENALTY_DRAW (id=14) ---

    @Test
    void penaltyDraw_playerWithMostCardsDrawsTwo() {
        // Give Bob an extra card so he has 8 (most)
        Card extra = Card.colorCard(999, CardColor.RED, 1);
        state.getPlayer("Bob").addCard(extra);

        Card ec = Card.eventCard(14);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        assertEquals(10, state.getPlayer("Bob").getHandSize(),
                "Bob (most cards) should have 8 + 2 = 10 after PENALTY_DRAW");
        assertEquals(7, state.getPlayer("Alice").getHandSize(), "Alice unaffected");
        assertEquals(7, state.getPlayer("Charlie").getHandSize(), "Charlie unaffected");
    }

    // --- EQUALIZE (id=15) ---

    @Test
    void equalize_smallerHandDrawsToMatchLarger() {
        // Give Alice 2 extra cards so she has 9
        state.getPlayer("Alice").addCard(Card.colorCard(998, CardColor.GREEN, 2));
        state.getPlayer("Alice").addCard(Card.colorCard(997, CardColor.GREEN, 3));

        Card ec = Card.eventCard(15);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        // Bob and Charlie should now have 9 cards (drawn up to match Alice)
        assertEquals(9, state.getPlayer("Bob").getHandSize(),
                "Bob should have drawn to match Alice's 9");
        assertEquals(9, state.getPlayer("Charlie").getHandSize(),
                "Charlie should have drawn to match Alice's 9");
    }

    @Test
    void equalize_alreadyEqualHandsNoChange() {
        // All 3 players already at 7 — no drawing needed
        Card ec = Card.eventCard(15);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        for (PlayerGameState p : state.getPlayerOrder()) {
            assertEquals(7, p.getHandSize(), p.getPlayerName() + " should stay at 7");
        }
    }

    // --- WILD_REQUEST (id=16) ---

    @Test
    void wildRequest_setsRequestedColor() {
        Card ec = Card.eventCard(16);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertNotNull(state.getRequestedColor(),
                "WILD_REQUEST must set a non-null requestedColor");
    }

    @Test
    void wildRequest_colorMatchesDiscardTop() {
        // Force a known top color
        state.getDiscardPile().clear();
        state.pushToDiscardPile(Card.colorCard(0, CardColor.GREEN, 5));

        Card ec = Card.eventCard(16);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        assertEquals(CardColor.GREEN, state.getRequestedColor());
    }

    // --- CANCEL_EFFECTS (id=17) ---

    @Test
    void cancelEffects_clearsPendingEffects() {
        state.getPendingEffects().push(SpecialEffect.SKIP);
        state.setPendingEffectTarget("Bob");

        Card ec = Card.eventCard(17);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        assertTrue(state.getPendingEffects().isEmpty(),
                "pendingEffects must be empty after CANCEL_EFFECTS");
        assertNull(state.getPendingEffectTarget(),
                "pendingEffectTarget must be null after CANCEL_EFFECTS");
    }

    // --- BONUS_PLAY (id=18) ---

    @Test
    void bonusPlay_setsPhaseToAwaitingPlay() {
        Card ec = Card.eventCard(18);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertEquals(GamePhase.AWAITING_PLAY, state.getPhase(),
                "BONUS_PLAY must set phase to AWAITING_PLAY");
    }

    // --- DOUBLE_SCORING (id=19) ---

    @Test
    void doubleScoring_flagIsSet() {
        Card ec = Card.eventCard(19);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);
        assertTrue(state.isDoubleScoringActive(),
                "doubleScoringActive must be true after DOUBLE_SCORING");
    }

    @Test
    void doubleScoring_scoresAreDoubledAtRoundEnd() {
        Card ec = Card.eventCard(19);
        state.setActiveEventCard(ec);
        EventResolver.resolve(ec, state);

        // Set up a known hand: Alice holds RED/3 (scoring = 3) → expect 6 with doubling
        PlayerGameState alice = state.getPlayer("Alice");
        alice.getHand().clear();
        alice.addCard(Card.colorCard(1, CardColor.RED, 3));

        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(state.getPlayerOrder(), state);

        assertEquals(6, roundScores.get("Alice"),
                "With DOUBLE_SCORING active, RED/3 (score 3) should count as 6");
        assertFalse(state.isDoubleScoringActive(),
                "doubleScoringActive must be cleared after calculateRoundScores");
    }
}
