package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.client.CardTextFormatter;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.service.Message;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import java.util.function.Consumer;

/**
 * JavaFX adapter around the shared protocol client.
 *
 * <p>This class contains GUI-specific behavior only. All socket management,
 * message transport, heartbeat handling, and uniform protocol sending are
 * delegated to {@link ClientProtocolClient}.</p>
 *
 * <p>Raw command parsing is also delegated to the shared protocol layer via
 * {@link ClientProtocolClient#parseRawCommand(String)} so the GUI command field
 * supports the same slash commands as the terminal client, including the
 * human-friendly effect helper commands parsed in {@link Message#parse(String)}.</p>
 */
public class FxNetworkClient implements ClientMessageHandler {

    /** Shared GUI state updated from incoming server messages. */
    private final ClientState state;

    /** Shared typed protocol client used by the GUI. */
    private final ClientProtocolClient protocolClient;

    /** Callback triggered once the first real game state arrives. */
    private Runnable gameStartListener;

    /** Prevents reopening the game view for every GAME_STATE update. */
    private boolean gameViewShown = false;

    private Runnable gameEndListener;

    private Consumer<String> effectRequestListener;

    /**
     * Creates a new GUI network adapter.
     *
     * @param state the shared client state
     */
    public FxNetworkClient(ClientState state) {
        this.state = state;
        this.protocolClient = new ClientProtocolClient(this);
    }

    /**
     * Registers a callback that is invoked when the server confirms that a game
     * has started by sending a {@code GAME_STATE} message.
     *
     * @param gameStartListener the callback to invoke
     */
    public void setGameStartListener(Runnable gameStartListener) {
        this.gameStartListener = gameStartListener;
    }

    /**
     * Connects to the server and initializes the GUI state.
     *
     * @param host the server host
     * @param port the server port
     * @param username the desired username
     * @throws IOException if the connection fails
     */
    public void connect(String host, int port, String username) throws IOException {
        protocolClient.connect(host, port);

        gameViewShown = false;

        Platform.runLater(() -> {
            state.setConnected(true);
            state.setUsername(username);
            state.setStatusText("Connected to " + host + ":" + port);
        });

        setName(username);
    }

    /**
     * Disconnects this client from the server.
     */
    public void disconnect() {
        protocolClient.disconnect();
        clearLocalState("Disconnected");
    }

    /**
     * Sends a request to set or change the player name.
     *
     * @param username the desired player name
     */
    public void setName(String username) {
        protocolClient.setName(username);
    }

    /**
     * Sends a global chat message.
     *
     * @param text the chat text
     */
    public void sendGlobalChat(String text) {
        protocolClient.sendGlobalChat(text);
    }

    /**
     * Sends a lobby chat message.
     *
     * @param text the chat text
     */
    public void sendLobbyChat(String text) {
        protocolClient.sendLobbyChat(text);
    }

    /**
     * Sends a whisper message.
     *
     * @param targetPlayer the whisper target
     * @param text the whisper text
     */
    public void sendWhisperChat(String targetPlayer, String text) {
        protocolClient.sendWhisperChat(targetPlayer, text);
    }

    /**
     * Requests the current lobby player list.
     */
    public void requestPlayers() {
        protocolClient.requestPlayers();
    }

    /**
     * Requests the global player list.
     */
    public void requestAllPlayers() {
        protocolClient.requestAllPlayers();
    }

    /**
     * Requests the lobby list.
     */
    public void requestLobbies() {
        protocolClient.requestLobbies();
    }

    /**
     * Creates a new lobby.
     *
     * @param lobbyId the desired lobby id
     */
    public void createLobby(String lobbyId) {
        protocolClient.createLobby(lobbyId);
    }

    /**
     * Joins an existing lobby.
     *
     * @param lobbyId the lobby id
     */
    public void joinLobby(String lobbyId) {
        protocolClient.joinLobby(lobbyId);
    }

