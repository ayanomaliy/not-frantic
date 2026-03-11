package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.controller.ServerController;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the game server application.
 * <p>
 * Initializes a {@link ServerService} to manage game state and creates a
 * {@link ServerController} to accept client connections on the specified port.
 * </p>
 */
public class ServerMain {

    /**
     * Starts the game server listening for client connections. Either accepts Port as argument or opens default port 5555.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        int port = 5555;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0]);
                System.err.println("Usage: server <port>");
                return;
            }
        }

        ServerService serverService = new ServerService();
        ServerController serverController = new ServerController(port, serverService);

        serverController.start();
    }
}