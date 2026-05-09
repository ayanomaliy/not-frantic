package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeInUp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Modal settings overlay for the game screen.
 *
 * <p>This view is intentionally separate from {@link GameView} so new settings
 * can be added later without making the main game layout larger and messier.</p>
 */
public class SettingsView extends StackPane {

    private final VBox panel = new VBox(18);
    private final VBox settingsContent = new VBox(12);
    private final Button okButton = new Button("OK");

    /**
     * Creates the settings overlay.
     */
    public SettingsView() {
        setId("settings-overlay");
        getStyleClass().add("settings-overlay");
        setAlignment(Pos.CENTER);
        setPickOnBounds(true);

        buildPanel();

        getChildren().add(panel);
        new FadeInUp(panel).play();
    }

    private void buildPanel() {
        panel.getStyleClass().add("settings-panel");
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setMinWidth(520);
        panel.setPrefWidth(560);
        panel.setMaxWidth(620);
        panel.setMinHeight(360);
        panel.setPadding(new Insets(28));

        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");

        Label subtitle = new Label("Adjust local client settings. More options can be added here later.");
        subtitle.getStyleClass().add("settings-subtitle");
        subtitle.setWrapText(true);

        settingsContent.getStyleClass().add("settings-content");

        // Placeholder examples. They do not change anything yet.
        settingsContent.getChildren().addAll(
                createToggleRow("Mute sound effects", "Temporarily disables local card and event sounds."),
                createToggleRow("Reduce animations", "Useful if animations feel distracting or slow."),
                createToggleRow("Compact sidebar", "Reserved for a later layout setting.")
        );

        okButton.getStyleClass().addAll("frantic-button", "primary-button");

        HBox buttonRow = new HBox(okButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));

        panel.getChildren().addAll(
                title,
                subtitle,
                new Separator(),
                settingsContent,
                buttonRow
        );
    }

    private HBox createToggleRow(String titleText, String descriptionText) {
        VBox textBox = new VBox(4);

        Label title = new Label(titleText);
        title.getStyleClass().add("settings-row-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("settings-row-description");
        description.setWrapText(true);

        textBox.getChildren().addAll(title, description);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        CheckBox checkBox = new CheckBox();
        checkBox.setDisable(true);

        HBox row = new HBox(16, textBox, checkBox);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    public Button getOkButton() {
        return okButton;
    }

    public VBox getSettingsContent() {
        return settingsContent;
    }
}