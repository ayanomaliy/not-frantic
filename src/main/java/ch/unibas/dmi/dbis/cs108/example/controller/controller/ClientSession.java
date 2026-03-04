package ch.unibas.dmi.dbis.cs108.example.controller.controller;

import ch.unibas.dmi.dbis.cs108.example.service.Message;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSession implements Runnable {

    private final Socket socket;
    private final ServerService serverService;

    private PrintWriter out;
    private BufferedReader in;

    private String playerName = "Anonymous";

    public ClientSession(Socket socket, ServerService serverService) {
        this.socket = socket;
        this.serverService = serverService;
    }

    public void send(String text) {
        out.println(text);
    }

    public String getPlayerName() {
        return playerName;
    }

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
                System.out.println("[SESSION] Raw input from " + playerName + ": " + line);

                Message message = Message.parse(line);

                if (message == null) {
                    send("ERROR Invalid command.");
                    continue;
                }

                if (message.type() == Message.Type.NAME) {
                    this.playerName = message.content();
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
            serverService.unregisterClient(this);
        }
    }
}