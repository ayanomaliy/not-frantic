package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.client.state.ClientState;
import ch.unibas.dmi.dbis.cs108.example.service.Message;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class FxNetworkClient {

    private final ClientState state;

    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private final AtomicLong lastServerPongTime = new AtomicLong(System.currentTimeMillis());

    public FxNetworkClient(ClientState state) {
        this.state = state;
    }

    public void connect(String host, int port, String username) throws IOException {
        socket = new Socket(host, port);
        serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

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
    }

    public void disconnect() {
        try {
            if (serverOut != null) {
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

        Platform.runLater(() -> {
            state.setConnected(false);
            state.setStatusText("Disconnected");
            state.getPlayers().clear();
        });
    }

    public void sendChat(String text) {
        send(new Message(Message.Type.CHAT, text));
    }

    public void requestPlayers() {
        send(new Message(Message.Type.PLAYERS, ""));
    }

    public void startGame() {
        send(new Message(Message.Type.START, ""));
    }

    private void send(Message message) {
        if (serverOut != null) {
            serverOut.println(message.encode());
        }
    }

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

    private void handleIncoming(Message message) {
        switch (message.type()) {
            case PING -> send(new Message(Message.Type.PONG, ""));
            case PONG -> lastServerPongTime.set(System.currentTimeMillis());

            case CHAT -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getChatMessages().add(parts[0] + ": " + parts[1]));
            }

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

            case GAME -> Platform.runLater(() ->
                    state.getGameMessages().add("[GAME] " + message.content()));

            default -> Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Unexpected: " + message.encode()));
        }
    }

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