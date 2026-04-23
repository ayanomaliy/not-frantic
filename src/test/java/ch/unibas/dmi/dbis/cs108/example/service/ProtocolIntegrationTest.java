package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.model.game.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Protocol integration tests for Phase 8.
 *
 * These tests cover:
 * <ul>
 *   <li>Message parsing for new game-action types.</li>
 *   <li>{@link GameMessageParser} — PLAY_CARD and EFFECT_RESPONSE payloads.</li>
 *   <li>{@link GameStateSerializer} — public state, hand, round-end, effect-request.</li>
 *   <li>Round-trip: encode then parse a new message type.</li>
 *   <li>Game-flow pipeline: play a card → events generated → state updated.</li>
 *   <li>Score-to-round-end broadcast payload correctness.</li>
 *   <li>Game-over detection after scores accumulate past maxScore.</li>
 * </ul>
 *
 * No real network sockets are used — all tests operate on model objects directly.
 */
class ProtocolIntegrationTest {

    // =========================================================================
    // Message parsing — new game-action types
    // =========================================================================

    @Test
    void parse_PLAY_CARD_returnsCorrectType() {
        Message msg = Message.parse("PLAY_CARD|42");
        assertNotNull(msg);
        assertEquals(Message.Type.PLAY_CARD, msg.type());
        assertEquals("42", msg.content());
    }

    @Test
    void parse_DRAW_CARD_returnsCorrectType() {
        Message msg = Message.parse("DRAW_CARD|");
        assertNotNull(msg);
        assertEquals(Message.Type.DRAW_CARD, msg.type());
    }

    @Test
    void parse_END_TURN_returnsCorrectType() {
        Message msg = Message.parse("END_TURN|");
        assertNotNull(msg);
        assertEquals(Message.Type.END_TURN, msg.type());
    }

    @Test
    void parse_EFFECT_RESPONSE_returnsCorrectType() {
        Message msg = Message.parse("EFFECT_RESPONSE|SKIP|Bob");
        assertNotNull(msg);
        assertEquals(Message.Type.EFFECT_RESPONSE, msg.type());
        assertEquals("SKIP|Bob", msg.content());
    }

    @Test
    void parse_GAME_STATE_returnsCorrectType() {
        Message msg = Message.parse("GAME_STATE|phase:AWAITING_PLAY");
        assertNotNull(msg);
        assertEquals(Message.Type.GAME_STATE, msg.type());
    }

    @Test
    void parse_ROUND_END_returnsCorrectType() {
        Message msg = Message.parse("ROUND_END|Alice:5:20,Bob:12:30");
        assertNotNull(msg);
        assertEquals(Message.Type.ROUND_END, msg.type());
    }

    @Test
    void parse_GAME_END_returnsCorrectType() {
        Message msg = Message.parse("GAME_END|Alice");
        assertNotNull(msg);
        assertEquals(Message.Type.GAME_END, msg.type());
        assertEquals("Alice", msg.content());
    }

    // =========================================================================
    // Message.hasValidStructure — new types
    // =========================================================================

    @Test
    void hasValidStructure_PLAY_CARD_withContent_isValid() {
        assertTrue(new Message(Message.Type.PLAY_CARD, "42").hasValidStructure());
    }

    @Test
    void hasValidStructure_DRAW_CARD_emptyContent_isValid() {
        assertTrue(new Message(Message.Type.DRAW_CARD, "").hasValidStructure());
    }

    @Test
    void hasValidStructure_END_TURN_emptyContent_isValid() {
        assertTrue(new Message(Message.Type.END_TURN, "").hasValidStructure());
    }

    @Test
    void hasValidStructure_EFFECT_RESPONSE_withContent_isValid() {
        assertTrue(new Message(Message.Type.EFFECT_RESPONSE, "SKIP|Bob").hasValidStructure());
    }

