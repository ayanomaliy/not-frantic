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

        /** Client requests automatic reconnection to a running game. */
        RECONNECT,

        /** Server sends a private reconnect token to the client. */
        RECONNECT_TOKEN,

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

        /** Message used to request or transmit the list of all existing lobbies. */
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

        /** Message used to broadcast a text to all clients. */
        BROADCAST,

        /** Cheat command to instantly win the match. */
        CHEATWIN,

        // ---- Game-action messages (client → server) ----

        /** Client requests to play a card. Payload: card id. */
        PLAY_CARD,

        /** Client requests to draw a card. No payload. */
        DRAW_CARD,

        /** Client explicitly ends their turn. No payload. */
        END_TURN,

        /** Client submits arguments for a pending special effect. */
        EFFECT_RESPONSE,

        /** Client requests their current hand. */
        GET_HAND,

        /** Client requests the current public game state. */
        GET_GAME_STATE,

        /** Client requests the most recent round scores. */
        GET_ROUND_END,

        /** Client requests the final game result. */
        GET_GAME_END,

        // ---- Game-state messages (server → client) ----

        /** Server broadcasts the public game state. */
        GAME_STATE,

        /** Server sends a player's private hand. */
        HAND_UPDATE,

        /** Server requests effect-resolution arguments. */
        EFFECT_REQUEST,

        /** Server broadcasts end-of-round scores. */
        ROUND_END,

        /** Server broadcasts the game-over result. */
        GAME_END,

        /** Client requests to start the next round. */
        START_NEXT_ROUND,

        /** Server notifies all clients that a new round is starting. */
        NEXT_ROUND,

        /** Message used to enable or configure dev mode. */
        DEV,

        /** Client requests the persistent high score list. */
        GET_HIGHSCORES,

        /** Server sends the aggregated high score list. */
        HIGHSCORES,

        /** Fallback type for unrecognized or malformed messages. */
        UNKNOWN
    }

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
                    yield new Message(
                            Type.WHISPERCHAT,
                            whisperParts[0].trim() + "|" + whisperParts[1].trim()
                    );
                }

                case "/broadcast" -> new Message(Type.BROADCAST, payload);
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

                case "/skip" -> parseSingleTargetEffectCommand("SKIP", payload);
                case "/counter" -> parseCounterattackCommand(payload);
                case "/nicetry" -> parseSingleTargetEffectCommand("NICE_TRY", payload);
                case "/gift" -> parseGiftCommand(payload);
                case "/exchange" -> parseExchangeCommand(payload);
                case "/fantastic" -> parseColorOrNumberEffectCommand("FANTASTIC", payload);
                case "/fantasticfour" -> parseFantasticFourCommand(payload);
                case "/equality" -> parseEqualityCommand(payload);
                case "/secondchance" -> parseSecondChanceCommand(payload);

                case "/dev" -> new Message(Type.DEV, payload);
                case "/highscores", "/scores" -> new Message(Type.GET_HIGHSCORES, "");
                case "/nextround" -> new Message(Type.START_NEXT_ROUND, "");

                default -> new Message(Type.UNKNOWN, trimmed);
            };
        }

        return new Message(Type.UNKNOWN, trimmed);
    }

    private static Type parseType(String typeText) {
        return switch (typeText) {
            case "NAME" -> Type.NAME;
            case "RECONNECT" -> Type.RECONNECT;
            case "RECONNECT_TOKEN" -> Type.RECONNECT_TOKEN;
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
            case "BROADCAST" -> Type.BROADCAST;
            case "CHEATWIN" -> Type.CHEATWIN;
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
            case "START_NEXT_ROUND" -> Type.START_NEXT_ROUND;
            case "NEXT_ROUND" -> Type.NEXT_ROUND;
            case "DEV" -> Type.DEV;
            case "GET_HIGHSCORES" -> Type.GET_HIGHSCORES;
            case "HIGHSCORES" -> Type.HIGHSCORES;
            default -> Type.UNKNOWN;
        };
    }

    public String encode() {
        return type.name() + "|" + safe(content);
    }

    public boolean expectsContent() {
        return switch (type) {
            case NAME, RECONNECT, RECONNECT_TOKEN,
                 GLOBALCHAT, LOBBYCHAT, WHISPERCHAT,
                 PLAYERS, ALLPLAYERS,
                 INFO, ERROR, GAME,
                 CREATE, LOBBIES, JOIN,
                 PLAY_CARD, EFFECT_RESPONSE, DEV,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST,
                 ROUND_END, GAME_END, NEXT_ROUND,
                 BROADCAST, HIGHSCORES -> true;

            case START, QUIT, LEAVE,
                 DRAW_CARD, END_TURN,
                 GET_HAND, GET_GAME_STATE, GET_ROUND_END, GET_GAME_END,
                 PING, PONG, CHEATWIN,
                 GET_HIGHSCORES, START_NEXT_ROUND,
                 UNKNOWN -> false;
        };
    }

    public boolean hasValidStructure() {
        if (type == Type.UNKNOWN) {
            return false;
        }

        return switch (type) {
            case START, QUIT, LEAVE,
                 DRAW_CARD, END_TURN,
                 GET_HAND, GET_GAME_STATE, GET_ROUND_END, GET_GAME_END,
                 PING, PONG, CHEATWIN,
                 START_NEXT_ROUND -> safe(content).isBlank();

            case NAME, RECONNECT, RECONNECT_TOKEN,
                 GLOBALCHAT, LOBBYCHAT, WHISPERCHAT,
                 PLAYERS, ALLPLAYERS,
                 INFO, ERROR, GAME,
                 CREATE, LOBBIES, JOIN,
                 PLAY_CARD, EFFECT_RESPONSE, DEV,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST,
                 ROUND_END, GAME_END, NEXT_ROUND,
                 HIGHSCORES, GET_HIGHSCORES,
                 BROADCAST -> true;

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

    private static Message parseSingleTargetEffectCommand(String effectName, String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase());
        }

        return new Message(Type.EFFECT_RESPONSE, effectName + "|" + payload.trim());
    }

    private static Message parseGiftCommand(String payload) {
        String[] parts = payload.split("\\s+");

        if (parts.length < 2 || parts.length > 3) {
            return new Message(Type.UNKNOWN, "/gift " + payload);
        }

        String target = parts[0].trim();
        StringBuilder cardIds = new StringBuilder();

        for (int i = 1; i < parts.length; i++) {
            if (!isInteger(parts[i])) {
                return new Message(Type.UNKNOWN, "/gift " + payload);
            }

            if (cardIds.length() > 0) {
                cardIds.append(",");
            }

            cardIds.append(parts[i].trim());
        }

        return new Message(Type.EFFECT_RESPONSE, "GIFT|" + target + "|" + cardIds);
    }

    private static Message parseExchangeCommand(String payload) {
        String[] parts = payload.split("\\s+");

        if (parts.length != 3) {
            return new Message(Type.UNKNOWN, "/exchange " + payload);
        }

        if (!isInteger(parts[1]) || !isInteger(parts[2])) {
            return new Message(Type.UNKNOWN, "/exchange " + payload);
        }

        return new Message(
                Type.EFFECT_RESPONSE,
                "EXCHANGE|" + parts[0].trim() + "|" + parts[1].trim() + "," + parts[2].trim()
        );
    }

    private static Message parseColorOrNumberEffectCommand(String effectName, String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase());
        }

        String[] parts = payload.trim().split("\\s+");

        if (parts.length != 1) {
            return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase() + " " + payload);
        }

        String token = parts[0].trim();
        String upper = token.toUpperCase();

        if (isPlayableColor(upper)) {
            return new Message(Type.EFFECT_RESPONSE, effectName + "|" + upper);
        }

        if (isPlayableNumber(token)) {
            return new Message(Type.EFFECT_RESPONSE, effectName + "||" + token);
        }

        return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase() + " " + payload);
    }

    private static Message parseEqualityCommand(String payload) {
        String[] parts = payload.split("\\s+");

        if (parts.length != 2) {
            return new Message(Type.UNKNOWN, "/equality " + payload);
        }

        String color = parts[1].trim().toUpperCase();

        if (!isPlayableColor(color)) {
            return new Message(Type.UNKNOWN, "/equality " + payload);
        }

        return new Message(Type.EFFECT_RESPONSE, "EQUALITY|" + parts[0].trim() + "|" + color);
    }

    private static Message parseSecondChanceCommand(String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/secondchance");
        }

        String trimmed = payload.trim();

        if ("draw".equalsIgnoreCase(trimmed)) {
            return new Message(Type.EFFECT_RESPONSE, "SECOND_CHANCE|");
        }

        if (!isInteger(trimmed)) {
            return new Message(Type.UNKNOWN, "/secondchance " + payload);
        }

        return new Message(Type.EFFECT_RESPONSE, "SECOND_CHANCE|" + trimmed);
    }

    private static Message parseCounterattackCommand(String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/counter");
        }

        String[] parts = payload.trim().split("\\s+");

        if (parts.length < 1 || parts.length > 2) {
            return new Message(Type.UNKNOWN, "/counter " + payload);
        }

        String color = parts[0].trim().toUpperCase();

        if (!isPlayableColor(color)) {
            return new Message(Type.UNKNOWN, "/counter " + payload);
        }

        if (parts.length == 1) {
            return new Message(Type.EFFECT_RESPONSE, "COUNTERATTACK||" + color);
        }

        return new Message(Type.EFFECT_RESPONSE, "COUNTERATTACK|" + parts[1].trim() + "|" + color);
    }

    private static Message parseFantasticFourCommand(String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/fantasticfour");
        }

        String[] parts = payload.trim().split("\\s+");

        if (parts.length != 5) {
            return new Message(Type.UNKNOWN, "/fantasticfour " + payload);
        }

        String first = parts[0].trim();
        String upper = first.toUpperCase();

        String targets = parts[1].trim() + ","
                + parts[2].trim() + ","
                + parts[3].trim() + ","
                + parts[4].trim();

        if (isPlayableColor(upper)) {
            return new Message(Type.EFFECT_RESPONSE, "FANTASTIC_FOUR|" + upper + "||" + targets);
        }

        if (isPlayableNumber(first)) {
            return new Message(Type.EFFECT_RESPONSE, "FANTASTIC_FOUR||" + first + "|" + targets);
        }

        return new Message(Type.UNKNOWN, "/fantasticfour " + payload);
    }

    private static boolean isInteger(String text) {
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isPlayableColor(String text) {
        return "RED".equals(text)
                || "GREEN".equals(text)
                || "BLUE".equals(text)
                || "YELLOW".equals(text);
    }

    private static boolean isPlayableNumber(String text) {
        try {
            int number = Integer.parseInt(text.trim());
            return number >= 1 && number <= 9;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}