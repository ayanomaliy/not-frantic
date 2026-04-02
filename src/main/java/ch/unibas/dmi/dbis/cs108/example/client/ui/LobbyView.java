package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Lobby view for the Frantic^-1 GUI client.
 *
 * <p>This screen shows the connected players, chat, informational messages,
 * and client controls such as start, disconnect, and slash-style command
 * execution. The visual appearance is controlled through JavaFX CSS.</p>
 */
public class LobbyView extends BorderPane {

    private final ListView<String> playersList = new ListView<>();
    private final ListView<String> chatList = new ListView<>();
    private final ListView<String> infoList = new ListView<>();

    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");

    private final TextField commandInput = new TextField();
    private final Button commandButton = new Button("Run");

    private final Button refreshPlayersButton = new Button("Refresh Players");
    private final Button startButton = new Button("Start Game");
    private final Button disconnectButton = new Button("Disconnect");

    private final ListView<String> lobbiesList = new ListView<>();
    private final Button refreshLobbiesButton = new Button("Refresh Lobbies");
    private final Button joinLobbyButton = new Button("Join Selected Lobby");
    private final Button createLobbyButton = new Button("Create New Lobby");

    private final Button chatModeButton = new Button("Global");

    /**
     * Creates the lobby view.
     *
     * <p>This screen contains the player list, lobby list, chat area,
     * informational messages, and controls for lobby and game actions.</p>
     */
    public LobbyView() {
        getStyleClass().addAll("screen", "lobby-screen");
        setPadding(new Insets(16));

        configureControls();

        VBox leftPanel = new VBox(
                12,
                createSectionTitle("Players"),
                playersList,
                refreshPlayersButton,
                createSectionTitle("Lobbies"),
                lobbiesList,
                refreshLobbiesButton,
                joinLobbyButton,
                createLobbyButton
        );
        leftPanel.getStyleClass().add("panel");
        leftPanel.setPrefWidth(260);

        VBox.setVgrow(playersList, Priority.ALWAYS);
        lobbiesList.setPrefHeight(180);


        VBox.setVgrow(playersList, Priority.ALWAYS);

        HBox chatBox = new HBox(10, chatInput, sendButton);
        chatBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        VBox centerPanel = new VBox(
                12,
                createChatHeader(),
                chatList,
                chatBox
        );

        centerPanel.getStyleClass().add("panel");
        VBox.setVgrow(chatList, Priority.ALWAYS);

        HBox commandBox = new HBox(10, commandInput, commandButton);
        commandBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        VBox rightPanel = new VBox(
                12,
                createSectionTitle("Game / Info"),
                infoList,
                createSectionTitle("Command"),
                commandBox,
                startButton,
                disconnectButton
        );
        rightPanel.getStyleClass().add("panel");
        rightPanel.setPrefWidth(310);
        VBox.setVgrow(infoList, Priority.ALWAYS);

        setLeft(leftPanel);
        setCenter(centerPanel);
        setRight(rightPanel);

        BorderPane.setMargin(leftPanel, new Insets(0, 12, 0, 0));
        BorderPane.setMargin(centerPanel, new Insets(0, 12, 0, 0));
    }

    /**
     * Applies CSS style classes and prompt texts to the controls of this view.
     */
    private void configureControls() {
        playersList.getStyleClass().add("frantic-list-view");
        chatList.getStyleClass().add("frantic-list-view");
        infoList.getStyleClass().add("frantic-list-view");

        chatInput.getStyleClass().add("frantic-text-field");
        commandInput.getStyleClass().add("frantic-text-field");

        sendButton.getStyleClass().addAll("frantic-button", "primary-button");
        commandButton.getStyleClass().addAll("frantic-button", "secondary-button");
        refreshPlayersButton.getStyleClass().addAll("frantic-button", "secondary-button");
        startButton.getStyleClass().addAll("frantic-button", "primary-button");
        disconnectButton.getStyleClass().addAll("frantic-button", "danger-button");

        chatInput.setPromptText("Type a message...");
        commandInput.setPromptText("/name Alice, /lobbies, /create Lobby1, /join Lobby1, /players, /start, /quit");

        lobbiesList.getStyleClass().add("frantic-list-view");

        refreshLobbiesButton.getStyleClass().addAll("frantic-button", "secondary-button");
        joinLobbyButton.getStyleClass().addAll("frantic-button", "primary-button");
        createLobbyButton.getStyleClass().addAll("frantic-button", "primary-button");

        chatModeButton.getStyleClass().addAll("frantic-button", "secondary-button");
    }

    /**
     * Creates a styled section title label.
     *
     * @param text the visible title text
     * @return the configured label
     */
    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * Creates the header row for the chat section.
     *
     * @return the configured chat header row
     */
    private HBox createChatHeader() {
        Label title = createSectionTitle("Chat");
        HBox header = new HBox(10, title, chatModeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Returns the players list view.
     *
     * @return the players list
     */
    public ListView<String> getPlayersList() {
        return playersList;
    }

    /**
     * Returns the chat list view.
     *
     * @return the chat list
     */
    public ListView<String> getChatList() {
        return chatList;
    }

    /**
     * Returns the game/info list view.
     *
     * @return the info list
     */
    public ListView<String> getInfoList() {
        return infoList;
    }

    /**
     * Returns the chat input field.
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
     * Returns the command input field.
     *
     * @return the command input field
     */
    public TextField getCommandInput() {
        return commandInput;
    }

    /**
     * Returns the button used to execute terminal-style commands.
     *
     * @return the command button
     */
    public Button getCommandButton() {
        return commandButton;
    }

    /**
     * Returns the button used to refresh the player list manually.
     *
     * @return the refresh button
     */
    public Button getRefreshPlayersButton() {
        return refreshPlayersButton;
    }

    /**
     * Returns the button used to start the game.
     *
     * @return the start button
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
     * Returns the list view showing the available lobbies.
     *
     * @return the lobby list view
     */
    public ListView<String> getLobbiesList() {
        return lobbiesList;
    }

    /**
     * Returns the button used to refresh the lobby list manually.
     *
     * @return the refresh lobbies button
     */
    public Button getRefreshLobbiesButton() {
        return refreshLobbiesButton;
    }

    /**
     * Returns the button used to join the currently selected lobby.
     *
     * @return the join lobby button
     */
    public Button getJoinLobbyButton() {
        return joinLobbyButton;
    }

    /**
     * Returns the button used to create a new lobby.
     *
     * @return the create lobby button
     */
    public Button getCreateLobbyButton() {
        return createLobbyButton;
    }

    /**
     * Returns the button used to toggle between global and lobby chat mode.
     *
     * @return the chat mode toggle button
     */
    public Button getChatModeButton() {
        return chatModeButton;
    }
}