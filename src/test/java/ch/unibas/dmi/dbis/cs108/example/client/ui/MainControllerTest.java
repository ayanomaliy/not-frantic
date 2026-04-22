package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MainController}.
 *
 * <p>The test suite focuses on controller responsibilities such as screen switching, event
 * wiring, chat behavior, command handling, and rendering the current hand in the game view.</p>
 */
class MainControllerTest {

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
     * Runs code on the JavaFX thread and waits for completion.
     *
     * @param action the action to execute
     * @throws Exception if waiting fails
     */
    private static void runOnFxAndWait(ThrowingRunnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] thrown = new Throwable[1];

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                thrown[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (thrown[0] != null) {
            if (thrown[0] instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(thrown[0]);
        }
    }

    /** Functional interface for helper code that runs on the JavaFX Application Thread. */
    @FunctionalInterface
    private interface ThrowingRunnable {
        /**
         * Executes the action.
         *
         * @throws Exception if the action fails
         */
        void run() throws Exception;
    }

    /**
     * Test double for {@link FxNetworkClient} that records controller interactions.
     */
    private static class FakeFxNetworkClient extends FxNetworkClient {

        private Runnable storedGameStartListener;

        private String connectedHost;
        private int connectedPort;
        private String connectedUsername;
        private IOException connectException;

        private final List<String> globalChats = new ArrayList<>();
        private final List<String> lobbyChats = new ArrayList<>();
        private final List<String> whispers = new ArrayList<>();
        private final List<String> createdLobbies = new ArrayList<>();
        private final List<String> joinedLobbies = new ArrayList<>();
        private final List<Integer> playedCards = new ArrayList<>();
        private final List<String> commands = new ArrayList<>();

        private int requestPlayersCount = 0;
        private int requestLobbiesCount = 0;
        private int startGameCount = 0;
        private int requestHandCount = 0;
        private int drawCardCount = 0;
        private int endTurnCount = 0;
        private int leaveLobbyCount = 0;
        private int disconnectCount = 0;

        private boolean sendCommandResult = false;

        /**
         * Creates the fake network client.
         *
         * @param state shared client state
         */
        FakeFxNetworkClient(ClientState state) {
            super(state);
        }

        @Override
        public void setGameStartListener(Runnable gameStartListener) {
            this.storedGameStartListener = gameStartListener;
        }

        @Override
        public void connect(String host, int port, String username) throws IOException {
            if (connectException != null) {
                throw connectException;
            }
            this.connectedHost = host;
            this.connectedPort = port;
            this.connectedUsername = username;
        }

        @Override
        public void sendGlobalChat(String text) {
            globalChats.add(text);
        }

        @Override
        public void sendLobbyChat(String text) {
            lobbyChats.add(text);
        }

        @Override
        public void sendWhisperChat(String targetPlayer, String text) {
            whispers.add(targetPlayer + "|" + text);
        }

        @Override
        public void requestPlayers() {
            requestPlayersCount++;
        }

        @Override
        public void requestLobbies() {
            requestLobbiesCount++;
        }

        @Override
        public void createLobby(String lobbyId) {
            createdLobbies.add(lobbyId);
        }

        @Override
        public void joinLobby(String lobbyId) {
            joinedLobbies.add(lobbyId);
        }

        @Override
        public void leaveLobby() {
            leaveLobbyCount++;
        }

        @Override
        public void startGame() {
            startGameCount++;
        }

        @Override
        public void requestHand() {
            requestHandCount++;
        }

        @Override
        public void drawCard() {
            drawCardCount++;
        }

        @Override
        public void endTurn() {
            endTurnCount++;
        }

        @Override
        public void playCard(int cardId) {
            playedCards.add(cardId);
        }

        @Override
        public boolean sendCommand(String rawInput) {
            commands.add(rawInput);
            return sendCommandResult;
        }

        @Override
        public void disconnect() {
            disconnectCount++;
        }

        /**
         * Triggers the stored game-start listener, if any.
         */
        void triggerGameStartListener() {
            if (storedGameStartListener != null) {
                storedGameStartListener.run();
            }
        }
    }

    /**
     * Verifies that showConnectView installs a ConnectView scene.
     *
     * @throws Exception if the test fails
     */
    @Test
    void showConnectViewSetsConnectViewScene() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showConnectView());

