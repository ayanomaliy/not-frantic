package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Game screen for the Frantic^-1 GUI client.
 *
 * <p>This view follows the same visual design language as the connect and
 * lobby screens by relying on shared CSS classes such as {@code panel},
 * {@code section-title}, {@code frantic-button}, and
 * {@code frantic-list-view}.</p>
 */
public class GameView extends BorderPane {

    private final VBox gameArea = new VBox(16);
    private final StackPane centerTablePane = new StackPane();
    private final VBox handSection = new VBox(10);

    private final Label currentPlayerLabel = new Label("Current Player: -");
    private final Label phaseLabel = new Label("Phase: -");
    private final Label discardTopLabel = new Label("Top Card: -");

    private final Button drawButton = new Button("Draw Card");
    private final Button endTurnButton = new Button("End Turn");

    private final FlowPane playerHandPane = new FlowPane();

    private final VBox sideBar = new VBox(16);

    private final ListView<String> playersList = new ListView<>();
    private final Button leaveButton = new Button("Leave Lobby");

    private final ListView<String> gameInfoList = new ListView<>();

    private final ListView<String> chatList = new ListView<>();
    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");

    private final TextField commandInput = new TextField();
    private final Button commandButton = new Button("Run");

    private final Button chatModeButton = new Button("Global");

    /**
     * Creates the game view.
     */
    public GameView() {
        getStyleClass().addAll("screen", "game-screen");
        setPadding(new Insets(16));

        configureControls();
        buildLayout();
    }

    /**
     * Applies shared CSS classes and basic control configuration.
     */
    private void configureControls() {
        drawButton.getStyleClass().addAll("frantic-button", "primary-button");
        endTurnButton.getStyleClass().addAll("frantic-button", "secondary-button");
        leaveButton.getStyleClass().addAll("frantic-button", "danger-button");

        sendButton.getStyleClass().addAll("frantic-button", "primary-button");
        commandButton.getStyleClass().addAll("frantic-button", "secondary-button");
        chatModeButton.getStyleClass().addAll("frantic-button", "secondary-button");

        chatInput.getStyleClass().add("frantic-text-field");
        commandInput.getStyleClass().add("frantic-text-field");

        chatInput.setPromptText("Type a global message...");
        commandInput.setPromptText("/name Alice, /lobbies, /create Lobby1, /join Lobby1, /players, /start, /quit");

        playersList.getStyleClass().add("frantic-list-view");
        gameInfoList.getStyleClass().add("frantic-list-view");
        chatList.getStyleClass().add("frantic-list-view");

        currentPlayerLabel.getStyleClass().add("field-label");
        phaseLabel.getStyleClass().add("field-label");
        discardTopLabel.getStyleClass().add("field-label");

        playerHandPane.getStyleClass().add("hand-pane");
    }

    /**
     * Builds the overall screen layout.
     */
    private void buildLayout() {
        setCenter(buildGamePanel());
        setRight(buildSidebar());

        BorderPane.setMargin(gameArea, new Insets(0, 12, 0, 0));
    }

    /**
     * Builds the main game panel on the left side.
     *
     * @return the configured main game panel
     */
    private VBox buildGamePanel() {
        gameArea.getStyleClass().add("panel");

        buildCenterArea();
        buildBottomHandArea();

        VBox.setVgrow(centerTablePane, Priority.ALWAYS);

        gameArea.getChildren().addAll(
                createSectionTitle("Game Table"),
                centerTablePane,
                handSection
        );

        return gameArea;
    }

    /**
     * Builds the central table area with piles, labels, and main action buttons.
     */
    private void buildCenterArea() {
        centerTablePane.setMinHeight(420);
        centerTablePane.setPrefHeight(500);

        HBox pilesBox = new HBox(28);
        pilesBox.setAlignment(Pos.CENTER);

        VBox drawPileBox = createPileBox("Draw Pile");
        VBox discardPileBox = createPileBox("Discard Pile");
        pilesBox.getChildren().addAll(drawPileBox, discardPileBox);

        HBox statusRow = new HBox(20, currentPlayerLabel, phaseLabel, discardTopLabel);
        statusRow.setAlignment(Pos.CENTER);

        HBox buttonRow = new HBox(10, drawButton, endTurnButton);
        buttonRow.setAlignment(Pos.CENTER);

        VBox centerWrapper = new VBox(24, pilesBox, statusRow, buttonRow);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setMaxWidth(Double.MAX_VALUE);

        centerTablePane.getChildren().add(centerWrapper);
    }