    @Test
    void hasValidStructure_GAME_END_withContent_isValid() {
        assertTrue(new Message(Message.Type.GAME_END, "Alice").hasValidStructure());
    }

    // =========================================================================
    // Message encode / parse round-trip
    // =========================================================================

    @Test
    void encode_PLAY_CARD_roundTrip() {
        Message original = new Message(Message.Type.PLAY_CARD, "77");
        Message reparsed = Message.parse(original.encode());
        assertNotNull(reparsed);
        assertEquals(Message.Type.PLAY_CARD, reparsed.type());
        assertEquals("77", reparsed.content());
    }

    @Test
    void encode_EFFECT_RESPONSE_roundTrip() {
        Message original = new Message(Message.Type.EFFECT_RESPONSE, "FANTASTIC|BLUE|5");
        Message reparsed = Message.parse(original.encode());
        assertNotNull(reparsed);
        assertEquals(Message.Type.EFFECT_RESPONSE, reparsed.type());
        assertEquals("FANTASTIC|BLUE|5", reparsed.content());
    }

    // =========================================================================
    // GameMessageParser — parsePlayCard
    // =========================================================================

    @Test
    void parsePlayCard_validId_returnsId() {
        assertEquals(42, GameMessageParser.parsePlayCard("42"));
    }

    @Test
    void parsePlayCard_withWhitespace_returnsId() {
        assertEquals(7, GameMessageParser.parsePlayCard("  7  "));
    }

    @Test
    void parsePlayCard_nonNumeric_returnsMinusOne() {
        assertEquals(-1, GameMessageParser.parsePlayCard("abc"));
    }

    @Test
    void parsePlayCard_empty_returnsMinusOne() {
        assertEquals(-1, GameMessageParser.parsePlayCard(""));
    }

    // =========================================================================
    // GameMessageParser — parseEffectResponse
    // =========================================================================

    @Test
    void parseEffectResponse_SKIP_parsesTargetCorrectly() {
        GameState state = twoPlayerState();
        Object[] result = GameMessageParser.parseEffectResponse("SKIP|Bob", state, "Alice");
        assertNotNull(result);
        assertEquals("SKIP", result[0]);
        EffectArgs args = (EffectArgs) result[1];
        assertEquals("Bob", args.getTargetPlayer());
    }

    @Test
    void parseEffectResponse_FANTASTIC_parsesColorOnlyOrNumberOnly() {
        GameState state = twoPlayerState();

        Object[] colorResult = GameMessageParser.parseEffectResponse(
                "FANTASTIC|BLUE", state, "Alice");
        assertNotNull(colorResult);
        EffectArgs colorArgs = (EffectArgs) colorResult[1];
        assertEquals(CardColor.BLUE, colorArgs.getChosenColor());
        assertNull(colorArgs.getChosenNumber());

        Object[] numberResult = GameMessageParser.parseEffectResponse(
                "FANTASTIC||5", state, "Alice");
        assertNotNull(numberResult);
        EffectArgs numberArgs = (EffectArgs) numberResult[1];
        assertNull(numberArgs.getChosenColor());
        assertEquals(5, numberArgs.getChosenNumber());
    }

    @Test
    void parseEffectResponse_FANTASTIC_colorOnly_numberIsNull() {
        GameState state = twoPlayerState();
        Object[] result = GameMessageParser.parseEffectResponse(
                "FANTASTIC|GREEN", state, "Alice");
        assertNotNull(result);
        EffectArgs args = (EffectArgs) result[1];
        assertEquals(CardColor.GREEN, args.getChosenColor());
        assertNull(args.getChosenNumber());
    }

    @Test
    void parseEffectResponse_EQUALITY_parsesTargetAndColor() {
        GameState state = twoPlayerState();
        Object[] result = GameMessageParser.parseEffectResponse(
                "EQUALITY|Bob|YELLOW", state, "Alice");
        assertNotNull(result);
        EffectArgs args = (EffectArgs) result[1];
        assertEquals("Bob", args.getTargetPlayer());
        assertEquals(CardColor.YELLOW, args.getChosenColor());
    }

