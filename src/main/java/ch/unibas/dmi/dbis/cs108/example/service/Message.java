package ch.unibas.dmi.dbis.cs108.example.service;

/**
 * Represents a message exchanged between client and server.
 *
 * @param type the message type
 * @param content the optional message content or arguments
 */
public record Message(Type type, String content) {

    /**
     * Supported protocol message types exchanged between client and server.
     */
    public enum Type {
        /** Message used to set or change a player's display name. */
        NAME,

        /** Message containing a global chat text. */
        GLOBALCHAT,

        /** Message containing a lobby chat text. */
        LOBBYCHAT,

        /** Message containing a whisper chat payload. */
        WHISPERCHAT,

        /** Message used to request or transmit the player list of the lobby. */
        PLAYERS,

        /** Message used to request or transmit the global player list. */
        ALLPLAYERS,

        /** Message used to request the start of the game. */
        START,

        /** Message used to leave the server. */
        QUIT,

        /** Message used to leave the current lobby. */
        LEAVE,

        /** Message used to create a new lobby. */
        CREATE,

        /** Message to give a list of all existing lobbies*/
        LOBBIES,

        /** Message used to join an existing lobby. */
        JOIN,

        /** Heartbeat ping message. */
        PING,

        /** Heartbeat pong response message. */
        PONG,

        /** Informational message sent by the server. */
        INFO,

        /** Error message sent by the server. */
        ERROR,

        /** Game-related message sent by the server. */
        GAME,

        // ---- Game-action messages (client → server) ----

        /** Client requests to play a card. Payload: card id. */
        PLAY_CARD,

        /** Client requests to draw a card. No payload. */
        DRAW_CARD,

        /** Client explicitly ends their turn (used after drawing without playing). No payload. */
        END_TURN,

        /** Client submits arguments for a pending special effect. Payload: effect|arg1|arg2|… */
        EFFECT_RESPONSE,

        /** Client requests their current hand. Server responds with HAND_UPDATE. No payload. */
        GET_HAND,

        /** Client requests the current public game state. Server responds with GAME_STATE. No payload. */
        GET_GAME_STATE,

        /** Client requests the most recent round scores. Server responds with ROUND_END. No payload. */
        GET_ROUND_END,

        /** Client requests the final game result. Server responds with GAME_END. No payload. */
        GET_GAME_END,

        // ---- Game-state messages (server → client) ----

        /** Server broadcasts the public game state. Payload: serialized key:value pairs. */
        GAME_STATE,

        /** Server sends a player's private hand. Payload: comma-separated card ids. */
        HAND_UPDATE,

        /** Server requests effect-resolution arguments from the acting player. Payload: effect|context. */
        EFFECT_REQUEST,

        /** Server broadcasts end-of-round scores. Payload: name:roundScore:totalScore per player. */
        ROUND_END,

        /** Server broadcasts the game-over result. Payload: winner name. */
        GAME_END,

        /** Fallback type for unrecognized or malformed messages. */
        UNKNOWN
    }

    /**
     * Parses a raw protocol string into a {@code Message}.
     *
     * <p>This method supports legacy heartbeat messages, the unified
     * {@code TYPE|payload} format, and legacy slash commands such as
     * {@code /chat Hello}.</p>
     *
     * @param raw the raw input string to parse
     * @return the parsed message, or {@code null} if the input is null or blank
     */
    public static Message parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();

        // Legacy heartbeat format
        if (trimmed.equalsIgnoreCase("SYS|PING")) {
            return new Message(Type.PING, "");
        }
        if (trimmed.equalsIgnoreCase("SYS|PONG")) {
            return new Message(Type.PONG, "");
        }

        // Unified TYPE|payload format
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|", 2);
            String typeText = parts[0].trim().toUpperCase();
            String payload = parts.length > 1 ? parts[1] : "";

            Type type = parseType(typeText);
            if (type == Type.UNKNOWN) {
                return new Message(Type.UNKNOWN, trimmed);
            }

