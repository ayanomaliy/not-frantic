package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared TCP transport and protocol client used by both the terminal client
 * and the JavaFX GUI client.
 *
 * <p>This class owns the socket connection, outgoing writer, incoming reader,
 * heartbeat loop, and server read loop. It sends and receives structured
 * {@link Message} objects uniformly regardless of the front end.</p>
 *
 * <p>UI-specific behavior is intentionally excluded. Incoming events are
 * forwarded to a {@link ClientMessageHandler} so different client front ends
 * can react in their own way.</p>
 */
public class NetworkClientCore {

    private final ClientMessageHandler handler;

    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private volatile int connectionGeneration = 0;

    private final AtomicLong lastServerPongTime =
            new AtomicLong(System.currentTimeMillis());

    private volatile boolean disconnected = false;

    /**
     * Creates a new shared client transport.
     *
     * @param handler the callback handler for incoming messages and disconnects
     */
    public NetworkClientCore(ClientMessageHandler handler) {
        this.handler = handler;
    }

    /**
     * Opens the TCP connection and starts background reader and heartbeat threads.
     *
     * @param host the server host
     * @param port the server port
     * @throws IOException if the connection cannot be established
     */
    public synchronized void connect(String host, int port) throws IOException {
        disconnect(false);

        int myGeneration = ++connectionGeneration;

        socket = new Socket(host, port);
        serverIn = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        disconnected = false;
        lastServerPongTime.set(System.currentTimeMillis());

        Thread readerThread = new Thread(() -> readLoop(myGeneration), "client-reader-" + myGeneration);
        readerThread.setDaemon(true);
        readerThread.start();

        Thread heartbeatThread = new Thread(() -> heartbeatLoop(myGeneration), "client-heartbeat-" + myGeneration);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Returns whether the client is currently connected.
     *
     * @return {@code true} if connected, otherwise {@code false}
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && !disconnected;
    }

    /**
     * Sends a structured protocol message to the server.
     *
     * @param message the message to send
     */
    public void send(Message message) {
        if (serverOut != null) {
            serverOut.println(message.encode());
        }
    }

    /**
     * Disconnects from the server and optionally sends a {@code QUIT} message first.
     *
     * @param notifyServer whether a quit message should be sent before closing
     */
    public synchronized void disconnect(boolean notifyServer) {
        if (disconnected) {
            return;
        }
        disconnected = true;

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
    }

    /**
     * Disconnects from the server and sends a {@code QUIT} message first.
     */
    public void disconnect() {
        disconnect(true);
    }

    /**
     * Continuously reads incoming server messages and dispatches them to the handler.
     */
    private void readLoop(int generation) {
        try {
            String line;

            while (isCurrentConnection(generation)
                    && serverIn != null
                    && (line = serverIn.readLine()) != null) {

                Message message = Message.parse(line);

                if (message == null || !message.hasValidStructure()) {
                    handler.onLocalMessage("[CLIENT] Invalid server message: " + line);
                    continue;
                }

                if (message.type() == Message.Type.PING) {
                    send(new Message(Message.Type.PONG, ""));
                    continue;
                }

                if (message.type() == Message.Type.PONG) {
                    lastServerPongTime.set(System.currentTimeMillis());
                    continue;
                }

                handler.onMessage(message);
            }

            if (isCurrentConnection(generation)) {
                handler.onDisconnected("Connection closed.");
            }

        } catch (Exception e) {
            if (isCurrentConnection(generation) && !disconnected) {
                handler.onDisconnected("Connection closed: " + e.getMessage());
            }
        } finally {
            if (isCurrentConnection(generation)) {
                disconnect(false);
            }
        }
    }

    /**
     * Periodically sends heartbeat pings and disconnects if the server becomes unresponsive.
     */
    private void heartbeatLoop(int generation) {
        final long heartbeatIntervalMillis = 5000;
        final long heartbeatTimeoutMillis = 15000;

        try {
            while (isCurrentConnection(generation)
                    && socket != null
                    && !socket.isClosed()
                    && !disconnected) {

                send(new Message(Message.Type.PING, ""));

                long now = System.currentTimeMillis();
                long lastPong = lastServerPongTime.get();

                if (now - lastPong > heartbeatTimeoutMillis) {
                    if (isCurrentConnection(generation)) {
                        handler.onDisconnected("Connection to server lost.");
                        disconnect(false);
                    }
                    break;
                }

                Thread.sleep(heartbeatIntervalMillis);
            }
        } catch (Exception ignored) {
        }
    }
    /**
     * Simulates an unexpected network loss for testing reconnect behavior.
     *
     * <p>This closes the socket without marking the client as intentionally
     * disconnected, so the read loop can detect the broken connection and trigger
     * the reconnect logic.</p>
     */
    public void simulateNetworkLossForTesting() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isCurrentConnection(int generation) {
        return generation == connectionGeneration;
    }

}