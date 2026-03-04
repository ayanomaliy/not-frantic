package ch.unibas.dmi.dbis.cs108.example.client.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
        String host = "10.192.4.22"; /*temporary solution, Aiysha's IP*/
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