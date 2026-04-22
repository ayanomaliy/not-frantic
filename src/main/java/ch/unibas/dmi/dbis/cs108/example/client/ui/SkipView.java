package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class SkipView extends StackPane {

    private String selectedPlayer;
    private final Button doneButton = new Button("Done");

    private Consumer<String> onFinish;

    public SkipView(List<String> players) {
        getStyleClass().add("effect-response-overlay");

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Skip");
        title.getStyleClass().add("effect-response-title");

        Label subtitle = new Label("Pick a player to skip");
        subtitle.getStyleClass().add("effect-response-subtitle");

        ListView<String> playerList = new ListView<>();
        playerList.setItems(FXCollections.observableArrayList(players));
        playerList.getStyleClass().addAll("frantic-list-view", "effect-response-player-list");
        playerList.setPrefHeight(180);
        playerList.setMaxWidth(280);

        playerList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedPlayer = newValue;
            doneButton.setDisable(selectedPlayer == null || selectedPlayer.isBlank());
        });

        doneButton.getStyleClass().addAll("frantic-button", "primary-button");
        doneButton.setDisable(true);
        doneButton.setOnAction(e -> {
            if (onFinish != null && selectedPlayer != null && !selectedPlayer.isBlank()) {
                onFinish.accept(selectedPlayer);
            }
        });

        panel.getChildren().addAll(title, subtitle, playerList, doneButton);
        getChildren().add(panel);
    }

    public void setOnFinish(Consumer<String> onFinish) {
        this.onFinish = onFinish;
    }
}