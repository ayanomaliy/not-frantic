package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * High-level protocol client built on top of {@link NetworkClientCore}.
 *
 * <p>This class centralizes all client-side message formatting so GUI and CLI
 * code can use typed helper methods instead of manually constructing protocol
 * strings.</p>
 */
public class ClientProtocolClient {

    private final NetworkClientCore core;

    /**
     * Creates a new protocol client.
     *
     * @param handler the active message handler
     */
    public ClientProtocolClient(ClientMessageHandler handler) {
        this.core = new NetworkClientCore(handler);
    }

    /**
     * Connects to the server.
     *
     * @param host the server host
     * @param port the server port
     * @throws IOException if the connection fails
     */
    public void connect(String host, int port) throws IOException {
        core.connect(host, port);
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        core.disconnect();
    }

    /**
     * Disconnects from the server and optionally skips sending {@code QUIT}.
     *
     * @param notifyServer whether the server should be notified first
     */
    public void disconnect(boolean notifyServer) {
        core.disconnect(notifyServer);
    }

    /**
     * Returns whether the client is currently connected.
     *
     * @return {@code true} if connected, otherwise {@code false}
     */
    public boolean isConnected() {
        return core.isConnected();
    }

    /**
     * Sends a typed protocol message directly.
     *
     * @param message the message to send
     */
    public void send(Message message) {
        core.send(message);
    }

    /**
     * Sends a name request.
     *
     * @param username the desired username
     */
    public void setName(String username) {
        send(new Message(Message.Type.NAME, username));
    }

    /**
     * Sends a global chat message.
     *
     * @param text the message text
     */
    public void sendGlobalChat(String text) {
        send(new Message(Message.Type.GLOBALCHAT, text));
    }

    /**
     * Sends a lobby chat message.
     *
     * @param text the message text
     */
    public void sendLobbyChat(String text) {
        send(new Message(Message.Type.LOBBYCHAT, text));
    }

    /**
     * Sends a whisper chat message.
     *
     * @param targetPlayer the whisper target
     * @param text the message text
     */
    public void sendWhisperChat(String targetPlayer, String text) {
        send(new Message(Message.Type.WHISPERCHAT, targetPlayer + "|" + text));
    }

    /**
     * Requests the lobby-local player list.
     */
    public void requestPlayers() {
        send(new Message(Message.Type.PLAYERS, ""));
    }

    /**
     * Requests the global player list.
     */
    public void requestAllPlayers() {
        send(new Message(Message.Type.ALLPLAYERS, ""));
    }

    /**
     * Requests the available lobby list.
     */
    public void requestLobbies() {
        send(new Message(Message.Type.LOBBIES, ""));
    }

    /**
     * Creates a new lobby.
     *
     * @param lobbyId the new lobby id
     */
    public void createLobby(String lobbyId) {
        send(new Message(Message.Type.CREATE, lobbyId));
    }

    /**
     * Joins an existing lobby.
     *
     * @param lobbyId the target lobby id
     */
    public void joinLobby(String lobbyId) {
        send(new Message(Message.Type.JOIN, lobbyId));
    }

    /**
     * Leaves the current lobby.
     */
    public void leaveLobby() {
        send(new Message(Message.Type.LEAVE, ""));
    }

    /**
     * Requests a game start.
     */
    public void startGame() {
        send(new Message(Message.Type.START, ""));
    }

    /**
     * Requests to end the current match immediately using the cheat command.
     */
    public void cheatWin() {
        send(new Message(Message.Type.CHEATWIN, ""));
    }

    /**
     * Requests the current private hand.
     */
    public void requestHand() {
        send(new Message(Message.Type.GET_HAND, ""));
    }

    /**
     * Requests the current public game state.
     */
    public void requestGameState() {
        send(new Message(Message.Type.GET_GAME_STATE, ""));
    }

    /**
     * Requests the most recent round-end summary.
     */
    public void requestRoundEnd() {
        send(new Message(Message.Type.GET_ROUND_END, ""));
    }

    /**
     * Requests the game-end summary.
     */
    public void requestGameEnd() {
        send(new Message(Message.Type.GET_GAME_END, ""));
    }

    /**
     * Requests to draw one card.
     */
    public void drawCard() {
        send(new Message(Message.Type.DRAW_CARD, ""));
    }

    /**
     * Requests to end the current turn.
     */
    public void endTurn() {
        send(new Message(Message.Type.END_TURN, ""));
    }

    /**
     * Requests to play the given card.
     *
     * @param cardId the id of the card to play
     */
    public void playCard(int cardId) {
        send(new Message(Message.Type.PLAY_CARD, String.valueOf(cardId)));
    }

    /**
     * Sends a generic target-only effect response.
     *
     * @param effectName the effect name
     * @param targetPlayer the selected target player
     */
    public void sendEffectTargetResponse(String effectName, String targetPlayer) {
        sendEffectResponse(effectName, targetPlayer);
    }

    /**
     * Sends a SKIP effect response.
     *
     * @param targetPlayer the player to skip
     */
    public void resolveSkip(String targetPlayer) {
        sendEffectTargetResponse("SKIP", targetPlayer);
    }

    /**
     * Sends a COUNTERATTACK response that only sets a color.
     *
     * @param color the requested color
     */
    public void resolveCounterattack(CardColor color) {
        Objects.requireNonNull(color, "color must not be null");
        sendEffectResponse("COUNTERATTACK", "", color.name());
    }

    /**
     * Sends a COUNTERATTACK response that redirects a target and sets a color.
     *
     * @param targetPlayer the redirected target
     * @param color the requested color
     */
    public void resolveCounterattack(String targetPlayer, CardColor color) {
        Objects.requireNonNull(color, "color must not be null");
        sendEffectResponse("COUNTERATTACK", targetPlayer, color.name());
    }

