package ch.unibas.dmi.dbis.cs108.example.client;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads and processes messages sent by the server.
 *
 * <p>This class runs in a background thread on the client side. It continuously
 * reads incoming messages from the server, validates them, and handles them
 * according to their {@link Message.Type}. It also responds to heartbeat
 * requests and updates the timestamp of the last received pong message.
 */
public class ClientReader implements Runnable {

    private final BufferedReader serverIn;
    private final PrintWriter serverOut;
    private final AtomicLong lastServerPongTime;

    /**
     * Creates a new client reader.
     *
     * @param serverIn reader used to receive messages from the server
     * @param serverOut writer used to send messages back to the server
     * @param lastServerPongTime shared timestamp of the last received pong
     *                           message from the server
     */
    public ClientReader(BufferedReader serverIn, PrintWriter serverOut, AtomicLong lastServerPongTime) {
        this.serverIn = serverIn;
        this.serverOut = serverOut;
        this.lastServerPongTime = lastServerPongTime;
    }

    /**
     * Continuously reads and handles incoming server messages.
     *
     * <p>The method validates each received message and processes it based on
     * its type. Ping messages are answered with a pong response, pong messages
     * update the heartbeat timestamp, and user-visible messages such as chat,
     * info, error, player list, and game messages are printed to standard
     * output.
     */
    @Override
    public void run() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                Message message = Message.parse(line);

                if (message == null || !message.hasValidStructure()) {
                    System.out.println("[CLIENT] Invalid server message: " + line);
                    continue;
                }

                switch (message.type()) {
                    case PING -> {
                        serverOut.println(new Message(Message.Type.PONG, "").encode());
                    }
                    case PONG -> {
                        lastServerPongTime.set(System.currentTimeMillis());
                    }
                    case GLOBALCHAT -> {
                        String[] parts = message.splitChatPayload();
                        String sender = parts[0];
                        String text = parts[1];
                        System.out.println("[Global] " + sender + ": " + text);
                    }
                    case LOBBYCHAT -> {
                        String[] parts = message.splitChatPayload();
                        String sender = parts[0];
                        String text = parts[1];
                        System.out.println("[Lobby] " + sender + ": " + text);
                    }
                    case WHISPERCHAT -> System.out.println("[Whisper] " + message.content());

                    case INFO -> System.out.println("[INFO] " + message.content());
                    case ERROR -> System.out.println("[ERROR] " + message.content());
                    case PLAYERS -> {
                        if (message.content().isBlank()) {
                            System.out.println("[PLAYERS] none");
                        } else {
                            System.out.println("[PLAYERS] " + message.content());
                        }
                    }
                    case ALLPLAYERS -> {
                        if (message.content().isBlank()) {
                            System.out.println("[ALLPLAYERS] none");
                        } else {
                            System.out.println("[ALLPLAYERS] " + message.content());
                        }
                    }
                    case LOBBIES -> {
                        if (message.content().isBlank()) {
                            System.out.println("[LOBBIES] none");
                        } else {
                            System.out.println("[LOBBIES] " + message.content());
                        }
                    }
                    case GAME -> System.out.println("[GAME] " + message.content());
                    case NAME, START, QUIT, UNKNOWN ->
                            System.out.println("[CLIENT] Unexpected server message: " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("Connection closed: " + e.getMessage());
        }
    }
}