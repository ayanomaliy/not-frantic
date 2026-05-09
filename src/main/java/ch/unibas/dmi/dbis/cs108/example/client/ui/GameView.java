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
import javafx.collections.ObservableList;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.ListCell;

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

    private static final double PLAYERS_HEIGHT_COLLAPSED = 350;
    private static final double CHAT_HEIGHT_COLLAPSED = 78;

    private static final double PLAYERS_HEIGHT_EXPANDED = 150;
    private static final double CHAT_HEIGHT_EXPANDED = 560;

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
    private Pane handFanPane;

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

    // events
    private final StackPane eventOverlay = new StackPane();
    private final VBox eventPanel = new VBox(18);
    private final Label eventTitleLabel = new Label();
    private final Label eventDescriptionLabel = new Label();

    private CircularTablePane circularTablePane;

    private VBox playersBox;
    private VBox infoBox;
    private VBox chatBox;

    private VBox chatContentBox;
    private HBox chatInputRow;

    // Fade in animation
    private final StackPane rootStack = new StackPane();
    private RoundResultsOverlay currentRoundResultsOverlay;
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

        installWrappingListCells(chatList);
        installWrappingListCells(gameInfoList);

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

        configureEventOverlay();

        rootStack.getChildren().addAll(outerScrollPane, turnOverlay, eventOverlay);
        StackPane.setAlignment(turnOverlay, Pos.CENTER);
        StackPane.setAlignment(eventOverlay, Pos.CENTER);

        setCenter(rootStack);
    }

    private void configureEventOverlay() {
        eventOverlay.getStyleClass().add("event-overlay");
        eventOverlay.setAlignment(Pos.CENTER);

        eventOverlay.setVisible(false);
        eventOverlay.setOpacity(0.0);
        eventOverlay.setMouseTransparent(true);

        eventOverlay.prefWidthProperty().bind(rootStack.widthProperty());
        eventOverlay.prefHeightProperty().bind(rootStack.heightProperty());
        eventOverlay.maxWidthProperty().bind(rootStack.widthProperty());
        eventOverlay.maxHeightProperty().bind(rootStack.heightProperty());

        eventPanel.getStyleClass().add("event-panel");
        eventPanel.setAlignment(Pos.CENTER);
        eventPanel.setFillWidth(false);
        eventPanel.setMinHeight(320);

        // Make the manga panel longer than the window so it always covers it.
        eventPanel.prefWidthProperty().bind(rootStack.widthProperty().multiply(1.35));
        eventPanel.maxWidthProperty().bind(rootStack.widthProperty().multiply(1.35));

        eventTitleLabel.getStyleClass().add("event-title");
        eventTitleLabel.setWrapText(true);
        eventTitleLabel.setAlignment(Pos.CENTER);

        eventDescriptionLabel.getStyleClass().add("event-description");
        eventDescriptionLabel.setWrapText(true);
        eventDescriptionLabel.setMaxWidth(520);
        eventDescriptionLabel.setAlignment(Pos.CENTER);

        eventPanel.getChildren().setAll(eventTitleLabel, eventDescriptionLabel);
        eventOverlay.getChildren().setAll(eventPanel);
    }

    public void playEventOverlay(String title, String description) {
        if (title == null || title.isBlank()) {
            return;
        }

        eventTitleLabel.setText(title);
        eventDescriptionLabel.setText(description == null ? "" : description);

        eventOverlay.setVisible(true);
        eventOverlay.setOpacity(1.0);

        outerScrollPane.setEffect(turnBlur);

        double sceneWidth = rootStack.getWidth() > 0 ? rootStack.getWidth() : 1280;
        double startX = sceneWidth * 1.2;

        eventPanel.setTranslateX(startX);

        Timeline blurIn = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(turnBlur.radiusProperty(), 0)
                ),
                new KeyFrame(
                        Duration.millis(90),
                        new KeyValue(
                                turnBlur.radiusProperty(),
                                12,
                                Interpolator.SPLINE(0.15, 0.95, 0.25, 1.0)
                        )
                )
        );

        Timeline slideIn = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(eventPanel.translateXProperty(), startX, Interpolator.EASE_OUT)
                ),
                new KeyFrame(
                        Duration.millis(170),
                        new KeyValue(eventPanel.translateXProperty(), 0, Interpolator.EASE_OUT)
                )
        );

        int baseMs = 1800;
        int extraMs = Math.min(2200, eventDescriptionLabel.getText().length() * 35);
        PauseTransition hold = new PauseTransition(Duration.millis(baseMs + extraMs));

        Timeline slideOut = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(eventPanel.translateXProperty(), 0, Interpolator.EASE_IN)
                ),
                new KeyFrame(
                        Duration.millis(220),
                        new KeyValue(eventPanel.translateXProperty(), -sceneWidth * 1.25, Interpolator.EASE_IN)
                )
        );

        Timeline blurOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(turnBlur.radiusProperty(), 12)),
                new KeyFrame(Duration.millis(220), new KeyValue(turnBlur.radiusProperty(), 0))
        );

        slideOut.setOnFinished(e -> {
            eventOverlay.setVisible(false);
            eventPanel.setTranslateX(0);
            outerScrollPane.setEffect(turnBlur);
        });

        blurIn.play();
        slideIn.setOnFinished(e -> hold.play());
        hold.setOnFinished(e -> {
            blurOut.play();
            slideOut.play();
        });

        slideIn.play();
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

        HBox buttonRow = new HBox(endTurnButton);
        buttonRow.setAlignment(Pos.CENTER);

        VBox centerWrapper = new VBox(24, pilesBox, statusRow, buttonRow);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setMaxWidth(Double.MAX_VALUE);
        centerWrapper.setTranslateY(36);

        circularTablePane = new CircularTablePane(centerWrapper);
        centerTablePane.getChildren().add(circularTablePane);
    }

    /**
     * Builds the bottom hand section with a fan arc layout.
     *
     * <p>Cards are positioned by {@link ch.unibas.dmi.dbis.cs108.example.client.ui.MainController}
     * using a circular arc formula so the hand curves up from the bottom center.</p>
     */
    private void buildBottomHandArea() {
        handFanPane = new Pane();
        handFanPane.setPrefHeight(180);
        handFanPane.prefWidthProperty().bind(widthProperty().multiply(0.60));

        handSection.getChildren().addAll(
                createSectionTitle("Your Hand"),
                handFanPane
        );
    }

    /**
     * Builds the right sidebar containing the player list and the unified
     * chat / game-info / command panel.
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

        playersList.setMinHeight(120);
        VBox.setVgrow(playersList, Priority.ALWAYS);

        chatInputRow = new HBox(10, chatInput, sendButton);
        chatInputRow.setAlignment(Pos.CENTER_LEFT);
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
        chatBox.setMaxHeight(Double.MAX_VALUE);

        chatList.setMinHeight(120);

        VBox.setVgrow(playersBox, Priority.ALWAYS);
        VBox.setVgrow(chatBox, Priority.ALWAYS);

        sideBar.getChildren().addAll(playersBox, chatBox);

        return sideBar;
    }

    /**
     * Applies the initial collapsed chat state without animation.
     */
    private void applyInitialChatState() {
        chatExpanded = false;
        toggleChatButton.setText("Expand Chat");

        playersBox.setPrefHeight(PLAYERS_HEIGHT_COLLAPSED);
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
     * Expands the chat panel and animates the player panel smaller.
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
                        new KeyValue(chatBox.prefHeightProperty(), CHAT_HEIGHT_EXPANDED, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();

        chatContentBox.setOpacity(1.0);
        new FadeInUp(chatContentBox).play();
    }

    /**
     * Collapses the chat panel and animates the player panel back to its
     * default size.
     */
    private void collapseChat() {
        toggleChatButton.setText("Expand Chat");

        Timeline timeline = new Timeline(
                new KeyFrame(
                        PANEL_ANIMATION_DURATION,
                        new KeyValue(playersBox.prefHeightProperty(), PLAYERS_HEIGHT_COLLAPSED, Interpolator.EASE_BOTH),
                        new KeyValue(chatBox.prefHeightProperty(), CHAT_HEIGHT_COLLAPSED, Interpolator.EASE_BOTH)
                )
        );

        timeline.setOnFinished(e -> {
            chatModeButton.setManaged(false);
            chatModeButton.setVisible(false);

            chatContentBox.setManaged(false);
            chatContentBox.setVisible(false);
            chatContentBox.setOpacity(0.0);
        });

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

        pilePane.setPrefSize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);
        pilePane.setMinSize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);
        pilePane.setMaxSize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);

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
     * Returns the legacy flow-pane field (kept for backward compatibility).
     *
     * @return the player hand flow pane (not used in the fan layout)
     */
    public FlowPane getPlayerHandPane() {
        return playerHandPane;
    }

    /**
     * Returns the fan-arc pane where the local player's hand cards are rendered.
     *
     * @return the hand fan pane
     */
    public Pane getHandFanPane() {
        return handFanPane;
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


    public StackPane getRootStack() {
        return rootStack;
    }

    /**
     * Returns the circular table pane that manages opponent player slots.
     *
     * @return the circular table pane
     */
    public CircularTablePane getCircularTablePane() {
        return circularTablePane;
    }

    /**
     * Shows the round-results overlay on top of the game board.
     *
     * <p>Any previously shown overlay is dismissed first. The overlay fills the
     * root stack and fades in automatically.</p>
     *
     * @param scoreRows        the score rows to display
     * @param onStartNextRound callback invoked when the user clicks "Next Round"
     * @param onLeaveLobby     callback invoked when the user clicks "Leave Lobby"
     */
    public void showRoundResults(ObservableList<String> scoreRows,
                                 Runnable onStartNextRound,
                                 Runnable onLeaveLobby) {
        hideRoundResults();

        currentRoundResultsOverlay = new RoundResultsOverlay(scoreRows, onStartNextRound, onLeaveLobby);
        currentRoundResultsOverlay.prefWidthProperty().bind(rootStack.widthProperty());
        currentRoundResultsOverlay.prefHeightProperty().bind(rootStack.heightProperty());
        currentRoundResultsOverlay.maxWidthProperty().bind(rootStack.widthProperty());
        currentRoundResultsOverlay.maxHeightProperty().bind(rootStack.heightProperty());

        rootStack.getChildren().add(currentRoundResultsOverlay);
    }

    /**
     * Fades out and removes the round-results overlay, if one is currently shown.
     */
    public void hideRoundResults() {
        if (currentRoundResultsOverlay == null) {
            return;
        }

        RoundResultsOverlay overlay = currentRoundResultsOverlay;
        currentRoundResultsOverlay = null;
        overlay.dismiss(() -> rootStack.getChildren().remove(overlay));
    }


    /**
     * Renders the draw pile as a face-down card stack.
     *
     * @param drawPileSize current number of cards in the draw pile
     */
    public void renderDrawPile(int drawPileSize) {
        drawPilePane.getChildren().clear();

        drawPilePane.getStyleClass().removeAll(
                "draw-pile-empty",
                "draw-pile-has-cards"
        );

        if (drawPileSize <= 0) {
            drawPilePane.getStyleClass().add("draw-pile-empty");
            return;
        }

        drawPilePane.getStyleClass().add("draw-pile-has-cards");

        CardBacksideView back = new CardBacksideView();
        back.setMouseTransparent(true);
        drawPilePane.getChildren().setAll(back);
    }

    /**
     * Installs wrapping cells so long chat/game-info messages continue
     * on the next line instead of creating a horizontal scrollbar.
     *
     * @param listView the list view whose string items should wrap
     */
    private void installWrappingListCells(ListView<String> listView) {
        listView.setCellFactory(view -> new ListCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.getStyleClass().add("wrapped-list-cell-label");

                /*
                 * Bind the label width to the list width.
                 * The subtraction leaves room for padding and the vertical scrollbar.
                 */
                label.maxWidthProperty().bind(view.widthProperty().subtract(36));
                label.prefWidthProperty().bind(view.widthProperty().subtract(36));

                setPrefWidth(0);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                label.setText(item);
                setText(null);
                setGraphic(label);
            }
        });
    }

}