package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LobbyView extends BorderPane {

    private final ListView<String> playersList = new ListView<>();
    private final ListView<String> chatList = new ListView<>();
    private final ListView<String> infoList = new ListView<>();

    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");
    private final Button refreshPlayersButton = new Button("Refresh Players");
    private final Button startButton = new Button("Start Game");
    private final Button disconnectButton = new Button("Disconnect");

    public LobbyView() {
        setPadding(new Insets(12));

        VBox left = new VBox(10, new Label("Players"), playersList, refreshPlayersButton);
        left.setPrefWidth(220);

        VBox center = new VBox(10, new Label("Chat"), chatList);
        HBox chatBox = new HBox(10, chatInput, sendButton);
        VBox chatSection = new VBox(10, center, chatBox);
        VBox.setVgrow(center, Priority.ALWAYS);
        VBox.setVgrow(chatList, Priority.ALWAYS);

        VBox right = new VBox(10, new Label("Game / Info"), infoList, startButton, disconnectButton);
        right.setPrefWidth(280);

        setLeft(left);
        setCenter(chatSection);
        setRight(right);

        BorderPane.setMargin(left, new Insets(0, 10, 0, 0));
        BorderPane.setMargin(chatSection, new Insets(0, 10, 0, 0));

        chatInput.setPromptText("Type a message...");
    }

    public ListView<String> getPlayersList() { return playersList; }
    public ListView<String> getChatList() { return chatList; }
    public ListView<String> getInfoList() { return infoList; }

    public TextField getChatInput() { return chatInput; }
    public Button getSendButton() { return sendButton; }
    public Button getRefreshPlayersButton() { return refreshPlayersButton; }
    public Button getStartButton() { return startButton; }
    public Button getDisconnectButton() { return disconnectButton; }
}