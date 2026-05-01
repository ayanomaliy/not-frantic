package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.GamePhase;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.PlayerGameState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Higher-level integration tests for {@link ServerService} covering multi-round game sequences.
 *
 * <p>Tests wire {@code ServerService} with minimal {@link ClientSession} test doubles,
 * inject scripted {@link GameState} objects to force deterministic round endings, and
 * verify the full message sequence received by clients across a multi-round game.</p>
 *
 * <p>Two-round scenario used throughout:
 * <ul>
 *   <li>Round 1 — Alice plays her only card; hand becomes empty; draw pile is empty →
 *       round ends. Round scores: Alice=0, Bob=7. maxScore=100 → no game over.</li>
 *   <li>Round 2 — Pre-loaded cumulative scores (Alice=0, Bob=7). Alice plays her only
 *       card; Bob keeps his (value=4). After round: Bob cumulative = 7+4 = 11 ≥
 *       maxScore=10 → game over. Winner: Alice (cumulative=0, the lowest).</li>
 * </ul>
 * </p>
 */
class ServerServiceMultiRoundTest {

    // =========================================================================
    //  Infrastructure
    // =========================================================================

    /**
     * Minimal {@link ClientSession} test double that records every outgoing
     * encoded message instead of writing to a real socket.
     */
    private static class TestClientSession extends ClientSession {

        private final List<String> sentMessages = new ArrayList<>();
        private String playerName = "Anonymous";

        TestClientSession() {
            super(null, null, null);
        }

        @Override
        public void send(String text) {
            sentMessages.add(text);
        }

        @Override
        public String getPlayerName() {
            return playerName;
        }

        @Override
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        List<String> getSentMessages() {
            return sentMessages;
        }

        boolean containsSentMessage(String fragment) {
            return sentMessages.stream().anyMatch(m -> m.contains(fragment));
        }
    }

    /** Returns the {@link Lobby} with the given id from the server's private lobby map. */
    @SuppressWarnings("unchecked")
    private static Lobby getLobby(ServerService service, String lobbyId)
            throws ReflectiveOperationException {
        Field f = ServerService.class.getDeclaredField("lobbies");
        f.setAccessible(true);
        return ((Map<String, Lobby>) f.get(service)).get(lobbyId);
    }

    /** Bundles the wired objects returned by {@link #startTwoPlayerGame}. */
    private record GameSetup(
            ServerService service,
            TestClientSession alice,
            TestClientSession bob) {}

    /**
     * Registers two players, creates a lobby, and starts the game.
     * All startup messages are cleared before returning so tests begin
     * with an empty capture list.
     */
    private static GameSetup startTwoPlayerGame(String lobbyId) {
        ServerService service = new ServerService();
        TestClientSession alice = new TestClientSession();
        TestClientSession bob   = new TestClientSession();

        service.registerClient(alice);
        service.registerClient(bob);

        service.handleMessage(alice, new Message(Message.Type.NAME,   "Alice"));
        service.handleMessage(bob,   new Message(Message.Type.NAME,   "Bob"));
        service.handleMessage(alice, new Message(Message.Type.CREATE, lobbyId));
        service.handleMessage(bob,   new Message(Message.Type.JOIN,   lobbyId));
        service.handleMessage(alice, new Message(Message.Type.START,  ""));

        alice.getSentMessages().clear();
        bob.getSentMessages().clear();

        return new GameSetup(service, alice, bob);
    }

    /**
     * Round 1 scripted state.
     *
     * <p>Alice holds one card (id=1); Bob holds one card (id=2, BLUE/7).
     * The draw pile is empty. When Alice plays card 1, her hand becomes empty
     * and the round ends immediately.</p>
     *
     * <p>Round scores: Alice=0 (empty hand), Bob=7. maxScore=100 → neither
     * player triggers game over.</p>
     */
    private static GameState createRound1State() {
        PlayerGameState alice = new PlayerGameState("Alice");
        PlayerGameState bob   = new PlayerGameState("Bob");

        alice.addCard(Card.colorCard(1, CardColor.RED,  5));
        bob.addCard(Card.colorCard(2,   CardColor.BLUE, 7));

        ArrayDeque<Card> drawPile    = new ArrayDeque<>();
        ArrayDeque<Card> discardPile = new ArrayDeque<>();
        ArrayDeque<Card> eventPile   = new ArrayDeque<>();
        discardPile.push(Card.colorCard(99, CardColor.RED, 9));

        GameState state = new GameState(
                new ArrayList<>(List.of(alice, bob)),
                drawPile, discardPile, eventPile,
                100, // maxScore; Alice=0+0=0, Bob=0+7=7 — neither reaches 100
                1
        );
        state.setPhase(GamePhase.AWAITING_PLAY);
        return state;
    }

