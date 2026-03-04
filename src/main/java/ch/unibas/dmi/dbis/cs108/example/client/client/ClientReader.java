package ch.unibas.dmi.dbis.cs108.example.client.client;

import java.io.BufferedReader;

/**
 * Reads incoming lines from the server and prints them to stdout.
 * <p>
 * Intended to run in a dedicated background thread so that user input handling
 * and server message processing can happen concurrently.
 * </p>
 */
public class ClientReader implements Runnable {

    private final BufferedReader serverIn;

    /**
     * Creates a reader task for server messages.
     *
     * @param serverIn buffered character stream connected to the server socket
     */
    public ClientReader(BufferedReader serverIn) {
        this.serverIn = serverIn;
    }

    /**
     * Continuously reads lines from the server and prints them to the console
     * until the connection closes or an I/O error occurs.
     */
    @Override
    public void run() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            System.err.println("Disconnected from server.");
        }
    }
}