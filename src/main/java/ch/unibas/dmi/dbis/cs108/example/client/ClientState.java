package ch.unibas.dmi.dbis.cs108.example.client;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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

    /** Public per-player state derived from each {@code GAME_STATE} broadcast. */
    public record PlayerInfo(String name, int handSize, String color) {}

    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty statusText = new SimpleStringProperty("Not connected");

    /** Current player whose turn it is. */
    private final StringProperty currentPlayer = new SimpleStringProperty("Unknown");

    /** Current phase of the game. */
    private final StringProperty currentPhase = new SimpleStringProperty("WAITING");

    /** Text representation of the current top discard card. */
    private final StringProperty topCardText = new SimpleStringProperty("-");

    private final StringProperty topCardId = new SimpleStringProperty("");

    private final StringProperty requestedColor = new SimpleStringProperty("");
    private final StringProperty requestedNumber = new SimpleStringProperty("");

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

    /** Current hand as card-id strings received from HAND_UPDATE. */
    private final ObservableList<String> currentHandCards = FXCollections.observableArrayList();

    /** Currently selected chat mode in the GUI. */
    private final StringProperty chatMode = new SimpleStringProperty("Global");

    /** Name of the lobby the client is currently in, or empty if not in a lobby. */
    private final StringProperty currentLobby = new SimpleStringProperty("");

    /** Name of the winner*/
    private final StringProperty winnerName = new SimpleStringProperty("");
    /** List of final scores*/
    private final ObservableList<String> finalScoreRows = FXCollections.observableArrayList();

    private final StringProperty pendingEffectRequest = new SimpleStringProperty("");

    private final StringProperty previousRenderableTopCardId = new SimpleStringProperty("");

    /** Ordered player list from the most recent {@code GAME_STATE} broadcast. */
    private final ObservableList<PlayerInfo> playerInfoList = FXCollections.observableArrayList();

    /** Current round number, updated from each {@code GAME_STATE} and {@code NEXT_ROUND} message. */
    private final IntegerProperty currentRound = new SimpleIntegerProperty(1);


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
     * Returns the observable list of current hand cards.
     *
     * <p>Each entry is the raw card id received from the server via
     * {@code HAND_UPDATE}.</p>
     *
     * @return the current hand card list
     */
    public ObservableList<String> getCurrentHandCards() {
        return currentHandCards;
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

    /**
     * Returns the observable current lobby property.
     *
     * @return the current lobby property
     */
    public StringProperty currentLobbyProperty() {
        return currentLobby;
    }

    /**
     * Returns the name of the current lobby, or an empty string if not in a lobby.
     *
     * @return the current lobby name
     */
    public String getCurrentLobby() {
        return currentLobby.get();
    }

    /**
     * Updates the current lobby name.
     *
     * @param value the new current lobby name
     */
    public void setCurrentLobby(String value) {
        currentLobby.set(value == null ? "" : value);
    }

    public StringProperty winnerNameProperty() {
        return winnerName;
    }

    public String getWinnerName() {
        return winnerName.get();
    }

    public void setWinnerName(String value) {
        winnerName.set(value == null ? "" : value);
    }

    public ObservableList<String> getFinalScoreRows() {
        return finalScoreRows;
    }

    public StringProperty topCardIdProperty() {
        return topCardId;
    }

    public String getTopCardId() {
        return topCardId.get();
    }

    public void setTopCardId(String value) {
        topCardId.set(value == null ? "" : value);
    }


    public StringProperty requestedColorProperty() {
        return requestedColor;
    }

    public String getRequestedColor() {
        return requestedColor.get();
    }

    public void setRequestedColor(String value) {
        requestedColor.set(value == null ? "" : value);
    }

    public StringProperty requestedNumberProperty() {
        return requestedNumber;
    }

    public String getRequestedNumber() {
        return requestedNumber.get();
    }

    public void setRequestedNumber(String value) {
        requestedNumber.set(value == null ? "" : value);
    }

    public StringProperty pendingEffectRequestProperty() {
        return pendingEffectRequest;
    }

    public String getPendingEffectRequest() {
        return pendingEffectRequest.get();
    }

    public void setPendingEffectRequest(String value) {
        pendingEffectRequest.set(value == null ? "" : value);
    }

    public StringProperty previousRenderableTopCardIdProperty() {
        return previousRenderableTopCardId;
    }

    public String getPreviousRenderableTopCardId() {
        return previousRenderableTopCardId.get();
    }

    public void setPreviousRenderableTopCardId(String value) {
        previousRenderableTopCardId.set(value == null ? "" : value);
    }

    /**
     * Returns the ordered list of players from the most recent {@code GAME_STATE} broadcast.
     *
     * @return the player info list
     */
    public ObservableList<PlayerInfo> getPlayerInfoList() {
        return playerInfoList;
    }

    public IntegerProperty currentRoundProperty() {
        return currentRound;
    }

    public int getCurrentRound() {
        return currentRound.get();
    }

    public void setCurrentRound(int round) {
        currentRound.set(round);
    }
}