    /**
     * Leaves the current lobby.
     */
    public void leaveLobby() {
        protocolClient.leaveLobby();
    }

    /**
     * Requests a game start.
     */
    public void startGame() {
        protocolClient.startGame();
    }

    /**
     * Requests the current hand from the server.
     */
    public void requestHand() {
        protocolClient.requestHand();
    }

    /**
     * Requests the current public game state from the server.
     */
    public void requestGameState() {
        protocolClient.requestGameState();
    }

    /**
     * Requests the most recent round-end summary from the server.
     */
    public void requestRoundEnd() {
        protocolClient.requestRoundEnd();
    }

    /**
     * Requests the game-end summary from the server.
     */
    public void requestGameEnd() {
        protocolClient.requestGameEnd();
    }

    /**
     * Requests to draw one card.
     */
    public void drawCard() {
        protocolClient.drawCard();
    }

    /**
     * Requests to end the current turn.
     */
    public void endTurn() {
        protocolClient.endTurn();
    }

    /**
     * Requests to play the given card.
     *
     * @param cardId the card id to play
     */
    public void playCard(int cardId) {
        protocolClient.playCard(cardId);
    }

// RESOLVE EFFECTS:
    /**
     * Sends a FANTASTIC effect response with either a color or a number.
     *
     * @param color the selected color, or null
     * @param number the selected number, or null
     */
    public void resolveFantastic(CardColor color, Integer number) {
        protocolClient.resolveFantastic(color, number);
    }

    /**
     * Sends an EQUALITY effect response.
     *
     * @param targetPlayer the player who should draw up
     * @param color the requested color
     */
    public void resolveEquality(String targetPlayer, CardColor color) {
        protocolClient.resolveEquality(targetPlayer, color);
    }

    /**
     * Sends a FANTASTIC_FOUR effect response.
     *
     * @param color the selected color, or null
     * @param number the selected number, or null
     * @param targetPlayers exactly four recipient names
     */
    public void resolveFantasticFour(CardColor color, Integer number, java.util.List<String> targetPlayers) {
        protocolClient.resolveFantasticFour(color, number, targetPlayers);
    }

    /**
     * Sends a SECOND_CHANCE effect response for playing another card.
     *
     * @param cardId the card id to play
     */
    public void resolveSecondChance(int cardId) {
        protocolClient.resolveSecondChance(cardId);
    }

    /**
     * Sends a SECOND_CHANCE effect response indicating that the player draws instead.
     */
    public void resolveSecondChanceDrawPenalty() {
        protocolClient.resolveSecondChanceDrawPenalty();
    }


    /**
     * Sends a GIFT effect response.
     *
     * @param targetPlayer the receiving player
     * @param cardIds the selected card ids
     */
    public void resolveGift(String targetPlayer, List<Integer> cardIds) {
        protocolClient.resolveGift(targetPlayer, cardIds);
    }

    /**
     * Sends an EXCHANGE effect response.
     *
     * @param targetPlayer the exchange partner
     * @param cardIds the selected card ids
     */
    public void resolveExchange(String targetPlayer, List<Integer> cardIds) {
        protocolClient.resolveExchange(targetPlayer, cardIds);
    }

    /**
     * Sends a SKIP effect response.
     *
     * @param targetPlayer the player whose next turn should be skipped
     */
    public void resolveSkip(String targetPlayer) {
        protocolClient.resolveSkip(targetPlayer);
    }

    /**
     * Sends a NICE_TRY effect response.
     *
     * @param targetPlayer the player who should receive the penalty
     */
    public void resolveNiceTry(String targetPlayer) {
        protocolClient.resolveNiceTry(targetPlayer);
    }

    // --------------------------------------------------------------------------------

    /**
     * Set Game end Listener
     *
     * @param gameEndListener as Runnable
     */
    public void setGameEndListener(Runnable gameEndListener) {
        this.gameEndListener = gameEndListener;
    }

