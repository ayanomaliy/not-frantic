package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.controller.ServerController;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

/**
 * Entry point for the game server application.
 * <p>
 * Initializes a {@link ServerService} to manage game state and creates a
 * {@link ServerController} to accept client connections on the specified port.
 * </p>
 */
public class ServerMain {

    /**
     * Starts the game server listening for client connections.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        int port = 5555;

        ServerService serverService = new ServerService();
        ServerController serverController = new ServerController(port, serverService);

        serverController.start();
    }
}