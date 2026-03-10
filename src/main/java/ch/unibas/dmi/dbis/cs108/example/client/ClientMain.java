package ch.unibas.dmi.dbis.cs108.example.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entry point for the client application.
 * <p>
 * Establishes a TCP connection to the game server, starts a background thread
 * to read incoming messages ({@link ClientReader}), and forwards user input
 * from stdin to the server.
 * </p>
 */
public class ClientMain {

    /**
     * Launches the client, connects to the server, and processes user commands.
     * <p>
     * Supported commands:
     * <ul>
     *   <li>{@code /name <username>} – set the player's display name</li>
     *   <li>{@code /chat <message>} – send a chat message</li>
     *   <li>{@code /players} – list connected players</li>
     *   <li>{@code /start} – request to start the game</li>
     *   <li>{@code /quit} – disconnect from the server</li>
     * </ul>
     * </p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        String host = "localhost"; /*temporary solution, Aiysha's IP*/
        int port = 5555;

        final long heartbeatIntervalMillis = 5000;
        final long heartbeatTimeoutMillis = 15000;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverIn = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader userIn = new BufferedReader(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8))
        ) {
            System.out.println("Connected to server at " + host + ":" + port);

            String suggestedName = System.getProperty("user.name");
            if (suggestedName == null || suggestedName.isBlank()) {
                suggestedName = "Player";
            }
            suggestedName = suggestedName.trim();
            System.out.println("Your suggested username is " + suggestedName + ".")
            // apply suggested name
            serverOut.println("/name " + suggestedName);


            AtomicLong lastServerPongTime = new AtomicLong(System.currentTimeMillis());

            Thread readerThread = new Thread(new ClientReader(serverIn, serverOut, lastServerPongTime));
            readerThread.setDaemon(true);
            readerThread.start();

            Thread heartbeatThread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        serverOut.println("SYS|PING");

                        long now = System.currentTimeMillis();
                        long lastPong = lastServerPongTime.get();

                        if (now - lastPong > heartbeatTimeoutMillis) {
                            System.err.println("Connection to server lost.");
                            socket.close();
                            break;
                        }

                        Thread.sleep(heartbeatIntervalMillis);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) {
                        System.err.println("Heartbeat error: " + e.getMessage());
                    }
                }
            });

            heartbeatThread.setDaemon(true);
            heartbeatThread.start();

            System.out.println("Commands:");
            System.out.println("  /name Alice");
            System.out.println("  /chat Hello");
            System.out.println("  /players");
            System.out.println("  /start");
            System.out.println("  /quit");

            String line;
            while ((line = userIn.readLine()) != null) {

                if (line.trim().equals("/name")) {
                    if (suggestedName != null && !suggestedName.isBlank()) {
                        System.out.println("Using suggested nickname: " + suggestedName);
                        serverOut.println("/name " + suggestedName);
                        continue;
                    }
                }

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