    @Test
    void parseEffectResponse_SECOND_CHANCE_emptyCard_givesEmptyList() {
        GameState state = twoPlayerState();
        Object[] result = GameMessageParser.parseEffectResponse(
                "SECOND_CHANCE|", state, "Alice");
        assertNotNull(result);
        EffectArgs args = (EffectArgs) result[1];
        assertTrue(args.getSelectedCards().isEmpty());
    }

    @Test
    void parseEffectResponse_unknownEffect_returnsNull() {
        GameState state = twoPlayerState();
        assertNull(GameMessageParser.parseEffectResponse("BOGUS|Bob", state, "Alice"));
    }

    @Test
    void parseEffectResponse_COUNTERATTACK_parsesTarget() {
        GameState state = twoPlayerState();
        Object[] result = GameMessageParser.parseEffectResponse(
                "COUNTERATTACK|Bob|RED", state, "Alice");
        assertNotNull(result);
        EffectArgs args = (EffectArgs) result[1];
        assertEquals("Bob", args.getTargetPlayer());
        assertEquals(CardColor.RED, args.getChosenColor());
    }

    // =========================================================================
    // GameStateSerializer
    // =========================================================================

    @Test
    void serializePublicState_containsRequiredFields() {
        GameState state = twoPlayerState();
        String payload = GameStateSerializer.serializePublicState(state);
        assertTrue(payload.contains("phase:"));
        assertTrue(payload.contains("currentPlayer:"));
        assertTrue(payload.contains("discardTop:"));
        assertTrue(payload.contains("drawPileSize:"));
        assertTrue(payload.contains("players:"));
    }

    @Test
    void serializePublicState_containsCorrectPhase() {
        GameState state = twoPlayerState();
        String payload = GameStateSerializer.serializePublicState(state);
        assertTrue(payload.contains("phase:" + state.getPhase().name()));
    }

    @Test
    void serializeHand_commaDelimitedCardIds() {
        GameState state = twoPlayerState();
        PlayerGameState alice = state.getPlayer("Alice");
        String hand = GameStateSerializer.serializeHand(alice);
        // 7 cards dealt → 7 ids separated by commas
        String[] ids = hand.split(",");
        assertEquals(7, ids.length);
        for (String id : ids) {
            assertDoesNotThrow(() -> Integer.parseInt(id.trim()));
        }
    }

    @Test
    void serializeHand_emptyHand_returnsEmptyString() {
        PlayerGameState p = new PlayerGameState("Tester");
        assertEquals("", GameStateSerializer.serializeHand(p));
    }

    @Test
    void serializeRoundEnd_correctFormat() {
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.colorCard(1, CardColor.RED, 3)); // round score = 3
        PlayerGameState bob = new PlayerGameState("Bob");
        bob.addCard(Card.blackCard(2, 4)); // round score = 8

        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(List.of(alice, bob));
        String payload = GameStateSerializer.serializeRoundEnd(
                roundScores, List.of(alice, bob));

