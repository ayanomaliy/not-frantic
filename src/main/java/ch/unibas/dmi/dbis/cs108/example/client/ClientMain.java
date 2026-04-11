package ch.unibas.dmi.dbis.cs108.example.client;

import ch.unibas.dmi.dbis.cs108.example.client.net.ClientMessageHandler;
import ch.unibas.dmi.dbis.cs108.example.client.net.ClientProtocolClient;
import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Entry point of the terminal client application.
 *
 * <p>This version uses the same shared protocol client as the GUI and only
 * differs in how it presents incoming messages and reads user input.</p>
 */
public class ClientMain {

    /**
     * Starts the terminal client.
     *
     * @param args command-line arguments in the form {@code <host:port> [username]}
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

        ClientMessageHandler handler = new ClientMessageHandler() {
            @Override
            public void onMessage(Message message) {
                switch (message.type()) {
                    case GLOBALCHAT -> {
                        String[] parts = message.splitChatPayload();
                        System.out.println("[Global] " + parts[0] + ": " + parts[1]);
                    }
                    case LOBBYCHAT -> {
                        String[] parts = message.splitChatPayload();
                        System.out.println("[Lobby] " + parts[0] + ": " + parts[1]);
                    }
                    case WHISPERCHAT -> System.out.println("[Whisper] " + message.content());
                    case INFO -> System.out.println("[INFO] " + message.content());
                    case ERROR -> System.out.println("[ERROR] " + message.content());
                    case PLAYERS -> System.out.println(
                            message.content().isBlank()
                                    ? "[PLAYERS] none"
                                    : "[PLAYERS] " + message.content()
                    );
                    case ALLPLAYERS -> System.out.println(
                            message.content().isBlank()
                                    ? "[ALLPLAYERS] none"
                                    : "[ALLPLAYERS] " + message.content()
                    );
                    case LOBBIES -> System.out.println(
                            message.content().isBlank()
                                    ? "[LOBBIES] none"
                                    : "[LOBBIES] " + message.content()
                    );
                    case GAME -> System.out.println("[GAME] " + message.content());
                    case HAND_UPDATE -> System.out.println("[HAND] " + message.content());
                    case GAME_STATE -> System.out.println("[GAME_STATE] " + message.content());
                    case EFFECT_REQUEST -> System.out.println("[EFFECT_REQUEST] " + message.content());
                    case ROUND_END -> System.out.println("[ROUND_END] " + message.content());
                    case GAME_END -> System.out.println("[GAME_END] " + message.content());
                    case BROADCAST -> {
                        String[] parts = message.splitChatPayload();
                        System.out.println("[INFO] [Broadcast] " + parts[0] + ": " + parts[1]);
                    }
                    default -> System.out.println("[CLIENT] Unexpected server message: " + message.encode());
                }
            }

            @Override
            public void onLocalMessage(String text) {
                System.out.println(text);
            }

            @Override
            public void onDisconnected(String reason) {
                System.err.println(reason);
            }
        };

        ClientProtocolClient client = new ClientProtocolClient(handler);

        try (BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {
            client.connect(host, port);

            System.out.println("Connected to server at " + host + ":" + port);
            System.out.println("Using username: " + suggestedName);

            client.setName(suggestedName);

            String line;
            while ((line = userIn.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isBlank()) {
                    continue;
                }

                Message message = client.parseRawCommand(trimmed);

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

                client.send(message);

                if (message.type() == Message.Type.QUIT) {
                    client.disconnect(false);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}