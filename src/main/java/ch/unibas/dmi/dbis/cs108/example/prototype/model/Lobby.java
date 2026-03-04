package ch.unibas.dmi.dbis.cs108.example.prototype.model;


import ch.unibas.dmi.dbis.cs108.example.prototype.controller.ClientSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the game lobby containing all connected players.
 * <p>
 * Maintains a list of active client sessions and provides methods to add/remove
 * clients and query the current set of players.
 * </p>
 */
public class Lobby {

    private final List<ClientSession> sessions = new ArrayList<>();

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
     * @return an unmodifiable copy of the sessions list
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