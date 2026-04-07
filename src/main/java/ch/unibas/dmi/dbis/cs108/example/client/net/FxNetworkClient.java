package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.service.Message;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Arrays;

/**
 * JavaFX adapter around the shared protocol client.
 *
 * <p>This class contains GUI-specific behavior only. All socket management,
 * message transport, heartbeat handling, and uniform protocol sending are
 * delegated to {@link ClientProtocolClient}.</p>
 */
public class FxNetworkClient implements ClientMessageHandler {

    /** Shared GUI state updated from incoming server messages. */
    private final ClientState state;

    /** Shared typed protocol client used by the GUI. */
    private final ClientProtocolClient protocolClient;

    /** Callback triggered once the first real game state arrives. */
    private Runnable gameStartListener;

    /** Prevents reopening the game view for every GAME_STATE update. */
    private boolean gameViewShown = false;

    /**
     * Creates a new GUI network adapter.
     *
     * @param state the shared client state
     */
    public FxNetworkClient(ClientState state) {
        this.state = state;
        this.protocolClient = new ClientProtocolClient(this);
    }

    /**
     * Registers a callback that is invoked when the server confirms that a game
     * has started by sending a {@code GAME_STATE} message.
     *
     * @param gameStartListener the callback to invoke
     */
    public void setGameStartListener(Runnable gameStartListener) {
        this.gameStartListener = gameStartListener;
    }

    /**
     * Connects to the server and initializes the GUI state.
     *
     * @param host the server host
     * @param port the server port
     * @param username the desired username
     * @throws IOException if the connection fails
     */
    public void connect(String host, int port, String username) throws IOException {
        protocolClient.connect(host, port);

        gameViewShown = false;

        Platform.runLater(() -> {
            state.setConnected(true);
            state.setUsername(username);
            state.setStatusText("Connected to " + host + ":" + port);
        });

        setName(username);

        requestAllPlayers();
        requestLobbies();
    }

    /**
     * Disconnects this client from the server.
     */
    public void disconnect() {
        protocolClient.disconnect();
        clearLocalState("Disconnected");
    }

    /**
     * Sends a request to set or change the player name.
     *
     * @param username the desired player name
     */
    public void setName(String username) {
        protocolClient.setName(username);
    }

    /**
     * Sends a global chat message.
     *
     * @param text the chat text
     */
    public void sendGlobalChat(String text) {
        protocolClient.sendGlobalChat(text);
    }

    /**
     * Sends a lobby chat message.
     *
     * @param text the chat text
     */
    public void sendLobbyChat(String text) {
        protocolClient.sendLobbyChat(text);
    }

    /**
     * Sends a whisper message.
     *
     * @param targetPlayer the whisper target
     * @param text the whisper text
     */
    public void sendWhisperChat(String targetPlayer, String text) {
        protocolClient.sendWhisperChat(targetPlayer, text);
    }

    /**
     * Requests the current lobby player list.
     */
    public void requestPlayers() {
        protocolClient.requestPlayers();
    }

    /**
     * Requests the global player list.
     */
    public void requestAllPlayers() {
        protocolClient.requestAllPlayers();
    }

    /**
     * Requests the lobby list.
     */
    public void requestLobbies() {
        protocolClient.requestLobbies();
    }

    /**
     * Creates a new lobby.
     *
     * @param lobbyId the desired lobby id
     */
    public void createLobby(String lobbyId) {
        protocolClient.createLobby(lobbyId);
    }

    /**
     * Joins an existing lobby.
     *
     * @param lobbyId the lobby id
     */
    public void joinLobby(String lobbyId) {
        protocolClient.joinLobby(lobbyId);
    }

    /**
     * Leaves the current lobby.
     */
    public void leaveLobby() {
        protocolClient.leaveLobby();
    }

    /**
     * Requests a game start.
     */
    public void startGame() {
        protocolClient.startGame();
    }

    /**
     * Parses and executes a raw terminal-style command for the manual command field.
     *
     * @param rawInput the raw command
     * @return {@code true} if the command caused a disconnect
     */
    public boolean sendCommand(String rawInput) {
        Message message = protocolClient.parseRawCommand(rawInput);

        if (message == null) {
            onLocalMessage("[CLIENT] Invalid input.");
            return false;
        }

        if (message.type() == Message.Type.UNKNOWN) {
            onLocalMessage("[CLIENT] Unknown command.");
            return false;
        }

        if (message.type() == Message.Type.PING || message.type() == Message.Type.PONG) {
            onLocalMessage("[CLIENT] Heartbeat messages are handled automatically.");
            return false;
        }

        protocolClient.send(message);

        if (message.type() == Message.Type.QUIT) {
            protocolClient.disconnect(false);
            clearLocalState("Disconnected");
            return true;
        }

        return false;
    }

