package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Overlay view used to resolve the Counterattack effect in the GUI.
 *
 * <p>This dialog lets the affected player choose the follow-up color and
 * submit the selection without relying on manual slash commands. It is shown
 * on top of the current {@link GameView} as a modal-style effect response
 * panel.</p>
 */
public class CounterattackView extends StackPane {

    private CardColor selectedColor;
    private final Button doneButton = new Button("Done");

    private Consumer<CardColor> onFinish;

    public CounterattackView() {
        getStyleClass().add("effect-response-overlay");

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel", "effect-response-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Counterattack");
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

        doneButton.getStyleClass().addAll("frantic-button", "primary-button");
        doneButton.setDisable(true);
        doneButton.setOnAction(e -> {
            if (onFinish != null && selectedColor != null) {
                onFinish.accept(selectedColor);
            }
        });

        panel.getChildren().addAll(title, subtitle, colorRow, doneButton);
        getChildren().add(panel);
    }

    public void setOnFinish(Consumer<CardColor> onFinish) {
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
            doneButton.setDisable(false);
        });

        return button;
    }
}