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

    private final Slider effectsVolumeSlider = new Slider(0, 100, 75);
    private final Label effectsVolumeValueLabel = new Label("75%");

    /**
     * Creates the settings overlay.
     *
     * @param initialMusicVolume initial music volume between 0.0 and 1.0
     * @param initialEffectsVolume initial sound-effects volume between 0.0 and 1.0
     */
    public SettingsView(double initialMusicVolume, double initialEffectsVolume) {
        setId("settings-overlay");
        getStyleClass().add("settings-overlay");
        setAlignment(Pos.CENTER);
        setPickOnBounds(true);

        double musicPercent = Math.round(clamp(initialMusicVolume) * 100.0);
        musicVolumeSlider.setValue(musicPercent);
        musicVolumeValueLabel.setText((int) musicPercent + "%");

        double effectsPercent = Math.round(clamp(initialEffectsVolume) * 100.0);
        effectsVolumeSlider.setValue(effectsPercent);
        effectsVolumeValueLabel.setText((int) effectsPercent + "%");

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
                createVolumeRow(
                        "Music volume",
                        "Controls the looping background music.",
                        musicVolumeSlider,
                        musicVolumeValueLabel
                ),
                createVolumeRow(
                        "Sound effects volume",
                        "Controls card sounds, draw sounds, and event sounds.",
                        effectsVolumeSlider,
                        effectsVolumeValueLabel
                ),
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

    private HBox createVolumeRow(
            String titleText,
            String descriptionText,
            Slider slider,
            Label valueLabel
    ) {
        VBox textBox = new VBox(4);

        Label title = new Label(titleText);
        title.getStyleClass().add("settings-row-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("settings-row-description");
        description.setWrapText(true);

        textBox.getChildren().addAll(title, description);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        slider.getStyleClass().add("settings-volume-slider");
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setBlockIncrement(5);

        slider.setMinWidth(180);
        slider.setPrefWidth(180);
        slider.setMaxWidth(180);
        HBox.setHgrow(slider, Priority.NEVER);

        StackPane sliderBox = new StackPane(slider);
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

        valueLabel.getStyleClass().add("settings-volume-value");
        valueLabel.setMinWidth(52);
        valueLabel.setPrefWidth(52);
        valueLabel.setMaxWidth(52);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        slider.valueProperty().addListener((obs, oldValue, newValue) ->
                valueLabel.setText(Math.round(newValue.doubleValue()) + "%")
        );

        HBox controlBox = new HBox(12, sliderBox, valueLabel);
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
        checkBox.getStyleClass().add("settings-checkbox");

        HBox row = new HBox(16, textBox, checkBox);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    public Slider getMusicVolumeSlider() {
        return musicVolumeSlider;
    }

    public Slider getEffectsVolumeSlider() {
        return effectsVolumeSlider;
    }

    public Button getOkButton() {
        return okButton;
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
}