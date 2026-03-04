package ch.unibas.dmi.dbis.cs108.example.service;

public record Message(Type type, String content) {

    public enum Type {
        NAME,
        CHAT,
        PLAYERS,
        START,
        QUIT,
        UNKNOWN
    }

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