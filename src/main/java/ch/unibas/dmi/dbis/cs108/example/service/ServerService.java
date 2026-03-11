package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages server-side game state, client connections, and message handling.
 *
 * <p>Handles client registration and unregistration, processes incoming player
 * commands, manages the lobby of connected players, and broadcasts structured
 * protocol messages to all clients. This class is thread-safe through
 * synchronized methods to handle concurrent client access.
 */
public class ServerService {

    private final Lobby lobby = new Lobby();
    private boolean gameStarted = false;

    /**
     * Logs a message to the console with a server prefix.
     *
     * @param message the message to log
     */
    private void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    /**
     * Registers a new client connection.
     *
     * <p>Adds the client session to the lobby and notifies all clients of the new connection.
     *
     * @param session the client session to register
     */
    public synchronized void registerClient(ClientSession session) {
        lobby.addSession(session);
        log("Client connected. Total clients: " + lobby.getPlayerCount());

        session.send(new Message(
                Message.Type.INFO,
                "Connected. Current players: " + lobby.getPlayerCount()
        ).encode());

        broadcast(new Message(
                Message.Type.INFO,
                "A new client connected."
        ));
    }

    /**
     * Unregisters a disconnected client.
     *
     * <p>Removes the client session from the lobby and notifies remaining clients
     * of the disconnection.
     *
     * @param session the client session to unregister
     */
    public synchronized void unregisterClient(ClientSession session) {
        lobby.removeSession(session);
        log("Client disconnected: " + session.getPlayerName()
                + ". Remaining clients: " + lobby.getPlayerCount());

        broadcast(new Message(
                Message.Type.INFO,
                session.getPlayerName() + " disconnected."
        ));
    }