    /**
     * Round 2 scripted state (carries forward cumulative scores from round 1).
     *
     * <p>Alice: totalScore=0 (cumulative from round 1), one card (id=3).
     * Bob: totalScore=7 (cumulative from round 1), one card (id=4, BLUE/4).
     * The draw pile is empty. When Alice plays card 3, her hand becomes empty
     * and the round ends.</p>
     *
     * <p>Round scores: Alice=0, Bob=4. Cumulative after round 2:
     * Alice=0, Bob=7+4=11 ≥ maxScore=10 → game over. Winner: Alice (0 &lt; 11).</p>
     */
    private static GameState createRound2StateForGameOver() {
        PlayerGameState alice = new PlayerGameState("Alice");    // totalScore=0
        PlayerGameState bob   = new PlayerGameState("Bob", 7);  // totalScore=7 from round 1

        alice.addCard(Card.colorCard(3, CardColor.RED,  5));
        bob.addCard(Card.colorCard(4,   CardColor.BLUE, 4));

        ArrayDeque<Card> drawPile    = new ArrayDeque<>();
        ArrayDeque<Card> discardPile = new ArrayDeque<>();
        ArrayDeque<Card> eventPile   = new ArrayDeque<>();
        discardPile.push(Card.colorCard(98, CardColor.RED, 9));

        GameState state = new GameState(
                new ArrayList<>(List.of(alice, bob)),
                drawPile, discardPile, eventPile,
                10, // maxScore=10; Bob cumulative = 7+4 = 11 >= 10 → game over
                2
        );
        state.setPhase(GamePhase.AWAITING_PLAY);
        return state;
    }

    /**
     * Asserts that the first message containing {@code first} appears at a lower
     * index than the next message containing {@code second} (substring match).
     *
     * @param messages the message list to search
     * @param first    substring that must appear first
     * @param second   substring that must appear after {@code first}
     */
    private static void assertOrdered(List<String> messages, String first, String second) {
        int firstIdx  = -1;
        int secondIdx = -1;
        for (int i = 0; i < messages.size(); i++) {
            String m = messages.get(i);
            if (firstIdx < 0 && m.contains(first)) {
                firstIdx = i;
            }
            if (firstIdx >= 0 && secondIdx < 0 && m.contains(second)) {
                secondIdx = i;
            }
        }
        assertTrue(firstIdx  >= 0,
                "Expected to find '" + first + "' in messages but did not");
        assertTrue(secondIdx > firstIdx,
                "Expected '" + second + "' to appear after '" + first + "'");
    }

    // =========================================================================
    //  Tests
    // =========================================================================