    /**
     * Sends a NICE_TRY effect response.
     *
     * @param targetPlayer the affected target player
     */
    public void resolveNiceTry(String targetPlayer) {
        sendEffectTargetResponse("NICE_TRY", targetPlayer);
    }

    /**
     * Sends a GIFT effect response.
     *
     * @param targetPlayer the receiver
     * @param cardIds the selected card ids
     */
    public void resolveGift(String targetPlayer, List<Integer> cardIds) {
        sendEffectResponse("GIFT", targetPlayer, joinCardIds(cardIds));
    }

    /**
     * Sends an EXCHANGE effect response.
     *
     * @param targetPlayer the exchange partner
     * @param cardIds the selected card ids
     */
    public void resolveExchange(String targetPlayer, List<Integer> cardIds) {
        sendEffectResponse("EXCHANGE", targetPlayer, joinCardIds(cardIds));
    }

    /**
     * Sends a FANTASTIC effect response.
     *
     * <p>Exactly one of color or number must be set. The protocol payload always
     * uses three parts: effect name, color slot, and number slot.</p>
     *
     * @param color the selected color, or {@code null}
     * @param number the selected number, or {@code null}
     */
    public void resolveFantastic(CardColor color, Integer number) {
        sendColorOrNumberEffect("FANTASTIC", color, number);
    }

    /**
     * Sends a FANTASTIC_FOUR effect response.
     *
     * <p>Exactly one of color or number must be set, and exactly four target
     * recipients must be supplied.</p>
     *
     * @param color the selected color, or {@code null}
     * @param number the selected number, or {@code null}
     * @param targetPlayers the four target recipients
     */
    public void resolveFantasticFour(CardColor color,
                                     Integer number,
                                     List<String> targetPlayers) {
        Objects.requireNonNull(targetPlayers, "targetPlayers must not be null");

        if (targetPlayers.size() != 4) {
            throw new IllegalArgumentException("FANTASTIC_FOUR requires exactly 4 target players.");
        }

        boolean hasColor = color != null;
        boolean hasNumber = number != null;

        if (hasColor == hasNumber) {
            throw new IllegalArgumentException("Exactly one of color or number must be set.");
        }

        String targets = String.join(",", targetPlayers);

        if (hasColor) {
            sendEffectResponse("FANTASTIC_FOUR", color.name(), "", targets);
        } else {
            sendEffectResponse("FANTASTIC_FOUR", "", String.valueOf(number), targets);
        }
    }

    /**
     * Sends an EQUALITY effect response.
     *
     * @param targetPlayer the player who should draw up
     * @param color the requested next color
     */
    public void resolveEquality(String targetPlayer, CardColor color) {
        Objects.requireNonNull(color, "color must not be null");
        sendEffectResponse("EQUALITY", targetPlayer, color.name());
    }

    /**
     * Sends a SECOND_CHANCE effect response for playing another card.
     *
     * @param cardId the id of the card to play
     */
    public void resolveSecondChance(int cardId) {
        sendEffectResponse("SECOND_CHANCE", String.valueOf(cardId));
    }

    /**
     * Sends a SECOND_CHANCE effect response indicating that no follow-up card is
     * played and the draw branch should be used.
     */
    public void resolveSecondChanceDrawPenalty() {
        send(new Message(Message.Type.EFFECT_RESPONSE, "SECOND_CHANCE|"));
    }

    /**
     * Builds and sends a raw unified effect response.
     *
     * <p>All parts are joined using {@code |}. Null values are converted to
     * empty strings.</p>
     *
     * @param parts the response parts
     */
    private void sendEffectResponse(String... parts) {
        String payload = String.join("|",
                java.util.Arrays.stream(parts)
                        .map(part -> part == null ? "" : part)
                        .toArray(String[]::new)
        );

        send(new Message(Message.Type.EFFECT_RESPONSE, payload));
    }

    /**
     * Sends an effect response that encodes either a color or a number.
     *
     * <p>The payload always reserves both slots so the server can parse color
     * and number consistently:</p>
     *
     * <pre>
     * EFFECT_NAME|COLOR|
     * EFFECT_NAME||NUMBER
     * </pre>
     *
     * @param effectName the effect name
     * @param color the selected color, or {@code null}
     * @param number the selected number, or {@code null}
     */
    private void sendColorOrNumberEffect(String effectName, CardColor color, Integer number) {
        boolean hasColor = color != null;
        boolean hasNumber = number != null;

        if (hasColor == hasNumber) {
            throw new IllegalArgumentException("Exactly one of color or number must be set.");
        }

        if (hasColor) {
            sendEffectResponse(effectName, color.name(), "");
        } else {
            sendEffectResponse(effectName, "", String.valueOf(number));
        }
    }

    /**
     * Joins card ids into the comma-separated protocol format.
     *
     * @param cardIds the card ids to join
     * @return a comma-separated list of ids
     */
    private String joinCardIds(List<Integer> cardIds) {
        Objects.requireNonNull(cardIds, "cardIds must not be null");

        return cardIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Parses a raw terminal-style command into a structured message.
     *
     * @param rawInput the raw input string
     * @return the parsed message, or {@code null} if invalid
     */
    public Message parseRawCommand(String rawInput) {
        return Message.parse(rawInput);
    }

    /**
     * Sends an automatic reconnect request.
     *
     * @param username the previous username
     * @param token the reconnect token issued by the server
     */
    public void reconnect(String username, String token) {
        send(new Message(Message.Type.RECONNECT, username + "|" + token));
    }

    /**
     * Simulates an unexpected network loss for reconnect testing.
     */
    public void simulateNetworkLossForTesting() {
        core.simulateNetworkLossForTesting();
    }

}