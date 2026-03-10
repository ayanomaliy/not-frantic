package ch.unibas.dmi.dbis.cs108.example.service;

/**
 * Represents a message exchanged between client and server.
 *
 * <p>This class models both application-level messages such as naming,
 * chatting, listing players, starting, and quitting, as well as
 * system-level heartbeat messages such as {@code SYS|PING} and
 * {@code SYS|PONG}.
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
        UNKNOWN
    }

    /**
     * Parses a raw protocol line into a {@code Message}.
     *
     * <p>Currently supported formats:
     * <ul>
     *   <li>{@code /name <username>}</li>
     *   <li>{@code /chat <message>}</li>
     *   <li>{@code /players}</li>
     *   <li>{@code /start}</li>
     *   <li>{@code /quit}</li>
     *   <li>{@code SYS|PING}</li>
     *   <li>{@code SYS|PONG}</li>
     * </ul>
     *
     * @param raw the raw line to parse
     * @return a parsed {@code Message}, or {@code null} if the input is null or blank
     */
    public static Message parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();

        if (trimmed.equalsIgnoreCase("SYS|PING")) {
            return new Message(Type.PING, "");
        }
        if (trimmed.equalsIgnoreCase("SYS|PONG")) {
            return new Message(Type.PONG, "");
        }

        if (trimmed.startsWith("/name ")) {
            return new Message(Type.NAME, trimmed.substring(6).trim());
        }
        if (trimmed.startsWith("/chat ")) {
            return new Message(Type.CHAT, trimmed.substring(6).trim());
        }
        if (trimmed.equals("/players")) {
            return new Message(Type.PLAYERS, "");
        }
        if (trimmed.equals("/start")) {
            return new Message(Type.START, "");
        }
        if (trimmed.equals("/quit")) {
            return new Message(Type.QUIT, "");
        }

        return new Message(Type.UNKNOWN, trimmed);
    }

    /**
     * Encodes this message back into the current wire format.
     *
     * @return the protocol line representing this message
     */
    public String encode() {
        return switch (type) {
            case NAME -> "/name " + content;
            case CHAT -> "/chat " + content;
            case PLAYERS -> "/players";
            case START -> "/start";
            case QUIT -> "/quit";
            case PING -> "SYS|PING";
            case PONG -> "SYS|PONG";
            case UNKNOWN -> content == null ? "" : content;
        };
    }
}