    /**
     * Processes an incoming message from a client.
     *
     * <p>Routes the message to the appropriate handler based on message type.
     *
     * @param session the client session sending the message
     * @param message the message to process
     */
    public synchronized void handleMessage(ClientSession session, Message message) {
        if (message == null || !message.hasValidStructure()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Malformed protocol message."
            ).encode());
            return;
        }

        log("Received from " + session.getPlayerName() + ": " + message.type()
                + (message.content().isBlank() ? "" : " -> " + message.content()));

        switch (message.type()) {
            case NAME -> handleName(session, message.content());
            case CHAT -> handleChat(session, message.content());
            case PLAYERS -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "PLAYERS must not contain content."
                    ).encode());
                    return;
                }
                handlePlayers(session);
            }
            case START -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "START must not contain content."
                    ).encode());
                    return;
                }
                handleStart(session);
            }
            case QUIT -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "QUIT must not contain content."
                    ).encode());
                    return;
                }
                log(session.getPlayerName() + " requested quit.");
                session.send(new Message(
                        Message.Type.INFO,
                        "Goodbye."
                ).encode());
            }
            case PING, PONG -> {
                // Heartbeat messages are handled in ClientSession / ClientReader.
                // They should normally not reach ServerService.
                log("Heartbeat message reached ServerService from " + session.getPlayerName()
                        + ": " + message.type());
            }
            case INFO, ERROR, GAME -> {
                session.send(new Message(
                        Message.Type.ERROR,
                        "Client may not send server-only message types."
                ).encode());
            }
            case UNKNOWN -> {
                log("Unknown command from " + session.getPlayerName() + ": " + message.content());
                session.send(new Message(
                        Message.Type.ERROR,
                        "Unknown command."
                ).encode());
            }
        }
    }

    /**
     * Handles a name change request from a client.
     *
     * @param session the client session requesting the name change
     * @param name the new name requested
     */
    private void handleName(ClientSession session, String name) {
        if (name == null || name.isBlank()) {
            log("Rejected empty name from " + session.getPlayerName());
            session.send(new Message(
                    Message.Type.ERROR,
                    "Name cannot be empty."
            ).encode());
            return;
        }

        String newName = name.trim();
        String oldName = session.getPlayerName();
        boolean firstTimeNaming = oldName.equals("Anonymous");

        if (newName.length() > 20) {
            log("Rejected too long name from " + oldName + ": " + newName);
            session.send(new Message(
                    Message.Type.ERROR,
                    "Name is too long. Maximum 20 characters."
            ).encode());
            return;
        }

        if (newName.contains("|") || newName.contains(",")) {
            log("Rejected invalid characters in name from " + oldName + ": " + newName);
            session.send(new Message(
                    Message.Type.ERROR,
                    "Name contains invalid characters."
            ).encode());
            return;
        }

        if (!oldName.equals("Anonymous") && oldName.equalsIgnoreCase(newName)) {
            log("Rejected same name from " + oldName);
            session.send(new Message(
                    Message.Type.ERROR,
                    "You already have this name."
            ).encode());
            return;
        }

        String uniqueName = makeUniqueName(newName, session);
        session.setPlayerName(uniqueName);

        if (firstTimeNaming) {
            log("Player set initial name: " + oldName + " -> " + uniqueName);

            if (uniqueName.equals(extractBaseName(newName)) || uniqueName.equals(newName)) {
                session.send(new Message(
                        Message.Type.INFO,
                        "Your name has been set to " + uniqueName
                ).encode());
            } else {
                session.send(new Message(
                        Message.Type.INFO,
                        "Your requested name was taken. Your name has been set to " + uniqueName
                ).encode());
            }

            broadcast(new Message(
                    Message.Type.INFO,
                    uniqueName + " joined the lobby."
            ));
        } else {
            log("Player renamed: " + oldName + " -> " + uniqueName);

            if (uniqueName.equals(extractBaseName(newName)) || uniqueName.equals(newName)) {
                session.send(new Message(
                        Message.Type.INFO,
                        "Your name is now " + uniqueName
                ).encode());
            } else {
                session.send(new Message(
                        Message.Type.INFO,
                        "Your requested name was taken. Your name is now " + uniqueName
                ).encode());
            }

            broadcast(new Message(
                    Message.Type.INFO,
                    oldName + " changed their name to " + uniqueName
            ));
        }
    }

    /**
     * Removes a trailing numeric suffix like "(2)" from a name.
     *
     * @param name the player name
     * @return the base name without trailing suffix
     */
    private String extractBaseName(String name) {
        return name.replaceAll("\\(\\d+\\)$", "").trim();
    }

    /**
     * Creates a unique player name.
     *
     * <p>If the requested name is already taken, the server appends
     * "(2)", "(3)", and so on until a free name is found.
     *
     * @param requestedName the requested player name
     * @param currentSession the client requesting the name
     * @return a unique player name
     */
    private String makeUniqueName(String requestedName, ClientSession currentSession) {
        String baseName = extractBaseName(requestedName);

        if (!isNameTaken(baseName, currentSession)) {
            return baseName;
        }

        int counter = 2;
        String candidate;

        do {
            candidate = baseName + "(" + counter + ")";
            counter++;
        } while (isNameTaken(candidate, currentSession));

        return candidate;
    }

    /**
     * Checks whether a player name is already used by another session.
     *
     * @param name the name to check
     * @param currentSession the current client session
     * @return true if the name is already taken, false otherwise
     */
    private boolean isNameTaken(String name, ClientSession currentSession) {
        for (ClientSession session : lobby.getSessions()) {
            if (session != currentSession && session.getPlayerName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a chat message from a client.
     *
     * @param session the client session sending the chat
     * @param text the chat message content
     */
    private void handleChat(ClientSession session, String text) {
        if (text == null || text.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Chat message cannot be empty."
            ).encode());
            return;
        }

        String cleanText = text.trim();

        if (cleanText.length() > 200) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Chat message is too long. Maximum 200 characters."
            ).encode());
            return;
        }

        if (cleanText.contains("\n") || cleanText.contains("\r")) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Chat message contains invalid characters."
            ).encode());
            return;
        }

        broadcast(new Message(
                Message.Type.CHAT,
                session.getPlayerName() + "|" + cleanText
        ));
    }

    /**
     * Handles a request to list all connected players.
     *
     * @param session the client session requesting the player list
     */
    private void handlePlayers(ClientSession session) {
        List<String> names = new ArrayList<>();
        for (Player player : lobby.getPlayers()) {
            names.add(player.name());
        }

        log(session.getPlayerName() + " requested player list: " + names);

        session.send(new Message(
                Message.Type.PLAYERS,
                String.join(",", names)
        ).encode());
    }

    /**
     * Handles a game start request from a client.
     *
     * <p>Validates that the game has not already started and that there are enough players.
     * Broadcasts game start information to all connected clients.
     *
     * @param session the client session requesting to start the game
     */
    private void handleStart(ClientSession session) {
        log(session.getPlayerName() + " requested game start.");

        if (gameStarted) {
            log("Start rejected: game already started.");
            session.send(new Message(
                    Message.Type.ERROR,
                    "Game already started."
            ).encode());
            return;
        }

        if (lobby.getPlayerCount() < 2) {
            log("Start rejected: not enough players.");
            session.send(new Message(
                    Message.Type.ERROR,
                    "Need at least 2 players to start."
            ).encode());
            return;
        }

        gameStarted = true;
        log("Game started with " + lobby.getPlayerCount() + " players.");

        broadcast(new Message(
                Message.Type.GAME,
                "Starting prototype game..."
        ));
        broadcast(new Message(
                Message.Type.GAME,
                "Connected players: " + lobby.getPlayerCount()
        ));
        broadcast(new Message(
                Message.Type.GAME,
                "This is where actual game initialization will go."
        ));
    }

    /**
     * Sends a structured message to all connected clients.
     *
     * @param message the message to broadcast
     */
    private void broadcast(Message message) {
        log("Broadcast: " + message.encode());
        for (ClientSession session : lobby.getSessions()) {
            session.send(message.encode());
        }
    }
}