package ch.unibas.dmi.dbis.cs108.example.model;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one game lobby containing the connected players of that game.
 * <p>
 * Maintains a list of active client sessions and provides methods to add/remove
 * clients and query the current set of players.
 * </p>
 */
public class Lobby {

    private final String lobbyId;
    private final List<ClientSession> sessions = new ArrayList<>();
    private boolean gameStarted = false;

    /**
     * Creates a new lobby with a unique lobby id.
     *
     * @param lobbyId the id of this lobby
     */
    public Lobby(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    /**
     * Returns the id of this lobby.
     *
     * @return the lobby id
     */
    public String getLobbyId() {
        return lobbyId;
    }

    /**
     * Returns whether the game of this lobby has already started.
     *
     * @return true if the game has started, otherwise false
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Sets whether the game of this lobby has started.
     *
     * @param gameStarted the new started state
     */
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    /**
     * Adds a client session to the lobby.
     *
     * @param session the client session to add
     */
    public void addSession(ClientSession session) {
        sessions.add(session);
    }

    /**
     * Removes a client session from the lobby.
     *
     * @param session the client session to remove
     */
    public void removeSession(ClientSession session) {
        sessions.remove(session);
    }

    /**
     * Gets all active client sessions in the lobby.
     *
     * @return a copy of the sessions list
     */
    public List<ClientSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    /**
     * Gets the number of players currently in the lobby.
     *
     * @return the player count
     */
    public int getPlayerCount() {
        return sessions.size();
    }

    /**
     * Gets all players currently in the lobby as {@link Player} records.
     *
     * @return a list of players in the lobby
     */
    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (ClientSession session : sessions) {
            players.add(new Player(session.getPlayerName()));
        }
        return players;
    }
}