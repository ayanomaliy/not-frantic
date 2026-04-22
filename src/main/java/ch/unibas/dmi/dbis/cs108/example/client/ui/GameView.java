package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeInUp;
import animatefx.animation.FadeOutDown;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Game screen for the Frantic^-1 GUI client.
 *
 * <p>This view uses a responsive JavaFX layout so that the content adapts
 * better to different window sizes. The main content is wrapped in a
 * {@link ScrollPane} to prevent clipping on smaller screens, while the hand,
 * sidebar, and center table areas scale more gracefully with the stage size.</p>
 *
 * <p>The class follows the same visual design language as the connect and
 * lobby screens by relying on shared CSS classes such as {@code panel},
 * {@code section-title}, {@code frantic-button}, and
 * {@code frantic-list-view}.</p>
 */
public class GameView extends BorderPane {

    private static final double SIDEBAR_MIN_WIDTH = 250;

    private static final double PLAYERS_HEIGHT_COLLAPSED = 230;
    private static final double INFO_HEIGHT_COLLAPSED = 230;
    private static final double CHAT_HEIGHT_COLLAPSED = 78;

    private static final double PLAYERS_HEIGHT_EXPANDED = 150;
    private static final double INFO_HEIGHT_EXPANDED = 170;
    private static final double CHAT_HEIGHT_EXPANDED = 320;

    private static final Duration PANEL_ANIMATION_DURATION = Duration.millis(260);
    private static final Duration CHAT_HIDE_DELAY = Duration.millis(180);

    private final VBox gameArea = new VBox(16);
    private final StackPane centerTablePane = new StackPane();
    private final VBox handSection = new VBox(10);

    private final Label currentPlayerLabel = new Label("Current Player: -");
    private final Label phaseLabel = new Label("Phase: -");
    private final Label discardTopLabel = new Label("Top Card: -");

    private final StackPane drawPilePane = new StackPane();
    private final StackPane discardPilePane = new StackPane();

    private final Button endTurnButton = new Button("End Turn");

    private final FlowPane playerHandPane = new FlowPane();

    private final VBox sideBar = new VBox(12);

    private final ListView<String> playersList = new ListView<>();
    private final Button leaveButton = new Button("Leave Lobby");

    private final ListView<String> gameInfoList = new ListView<>();

    private final ListView<String> chatList = new ListView<>();
    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");

    private final TextField commandInput = new TextField();
    private final Button commandButton = new Button("Run");

    private final Button chatModeButton = new Button("Global");

    private boolean chatExpanded = false;
    private final Button toggleChatButton = new Button("Expand Chat");

    private VBox playersBox;
    private VBox infoBox;
    private VBox chatBox;

    private VBox chatContentBox;
    private HBox chatInputRow;

    // Fade in animation
    private final StackPane rootStack = new StackPane();
    private final ScrollPane outerScrollPane = new ScrollPane();
    private final StackPane turnOverlay = new StackPane();
    private final Label turnOverlayLabel = new Label();
    private final GaussianBlur turnBlur = new GaussianBlur(0);

    /**
     * Creates the game view.
     */
    public GameView() {
        getStyleClass().addAll("screen", "game-screen");
        setPadding(new Insets(16));

        configureControls();
        buildLayout();
        applyInitialChatState();
    }

    /**
     * Applies shared CSS classes and basic control configuration.
     */
    private void configureControls() {
        drawPilePane.getStyleClass().addAll("pile-placeholder", "draw-pile-clickable");
        discardPilePane.getStyleClass().add("pile-placeholder");

        drawPilePane.setFocusTraversable(false);
        discardPilePane.setFocusTraversable(false);

        endTurnButton.getStyleClass().addAll("frantic-button", "secondary-button");
        leaveButton.getStyleClass().addAll("frantic-button", "danger-button");

        sendButton.getStyleClass().addAll("frantic-button", "primary-button");
        commandButton.getStyleClass().addAll("frantic-button", "secondary-button");
        chatModeButton.getStyleClass().addAll("frantic-button", "secondary-button");
        toggleChatButton.getStyleClass().addAll("frantic-button", "secondary-button");

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

        toggleChatButton.setOnAction(e -> toggleChat());
    }

