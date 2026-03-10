package ch.unibas.dmi.dbis.cs108.example.controller;

import ch.unibas.dmi.dbis.cs108.example.service.Message;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controls the game server.
 * <p>
 * Accepts client connections and manages active sessions.
 * A background heartbeat thread periodically checks whether
 * connected clients are still alive.
 */
public class ServerController {

    /** Server listening port. */
    private final int port;

    /** Service responsible for game logic and message handling. */
    private final ServerService serverService;

    /** Active client sessions. */
    private final List<ClientSession> sessions = new CopyOnWriteArrayList<>();

    /** Interval between heartbeat pings. */
    private static final long HEARTBEAT_INTERVAL_MS = 5000;

    /** Timeout before a client is considered disconnected. */
    private static final long HEARTBEAT_TIMEOUT_MS = 15000;

    /**
     * Creates a new server controller.
     *
     * @param port server port
     * @param serverService service managing game state
     */
    public ServerController(int port, ServerService serverService) {
        this.port = port;
        this.serverService = serverService;
    }

    /**
     * Starts the server and accepts incoming client connections.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);

            startHeartbeatMonitor();

            while (true) {
                Socket socket = serverSocket.accept();

                ClientSession clientSession = new ClientSession(socket, serverService, this);
                sessions.add(clientSession);

                Thread thread = new Thread(clientSession);
                thread.start();
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    /**
     * Removes a client session from the active session list.
     *
     * @param session the session to remove
     */
    public void removeSession(ClientSession session) {
        sessions.remove(session);
    }

    /**
     * Starts a background thread that monitors client heartbeats.
     * Clients that do not respond in time are removed.
     */
    private void startHeartbeatMonitor() {

        Thread heartbeatThread = new Thread(() -> {
            try {
                while (true) {

                    long now = System.currentTimeMillis();

                    for (ClientSession session : sessions) {

                        session.send(new Message(Message.Type.PING, "").encode());

                        if (now - session.getLastHeartbeatTime() > HEARTBEAT_TIMEOUT_MS) {
                            System.err.println("[SERVER] Client timed out: " + session.getPlayerName());
                            session.disconnect();
                        }
                    }

                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                }

            } catch (InterruptedException ignored) {
            }
        });

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}