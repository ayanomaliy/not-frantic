package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/**
 * Multi-step overlay view used to resolve the Fantastic Four effect in the GUI.
 *
 * <p>This dialog first lets the player choose either a color or a number and
 * then assign the four resulting cards to target players. It is shown as a
 * modal-style overlay on top of the {@link GameView} so the full effect can
 * be resolved graphically instead of through text commands.</p>
 */
public class FantasticFourView extends StackPane {

    public static final class Result {
        private final CardColor color;
        private final Integer number;
        private final List<String> targetPlayers;

        public Result(CardColor color, Integer number, List<String> targetPlayers) {
            this.color = color;
            this.number = number;
            this.targetPlayers = List.copyOf(targetPlayers);
        }

        public CardColor getColor() {
            return color;
        }

        public Integer getNumber() {
            return number;
        }

        public List<String> getTargetPlayers() {
            return targetPlayers;
        }
    }

    private int step = 1;

    private CardColor selectedColor;
    private Integer selectedNumber;

    private final List<String> assignedTargets = new ArrayList<>();

    private final VBox panel = new VBox(18);
    private final Button mainButton = new Button("Next");

    private final List<String> players;

    private Consumer<Result> onFinish;

    public FantasticFourView(List<String> players) {
        this.players = List.copyOf(players);

        getStyleClass().add("effect-response-overlay");

        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        mainButton.getStyleClass().addAll("frantic-button", "primary-button");

        renderStep1();

        getChildren().add(panel);
    }

    public void setOnFinish(Consumer<Result> onFinish) {
        this.onFinish = onFinish;
    }

    private void renderStep1() {
        panel.getChildren().clear();
        step = 1;

        Label title = new Label("Fantastic Four");
        title.getStyleClass().add("effect-response-title");

        Label subtitle = new Label("Choose a color or a number");
        subtitle.getStyleClass().add("effect-response-subtitle");

        FlowPane colorRow = new FlowPane();
        colorRow.setAlignment(Pos.CENTER);
        colorRow.setHgap(18);
        colorRow.getStyleClass().add("effect-response-choice-row");

        colorRow.getChildren().addAll(
                createColorButton(CardColor.RED, "effect-response-color-red"),
                createColorButton(CardColor.YELLOW, "effect-response-color-yellow"),
                createColorButton(CardColor.GREEN, "effect-response-color-green"),
                createColorButton(CardColor.BLUE, "effect-response-color-blue")
        );

        Label orLabel = new Label("OR");
        orLabel.getStyleClass().add("effect-response-subtitle");

        FlowPane numberGrid = new FlowPane();
        numberGrid.setAlignment(Pos.CENTER);
        numberGrid.setHgap(18);
        numberGrid.setVgap(18);
        numberGrid.setPrefWrapLength(360);
        numberGrid.getStyleClass().add("effect-response-number-grid");

        for (int i = 1; i <= 9; i++) {
            numberGrid.getChildren().add(createNumberButton(i));
        }

        mainButton.setText("Next");
        mainButton.setDisable(selectedColor == null && selectedNumber == null);
        mainButton.setOnAction(e -> renderStep2());

        panel.getChildren().addAll(title, subtitle, colorRow, orLabel, numberGrid, mainButton);
    }

    private void renderStep2() {
        panel.getChildren().clear();
        step = 2;

        Label title = new Label("Fantastic Four");
        title.getStyleClass().add("effect-response-title");

        Label subtitle = new Label("Distribute 4 cards");
        subtitle.getStyleClass().add("effect-response-subtitle");

        Label info = new Label("Click players to assign the next card");
        info.getStyleClass().add("effect-response-subtitle");

        HBox assignedBox = new HBox(12);
        assignedBox.setAlignment(Pos.CENTER);
        assignedBox.getStyleClass().add("effect-response-choice-row");

        for (int i = 0; i < 4; i++) {
            Button slot = new Button();
            slot.getStyleClass().addAll(
                    "frantic-button",
                    "secondary-button",
                    "effect-response-assigned-slot"
            );

            if (i < assignedTargets.size()) {
                slot.setText(assignedTargets.get(i));
                int index = i;
                slot.setOnAction(e -> {
                    assignedTargets.remove(index);
                    renderStep2();
                });
            } else {
                slot.setText("?");
                slot.setDisable(true);
            }

            assignedBox.getChildren().add(slot);
        }

        ListView<String> playerList = new ListView<>();
        playerList.setItems(FXCollections.observableArrayList(players));
        playerList.getStyleClass().addAll("frantic-list-view", "effect-response-player-list");
        playerList.setPrefHeight(180);
        playerList.setMaxWidth(280);

        playerList.setOnMouseClicked(e -> {
            String selected = playerList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                return;
            }

            if (!players.contains(selected)) {
                return;
            }

            if (assignedTargets.size() >= 4) {
                return;
            }

            assignedTargets.add(selected);
            renderStep2();
        });

        mainButton.setText("Done");
        mainButton.setDisable(assignedTargets.size() != 4);
        mainButton.setOnAction(e -> {
            if (onFinish != null && assignedTargets.size() == 4) {
                onFinish.accept(new Result(selectedColor, selectedNumber, assignedTargets));
            }
        });

        panel.getChildren().addAll(title, subtitle, info, assignedBox, playerList, mainButton);
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
            selectedNumber = null;
            updateStep1ButtonState();
        });

        return button;
    }

    private Button createNumberButton(int number) {
        Button button = new Button(String.valueOf(number));
        button.getStyleClass().addAll(
                "frantic-button",
                "secondary-button",
                "effect-response-choice-button"
        );

        button.setOnAction(e -> {
            selectedNumber = number;
            selectedColor = null;
            updateStep1ButtonState();
        });

        return button;
    }

    private void updateStep1ButtonState() {
        if (step == 1) {
            mainButton.setDisable(selectedColor == null && selectedNumber == null);
        }
    }
}