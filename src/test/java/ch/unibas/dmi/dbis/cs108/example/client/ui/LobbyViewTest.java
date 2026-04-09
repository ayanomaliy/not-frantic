package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LobbyView}.
 */
class LobbyViewTest {

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
     * Verifies the view root has expected style classes.
     */
    @Test
    void constructorSetsRootStyleClasses() {
        LobbyView view = new LobbyView();

        assertTrue(view.getStyleClass().contains("screen"));
        assertTrue(view.getStyleClass().contains("lobby-screen"));
    }

    /**
     * Verifies getters return the main controls.
     */
    @Test
    void gettersReturnMainControls() {
        LobbyView view = new LobbyView();

        ListView<String> lobbyPlayers = view.getLobbyPlayersList();
        ListView<String> allPlayers = view.getAllPlayersList();
        ListView<String> chatList = view.getChatList();
        ListView<String> infoList = view.getInfoList();

        TextField chatInput = view.getChatInput();
        TextField commandInput = view.getCommandInput();

        Button sendButton = view.getSendButton();
        Button commandButton = view.getCommandButton();
        Button refreshPlayersButton = view.getRefreshPlayersButton();
        Button startButton = view.getStartButton();
        Button disconnectButton = view.getDisconnectButton();
        Button refreshLobbiesButton = view.getRefreshLobbiesButton();
        Button joinLobbyButton = view.getJoinLobbyButton();
        Button createLobbyButton = view.getCreateLobbyButton();
        Button leaveLobbyButton = view.getLeaveLobbyButton();
        Button chatModeButton = view.getChatModeButton();
        ListView<String> lobbiesList = view.getLobbiesList();

        assertNotNull(lobbyPlayers);
        assertNotNull(allPlayers);
        assertNotNull(chatList);
        assertNotNull(infoList);
        assertNotNull(chatInput);
        assertNotNull(commandInput);
        assertNotNull(sendButton);
        assertNotNull(commandButton);
        assertNotNull(refreshPlayersButton);
        assertNotNull(startButton);
        assertNotNull(disconnectButton);
        assertNotNull(refreshLobbiesButton);
        assertNotNull(joinLobbyButton);
        assertNotNull(createLobbyButton);
        assertNotNull(leaveLobbyButton);
        assertNotNull(chatModeButton);
        assertNotNull(lobbiesList);
    }

    /**
     * Verifies text fields and button texts are initialized correctly.
     */
    @Test
    void constructorInitializesPromptsAndButtonTexts() {
        LobbyView view = new LobbyView();

        assertEquals("Type a message...", view.getChatInput().getPromptText());
        assertEquals(
                "/name Alice, /lobbies, /create Lobby1, /join Lobby1, /players, /start, /quit",
                view.getCommandInput().getPromptText()
        );

        assertEquals("Send", view.getSendButton().getText());
        assertEquals("Run", view.getCommandButton().getText());
        assertEquals("Refresh Players", view.getRefreshPlayersButton().getText());
        assertEquals("Start Game", view.getStartButton().getText());
        assertEquals("Disconnect", view.getDisconnectButton().getText());
        assertEquals("Refresh Lobbies", view.getRefreshLobbiesButton().getText());
        assertEquals("Join Selected Lobby", view.getJoinLobbyButton().getText());
        assertEquals("Create New Lobby", view.getCreateLobbyButton().getText());
        assertEquals("Leave Lobby", view.getLeaveLobbyButton().getText());
        assertEquals("Global", view.getChatModeButton().getText());
    }

    /**
     * Verifies major controls receive expected style classes.
     */
    @Test
    void constructorAppliesExpectedStyleClasses() {
        LobbyView view = new LobbyView();

        assertTrue(view.getLobbyPlayersList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getAllPlayersList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getChatList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getInfoList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getLobbiesList().getStyleClass().contains("frantic-list-view"));

        assertTrue(view.getChatInput().getStyleClass().contains("frantic-text-field"));
        assertTrue(view.getCommandInput().getStyleClass().contains("frantic-text-field"));

        assertTrue(view.getSendButton().getStyleClass().contains("frantic-button"));
        assertTrue(view.getStartButton().getStyleClass().contains("frantic-button"));
        assertTrue(view.getDisconnectButton().getStyleClass().contains("danger-button"));
        assertTrue(view.getChatModeButton().getStyleClass().contains("secondary-button"));
    }

    /**
     * Verifies the main border-pane regions are created by the constructor.
     */
    @Test
    void constructorBuildsMainLayoutRegions() {
        LobbyView view = new LobbyView();

        assertNotNull(view.getLeft());
        assertNotNull(view.getCenter());
        assertNotNull(view.getRight());
    }
}