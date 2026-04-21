package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.dev.DevModeManager;
import ch.unibas.dmi.dbis.cs108.example.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.LobbyStatus;
import ch.unibas.dmi.dbis.cs108.example.model.Player;
import ch.unibas.dmi.dbis.cs108.example.model.game.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.nio.file.Path;
import java.util.stream.Collectors;


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

    /** All currently connected client sessions. */
    private final List<ClientSession> connectedClients = new ArrayList<>();

    private final HighScoreHistory highScoreHistory =
            new HighScoreHistory(Path.of("highscores.txt"));

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
     * <p>This helper centralizes all lobby-leaving behavior. It removes the
     * session from the current lobby, clears the lobby mapping, broadcasts the
     * updated player list to the remaining lobby members, deletes the lobby if
     * it becomes empty and is not finished, and refreshes the global lobby list
     * for all clients.</p>
     *
     * <p>If {@code reasonText} is not blank, an informational message of the
     * form {@code "<playerName> <reasonText>"} is broadcast to the remaining
     * members of the old lobby. If {@code reasonText} is {@code null} or blank,
     * the leave happens silently apart from list updates.</p>
     *
     * @param session the client session leaving its current lobby
     * @param reasonText optional informational suffix such as
     *                   {@code "left the lobby."} or {@code "disconnected."}
     * @return the old lobby the client left, or {@code null} if the client was
     *         not in any lobby
     */
    private Lobby leaveCurrentLobby(ClientSession session, String reasonText) {
        Lobby currentLobby = getLobbyOf(session);

        if (currentLobby == null) {
            return null;
        }

        currentLobby.removeSession(session);
        playerLobbyMap.remove(session);

        if (reasonText != null && !reasonText.isBlank()) {
            broadcastToLobby(currentLobby, new Message(
                    Message.Type.INFO,
                    session.getPlayerName() + " " + reasonText
            ));
        }

        broadcastPlayerList(currentLobby);

        if (currentLobby.getPlayerCount() == 0
                && currentLobby.getLobbyStatus() != LobbyStatus.FINISHED) {
            lobbies.remove(currentLobby.getLobbyId());
            log("Removed empty lobby: " + currentLobby.getLobbyId());
        }

        broadcastLobbyListToAllClients();
        return currentLobby;
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
     * <p>If the client is currently inside a lobby, the centralized lobby-leave
     * helper is used so that player lists and lobby lists stay consistent.</p>
     *
     * @param session the client session to unregister
     */
    public synchronized void unregisterClient(ClientSession session) {
        connectedClients.remove(session);

        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            log("Client disconnected without lobby: " + session.getPlayerName());
            broadcastAllPlayersListToAllClients();
            return;
        }

        log("Client disconnected: " + session.getPlayerName()
                + ". Leaving lobby " + lobby.getLobbyId());

        leaveCurrentLobby(session, "disconnected.");
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

    /**
     * Handles a lobby chat message from a client.
     *
     * @param session the sending client session
     * @param text the chat message content
     */
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
            case BROADCAST -> handleBroadcast(session, message.content());

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

            case CHEATWIN -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "CHEATWIN must not contain content."
                    ).encode());
                    return;
                }
                handleCheatWin(session);
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

            case LEAVE -> {
                if (!message.content().isBlank()) {
                    session.send(new Message(
                            Message.Type.ERROR,
                            "LEAVE must not contain content."
                    ).encode());
                    return;
                }
                handleLeave(session);
            }

            case PLAY_CARD -> handlePlayCard(session, message.content());
            case DRAW_CARD -> handleDrawCard(session);
            case END_TURN -> handleEndTurn(session);
            case EFFECT_RESPONSE -> handleEffectResponse(session, message.content());
            case GET_HAND -> handleGetHand(session);
            case GET_GAME_STATE -> handleGetGameState(session);
            case GET_ROUND_END -> handleGetRoundEnd(session);
            case GET_GAME_END -> handleGetGameEnd(session);
            case GET_HIGHSCORES -> handleGetHighScores(session);

            case PING, PONG -> {
                log("Heartbeat message reached ServerService from "
                        + session.getPlayerName() + ": " + message.type());
            }

            case GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, ROUND_END, GAME_END ->
                    session.send(new Message(
                            Message.Type.ERROR,
                            "Client may not send server-only message types."
                    ).encode());

            case INFO, ERROR, GAME -> session.send(new Message(
                    Message.Type.ERROR,
                    "Client may not send server-only message types."
            ).encode());


            case DEV -> handleDevMode(session, message.content());

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

        leaveCurrentLobby(session, null);

        Lobby newLobby = getOrCreateLobby(cleanLobbyId);

        if (!newLobby.addSession(session)) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby is full."
            ).encode());
            return;
        }

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
     * <p>A client may only join a lobby if:
     * <ul>
     *     <li>The lobby exists</li>
     *     <li>The lobby is not full</li>
     *     <li>The game is not currently running</li>
     *     <li>The lobby is not finished</li>
     * </ul>
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

        if (lobby.getLobbyStatus() == LobbyStatus.PLAYING) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Cannot join: game is already running in this lobby."
            ).encode());
            return;
        }

        if (lobby.getLobbyStatus() == LobbyStatus.FINISHED) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Cannot join: this lobby has already finished."
            ).encode());
            return;
        }

        if (lobby.isFull()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Lobby is full (max 5 players)."
            ).encode());
            return;
        }

        // leave current lobby if needed
        leaveCurrentLobby(session, null);

        boolean added = lobby.addSession(session);
        if (!added) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Could not join the lobby."
            ).encode());
            return;
        }

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
        broadcastLobbyListToAllClients();
    }

    /**
     * Handles a request to leave the current lobby without disconnecting from
     * the server.
     *
     * <p>The leaving client receives a confirmation message, an explicit empty
     * {@code PLAYERS} update so GUI lobby-player lists clear immediately, and
     * refreshed global lists.</p>
     *
     * @param session the client session leaving its current lobby
     */
    private void handleLeave(ClientSession session) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You are not in a lobby."
            ).encode());
            return;
        }

        String oldLobbyId = lobby.getLobbyId();

        leaveCurrentLobby(session, "left the lobby.");

        session.send(new Message(
                Message.Type.INFO,
                "You left lobby: " + oldLobbyId
        ).encode());

        session.send(new Message(
                Message.Type.PLAYERS,
                ""
        ).encode());

        sendLobbyList(session);
        sendAllPlayersList(session);
    }

    /**
     * Sends the current list of lobby names to the given client.
     *
     * @param session the client receiving the lobby list
     */
    private void sendLobbyList(ClientSession session) {
        List<String> lobbyEntries = new ArrayList<>();

        for (Lobby lobby : lobbies.values()) {
            String entry = lobby.getLobbyId()
                    + ":" + lobby.getStatus()
                    + ":" + lobby.getPlayerCount()
                    + ":" + lobby.getMaxPlayers();

            lobbyEntries.add(entry);
        }

        session.send(new Message(
                Message.Type.LOBBIES,
                String.join(",", lobbyEntries)
        ).encode());
    }

    /**
     * Sends the current global player list to the given client.
     *
     * @param session the client receiving the global player list
     */
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

    /**
     * Broadcasts the global player list to all connected clients.
     */
    private void broadcastAllPlayersListToAllClients() {
        for (ClientSession client : connectedClients) {
            sendAllPlayersList(client);
        }
    }

    /**
     * Broadcasts the lobby list to all connected clients.
     */
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
     * be present in that lobby. The gameStarted state is stored per lobby.</p>
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

        if (lobby.getLobbyStatus() == LobbyStatus.FINISHED) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "This lobby has already finished and cannot be started again."
            ).encode());
            return;
        }

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

        lobby.setGameStarted(true);
        broadcastLobbyListToAllClients();
        int round = lobby.nextRound();

        List<String> playerNames = new ArrayList<>();
        for (Player p : lobby.getPlayers()) {
            playerNames.add(p.name());
        }

        GameState gameState = GameInitializer.initialize(
                playerNames, round, lobby.getCumulativeScores(), new Random());
        lobby.setGameState(gameState);



        //devmode:
        if (lobby.isDevModeEnabled()) {
            try {
                DevModeManager.applyIfEnabled(lobby);
                log("Applied dev mode scenario '" + lobby.getDevScenario()
                        + "' in lobby " + lobby.getLobbyId() + ".");
            } catch (Exception e) {
                lobby.setGameStarted(false);
                lobby.setGameState(null);
                lobby.setLobbyStatus(LobbyStatus.WAITING);

                session.send(new Message(
                        Message.Type.ERROR,
                        "Could not apply dev mode scenario '" + lobby.getDevScenario()
                                + "': " + e.getMessage()
                ).encode());
                broadcastLobbyListToAllClients();
                return;
            }
        }

        log("Game started in lobby " + lobby.getLobbyId()
                + " with " + lobby.getPlayerCount() + " players (round " + round + ").");

        broadcastGameState(lobby);
        broadcastAllHands(lobby);

        TurnEngine.startTurn(gameState);
        broadcastGameState(lobby);
    }

    /**
     * Handles a cheat command that instantly ends the current match
     * and declares the requesting player as the winner.
     *
     * @param session the client session triggering the cheat
     */
    private void handleCheatWin(ClientSession session) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You are not in a lobby."
            ).encode());
            return;
        }

        if (lobby.getGameState() == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "No active game."
            ).encode());
            return;
        }

        GameState state = lobby.getGameState();

        // calculate round scores from current hands
        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(state.getPlayerOrder(), state);

        // keep cumulative scores in the lobby
        for (PlayerGameState player : state.getPlayerOrder()) {
            lobby.getCumulativeScores().put(player.getPlayerName(), player.getTotalScore());
        }

        // build ROUND_END payload: player:roundPoints:totalPoints,...
        String roundEndPayload = state.getPlayerOrder().stream()
                .map(player -> {
                    int roundPoints = roundScores.getOrDefault(player.getPlayerName(), 0);
                    int totalPoints = player.getTotalScore();
                    return player.getPlayerName() + ":" + roundPoints + ":" + totalPoints;
                })
                .collect(java.util.stream.Collectors.joining(","));

        broadcastToLobby(lobby, new Message(
                Message.Type.ROUND_END,
                roundEndPayload
        ));

        // winner stays the cheat user if that is your intended cheat behavior
        String winner = session.getPlayerName();
        highScoreHistory.appendFinishedGame(
                lobby.getLobbyId(),
                winner,
                new LinkedHashMap<>(lobby.getCumulativeScores())
        );

        broadcastToLobby(lobby, new Message(
                Message.Type.GAME_END,
                winner
        ));

        state.setPhase(GamePhase.GAME_OVER);
        lobby.setGameState(null);
        lobby.setGameStarted(false);
        lobby.setLobbyStatus(LobbyStatus.FINISHED);

        broadcastLobbyListToAllClients();

        log("CHEATWIN used by " + winner + " in lobby " + lobby.getLobbyId());
    }

    /**
     * Handles a {@code GET_HAND} message: sends the requesting player their current hand.
     */
    private void handleGetHand(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }
        try {
            PlayerGameState p = lobby.getGameState().getPlayer(session.getPlayerName());
            session.send(new Message(Message.Type.HAND_UPDATE,
                    GameStateSerializer.serializeHand(p)).encode());
        } catch (IllegalArgumentException e) {
            session.send(new Message(Message.Type.ERROR, "You are not in this game.").encode());
        }
    }

    /**
     * Handles a {@code GET_GAME_STATE} message: sends the current public game state to the requesting client.
     */
    private void handleGetGameState(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }
        session.send(new Message(Message.Type.GAME_STATE,
                GameStateSerializer.serializePublicState(lobby.getGameState())).encode());
    }

    /**
     * Handles a {@code GET_ROUND_END} message: sends current round scores computed from the
     * active game state, falling back to cumulative scores if no game is running.
     *
     * <p>Scores are computed read-only (hand value sum) without mutating player state.</p>
     */
    private void handleGetRoundEnd(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null) {
            session.send(new Message(Message.Type.ERROR, "Not in a lobby.").encode());
            return;
        }
        GameState state = lobby.getGameState();
        if (state != null) {
            // Game is active: compute live hand-value sums read-only (do NOT call
            // ScoreCalculator.calculateRoundScores — it mutates player.addToTotalScore)
            Map<String, Integer> roundScores = new LinkedHashMap<>();
            for (PlayerGameState p : state.getPlayerOrder()) {
                int handValue = p.getHand().stream().mapToInt(Card::scoringValue).sum();
                roundScores.put(p.getPlayerName(), handValue);
            }
            String payload = GameStateSerializer.serializeRoundEnd(roundScores, state.getPlayerOrder());
            session.send(new Message(Message.Type.ROUND_END, payload).encode());
        } else if (!lobby.getCumulativeScores().isEmpty()) {
            // Game just ended: use last recorded cumulative scores
            String payload = GameStateSerializer.serializeRoundEnd(lobby.getCumulativeScores(), List.of());
            session.send(new Message(Message.Type.ROUND_END, payload).encode());
        } else {
            session.send(new Message(Message.Type.ERROR, "No round scores available.").encode());
        }
    }

    /**
     * Handles a {@code GET_GAME_END} message: sends the winner if the game is over,
     * or an error if it is still in progress.
     */
    private void handleGetGameEnd(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null) {
            session.send(new Message(Message.Type.ERROR, "Not in a lobby.").encode());
            return;
        }
        GameState state = lobby.getGameState();
        Map<String, Integer> scores = lobby.getCumulativeScores();
        if (scores.isEmpty()) {
            session.send(new Message(Message.Type.ERROR, "No game result available.").encode());
            return;
        }
        int maxScore = state != null ? state.getMaxScore() : 0;
        if (state != null && !ScoreCalculator.isGameOver(scores, maxScore)) {
            session.send(new Message(Message.Type.ERROR, "Game is not over yet.").encode());
            return;
        }
        session.send(new Message(Message.Type.GAME_END,
                ScoreCalculator.getWinner(scores)).encode());
    }

    /**
     * Handles a {@code PLAY_CARD} message: the acting player attempts to play one
     * card from their hand.
     *
     * <p>This method validates that a game is active, parses the requested card id,
     * checks that the card is currently in the acting player's hand, delegates the
     * actual play to {@link TurnEngine#playCard(GameState, String, Card)}, and then
     * broadcasts the resulting events, hand updates, and public game state.</p>
     *
     * <p>Follow-up handling depends on the resulting phase:</p>
     * <ul>
     *   <li>If the play ends the round, round-end handling is triggered.</li>
     *   <li>If the play flips an active event card from a black-card play, the event
     *       is resolved immediately on the server without additional client input.</li>
     *   <li>If the game is still waiting for one or more pending special-effect
     *       responses, an {@code EFFECT_REQUEST} is broadcast.</li>
     *   <li>If effect resolution finishes with no further pending effects, the phase
     *       is advanced back to {@link GamePhase#AWAITING_PLAY} so the turn can
     *       continue instead of getting stuck in
     *       {@link GamePhase#RESOLVING_EFFECT}.</li>
     *   <li>If the play advances the game to {@link GamePhase#TURN_START}, the next
     *       turn is started immediately.</li>
     * </ul>
     *
     * @param session the client session that sent the play request
     * @param payload the raw payload containing the card id to play
     */
    private void handlePlayCard(ClientSession session, String payload) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }

        int cardId = GameMessageParser.parsePlayCard(payload);
        if (cardId < 0) {
            session.send(new Message(Message.Type.ERROR, "Invalid card id.").encode());
            return;
        }

        GameState state = lobby.getGameState();
        String playerName = session.getPlayerName();

        Card card = state.getPlayer(playerName).getHand().stream()
                .filter(c -> c.id() == cardId)
                .findFirst()
                .orElse(null);

        if (card == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Card " + cardId + " not in your hand."
            ).encode());
            return;
        }

        List<GameEvent> events = TurnEngine.playCard(state, playerName, card);
        broadcastEvents(lobby, events);
        broadcastAllHands(lobby);

        if (state.getPhase() == GamePhase.ROUND_END) {
            broadcastGameState(lobby);
            handleRoundEnd(lobby);
            return;
        }

        if (state.getPhase() == GamePhase.RESOLVING_EFFECT) {
            Card eventCard = state.getActiveEventCard();

            if (eventCard != null) {
                // Event card from a BLACK play — resolve immediately, no client input needed
                List<GameEvent> eventEvents = EventResolver.resolve(eventCard, state);
                broadcastEvents(lobby, eventEvents);
                broadcastGameState(lobby);

                // Re-check phase after event resolution
                if (state.getPhase() == GamePhase.ROUND_END) {
                    handleRoundEnd(lobby);
                } else if (state.getPhase() == GamePhase.RESOLVING_EFFECT
                        && !state.getPendingEffects().isEmpty()) {
                    String nextEffect = state.getPendingEffects().peek().name();
                    broadcastToLobby(lobby, new Message(
                            Message.Type.EFFECT_REQUEST,
                            GameStateSerializer.serializeEffectRequest(nextEffect, playerName)
                    ));
                } else if (state.getPhase() == GamePhase.RESOLVING_EFFECT) {
                    state.setPhase(GamePhase.AWAITING_PLAY);
                    broadcastGameState(lobby);
                } else if (state.getPhase() == GamePhase.TURN_START) {
                    TurnEngine.startTurn(state);
                    broadcastGameState(lobby);
                }

                return;
            }

            if (!state.getPendingEffects().isEmpty()) {
                // Special card effect — send to client for resolution
                String effectName = state.getPendingEffects().peek().name();
                broadcastToLobby(lobby, new Message(
                        Message.Type.EFFECT_REQUEST,
                        GameStateSerializer.serializeEffectRequest(effectName, playerName)
                ));
                return;
            }

            // Safety fallback: do not remain stuck in RESOLVING_EFFECT if there is
            // nothing left to resolve.
            state.setPhase(GamePhase.AWAITING_PLAY);
            broadcastGameState(lobby);
            return;
        }

        if (state.getPhase() == GamePhase.TURN_START) {
            broadcastGameState(lobby);
            TurnEngine.startTurn(state);
            broadcastGameState(lobby);
            return;
        }

        broadcastGameState(lobby);
    }

    /**
     * Handles a {@code DRAW_CARD} message: the acting player draws one card.
     */
    private void handleDrawCard(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }

        GameState state = lobby.getGameState();
        String playerName = session.getPlayerName();

        List<GameEvent> events = TurnEngine.drawCard(state, playerName);
        broadcastEvents(lobby, events);
        broadcastAllHands(lobby);
        broadcastGameState(lobby);

        if (state.getPhase() == GamePhase.ROUND_END) {
            handleRoundEnd(lobby);
        }
    }

    /**
     * Handles an {@code END_TURN} message: the acting player explicitly ends
     * their turn after drawing without playing.
     */
    private void handleEndTurn(ClientSession session) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }

        GameState state = lobby.getGameState();
        String playerName = session.getPlayerName();

        if (!state.getCurrentPlayer().getPlayerName().equals(playerName)) {
            session.send(new Message(Message.Type.ERROR, "Not your turn.").encode());
            return;
        }

        if (state.getPhase() != GamePhase.AWAITING_PLAY) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Cannot end turn in phase " + state.getPhase()
            ).encode());
            return;
        }

        List<GameEvent> events = TurnEngine.endTurn(state);
        broadcastEvents(lobby, events);
        TurnEngine.startTurn(state);
        broadcastGameState(lobby);
    }

    /**
     * Handles a client's response to a pending special-effect request.
     *
     * <p>This method validates that a game is active, parses the effect-response
     * payload, resolves the effect, and broadcasts the resulting events and
     * updated game state to the lobby.</p>
     *
     * <p>Invalid effect arguments, such as an unknown target player or a selected
     * card that is not valid, must not terminate the client session. Instead,
     * the acting player receives an {@code ERROR} message and stays connected.</p>
     *
     * @param session the client session that sent the effect response
     * @param payload the raw effect-response payload
     */
    private void handleEffectResponse(ClientSession session, String payload) {
        Lobby lobby = getLobbyOf(session);
        if (lobby == null || lobby.getGameState() == null) {
            session.send(new Message(Message.Type.ERROR, "No active game.").encode());
            return;
        }

        GameState state = lobby.getGameState();
        String playerName = session.getPlayerName();

        final Object[] parsed;
        try {
            parsed = GameMessageParser.parseEffectResponse(payload, state, playerName);
        } catch (IllegalArgumentException e) {
            session.send(new Message(
                    Message.Type.ERROR,
                    e.getMessage()
            ).encode());
            return;
        }

        if (parsed == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Malformed effect response."
            ).encode());
            return;
        }

        String effectName = (String) parsed[0];
        EffectArgs args = (EffectArgs) parsed[1];

        final SpecialEffect effect;
        try {
            effect = SpecialEffect.valueOf(effectName);
        } catch (IllegalArgumentException e) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Unknown effect: " + effectName
            ).encode());
            return;
        }

        final List<GameEvent> events;
        try {
            events = EffectResolver.resolve(effect, state, playerName, args);
        } catch (IllegalArgumentException e) {
            session.send(new Message(
                    Message.Type.ERROR,
                    e.getMessage()
            ).encode());
            return;
        }

        broadcastEvents(lobby, events);
        broadcastAllHands(lobby);
        broadcastGameState(lobby);

        if (state.getPhase() == GamePhase.ROUND_END) {
            handleRoundEnd(lobby);
        } else if (state.getPhase() == GamePhase.RESOLVING_EFFECT
                && !state.getPendingEffects().isEmpty()) {
            String nextEffect = state.getPendingEffects().peek().name();
            String target = state.getPendingEffectTarget() != null
                    ? state.getPendingEffectTarget()
                    : playerName;

            broadcastToLobby(lobby, new Message(
                    Message.Type.EFFECT_REQUEST,
                    GameStateSerializer.serializeEffectRequest(nextEffect, target)
            ));
        } else if (state.getPhase() == GamePhase.RESOLVING_EFFECT) {
            state.setPhase(GamePhase.AWAITING_PLAY);
            broadcastGameState(lobby);
        } else if (state.getPhase() == GamePhase.TURN_START) {
            List<GameEvent> turnEvents = TurnEngine.startTurn(state);
            broadcastEvents(lobby, turnEvents);
            broadcastGameState(lobby);
        }
    }

    /**
     * Called when the game state transitions to {@link GamePhase#ROUND_END}.
     * Calculates scores, broadcasts results, and either starts a new round or
     * declares the game over.
     *
     * @param lobby the lobby whose round ended
     */
    private void handleRoundEnd(Lobby lobby) {
        GameState state = lobby.getGameState();

        Map<String, Integer> roundScores =
                ScoreCalculator.calculateRoundScores(state.getPlayerOrder(), state);

        for (PlayerGameState p : state.getPlayerOrder()) {
            lobby.getCumulativeScores().put(p.getPlayerName(), p.getTotalScore());
        }

        String roundEndPayload = GameStateSerializer.serializeRoundEnd(
                roundScores, state.getPlayerOrder());

        broadcastToLobby(lobby, new Message(Message.Type.ROUND_END, roundEndPayload));

        // End the whole game immediately after this round
        String winner = ScoreCalculator.getWinner(lobby.getCumulativeScores());

        highScoreHistory.appendFinishedGame(
                lobby.getLobbyId(),
                winner,
                new LinkedHashMap<>(lobby.getCumulativeScores())
        );

        broadcastToLobby(lobby, new Message(Message.Type.GAME_END, winner));

        state.setPhase(GamePhase.GAME_OVER);
        lobby.setGameState(null);
        lobby.setGameStarted(false);
        lobby.setLobbyStatus(LobbyStatus.FINISHED);

        broadcastLobbyListToAllClients();

        log("Game over in lobby " + lobby.getLobbyId() + ". Winner: " + winner);
    }

    /**
     * Sends a broadcast message from one client session to all connected clients.
     *
     * <p>This method validates the given broadcast text, rejects empty or
     * multiline messages, determines the sender name, and then distributes
     * the broadcast to every connected client regardless of lobby or game.</p>
     *
     * <p>The outgoing broadcast payload is encoded in the form
     * {@code senderName|messageText} so clients can display both the sender
     * and the broadcast content consistently.</p>
     *
     * @param session the client session that initiated the broadcast
     * @param text the raw broadcast text provided by the client
     */
    private synchronized void handleBroadcast(ClientSession session, String text) {
        String cleaned = text == null ? "" : text.trim();

        if (cleaned.isEmpty()) {
            session.send(new Message(Message.Type.ERROR, "Broadcast message must not be empty.").encode());
            return;
        }

        if (cleaned.contains("\n") || cleaned.contains("\r")) {
            session.send(new Message(Message.Type.ERROR, "Broadcast message must be a single line.").encode());
            return;
        }

        String senderName = session.getPlayerName();
        if (senderName == null || senderName.isBlank()) {
            senderName = "Unknown";
        }

        log("Broadcast from " + senderName + ": " + cleaned);

        broadcastToAllClients(
                new Message(Message.Type.BROADCAST, senderName + "|" + cleaned)
        );
    }

    /**
     * Broadcasts the public game state snapshot to every player in the lobby.
     *
     * @param lobby the target lobby
     */
    private void broadcastGameState(Lobby lobby) {
        GameState state = lobby.getGameState();
        if (state == null) {
            return;
        }

        broadcastToLobby(lobby, new Message(
                Message.Type.GAME_STATE,
                GameStateSerializer.serializePublicState(state)
        ));
    }

    /**
     * Sends each player in the lobby their private hand update.
     *
     * @param lobby the target lobby
     */
    private void broadcastAllHands(Lobby lobby) {
        GameState state = lobby.getGameState();
        if (state == null) {
            return;
        }

        for (ClientSession s : lobby.getSessions()) {
            try {
                PlayerGameState p = state.getPlayer(s.getPlayerName());
                s.send(new Message(
                        Message.Type.HAND_UPDATE,
                        GameStateSerializer.serializeHand(p)
                ).encode());
            } catch (IllegalArgumentException ignored) {
                // session has a name not yet in game state — skip
            }
        }
    }

    /**
     * Converts a list of {@link GameEvent}s into {@code GAME} messages and
     * broadcasts each one to the whole lobby.
     *
     * @param lobby the target lobby
     * @param events the events to broadcast
     */
    private void broadcastEvents(Lobby lobby, List<GameEvent> events) {
        for (GameEvent event : events) {
            broadcastToLobby(lobby, new Message(
                    Message.Type.GAME,
                    event.type().name() + ":" + event.detail()
            ));
        }
    }





    /**
     * Handles a dev-mode request for the current lobby.
     *
     * <p>Usage examples:</p>
     * <ul>
     *   <li>{@code /dev secondchance}</li>
     *   <li>{@code DEV|secondchance}</li>
     *   <li>{@code /dev off}</li>
     * </ul>
     *
     * <p>The chosen scenario is stored on the lobby and applied the next time a
     * game is started in that lobby.</p>
     *
     * @param session the requesting client session
     * @param payload the scenario name or {@code off}
     */
    private void handleDevMode(ClientSession session, String payload) {
        Lobby lobby = getLobbyOf(session);

        if (lobby == null) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "You must be in a lobby to configure dev mode."
            ).encode());
            return;
        }

        String cleaned = payload == null ? "" : payload.trim();

        if (cleaned.isBlank()) {
            session.send(new Message(
                    Message.Type.ERROR,
                    "Usage: /dev <scenarioName> or /dev off"
            ).encode());
            return;
        }

        if ("off".equalsIgnoreCase(cleaned)) {
            DevModeManager.disableForLobby(lobby);
            broadcastToLobby(lobby, new Message(
                    Message.Type.INFO,
                    "Dev mode disabled for lobby: " + lobby.getLobbyId()
            ));
            return;
        }

        DevModeManager.enableForLobby(lobby, cleaned);
        broadcastToLobby(lobby, new Message(
                Message.Type.INFO,
                "Dev mode enabled for lobby " + lobby.getLobbyId()
                        + " with scenario: " + cleaned
        ));
    }

    private void handleGetHighScores(ClientSession session) {
        List<HighScoreHistory.LeaderboardRow> rows = highScoreHistory.buildLeaderboard();
        String payload = serializeHighScores(rows);
        session.send(new Message(Message.Type.HIGHSCORES, payload).encode());
    }

    private String serializeHighScores(List<HighScoreHistory.LeaderboardRow> rows) {
        return rows.stream()
                .map(row -> row.playerName()
                        + ":" + row.wins()
                        + ":" + row.gamesPlayed()
                        + ":" + row.totalPenaltyPoints())
                .collect(Collectors.joining(","));
    }
}