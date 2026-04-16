package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * UI shown when the game ends.
 */
public class GameEndView extends BorderPane {

    private final Label titleLabel;
    private final Label winnerLabel;

    private final ListView<String> rankingList;


    private final Button leaveLobbyButton;

    private final VBox centerBox;
    private final HBox buttonBox;

    public GameEndView() {

        // Root styles
        getStyleClass().addAll("screen", "game-end-screen");

        // Title
        titleLabel = new Label("🏁 Game Over");
        titleLabel.getStyleClass().add("title-label");

        // Winner highlight
        winnerLabel = new Label("🏆 Winner: -");
        winnerLabel.getStyleClass().add("winner-label");

        // Ranking list (scrollbar automatisch dabei)
        rankingList = new ListView<>();
        rankingList.setItems(FXCollections.observableArrayList());
        rankingList.getStyleClass().add("frantic-list-view");

        rankingList.setPlaceholder(new Label("No results yet"));

        // Custom cell factory → Gewinner hervorheben
        rankingList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("winner-cell");
                } else {
                    setText(item);

                    // Einfach: erster Platz = Gewinner
                    if (getIndex() == 0) {
                        if (!getStyleClass().contains("winner-cell")) {
                            getStyleClass().add("winner-cell");
                        }
                    } else {
                        getStyleClass().remove("winner-cell");
                    }
                }
            }
        });

        // Buttons

        leaveLobbyButton = new Button("Leave Lobby");


        leaveLobbyButton.getStyleClass().add("danger-button");

        buttonBox = new HBox(10, leaveLobbyButton);
        buttonBox.getStyleClass().add("button-box");

        // Layout
        centerBox = new VBox(15,
                titleLabel,
                winnerLabel,
                new Label("Ranking"),
                rankingList,
                buttonBox
        );

        centerBox.getStyleClass().add("center-box");

        setCenter(centerBox);
    }

    // ===== Public API =====

    public void setWinner(String winnerName) {
        winnerLabel.setText("🏆 Winner: " + winnerName);
    }

    public void setRanking(java.util.List<String> players) {
        rankingList.setItems(FXCollections.observableArrayList(players));
    }

    // ===== Getters (für Tests) =====

    public Label getTitleLabel() {
        return titleLabel;
    }

    public Label getWinnerLabel() {
        return winnerLabel;
    }

    public ListView<String> getRankingList() {
        return rankingList;
    }

    public Button getLeaveLobbyButton() {
        return leaveLobbyButton;
    }

    public VBox getCenterBox() {
        return centerBox;
    }

    public HBox getButtonBox() {
        return buttonBox;
    }
}
