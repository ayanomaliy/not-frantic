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
 * <p>This class exposes uniform typed methods for sending protocol messages,
 * so both CLI and GUI can use the same message-sending API without relying on
 * raw protocol strings internally.</p>
 *
 * <p>All effect-response protocol formatting is centralized in this class so
 * future protocol changes only have to be implemented in one place.</p>
 */
public class ClientProtocolClient {

    private final NetworkClientCore core;

    /**
     * Creates a new protocol client.
     *
     * @param handler the message handler used by the active front end
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
     * @param notifyServer whether {@code QUIT} should be sent first
     */
    public void disconnect(boolean notifyServer) {
        core.disconnect(notifyServer);
    }

    /**
     * Returns whether the connection is active.
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
     * Sends a request to set or change the player name.
     *
     * @param username the desired player name
     */
    public void setName(String username) {
        send(new Message(Message.Type.NAME, username));
    }

    /**
     * Sends a global chat message.
     *
     * @param text the chat text
     */
    public void sendGlobalChat(String text) {
        send(new Message(Message.Type.GLOBALCHAT, text));
    }

    /**
     * Sends a lobby chat message.
     *
     * @param text the chat text
     */
    public void sendLobbyChat(String text) {
        send(new Message(Message.Type.LOBBYCHAT, text));
    }

    /**
     * Sends a whisper message payload.
     *
     * @param targetPlayer the whisper target
     * @param text the whisper text
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
     * @param lobbyId the desired lobby id
     */
    public void createLobby(String lobbyId) {
        send(new Message(Message.Type.CREATE, lobbyId));
    }

    /**
     * Joins an existing lobby.
     *
     * @param lobbyId the lobby id to join
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
     * Requests the current hand.
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
     * Requests to play a card.
     *
     * @param cardId the id of the card to play
     */
    public void playCard(int cardId) {
        send(new Message(Message.Type.PLAY_CARD, String.valueOf(cardId)));
    }

    /**
     * Sends an effect response with a target player for effects such as
     * {@code SKIP}, {@code COUNTERATTACK}, or {@code NICE_TRY}.
     *
     * @param effectName the effect name as expected by the server
     * @param targetPlayer the selected target player
     */
    public void sendEffectTargetResponse(String effectName, String targetPlayer) {
        sendEffectResponse(effectName, targetPlayer);
    }

    /**
     * Sends a {@code SKIP} effect response.
     *
     * @param targetPlayer the player whose next turn should be skipped
     */
    public void resolveSkip(String targetPlayer) {
        sendEffectTargetResponse("SKIP", targetPlayer);
    }

    /**
     * Sends a {@code COUNTERATTACK} effect response.
     *
     * @param targetPlayer the new redirected target player
     */
    public void resolveCounterattack(String targetPlayer) {
        sendEffectTargetResponse("COUNTERATTACK", targetPlayer);
    }

    /**
     * Sends a {@code NICE_TRY} effect response.
     *
     * @param targetPlayer the player who must draw after attempting to end the round
     */
    public void resolveNiceTry(String targetPlayer) {
        sendEffectTargetResponse("NICE_TRY", targetPlayer);
    }

    /**
     * Sends a {@code GIFT} effect response.
     *
     * @param targetPlayer the receiving player
     * @param cardIds the ids of the cards to transfer
     */
    public void resolveGift(String targetPlayer, List<Integer> cardIds) {
        sendEffectResponse("GIFT", targetPlayer, joinCardIds(cardIds));
    }

    /**
     * Sends an {@code EXCHANGE} effect response.
     *
     * @param targetPlayer the exchange partner
     * @param cardIds the ids of the selected cards to exchange
     */
    public void resolveExchange(String targetPlayer, List<Integer> cardIds) {
        sendEffectResponse("EXCHANGE", targetPlayer, joinCardIds(cardIds));
    }

    /**
     * Sends a {@code FANTASTIC} effect response.
     *
     * <p>The number may be {@code null} if only a color should be requested.</p>
     *
     * @param color the selected color
     * @param number the selected number, or {@code null}
     */
    public void resolveFantastic(CardColor color, Integer number) {
        sendColorOrNumberEffect("FANTASTIC", color, number);
    }

    /**
     * Sends a {@code FANTASTIC_FOUR} effect response.
     *
     * <p>The number may be {@code null} if only a color should be requested.</p>
     *
     * @param color the selected color
     * @param number the selected number, or {@code null}
     */
    public void resolveFantasticFour(CardColor color, Integer number) {
        sendColorOrNumberEffect("FANTASTIC_FOUR", color, number);
    }

    /**
     * Sends an {@code EQUALITY} effect response.
     *
     * @param targetPlayer the player who should draw up
     * @param color the requested color for the next play
     */
    public void resolveEquality(String targetPlayer, CardColor color) {
        Objects.requireNonNull(color, "color must not be null");
        sendEffectResponse("EQUALITY", targetPlayer, color.name());
    }

    /**
     * Sends a {@code SECOND_CHANCE} effect response for playing another card.
     *
     * @param cardId the id of the card to play next
     */
    public void resolveSecondChance(int cardId) {
        sendEffectResponse("SECOND_CHANCE", String.valueOf(cardId));
    }

    /**
     * Sends a {@code SECOND_CHANCE} effect response indicating that no card
     * is played and the draw penalty should be applied.
     */
    public void resolveSecondChanceDrawPenalty() {
        send(new Message(Message.Type.EFFECT_RESPONSE, "SECOND_CHANCE|"));
    }


    /**
     * Sends a raw effect response using the unified protocol type.
     *
     * <p>All given parts are joined using {@code |}. Null parts are converted
     * to empty strings.</p>
     *
     * @param parts the payload parts starting with the effect name
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
     * Sends an effect response that requests either one color or one number.
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
            sendEffectResponse(effectName, color.name());
        } else {
            sendEffectResponse(effectName, "", String.valueOf(number));
        }
    }

    /**
     * Joins card ids into the comma-separated format expected by the server.
     *
     * @param cardIds the card ids to join
     * @return a comma-separated card id list
     */
    private String joinCardIds(List<Integer> cardIds) {
        Objects.requireNonNull(cardIds, "cardIds must not be null");

        return cardIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }







    /**
     * Sends a raw terminal-style command by parsing it into a structured message.
     *
     * <p>This exists for the manual command field and terminal input. Internal
     * GUI logic should prefer the typed methods above.</p>
     *
     * @param rawInput the raw user-entered command
     * @return the parsed message, or {@code null} if invalid
     */
    public Message parseRawCommand(String rawInput) {
        return Message.parse(rawInput);
    }
}