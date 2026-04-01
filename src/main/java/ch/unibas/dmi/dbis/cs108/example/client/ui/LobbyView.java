package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * JavaFX view representing the lobby screen of the Frantic^-1 client.
 *
 * <p>This view displays the list of connected players, the chat area,
 * and a panel for game-related information and controls. It is shown
 * after a successful connection to the server.</p>
 */
public class LobbyView extends BorderPane {

    /** List view displaying all currently connected players. */
    private final ListView<String> playersList = new ListView<>();

    /** List view displaying chat messages exchanged in the lobby. */
    private final ListView<String> chatList = new ListView<>();

    /** List view displaying informational and game-related messages. */
    private final ListView<String> infoList = new ListView<>();

    /** Text field for entering a chat message. */
    private final TextField chatInput = new TextField();

    /** Button for sending the current chat message. */
    private final Button sendButton = new Button("Send");

    /** Button for manually requesting the current player list from the server. */
    private final Button refreshPlayersButton = new Button("Refresh Players");

    /** Button for sending a game start request to the server. */
    private final Button startButton = new Button("Start Game");

    /** Button for disconnecting from the server and returning to the connect screen. */
    private final Button disconnectButton = new Button("Disconnect");

    private final TextField commandInput = new TextField();
    private final Button commandButton = new Button("Run");

    /**
     * Creates the lobby view and initializes its layout and controls.
     *
     * <p>The layout consists of three main areas: a player list on the left,
     * a chat section in the center, and a game/info panel on the right.</p>
     */
    public LobbyView() {
        setPadding(new Insets(12));

        VBox left = new VBox(10, new Label("Players"), playersList, refreshPlayersButton);
        left.setPrefWidth(220);

        VBox center = new VBox(10, new Label("Chat"), chatList);
        HBox chatBox = new HBox(10, chatInput, sendButton);
        VBox chatSection = new VBox(10, center, chatBox);
        VBox.setVgrow(center, Priority.ALWAYS);
        VBox.setVgrow(chatList, Priority.ALWAYS);

        HBox commandBox = new HBox(10, commandInput, commandButton);
        VBox right = new VBox(
                10,
                new Label("Game / Info"),
                infoList,
                new Label("Command"),
                commandBox,
                startButton,
                disconnectButton
        );
        right.setPrefWidth(280);

        setLeft(left);
        setCenter(chatSection);
        setRight(right);

        BorderPane.setMargin(left, new Insets(0, 10, 0, 0));
        BorderPane.setMargin(chatSection, new Insets(0, 10, 0, 0));

        chatInput.setPromptText("Type a message...");
        commandInput.setPromptText("/name Alice, /players, /start, /quit");
    }

    /**
     * Returns the list view displaying connected players.
     *
     * @return the player list view
     */
    public ListView<String> getPlayersList() {
        return playersList;
    }

    /**
     * Returns the list view displaying chat messages.
     *
     * @return the chat list view
     */
    public ListView<String> getChatList() {
        return chatList;
    }

    /**
     * Returns the list view displaying informational and game-related messages.
     *
     * @return the info list view
     */
    public ListView<String> getInfoList() {
        return infoList;
    }

    /**
     * Returns the text field used for entering chat messages.
     *
     * @return the chat input field
     */
    public TextField getChatInput() {
        return chatInput;
    }

    /**
     * Returns the button used to send chat messages.
     *
     * @return the send button
     */
    public Button getSendButton() {
        return sendButton;
    }

    /**
     * Returns the button used to refresh the player list manually.
     *
     * @return the refresh players button
     */
    public Button getRefreshPlayersButton() {
        return refreshPlayersButton;
    }

    /**
     * Returns the button used to request the start of the game.
     *
     * @return the start game button
     */
    public Button getStartButton() {
        return startButton;
    }

    /**
     * Returns the button used to disconnect from the server.
     *
     * @return the disconnect button
     */
    public Button getDisconnectButton() {
        return disconnectButton;
    }

    /**
     * Returns the text field used to enter terminal-style client commands.
     *
     * @return the command input field
     */
    public TextField getCommandInput() {
        return commandInput;
    }

    /**
     * Returns the button that submits the command entered in the command field.
     *
     * @return the command submit button
     */
    public Button getCommandButton() {
        return commandButton;
    }
}