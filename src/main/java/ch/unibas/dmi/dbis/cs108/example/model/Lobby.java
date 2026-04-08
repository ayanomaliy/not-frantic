package ch.unibas.dmi.dbis.cs108.example.model;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int MAX_PLAYERS = 5;

    /** Live game state for the current round, or {@code null} before the game starts. */
    private GameState gameState;

    /** Round counter, incremented at the start of each new round (1-based). */
    private int currentRound = 0;

    /**
     * Cumulative scores carried across rounds.
     * Key = player name, value = total score so far.
     * Populated after each round ends.
     */
    private final Map<String, Integer> cumulativeScores = new HashMap<>();

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
    public boolean addSession(ClientSession session) {
        if (isFull()) {
            return false;
        }
        sessions.add(session);
        return true;
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

    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    public boolean isFull() {
        return sessions.size() >= MAX_PLAYERS;
    }

    // ---- Game state accessors ----

    /** Returns the live {@link GameState} for the current round, or {@code null} if not started. */
    public GameState getGameState() { return gameState; }

    /** Sets the live {@link GameState} (called by ServerService at round start). */
    public void setGameState(GameState gameState) { this.gameState = gameState; }

    /** Returns the current round number (1-based; 0 = game not yet started). */
    public int getCurrentRound() { return currentRound; }

    /** Increments the round counter and returns the new value. */
    public int nextRound() { return ++currentRound; }

    /** Returns a live view of the cumulative score map (player name → total score). */
    public Map<String, Integer> getCumulativeScores() { return cumulativeScores; }

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