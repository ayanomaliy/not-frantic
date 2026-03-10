package ch.unibas.dmi.dbis.cs108.example.service;

/**
 * Represents a message exchanged between client and server.
 *
 * <p>This class models both application-level messages such as naming,
 * chatting, listing players, starting, and quitting, as well as
 * system-level heartbeat messages and structured server responses.
 *
 * @param type the message type
 * @param content the optional message content or arguments
 */
public record Message(Type type, String content) {

    /**
     * Enumeration of supported message types.
     */
    public enum Type {
        NAME,
        CHAT,
        PLAYERS,
        START,
        QUIT,
        PING,
        PONG,
        INFO,
        ERROR,
        GAME,
        UNKNOWN
    }

    /**
     * Parses a raw protocol line into a {@code Message}.
     *
     * <p>Supported formats include both the old slash-command syntax
     * and the newer {@code TYPE|payload} syntax.
     *
     * @param raw the raw line to parse
     * @return a parsed {@code Message}, or {@code null} if the input is null or blank
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
            String content = parts.length > 1 ? parts[1].trim() : "";

            return switch (typeText) {
                case "NAME" -> new Message(Type.NAME, content);
                case "CHAT" -> new Message(Type.CHAT, content);
                case "PLAYERS" -> new Message(Type.PLAYERS, content);
                case "START" -> new Message(Type.START, content);
                case "QUIT" -> new Message(Type.QUIT, content);
                case "PING" -> new Message(Type.PING, content);
                case "PONG" -> new Message(Type.PONG, content);
                case "INFO" -> new Message(Type.INFO, content);
                case "ERROR" -> new Message(Type.ERROR, content);
                case "GAME" -> new Message(Type.GAME, content);
                default -> new Message(Type.UNKNOWN, trimmed);
            };
        }

        // Legacy slash-command format
        if (trimmed.startsWith("/")) {
            String[] parts = trimmed.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String content = parts.length > 1 ? parts[1].trim() : "";

            return switch (command) {
                case "/name" -> new Message(Type.NAME, content);
                case "/chat" -> new Message(Type.CHAT, content);
                case "/players" -> new Message(Type.PLAYERS, "");
                case "/start" -> new Message(Type.START, "");
                case "/quit" -> new Message(Type.QUIT, "");
                default -> new Message(Type.UNKNOWN, trimmed);
            };
        }

        return new Message(Type.UNKNOWN, trimmed);
    }

    /**
     * Encodes this message into the unified {@code TYPE|payload} wire format.
     *
     * @return the encoded protocol line
     */
    public String encode() {
        return switch (type) {
            case NAME -> "NAME|" + safe(content);
            case CHAT -> "CHAT|" + safe(content);
            case PLAYERS -> "PLAYERS|" + safe(content);
            case START -> "START|" + safe(content);
            case QUIT -> "QUIT|" + safe(content);
            case PING -> "PING|" + safe(content);
            case PONG -> "PONG|" + safe(content);
            case INFO -> "INFO|" + safe(content);
            case ERROR -> "ERROR|" + safe(content);
            case GAME -> "GAME|" + safe(content);
            case UNKNOWN -> "UNKNOWN|" + safe(content);
        };
    }

    /**
     * Returns a legacy string representation for compatibility with older code.
     *
     * @return the encoded message in the old protocol format
     */
    public String encodeLegacy() {
        return switch (type) {
            case NAME -> "/name " + safe(content);
            case CHAT -> "/chat " + safe(content);
            case PLAYERS -> "/players";
            case START -> "/start";
            case QUIT -> "/quit";
            case PING -> "SYS|PING";
            case PONG -> "SYS|PONG";
            case INFO -> "INFO|" + safe(content);
            case ERROR -> "ERROR|" + safe(content);
            case GAME -> "GAME|" + safe(content);
            case UNKNOWN -> safe(content);
        };
    }

    /**
     * Returns whether this message type normally expects payload content.
     *
     * @return true if the message should carry content
     */
    public boolean expectsContent() {
        return switch (type) {
            case NAME, CHAT, INFO, ERROR, GAME -> true;
            case PLAYERS, START, QUIT, PING, PONG, UNKNOWN -> false;
        };
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}