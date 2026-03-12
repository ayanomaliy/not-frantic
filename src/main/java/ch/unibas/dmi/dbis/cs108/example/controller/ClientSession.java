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
 *
 * <p>This class handles communication with one connected client, processes
 * incoming messages, and keeps track of the client's session state. Each
 * client session runs in its own thread to support concurrent connections.</p>
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
     * @param serverService the server service responsible for handling messages
     *                      and managing connected clients
     * @param serverController the server controller managing active sessions
     */
    public ClientSession(Socket socket, ServerService serverService, ServerController serverController) {
        this.socket = socket;
        this.serverService = serverService;
        this.serverController = serverController;
    }

    /**
     * Sends a raw encoded message to this client.
     *
     * @param text the message text to send
     */
    public void send(String text) {
        if (out != null) {
            out.println(text);
        }
    }

    /**
     * Returns the current player name associated with this session.
     *
     * @return the player's display name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Updates the player name associated with this session.
     *
     * @param playerName the new display name of the player
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Returns the timestamp of the last received heartbeat message.
     *
     * @return the last heartbeat time in milliseconds
     */
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime.get();
    }

    /**
     * Updates the heartbeat timestamp to the current system time.
     */
    public void updateHeartbeatTime() {
        lastHeartbeatTime.set(System.currentTimeMillis());
    }

    /**
     * Closes this client connection and removes the session from the server.
     *
     * <p>This method is synchronized to prevent duplicate disconnection logic
     * when multiple parts of the code attempt to close the same session.</p>
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
     * Executes the client session.
     *
     * <p>This method initializes the input and output streams, registers the
     * client with the server, sends a welcome message, and continuously reads
     * incoming client messages. Heartbeat messages are handled directly, while
     * all other valid messages are forwarded to the server service.</p>
     */
    @Override
    public void run() {
        try (Socket s = this.socket) {
            System.out.println("[SESSION] New socket connection from " + s.getRemoteSocketAddress());

            this.out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            serverService.registerClient(this);

            send(new Message(Message.Type.INFO, "WELCOME").encode());

            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                System.out.println("[SESSION] Raw input from " + playerName + ": " + trimmed);

                Message message = Message.parse(trimmed);

                if (message == null) {
                    send(new Message(Message.Type.ERROR, "Invalid command.").encode());
                    continue;
                }

                if (message.type() == Message.Type.PING) {
                    updateHeartbeatTime();
                    send(new Message(Message.Type.PONG, "").encode());
                    continue;
                }

                if (message.type() == Message.Type.PONG) {
                    updateHeartbeatTime();
                    continue;
                }

                serverService.handleMessage(this, message);

                if (message.type() == Message.Type.QUIT) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("[SESSION] Client session ended: " + e.getMessage());
        } finally {
            System.out.println("[SESSION] Closing session for " + playerName);
            disconnect();
        }
    }
}