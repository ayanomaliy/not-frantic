package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages server-side game state, client connections, and message handling.
 * <p>
 * Handles client registration/unregistration, processes incoming player commands,
 * manages the lobby of connected players, and broadcasts messages to all clients.
 * This class is thread-safe through synchronized methods to handle concurrent client access.
 * </p>
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
     * <p>
     * Adds the client session to the lobby and notifies all clients of the new connection.
     * </p>
     *
     * @param session the client session to register
     */
    public synchronized void registerClient(ClientSession session) {
        lobby.addSession(session);
        log("Client connected. Total clients: " + lobby.getPlayerCount());

        session.send("INFO Connected. Current players: " + lobby.getPlayerCount());
        broadcast("INFO A new client connected.");
    }

    /**
     * Unregisters a disconnected client.
     * <p>
     * Removes the client session from the lobby and notifies remaining clients
     * of the disconnection.
     * </p>
     *
     * @param session the client session to unregister
     */
    public synchronized void unregisterClient(ClientSession session) {
        lobby.removeSession(session);
        log("Client disconnected: " + session.getPlayerName()
                + ". Remaining clients: " + lobby.getPlayerCount());

        broadcast("INFO " + session.getPlayerName() + " disconnected.");
    }

    /**
     * Processes an incoming message from a client.
     * <p>
     * Routes the message to the appropriate handler based on message type.
     * </p>
     *
     * @param session the client session sending the message
     * @param message the message to process
     */
    public synchronized void handleMessage(ClientSession session, Message message) {
        log("Received from " + session.getPlayerName() + ": " + message.type()
                + (message.content().isBlank() ? "" : " -> " + message.content()));

        switch (message.type()) {
            case NAME -> handleName(session, message.content());
            case CHAT -> handleChat(session, message.content());
            case PLAYERS -> handlePlayers(session);
            case START -> handleStart(session);
            case QUIT -> {
                log(session.getPlayerName() + " requested quit.");
                session.send("INFO Goodbye.");
            }
            case UNKNOWN -> {
                log("Unknown command from " + session.getPlayerName() + ": " + message.content());
                session.send("ERROR Unknown command.");
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
            session.send("ERROR Name cannot be empty.");
            return;
        }

        log("Player renamed: " + session.getPlayerName() + " -> " + name);
        session.send("INFO Name set to " + name);
        broadcast("INFO " + name + " joined the lobby.");
    }

    /**
     * Handles a chat message from a client.
     *
     * @param session the client session sending the chat
     * @param text the chat message content
     */
    private void handleChat(ClientSession session, String text) {
        log("Chat from " + session.getPlayerName() + ": " + text);
        broadcast("CHAT [" + session.getPlayerName() + "]: " + text);
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
        session.send("PLAYERS " + String.join(", ", names));
    }

    /**
     * Handles a game start request from a client.
     * <p>
     * Validates that the game has not already started and that there are enough players.
     * Broadcasts game start information to all connected clients.
     * </p>
     *
     * @param session the client session requesting to start the game
     */
    private void handleStart(ClientSession session) {
        log(session.getPlayerName() + " requested game start.");

        if (gameStarted) {
            log("Start rejected: game already started.");
            session.send("ERROR Game already started.");
            return;
        }

        if (lobby.getPlayerCount() < 2) {
            log("Start rejected: not enough players.");
            session.send("ERROR Need at least 2 players to start.");
            return;
        }

        gameStarted = true;
        log("Game started with " + lobby.getPlayerCount() + " players.");

        broadcast("GAME Starting prototype game...");
        broadcast("GAME Connected players: " + lobby.getPlayerCount());
        broadcast("GAME This is where actual game initialization will go.");
    }

    /**
     * Sends a message to all connected clients.
     *
     * @param text the message to broadcast
     */
    private void broadcast(String text) {
        log("Broadcast: " + text);
        for (ClientSession session : lobby.getSessions()) {
            session.send(text);
        }
    }
}