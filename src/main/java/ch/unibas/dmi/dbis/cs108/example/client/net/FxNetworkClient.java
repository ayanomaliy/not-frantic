package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.service.Message;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;


/**
 * JavaFX-aware network client for the Frantic^-1 GUI.
 *
 * <p>This class manages the TCP connection to the game server, sends
 * protocol messages, receives server responses, and updates the shared
 * {@link ClientState} on the JavaFX application thread.</p>
 *
 * <p>It also maintains a heartbeat mechanism to detect lost server
 * connections automatically.</p>
 */
public class FxNetworkClient {

    /** Shared GUI state updated from incoming server messages. */
    private final ClientState state;
    private Runnable gameStartListener;
    private boolean gameViewShown = false;

    /** TCP socket connected to the game server. */
    private Socket socket;
    /** Reader for incoming server messages. */
    private BufferedReader serverIn;
    /** Writer for outgoing client messages. */
    private PrintWriter serverOut;
    /** Timestamp of the last received pong message from the server. */
    private final AtomicLong lastServerPongTime = new AtomicLong(System.currentTimeMillis());

    /**
     * Creates a new GUI network client.
     *
     * @param state the shared client state to update from network events
     */
    public FxNetworkClient(ClientState state) {
        this.state = state;
    }


    public void setGameStartListener(Runnable gameStartListener) {
        this.gameStartListener = gameStartListener;
    }


