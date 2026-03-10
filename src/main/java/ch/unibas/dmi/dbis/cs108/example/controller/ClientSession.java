package ch.unibas.dmi.dbis.cs108.example.controller;

import ch.unibas.dmi.dbis.cs108.example.service.Message;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single client connection session.
 * <p>
 * Handles communication with a connected client, processes incoming commands,
 * and manages the client's session state. Runs in its own thread to handle
 * concurrent client connections.
 * </p>
 */
public class ClientSession implements Runnable {

    private final Socket socket;
    private final ServerService serverService;

    private PrintWriter out;
    private BufferedReader in;

    private String playerName = "Anonymous";
    private final AtomicLong lastHeartbeatTime = new AtomicLong(System.currentTimeMillis());

    private boolean disconnected = false;

    private final ServerController serverController;

    /**
     * Creates a new client session for the given socket connection.
     *
     * @param socket the client's TCP socket connection
     * @param serverService the server service managing game state
     */
    public ClientSession(Socket socket, ServerService serverService, ServerController serverController) {
        this.socket = socket;
        this.serverService = serverService;
        this.serverController = serverController;
    }

    /**
     * Sends a message to this client.
     *
     * @param text the message to send
     */
    public void send(String text) {
        if (out != null) {
            out.println(text);
        }
    }

    /**
     * Gets the name of the player associated with this session.
     *
     * @return the player's display name
     */
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime.get();
    }

    public void updateHeartbeatTime() {
        lastHeartbeatTime.set(System.currentTimeMillis());
    }

    /**
     * Closes this client connection and removes the session from the server.
     */
    public synchronized void disconnect() {
        if (disconnected) return;
        disconnected = true;

        try {
            socket.close();
        } catch (Exception ignored) {
        }

        serverService.unregisterClient(this);
        serverController.removeSession(this);
    }

    /**
     * Executes the client session thread.
     * <p>
     * Registers the client with the server, welcomes them, reads incoming commands,
     * processes them through the server service, and handles disconnection cleanup.
     * </p>
     */
    @Override
    public void run() {
        try (Socket s = this.socket) {
            System.out.println("[SESSION] New socket connection from " + s.getRemoteSocketAddress());

            this.out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            serverService.registerClient(this);

            send("WELCOME");
            send("Set your name with: /name YourName");

            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                System.out.println("[SESSION] Raw input from " + playerName + ": " + trimmed);

                if ("SYS|PING".equalsIgnoreCase(trimmed)) {
                    updateHeartbeatTime();
                    send("SYS|PONG");
                    continue;
                }

                if ("SYS|PONG".equalsIgnoreCase(trimmed)) {
                    updateHeartbeatTime();
                    continue;
                }

                Message message = Message.parse(trimmed);

                if (message == null) {
                    send("ERROR Invalid command.");
                    continue;
                }

                serverService.handleMessage(this, message);

                if (message.type() == Message.Type.QUIT) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("[SESSION] Client session ended: " + e.getMessage());
        }finally {
            System.out.println("[SESSION] Closing session for " + playerName);
            disconnect();
        }

    }
}