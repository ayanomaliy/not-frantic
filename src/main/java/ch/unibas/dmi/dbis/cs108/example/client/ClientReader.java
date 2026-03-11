package ch.unibas.dmi.dbis.cs108.example.client;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

public class ClientReader implements Runnable {

    private final BufferedReader serverIn;
    private final PrintWriter serverOut;
    private final AtomicLong lastServerPongTime;

    public ClientReader(BufferedReader serverIn, PrintWriter serverOut, AtomicLong lastServerPongTime) {
        this.serverIn = serverIn;
        this.serverOut = serverOut;
        this.lastServerPongTime = lastServerPongTime;
    }

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
                    case CHAT -> {
                        String[] parts = message.splitChatPayload();
                        String sender = parts[0];
                        String text = parts[1];
                        System.out.println(sender + ": " + text);
                    }
                    case INFO -> System.out.println("[INFO] " + message.content());
                    case ERROR -> System.out.println("[ERROR] " + message.content());
                    case PLAYERS -> {
                        if (message.content().isBlank()) {
                            System.out.println("[PLAYERS] none");
                        } else {
                            System.out.println("[PLAYERS] " + message.content());
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