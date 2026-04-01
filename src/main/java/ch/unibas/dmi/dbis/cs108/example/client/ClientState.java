package ch.unibas.dmi.dbis.cs108.example.client.state;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClientState {

    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty statusText = new SimpleStringProperty("Not connected");
    private final ObservableList<String> players = FXCollections.observableArrayList();
    private final ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> gameMessages = FXCollections.observableArrayList();

    public BooleanProperty connectedProperty() { return connected; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty statusTextProperty() { return statusText; }

    public ObservableList<String> getPlayers() { return players; }
    public ObservableList<String> getChatMessages() { return chatMessages; }
    public ObservableList<String> getGameMessages() { return gameMessages; }

    public boolean isConnected() { return connected.get(); }
    public void setConnected(boolean value) { connected.set(value); }

    public String getUsername() { return username.get(); }
    public void setUsername(String value) { username.set(value); }

    public String getStatusText() { return statusText.get(); }
    public void setStatusText(String value) { statusText.set(value); }
}