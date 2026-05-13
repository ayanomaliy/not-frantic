package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

    private final ListView<String> lobbyPlayersList = new ListView<>();
    private final ListView<String> allPlayersList = new ListView<>();
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
    private final Button spectateLobbyButton = new Button("Spectate Lobby");
    private final Button createLobbyButton = new Button("Create New Lobby");
    private final Button leaveLobbyButton = new Button("Leave Lobby");

    private final Button chatModeButton = new Button("Global");

    private final Button nameButton = new Button("Change Name");
    private final Button settingsButton = new Button("Settings");
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
        configureLobbyListBehavior();

        VBox leftPanel = new VBox(12);
        leftPanel.getStyleClass().add("panel");
        leftPanel.setPrefWidth(260);

        Label playersTitle = createSectionTitle("Players");
        Label lobbyPlayersTitle = createSectionTitle("Lobby");
        Label allPlayersTitle = createSectionTitle("Global");

        lobbyPlayersList.setPrefHeight(180);
        allPlayersList.setPrefHeight(180);
        lobbiesList.setPrefHeight(160);

        leftPanel.getChildren().addAll(
                playersTitle,
                lobbyPlayersTitle,
                lobbyPlayersList,
                refreshPlayersButton,
                allPlayersTitle,
                allPlayersList,
                createSectionTitle("Lobbies"),
                lobbiesList,
                refreshLobbiesButton,
                createLobbyButton,
                joinLobbyButton,
                spectateLobbyButton,
                leaveLobbyButton
        );

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

        HBox lobbyActionButtons = new HBox(10, nameButton, settingsButton);
        lobbyActionButtons.setAlignment(Pos.CENTER_LEFT);

        VBox rightPanel = new VBox(
                12,
                createSectionTitle("Game / Info"),
                infoList,
                createSectionTitle("Command"),
                commandBox,
                startButton,
                lobbyActionButtons,
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
        lobbyPlayersList.getStyleClass().add("frantic-list-view");
        allPlayersList.getStyleClass().add("frantic-list-view");
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
        leaveLobbyButton.getStyleClass().addAll("frantic-button", "danger-button");

        spectateLobbyButton.getStyleClass().addAll("frantic-button", "secondary-button");
        spectateLobbyButton.setDisable(true);

        chatModeButton.getStyleClass().addAll("frantic-button", "secondary-button");

        nameButton.getStyleClass().addAll("frantic-button", "secondary-button");
        settingsButton.getStyleClass().addAll("frantic-button", "secondary-button");

        joinLobbyButton.setDisable(true);
    }

    /**
     * Configures the lobby list appearance and join-button behavior.
     *
     * <p>Expected lobby entry format:
     * {@code LobbyName | current/max | STATUS}</p>
     *
     * <p>Examples:
     * {@code Lobby-1 | 3/5 | WAITING}
     * {@code Lobby-2 | 5/5 | WAITING}
     * {@code Lobby-3 | 4/5 | PLAYING}</p>
     */
    private void configureLobbyListBehavior() {
        lobbiesList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                boolean full = isLobbyFull(item);
                String status = extractLobbyStatus(item);

                if ("PLAYING".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
                } else if ("FINISHED".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                } else if (full) {
                    setStyle("-fx-text-fill: #f0ad4e; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #2e8b57; -fx-font-weight: bold;");
                }
            }
        });

        lobbiesList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            joinLobbyButton.setDisable(!isJoinableLobbyEntry(newValue));
            spectateLobbyButton.setDisable(!isSpectatableLobbyEntry(newValue));
        });
    }

    public boolean isSpectatableLobbyEntry(String lobbyEntry) {
        if (lobbyEntry == null || lobbyEntry.isBlank()) {
            return false;
        }

        String status = extractLobbyStatus(lobbyEntry);
        return "PLAYING".equalsIgnoreCase(status);
    }

    public Button getSpectateLobbyButton() {
        return spectateLobbyButton;
    }

    /**
     * Returns whether the given lobby entry can be joined.
     *
     * <p>A lobby is joinable only if it is not full and not in PLAYING state.</p>
     *
     * @param lobbyEntry the visible lobby entry text
     * @return {@code true} if the lobby can be joined, otherwise {@code false}
     */
    public boolean isJoinableLobbyEntry(String lobbyEntry) {
        if (lobbyEntry == null || lobbyEntry.isBlank()) {
            return false;
        }

        String status = extractLobbyStatus(lobbyEntry);
        return !"PLAYING".equalsIgnoreCase(status) && !isLobbyFull(lobbyEntry);
    }

    /**
     * Returns whether the currently selected lobby can be joined.
     *
     * @return {@code true} if the selected lobby is joinable, otherwise {@code false}
     */
    public boolean isSelectedLobbyJoinable() {
        return isJoinableLobbyEntry(lobbiesList.getSelectionModel().getSelectedItem());
    }

    /**
     * Extracts the status token from a lobby list entry.
     *
     * @param lobbyEntry the lobby entry text
     * @return the extracted status, or an empty string if no status is found
     */
    private String extractLobbyStatus(String lobbyEntry) {
        if (lobbyEntry == null || lobbyEntry.isBlank()) {
            return "";
        }

        int start = lobbyEntry.indexOf("(");
        int end = lobbyEntry.indexOf(")");

        if (start >= 0 && end > start) {
            return lobbyEntry.substring(start + 1, end).trim();
        }

        return "";
    }

    /**
     * Returns whether the lobby entry represents a full lobby.
     *
     * @param lobbyEntry the lobby entry text
     * @return {@code true} if the lobby is full, otherwise {@code false}
     */
    private boolean isLobbyFull(String lobbyEntry) {
        if (lobbyEntry == null || lobbyEntry.isBlank()) {
            return false;
        }

        String[] parts = lobbyEntry.trim().split("\\s+");
        if (parts.length < 3) {
            return false;
        }

        String countPart = parts[parts.length - 1];
        String[] counts = countPart.split("/");

        if (counts.length != 2) {
            return false;
        }

        try {
            int currentPlayers = Integer.parseInt(counts[0].trim());
            int maxPlayers = Integer.parseInt(counts[1].trim());
            return currentPlayers >= maxPlayers;
        } catch (NumberFormatException exception) {
            return false;
        }
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
     * Returns the list view showing players in the current lobby.
     *
     * @return the lobby players list
     */
    public ListView<String> getLobbyPlayersList() {
        return lobbyPlayersList;
    }

    /**
     * Returns the list view showing all connected players on the server.
     *
     * @return the global players list
     */
    public ListView<String> getAllPlayersList() {
        return allPlayersList;
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

    /**
     * Returns the button used to leave the current lobby.
     *
     * @return the leave lobby button
     */
    public Button getLeaveLobbyButton() {
        return leaveLobbyButton;
    }

    /**
     * Returns the button used to change the name
     *
     * @return the change name button
     */
    public Button getNameButton() {
        return nameButton;
    }

    /**
     * Returns the button used to open settings
     *
     * @return the settings button
     */
    public Button getSettingsButton() {
        return settingsButton;
    }
}
