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
 * Game screen layout with:
 * - game area on the left
 * - players / game info / chat on the right
 */
public class GameView extends StackPane {

    private final BorderPane mainLayout = new BorderPane();

    // LEFT SIDE - GAME AREA
    private final VBox gameArea = new VBox();
    private final StackPane centerTablePane = new StackPane();
    private final VBox centerInfoBox = new VBox();
    private final VBox handSection = new VBox();

    private final Label currentPlayerLabel = new Label("Current Player: -");
    private final Label phaseLabel = new Label("Phase: -");
    private final Label discardTopLabel = new Label("Top Card: -");

    private final Button drawButton = new Button("Draw Card");
    private final Button endTurnButton = new Button("End Turn");
    private final Button backToLobbyButton = new Button("Back to Lobby");

    private final FlowPane playerHandPane = new FlowPane();

    // RIGHT SIDE - SIDEBAR
    private final VBox sideBar = new VBox();

    private final ListView<String> playersList = new ListView<>();
    private final Button leaveButton = new Button("Leave");

    private final ListView<String> gameInfoList = new ListView<>();

    private final ListView<String> chatList = new ListView<>();
    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");

    public GameView() {
        buildLayout();
        getChildren().add(mainLayout);
    }

    private void buildLayout() {
        setPadding(new Insets(20));
        mainLayout.setPadding(new Insets(20));

        buildGameArea();
        buildRightSidebar();

        mainLayout.setCenter(gameArea);
        mainLayout.setRight(sideBar);
    }

    private void buildGameArea() {
        gameArea.setSpacing(20);
        gameArea.setPadding(new Insets(10));

        buildCenterArea();
        buildBottomHandArea();

        VBox.setVgrow(centerTablePane, Priority.ALWAYS);
        centerTablePane.setMaxWidth(Double.MAX_VALUE);

        gameArea.getChildren().addAll(centerTablePane, handSection);
    }

    private void buildCenterArea() {
        centerTablePane.setMinHeight(420);
        centerTablePane.setPrefHeight(500);

        HBox pilesBox = new HBox(30);
        pilesBox.setAlignment(Pos.CENTER);

        VBox drawPileBox = createPileBox("Draw Pile");
        VBox discardPileBox = createPileBox("Discard Pile");

        pilesBox.getChildren().addAll(drawPileBox, discardPileBox);

        centerInfoBox.setAlignment(Pos.CENTER);
        centerInfoBox.setSpacing(10);

        HBox buttonRow = new HBox(10, drawButton, endTurnButton, backToLobbyButton);
        buttonRow.setAlignment(Pos.CENTER);

        VBox centerWrapper = new VBox(
                20,
                pilesBox,
                currentPlayerLabel,
                phaseLabel,
                discardTopLabel,
                buttonRow
        );
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setMaxWidth(600);

        centerTablePane.getChildren().add(centerWrapper);
    }

    private void buildBottomHandArea() {
        handSection.setSpacing(10);
        handSection.setPadding(new Insets(10));

        Label handTitle = new Label("Your Hand");
        handTitle.getStyleClass().add("section-title");

        playerHandPane.setHgap(10);
        playerHandPane.setVgap(10);
        playerHandPane.setPrefWrapLength(900);

        handSection.getChildren().addAll(handTitle, playerHandPane);
    }

    private void buildRightSidebar() {
        sideBar.setSpacing(20);
        sideBar.setPadding(new Insets(10));
        sideBar.setPrefWidth(320);

        // Players section
        playersList.setPrefHeight(140);
        VBox playersContent = new VBox(10, playersList, leaveButton);
        VBox playersBox = createSection("Players in Lobby", playersContent);

        // Game info section
        gameInfoList.setPrefHeight(180);
        VBox infoContent = new VBox(gameInfoList);
        VBox infoBox = createSection("Game Info", infoContent);

        // Chat section
        chatList.setPrefHeight(220);

        HBox chatInputRow = new HBox(10, chatInput, sendButton);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        VBox chatContent = new VBox(10, chatList, chatInputRow);
        VBox.setVgrow(chatList, Priority.ALWAYS);

        VBox chatBox = createSection("Chat", chatContent);
        VBox.setVgrow(chatBox, Priority.ALWAYS);

        sideBar.getChildren().addAll(playersBox, infoBox, chatBox);
    }

    private VBox createSection(String titleText, VBox content) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        VBox box = new VBox(10, title, content);
        box.setPadding(new Insets(12));

        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.55);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #aab0d6;" +
                        "-fx-border-radius: 16;"
        );

        return box;
    }

    private VBox createPileBox(String titleText) {
        Label title = new Label(titleText);

        StackPane cardPlaceholder = new StackPane();
        cardPlaceholder.setPrefSize(100, 140);
        cardPlaceholder.setMinSize(100, 140);
        cardPlaceholder.setMaxSize(100, 140);

        cardPlaceholder.setStyle(
                "-fx-background-color: rgba(255,255,255,0.65);" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #aab0d6;" +
                        "-fx-border-radius: 14;"
        );

        VBox box = new VBox(10, title, cardPlaceholder);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // --- Getters ---

    public Label getCurrentPlayerLabel() {
        return currentPlayerLabel;
    }

    public Label getPhaseLabel() {
        return phaseLabel;
    }

    public Label getDiscardTopLabel() {
        return discardTopLabel;
    }

    public Button getDrawButton() {
        return drawButton;
    }

    public Button getEndTurnButton() {
        return endTurnButton;
    }

    public Button getBackToLobbyButton() {
        return backToLobbyButton;
    }

    public FlowPane getPlayerHandPane() {
        return playerHandPane;
    }

    public ListView<String> getPlayersList() {
        return playersList;
    }

    public Button getLeaveButton() {
        return leaveButton;
    }

    public ListView<String> getGameInfoList() {
        return gameInfoList;
    }

    public ListView<String> getChatList() {
        return chatList;
    }

    public TextField getChatInput() {
        return chatInput;
    }

    public Button getSendButton() {
        return sendButton;
    }
}