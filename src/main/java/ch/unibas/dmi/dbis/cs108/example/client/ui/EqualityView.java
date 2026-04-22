package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;

public class EqualityView extends StackPane {

    private CardColor selectedColor;
    private String selectedPlayer;

    private final Button doneButton = new Button("Done");

    private BiConsumer<String, CardColor> onFinish;

    public EqualityView(List<String> players) {
        getStyleClass().add("effect-response-overlay");

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Equality");
        title.getStyleClass().add("effect-response-title");

        Label subtitle = new Label("Choose a color");
        subtitle.getStyleClass().add("effect-response-subtitle");

        FlowPane colorRow = new FlowPane();
        colorRow.setAlignment(Pos.CENTER);
        colorRow.setHgap(18);
        colorRow.getStyleClass().add("effect-response-choice-row");

        Button redButton = createColorButton(CardColor.RED, "effect-response-color-red");
        Button yellowButton = createColorButton(CardColor.YELLOW, "effect-response-color-yellow");
        Button greenButton = createColorButton(CardColor.GREEN, "effect-response-color-green");
        Button blueButton = createColorButton(CardColor.BLUE, "effect-response-color-blue");

        colorRow.getChildren().addAll(redButton, yellowButton, greenButton, blueButton);

        Label playerLabel = new Label("Pick a player to equalize");
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
            if (onFinish != null && selectedPlayer != null && selectedColor != null) {
                onFinish.accept(selectedPlayer, selectedColor);
            }
        });

        panel.getChildren().addAll(
                title,
                subtitle,
                colorRow,
                playerLabel,
                playerList,
                doneButton
        );

        getChildren().add(panel);
    }

    public void setOnFinish(BiConsumer<String, CardColor> onFinish) {
        this.onFinish = onFinish;
    }

    private Button createColorButton(CardColor color, String extraStyleClass) {
        Button button = new Button();
        button.getStyleClass().addAll(
                "frantic-button",
                "effect-response-choice-button",
                extraStyleClass
        );

        button.setOnAction(e -> {
            selectedColor = color;
            updateDoneState();
        });

        return button;
    }

    private void updateDoneState() {
        doneButton.setDisable(selectedColor == null || selectedPlayer == null);
    }
}