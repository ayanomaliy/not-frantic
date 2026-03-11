package ch.unibas.dmi.dbis.cs108.example.service;

/**
 * Represents a message exchanged between client and server.
 *
 * @param type the message type
 * @param content the optional message content or arguments
 */
public record Message(Type type, String content) {

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
                case "/chat" -> new Message(Type.CHAT, payload);
                case "/players" -> new Message(Type.PLAYERS, "");
                case "/start" -> new Message(Type.START, "");
                case "/quit" -> new Message(Type.QUIT, "");
                default -> new Message(Type.UNKNOWN, trimmed);
            };
        }

        return new Message(Type.UNKNOWN, trimmed);
    }

    private static Type parseType(String typeText) {
        return switch (typeText) {
            case "NAME" -> Type.NAME;
            case "CHAT" -> Type.CHAT;
            case "PLAYERS" -> Type.PLAYERS;
            case "START" -> Type.START;
            case "QUIT" -> Type.QUIT;
            case "PING" -> Type.PING;
            case "PONG" -> Type.PONG;
            case "INFO" -> Type.INFO;
            case "ERROR" -> Type.ERROR;
            case "GAME" -> Type.GAME;
            default -> Type.UNKNOWN;
        };
    }

    public String encode() {
        return type.name() + "|" + safe(content);
    }

    public boolean expectsContent() {
        return switch (type) {
            case NAME, CHAT, PLAYERS, INFO, ERROR, GAME -> true;
            case START, QUIT, PING, PONG, UNKNOWN -> false;
        };
    }

    public boolean hasValidStructure() {
        if (type == Type.UNKNOWN) {
            return false;
        }

        return switch (type) {
            case START, QUIT, PING, PONG -> safe(content).isBlank();
            case NAME, CHAT, PLAYERS, INFO, ERROR, GAME -> true;
            case UNKNOWN -> false;
        };
    }

    public String[] splitChatPayload() {
        String[] parts = safe(content).split("\\|", 2);
        if (parts.length < 2) {
            return new String[]{"?", safe(content)};
        }
        return new String[]{parts[0], parts[1]};
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}