    @Override
    public void onMessage(Message message) {
        switch (message.type()) {
            case GLOBALCHAT -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getGlobalChatMessages().add(parts[0] + ": " + parts[1]));
            }

            case LOBBYCHAT -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getLobbyChatMessages().add(parts[0] + ": " + parts[1]));
            }

            case WHISPERCHAT -> Platform.runLater(() -> {
                String[] parts = message.content().split("\\|", 3);

                if (parts.length == 3) {
                    String direction = parts[0];
                    String otherUser = parts[1];
                    String text = parts[2];

                    if ("FROM".equals(direction)) {
                        state.getWhisperChatMessages().add("[From " + otherUser + "] " + text);
                    } else if ("TO".equals(direction)) {
                        state.getWhisperChatMessages().add("[To " + otherUser + "] " + text);
                    } else {
                        state.getWhisperChatMessages().add(message.content());
                    }
                } else {
                    state.getWhisperChatMessages().add(message.content());
                }
            });

            case INFO -> Platform.runLater(() ->
                    state.getGameMessages().add("[INFO] " + message.content()));

            case ERROR -> Platform.runLater(() ->
                    state.getGameMessages().add("[ERROR] " + message.content()));

            case PLAYERS -> Platform.runLater(() -> {
                state.getPlayers().setAll(
                        message.content().isBlank()
                                ? java.util.List.of()
                                : Arrays.stream(message.content().split(","))
                                  .map(String::trim)
                                  .filter(s -> !s.isBlank())
                                  .toList()
                );
            });

            case ALLPLAYERS -> Platform.runLater(() -> {
                state.getAllPlayers().setAll(
                        message.content().isBlank()
                                ? java.util.List.of()
                                : Arrays.stream(message.content().split(","))
                                  .map(String::trim)
                                  .filter(s -> !s.isBlank())
                                  .toList()
                );
            });

            case LOBBIES -> Platform.runLater(() -> {
                state.getLobbies().setAll(
                        message.content().isBlank()
                                ? java.util.List.of()
                                : Arrays.stream(message.content().split(","))
                                  .map(String::trim)
                                  .filter(s -> !s.isBlank())
                                  .toList()
                );
            });

            case GAME_STATE -> Platform.runLater(() -> {
                state.getGameMessages().add("[GAME_STATE] " + message.content());

                if (!gameViewShown) {
                    gameViewShown = true;
                    if (gameStartListener != null) {
                        gameStartListener.run();
                    }
                }
            });

            case GAME -> Platform.runLater(() ->
                    state.getGameMessages().add("[GAME] " + message.content()));

            case HAND_UPDATE -> Platform.runLater(() ->
                    state.getGameMessages().add("[HAND] " + message.content()));

            case EFFECT_REQUEST -> Platform.runLater(() ->
                    state.getGameMessages().add("[EFFECT_REQUEST] " + message.content()));

            case ROUND_END -> Platform.runLater(() ->
                    state.getGameMessages().add("[ROUND_END] " + message.content()));

            case GAME_END -> Platform.runLater(() ->
                    state.getGameMessages().add("[GAME_END] " + message.content()));

            default -> Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Unexpected: " + message.encode()));
        }
    }

    @Override
    public void onLocalMessage(String text) {
        Platform.runLater(() -> state.getGameMessages().add(text));
    }

    @Override
    public void onDisconnected(String reason) {
        clearLocalState(reason);
    }

    /**
     * Clears the local GUI state after a disconnect.
     *
     * @param statusText the new status text
     */
    private void clearLocalState(String statusText) {
        gameViewShown = false;

        Platform.runLater(() -> {
            state.setConnected(false);
            state.setStatusText(statusText);
            state.setChatMode("Global");

            state.getPlayers().clear();
            state.getAllPlayers().clear();
            state.getLobbies().clear();

            state.getGlobalChatMessages().clear();
            state.getLobbyChatMessages().clear();
            state.getWhisperChatMessages().clear();

            state.getGameMessages().clear();
        });
    }
}