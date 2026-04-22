package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ExchangeView extends StackPane {

    private final List<Integer> selectedCardIds = new ArrayList<>();
    private String selectedPlayer;

    private final Button doneButton = new Button("Done");

    private final List<Integer> availableCardIds;
    private final AssetRegistry registry;
    private final FlowPane cardPane = new FlowPane();

    private BiConsumer<String, List<Integer>> onFinish;

    public ExchangeView(List<String> players, List<Integer> handCardIds, AssetRegistry registry) {
        this.availableCardIds = List.copyOf(handCardIds);
        this.registry = registry;

        getStyleClass().add("effect-response-overlay");

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Exchange");
        title.getStyleClass().add("effect-response-title");

        Label cardLabel = new Label("Pick two cards to exchange");
        cardLabel.getStyleClass().add("effect-response-subtitle");

        cardPane.setAlignment(Pos.CENTER);
        cardPane.setHgap(12);
        cardPane.setVgap(12);
        cardPane.getStyleClass().add("effect-response-card-pane");

        rebuildCardPane();

        Label playerLabel = new Label("Pick who you want to exchange with");
        playerLabel.getStyleClass().add("effect-response-subtitle");

        ListView<String> playerList = new ListView<>();
        playerList.setItems(FXCollections.observableArrayList(players));
        playerList.getStyleClass().addAll("frantic-list-view", "effect-response-player-list");
        playerList.setPrefHeight(180);
        playerList.setMaxWidth(280);

        playerList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedPlayer = newValue;
            updateDoneState();
        });

        doneButton.getStyleClass().addAll("frantic-button", "primary-button");
        doneButton.setDisable(true);
        doneButton.setOnAction(e -> {
            if (onFinish != null && selectedPlayer != null && isCorrectCardSelection()) {
                onFinish.accept(selectedPlayer, List.copyOf(selectedCardIds));
            }
        });

        panel.getChildren().addAll(
                title,
                cardLabel,
                cardPane,
                playerLabel,
                playerList,
                doneButton
        );

        getChildren().add(panel);
    }

    public void setOnFinish(BiConsumer<String, List<Integer>> onFinish) {
        this.onFinish = onFinish;
    }

    private void rebuildCardPane() {
        cardPane.getChildren().clear();

        for (Integer cardId : availableCardIds) {
            CardView cardView = new CardView(cardId, registry, null);

            if (selectedCardIds.contains(cardId)) {
                cardView.getStyleClass().add("effect-response-selected-card");
            }

            cardView.setOnMouseClicked(e -> toggleCard(cardId));
            cardPane.getChildren().add(cardView);
        }
    }

    private void toggleCard(int cardId) {
        if (selectedCardIds.contains(cardId)) {
            selectedCardIds.remove(Integer.valueOf(cardId));
        } else {
            selectedCardIds.add(cardId);
            if (selectedCardIds.size() > getRequiredCardAmount()) {
                selectedCardIds.remove(0);
            }
        }

        rebuildCardPane();
        updateDoneState();
    }

    private int getRequiredCardAmount() {
        return Math.min(2, availableCardIds.size());
    }

    private boolean isCorrectCardSelection() {
        return selectedCardIds.size() == getRequiredCardAmount();
    }

    private void updateDoneState() {
        doneButton.setDisable(selectedPlayer == null || !isCorrectCardSelection());
    }
}