        Scene scene = stage.getScene();
        assertNotNull(scene);
        assertTrue(scene.getRoot() instanceof ConnectView);
    }

    /**
     * Verifies that the connect button forwards host, port, and username and
     * then switches to the lobby view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void connectButtonConnectsAndShowsLobbyView() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showConnectView();
            ConnectView view = (ConnectView) stage.getScene().getRoot();
            view.getHostField().setText("example.org");
            view.getPortField().setText("7777");
            view.getUsernameField().setText("Alice");
            view.getConnectButton().fire();
        });

        assertEquals("example.org", network.connectedHost);
        assertEquals(7777, network.connectedPort);
        assertEquals("Alice", network.connectedUsername);
        assertTrue(stage.getScene().getRoot() instanceof LobbyView);
    }

    /**
     * Verifies that connection failure updates the shared status text.
     *
     * @throws Exception if the test fails
     */
    @Test
    void connectButtonShowsFailureInStatusText() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        network.connectException = new IOException("boom");
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showConnectView();
            ConnectView view = (ConnectView) stage.getScene().getRoot();
            view.getHostField().setText("localhost");
            view.getPortField().setText("5555");
            view.getUsernameField().setText("Alice");
            view.getConnectButton().fire();
        });

        assertEquals("Connection failed: boom", state.getStatusText());
        assertTrue(stage.getScene().getRoot() instanceof ConnectView);
    }

    /**
     * Verifies that showLobbyView binds shared lists and chat defaults.
     *
     * @throws Exception if the test fails
     */
    @Test
    void showLobbyViewWiresStateIntoView() throws Exception {
        ClientState state = new ClientState();
        state.getPlayers().add("Alice");
        state.getAllPlayers().add("Bob");
        state.getLobbies().add("FunRoom (WAITING) 1/5");
        state.getGameMessages().add("Info");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showLobbyView());

        LobbyView view = (LobbyView) stage.getScene().getRoot();

        assertSame(state.getPlayers(), view.getLobbyPlayersList().getItems());
        assertSame(state.getAllPlayers(), view.getAllPlayersList().getItems());
        assertSame(state.getLobbies(), view.getLobbiesList().getItems());
        assertSame(state.getGameMessages(), view.getInfoList().getItems());
        assertSame(state.getGlobalChatMessages(), view.getChatList().getItems());
        assertEquals("Type a global message...", view.getChatInput().getPromptText());
    }

    /**
     * Verifies the lobby chat-mode button cycles through Global, Lobby, Whisper.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyChatModeButtonCyclesModes() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showLobbyView());
        LobbyView view = (LobbyView) stage.getScene().getRoot();

        runOnFxAndWait(view.getChatModeButton()::fire);
        assertEquals("Lobby", state.getChatMode());
        assertSame(state.getLobbyChatMessages(), view.getChatList().getItems());
        assertEquals("Type a lobby message...", view.getChatInput().getPromptText());

        runOnFxAndWait(view.getChatModeButton()::fire);
        assertEquals("Whisper", state.getChatMode());
        assertSame(state.getWhisperChatMessages(), view.getChatList().getItems());
        assertEquals("player: message", view.getChatInput().getPromptText());

        runOnFxAndWait(view.getChatModeButton()::fire);
        assertEquals("Global", state.getChatMode());
        assertSame(state.getGlobalChatMessages(), view.getChatList().getItems());
        assertEquals("Type a global message...", view.getChatInput().getPromptText());
    }

    /**
     * Verifies that sending a global lobby-view chat forwards to the network client.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbySendButtonSendsGlobalChat() throws Exception {
        ClientState state = new ClientState();
        state.setChatMode("Global");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getChatInput().setText("hello world");
            view.getSendButton().fire();
            assertEquals("", view.getChatInput().getText());
        });

        assertEquals(List.of("hello world"), network.globalChats);
    }

    /**
     * Verifies that sending a lobby chat forwards to the lobby-chat method.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbySendButtonSendsLobbyChat() throws Exception {
        ClientState state = new ClientState();
        state.setChatMode("Lobby");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getChatInput().setText("hello lobby");
            view.getSendButton().fire();
        });

        assertEquals(List.of("hello lobby"), network.lobbyChats);
    }

    /**
     * Verifies whisper format forwarding from the lobby view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbySendButtonSendsWhisper() throws Exception {
        ClientState state = new ClientState();
        state.setChatMode("Whisper");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getChatInput().setText("Bob: secret");
            view.getSendButton().fire();
        });

        assertEquals(List.of("Bob|secret"), network.whispers);
    }

    /**
     * Verifies invalid whisper format produces a local client message.
     *
     * @throws Exception if the test fails
     */
    @Test
    void invalidWhisperFormatAddsClientMessage() throws Exception {
        ClientState state = new ClientState();
        state.setChatMode("Whisper");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getChatInput().setText("bad whisper");
            view.getSendButton().fire();
        });

        assertEquals(List.of("[CLIENT] Whisper format: player: message"), state.getGameMessages());
        assertTrue(network.whispers.isEmpty());
    }

    /**
     * Verifies join button extracts the pure lobby name before joining.
     *
     * @throws Exception if the test fails
     */
    @Test
    void joinLobbyButtonExtractsPureLobbyName() throws Exception {
        ClientState state = new ClientState();
        state.getLobbies().add("FunRoom (WAITING) 2/5");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getLobbiesList().getSelectionModel().select(0);
            view.getJoinLobbyButton().fire();
        });

        assertEquals(List.of("FunRoom"), network.joinedLobbies);
    }

    /**
     * Verifies refresh buttons call the corresponding network requests.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyRefreshButtonsTriggerRequests() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getRefreshPlayersButton().fire();
            view.getRefreshLobbiesButton().fire();
        });

        assertEquals(1, network.requestPlayersCount);
        assertEquals(1, network.requestLobbiesCount);
    }

    /**
     * Verifies command sending clears the field and stays on the lobby view when
     * the network client does not disconnect.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyCommandButtonSendsCommandWithoutDisconnect() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        network.sendCommandResult = false;
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getCommandInput().setText("/players");
            view.getCommandButton().fire();
            assertEquals("", view.getCommandInput().getText());
        });

        assertEquals(List.of("/players"), network.commands);
        assertTrue(stage.getScene().getRoot() instanceof LobbyView);
    }

    /**
     * Verifies disconnecting through a command returns to the connect view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyCommandButtonShowsConnectViewOnDisconnect() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        network.sendCommandResult = true;
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getCommandInput().setText("/quit");
            view.getCommandButton().fire();
        });

        assertEquals(List.of("/quit"), network.commands);
        assertTrue(stage.getScene().getRoot() instanceof ConnectView);
    }

    /**
     * Verifies leave-lobby action forwards to the network client and keeps the
     * user on a lobby view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyLeaveButtonLeavesAndShowsLobbyView() throws Exception {
        ClientState state = new ClientState();
        state.getPlayers().add("Alice");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getLeaveLobbyButton().fire();
        });

        assertEquals(1, network.leaveLobbyCount);
        assertTrue(stage.getScene().getRoot() instanceof LobbyView);
    }

    /**
     * Verifies disconnect button disconnects and returns to the connect view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void lobbyDisconnectButtonDisconnectsAndShowsConnectView() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showLobbyView();
            LobbyView view = (LobbyView) stage.getScene().getRoot();
            view.getDisconnectButton().fire();
        });

        assertEquals(1, network.disconnectCount);
        assertTrue(stage.getScene().getRoot() instanceof ConnectView);
    }

    /**
     * Verifies showGameView binds labels, renders cards, and requests the hand.
     *
     * @throws Exception if the test fails
     */
    @Test
    void showGameViewBindsStateRendersHandAndRequestsHand() throws Exception {
        ClientState state = new ClientState();
        state.getPlayers().add("Alice");
        state.setCurrentPlayer("Alice");
        state.setCurrentPhase("AWAITING_PLAY");
        state.setTopCardText("Card #42");
        state.getCurrentHandCards().addAll("7", "18", "not-a-number");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showGameView());

        GameView view = (GameView) stage.getScene().getRoot();

        assertEquals("Current Player: Alice", view.getCurrentPlayerLabel().getText());
        assertEquals("Phase: AWAITING_PLAY", view.getPhaseLabel().getText());
        assertEquals("Top Card: Card #42", view.getDiscardTopLabel().getText());

        assertEquals(2, view.getPlayerHandPane().getChildren().size());
        assertEquals(1, network.requestHandCount);
    }

    /**
     * Verifies rendered hand cards trigger playCard with the correct card id when clicked.
     *
     * @throws Exception if the test fails
     */
    @Test
    void renderedHandButtonsPlayCards() throws Exception {
        ClientState state = new ClientState();
        state.getCurrentHandCards().add("42");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showGameView();
            GameView view = (GameView) stage.getScene().getRoot();
            CardView cardView = (CardView) view.getPlayerHandPane().getChildren().get(0);
            cardView.fireEvent(new MouseEvent(
                    MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    MouseButton.PRIMARY, 1,
                    false, false, false, false,
                    false, false, false, false, false, false, null));
        });

        assertEquals(List.of(42), network.playedCards);
    }

    /**
     * Verifies game-view chat sending forwards to the correct method.
     *
     * @throws Exception if the test fails
     */
    @Test
    void gameViewSendButtonSendsGlobalChat() throws Exception {
        ClientState state = new ClientState();
        state.setChatMode("Global");
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showGameView();
            GameView view = (GameView) stage.getScene().getRoot();
            view.getChatInput().setText("hello from game");
            view.getSendButton().fire();
        });

        assertEquals(List.of("hello from game"), network.globalChats);
    }

    /**
     * Verifies game-view draw and end-turn buttons forward to the network client.
     *
     * @throws Exception if the test fails
     */
    @Test
    void gameViewActionButtonsTriggerNetworkCalls() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> {
            controller.showGameView();
            GameView view = (GameView) stage.getScene().getRoot();
            //view.getDrawButton().fire();
            view.getEndTurnButton().fire();
            view.getLeaveButton().fire();
        });

        assertEquals(1, network.drawCardCount);
        assertEquals(1, network.endTurnCount);
        assertEquals(1, network.leaveLobbyCount);
        assertTrue(stage.getScene().getRoot() instanceof LobbyView);
    }

    /**
     * Verifies the constructor-installed game-start listener switches to game view
     * when not already on it.
     *
     * @throws Exception if the test fails
     */
    @Test
    void gameStartListenerShowsGameViewWhenNeeded() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showLobbyView());
        assertTrue(stage.getScene().getRoot() instanceof LobbyView);

        runOnFxAndWait(network::triggerGameStartListener);

        assertTrue(stage.getScene().getRoot() instanceof GameView);
    }

    /**
     * Verifies the game-start listener does nothing when already on the game view.
     *
     * @throws Exception if the test fails
     */
    @Test
    void gameStartListenerDoesNothingWhenAlreadyOnGameView() throws Exception {
        ClientState state = new ClientState();
        FakeFxNetworkClient network = new FakeFxNetworkClient(state);
        Stage stage = createStageOnFxThread();
        MainController controller = new MainController(stage, state, network);

        runOnFxAndWait(() -> controller.showGameView());
        Scene before = stage.getScene();
        assertTrue(before.getRoot() instanceof GameView);

        runOnFxAndWait(network::triggerGameStartListener);

        assertSame(before, stage.getScene());
        assertTrue(stage.getScene().getRoot() instanceof GameView);
    }

    private static Stage createStageOnFxThread() throws Exception {
        final Stage[] stageRef = new Stage[1];
        runOnFxAndWait(() -> stageRef[0] = new Stage());
        return stageRef[0];
    }
}
