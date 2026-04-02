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
    private final ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> gameMessages = FXCollections.observableArrayList();
    private final ObservableList<String> lobbies = FXCollections.observableArrayList();


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
     * Returns the observable list of chat messages.
     *
     * @return the chat message list
     */
    public ObservableList<String> getChatMessages() { return chatMessages; }
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
}