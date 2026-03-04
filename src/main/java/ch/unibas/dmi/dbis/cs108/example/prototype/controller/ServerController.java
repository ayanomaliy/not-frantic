package ch.unibas.dmi.dbis.cs108.example.prototype.controller;

import ch.unibas.dmi.dbis.cs108.example.prototype.service.ServerService;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerController {

    private final int port;
    private final ServerService serverService;

    public ServerController(int port, ServerService serverService) {
        this.port = port;
        this.serverService = serverService;
    }

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