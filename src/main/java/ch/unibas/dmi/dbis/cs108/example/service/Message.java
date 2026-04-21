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

        /** Message used to enable or configure dev mode for the current lobby. */
        DEV,

        /** Client requests the persistent high score list. Server responds with HIGHSCORES. No payload. */
        GET_HIGHSCORES,

        /** Server sends the aggregated high score list. Payload: player:wins:gamesPlayed:totalPenaltyPoints,... */
        HIGHSCORES,

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
                case "/cheatwin" -> new Message(Type.CHEATWIN, "");

                case "/skip" -> parseSingleTargetEffectCommand("SKIP", payload);
                case "/counter" -> parseCounterattackCommand(payload);                case "/nicetry" -> parseSingleTargetEffectCommand("NICE_TRY", payload);
                case "/gift" -> parseGiftCommand(payload);
                case "/exchange" -> parseExchangeCommand(payload);
                case "/fantastic" -> parseColorOrNumberEffectCommand("FANTASTIC", payload);
                case "/fantasticfour" -> parseFantasticFourCommand(payload);
                case "/equality" -> parseEqualityCommand(payload);
                case "/secondchance" -> parseSecondChanceCommand(payload);

                case "/dev" -> new Message(Type.DEV, payload);

                case "/highscores", "/scores" -> new Message(Type.GET_HIGHSCORES, "");

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
            case "BROADCAST" -> Type.BROADCAST;
            case "CHEATWIN" -> Type.CHEATWIN;
            case "DEV" -> Type.DEV;
            case "GET_HIGHSCORES" -> Type.GET_HIGHSCORES;
            case "HIGHSCORES" -> Type.HIGHSCORES;
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
                 PLAY_CARD, EFFECT_RESPONSE, DEV,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, ROUND_END, GAME_END, BROADCAST -> true;
            case START, QUIT, LEAVE, DRAW_CARD, END_TURN, GET_HAND,
                 GET_GAME_STATE, GET_ROUND_END, GET_GAME_END, PING, PONG, CHEATWIN, HIGHSCORES, GET_HIGHSCORES, UNKNOWN -> false;
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
                 GET_GAME_STATE, GET_ROUND_END, GET_GAME_END, PING, PONG, CHEATWIN -> safe(content).isBlank();
            case NAME, GLOBALCHAT, LOBBYCHAT, WHISPERCHAT, PLAYERS, ALLPLAYERS,
                 INFO, ERROR, GAME, CREATE, LOBBIES, JOIN,
                 PLAY_CARD, EFFECT_RESPONSE, DEV,
                 GAME_STATE, HAND_UPDATE, EFFECT_REQUEST, ROUND_END, GAME_END, HIGHSCORES, GET_HIGHSCORES,BROADCAST -> true;
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

    /**
     * Parses a slash command for an effect that only needs one target player.
     *
     * @param effectName the protocol effect name
     * @param payload the raw payload after the slash command
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
    private static Message parseSingleTargetEffectCommand(String effectName, String payload) {
        if (payload == null || payload.isBlank()) {
            return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase());
        }

        String target = payload.trim();
        return new Message(Type.EFFECT_RESPONSE, effectName + "|" + target);
    }

    /**
     * Parses a human-friendly {@code /gift} command.
     *
     * <p>Format: {@code /gift <player> <cardId1> [cardId2]}</p>
     *
     * @param payload the raw payload after {@code /gift}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
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

    /**
     * Parses a human-friendly {@code /exchange} command.
     *
     * <p>Format: {@code /exchange <player> <cardId1> <cardId2>}</p>
     *
     * @param payload the raw payload after {@code /exchange}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
    private static Message parseExchangeCommand(String payload) {
        String[] parts = payload.split("\\s+");
        if (parts.length != 3) {
            return new Message(Type.UNKNOWN, "/exchange " + payload);
        }

        if (!isInteger(parts[1]) || !isInteger(parts[2])) {
            return new Message(Type.UNKNOWN, "/exchange " + payload);
        }

        String target = parts[0].trim();
        return new Message(Type.EFFECT_RESPONSE,
                "EXCHANGE|" + target + "|" + parts[1].trim() + "," + parts[2].trim());
    }

    /**
     * Parses a human-friendly Fantastic-style command that may request either
     * one playable color or one number, but never both.
     *
     * <p>Valid examples:</p>
     * <ul>
     *   <li>{@code /fantastic red}</li>
     *   <li>{@code /fantastic 4}</li>
     *   <li>{@code /fantasticfour blue}</li>
     *   <li>{@code /fantasticfour 7}</li>
     * </ul>
     *
     * @param effectName the protocol effect name
     * @param payload the raw payload after the slash command
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
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

        if (isInteger(token)) {
            return new Message(Type.EFFECT_RESPONSE, effectName + "||" + token);
        }

        return new Message(Type.UNKNOWN, "/" + effectName.toLowerCase() + " " + payload);
    }

    /**
     * Parses a human-friendly {@code /equality} command.
     *
     * <p>Format: {@code /equality <player> <color>}</p>
     *
     * @param payload the raw payload after {@code /equality}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
    private static Message parseEqualityCommand(String payload) {
        String[] parts = payload.split("\\s+");
        if (parts.length != 2) {
            return new Message(Type.UNKNOWN, "/equality " + payload);
        }

        String target = parts[0].trim();
        String color = parts[1].trim().toUpperCase();

        if (!isPlayableColor(color)) {
            return new Message(Type.UNKNOWN, "/equality " + payload);
        }

        return new Message(Type.EFFECT_RESPONSE, "EQUALITY|" + target + "|" + color);
    }

    /**
     * Parses a human-friendly {@code /secondchance} command.
     *
     * <p>Formats:</p>
     * <ul>
     *   <li>{@code /secondchance <cardId>}</li>
     *   <li>{@code /secondchance draw}</li>
     * </ul>
     *
     * @param payload the raw payload after {@code /secondchance}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
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

    /**
     * Parses a human-friendly {@code /counter} command.
     *
     * <p>Supported formats:</p>
     * <ul>
     *   <li>{@code /counter <color>}</li>
     *   <li>{@code /counter <color> <player>}</li>
     * </ul>
     *
     * <p>The command always carries a requested color. If a target player is also
     * given, the wire format includes both target and color. If no target is
     * given, the target field is left empty.</p>
     *
     * @param payload the raw payload after {@code /counter}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
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

        String target = parts[1].trim();
        if (target.isBlank()) {
            return new Message(Type.UNKNOWN, "/counter " + payload);
        }

        return new Message(Type.EFFECT_RESPONSE, "COUNTERATTACK|" + target + "|" + color);
    }
    /**
     * Returns whether the given text is a valid integer.
     *
     * @param text the raw text to test
     * @return {@code true} if the text can be parsed as an integer
     */
    private static boolean isInteger(String text) {
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns whether the given text is one of the playable requested colors.
     *
     * @param text the color text to test
     * @return {@code true} if the text is {@code RED}, {@code GREEN},
     *         {@code BLUE}, or {@code YELLOW}
     */
    private static boolean isPlayableColor(String text) {
        return "RED".equals(text)
                || "GREEN".equals(text)
                || "BLUE".equals(text)
                || "YELLOW".equals(text);
    }

    /**
     * Parses a human-friendly {@code /fantasticfour} command.
     *
     * <p>Format:</p>
     * <ul>
     *   <li>{@code /fantasticfour <color> <player1> <player2> <player3> <player4>}</li>
     *   <li>{@code /fantasticfour <number> <player1> <player2> <player3> <player4>}</li>
     * </ul>
     *
     * <p>The first argument is either one playable color or one requested number.
     * The next four arguments specify the recipients of the four distributed cards.
     * Repeated player names are allowed.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code /fantasticfour blue Alice Bob Charlie David}</li>
     *   <li>{@code /fantasticfour 7 Alice Alice Bob Charlie}</li>
     * </ul>
     *
     * @param payload the raw payload after {@code /fantasticfour}
     * @return an {@code EFFECT_RESPONSE} message or {@code UNKNOWN} if invalid
     */
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

        String targets = parts[1].trim() + "," +
                parts[2].trim() + "," +
                parts[3].trim() + "," +
                parts[4].trim();

        if (isPlayableColor(upper)) {
            return new Message(Type.EFFECT_RESPONSE, "FANTASTIC_FOUR|" + upper + "||" + targets);
        }

        if (isInteger(first)) {
            return new Message(Type.EFFECT_RESPONSE, "FANTASTIC_FOUR||" + first.trim() + "|" + targets);
        }

        return new Message(Type.UNKNOWN, "/fantasticfour " + payload);
    }
}