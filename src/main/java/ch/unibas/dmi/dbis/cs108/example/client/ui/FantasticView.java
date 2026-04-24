package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;
/**
 * Overlay view used to resolve the Fantastic effect in the GUI.
 *
 * <p>This dialog allows the acting player to choose either a color or a
 * number for the next requested play. It is displayed on top of the current
 * {@link GameView} and sends the result as a structured protocol response.</p>
 */
public class FantasticView extends StackPane {

    private CardColor selectedColor;
    private Integer selectedNumber;

    private final Button doneButton = new Button("Done");

    private BiConsumer<CardColor, Integer> onFinish;

    public FantasticView() {
        getStyleClass().add("effect-response-overlay");

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Fantastic");
        title.getStyleClass().add("effect-response-title");

        Label subtitle = new Label("Choose a color or a number");
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

        Label orLabel = new Label("OR");
        orLabel.getStyleClass().add("effect-response-subtitle");

        FlowPane numberGrid = new FlowPane();
        numberGrid.setAlignment(Pos.CENTER);
        numberGrid.setHgap(18);
        numberGrid.setVgap(18);
        numberGrid.setPrefWrapLength(360);
        numberGrid.getStyleClass().add("effect-response-number-grid");

        for (int i = 1; i <= 9; i++) {
            int number = i;
            Button button = createNumberButton(number);
            numberGrid.getChildren().add(button);
        }

        doneButton.getStyleClass().addAll("frantic-button", "primary-button");
        doneButton.setDisable(true);
        doneButton.setOnAction(e -> {
            if (onFinish != null) {
                onFinish.accept(selectedColor, selectedNumber);
            }
        });

        panel.getChildren().addAll(
                title,
                subtitle,
                colorRow,
                orLabel,
                numberGrid,
                doneButton
        );

        getChildren().add(panel);
    }

    public void setOnFinish(BiConsumer<CardColor, Integer> onFinish) {
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
            selectedNumber = null;
            updateDoneState();
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
            updateDoneState();
        });

        return button;
    }

    private void updateDoneState() {
        doneButton.setDisable(selectedColor == null && selectedNumber == null);
    }
}