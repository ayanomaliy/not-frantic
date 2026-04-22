package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
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
        getStyleClass().addAll("screen", "game-end-screen");

        titleLabel = new Label("🏁 Game Over");
        titleLabel.getStyleClass().add("title-label");

        winnerLabel = new Label("🏆 Winner: -");
        winnerLabel.getStyleClass().add("winner-label");

        rankingList = new ListView<>();
        rankingList.setItems(FXCollections.observableArrayList());
        rankingList.getStyleClass().add("frantic-list-view");
        rankingList.setPlaceholder(new Label("No results yet"));

        rankingList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("winner-cell");
                } else {
                    setText(item);

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

        leaveLobbyButton = new Button("Leave Lobby");
        leaveLobbyButton.getStyleClass().add("danger-button");

        buttonBox = new HBox(10, leaveLobbyButton);
        buttonBox.getStyleClass().add("button-box");

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

    public void setWinner(String winnerName) {
        winnerLabel.setText("🏆 Winner: " + winnerName);
    }

    public void setRanking(java.util.List<String> players) {
        rankingList.setItems(FXCollections.observableArrayList(players));
    }

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