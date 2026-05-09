package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeInUp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

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

    private final Slider musicVolumeSlider = new Slider(0, 100, 35);
    private final Label musicVolumeValueLabel = new Label("35%");

    /**
     * Creates the settings overlay.
     *
     * @param initialMusicVolume initial music volume between 0.0 and 1.0
     */
    public SettingsView(double initialMusicVolume) {
        setId("settings-overlay");
        getStyleClass().add("settings-overlay");
        setAlignment(Pos.CENTER);
        setPickOnBounds(true);

        double percent = Math.round(clamp(initialMusicVolume) * 100.0);
        musicVolumeSlider.setValue(percent);
        musicVolumeValueLabel.setText((int) percent + "%");

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

        Label subtitle = new Label("Adjust local client settings.");
        subtitle.getStyleClass().add("settings-subtitle");
        subtitle.setWrapText(true);

        settingsContent.getStyleClass().add("settings-content");

        settingsContent.getChildren().addAll(
                createMusicVolumeRow(),
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

    private HBox createMusicVolumeRow() {
        VBox textBox = new VBox(4);

        Label title = new Label("Music volume");
        title.getStyleClass().add("settings-row-title");

        Label description = new Label("Controls the looping background music.");
        description.getStyleClass().add("settings-row-description");
        description.setWrapText(true);

        textBox.getChildren().addAll(title, description);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        musicVolumeSlider.getStyleClass().add("settings-volume-slider");
        musicVolumeSlider.setShowTickMarks(false);
        musicVolumeSlider.setShowTickLabels(false);
        musicVolumeSlider.setBlockIncrement(5);

        /*
         * Keep the actual slider small.
         */
        musicVolumeSlider.setMinWidth(180);
        musicVolumeSlider.setPrefWidth(180);
        musicVolumeSlider.setMaxWidth(180);
        HBox.setHgrow(musicVolumeSlider, Priority.NEVER);

        /*
         * Important:
         * The wrapper clips the slider skin so the track cannot visually escape
         * across the whole settings overlay / screen.
         */
        StackPane sliderBox = new StackPane(musicVolumeSlider);
        sliderBox.getStyleClass().add("settings-slider-box");
        sliderBox.setMinWidth(190);
        sliderBox.setPrefWidth(190);
        sliderBox.setMaxWidth(190);
        sliderBox.setMinHeight(28);
        sliderBox.setPrefHeight(28);
        sliderBox.setMaxHeight(28);
        HBox.setHgrow(sliderBox, Priority.NEVER);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sliderBox.widthProperty());
        clip.heightProperty().bind(sliderBox.heightProperty());
        sliderBox.setClip(clip);

        musicVolumeValueLabel.getStyleClass().add("settings-volume-value");
        musicVolumeValueLabel.setMinWidth(52);
        musicVolumeValueLabel.setPrefWidth(52);
        musicVolumeValueLabel.setMaxWidth(52);
        musicVolumeValueLabel.setAlignment(Pos.CENTER_RIGHT);

        musicVolumeSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                musicVolumeValueLabel.setText(Math.round(newValue.doubleValue()) + "%")
        );

        HBox controlBox = new HBox(12, sliderBox, musicVolumeValueLabel);
        controlBox.setAlignment(Pos.CENTER_RIGHT);
        controlBox.setMinWidth(260);
        controlBox.setPrefWidth(260);
        controlBox.setMaxWidth(260);
        HBox.setHgrow(controlBox, Priority.NEVER);

        HBox row = new HBox(16, textBox, controlBox);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFillHeight(false);

        return row;
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

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }

        if (value > 1.0) {
            return 1.0;
        }

        return value;
    }

    public Button getOkButton() {
        return okButton;
    }

    public VBox getSettingsContent() {
        return settingsContent;
    }

    public Slider getMusicVolumeSlider() {
        return musicVolumeSlider;
    }
}