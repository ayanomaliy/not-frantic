package ch.unibas.dmi.dbis.cs108.example.prototype.service;

/**
 * Represents a message exchanged between client and server.
 * <p>
 * Each message has a type (command) and optional content. Messages are parsed
 * from client input using slash commands (e.g., "/name Alice").
 * </p>
 *
 * @param type the message type/command
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
        UNKNOWN
    }

    /**
     * Parses a raw command string into a Message.
     * <p>
     * Recognized commands are:
     * <ul>
     *   <li>{@code /name <username>} – NAME message</li>
     *   <li>{@code /chat <message>} – CHAT message</li>
     *   <li>{@code /players} – PLAYERS message</li>
     *   <li>{@code /start} – START message</li>
     *   <li>{@code /quit} – QUIT message</li>
     *   <li>Any other input – UNKNOWN message</li>
     * </ul>
     * </p>
     *
     * @param raw the raw command string to parse
     * @return a Message with appropriate type and content, or null if raw is empty/blank
     */
    public static Message parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();

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
}