package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.controller.controller.ServerController;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

public class ServerMain {

    public static void main(String[] args) {
        int port = 5555;

        ServerService serverService = new ServerService();
        ServerController serverController = new ServerController(port, serverService);

        serverController.start();
    }
}