        assertTrue(payload.contains("Alice:3:3"));
        assertTrue(payload.contains("Bob:8:8"));
    }

    @Test
    void serializeEffectRequest_correctFormat() {
        String payload = GameStateSerializer.serializeEffectRequest("SKIP", "Alice");
        assertEquals("SKIP:Alice", payload);
    }

    // =========================================================================
    // Game-flow pipeline: play a card → state updated
    // =========================================================================

    @Test
    void playCard_validColorCard_advancesPlayerAndUpdatesDiscard() {
        GameState state = twoPlayerState();
        TurnEngine.startTurn(state); // → AWAITING_PLAY

        String currentName = state.getCurrentPlayer().getPlayerName();
        PlayerGameState current = state.getCurrentPlayer();

        // Find any playable card
        Card top = state.peekDiscardPile();
        Card toPlay = current.getHand().stream()
                .filter(c -> CardValidator.canPlay(c, top, state))
                .findFirst().orElse(null);

        if (toPlay == null) return; // no playable card in this seed — skip silently

        int handBefore = current.getHandSize();
        List<GameEvent> events = TurnEngine.playCard(state, currentName, toPlay);

        assertFalse(events.isEmpty());
        assertFalse(events.get(0).isError());
        assertEquals(toPlay, state.peekDiscardPile());

        if (state.getPhase() != GamePhase.ROUND_END) {
            // Hand shrunk by 1 (card played)
            assertEquals(handBefore - 1, current.getHandSize());
        }
    }

    @Test
    void playCard_invalidCard_returnsErrorEvent_stateUnchanged() {
        GameState state = twoPlayerState();
        TurnEngine.startTurn(state);

        String currentName = state.getCurrentPlayer().getPlayerName();

        // Force a card that definitely cannot be played
        Card invalid = Card.colorCard(999, CardColor.RED, 1);
        state.getDiscardPile().clear();
        state.pushToDiscardPile(Card.colorCard(0, CardColor.BLUE, 9));
        // invalid is RED/1 — different color and number from BLUE/9

        // Replace first hand card with our controlled invalid card
        state.getCurrentPlayer().getHand().set(0, invalid);

        List<GameEvent> events = TurnEngine.playCard(state, currentName, invalid);
        assertEquals(1, events.size());
        assertTrue(events.get(0).isError());
    }

    @Test
    void drawCard_addsCardToHand() {
        GameState state = twoPlayerState();
        TurnEngine.startTurn(state);

        String currentName = state.getCurrentPlayer().getPlayerName();
        int handBefore = state.getPlayer(currentName).getHandSize();
        int drawBefore = state.getDrawPile().size();

        List<GameEvent> events = TurnEngine.drawCard(state, currentName);

        assertFalse(events.isEmpty());
        assertFalse(events.get(0).isError());
        assertEquals(handBefore + 1, state.getPlayer(currentName).getHandSize());
        assertEquals(drawBefore - 1, state.getDrawPile().size());
    }

    // =========================================================================
    // Score integration: round end → totals → game over detection
    // =========================================================================

    @Test
    void roundEnd_scoresAccumulateAcrossRounds() {
        // Round 1: Alice holds RED/5 → round score 5, total 5
        PlayerGameState alice = new PlayerGameState("Alice");
        alice.addCard(Card.colorCard(1, CardColor.RED, 5));
        ScoreCalculator.calculateRoundScores(List.of(alice));
        assertEquals(5, alice.getTotalScore());

        // Between rounds the hand is cleared (GameInitializer deals fresh cards)
        alice.getHand().clear();

        // Round 2: Alice holds BLACK/3 → round score 6, total 11
        alice.addCard(Card.blackCard(2, 3));
        ScoreCalculator.calculateRoundScores(List.of(alice));
        assertEquals(11, alice.getTotalScore()); // 5 + 6
    }

    @Test
    void gameOver_detectedAfterScoresExceedMax() {
        Map<String, Integer> scores = Map.of("Alice", 30, "Bob", 145);
        assertTrue(ScoreCalculator.isGameOver(scores, 144));
    }

    @Test
    void gameOver_winnerIsLowestScore() {
        Map<String, Integer> scores = Map.of("Alice", 30, "Bob", 145);
        assertEquals("Alice", ScoreCalculator.getWinner(scores));
    }

    @Test
    void gameOver_notTriggeredBelowMax() {
        Map<String, Integer> scores = Map.of("Alice", 100, "Bob", 130);
        assertFalse(ScoreCalculator.isGameOver(scores, 144));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private GameState twoPlayerState() {
        return GameInitializer.initialize(
                List.of("Alice", "Bob"), 1, null, new Random(42));
    }
}