    /**
     * Builds the overall screen layout.
     *
     * <p>The main content is placed inside an outer scroll pane so that the
     * complete game screen remains accessible even on smaller displays.</p>
     */
    private void buildLayout() {
        BorderPane content = new BorderPane();

        content.setCenter(buildGamePanel());
        content.setRight(buildSidebar());

        BorderPane.setMargin(gameArea, new Insets(0, 12, 0, 0));

        outerScrollPane.setContent(content);
        outerScrollPane.setFitToWidth(true);
        outerScrollPane.setFitToHeight(true);
        outerScrollPane.setPannable(true);
        outerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        configureTurnOverlay();

        rootStack.getChildren().addAll(outerScrollPane, turnOverlay);
        StackPane.setAlignment(turnOverlay, Pos.CENTER);

        setCenter(rootStack);
    }

    /**
     * Builds the main game panel on the left side.
     *
     * @return the configured main game panel
     */
    private VBox buildGamePanel() {
        gameArea.getStyleClass().add("panel");
        gameArea.setMaxWidth(Double.MAX_VALUE);

        /*
         * Keep the game area responsive relative to the available width.
         * This binding is intentionally conservative so that the sidebar
         * still keeps enough room on medium-sized screens.
         */
        gameArea.prefWidthProperty().bind(widthProperty().multiply(0.70));

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
        centerTablePane.setMinHeight(250);
        centerTablePane.prefHeightProperty().bind(heightProperty().multiply(0.50));

        HBox pilesBox = new HBox(28);
        pilesBox.setAlignment(Pos.CENTER);

        VBox drawPileBox = createPileBox("Draw Pile", drawPilePane);
        VBox discardPileBox = createPileBox("Discard Pile", discardPilePane);
        pilesBox.getChildren().addAll(drawPileBox, discardPileBox);

        currentPlayerLabel.setWrapText(true);
        phaseLabel.setWrapText(true);
        discardTopLabel.setWrapText(true);

        HBox statusRow = new HBox(20, currentPlayerLabel, phaseLabel, discardTopLabel);
        statusRow.setAlignment(Pos.CENTER);

        HBox buttonRow = new HBox(10, endTurnButton);

        VBox centerWrapper = new VBox(24, pilesBox, statusRow, buttonRow);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setMaxWidth(Double.MAX_VALUE);

        centerTablePane.getChildren().add(centerWrapper);
    }

    /**
     * Builds the bottom hand section.
     *
     * <p>The player's hand is placed inside its own scroll pane so that many
     * cards remain accessible without breaking the overall layout.</p>
     */
    private void buildBottomHandArea() {
        playerHandPane.setHgap(12);
        playerHandPane.setVgap(12);

        /*
         * Make the wrapping width adapt to the current window width instead
         * of relying on a fixed number.
         */
        playerHandPane.prefWrapLengthProperty().bind(widthProperty().multiply(0.60));

        ScrollPane handScrollPane = new ScrollPane(playerHandPane);
        handScrollPane.setFitToWidth(true);
        handScrollPane.setPannable(true);
        handScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        handScrollPane.setPrefViewportHeight(180);
        handScrollPane.setMinHeight(150);

        handSection.getChildren().addAll(
                createSectionTitle("Your Hand"),
                handScrollPane
        );
    }

