package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GameView}.
 */
class GameViewTest {

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
     * Waits for already scheduled JavaFX tasks to finish.
     *
     * @throws Exception if waiting fails
     */
    private static void flushFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Verifies constructor builds the main control set.
     */
    @Test
    void gettersReturnMainControls() {
        GameView view = new GameView();

        Label currentPlayerLabel = view.getCurrentPlayerLabel();
        Label phaseLabel = view.getPhaseLabel();
        Label discardTopLabel = view.getDiscardTopLabel();

        Button drawButton = view.getDrawButton();
        Button endTurnButton = view.getEndTurnButton();
        FlowPane playerHandPane = view.getPlayerHandPane();
        ListView<String> playersList = view.getPlayersList();
        Button leaveButton = view.getLeaveButton();
        ListView<String> gameInfoList = view.getGameInfoList();
        ListView<String> chatList = view.getChatList();
        TextField chatInput = view.getChatInput();
        Button sendButton = view.getSendButton();
        TextField commandInput = view.getCommandInput();
        Button commandButton = view.getCommandButton();
        Button chatModeButton = view.getChatModeButton();
        Button toggleChatButton = view.getToggleChatButton();

        assertNotNull(currentPlayerLabel);
        assertNotNull(phaseLabel);
        assertNotNull(discardTopLabel);
        assertNotNull(drawButton);
        assertNotNull(endTurnButton);
        assertNotNull(playerHandPane);
        assertNotNull(playersList);
        assertNotNull(leaveButton);
        assertNotNull(gameInfoList);
        assertNotNull(chatList);
        assertNotNull(chatInput);
        assertNotNull(sendButton);
        assertNotNull(commandInput);
        assertNotNull(commandButton);
        assertNotNull(chatModeButton);
        assertNotNull(toggleChatButton);
    }

    /**
     * Verifies root style classes and prompt texts.
     */
    @Test
    void constructorInitializesRootAndPrompts() {
        GameView view = new GameView();

        assertTrue(view.getStyleClass().contains("screen"));
        assertTrue(view.getStyleClass().contains("game-screen"));

        assertEquals("Type a global message...", view.getChatInput().getPromptText());
        assertEquals(
                "/name Alice, /lobbies, /create Lobby1, /join Lobby1, /players, /start, /quit",
                view.getCommandInput().getPromptText()
        );
    }

    /**
     * Verifies initial labels and button texts.
     */
    @Test
    void constructorInitializesLabelsAndButtons() {
        GameView view = new GameView();

        assertEquals("Current Player: -", view.getCurrentPlayerLabel().getText());
        assertEquals("Phase: -", view.getPhaseLabel().getText());
        assertEquals("Top Card: -", view.getDiscardTopLabel().getText());

        assertEquals("Draw Card", view.getDrawButton().getText());
        assertEquals("End Turn", view.getEndTurnButton().getText());
        assertEquals("Leave Lobby", view.getLeaveButton().getText());
        assertEquals("Send", view.getSendButton().getText());
        assertEquals("Run", view.getCommandButton().getText());
        assertEquals("Global", view.getChatModeButton().getText());
        assertEquals("Expand Chat", view.getToggleChatButton().getText());
    }

    /**
     * Verifies major controls receive expected style classes.
     */
    @Test
    void constructorAppliesExpectedStyleClasses() {
        GameView view = new GameView();

        assertTrue(view.getDrawButton().getStyleClass().contains("primary-button"));
        assertTrue(view.getEndTurnButton().getStyleClass().contains("secondary-button"));
        assertTrue(view.getLeaveButton().getStyleClass().contains("danger-button"));
        assertTrue(view.getSendButton().getStyleClass().contains("primary-button"));
        assertTrue(view.getCommandButton().getStyleClass().contains("secondary-button"));
        assertTrue(view.getChatModeButton().getStyleClass().contains("secondary-button"));
        assertTrue(view.getToggleChatButton().getStyleClass().contains("secondary-button"));

        assertTrue(view.getChatInput().getStyleClass().contains("frantic-text-field"));
        assertTrue(view.getCommandInput().getStyleClass().contains("frantic-text-field"));

        assertTrue(view.getPlayersList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getGameInfoList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getChatList().getStyleClass().contains("frantic-list-view"));
        assertTrue(view.getPlayerHandPane().getStyleClass().contains("hand-pane"));
    }

    /**
     * Verifies chat starts collapsed.
     */
    @Test
    void constructorStartsWithCollapsedChat() {
        GameView view = new GameView();

        assertEquals("Expand Chat", view.getToggleChatButton().getText());
        assertFalse(view.getChatModeButton().isVisible());
        assertFalse(view.getChatModeButton().isManaged());
    }

    /**
     * Verifies clicking the toggle expands the chat.
     *
     * @throws Exception if JavaFX synchronization fails
     */
    @Test
    void toggleChatExpandsChat() throws Exception {
        GameView view = new GameView();

        Platform.runLater(() -> view.getToggleChatButton().fire());
        flushFx();

        assertEquals("Collapse Chat", view.getToggleChatButton().getText());
        assertTrue(view.getChatModeButton().isVisible());
        assertTrue(view.getChatModeButton().isManaged());
    }

    /**
     * Verifies clicking the toggle twice returns to collapsed state text.
     *
     * @throws Exception if JavaFX synchronization fails
     */
    @Test
    void toggleChatCanCollapseAgain() throws Exception {
        GameView view = new GameView();

        Platform.runLater(() -> {
            view.getToggleChatButton().fire();
            view.getToggleChatButton().fire();
        });
        flushFx();

        assertEquals("Expand Chat", view.getToggleChatButton().getText());
    }

    /**
     * Verifies the main border-pane regions are built by the constructor.
     */
    @Test
    void constructorBuildsMainLayoutRegions() {
        GameView view = new GameView();

        assertNotNull(view.getCenter());
        assertNotNull(view.getRight());
    }
}