    /**
     * Connects to the server and initializes background reader and heartbeat threads.
     *
     * <p>After the connection is established, the client sends its initial
     * player name, starts a reader thread for incoming messages, starts a
     * heartbeat thread for connection monitoring, and requests the current
     * player list from the server.</p>
     *
     * @param host the server host name or IP address
     * @param port the server port
     * @param username the desired player name
     * @throws IOException if the socket connection cannot be established
     */
    public void connect(String host, int port, String username) throws IOException {
        socket = new Socket(host, port);
        serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        lastServerPongTime.set(System.currentTimeMillis());
        gameViewShown = false;

        Platform.runLater(() -> {
            state.setConnected(true);
            state.setUsername(username);
            state.setStatusText("Connected to " + host + ":" + port);
        });

        send(new Message(Message.Type.NAME, username));

        Thread readerThread = new Thread(this::readLoop, "client-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        Thread heartbeatThread = new Thread(this::heartbeatLoop, "client-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        requestPlayers();
        requestAllPlayers();
        requestLobbies();
    }
    /**
     * Disconnects from the server and updates the client state.
     *
     * <p>If a connection is currently active, a quit message is sent first.
     * The socket is then closed and the GUI state is reset to a disconnected
     * status.</p>
     */
    /**
     * Disconnects this client from the server and updates the local GUI state.
     *
     * <p>This method sends a {@code QUIT} message before closing the socket.</p>
     */
    public void disconnect() {
        disconnect(true);
    }

    /**
     * Disconnects this client from the server and updates the local GUI state.
     *
     * <p>If {@code notifyServer} is {@code true}, a {@code QUIT} message is
     * sent before the socket is closed. This is useful for distinguishing
     * between an explicit quit command and a local shutdown after the command
     * was already sent.</p>
     *
     * @param notifyServer whether a {@code QUIT} message should be sent first
     */
    private void disconnect(boolean notifyServer) {
        try {
            if (notifyServer && serverOut != null) {
                send(new Message(Message.Type.QUIT, ""));
            }
        } catch (Exception ignored) {
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        socket = null;
        serverIn = null;
        serverOut = null;
        gameViewShown = false;

        Platform.runLater(() -> {
            state.setConnected(false);
            state.setStatusText("Disconnected");
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

    /**
     * Sends a chat message to the server.
     *
     * @param text the chat message text
     */
    /**
     * Sends a global chat message to the server.
     *
     * @param text the chat message text
     */
    public void sendGlobalChat(String text) {
        send(new Message(Message.Type.GLOBALCHAT, text));
    }
    public void sendLobbyChat(String text) {
        send(new Message(Message.Type.LOBBYCHAT, text));
    }

    public void sendWhisperChat(String payload) {
        send(new Message(Message.Type.WHISPERCHAT, payload));
    }


    /**
     * Requests the current player list from the server.
     */
    public void requestPlayers() {
        send(new Message(Message.Type.PLAYERS, ""));
    }

    public void requestAllPlayers() {
        send(new Message(Message.Type.ALLPLAYERS, ""));
    }


    /**
     * Requests the current lobby list from the server.
     */
    public void requestLobbies() {
        send(new Message(Message.Type.LOBBIES, ""));
    }
    /**
     * Sends a request to start the game.
     */
    public void startGame() {
        send(new Message(Message.Type.START, ""));
    }



    /**
     * Sends a protocol message to the server if an output stream is available.
     *
     * @param message the message to send
     */
    private void send(Message message) {
        if (serverOut != null) {
            serverOut.println(message.encode());
        }
    }

    /**
     * Parses and executes a raw client command using the same rules as the
     * terminal client.
     *
     * <p>Supported input includes both slash commands such as
     * {@code /name Alice} and the structured protocol format parsed by
     * {@link Message#parse(String)}. Invalid input and unknown commands are
     * reported to the GUI. Manual heartbeat commands are rejected because
     * heartbeat handling is automatic.</p>
     *
     * <p>If the command is {@code /quit}, the method closes the local
     * connection after sending the quit request and returns {@code true} so the
     * caller can switch back to the connect view.</p>
     *
     * @param rawInput the raw command entered by the user
     * @return {@code true} if the command caused a disconnect, otherwise {@code false}
     */
    public boolean sendCommand(String rawInput) {
        Message message = Message.parse(rawInput);

        if (message == null) {
            Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Invalid input."));
            return false;
        }

        if (message.type() == Message.Type.UNKNOWN) {
            Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Unknown command."));
            return false;
        }

        if (message.type() == Message.Type.PING || message.type() == Message.Type.PONG) {
            Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Heartbeat messages are handled automatically."));
            return false;
        }

        send(message);

        if (message.type() == Message.Type.QUIT) {
            disconnect(false);
            return true;
        }

        return false;
    }

    /**
     * Continuously reads incoming server messages and processes them.
     *
     * <p>Malformed messages are reported in the GUI state. Valid messages
     * are forwarded to {@link #handleIncoming(Message)}. If the connection
     * is lost, the client state is updated accordingly.</p>
     */
    private void readLoop() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                Message message = Message.parse(line);

                if (message == null || !message.hasValidStructure()) {
                    final String invalidLine = line;
                    Platform.runLater(() ->
                            state.getGameMessages().add("[CLIENT] Invalid server message: " + invalidLine));
                    continue;
                }

                handleIncoming(message);
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                state.setConnected(false);
                state.setStatusText("Connection closed: " + e.getMessage());
            });
        }
    }
    /**
     * Handles a single incoming protocol message from the server.
     *
     * <p>Depending on the message type, this method may respond to
     * heartbeat messages, update the chat view, refresh the player list,
     * or append informational/game messages to the GUI state.</p>
     *
     * @param message the parsed incoming message
     */
    private void handleIncoming(Message message) {
        switch (message.type()) {
            case PING -> send(new Message(Message.Type.PONG, ""));
            case PONG -> lastServerPongTime.set(System.currentTimeMillis());

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

            case GAME_STATE -> {
                Platform.runLater(() -> {
                    state.getGameMessages().add("[GAME_STATE] " + message.content());

                    if (!gameViewShown) {
                        gameViewShown = true;
                        if (gameStartListener != null) {
                            gameStartListener.run();
                        }
                    }
                });
            }

            case GAME -> Platform.runLater(() ->
                    state.getGameMessages().add("[GAME] " + message.content()));

            default -> Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Unexpected: " + message.encode()));
        }
    }
    /**
     * Periodically sends heartbeat ping messages and checks server responsiveness.
     *
     * <p>If no pong message is received within the configured timeout,
     * the client disconnects automatically.</p>
     */
    private void heartbeatLoop() {
        final long heartbeatIntervalMillis = 5000;
        final long heartbeatTimeoutMillis = 15000;

        try {
            while (socket != null && !socket.isClosed()) {
                send(new Message(Message.Type.PING, ""));

                long now = System.currentTimeMillis();
                long lastPong = lastServerPongTime.get();

                if (now - lastPong > heartbeatTimeoutMillis) {
                    disconnect();
                    break;
                }

                Thread.sleep(heartbeatIntervalMillis);
            }
        } catch (Exception ignored) {
        }
    }
}