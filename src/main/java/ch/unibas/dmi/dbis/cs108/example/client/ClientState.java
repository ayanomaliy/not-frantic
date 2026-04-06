package ch.unibas.dmi.dbis.cs108.example.client;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Shared observable state for the Frantic^-1 GUI client.
 *
 * <p>This class stores connection state, username, status text,
 * connected players, chat messages, and game-related messages in
 * JavaFX properties and observable lists so the GUI can react to
 * updates automatically.</p>
 */
public class ClientState {

    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty statusText = new SimpleStringProperty("Not connected");

    /** Current player whose turn it is. */
    private final StringProperty currentPlayer = new SimpleStringProperty("Unknown");

    /** Current phase of the game. */
    private final StringProperty currentPhase = new SimpleStringProperty("WAITING");

    /** Text representation of the current top discard card. */
    private final StringProperty topCardText = new SimpleStringProperty("-");

    /** Players in the currently joined lobby. */
    private final ObservableList<String> players = FXCollections.observableArrayList();

    /** All connected players on the server across all lobbies. */
    private final ObservableList<String> allPlayers = FXCollections.observableArrayList();

    /** Messages from the global chat channel. */
    private final ObservableList<String> globalChatMessages = FXCollections.observableArrayList();

    /** Messages from the current lobby chat channel. */
    private final ObservableList<String> lobbyChatMessages = FXCollections.observableArrayList();

    /** Private whisper messages. */
    private final ObservableList<String> whisperChatMessages = FXCollections.observableArrayList();

    /** Informational and game-related messages. */
    private final ObservableList<String> gameMessages = FXCollections.observableArrayList();

    /** Available lobby names on the server. */
    private final ObservableList<String> lobbies = FXCollections.observableArrayList();

    /** Currently selected chat mode in the GUI. */
    private final StringProperty chatMode = new SimpleStringProperty("Global");

    /**
     * Creates a new shared client state object.
     */
    public ClientState() {
    }

    /**
     * Returns the observable connection status property.
     *
     * @return the connected property
     */
    public BooleanProperty connectedProperty() {
        return connected;
    }

    /**
     * Returns the observable username property.
     *
     * @return the username property
     */
    public StringProperty usernameProperty() {
        return username;
    }

    /**
     * Returns the observable status text property.
     *
     * @return the status text property
     */
    public StringProperty statusTextProperty() {
        return statusText;
    }

    /**
     * Returns the observable current player property.
     *
     * @return the current player property
     */
    public StringProperty currentPlayerProperty() {
        return currentPlayer;
    }

    /**
     * Returns the observable current phase property.
     *
     * @return the current phase property
     */
    public StringProperty currentPhaseProperty() {
        return currentPhase;
    }

    /**
     * Returns the observable top card text property.
     *
     * @return the top card text property
     */
    public StringProperty topCardTextProperty() {
        return topCardText;
    }

    /**
     * Returns the observable list of players in the current lobby.
     *
     * @return the lobby player list
     */
    public ObservableList<String> getPlayers() {
        return players;
    }

    /**
     * Returns the observable list of all connected players on the server.
     *
     * @return the global player list
     */
    public ObservableList<String> getAllPlayers() {
        return allPlayers;
    }

    /**
     * Returns the observable list of global chat messages.
     *
     * @return the global chat message list
     */
    public ObservableList<String> getGlobalChatMessages() {
        return globalChatMessages;
    }

    /**
     * Returns the observable list of lobby chat messages.
     *
     * @return the lobby chat message list
     */
    public ObservableList<String> getLobbyChatMessages() {
        return lobbyChatMessages;
    }

    /**
     * Returns the observable list of whisper chat messages.
     *
     * @return the whisper chat message list
     */
    public ObservableList<String> getWhisperChatMessages() {
        return whisperChatMessages;
    }

    /**
     * Returns the observable list of game and informational messages.
     *
     * @return the game message list
     */
    public ObservableList<String> getGameMessages() {
        return gameMessages;
    }

    /**
     * Returns the observable list of available lobbies.
     *
     * @return the lobby list
     */
    public ObservableList<String> getLobbies() {
        return lobbies;
    }

    /**
     * Returns whether the client is currently connected.
     *
     * @return true if connected, otherwise false
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Updates the connection state.
     *
     * @param value the new connection state
     */
    public void setConnected(boolean value) {
        connected.set(value);
    }

    /**
     * Returns the current username.
     *
     * @return the username
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * Updates the current username.
     *
     * @param value the new username
     */
    public void setUsername(String value) {
        username.set(value);
    }

    /**
     * Returns the current status text.
     *
     * @return the status text
     */
    public String getStatusText() {
        return statusText.get();
    }

    /**
     * Updates the current status text.
     *
     * @param value the new status text
     */
    public void setStatusText(String value) {
        statusText.set(value);
    }

    /**
     * Returns the current player.
     *
     * @return the current player name
     */
    public String getCurrentPlayer() {
        return currentPlayer.get();
    }

    /**
     * Updates the current player.
     *
     * @param value the new current player
     */
    public void setCurrentPlayer(String value) {
        currentPlayer.set(value);
    }

    /**
     * Returns the current phase.
     *
     * @return the current phase
     */
    public String getCurrentPhase() {
        return currentPhase.get();
    }

    /**
     * Updates the current phase.
     *
     * @param value the new current phase
     */
    public void setCurrentPhase(String value) {
        currentPhase.set(value);
    }

    /**
     * Returns the current top card text.
     *
     * @return the top card text
     */
    public String getTopCardText() {
        return topCardText.get();
    }

    /**
     * Updates the current top card text.
     *
     * @param value the new top card text
     */
    public void setTopCardText(String value) {
        topCardText.set(value);
    }

    /**
     * Returns the observable chat mode property.
     *
     * @return the chat mode property
     */
    public StringProperty chatModeProperty() {
        return chatMode;
    }

    /**
     * Returns the currently selected chat mode.
     *
     * @return the chat mode
     */
    public String getChatMode() {
        return chatMode.get();
    }

    /**
     * Updates the currently selected chat mode.
     *
     * @param value the new chat mode
     */
    public void setChatMode(String value) {
        chatMode.set(value);
    }
}