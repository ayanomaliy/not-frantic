package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages server-side game state, client connections, and message handling.
 *
 * <p>This service supports multiple lobbies. A client may connect to the server,
 * choose a unique player name, then create a new lobby or join an existing one.
 * Chat messages, player lists, and start requests are handled per lobby.</p>
 *
 * <p>This class uses synchronized methods to ensure thread-safe access to the
 * shared lobby and client state.</p>
 */
public class ServerService {

    /** All existing lobbies mapped by their lobby id. */
    private final Map<String, Lobby> lobbies = new HashMap<>();

    /** Maps each connected client session to the id of its current lobby. */
    private final Map<ClientSession, String> playerLobbyMap = new HashMap<>();

    private final List<ClientSession> connectedClients = new ArrayList<>();



    /**
     * Logs a message to the console with a server prefix.
     *
     * @param message the message to log
     */
    private void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    /**
     * Returns the existing lobby with the given id or creates it if absent.
     *
     * @param lobbyId the lobby id
     * @return the existing or newly created lobby
     */
    private Lobby getOrCreateLobby(String lobbyId) {
        return lobbies.computeIfAbsent(lobbyId, Lobby::new);
    }

    /**
     * Returns the lobby of the given client session.
     *
     * @param session the client session
     * @return the lobby of the session, or {@code null} if none is assigned
     */
    private Lobby getLobbyOf(ClientSession session) {
        String lobbyId = playerLobbyMap.get(session);
        return lobbyId == null ? null : lobbies.get(lobbyId);
    }

    /**
     * Sends a structured message to all clients inside the given lobby.
     *
     * @param lobby the target lobby
     * @param message the message to broadcast
     */
    private void broadcastToLobby(Lobby lobby, Message message) {
        log("Broadcast to lobby " + lobby.getLobbyId() + ": " + message.encode());
        for (ClientSession session : lobby.getSessions()) {
            session.send(message.encode());
        }
    }

    /**
     * Broadcasts the current player list to all players inside the given lobby.
     *
     * @param lobby the target lobby
     */
    private void broadcastPlayerList(Lobby lobby) {
        List<String> names = new ArrayList<>();
        for (Player player : lobby.getPlayers()) {
            names.add(player.name());
        }

        broadcastToLobby(lobby, new Message(
                Message.Type.PLAYERS,
                String.join(",", names)
        ));
    }

    /**
     * Broadcasts a structured message to all connected clients.
     *
     * @param message the message to broadcast
     */
    private void broadcastToAllClients(Message message) {
        log("Broadcast to all clients: " + message.encode());
        for (ClientSession session : connectedClients) {
            session.send(message.encode());
        }
    }

    /**
     * Removes the client from its current lobby, if any.
     *
     * <p>The remaining players in that lobby are informed about the departure,
     * the player list is refreshed, and the lobby is deleted if it becomes empty.</p>
     *
     * @param session the client session leaving its current lobby
     */
    private void leaveCurrentLobby(ClientSession session) {
        Lobby currentLobby = getLobbyOf(session);

        if (currentLobby == null) {
            return;
        }

        currentLobby.removeSession(session);
        playerLobbyMap.remove(session);

        broadcastToLobby(currentLobby, new Message(
                Message.Type.INFO,
                session.getPlayerName() + " left the lobby."
        ));

        broadcastPlayerList(currentLobby);

        if (currentLobby.getPlayerCount() == 0) {
            lobbies.remove(currentLobby.getLobbyId());
            log("Removed empty lobby: " + currentLobby.getLobbyId());
            broadcastLobbyListToAllClients();
        }
    }

    /**
     * Registers a new client connection.
     *
     * <p>New clients connect to the server without being placed into a lobby
     * automatically. They must explicitly create or join one.</p>
     *
     * @param session the client session to register
     */
    public synchronized void registerClient(ClientSession session) {
        connectedClients.add(session);

        log("Client connected without lobby yet: " + session.getPlayerName());

        session.send(new Message(
                Message.Type.INFO,
                "Connected to server. Use /create <lobby> or /join <lobby>."
        ).encode());

        sendLobbyList(session);
        sendAllPlayersList(session);
        broadcastAllPlayersListToAllClients();
    }