    /**
     * Builds the bottom hand section.
     */
    private void buildBottomHandArea() {
        handSection.getChildren().addAll(
                createSectionTitle("Your Hand"),
                playerHandPane
        );

        playerHandPane.setHgap(12);
        playerHandPane.setVgap(12);
        playerHandPane.setPrefWrapLength(900);
    }

    /**
     * Builds the right sidebar containing players, game info, command input, and chat.
     *
     * @return the configured sidebar
     */
    private VBox buildSidebar() {
        sideBar.setPrefWidth(320);

        VBox playersBox = new VBox(
                10,
                createSectionTitle("Players in Lobby"),
                playersList,
                leaveButton
        );
        playersBox.getStyleClass().add("panel");
        playersList.setPrefHeight(150);

        HBox commandBox = new HBox(10, commandInput, commandButton);
        commandBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        VBox infoBox = new VBox(
                10,
                createSectionTitle("Game Info"),
                gameInfoList,
                createSectionTitle("Command"),
                commandBox
        );
        infoBox.getStyleClass().add("panel");
        gameInfoList.setPrefHeight(190);
        VBox.setVgrow(gameInfoList, Priority.ALWAYS);

        HBox chatInputRow = new HBox(10, chatInput, sendButton);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        VBox chatBox = new VBox(
                10,
                createChatHeader(),
                chatList,
                chatInputRow
        );
        chatBox.getStyleClass().add("panel");
        VBox.setVgrow(chatList, Priority.ALWAYS);

        sideBar.getChildren().addAll(playersBox, infoBox, chatBox);
        VBox.setVgrow(chatBox, Priority.ALWAYS);

        return sideBar;
    }

    /**
     * Creates a styled section title label.
     *
     * @param text the title text
     * @return the configured label
     */
    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * Creates the header row for the chat section.
     *
     * @return the configured chat header row
     */
    private HBox createChatHeader() {
        Label title = createSectionTitle("Chat");
        HBox header = new HBox(10, title, chatModeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Creates a visual placeholder for a card pile.
     *
     * @param titleText the pile title
     * @return the configured pile box
     */
    private VBox createPileBox(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("field-label");

        StackPane cardPlaceholder = new StackPane();
        cardPlaceholder.setPrefSize(110, 150);
        cardPlaceholder.setMinSize(110, 150);
        cardPlaceholder.setMaxSize(110, 150);
        cardPlaceholder.getStyleClass().add("pile-placeholder");

        VBox box = new VBox(10, title, cardPlaceholder);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /**
     * Returns the current player label.
     *
     * @return the current player label
     */
    public Label getCurrentPlayerLabel() {
        return currentPlayerLabel;
    }

    /**
     * Returns the phase label.
     *
     * @return the phase label
     */
    public Label getPhaseLabel() {
        return phaseLabel;
    }

    /**
     * Returns the discard-top label.
     *
     * @return the discard-top label
     */
    public Label getDiscardTopLabel() {
        return discardTopLabel;
    }

    /**
     * Returns the draw button.
     *
     * @return the draw button
     */
    public Button getDrawButton() {
        return drawButton;
    }

    /**
     * Returns the end-turn button.
     *
     * @return the end-turn button
     */
    public Button getEndTurnButton() {
        return endTurnButton;
    }

    /**
     * Returns the hand pane used for the current player's cards.
     *
     * @return the hand pane
     */
    public FlowPane getPlayerHandPane() {
        return playerHandPane;
    }

    /**
     * Returns the player list.
     *
     * @return the player list
     */
    public ListView<String> getPlayersList() {
        return playersList;
    }

    /**
     * Returns the leave-lobby button.
     *
     * @return the leave button
     */
    public Button getLeaveButton() {
        return leaveButton;
    }

    /**
     * Returns the game info list.
     *
     * @return the game info list
     */
    public ListView<String> getGameInfoList() {
        return gameInfoList;
    }

    /**
     * Returns the chat list.
     *
     * @return the chat list
     */
    public ListView<String> getChatList() {
        return chatList;
    }

    /**
     * Returns the chat input field.
     *
     * @return the chat input field
     */
    public TextField getChatInput() {
        return chatInput;
    }

    /**
     * Returns the send button.
     *
     * @return the send button
     */
    public Button getSendButton() {
        return sendButton;
    }

    /**
     * Returns the command input field.
     *
     * @return the command input field
     */
    public TextField getCommandInput() {
        return commandInput;
    }

    /**
     * Returns the command button.
     *
     * @return the command button
     */
    public Button getCommandButton() {
        return commandButton;
    }

    /**
     * Returns the button used to toggle between global, lobby, and whisper chat.
     *
     * @return the chat mode toggle button
     */
    public Button getChatModeButton() {
        return chatModeButton;
    }
}