package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.GamePhase;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.PlayerGameState;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GameStateSerializer}.
 */
class GameStateSerializerTest {

    /**
     * Verifies that the public-state payload contains the required fields
     * and correct values for a state with a discard top card.
     */
    @Test
    void serializePublicStateProducesExpectedPayload() {
        PlayerGameState alice = new PlayerGameState("Alice", 5);
        PlayerGameState bob = new PlayerGameState("Bob", 12);

        alice.addCard(Card.colorCard(1, CardColor.RED, 3));
        alice.addCard(Card.blackCard(2, 4));
        bob.addCard(Card.colorCard(3, CardColor.BLUE, 7));

        ArrayDeque<Card> drawPile = new ArrayDeque<>();
        drawPile.push(Card.colorCard(10, CardColor.GREEN, 8));
        drawPile.push(Card.colorCard(11, CardColor.YELLOW, 2));

        ArrayDeque<Card> discardPile = new ArrayDeque<>();
        discardPile.push(Card.colorCard(42, CardColor.RED, 9));

        ArrayDeque<Card> eventPile = new ArrayDeque<>();

        GameState state = new GameState(
                List.of(alice, bob),
                drawPile,
                discardPile,
                eventPile,
                100
        );
        state.setPhase(GamePhase.AWAITING_PLAY);

        String payload = GameStateSerializer.serializePublicState(state);

        assertEquals(
                "phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:42,drawPileSize:2,players:Alice:2:5,Bob:1:12",
                payload
        );
    }

    /**
     * Verifies that the discard-top field becomes "none" when the discard pile is empty.
     */
    @Test
    void serializePublicStateUsesNoneWhenDiscardPileIsEmpty() {
        PlayerGameState alice = new PlayerGameState("Alice");
        PlayerGameState bob = new PlayerGameState("Bob");

        ArrayDeque<Card> drawPile = new ArrayDeque<>();
        drawPile.push(Card.colorCard(5, CardColor.GREEN, 1));

        ArrayDeque<Card> discardPile = new ArrayDeque<>();
        ArrayDeque<Card> eventPile = new ArrayDeque<>();

        GameState state = new GameState(
                List.of(alice, bob),
                drawPile,
                discardPile,
                eventPile,
                100
        );
        state.setPhase(GamePhase.TURN_START);

        String payload = GameStateSerializer.serializePublicState(state);

        assertEquals(
                "phase:TURN_START,currentPlayer:Alice,discardTop:none,drawPileSize:1,players:Alice:0:0,Bob:0:0",
                payload
        );
    }

    /**
     * Verifies that a player's hand is serialized as comma-separated card ids
     * in hand order.
     */
    @Test
    void serializeHandReturnsCommaSeparatedCardIds() {
        PlayerGameState player = new PlayerGameState("Alice");
        player.addCard(Card.colorCard(7, CardColor.RED, 3));
        player.addCard(Card.blackCard(18, 5));
        player.addCard(Card.colorCard(42, CardColor.BLUE, 9));

        String payload = GameStateSerializer.serializeHand(player);

        assertEquals("7,18,42", payload);
    }

    /**
     * Verifies that an empty hand becomes an empty string.
     */
    @Test
    void serializeHandReturnsEmptyStringForEmptyHand() {
        PlayerGameState player = new PlayerGameState("Alice");

        String payload = GameStateSerializer.serializeHand(player);

        assertEquals("", payload);
    }

    /**
     * Verifies that round-end serialization includes round score and total score
     * for each player in player-list order.
     */
    @Test
    void serializeRoundEndReturnsExpectedFormat() {
        PlayerGameState alice = new PlayerGameState("Alice", 20);
        PlayerGameState bob = new PlayerGameState("Bob", 30);

        Map<String, Integer> roundScores = Map.of(
                "Alice", 5,
                "Bob", 12
        );

        String payload = GameStateSerializer.serializeRoundEnd(
                roundScores,
                List.of(alice, bob)
        );

        assertEquals("Alice:5:20,Bob:12:30", payload);
    }

    /**
     * Verifies that missing round scores default to zero.
     */
    @Test
    void serializeRoundEndUsesZeroForMissingRoundScore() {
        PlayerGameState alice = new PlayerGameState("Alice", 20);
        PlayerGameState bob = new PlayerGameState("Bob", 30);

        Map<String, Integer> roundScores = Map.of(
                "Alice", 5
        );

        String payload = GameStateSerializer.serializeRoundEnd(
                roundScores,
                List.of(alice, bob)
        );

        assertEquals("Alice:5:20,Bob:0:30", payload);
    }

    /**
     * Verifies that the effect request format is "EFFECT:player".
     */
    @Test
    void serializeEffectRequestReturnsExpectedFormat() {
        String payload = GameStateSerializer.serializeEffectRequest("SKIP", "Alice");

        assertEquals("SKIP:Alice", payload);
    }
}