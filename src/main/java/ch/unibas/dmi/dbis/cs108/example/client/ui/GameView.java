package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * First basic game screen for the JavaFX Frantic client.
 *
 * <p>This class currently contains only the visual skeleton of the game table.
 * Real game data will be connected later.</p>
 */
public class GameView extends StackPane {

    private final BorderPane mainLayout = new BorderPane();

    private final HBox topOpponentsBox = new HBox();
    private final VBox leftOpponentsBox = new VBox();
    private final VBox rightPanelBox = new VBox();

    private final StackPane centerTablePane = new StackPane();
    private final VBox centerInfoBox = new VBox();

    private final Label currentPlayerLabel = new Label("Current Player: -");
    private final Label phaseLabel = new Label("Phase: -");
    private final Label discardTopLabel = new Label("Top Card: -");

    private final Button drawButton = new Button("Draw Card");
    private final Button endTurnButton = new Button("End Turn");
    private final Button backToLobbyButton = new Button("Back to Lobby");

    private final FlowPane playerHandPane = new FlowPane();

    public GameView() {
        buildLayout();
        getChildren().add(mainLayout);
    }

    private void buildLayout() {
        setPadding(new Insets(20));

        mainLayout.setPadding(new Insets(20));

        buildTopArea();
        buildLeftArea();
        buildRightArea();
        buildCenterArea();
        buildBottomArea();
    }

    private void buildTopArea() {
        topOpponentsBox.setAlignment(Pos.CENTER);
        topOpponentsBox.setSpacing(20);
        topOpponentsBox.setPadding(new Insets(10));

        topOpponentsBox.getChildren().add(createOpponentPlaceholder("Top Opponent"));
        mainLayout.setTop(topOpponentsBox);
    }

    private void buildLeftArea() {
        leftOpponentsBox.setAlignment(Pos.CENTER);
        leftOpponentsBox.setSpacing(20);
        leftOpponentsBox.setPadding(new Insets(10));

        leftOpponentsBox.getChildren().add(createOpponentPlaceholder("Left Opponent"));
        mainLayout.setLeft(leftOpponentsBox);
    }

    private void buildRightArea() {
        rightPanelBox.setSpacing(15);
        rightPanelBox.setPadding(new Insets(10));
        rightPanelBox.setPrefWidth(220);

        Label rightTitle = new Label("Opponents / Info");
        rightTitle.getStyleClass().add("section-title");

        VBox rightOpponentBox = createOpponentPlaceholder("Right Opponent");

        rightPanelBox.getChildren().addAll(rightTitle, rightOpponentBox);
        mainLayout.setRight(rightPanelBox);
    }

    private void buildCenterArea() {
        centerTablePane.setMinHeight(350);
        centerTablePane.setPrefHeight(400);

        HBox pilesBox = new HBox(30);
        pilesBox.setAlignment(Pos.CENTER);

        VBox drawPileBox = createPileBox("Draw Pile");
        VBox discardPileBox = createPileBox("Discard Pile");

        pilesBox.getChildren().addAll(drawPileBox, discardPileBox);

        centerInfoBox.setAlignment(Pos.CENTER);
        centerInfoBox.setSpacing(10);

        HBox buttonRow = new HBox(10, drawButton, endTurnButton, backToLobbyButton);
        buttonRow.setAlignment(Pos.CENTER);

        centerInfoBox.getChildren().addAll(currentPlayerLabel, phaseLabel, discardTopLabel, buttonRow);

        VBox centerWrapper = new VBox(20, pilesBox, centerInfoBox);
        centerWrapper.setAlignment(Pos.CENTER);

        centerTablePane.getChildren().add(centerWrapper);
        mainLayout.setCenter(centerTablePane);
    }

    private void buildBottomArea() {
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(15));

        Label handTitle = new Label("Your Hand");
        handTitle.getStyleClass().add("section-title");

        playerHandPane.setHgap(10);
        playerHandPane.setVgap(10);
        playerHandPane.setPrefWrapLength(900);

        bottomBox.getChildren().addAll(handTitle, playerHandPane);
        mainLayout.setBottom(bottomBox);
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

    private VBox createOpponentPlaceholder(String name) {
        Label nameLabel = new Label(name);
        Label cardCountLabel = new Label("Cards: -");

        VBox box = new VBox(8, nameLabel, cardCountLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setMinWidth(140);
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.45);" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #aab0d6;" +
                        "-fx-border-radius: 14;"
        );

        return box;
    }

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
}