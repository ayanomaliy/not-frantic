package ch.unibas.dmi.dbis.cs108.example.client;

import javafx.beans.property.*;
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
    private final ObservableList<String> players = FXCollections.observableArrayList();
    private final ObservableList<String> globalChatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> lobbyChatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> whisperChatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> gameMessages = FXCollections.observableArrayList();
    private final ObservableList<String> lobbies = FXCollections.observableArrayList();
    private final StringProperty chatMode = new SimpleStringProperty("Global");

    /**
     * Returns the observable connection status property.
     *
     * @return the connected property
     */
    public BooleanProperty connectedProperty() { return connected; }
    /**
     * Returns the observable username property.
     *
     * @return the username property
     */
    public StringProperty usernameProperty() { return username; }
    /**
     * Returns the observable status text property.
     *
     * @return the status text property
     */
    public StringProperty statusTextProperty() { return statusText; }

    /**
     * Returns the observable list of connected players.
     *
     * @return the player list
     */
    public ObservableList<String> getPlayers() { return players; }


    /**
     * Returns the observable list of global chat messages.
     *
     * <p>This list contains messages received through the global chat channel.
     * UI components can bind to this list to display updates automatically.</p>
     *
     * @return the global chat message list
     */
    public ObservableList<String> getGlobalChatMessages() {
        return globalChatMessages;
    }

    /**
     * Returns the observable list of lobby chat messages.
     *
     * <p>This list contains messages received through the currently joined lobby's
     * chat channel. UI components can bind to this list to display updates
     * automatically.</p>
     *
     * @return the lobby chat message list
     */
    public ObservableList<String> getLobbyChatMessages() {
        return lobbyChatMessages;
    }

    /**
     * Returns the observable list of whisper chat messages.
     *
     * <p>This list contains private chat messages exchanged directly between
     * players. UI components can bind to this list to display updates
     * automatically.</p>
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
    public ObservableList<String> getGameMessages() { return gameMessages; }

    public boolean isConnected() { return connected.get(); }
    public void setConnected(boolean value) { connected.set(value); }

    public String getUsername() { return username.get(); }
    public void setUsername(String value) { username.set(value); }

    public String getStatusText() { return statusText.get(); }
    public void setStatusText(String value) { statusText.set(value); }

    public ObservableList<String> getLobbies() {
        return lobbies;
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