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

        /** Message used to request or transmit the player list. */
        PLAYERS,

        /** Message used to request the start of the game. */
        START,

        /** Message used to leave the server. */
        QUIT,

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

                case "/players" -> new Message(Type.PLAYERS, "");
                case "/start" -> new Message(Type.START, "");
                case "/quit" -> new Message(Type.QUIT, "");
                case "/create" -> new Message(Type.CREATE, payload);
                case "/join" -> new Message(Type.JOIN, payload);
                case "/lobbies" -> new Message(Type.LOBBIES, "");
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
            case "START" -> Type.START;
            case "QUIT" -> Type.QUIT;
            case "CREATE" -> Type.CREATE;
            case "JOIN" -> Type.JOIN;
            case "LOBBIES" -> Type.LOBBIES;
            case "PING" -> Type.PING;
            case "PONG" -> Type.PONG;
            case "INFO" -> Type.INFO;
            case "ERROR" -> Type.ERROR;
            case "GAME" -> Type.GAME;
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
            case NAME, GLOBALCHAT, LOBBYCHAT, WHISPERCHAT, PLAYERS, INFO, ERROR, GAME, CREATE, LOBBIES, JOIN -> true;
            case START, QUIT, PING, PONG, UNKNOWN -> false;
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
            case START, QUIT, PING, PONG -> safe(content).isBlank();
            case NAME, GLOBALCHAT, LOBBYCHAT, WHISPERCHAT, PLAYERS, INFO, ERROR, GAME, CREATE, LOBBIES, JOIN -> true;
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