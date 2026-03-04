package ch.unibas.dmi.dbis.cs108.example.prototype.controller;

import ch.unibas.dmi.dbis.cs108.example.prototype.service.ServerService;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manages the server's TCP connection handling and client session creation.
 * <p>
 * Listens on a specified port for incoming client connections and spawns a new
 * {@link ClientSession} thread for each connected client to handle their communication.
 * </p>
 */
public class ServerController {

    private final int port;
    private final ServerService serverService;

    /**
     * Creates a server controller for accepting client connections.
     *
     * @param port the port number on which the server listens
     * @param serverService the service used to manage game state and client interactions
     */
    public ServerController(int port, ServerService serverService) {
        this.port = port;
        this.serverService = serverService;
    }

    /**
     * Starts the server and begins accepting client connections.
     * <p>
     * Continuously listens for incoming connections and creates a new client session
     * thread for each connected client. Runs indefinitely until interrupted.
     * </p>
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientSession clientSession = new ClientSession(socket, serverService);
                Thread thread = new Thread(clientSession);
                thread.start();
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}