    /**
     * Builds the right sidebar containing players, game info, command input,
     * and chat.
     *
     * @return the configured sidebar
     */
    private VBox buildSidebar() {
        sideBar.setFillWidth(true);
        sideBar.setMinWidth(SIDEBAR_MIN_WIDTH);
        sideBar.prefWidthProperty().bind(widthProperty().multiply(0.28));

        playersBox = new VBox(
                8,
                createSectionTitle("Players in Lobby"),
                playersList,
                leaveButton
        );
        playersBox.getStyleClass().add("panel");
        playersBox.setMinHeight(130);
        playersBox.setPrefHeight(PLAYERS_HEIGHT_COLLAPSED);
        playersBox.setMaxHeight(Double.MAX_VALUE);

        playersList.setMinHeight(80);
        VBox.setVgrow(playersList, Priority.ALWAYS);

        HBox commandBox = new HBox(10, commandInput, commandButton);
        commandBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        infoBox = new VBox(
                8,
                createSectionTitle("Game Info"),
                gameInfoList,
                createSectionTitle("Command"),
                commandBox
        );
        infoBox.getStyleClass().add("panel");
        infoBox.setMinHeight(150);
        infoBox.setPrefHeight(INFO_HEIGHT_COLLAPSED);
        infoBox.setMaxHeight(Double.MAX_VALUE);

        gameInfoList.setMinHeight(120);
        VBox.setVgrow(gameInfoList, Priority.ALWAYS);

        chatInputRow = new HBox(10, chatInput, sendButton);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        chatContentBox = new VBox(10, chatList, chatInputRow);
        VBox.setVgrow(chatList, Priority.ALWAYS);

        chatBox = new VBox(
                8,
                createChatHeader(),
                chatContentBox
        );
        chatBox.getStyleClass().add("panel");
        chatBox.setPrefHeight(CHAT_HEIGHT_COLLAPSED);
        chatBox.setMinHeight(CHAT_HEIGHT_COLLAPSED);

        chatList.setMinHeight(120);

        VBox.setVgrow(playersBox, Priority.ALWAYS);
        VBox.setVgrow(infoBox, Priority.ALWAYS);

        sideBar.getChildren().addAll(playersBox, infoBox, chatBox);

        return sideBar;
    }

    /**
     * Applies the initial collapsed chat state without animation.
     */
    private void applyInitialChatState() {
        chatExpanded = false;
        toggleChatButton.setText("Expand Chat");

        playersBox.setPrefHeight(PLAYERS_HEIGHT_COLLAPSED);
        infoBox.setPrefHeight(INFO_HEIGHT_COLLAPSED);
        chatBox.setPrefHeight(CHAT_HEIGHT_COLLAPSED);

        chatModeButton.setManaged(false);
        chatModeButton.setVisible(false);

        chatContentBox.setManaged(false);
        chatContentBox.setVisible(false);
        chatContentBox.setOpacity(0.0);
    }

    /**
     * Toggles the chat between collapsed and expanded mode.
     */
    private void toggleChat() {
        chatExpanded = !chatExpanded;

        if (chatExpanded) {
            expandChat();
        } else {
            collapseChat();
        }
    }

