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
    private final ListView<String> leaderboardList;

    private final Button leaveLobbyButton;

    private final VBox centerBox;
    private final HBox buttonBox;
    private final HBox listsBox;
    private final VBox localBox;
    private final VBox globalBox;

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

        leaderboardList = new ListView<>();
        leaderboardList.setItems(FXCollections.observableArrayList());
        leaderboardList.getStyleClass().add("frantic-list-view");
        leaderboardList.setPlaceholder(new Label("No stored high scores yet"));

        leaveLobbyButton = new Button("Leave Lobby");
        leaveLobbyButton.getStyleClass().add("danger-button");

        buttonBox = new HBox(10, leaveLobbyButton);
        buttonBox.getStyleClass().add("button-box");

        localBox = new VBox(
                10,
                winnerLabel,
                new Label("This Game"),
                rankingList
        );

        globalBox = new VBox(
                10,
                new Label("Global Leaderboard"),
                leaderboardList
        );

        listsBox = new HBox(20, localBox, globalBox);

        centerBox = new VBox(
                15,
                titleLabel,
                listsBox,
                buttonBox
        );

        centerBox.setFillWidth(false);
        centerBox.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        centerBox.setAlignment(javafx.geometry.Pos.CENTER);

        centerBox.getStyleClass().add("center-box");

        setCenter(centerBox);
    }

    public void setWinner(String winnerName) {
        winnerLabel.setText("🏆 Winner: " + winnerName);
    }

    public void setRanking(java.util.List<String> players) {
        rankingList.setItems(FXCollections.observableArrayList(players));
    }

    public void setLeaderboard(java.util.List<String> rows) {
        leaderboardList.setItems(FXCollections.observableArrayList(rows));
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

    public ListView<String> getLeaderboardList() {
        return leaderboardList;
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