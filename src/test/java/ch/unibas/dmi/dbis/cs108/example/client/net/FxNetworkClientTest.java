package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.service.Message;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FxNetworkClient}.
 *
 * <p>These tests cover GUI-state updates caused by incoming protocol messages,
 * command parsing behavior, and delegation to the wrapped protocol client.</p>
 */
class FxNetworkClientTest {

    /**
     * Starts the JavaFX runtime once for all tests.
     *
     * @throws Exception if startup coordination fails
     */
    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started.
        }
    }

    /**
     * Waits until all previously queued JavaFX runLater tasks have been processed.
     *
     * @throws Exception if the wait is interrupted or times out
     */
    private static void flushFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Replaces the private protocol client field with a fake test double.
     *
     * @param fxClient the client under test
     * @param fake the fake protocol client to inject
     * @throws Exception if reflection fails
     */
    private static void injectProtocolClient(FxNetworkClient fxClient,
                                             FakeProtocolClient fake) throws Exception {
        Field field = FxNetworkClient.class.getDeclaredField("protocolClient");
        field.setAccessible(true);
        field.set(fxClient, fake);
    }

    /**
     * Creates a client under test together with injected fake protocol client.
     *
     * @return holder containing state, fx client, and fake protocol client
     * @throws Exception if injection fails
     */
    private static TestContext createContext() throws Exception {
        ClientState state = new ClientState();
        FxNetworkClient fxClient = new FxNetworkClient(state);
        FakeProtocolClient fake = new FakeProtocolClient(fxClient);
        injectProtocolClient(fxClient, fake);
        return new TestContext(state, fxClient, fake);
    }

    /**
     * Bundles objects commonly needed in tests.
     */
    private record TestContext(ClientState state,
                               FxNetworkClient fxClient,
                               FakeProtocolClient fake) {
    }

    /**
     * Fake protocol client used to verify delegation without opening sockets.
     */
    private static class FakeProtocolClient extends ClientProtocolClient {

        private String connectedHost;
        private int connectedPort;
        private boolean disconnectCalled;
        private Boolean disconnectNotifyServer;
        private Message parseResult;

        private final List<Message> sentMessages = new ArrayList<>();

        /**
         * Creates a fake protocol client.
         *
         * @param handler the handler passed to the superclass
         */
        FakeProtocolClient(ClientMessageHandler handler) {
            super(handler);
        }

        @Override
        public void connect(String host, int port) throws IOException {
            this.connectedHost = host;
            this.connectedPort = port;
        }

        @Override
        public void disconnect() {
            this.disconnectCalled = true;
            this.disconnectNotifyServer = null;
        }

        @Override
        public void disconnect(boolean notifyServer) {
            this.disconnectCalled = true;
            this.disconnectNotifyServer = notifyServer;
        }

        @Override
        public void send(Message message) {
            sentMessages.add(message);
        }

        @Override
        public void setName(String username) {
            sentMessages.add(new Message(Message.Type.NAME, username));
        }

        @Override
        public void sendGlobalChat(String text) {
            sentMessages.add(new Message(Message.Type.GLOBALCHAT, text));
        }

        @Override
        public void sendLobbyChat(String text) {
            sentMessages.add(new Message(Message.Type.LOBBYCHAT, text));
        }

        @Override
        public void sendWhisperChat(String targetPlayer, String text) {
            sentMessages.add(new Message(Message.Type.WHISPERCHAT, targetPlayer + "|" + text));
        }

        @Override
        public void requestPlayers() {
            sentMessages.add(new Message(Message.Type.PLAYERS, ""));
        }

        @Override
        public void requestAllPlayers() {
            sentMessages.add(new Message(Message.Type.ALLPLAYERS, ""));
        }

        @Override
        public void requestLobbies() {
            sentMessages.add(new Message(Message.Type.LOBBIES, ""));
        }

        @Override
        public void createLobby(String lobbyId) {
            sentMessages.add(new Message(Message.Type.CREATE, lobbyId));
        }

        @Override
        public void joinLobby(String lobbyId) {
            sentMessages.add(new Message(Message.Type.JOIN, lobbyId));
        }

        @Override
        public void leaveLobby() {
            sentMessages.add(new Message(Message.Type.LEAVE, ""));
        }

        @Override
        public void startGame() {
            sentMessages.add(new Message(Message.Type.START, ""));
        }

        @Override
        public void requestHand() {
            sentMessages.add(new Message(Message.Type.GET_HAND, ""));
        }

        @Override
        public void drawCard() {
            sentMessages.add(new Message(Message.Type.DRAW_CARD, ""));
        }

        @Override
        public void endTurn() {
            sentMessages.add(new Message(Message.Type.END_TURN, ""));
        }

        @Override
        public void playCard(int cardId) {
            sentMessages.add(new Message(Message.Type.PLAY_CARD, String.valueOf(cardId)));
        }

        @Override
        public Message parseRawCommand(String rawInput) {
            return parseResult;
        }

        /**
         * Sets the next parse result returned by {@link #parseRawCommand(String)}.
         *
         * @param parseResult the parse result to return
         */
        void setParseResult(Message parseResult) {
            this.parseResult = parseResult;
        }

        /**
         * Returns the last sent message.
         *
         * @return the last sent message, or {@code null} if none were sent
         */
        Message lastSent() {
            if (sentMessages.isEmpty()) {
                return null;
            }
            return sentMessages.get(sentMessages.size() - 1);
        }
    }

    /**
     * Verifies that connect updates GUI state and delegates to the protocol client.
     *
     * @throws Exception if the test fails
     */
    @Test
    void connectSetsStateAndSendsName() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().connect("localhost", 5555, "Alice");
        flushFx();

        assertEquals("localhost", ctx.fake().connectedHost);
        assertEquals(5555, ctx.fake().connectedPort);
        assertTrue(ctx.state().isConnected());
        assertEquals("Alice", ctx.state().getUsername());
        assertEquals("Connected to localhost:5555", ctx.state().getStatusText());

        Message last = ctx.fake().lastSent();
        assertNotNull(last);
        assertEquals(Message.Type.NAME, last.type());
        assertEquals("Alice", last.content());
    }

    /**
     * Verifies that disconnect clears local GUI state and delegates to protocol disconnect.
     *
     * @throws Exception if the test fails
     */
    @Test
    void disconnectClearsState() throws Exception {
        TestContext ctx = createContext();

        ctx.state().setConnected(true);
        ctx.state().setCurrentLobby("Lobby1");
        ctx.state().getPlayers().add("Alice");
        ctx.state().getAllPlayers().add("Bob");
        ctx.state().getLobbies().add("Lobby1");
        ctx.state().getCurrentHandCards().add("7");
        ctx.state().getGlobalChatMessages().add("g");
        ctx.state().getLobbyChatMessages().add("l");
        ctx.state().getWhisperChatMessages().add("w");
        ctx.state().getGameMessages().add("m");
        ctx.state().setCurrentPlayer("Alice");
        ctx.state().setCurrentPhase("AWAITING_PLAY");
        ctx.state().setTopCardText("Card #7");

        ctx.fxClient().disconnect();
        flushFx();

        assertTrue(ctx.fake().disconnectCalled);
        assertFalse(ctx.state().isConnected());
        assertEquals("Disconnected", ctx.state().getStatusText());
        assertEquals("", ctx.state().getCurrentLobby());
        assertEquals("Global", ctx.state().getChatMode());

        assertTrue(ctx.state().getPlayers().isEmpty());
        assertTrue(ctx.state().getAllPlayers().isEmpty());
        assertTrue(ctx.state().getLobbies().isEmpty());
        assertTrue(ctx.state().getCurrentHandCards().isEmpty());
        assertTrue(ctx.state().getGlobalChatMessages().isEmpty());
        assertTrue(ctx.state().getLobbyChatMessages().isEmpty());
        assertTrue(ctx.state().getWhisperChatMessages().isEmpty());
        assertTrue(ctx.state().getGameMessages().isEmpty());
        assertTrue(ctx.state().getPlayerInfoList().isEmpty());

        assertEquals("Unknown", ctx.state().getCurrentPlayer());
        assertEquals("WAITING", ctx.state().getCurrentPhase());
        assertEquals("-", ctx.state().getTopCardText());
    }

    /**
     * Verifies delegation methods for outgoing requests.
     *
     * @throws Exception if the test fails
     */
    @Test
    void delegationMethodsSendExpectedMessages() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().setName("Alice");
        ctx.fxClient().sendGlobalChat("hello");
        ctx.fxClient().sendLobbyChat("hi lobby");
        ctx.fxClient().sendWhisperChat("Bob", "secret");
        ctx.fxClient().requestPlayers();
        ctx.fxClient().requestAllPlayers();
        ctx.fxClient().requestLobbies();
        ctx.fxClient().createLobby("Lobby1");
        ctx.fxClient().joinLobby("Lobby2");
        ctx.fxClient().leaveLobby();
        ctx.fxClient().startGame();
        ctx.fxClient().requestHand();
        ctx.fxClient().drawCard();
        ctx.fxClient().endTurn();
        ctx.fxClient().playCard(42);

        List<Message> sent = ctx.fake().sentMessages;
        assertEquals(15, sent.size());

        assertEquals(Message.Type.NAME, sent.get(0).type());
        assertEquals("Alice", sent.get(0).content());

        assertEquals(Message.Type.GLOBALCHAT, sent.get(1).type());
        assertEquals("hello", sent.get(1).content());

        assertEquals(Message.Type.LOBBYCHAT, sent.get(2).type());
        assertEquals("hi lobby", sent.get(2).content());

        assertEquals(Message.Type.WHISPERCHAT, sent.get(3).type());
        assertEquals("Bob|secret", sent.get(3).content());

        assertEquals(Message.Type.PLAYERS, sent.get(4).type());
        assertEquals(Message.Type.ALLPLAYERS, sent.get(5).type());
        assertEquals(Message.Type.LOBBIES, sent.get(6).type());

        assertEquals(Message.Type.CREATE, sent.get(7).type());
        assertEquals("Lobby1", sent.get(7).content());

        assertEquals(Message.Type.JOIN, sent.get(8).type());
        assertEquals("Lobby2", sent.get(8).content());

        assertEquals(Message.Type.LEAVE, sent.get(9).type());
        assertEquals(Message.Type.START, sent.get(10).type());
        assertEquals(Message.Type.GET_HAND, sent.get(11).type());
        assertEquals(Message.Type.DRAW_CARD, sent.get(12).type());
        assertEquals(Message.Type.END_TURN, sent.get(13).type());

        assertEquals(Message.Type.PLAY_CARD, sent.get(14).type());
        assertEquals("42", sent.get(14).content());
    }

    /**
     * Verifies sendCommand for invalid input.
     *
     * @throws Exception if the test fails
     */
    @Test
    void sendCommandHandlesNullParseResult() throws Exception {
        TestContext ctx = createContext();
        ctx.fake().setParseResult(null);

        boolean disconnected = ctx.fxClient().sendCommand("whatever");
        flushFx();

        assertFalse(disconnected);
        assertEquals("[CLIENT] Invalid input.", ctx.state().getGameMessages().get(0));
    }

    /**
     * Verifies sendCommand for unknown commands.
     *
     * @throws Exception if the test fails
     */
    @Test
    void sendCommandHandlesUnknownCommand() throws Exception {
        TestContext ctx = createContext();
        ctx.fake().setParseResult(new Message(Message.Type.UNKNOWN, "bad"));

        boolean disconnected = ctx.fxClient().sendCommand("/bad");
        flushFx();

        assertFalse(disconnected);
        assertEquals("[CLIENT] Unknown command.", ctx.state().getGameMessages().get(0));
    }

    /**
     * Verifies sendCommand for heartbeat commands.
     *
     * @throws Exception if the test fails
     */
    @Test
    void sendCommandHandlesHeartbeatMessages() throws Exception {
        TestContext ctx = createContext();
        ctx.fake().setParseResult(new Message(Message.Type.PING, ""));

        boolean disconnected = ctx.fxClient().sendCommand("PING");
        flushFx();

        assertFalse(disconnected);
        assertEquals("[CLIENT] Heartbeat messages are handled automatically.",
                ctx.state().getGameMessages().get(0));
    }

    /**
     * Verifies sendCommand sends normal commands.
     *
     * @throws Exception if the test fails
     */
    @Test
    void sendCommandSendsRegularMessage() throws Exception {
        TestContext ctx = createContext();
        ctx.fake().setParseResult(new Message(Message.Type.GLOBALCHAT, "hello"));

        boolean disconnected = ctx.fxClient().sendCommand("/g hello");
        flushFx();

        assertFalse(disconnected);
        assertEquals(Message.Type.GLOBALCHAT, ctx.fake().lastSent().type());
        assertEquals("hello", ctx.fake().lastSent().content());
    }

    /**
     * Verifies sendCommand for quit.
     *
     * @throws Exception if the test fails
     */
    @Test
    void sendCommandHandlesQuitAndClearsState() throws Exception {
        TestContext ctx = createContext();
        ctx.state().setConnected(true);
        ctx.state().getPlayers().add("Alice");

        ctx.fake().setParseResult(new Message(Message.Type.QUIT, ""));

        boolean disconnected = ctx.fxClient().sendCommand("/quit");
        flushFx();

        assertTrue(disconnected);
        assertTrue(ctx.fake().disconnectCalled);
        assertEquals(Boolean.FALSE, ctx.fake().disconnectNotifyServer);
        assertFalse(ctx.state().isConnected());
        assertEquals("Disconnected", ctx.state().getStatusText());
        assertTrue(ctx.state().getPlayers().isEmpty());
    }

    /**
     * Verifies global chat handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesGlobalChat() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.GLOBALCHAT, "Alice|hello"));
        flushFx();

        assertEquals(List.of("Alice: hello"), ctx.state().getGlobalChatMessages());
    }

    /**
     * Verifies lobby chat handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesLobbyChat() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.LOBBYCHAT, "Alice|hello"));
        flushFx();

        assertEquals(List.of("Alice: hello"), ctx.state().getLobbyChatMessages());
    }

    /**
     * Verifies whisper chat handling for FROM.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesWhisperFrom() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.WHISPERCHAT, "FROM|Bob|secret"));
        flushFx();

        assertEquals(List.of("[From Bob] secret"), ctx.state().getWhisperChatMessages());
    }

    /**
     * Verifies whisper chat handling for TO.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesWhisperTo() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.WHISPERCHAT, "TO|Bob|secret"));
        flushFx();

        assertEquals(List.of("[To Bob] secret"), ctx.state().getWhisperChatMessages());
    }

    /**
     * Verifies whisper chat handling for unknown direction.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesWhisperUnknownDirection() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.WHISPERCHAT, "SIDE|Bob|secret"));
        flushFx();

        assertEquals(List.of("SIDE|Bob|secret"), ctx.state().getWhisperChatMessages());
    }

    /**
     * Verifies whisper chat handling for malformed content.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesWhisperMalformedContent() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.WHISPERCHAT, "just-text"));
        flushFx();

        assertEquals(List.of("just-text"), ctx.state().getWhisperChatMessages());
    }

    /**
     * Verifies info handling for lobby creation.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoLobbyCreated() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "Lobby created and joined: Lobby1"));
        flushFx();

        assertEquals("Lobby1", ctx.state().getCurrentLobby());
        assertEquals("[INFO] Lobby created and joined: Lobby1", ctx.state().getGameMessages().get(0));
    }

    /**
     * Verifies info handling for joined lobby.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoJoinedLobby() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "Joined lobby: Lobby2"));
        flushFx();

        assertEquals("Lobby2", ctx.state().getCurrentLobby());
    }

    /**
     * Verifies info handling for already-in-lobby.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoAlreadyInLobby() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "You are already in lobby: Lobby3"));
        flushFx();

        assertEquals("Lobby3", ctx.state().getCurrentLobby());
    }

    /**
     * Verifies info handling for leaving a lobby.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoLeftLobby() throws Exception {
        TestContext ctx = createContext();
        ctx.state().setCurrentLobby("Lobby4");

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "You left lobby: Lobby4"));
        flushFx();

        assertEquals("", ctx.state().getCurrentLobby());
    }

    /**
     * Verifies info handling for first-time naming.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoInitialNameSet() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "Your name has been set to Alice"));
        flushFx();

        assertEquals("Alice", ctx.state().getUsername());
    }

    /**
     * Verifies info handling for taken initial name.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoTakenInitialName() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(
                Message.Type.INFO,
                "Your requested name was taken. Your name has been set to Alice(2)"
        ));
        flushFx();

        assertEquals("Alice(2)", ctx.state().getUsername());
    }

    /**
     * Verifies info handling for rename.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoRename() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.INFO, "Your name is now Bob"));
        flushFx();

        assertEquals("Bob", ctx.state().getUsername());
    }

    /**
     * Verifies info handling for taken rename.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesInfoTakenRename() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(
                Message.Type.INFO,
                "Your requested name was taken. Your name is now Bob(2)"
        ));
        flushFx();

        assertEquals("Bob(2)", ctx.state().getUsername());
    }

    /**
     * Verifies error message handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesError() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.ERROR, "Something failed"));
        flushFx();

        assertEquals(List.of("[ERROR] Something failed"), ctx.state().getGameMessages());
    }

    /**
     * Verifies players handling with non-empty content.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesPlayers() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.PLAYERS, "Alice, Bob , Charlie"));
        flushFx();

        assertEquals(List.of("Alice", "Bob", "Charlie"), ctx.state().getPlayers());
    }

    /**
     * Verifies players handling with empty content clears only game-specific state.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesEmptyPlayersAndClearsLocalGameOnly() throws Exception {
        TestContext ctx = createContext();

        ctx.state().setCurrentPlayer("Alice");
        ctx.state().setCurrentPhase("AWAITING_PLAY");
        ctx.state().setTopCardText("Card #7");
        ctx.state().getCurrentHandCards().add("7");
        ctx.state().getGlobalChatMessages().add("keep me");
        ctx.state().setCurrentLobby("Lobby1");

        ctx.fxClient().onMessage(new Message(Message.Type.PLAYERS, ""));
        flushFx();

        assertTrue(ctx.state().getPlayers().isEmpty());
        assertEquals("Unknown", ctx.state().getCurrentPlayer());
        assertEquals("WAITING", ctx.state().getCurrentPhase());
        assertEquals("-", ctx.state().getTopCardText());
        assertTrue(ctx.state().getCurrentHandCards().isEmpty());

        assertEquals(List.of("keep me"), ctx.state().getGlobalChatMessages());
        assertEquals("Lobby1", ctx.state().getCurrentLobby());
    }

    /**
     * Verifies all-players handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesAllPlayers() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.ALLPLAYERS, "Alice, Bob"));
        flushFx();

        assertEquals(List.of("Alice", "Bob"), ctx.state().getAllPlayers());
    }

    /**
     * Verifies lobby formatting.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesLobbies() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(
                Message.Type.LOBBIES,
                "FunRoom:WAITING:2:5,badformat"
        ));
        flushFx();

        assertEquals(List.of("FunRoom (WAITING) 2/5", "badformat"), ctx.state().getLobbies());
    }

    /**
     * Verifies GAME_STATE handling updates public state and triggers listener once.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesGameStateAndRunsListenerOnlyOnce() throws Exception {
        TestContext ctx = createContext();
        AtomicInteger calls = new AtomicInteger();
        ctx.fxClient().setGameStartListener(calls::incrementAndGet);

        ctx.fxClient().onMessage(new Message(
                Message.Type.GAME_STATE,
                "phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:42,drawPileSize:10,players:Alice:3:0"
        ));
        flushFx();

        assertEquals("AWAITING_PLAY", ctx.state().getCurrentPhase());
        assertEquals("Alice", ctx.state().getCurrentPlayer());
        assertEquals(
                ch.unibas.dmi.dbis.cs108.example.client.CardTextFormatter.formatCardLabelWithId(42),
                ctx.state().getTopCardText()
        );
        assertEquals(1, calls.get());
        assertEquals(1, ctx.state().getGameMessages().size());

        ctx.fxClient().onMessage(new Message(
                Message.Type.GAME_STATE,
                "phase:TURN_START,currentPlayer:Bob,discardTop:none,players:Bob:2:0"
        ));
        flushFx();

        assertEquals("TURN_START", ctx.state().getCurrentPhase());
        assertEquals("Bob", ctx.state().getCurrentPlayer());
        assertEquals("-", ctx.state().getTopCardText());
        assertEquals(1, calls.get());

        // player info list reflects the second state
        assertEquals(1, ctx.state().getPlayerInfoList().size());
        assertEquals("Bob", ctx.state().getPlayerInfoList().get(0).name());
        assertEquals(2, ctx.state().getPlayerInfoList().get(0).handSize());
    }

    /**
     * Verifies that GAME_STATE populates playerInfoList with name and hand size for every
     * player in the players section, in order.
     *
     * @throws Exception if the test fails
     */
    @Test
    void gameStatePayloadParsesPlayerInfoList() throws Exception {
        TestContext ctx = createContext();
        ctx.state().setUsername("Alice");

        ctx.fxClient().onMessage(new Message(
                Message.Type.GAME_STATE,
                "phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:none,players:Alice:5:12,Bob:3:8,Charlie:7:0"
        ));
        flushFx();

        List<ClientState.PlayerInfo> infos = ctx.state().getPlayerInfoList();
        assertEquals(3, infos.size());

        assertEquals("Alice", infos.get(0).name());
        assertEquals(5, infos.get(0).handSize());
        assertEquals("red", infos.get(0).color());

        assertEquals("Bob", infos.get(1).name());
        assertEquals(3, infos.get(1).handSize());
        assertEquals("green", infos.get(1).color());

        assertEquals("Charlie", infos.get(2).name());
        assertEquals(7, infos.get(2).handSize());
        assertEquals("blue", infos.get(2).color());
    }

    /**
     * Verifies that playerInfoList is cleared when the game resets (empty PLAYERS message).
     *
     * @throws Exception if the test fails
     */
    @Test
    void emptyPlayersMessageClearsPlayerInfoList() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(
                Message.Type.GAME_STATE,
                "phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:none,players:Alice:5:0,Bob:3:0"
        ));
        flushFx();
        assertEquals(2, ctx.state().getPlayerInfoList().size());

        ctx.fxClient().onMessage(new Message(Message.Type.PLAYERS, ""));
        flushFx();

        assertTrue(ctx.state().getPlayerInfoList().isEmpty());
    }

    /**
     * Verifies hand updates.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesHandUpdate() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.HAND_UPDATE, "7, 18 ,42"));
        flushFx();

        assertEquals(List.of("7", "18", "42"), ctx.state().getCurrentHandCards());
        assertEquals("[HAND] 7, 18 ,42", ctx.state().getGameMessages().get(0));
    }

    /**
     * Verifies game message handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesGame() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.GAME, "A move happened"));
        flushFx();

        assertEquals(List.of("[GAME] A move happened"), ctx.state().getGameMessages());
    }

    /**
     * Verifies effect request handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesEffectRequest() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.EFFECT_REQUEST, "SKIP:Alice"));
        flushFx();

        assertEquals(List.of("[EFFECT_REQUEST] SKIP:Alice"), ctx.state().getGameMessages());
    }

    /**
     * Verifies round-end handling: message logged and phase updated.
     *
     * <p>The hand must NOT be cleared here because the same ROUND_END message type is
     * also returned by the GET_ROUND_END query (a read-only request), which sends no
     * follow-up HAND_UPDATE. Clearing the hand would leave the player with an empty hand
     * until they manually re-request it. For real round ends the HAND_UPDATE from
     * broadcastAllHands replaces the hand immediately after ROUND_END arrives.</p>
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesRoundEnd() throws Exception {
        TestContext ctx = createContext();

        ctx.state().getCurrentHandCards().addAll(List.of("card1", "card2"));
        ctx.state().setCurrentPhase("AWAITING_PLAY");

        ctx.fxClient().onMessage(new Message(Message.Type.ROUND_END, "Alice:5:10"));
        flushFx();

        assertEquals(List.of("[ROUND_END] Round over! Scores: Alice:5:10"), ctx.state().getGameMessages());
        assertEquals(List.of("card1", "card2"), ctx.state().getCurrentHandCards(),
                "Hand must NOT be cleared on ROUND_END — GET_ROUND_END queries send no follow-up HAND_UPDATE");
        assertEquals("ROUND_END", ctx.state().getCurrentPhase(),
                "Phase must be set to ROUND_END");
    }

    /**
     * Verifies game-end handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesGameEnd() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.GAME_END, "Alice"));
        flushFx();

        assertEquals(List.of("[GAME_END] Alice"), ctx.state().getGameMessages());
    }

    /**
     * Verifies broadcast handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesBroadcast() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.BROADCAST, "Alice|Announcement"));
        flushFx();

        assertEquals(List.of("[INFO] [Broadcast] Alice: Announcement"), ctx.state().getGameMessages());
    }

    /**
     * Verifies default handling for unexpected message types.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onMessageHandlesUnexpectedType() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onMessage(new Message(Message.Type.PING, ""));
        flushFx();

        assertEquals(List.of("[CLIENT] Unexpected: PING|"), ctx.state().getGameMessages());
    }

    /**
     * Verifies local-message handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onLocalMessageAddsGameMessage() throws Exception {
        TestContext ctx = createContext();

        ctx.fxClient().onLocalMessage("local info");
        flushFx();

        assertEquals(List.of("local info"), ctx.state().getGameMessages());
    }

    /**
     * Verifies disconnect callback handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void onDisconnectedClearsState() throws Exception {
        TestContext ctx = createContext();

        ctx.state().setConnected(true);
        ctx.state().setCurrentLobby("Lobby1");
        ctx.state().getPlayers().add("Alice");
        ctx.state().getGameMessages().add("old");

        ctx.fxClient().onDisconnected("Connection closed.");
        flushFx();

        assertFalse(ctx.state().isConnected());
        assertEquals("Connection closed.", ctx.state().getStatusText());
        assertEquals("", ctx.state().getCurrentLobby());
        assertTrue(ctx.state().getPlayers().isEmpty());
        assertTrue(ctx.state().getGameMessages().isEmpty());
    }
}