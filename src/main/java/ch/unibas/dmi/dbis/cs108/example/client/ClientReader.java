package ch.unibas.dmi.dbis.cs108.example.client;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads incoming lines from the server and processes system heartbeat messages
 * in the background without printing them to the user.
 */
public class ClientReader implements Runnable {

    private final BufferedReader serverIn;
    private final PrintWriter serverOut;
    private final AtomicLong lastServerPongTime;

    /**
     * Creates a reader task for server messages.
     *
     * @param serverIn buffered character stream connected to the server socket
     * @param serverOut writer connected to the server socket
     * @param lastServerPongTime timestamp of the last received server pong
     */
    public ClientReader(BufferedReader serverIn, PrintWriter serverOut, AtomicLong lastServerPongTime) {
        this.serverIn = serverIn;
        this.serverOut = serverOut;
        this.lastServerPongTime = lastServerPongTime;
    }

    /**
     * Continuously reads lines from the server.
     * System heartbeat messages are handled silently.
     * User-visible messages are printed to the console.
     */
    @Override
    public void run() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                String trimmed = line.trim();

                if ("SYS|PING".equalsIgnoreCase(trimmed)) {
                    serverOut.println("SYS|PONG");
                    continue;
                }

                if ("SYS|PONG".equalsIgnoreCase(trimmed)) {
                    lastServerPongTime.set(System.currentTimeMillis());
                    continue;
                }

                System.out.println(line);
            }

            System.err.println("Disconnected from server.");
        } catch (Exception e) {
            System.err.println("Disconnected from server.");
        }
    }
}