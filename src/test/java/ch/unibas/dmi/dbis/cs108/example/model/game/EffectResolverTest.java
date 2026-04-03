package ch.unibas.dmi.dbis.cs108.example.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EffectResolver}.
 *
 * Each test builds a real {@link GameState} via {@link GameInitializer} and
 * then directly manipulates only the fields under test (hand contents,
 * pendingEffects, phase). Tests avoid mocking.
 *
 * <h2>Setup</h2>
 * Three players: Alice (current), Bob, Charlie — alphabetical order for round 1.
 * Each starts with 7 cards dealt by GameInitializer.
 * Phase is set to RESOLVING_EFFECT before each effect-under-test is pushed.
 */
class EffectResolverTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = GameInitializer.initialize(
                List.of("Alice", "Bob", "Charlie"), 1, null, new Random(42));
        state.setPhase(GamePhase.RESOLVING_EFFECT);
    }

    // =========================================================================
    // SKIP
    // =========================================================================

    @Test
    void skip_marksTargetAsSkipped() {
        // Target Charlie (not the immediate-next player Bob) so that endTurn
        // advances to Bob without consuming Charlie's skip flag.
        state.getPendingEffects().push(SpecialEffect.SKIP);

        EffectResolver.resolve(SpecialEffect.SKIP, state, "Alice",
                EffectArgs.withTarget("Charlie"));

        // Charlie's flag is set; it will be consumed when her turn comes around
        assertTrue(state.getPlayer("Charlie").isSkipped());
        assertFalse(state.getPlayer("Alice").isSkipped());
        assertFalse(state.getPlayer("Bob").isSkipped());
    }

    @Test
    void skip_immediateNextPlayer_skipConsumedByEndTurn() {
        // When SKIP targets the immediate next player (Bob), endTurn advances
        // past Bob and lands on Charlie — the skip is consumed in the same call.
        state.getPendingEffects().push(SpecialEffect.SKIP);

        EffectResolver.resolve(SpecialEffect.SKIP, state, "Alice",
                EffectArgs.withTarget("Bob"));

        // Skip was consumed: endTurn skipped Bob and landed on Charlie
        assertFalse(state.getPlayer("Bob").isSkipped());
        assertEquals("Charlie", state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void skip_advancesToNextPlayer() {
        state.getPendingEffects().push(SpecialEffect.SKIP);

        EffectResolver.resolve(SpecialEffect.SKIP, state, "Alice",
                EffectArgs.withTarget("Charlie"));

        // endTurn was called → no longer Alice's turn
        assertNotEquals("Alice", state.getCurrentPlayer().getPlayerName());
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    // =========================================================================
    // GIFT
    // =========================================================================

    @Test
    void gift_transfersTwoCards_handSizesChangeCorrectly() {
        PlayerGameState alice = state.getPlayer("Alice");
        PlayerGameState bob   = state.getPlayer("Bob");

        // Give Alice a clean hand with 3 known cards
        alice.getHand().clear();
        Card c1 = Card.colorCard(200, CardColor.RED, 1);
        Card c2 = Card.colorCard(201, CardColor.RED, 2);
        Card c3 = Card.colorCard(202, CardColor.RED, 3);
        alice.addCard(c1);
        alice.addCard(c2);
        alice.addCard(c3);

        int bobBefore = bob.getHandSize();

        state.getPendingEffects().push(SpecialEffect.GIFT);
        EffectResolver.resolve(SpecialEffect.GIFT, state, "Alice",
                EffectArgs.withTargetAndCards("Bob", List.of(c1, c2)));

        assertEquals(1, alice.getHandSize());
        assertEquals(bobBefore + 2, bob.getHandSize());
    }

    @Test
    void gift_withOneCardInHand_transfersJustOneCard() {
        PlayerGameState alice = state.getPlayer("Alice");
        PlayerGameState bob   = state.getPlayer("Bob");

        alice.getHand().clear();
        Card only = Card.colorCard(200, CardColor.GREEN, 5);
        alice.addCard(only);

        int bobBefore = bob.getHandSize();

        state.getPendingEffects().push(SpecialEffect.GIFT);
        EffectResolver.resolve(SpecialEffect.GIFT, state, "Alice",
                EffectArgs.withTargetAndCards("Bob", List.of(only)));

        assertEquals(0, alice.getHandSize());
        assertEquals(bobBefore + 1, bob.getHandSize());
    }

    // =========================================================================
    // EXCHANGE
    // =========================================================================

    @Test
    void exchange_swapsExactlyTwoCards() {
        PlayerGameState alice   = state.getPlayer("Alice");
        PlayerGameState charlie = state.getPlayer("Charlie");

        // Give Alice known cards
        alice.getHand().clear();
        Card a1 = Card.colorCard(200, CardColor.RED, 1);
        Card a2 = Card.colorCard(201, CardColor.RED, 2);
        Card a3 = Card.colorCard(202, CardColor.RED, 3);
        alice.addCard(a1);
        alice.addCard(a2);
        alice.addCard(a3);

        // Give Charlie known cards
        charlie.getHand().clear();
        Card c1 = Card.colorCard(203, CardColor.BLUE, 1);
        Card c2 = Card.colorCard(204, CardColor.BLUE, 2);
        Card c3 = Card.colorCard(205, CardColor.BLUE, 3);
        charlie.addCard(c1);
        charlie.addCard(c2);
        charlie.addCard(c3);

        state.getPendingEffects().push(SpecialEffect.EXCHANGE);
        EffectResolver.resolve(SpecialEffect.EXCHANGE, state, "Alice",
                EffectArgs.withTargetAndCards("Charlie", List.of(a1, a2)));

        // Alice gave a1,a2 and received c1,c2 (first 2 from Charlie)
        assertFalse(alice.getHand().contains(a1));
        assertFalse(alice.getHand().contains(a2));
        assertTrue(alice.getHand().contains(a3));
        assertTrue(alice.getHand().contains(c1));
        assertTrue(alice.getHand().contains(c2));

        // Charlie gave c1,c2 and received a1,a2
        assertTrue(charlie.getHand().contains(a1));
        assertTrue(charlie.getHand().contains(a2));
        assertFalse(charlie.getHand().contains(c1));
        assertFalse(charlie.getHand().contains(c2));
        assertTrue(charlie.getHand().contains(c3));
    }

    // =========================================================================
    // FANTASTIC
    // =========================================================================

    @Test
    void fantastic_setsRequestedColor() {
        state.getPendingEffects().push(SpecialEffect.FANTASTIC);

        EffectResolver.resolve(SpecialEffect.FANTASTIC, state, "Alice",
                EffectArgs.withColor(CardColor.BLUE));

        assertEquals(CardColor.BLUE, state.getRequestedColor());
        assertNull(state.getRequestedNumber());
    }

    @Test
    void fantastic_setsColorAndNumber() {
        state.getPendingEffects().push(SpecialEffect.FANTASTIC);

        EffectResolver.resolve(SpecialEffect.FANTASTIC, state, "Alice",
                EffectArgs.withColorAndNumber(CardColor.GREEN, 7));

        assertEquals(CardColor.GREEN, state.getRequestedColor());
        assertEquals(7, state.getRequestedNumber());
    }

    @Test
    void fantastic_nullClearsBothRequests() {
        state.setRequestedColor(CardColor.RED);
        state.setRequestedNumber(5);
        state.getPendingEffects().push(SpecialEffect.FANTASTIC);

        EffectResolver.resolve(SpecialEffect.FANTASTIC, state, "Alice",
                EffectArgs.withColorAndNumber(null, null));

        assertNull(state.getRequestedColor());
        assertNull(state.getRequestedNumber());
    }

    // =========================================================================
    // FANTASTIC_FOUR
    // =========================================================================

    @Test
    void fantasticFour_distributesFourCards_andSetsRequest() {
        int aliceBefore   = state.getPlayer("Alice").getHandSize();
        int bobBefore     = state.getPlayer("Bob").getHandSize();
        int charlieBefore = state.getPlayer("Charlie").getHandSize();
        int drawBefore    = state.getDrawPile().size();

        state.getPendingEffects().push(SpecialEffect.FANTASTIC_FOUR);
        EffectResolver.resolve(SpecialEffect.FANTASTIC_FOUR, state, "Alice",
                EffectArgs.withColorAndNumber(CardColor.YELLOW, 3));

        // Total hand cards should have increased by 4
        int totalAfter = state.getPlayer("Alice").getHandSize()
                + state.getPlayer("Bob").getHandSize()
                + state.getPlayer("Charlie").getHandSize();
        assertEquals(aliceBefore + bobBefore + charlieBefore + 4, totalAfter);

        // Draw pile should have shrunk by 4
        assertEquals(drawBefore - 4, state.getDrawPile().size());

        // Request set
        assertEquals(CardColor.YELLOW, state.getRequestedColor());
        assertEquals(3, state.getRequestedNumber());
    }

    @Test
    void fantasticFour_doesNotGiveCardToActingPlayer_first() {
        // Cards are distributed starting with the player AFTER the actor.
        // Actor is Alice (index 0); next is Bob (index 1).
        int aliceBefore = state.getPlayer("Alice").getHandSize();

        state.getPendingEffects().push(SpecialEffect.FANTASTIC_FOUR);
        EffectResolver.resolve(SpecialEffect.FANTASTIC_FOUR, state, "Alice",
                EffectArgs.withColor(CardColor.RED));

        // Alice gets the 3rd card (i=3 → index 0) and the loop ends at i=4 (index 1=Bob).
        // With 3 players: indices 1,2,0,1 → Bob(+2), Charlie(+1), Alice(+1)
        // Alice should have received at most 1 card (not the first card given out)
        int aliceAfter = state.getPlayer("Alice").getHandSize();
        assertTrue(aliceAfter <= aliceBefore + 1,
                "Actor should receive at most 1 of the 4 distributed cards");
    }

    // =========================================================================
    // EQUALITY
    // =========================================================================

    @Test
    void equality_targetDrawsUntilHandSizeEqualsActor() {
        PlayerGameState alice = state.getPlayer("Alice");
        PlayerGameState bob   = state.getPlayer("Bob");

        // Give Alice 5 cards, Bob 2 cards
        alice.getHand().clear();
        for (int i = 0; i < 5; i++) alice.addCard(Card.colorCard(200 + i, CardColor.RED, i + 1));
        bob.getHand().clear();
        for (int i = 0; i < 2; i++) bob.addCard(Card.colorCard(210 + i, CardColor.BLUE, i + 1));

        state.getPendingEffects().push(SpecialEffect.EQUALITY);
        EffectResolver.resolve(SpecialEffect.EQUALITY, state, "Alice",
                EffectArgs.withTargetAndColor("Bob", CardColor.GREEN));

        assertEquals(alice.getHandSize(), bob.getHandSize());
        assertEquals(CardColor.GREEN, state.getRequestedColor());
    }

    @Test
    void equality_doesNotReduceTargetHandIfAlreadyEqual() {
        PlayerGameState alice = state.getPlayer("Alice");
        PlayerGameState bob   = state.getPlayer("Bob");

        alice.getHand().clear();
        bob.getHand().clear();
        // Both have 3 cards
        for (int i = 0; i < 3; i++) {
            alice.addCard(Card.colorCard(200 + i, CardColor.RED, i + 1));
            bob.addCard(Card.colorCard(210 + i, CardColor.BLUE, i + 1));
        }

        state.getPendingEffects().push(SpecialEffect.EQUALITY);
        EffectResolver.resolve(SpecialEffect.EQUALITY, state, "Alice",
                EffectArgs.withTargetAndColor("Bob", CardColor.YELLOW));

        // Bob's size unchanged (already equal)
        assertEquals(3, bob.getHandSize());
    }

    // =========================================================================
    // COUNTERATTACK
    // =========================================================================

    @Test
    void counterattack_redirectsPendingEffectToNewTarget() {
        // Original state: SKIP pending, targeting "Bob"
        state.getPendingEffects().push(SpecialEffect.SKIP);
        state.setPendingEffectTarget("Bob");

        // Bob plays COUNTERATTACK → push it on top
        state.getPendingEffects().push(SpecialEffect.COUNTERATTACK);

        EffectResolver.resolve(SpecialEffect.COUNTERATTACK, state, "Bob",
                EffectArgs.withTarget("Charlie"));

        // COUNTERATTACK popped; SKIP still in stack; target changed to Charlie
        assertEquals(1, state.getPendingEffects().size());
        assertEquals(SpecialEffect.SKIP, state.getPendingEffects().peek());
        assertEquals("Charlie", state.getPendingEffectTarget());

        // Phase still RESOLVING_EFFECT (SKIP must still be resolved)
        assertEquals(GamePhase.RESOLVING_EFFECT, state.getPhase());
    }

    @Test
    void counterattack_chain_twoCounterattacksCancelAndReapplyToOriginalTarget() {
        // Original: SKIP targeting "Bob"
        state.getPendingEffects().push(SpecialEffect.SKIP);
        state.setPendingEffectTarget("Bob");

        // Bob counters → target becomes Charlie
        state.getPendingEffects().push(SpecialEffect.COUNTERATTACK);
        EffectResolver.resolve(SpecialEffect.COUNTERATTACK, state, "Bob",
                EffectArgs.withTarget("Charlie"));

        assertEquals("Charlie", state.getPendingEffectTarget());

        // Charlie counters → target becomes Bob again
        state.getPendingEffects().push(SpecialEffect.COUNTERATTACK);
        EffectResolver.resolve(SpecialEffect.COUNTERATTACK, state, "Charlie",
                EffectArgs.withTarget("Bob"));

        // Back to Bob; SKIP still in stack
        assertEquals("Bob", state.getPendingEffectTarget());
        assertEquals(1, state.getPendingEffects().size());
        assertEquals(SpecialEffect.SKIP, state.getPendingEffects().peek());
    }

    // =========================================================================
    // NICE_TRY
    // =========================================================================

    @Test
    void niceTry_targetDrawsThreeCards_roundContinues() {
        // Simulate Alice having just emptied her hand → ROUND_END
        PlayerGameState alice = state.getPlayer("Alice");
        alice.getHand().clear();
        state.setPhase(GamePhase.ROUND_END);

        state.getPendingEffects().push(SpecialEffect.NICE_TRY);
        EffectResolver.resolve(SpecialEffect.NICE_TRY, state, "Bob",
                EffectArgs.withTarget("Alice"));

        assertEquals(3, alice.getHandSize());
        // endTurn was called → phase is TURN_START (not ROUND_END)
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    @Test
    void niceTry_doesNotDrawMoreThanAvailableInDrawPile() {
        PlayerGameState alice = state.getPlayer("Alice");
        alice.getHand().clear();

        // Drain the draw pile to just 1 card
        while (state.getDrawPile().size() > 1) {
            state.drawFromDrawPile();
        }

        state.getPendingEffects().push(SpecialEffect.NICE_TRY);
        EffectResolver.resolve(SpecialEffect.NICE_TRY, state, "Bob",
                EffectArgs.withTarget("Alice"));

        // Only 1 card could be drawn, not 3
        assertEquals(1, alice.getHandSize());
    }

    // =========================================================================
    // SECOND_CHANCE
    // =========================================================================

    @Test
    void secondChance_playsSelectedCard_andEndsTurn() {
        PlayerGameState alice = state.getPlayer("Alice");
        alice.getHand().clear();
        Card toPlay = Card.colorCard(200, CardColor.RED, 5);
        Card keeper = Card.colorCard(201, CardColor.RED, 6);
        alice.addCard(toPlay);
        alice.addCard(keeper);

        // Make toPlay valid on the discard pile
        state.getDiscardPile().clear();
        state.pushToDiscardPile(Card.colorCard(0, CardColor.RED, 3));

        state.getPendingEffects().push(SpecialEffect.SECOND_CHANCE);
        EffectResolver.resolve(SpecialEffect.SECOND_CHANCE, state, "Alice",
                EffectArgs.withCards(List.of(toPlay)));

        // Card played: removed from Alice's hand and on the discard pile
        assertFalse(alice.getHand().contains(toPlay));
        assertEquals(toPlay, state.peekDiscardPile());
        assertEquals(1, alice.getHandSize());

        // Turn advanced
        assertNotEquals("Alice", state.getCurrentPlayer().getPlayerName());
        assertEquals(GamePhase.TURN_START, state.getPhase());
    }

    @Test
    void secondChance_drawsOneCard_whenNoCardSelected() {
        PlayerGameState alice = state.getPlayer("Alice");
        int handBefore = alice.getHandSize();
        int drawBefore = state.getDrawPile().size();

        state.getPendingEffects().push(SpecialEffect.SECOND_CHANCE);
        EffectResolver.resolve(SpecialEffect.SECOND_CHANCE, state, "Alice",
                EffectArgs.withCards(List.of())); // empty = impossible to play

        assertEquals(handBefore + 1, alice.getHandSize());
        assertEquals(drawBefore - 1, state.getDrawPile().size());
        assertNotEquals("Alice", state.getCurrentPlayer().getPlayerName());
    }

    @Test
    void secondChance_playsLastCard_triggersRoundEnd() {
        PlayerGameState alice = state.getPlayer("Alice");
        alice.getHand().clear();
        Card lastCard = Card.colorCard(200, CardColor.RED, 5);
        alice.addCard(lastCard);

        state.getDiscardPile().clear();
        state.pushToDiscardPile(Card.colorCard(0, CardColor.RED, 3));

        state.getPendingEffects().push(SpecialEffect.SECOND_CHANCE);
        List<GameEvent> events = EffectResolver.resolve(SpecialEffect.SECOND_CHANCE, state, "Alice",
                EffectArgs.withCards(List.of(lastCard)));

        assertEquals(0, alice.getHandSize());
        assertEquals(GamePhase.ROUND_END, state.getPhase());
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEvent.EventType.ROUND_ENDED));
    }

    // =========================================================================
    // Pending effects stack is popped correctly
    // =========================================================================

    @Test
    void resolve_popsEffectFromPendingStack() {
        state.getPendingEffects().push(SpecialEffect.SKIP);
        assertEquals(1, state.getPendingEffects().size());

        EffectResolver.resolve(SpecialEffect.SKIP, state, "Alice",
                EffectArgs.withTarget("Bob"));

        assertTrue(state.getPendingEffects().isEmpty());
    }
}