            return new Message(type, payload);
        }

        // Legacy slash-command format
        if (trimmed.startsWith("/")) {
            String[] parts = trimmed.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String payload = parts.length > 1 ? parts[1].trim() : "";

            return switch (command) {
                case "/name" -> new Message(Type.NAME, payload);
                case "/chat", "/g", "/global" -> new Message(Type.GLOBALCHAT, payload);

                case "/l", "/lobby" -> new Message(Type.LOBBYCHAT, payload);

                case "/w", "/whisper", "/msg", "/tell" -> {
                    String[] whisperParts = payload.split("\\s+", 2);
                    if (whisperParts.length < 2) {
                        yield new Message(Type.WHISPERCHAT, "");
                    }
                    yield new Message(Type.WHISPERCHAT, whisperParts[0].trim() + "|" + whisperParts[1].trim());
                }

                case "/hand" -> new Message(Type.GET_HAND, "");
                case "/gamestate" -> new Message(Type.GET_GAME_STATE, "");
                case "/roundend" -> new Message(Type.GET_ROUND_END, "");
                case "/gameend" -> new Message(Type.GET_GAME_END, "");
                case "/players" -> new Message(Type.PLAYERS, "");
                case "/allplayers" -> new Message(Type.ALLPLAYERS, "");
                case "/start" -> new Message(Type.START, "");
                case "/quit" -> new Message(Type.QUIT, "");
                case "/leave" -> new Message(Type.LEAVE, "");
                case "/create" -> new Message(Type.CREATE, payload);
                case "/join" -> new Message(Type.JOIN, payload);
                case "/lobbies" -> new Message(Type.LOBBIES, "");
                case "/play", "/card" -> new Message(Type.PLAY_CARD, payload);
                case "/draw", "/pickup" -> new Message(Type.DRAW_CARD, "");
                case "/end", "/endturn" -> new Message(Type.END_TURN, "");
                default -> new Message(Type.UNKNOWN, trimmed);
            };
        }

        return new Message(Type.UNKNOWN, trimmed);
    }

    /**
     * Parses a protocol type string into a message type.
     *
     * @param typeText the raw type text
     * @return the corresponding message type or {@link Type#UNKNOWN}
     */
    private static Type parseType(String typeText) {
        return switch (typeText) {
            case "NAME" -> Type.NAME;
            case "GLOBALCHAT" -> Type.GLOBALCHAT;
            case "LOBBYCHAT" -> Type.LOBBYCHAT;
            case "WHISPERCHAT" -> Type.WHISPERCHAT;
            case "PLAYERS" -> Type.PLAYERS;
            case "ALLPLAYERS" -> Type.ALLPLAYERS;
            case "START" -> Type.START;
            case "QUIT" -> Type.QUIT;
            case "LEAVE" -> Type.LEAVE;
            case "CREATE" -> Type.CREATE;
            case "JOIN" -> Type.JOIN;
            case "LOBBIES" -> Type.LOBBIES;
            case "PING" -> Type.PING;
            case "PONG" -> Type.PONG;
            case "INFO" -> Type.INFO;
            case "ERROR" -> Type.ERROR;
            case "GAME" -> Type.GAME;
            case "PLAY_CARD" -> Type.PLAY_CARD;
            case "DRAW_CARD" -> Type.DRAW_CARD;
            case "END_TURN" -> Type.END_TURN;
            case "EFFECT_RESPONSE" -> Type.EFFECT_RESPONSE;
            case "GET_HAND" -> Type.GET_HAND;
            case "GET_GAME_STATE" -> Type.GET_GAME_STATE;
            case "GET_ROUND_END" -> Type.GET_ROUND_END;
            case "GET_GAME_END" -> Type.GET_GAME_END;
            case "GAME_STATE" -> Type.GAME_STATE;
            case "HAND_UPDATE" -> Type.HAND_UPDATE;
            case "EFFECT_REQUEST" -> Type.EFFECT_REQUEST;
            case "ROUND_END" -> Type.ROUND_END;
            case "GAME_END" -> Type.GAME_END;
            default -> Type.UNKNOWN;
        };
    }

    /**
     * Encodes this message into the unified protocol format.
     *
     * @return the encoded message string in the form {@code TYPE|payload}
     */
    public String encode() {
        return type.name() + "|" + safe(content);
    }

    /**
     * Returns whether this message type normally expects a content payload.
     *
     * @return {@code true} if the type expects content, {@code false} otherwise
     */
    public boolean expectsContent() {
        return switch (type) {
            case NAME, GLOBALCHAT, LOBBYCHAT, WHISPERCHAT, PLAYERS, ALLPLAYERS,
                 INFO, ERROR, GAME, CREATE, LOBBIES, JOIN,
                 PLAY_CARD, EFFECT_RESPONSE,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, ROUND_END, GAME_END -> true;
            case START, QUIT, LEAVE, DRAW_CARD, END_TURN, GET_HAND,
                 GET_GAME_STATE, GET_ROUND_END, GET_GAME_END, PING, PONG, UNKNOWN -> false;
        };
    }

    /**
     * Checks whether this message has a valid structure for its type.
     *
     * <p>For example, heartbeat and control messages such as {@code PING},
     * {@code PONG}, {@code START}, and {@code QUIT} must not contain payload
     * text, while other message types may contain content.</p>
     *
     * @return {@code true} if the message structure is valid, {@code false} otherwise
     */
    public boolean hasValidStructure() {
        if (type == Type.UNKNOWN) {
            return false;
        }

        return switch (type) {
            case START, QUIT, LEAVE, DRAW_CARD, END_TURN, GET_HAND,
                 GET_GAME_STATE, GET_ROUND_END, GET_GAME_END, PING, PONG -> safe(content).isBlank();
            case NAME, GLOBALCHAT, LOBBYCHAT, WHISPERCHAT, PLAYERS, ALLPLAYERS,
                 INFO, ERROR, GAME, CREATE, LOBBIES, JOIN,
                 PLAY_CARD, EFFECT_RESPONSE,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, ROUND_END, GAME_END -> true;
            case UNKNOWN -> false;
        };
    }

    /**
     * Splits a chat payload into sender name and message text.
     *
     * <p>If the payload does not contain the expected separator, the sender
     * is returned as {@code "?"} and the entire payload is treated as the
     * message text.</p>
     *
     * @return a two-element array containing sender name and chat text
     */
    public String[] splitChatPayload() {
        String[] parts = safe(content).split("\\|", 2);
        if (parts.length < 2) {
            return new String[]{"?", safe(content)};
        }
        return new String[]{parts[0], parts[1]};
    }

    /**
     * Returns a non-null string for the given text.
     *
     * @param text the input text
     * @return the same text, or an empty string if null
     */
    private static String safe(String text) {
        return text == null ? "" : text;
    }
}