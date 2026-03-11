package ch.unibas.dmi.dbis.cs108.example.client;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entry point for the client application.
 *
 * <p>Establishes a TCP connection to the game server, starts a background thread
 * to read incoming messages ({@link ClientReader}), and forwards user input
 * from stdin to the server.
 */
public class ClientMain {

    /**
     * Launches the client, connects to the server, and processes user commands.
     *
     * <p>Supported commands:
     * <ul>
     *   <li>{@code /name <username>} – set the player's display name</li>
     *   <li>{@code /chat <message>} – send a chat message</li>
     *   <li>{@code /players} – list connected players</li>
     *   <li>{@code /start} – request to start the game</li>
     *   <li>{@code /quit} – disconnect from the server</li>
     * </ul>
     *
     * <p>Command-line usage:
     * <ul>
     *   <li>{@code client <hostaddress>:<port> [<username>]}</li>
     * </ul>
     *
     * @param args command-line arguments:
     *             {@code <hostaddress>:<port>} and optionally {@code <username>}
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;
        String suggestedName = System.getProperty("user.name");

        if (args.length >= 1) {
            String[] hostPort = args[0].split(":", 2);

            if (hostPort.length != 2 || hostPort[0].isBlank() || hostPort[1].isBlank()) {
                System.err.println("Usage: client <hostaddress>:<port> [<username>]");
                return;
            }

            host = hostPort[0].trim();

            try {
                port = Integer.parseInt(hostPort[1].trim());
                if (port < 1 || port > 65535) {
                    System.err.println("Invalid port: " + hostPort[1]);
                    System.err.println("Port must be between 1 and 65535.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + hostPort[1]);
                System.err.println("Usage: client <hostaddress>:<port> [<username>]");
                return;
            }
        }

        if (args.length >= 2) {
            suggestedName = args[1];
        }

        if (args.length > 2) {
            System.err.println("Usage: client <hostaddress>:<port> [<username>]");
            return;
        }

        if (suggestedName == null || suggestedName.isBlank()) {
            suggestedName = "Player";
        }
        suggestedName = suggestedName.trim();

        final long heartbeatIntervalMillis = 5000;
        final long heartbeatTimeoutMillis = 15000;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverIn = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader userIn = new BufferedReader(
                        new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to server at " + host + ":" + port);
            System.out.println("Using username: " + suggestedName);

            serverOut.println(new Message(Message.Type.NAME, suggestedName).encode());

            AtomicLong lastServerPongTime = new AtomicLong(System.currentTimeMillis());

            Thread readerThread = new Thread(new ClientReader(serverIn, serverOut, lastServerPongTime));
            readerThread.setDaemon(true);
            readerThread.start();

            Thread heartbeatThread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        serverOut.println(new Message(Message.Type.PING, "").encode());

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
                String trimmed = line.trim();
/*
                // edge case: /name without any name uses suggested user name
                if (trimmed.equals("/name")) {
                    System.out.println("Using suggested nickname: " + suggestedName);
                    serverOut.println(new Message(Message.Type.NAME, suggestedName).encode());
                    continue;
                }
*/
                Message message = Message.parse(trimmed);

                if (message == null) {
                    System.out.println("Invalid input.");
                    continue;
                }

                if (message.type() == Message.Type.UNKNOWN) {
                    System.out.println("Unknown command.");
                    continue;
                }

                if (message.type() == Message.Type.PING || message.type() == Message.Type.PONG) {
                    System.out.println("Heartbeat messages are handled automatically.");
                    continue;
                }
                serverOut.println(message.encode());

                if (message.type() == Message.Type.QUIT) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}