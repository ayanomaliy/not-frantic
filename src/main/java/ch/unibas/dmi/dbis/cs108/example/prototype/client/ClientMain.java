package ch.unibas.dmi.dbis.cs108.example.prototype.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientMain {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5555;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverIn = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader userIn = new BufferedReader(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8))
        ) {
            System.out.println("Connected to server at " + host + ":" + port);

            Thread readerThread = new Thread(new ClientReader(serverIn));
            readerThread.setDaemon(true);
            readerThread.start();

            System.out.println("Commands:");
            System.out.println("  /name Alice");
            System.out.println("  /chat Hello");
            System.out.println("  /players");
            System.out.println("  /start");
            System.out.println("  /quit");

            String line;
            while ((line = userIn.readLine()) != null) {
                serverOut.println(line);

                if ("/quit".equalsIgnoreCase(line.trim())) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}