    /**
     * Unregisters a disconnected client.
     *
     * <p>If the client is currently inside a lobby, it is removed from that lobby.
     * If the client is not in any lobby, only a log entry is written.</p>
     *
     * @param session the client session to unregister
     */
    public synchronized void unregisterClient(ClientSession session) {
        connectedClients.remove(session);

        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            log("Client disconnected without lobby: " + session.getPlayerName());
            return;
        }

        lobby.removeSession(session);
        playerLobbyMap.remove(session);

        log("Client disconnected: " + session.getPlayerName()
                + ". Remaining clients in lobby " + lobby.getLobbyId() + ": " + lobby.getPlayerCount());

        broadcastToLobby(lobby, new Message(
                Message.Type.INFO,
                session.getPlayerName() + " disconnected."
        ));

        broadcastPlayerList(lobby);

        if (lobby.getPlayerCount() == 0) {
            lobbies.remove(lobby.getLobbyId());
            log("Removed empty lobby: " + lobby.getLobbyId());
        }

        broadcastLobbyListToAllClients();
        broadcastAllPlayersListToAllClients();
    }

    /**
     * Handles a global chat message from a client.
     *
     * <p>Global chat is broadcast to all connected clients, regardless of lobby.</p>
     *
     * @param session the client session sending the message
     * @param text the chat message content
     */
    private void handleGlobalChat(ClientSession session, String text) {
        if (text == null || text.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Global chat message cannot be empty."
            ).encode());
            return;
        }

        String cleanText = text.trim();

        if (cleanText.length() > 200) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Global chat message is too long. Maximum 200 characters."
            ).encode());
            return;
        }

        if (cleanText.contains("\n") || cleanText.contains("\r")) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Global chat message contains invalid characters."
            ).encode());
            return;
        }

        broadcastToAllClients(new Message(
                Message.Type.GLOBALCHAT,
                session.getPlayerName() + "|" + cleanText
        ));
    }

    private void handleLobbyChat(ClientSession session, String text) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You are not in a lobby."
            ).encode());
            return;
        }

        if (text == null || text.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby chat message cannot be empty."
            ).encode());
            return;
        }

        String cleanText = text.trim();

        if (cleanText.length() > 200) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby chat message is too long. Maximum 200 characters."
            ).encode());
            return;
        }

        if (cleanText.contains("\n") || cleanText.contains("\r")) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby chat message contains invalid characters."
            ).encode());
            return;
        }

        broadcastToLobby(lobby, new Message(
                Message.Type.LOBBYCHAT,
                session.getPlayerName() + "|" + cleanText
        ));
    }

    /**
     * Handles a whisper chat message from a client.
     *
     * <p>The payload must have the form {@code targetName|message text}.
     * The server delivers the message privately to the target player and
     * also sends a confirmation copy back to the sender.</p>
     *
     * @param session the sending client session
     * @param payload the whisper payload
     */
    private void handleWhisperChat(ClientSession session, String payload) {
        if (payload == null || payload.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Whisper message cannot be empty."
            ).encode());
            return;
        }

        String[] parts = payload.split("\\|", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Usage: /whisper <player> <message>"
            ).encode());
            return;
        }

        String targetName = parts[0].trim();
        String text = parts[1].trim();

        if (text.length() > 200) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Whisper message is too long. Maximum 200 characters."
            ).encode());
            return;
        }

        if (text.contains("\n") || text.contains("\r")) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Whisper message contains invalid characters."
            ).encode());
            return;
        }

        ClientSession target = findConnectedClientByName(targetName);

        if (target == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Player not found: " + targetName
            ).encode());
            return;
        }

        if (target == session) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You cannot whisper to yourself."
            ).encode());
            return;
        }

        target.send(new Message(
                Message.Type.WHISPERCHAT,
                "FROM|" + session.getPlayerName() + "|" + text
        ).encode());

        session.send(new Message(
                Message.Type.WHISPERCHAT,
                "TO|" + target.getPlayerName() + "|" + text
        ).encode());
    }


    /**
     * Finds a connected client session by player name.
     *
     * @param name the player name to search for
     * @return the matching client session, or {@code null} if not found
     */
    private ClientSession findConnectedClientByName(String name) {
        for (ClientSession client : connectedClients) {
            if (client.getPlayerName().equalsIgnoreCase(name)) {
                return client;
            }
        }
        return null;
    }


    /**
     * Processes an incoming message from a client.
     *
     * <p>The message is validated and then routed to the corresponding handler
     * based on its type.</p>
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
            case GLOBALCHAT -> handleGlobalChat(session, message.content());
            case LOBBYCHAT -> handleLobbyChat(session, message.content());
            case WHISPERCHAT -> handleWhisperChat(session, message.content());
            case CREATE -> handleCreateLobby(session, message.content());
            case JOIN -> handleJoinLobby(session, message.content());
            case LOBBIES -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "LOBBIES must not contain content."
                    ).encode());
                    return;
                }
                sendLobbyList(session);
            }

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

            case ALLPLAYERS -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "ALLPLAYERS must not contain content."
                    ).encode());
                    return;
                }
                handleAllPlayers(session);
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
                log("Heartbeat message reached ServerService from "
                        + session.getPlayerName() + ": " + message.type());
            }

            case INFO, ERROR, GAME -> session.send(new Message(
                    Message.Type.ERROR,
                    "Client may not send server-only message types."
            ).encode());

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
     * Handles a request to create a new lobby and join it.
     *
     * @param session the client session creating the lobby
     * @param lobbyId the requested lobby id
     */
    private void handleCreateLobby(ClientSession session, String lobbyId) {
        if (lobbyId == null || lobbyId.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby name cannot be empty."
            ).encode());
            return;
        }

        String cleanLobbyId = lobbyId.trim();

        if (cleanLobbyId.length() > 20) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby name is too long. Maximum 20 characters."
            ).encode());
            return;
        }

        if (cleanLobbyId.contains("|") || cleanLobbyId.contains(",")) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby name contains invalid characters."
            ).encode());
            return;
        }

        if (lobbies.containsKey(cleanLobbyId)) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby already exists."
            ).encode());
            return;
        }

        leaveCurrentLobby(session);

        Lobby newLobby = getOrCreateLobby(cleanLobbyId);
        newLobby.addSession(session);
        playerLobbyMap.put(session, cleanLobbyId);

        session.send(new Message(
                Message.Type.INFO,
                "Lobby created and joined: " + cleanLobbyId
        ).encode());

        broadcastToLobby(newLobby, new Message(
                Message.Type.INFO,
                session.getPlayerName() + " joined the lobby."
        ));

        broadcastPlayerList(newLobby);
        broadcastLobbyListToAllClients();
    }

    /**
     * Handles a request to join an existing lobby.
     *
     * @param session the client session joining the lobby
     * @param lobbyId the lobby id to join
     */
    private void handleJoinLobby(ClientSession session, String lobbyId) {
        if (lobbyId == null || lobbyId.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby name cannot be empty."
            ).encode());
            return;
        }

        String cleanLobbyId = lobbyId.trim();

        String currentLobbyId = playerLobbyMap.get(session);
        if (currentLobbyId != null && currentLobbyId.equals(cleanLobbyId)) {
            session.send(new Message(
                    Message.Type.INFO,
                    "You are already in lobby: " + cleanLobbyId
            ).encode());
            return;
        }

        Lobby lobby = lobbies.get(cleanLobbyId);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby does not exist."
            ).encode());
            return;
        }

        leaveCurrentLobby(session);

        lobby.addSession(session);
        playerLobbyMap.put(session, cleanLobbyId);

        session.send(new Message(
                Message.Type.INFO,
                "Joined lobby: " + cleanLobbyId
        ).encode());

        broadcastToLobby(lobby, new Message(
                Message.Type.INFO,
                session.getPlayerName() + " joined the lobby."
        ));

        broadcastPlayerList(lobby);
    }

    private void sendLobbyList(ClientSession session) {
        List<String> lobbyNames = new ArrayList<>(lobbies.keySet());
        session.send(new Message(
                Message.Type.LOBBIES,
                String.join(",", lobbyNames)
        ).encode());
    }

    private void sendAllPlayersList(ClientSession session) {
        List<String> names = new ArrayList<>();
        for (ClientSession client : connectedClients) {
            names.add(client.getPlayerName());
        }

        session.send(new Message(
                Message.Type.ALLPLAYERS,
                String.join(",", names)
        ).encode());
    }

    private void broadcastAllPlayersListToAllClients() {
        for (ClientSession client : connectedClients) {
            sendAllPlayersList(client);
        }
    }


    private void broadcastLobbyListToAllClients() {
        for (ClientSession client : connectedClients) {
            sendLobbyList(client);
        }
    }

    /**
     * Handles a name change request from a client.
     *
     * <p>A player may choose or change a name even before joining a lobby.
     * If the player is already inside a lobby, the lobby is informed about
     * the new or changed name and its player list is refreshed.</p>
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
        }

        Lobby lobby = getLobbyOf(session);
        if (lobby != null) {
            if (firstTimeNaming) {
                broadcastToLobby(lobby, new Message(
                        Message.Type.INFO,
                        uniqueName + " joined the lobby."
                ));
            } else {
                broadcastToLobby(lobby, new Message(
                        Message.Type.INFO,
                        oldName + " changed their name to " + uniqueName
                ));
            }

            broadcastPlayerList(lobby);
        }
        broadcastAllPlayersListToAllClients();
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
     * Creates a unique player name across all lobbies.
     *
     * <p>If the requested name is already taken, the server appends
     * "(2)", "(3)", and so on until a free name is found.</p>
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
     * @return {@code true} if the name is already taken, otherwise {@code false}
     */
    private boolean isNameTaken(String name, ClientSession currentSession) {
        for (ClientSession session : connectedClients) {
            if (session != currentSession && session.getPlayerName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Handles a request to list all connected players in the current lobby.
     *
     * @param session the client session requesting the player list
     */
    private void handlePlayers(ClientSession session) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You are not in a lobby."
            ).encode());
            return;
        }

        List<String> names = new ArrayList<>();
        for (Player player : lobby.getPlayers()) {
            names.add(player.name());
        }

        log(session.getPlayerName() + " requested player list in lobby "
                + lobby.getLobbyId() + ": " + names);

        session.send(new Message(
                Message.Type.PLAYERS,
                String.join(",", names)
        ).encode());
    }

    /**
     * Handles a request to list all connected players on the server.
     *
     * @param session the client session requesting the global player list
     */
    private void handleAllPlayers(ClientSession session) {
        List<String> names = new ArrayList<>();
        for (ClientSession client : connectedClients) {
            names.add(client.getPlayerName());
        }

        log(session.getPlayerName() + " requested global player list: " + names);

        session.send(new Message(
                Message.Type.ALLPLAYERS,
                String.join(",", names)
        ).encode());
    }

    /**
     * Handles a game start request from a client.
     *
     * <p>The request is only valid inside a lobby. At least two players must
     * be present in that lobby. The gameStarted state is now stored per lobby.</p>
     *
     * @param session the client session requesting to start the game
     */
    private void handleStart(ClientSession session) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You are not in a lobby."
            ).encode());
            return;
        }

        log(session.getPlayerName() + " requested game start in lobby " + lobby.getLobbyId() + ".");

        // 🔥 FIX: use lobby-specific gameStarted
        if (lobby.isGameStarted()) {
            log("Start rejected: game already started in lobby " + lobby.getLobbyId() + ".");
            session.send(new Message(
                    Message.Type.ERROR,
                    "Game already started in this lobby."
            ).encode());
            return;
        }

        if (lobby.getPlayerCount() < 2) {
            log("Start rejected in lobby " + lobby.getLobbyId() + ": not enough players.");
            session.send(new Message(
                    Message.Type.ERROR,
                    "Need at least 2 players in the lobby to start."
            ).encode());
            return;
        }

        // 🔥 FIX: set gameStarted per lobby
        lobby.setGameStarted(true);

        log("Game started in lobby " + lobby.getLobbyId()
                + " with " + lobby.getPlayerCount() + " players.");

        broadcastToLobby(lobby, new Message(
                Message.Type.GAME,
                "Starting prototype game..."
        ));
        broadcastToLobby(lobby, new Message(
                Message.Type.GAME,
                "Connected players in lobby: " + lobby.getPlayerCount()
        ));
        broadcastToLobby(lobby, new Message(
                Message.Type.GAME,
                "This is where actual game initialization will go."
        ));
    }
}