    /**
     * Verifies the end-to-end message sequence for a 2-round game.
     *
     * <ul>
     *   <li>Round 1 (non-final): {@code ROUND_END} is broadcast immediately after
     *       the round-ending play. No {@code NEXT_ROUND} is sent until the client
     *       explicitly sends {@code START_NEXT_ROUND}. After that:
     *       {@code ROUND_END → NEXT_ROUND|2 → HAND_UPDATE → GAME_STATE}.</li>
     *   <li>Round 2 (final): {@code ROUND_END → GAME_END}; no {@code NEXT_ROUND}
     *       is broadcast.</li>
     *   <li>Total {@code ROUND_END} broadcasts received by a client equals the
     *       number of rounds played (2).</li>
     * </ul>
     *
     * @throws ReflectiveOperationException if lobby field access fails
     */
    @Test
    void fullGame_messageSequence_roundEndBeforeEachNewRound() throws ReflectiveOperationException {
        GameSetup g = startTwoPlayerGame("MRLobby");
        Lobby lobby = getLobby(g.service(), "MRLobby");

        // ── Round 1 ──────────────────────────────────────────────────────────
        lobby.setGameState(createRound1State());
        g.service().handleMessage(g.alice(), new Message(Message.Type.PLAY_CARD, "1"));

        // Immediately after the round-ending play: ROUND_END broadcast, nothing else.
        assertTrue(g.alice().containsSentMessage("ROUND_END|"),
                "Round 1: ROUND_END must be broadcast");
        assertFalse(g.alice().containsSentMessage("GAME_END|"),
                "Round 1: GAME_END must not be sent when game continues");
        assertFalse(g.alice().containsSentMessage("NEXT_ROUND|"),
                "Round 1: NEXT_ROUND must not be sent before START_NEXT_ROUND");

        // Client requests the next round.
        g.service().handleMessage(g.alice(), new Message(Message.Type.START_NEXT_ROUND, ""));

        // Capture the full round-1-end + next-round-setup sequence.
        List<String> preRound2 = new ArrayList<>(g.alice().getSentMessages());

        assertTrue(preRound2.stream().anyMatch(m -> m.contains("NEXT_ROUND|2")),
                "NEXT_ROUND|2 must be broadcast after START_NEXT_ROUND");

        // Ordering: ROUND_END → NEXT_ROUND|2 → HAND_UPDATE → GAME_STATE
        assertOrdered(preRound2, "ROUND_END|",   "NEXT_ROUND|2");
        assertOrdered(preRound2, "NEXT_ROUND|2", "HAND_UPDATE|");
        assertOrdered(preRound2, "HAND_UPDATE|", "GAME_STATE|");

        // ── Round 2 (final) ───────────────────────────────────────────────────
        g.alice().getSentMessages().clear();
        g.bob().getSentMessages().clear();

        lobby.setGameState(createRound2StateForGameOver());
        g.service().handleMessage(g.alice(), new Message(Message.Type.PLAY_CARD, "3"));

        List<String> round2 = new ArrayList<>(g.alice().getSentMessages());

        // Final round: ROUND_END then GAME_END; no NEXT_ROUND.
        assertTrue(round2.stream().anyMatch(m -> m.contains("ROUND_END|")),
                "Round 2: ROUND_END must be broadcast");
        assertTrue(round2.stream().anyMatch(m -> m.contains("GAME_END|")),
                "Round 2: GAME_END must be broadcast");
        assertFalse(round2.stream().anyMatch(m -> m.contains("NEXT_ROUND|")),
                "Round 2: NEXT_ROUND must not be broadcast after the final round");

        // ROUND_END must precede GAME_END in the final round.
        assertOrdered(round2, "ROUND_END|", "GAME_END|");

        // Total ROUND_END messages received == rounds played (2).
        long r1Count = preRound2.stream().filter(m -> m.contains("ROUND_END|")).count();
        long r2Count = round2.stream()  .filter(m -> m.contains("ROUND_END|")).count();
        assertEquals(2, r1Count + r2Count,
                "Total ROUND_END broadcasts must equal the number of rounds played");
    }

    /**
     * Verifies that the declared game winner is the player with the lowest
     * cumulative score at game end.
     *
     * <p>After 2 rounds Alice has a cumulative score of 0 and Bob 11.
     * The server must broadcast {@code GAME_END|Alice} and Alice's recorded
     * cumulative score must be strictly less than Bob's.</p>
     *
     * @throws ReflectiveOperationException if lobby field access fails
     */
    @Test
    void fullGame_winner_isPlayerWithLowestTotalScore() throws ReflectiveOperationException {
        GameSetup g = startTwoPlayerGame("WinnerLobby");
        Lobby lobby = getLobby(g.service(), "WinnerLobby");

        // Round 1: Alice=0, Bob=7 cumulative; no game over (maxScore=100).
        lobby.setGameState(createRound1State());
        g.service().handleMessage(g.alice(), new Message(Message.Type.PLAY_CARD, "1"));
        g.service().handleMessage(g.alice(), new Message(Message.Type.START_NEXT_ROUND, ""));

        assertEquals(0, lobby.getCumulativeScores().get("Alice"),
                "Alice emptied her hand in round 1; cumulative score must be 0");
        assertEquals(7, lobby.getCumulativeScores().get("Bob"),
                "Bob held BLUE/7 in round 1; cumulative score must be 7");

        // Round 2: Bob cumulative reaches 11 >= maxScore=10 → game over.
        g.alice().getSentMessages().clear();
        g.bob().getSentMessages().clear();

        lobby.setGameState(createRound2StateForGameOver());
        g.service().handleMessage(g.alice(), new Message(Message.Type.PLAY_CARD, "3"));

        // Alice wins; result must be broadcast to every lobby member.
        assertTrue(g.alice().containsSentMessage("GAME_END|Alice"),
                "GAME_END must name Alice as winner (lowest cumulative score)");
        assertTrue(g.bob().containsSentMessage("GAME_END|Alice"),
                "GAME_END must be broadcast to all lobby members");

        // Verify the score ordering that produced the winner.
        int aliceTotal = lobby.getCumulativeScores().get("Alice");
        int bobTotal   = lobby.getCumulativeScores().get("Bob");
        assertTrue(aliceTotal < bobTotal,
                "Winner must have the lower cumulative score: Alice=" + aliceTotal
                        + ", Bob=" + bobTotal);
    }
}
