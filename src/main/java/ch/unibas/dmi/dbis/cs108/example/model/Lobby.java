package ch.unibas.dmi.dbis.cs108.example.model;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents one game lobby containing the connected players of a single game.
 *
 * <p>A lobby stores its connected client sessions, tracks the lifecycle state
 * of the game (waiting, playing, finished), and maintains cumulative scores
 * across rounds.</p>
 */
public class Lobby {

    /** Maximum number of players allowed in one lobby. */
    private static final int MAX_PLAYERS = 5;

    /** Unique identifier of this lobby. */
    private final String lobbyId;

    /** Active client sessions currently inside this lobby. */
    private final List<ClientSession> sessions = new ArrayList<>();

    /** Whether a game is currently running in this lobby. */
    private boolean gameStarted = false;

    /** Current lifecycle status of the lobby. */
    private LobbyStatus status = LobbyStatus.WAITING;

    /** Live game state for the current round, or {@code null} before the game starts. */
    private GameState gameState;

    /** Round counter, incremented at the start of each new round (1-based). */
    private int currentRound = 0;

    private boolean devModeEnabled = false;
    private String devScenario = "none";

    /**
     * Cumulative scores carried across rounds.
     * Key = player name, value = total score so far.
     */
    private final Map<String, Integer> cumulativeScores = new HashMap<>();

    /**
     * Creates a new lobby with a unique lobby id.
     *
     * @param lobbyId the id of this lobby
     */
    public Lobby(String lobbyId) {
        this.lobbyId = lobbyId;
        this.status = LobbyStatus.WAITING;
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
     * Returns whether the game in this lobby has already started.
     *
     * @return {@code true} if a game is running, otherwise {@code false}
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Sets whether the game in this lobby has started.
     *
     * <p>If set to {@code true}, the lobby status is automatically updated to
     * {@link LobbyStatus#PLAYING}.</p>
     *
     * @param gameStarted the new started state
     */
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;

        if (gameStarted) {
            this.status = LobbyStatus.PLAYING;
        }
    }

    /**
     * Returns the current lifecycle status of this lobby.
     *
     * @return the lobby status
     */
    public LobbyStatus getLobbyStatus() {
        return status;
    }

    /**
     * Sets the current lifecycle status of this lobby.
     *
     * @param status the new lobby status
     */
    public void setLobbyStatus(LobbyStatus status) {
        this.status = status;
    }

    /**
     * Returns the display status of this lobby.
     *
     * <p>This is used in lobby-list responses sent to clients.</p>
     *
     * @return the status name as string (e.g., "WAITING", "PLAYING")
     */
    public String getStatus() {
        return status.name();
    }

    /**
     * Adds a client session to the lobby if the lobby is not full.
     *
     * @param session the client session to add
     * @return {@code true} if the session was added, otherwise {@code false}
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
     * Returns all active client sessions in the lobby.
     *
     * @return a copy of the current session list
     */
    public List<ClientSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    /**
     * Returns the number of players currently in the lobby.
     *
     * @return the current player count
     */
    public int getPlayerCount() {
        return sessions.size();
    }

    /**
     * Returns the maximum number of players allowed in this lobby.
     *
     * @return the maximum player capacity
     */
    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    /**
     * Returns whether this lobby has reached its maximum player capacity.
     *
     * @return {@code true} if the lobby is full, otherwise {@code false}
     */
    public boolean isFull() {
        return sessions.size() >= MAX_PLAYERS;
    }

    /**
     * Returns the live {@link GameState} for the current round.
     *
     * @return the current game state, or {@code null} if no game is active
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the live {@link GameState} for the current round.
     *
     * @param gameState the new game state
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Returns the current round number.
     *
     * <p>The counter is 1-based. A value of {@code 0} means no round has started yet.</p>
     *
     * @return the current round number
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Increments the round counter and returns the new value.
     *
     * @return the incremented round number
     */
    public int nextRound() {
        return ++currentRound;
    }

    /**
     * Returns the cumulative score map of this lobby.
     *
     * @return the cumulative score map
     */
    public Map<String, Integer> getCumulativeScores() {
        return cumulativeScores;
    }

    /**
     * Returns all players currently in the lobby as {@link Player} records.
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

    public boolean isDevModeEnabled() {
        return devModeEnabled;
    }

    public void setDevModeEnabled(boolean devModeEnabled) {
        this.devModeEnabled = devModeEnabled;
    }

    public String getDevScenario() {
        return devScenario;
    }

    public void setDevScenario(String devScenario) {
        this.devScenario = devScenario == null ? "none" : devScenario;
    }
}