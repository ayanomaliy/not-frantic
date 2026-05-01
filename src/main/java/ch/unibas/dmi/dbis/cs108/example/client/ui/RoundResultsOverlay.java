package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Semi-transparent overlay shown at the end of each round.
 *
 * <p>Displays the per-player round scores and cumulative totals and provides
 * two action buttons: one to leave the lobby and one to start the next round.
 * The overlay fades in on creation and fades out when {@link #dismiss(Runnable)}
 * is called.</p>
 */
public class RoundResultsOverlay extends StackPane {

    /**
     * Creates the overlay.
     *
     * @param scoreRows        observable list of formatted score strings (player | Round: N | Total: M)
     * @param onStartNextRound callback invoked when the user clicks "Next Round"
     * @param onLeaveLobby     callback invoked when the user clicks "Leave Lobby"
     */
    public RoundResultsOverlay(ObservableList<String> scoreRows,
                               Runnable onStartNextRound,
                               Runnable onLeaveLobby) {
        getStyleClass().add("round-results-overlay");
        setAlignment(Pos.CENTER);
        setMouseTransparent(false);

        Label title = new Label("Round Complete!");
        title.getStyleClass().add("round-results-title");
        title.setWrapText(true);
        title.setAlignment(Pos.CENTER);

        ListView<String> scoreList = new ListView<>(scoreRows);
        scoreList.getStyleClass().add("frantic-list-view");
        scoreList.setPrefHeight(Math.min(scoreRows.size() * 34 + 12, 180));
        scoreList.setMouseTransparent(true);
        scoreList.setFocusTraversable(false);

        Button leaveButton = new Button("Leave Lobby");
        leaveButton.getStyleClass().addAll("frantic-button", "danger-button");
        leaveButton.setOnAction(e -> {
            if (onLeaveLobby != null) {
                onLeaveLobby.run();
            }
        });

        Button nextRoundButton = new Button("Next Round");
        nextRoundButton.getStyleClass().addAll("frantic-button", "primary-button");
        nextRoundButton.setOnAction(e -> {
            if (onStartNextRound != null) {
                onStartNextRound.run();
            }
        });

        HBox buttonRow = new HBox(16, leaveButton, nextRoundButton);
        buttonRow.setAlignment(Pos.CENTER);

        VBox panel = new VBox(20, title, scoreList, buttonRow);
        panel.getStyleClass().add("round-results-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(36, 48, 36, 48));
        panel.setMaxWidth(500);

        getChildren().add(panel);

        setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Fades the overlay out and then runs {@code onDone}.
     *
     * @param onDone callback invoked after the fade completes (use to remove from parent)
     */
    public void dismiss(Runnable onDone) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), this);
        fadeOut.setFromValue(getOpacity());
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (onDone != null) {
                onDone.run();
            }
        });
        fadeOut.play();
    }
}