    /**
     * Expands the chat panel and animates the other panels smaller.
     */
    private void expandChat() {
        toggleChatButton.setText("Collapse Chat");

        chatModeButton.setManaged(true);
        chatModeButton.setVisible(true);

        chatContentBox.setManaged(true);
        chatContentBox.setVisible(true);

        Timeline timeline = new Timeline(
                new KeyFrame(
                        PANEL_ANIMATION_DURATION,
                        new KeyValue(playersBox.prefHeightProperty(), PLAYERS_HEIGHT_EXPANDED, Interpolator.EASE_BOTH),
                        new KeyValue(infoBox.prefHeightProperty(), INFO_HEIGHT_EXPANDED, Interpolator.EASE_BOTH),
                        new KeyValue(chatBox.prefHeightProperty(), CHAT_HEIGHT_EXPANDED, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();

        chatContentBox.setOpacity(1.0);
        new FadeInUp(chatContentBox).play();
    }

    /**
     * Collapses the chat panel and animates the other panels back to their
     * default sizes.
     */
    private void collapseChat() {
        toggleChatButton.setText("Expand Chat");

        new FadeOutDown(chatContentBox).play();

        PauseTransition hideDelay = new PauseTransition(CHAT_HIDE_DELAY);
        hideDelay.setOnFinished(e -> {
            chatContentBox.setManaged(false);
            chatContentBox.setVisible(false);
            chatContentBox.setOpacity(0.0);

            chatModeButton.setManaged(false);
            chatModeButton.setVisible(false);
        });
        hideDelay.play();

        Timeline timeline = new Timeline(
                new KeyFrame(
                        PANEL_ANIMATION_DURATION,
                        new KeyValue(playersBox.prefHeightProperty(), PLAYERS_HEIGHT_COLLAPSED, Interpolator.EASE_BOTH),
                        new KeyValue(infoBox.prefHeightProperty(), INFO_HEIGHT_COLLAPSED, Interpolator.EASE_BOTH),
                        new KeyValue(chatBox.prefHeightProperty(), CHAT_HEIGHT_COLLAPSED, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, title, spacer, chatModeButton, toggleChatButton);
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    /**
     * Creates a visual placeholder for a card pile.
     *
     * @param titleText the pile title
     * @return the configured pile box
     */
    private VBox createPileBox(String titleText, StackPane pilePane) {
        Label title = new Label(titleText);
        title.getStyleClass().add("field-label");

        pilePane.setPrefSize(90, 130);
        pilePane.setMinSize(90, 130);
        pilePane.setMaxSize(90, 130);

        VBox box = new VBox(10, title, pilePane);
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

    /**
     * Returns the button used to expand or collapse the chat panel.
     *
     * @return the chat toggle button
     */
    public Button getToggleChatButton() {
        return toggleChatButton;
    }

    public StackPane getDrawPilePane() {
        return drawPilePane;
    }

    /**
     * Getter to display the top card of the discard pile.
     * @return discardPilePane
     */
    public StackPane getDiscardPilePane() {
        return discardPilePane;
    }

    private void configureTurnOverlay() {
        turnOverlay.getStyleClass().add("turn-overlay");
        turnOverlay.setAlignment(Pos.CENTER);

        turnOverlay.setVisible(false);
        turnOverlay.setOpacity(0.0);
        turnOverlay.setMouseTransparent(true);

        turnOverlay.prefWidthProperty().bind(rootStack.widthProperty());
        turnOverlay.prefHeightProperty().bind(rootStack.heightProperty());
        turnOverlay.maxWidthProperty().bind(rootStack.widthProperty());
        turnOverlay.maxHeightProperty().bind(rootStack.heightProperty());

        turnOverlayLabel.setText("It's your turn");
        turnOverlayLabel.getStyleClass().add("turn-overlay-label");

        turnOverlay.getChildren().setAll(turnOverlayLabel);
    }

    public void playTurnOverlay() {
        if (getCenter() == null) {
            return;
        }

        turnOverlay.setVisible(true);
        turnOverlay.setOpacity(0.0);

        outerScrollPane.setEffect(turnBlur);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), turnOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.millis(1500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), turnOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        Timeline blurIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(turnBlur.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(250), new KeyValue(turnBlur.radiusProperty(), 12))
        );

        Timeline blurOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(turnBlur.radiusProperty(), 12)),
                new KeyFrame(Duration.millis(250), new KeyValue(turnBlur.radiusProperty(), 0))
        );

        fadeOut.setOnFinished(e -> {
            turnOverlay.setVisible(false);
            outerScrollPane.setEffect(turnBlur);
        });

        fadeIn.setOnFinished(e -> blurIn.play());
        hold.setOnFinished(e -> {
            blurOut.play();
            fadeOut.play();
        });

        SequentialTransition sequence = new SequentialTransition(fadeIn, hold, fadeOut);
        sequence.play();
    }

}