    /**
     * Parses and executes a raw terminal-style command for the GUI command field.
     *
     * <p>This method supports the same slash commands as the terminal client
     * because parsing is delegated to {@link Message#parse(String)} through the
     * shared protocol client.</p>
     *
     * @param rawInput the raw command
     * @return {@code true} if the command caused a disconnect
     */
    public boolean sendCommand(String rawInput) {
        Message message = protocolClient.parseRawCommand(rawInput);

        if (message == null) {
            onLocalMessage("[CLIENT] Invalid input.");
            return false;
        }

        if (message.type() == Message.Type.UNKNOWN) {
            onLocalMessage("[CLIENT] Unknown command.");
            return false;
        }

        if (message.type() == Message.Type.PING || message.type() == Message.Type.PONG) {
            onLocalMessage("[CLIENT] Heartbeat messages are handled automatically.");
            return false;
        }

        protocolClient.send(message);

        if (message.type() == Message.Type.QUIT) {
            protocolClient.disconnect(false);
            clearLocalState("Disconnected");
            return true;
        }

        return false;
    }

    @Override
    public void onMessage(Message message) {
        switch (message.type()) {
            case GLOBALCHAT -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getGlobalChatMessages().add(parts[0] + ": " + parts[1]));
            }

            case LOBBYCHAT -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getLobbyChatMessages().add(parts[0] + ": " + parts[1]));
            }

            case WHISPERCHAT -> Platform.runLater(() -> {
                String[] parts = message.content().split("\\|", 3);

                if (parts.length == 3) {
                    String direction = parts[0];
                    String otherUser = parts[1];
                    String text = parts[2];

                    if ("FROM".equals(direction)) {
                        state.getWhisperChatMessages().add("[From " + otherUser + "] " + text);
                    } else if ("TO".equals(direction)) {
                        state.getWhisperChatMessages().add("[To " + otherUser + "] " + text);
                    } else {
                        state.getWhisperChatMessages().add(message.content());
                    }
                } else {
                    state.getWhisperChatMessages().add(message.content());
                }
            });

            case INFO -> Platform.runLater(() -> {
                String info = message.content();
                state.getGameMessages().add("[INFO] " + info);

                if (info.startsWith("Lobby created and joined: ")) {
                    state.setCurrentLobby(info.substring("Lobby created and joined: ".length()).trim());
                } else if (info.startsWith("Joined lobby: ")) {
                    state.setCurrentLobby(info.substring("Joined lobby: ".length()).trim());
                } else if (info.startsWith("You are already in lobby: ")) {
                    state.setCurrentLobby(info.substring("You are already in lobby: ".length()).trim());
                } else if (info.startsWith("You left lobby: ")) {
                    state.setCurrentLobby("");
                }

                if (info.startsWith("Your name has been set to ")) {
                    state.setUsername(info.substring("Your name has been set to ".length()).trim());
                } else if (info.startsWith("Your requested name was taken. Your name has been set to ")) {
                    state.setUsername(info.substring(
                            "Your requested name was taken. Your name has been set to ".length()
                    ).trim());
                } else if (info.startsWith("Your name is now ")) {
                    state.setUsername(info.substring("Your name is now ".length()).trim());
                } else if (info.startsWith("Your requested name was taken. Your name is now ")) {
                    state.setUsername(info.substring(
                            "Your requested name was taken. Your name is now ".length()
                    ).trim());
                }
            });

            case ERROR -> Platform.runLater(() ->
                    state.getGameMessages().add("[ERROR] " + message.content()));

            case PLAYERS -> Platform.runLater(() -> {
                List<String> updatedPlayers = message.content().isBlank()
                        ? List.of()
                        : Arrays.stream(message.content().split(","))
                          .map(String::trim)
                          .filter(s -> !s.isBlank())
                          .toList();

                state.getPlayers().setAll(updatedPlayers);

                if (updatedPlayers.isEmpty()) {
                    clearLocalGameOnly();
                }
            });

            case ALLPLAYERS -> Platform.runLater(() -> {
                state.getAllPlayers().setAll(
                        message.content().isBlank()
                                ? List.of()
                                : Arrays.stream(message.content().split(","))
                                  .map(String::trim)
                                  .filter(s -> !s.isBlank())
                                  .toList()
                );
            });

            case LOBBIES -> Platform.runLater(() -> {
                List<String> formattedLobbies = message.content().isBlank()
                        ? List.of()
                        : Arrays.stream(message.content().split(","))
                          .map(String::trim)
                          .filter(s -> !s.isBlank())
                          .map(this::formatLobbyEntry)
                          .toList();

                state.getLobbies().setAll(formattedLobbies);
            });

            case GAME_STATE -> Platform.runLater(() -> {
                applyGameStatePayload(message.content());
                if (!"RESOLVING_EFFECT".equals(state.getCurrentPhase())) {
                    state.setPendingEffectRequest("");
                }
                state.getGameMessages().add("[GAME_STATE] " + message.content());

                if (!gameViewShown) {
                    gameViewShown = true;
                    if (gameStartListener != null) {
                        gameStartListener.run();
                    }
                }
            });

            case HAND_UPDATE -> Platform.runLater(() -> {
                state.getCurrentHandCards().setAll(
                        message.content().isBlank()
                                ? List.of()
                                : Arrays.stream(message.content().split(","))
                                  .map(String::trim)
                                  .filter(s -> !s.isBlank())
                                  .toList()
                );
                state.getGameMessages().add("[HAND] " + message.content());
            });

            case GAME -> Platform.runLater(() ->
                    state.getGameMessages().add("[GAME] " + message.content()));

            case EFFECT_REQUEST -> Platform.runLater(() -> {
                String content = message.content();
                state.getGameMessages().add("[EFFECT_REQUEST] " + content);
                state.setPendingEffectRequest(content);

                if (effectRequestListener != null) {
                    effectRequestListener.accept(content);
                }
            });

            case ROUND_END -> Platform.runLater(() -> {
                state.getGameMessages().add("[ROUND_END] Round over! Scores: " + message.content());
                state.setCurrentPhase("ROUND_END");

                state.getFinalScoreRows().clear();

                if (!message.content().isBlank()) {
                    for (String entry : message.content().split(",")) {
                        String[] parts = entry.split(":");
                        if (parts.length == 3) {
                            String player = parts[0].trim();
                            String roundPoints = parts[1].trim();
                            String totalPoints = parts[2].trim();

                            state.getFinalScoreRows().add(
                                    player + " | Round: " + roundPoints + " | Total: " + totalPoints
                            );
                        }
                    }
                }
            });

            case GAME_END -> Platform.runLater(() -> {
                state.getGameMessages().add("[GAME_END] " + message.content());
                state.setWinnerName(message.content());
                state.setCurrentPhase("GAME_OVER");

                if (gameEndListener != null) {
                    gameEndListener.run();
                }
            });

            case BROADCAST -> {
                String[] parts = message.splitChatPayload();
                Platform.runLater(() ->
                        state.getGameMessages().add("[INFO] [Broadcast] " + parts[0] + ": " + parts[1]));
            }

            default -> Platform.runLater(() ->
                    state.getGameMessages().add("[CLIENT] Unexpected: " + message.encode()));
        }
    }

    @Override
    public void onLocalMessage(String text) {
        Platform.runLater(() -> state.getGameMessages().add(text));
    }

    @Override
    public void onDisconnected(String reason) {
        clearLocalState(reason);
    }

    /**
     * Formats one raw lobby entry received from the server into a GUI-friendly string.
     *
     * <p>The server sends lobby entries in the format
     * {@code lobbyId:status:currentPlayers:maxPlayers}. This method converts
     * them into a readable display form such as
     * {@code FunRoom (WAITING) 2/5}.</p>
     *
     * <p>If the entry does not match the expected format, the original entry
     * is returned unchanged for forward compatibility.</p>
     *
     * @param entry the raw lobby entry received from the server
     * @return the formatted lobby entry for display in the GUI
     */
    private String formatLobbyEntry(String entry) {
        String[] parts = entry.split(":", 4);
        if (parts.length != 4) {
            return entry;
        }

        String lobbyName = parts[0].trim();
        String status = parts[1].trim();
        String currentPlayers = parts[2].trim();
        String maxPlayers = parts[3].trim();

        return lobbyName + " (" + status + ") " + currentPlayers + "/" + maxPlayers;
    }

    /**
     * Applies the public-state payload from a {@code GAME_STATE} message to the GUI state.
     *
     * <p>The server currently serializes public state as comma-separated
     * {@code key:value} pairs with a trailing {@code players:...} section.</p>
     *
     * @param payload the raw game-state payload
     */
    private void applyGameStatePayload(String payload) {
        String prefix = payload;
        int playersIndex = payload.indexOf(",players:");
        if (playersIndex >= 0) {
            prefix = payload.substring(0, playersIndex);
        }

        for (String entry : prefix.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            switch (key) {
                case "phase" -> state.setCurrentPhase(value);
                case "currentPlayer" -> state.setCurrentPlayer(value);
                case "requestedColor" -> {
                    if ("none".equalsIgnoreCase(value)) {
                        state.setRequestedColor("");
                    } else {
                        state.setRequestedColor(value);
                    }
                }
                case "requestedNumber" -> {
                    if ("none".equalsIgnoreCase(value)) {
                        state.setRequestedNumber("");
                    } else {
                        state.setRequestedNumber(value);
                    }
                }
                case "discardTop" -> {
                    if ("none".equalsIgnoreCase(value)) {
                        state.setTopCardId("");
                        state.setTopCardText("-");
                    } else {
                        try {
                            int cardId = Integer.parseInt(value);
                            state.setTopCardId(String.valueOf(cardId));
                            state.setTopCardText(CardTextFormatter.formatCardLabelWithId(cardId));
                        } catch (NumberFormatException e) {
                            state.setTopCardId("");
                            state.setTopCardText("Card #" + value);
                        }
                    }


                }
                default -> {
                    // Ignore unknown fields for forward compatibility.
                }
            }
        }
    }

    /**
     * Clears only game-specific GUI state while keeping connection, lobby,
     * and chat information intact.
     */
    private void clearLocalGameOnly() {
        gameViewShown = false;
        state.setCurrentPlayer("Unknown");
        state.setCurrentPhase("WAITING");
        state.setTopCardText("-");
        state.getCurrentHandCards().clear();
        state.setTopCardId("");
        state.setRequestedColor("");
        state.setRequestedNumber("");
    }

    /**
     * Clears the local GUI state after a disconnect.
     *
     * @param statusText the new status text
     */
    private void clearLocalState(String statusText) {
        gameViewShown = false;
        state.setCurrentLobby("");
        state.setTopCardId("");
        state.setRequestedColor("");
        state.setRequestedNumber("");

        Platform.runLater(() -> {
            state.setConnected(false);
            state.setStatusText(statusText);
            state.setChatMode("Global");

            state.getPlayers().clear();
            state.getAllPlayers().clear();
            state.getLobbies().clear();
            state.getCurrentHandCards().clear();

            state.setCurrentPlayer("Unknown");
            state.setCurrentPhase("WAITING");
            state.setTopCardText("-");

            state.getGlobalChatMessages().clear();
            state.getLobbyChatMessages().clear();
            state.getWhisperChatMessages().clear();

            state.getGameMessages().clear();
        });
    }



    /**
     * Registers a callback that is invoked when the server sends an EFFECT_REQUEST.
     *
     * @param effectRequestListener the callback to invoke with the raw effect payload
     */
    public void setEffectRequestListener(Consumer<String> effectRequestListener) {
        this.effectRequestListener = effectRequestListener;
    }
}