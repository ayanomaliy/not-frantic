package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

import java.io.IOException;

/**
 * High-level protocol client built on top of {@link NetworkClientCore}.
 *
 * <p>This class exposes uniform typed methods for sending protocol messages,
 * so both CLI and GUI can use the same message-sending API without relying on
 * legacy slash commands internally.</p>
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