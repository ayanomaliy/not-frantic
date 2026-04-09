package ch.unibas.dmi.dbis.cs108.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link Message} record.
 *
 * <p>These tests focus on protocol parsing, encoding, payload handling,
 * and structure validation so that the service package gains coverage
 * on its most important protocol logic.</p>
 */
class MessageTest {

    /**
     * Verifies that {@code null} input is rejected.
     */
    @Test
    void parseReturnsNullForNullInput() {
        assertNull(Message.parse(null));
    }

    /**
     * Verifies that blank input is rejected.
     */
    @Test
    void parseReturnsNullForBlankInput() {
        assertNull(Message.parse("   "));
    }

    /**
     * Verifies parsing of the legacy heartbeat ping format.
     */
    @Test
    void parseRecognizesLegacyPing() {
        Message message = Message.parse("SYS|PING");

        assertNotNull(message);
        assertEquals(Message.Type.PING, message.type());
        assertEquals("", message.content());
    }

    /**
     * Verifies parsing of the legacy heartbeat pong format.
     */
    @Test
    void parseRecognizesLegacyPong() {
        Message message = Message.parse("SYS|PONG");

        assertNotNull(message);
        assertEquals(Message.Type.PONG, message.type());
        assertEquals("", message.content());
    }

    /**
     * Verifies parsing of unified protocol messages with payload.
     */
    @Test
    void parseRecognizesUnifiedMessageWithPayload() {
        Message message = Message.parse("NAME|Ayano");

        assertNotNull(message);
        assertEquals(Message.Type.NAME, message.type());
        assertEquals("Ayano", message.content());
    }

    /**
     * Verifies parsing of unified protocol messages with whitespace around the type.
     */
    @Test
    void parseRecognizesUnifiedMessageWithTrimmedType() {
        Message message = Message.parse("  info|Hello there");

        assertNotNull(message);
        assertEquals(Message.Type.INFO, message.type());
        assertEquals("Hello there", message.content());
    }

    /**
     * Verifies that unknown unified protocol types become UNKNOWN messages.
     */
    @Test
    void parseReturnsUnknownForUnknownUnifiedType() {
        Message message = Message.parse("NOT_A_TYPE|payload");

        assertNotNull(message);
        assertEquals(Message.Type.UNKNOWN, message.type());
        assertEquals("NOT_A_TYPE|payload", message.content());
    }

    /**
     * Verifies parsing of the /name legacy slash command.
     */
    @Test
    void parseRecognizesNameSlashCommand() {
        Message message = Message.parse("/name Ayano");

        assertNotNull(message);
        assertEquals(Message.Type.NAME, message.type());
        assertEquals("Ayano", message.content());
    }

    /**
     * Verifies parsing of global chat aliases.
     */
    @Test
    void parseRecognizesGlobalChatSlashCommandAlias() {
        Message message = Message.parse("/g hello world");

        assertNotNull(message);
        assertEquals(Message.Type.GLOBALCHAT, message.type());
        assertEquals("hello world", message.content());
    }

    /**
     * Verifies parsing of lobby chat aliases.
     */
    @Test
    void parseRecognizesLobbyChatSlashCommandAlias() {
        Message message = Message.parse("/l lobby message");

        assertNotNull(message);
        assertEquals(Message.Type.LOBBYCHAT, message.type());
        assertEquals("lobby message", message.content());
    }

    /**
     * Verifies parsing of whisper commands with target and message.
     */
    @Test
    void parseRecognizesWhisperSlashCommand() {
        Message message = Message.parse("/whisper Bob hello there");

        assertNotNull(message);
        assertEquals(Message.Type.WHISPERCHAT, message.type());
        assertEquals("Bob|hello there", message.content());
    }

    /**
     * Verifies that whisper commands without enough arguments still produce a whisper message.
     */
    @Test
    void parseWhisperWithoutEnoughArgumentsProducesEmptyPayload() {
        Message message = Message.parse("/whisper Bob");

        assertNotNull(message);
        assertEquals(Message.Type.WHISPERCHAT, message.type());
        assertEquals("", message.content());
    }

    /**
     * Verifies parsing of game-related slash commands without payload.
     */
    @Test
    void parseRecognizesNoPayloadSlashCommands() {
        Message hand = Message.parse("/hand");
        Message draw = Message.parse("/draw");
        Message end = Message.parse("/endturn");

        assertNotNull(hand);
        assertNotNull(draw);
        assertNotNull(end);

        assertEquals(Message.Type.GET_HAND, hand.type());
        assertEquals("", hand.content());

        assertEquals(Message.Type.DRAW_CARD, draw.type());
        assertEquals("", draw.content());

        assertEquals(Message.Type.END_TURN, end.type());
        assertEquals("", end.content());
    }

    /**
     * Verifies parsing of payload-based slash commands.
     */
    @Test
    void parseRecognizesPayloadSlashCommands() {
        Message create = Message.parse("/create ChaosLobby");
        Message join = Message.parse("/join ChaosLobby");
        Message play = Message.parse("/play 42");

        assertNotNull(create);
        assertNotNull(join);
        assertNotNull(play);

        assertEquals(Message.Type.CREATE, create.type());
        assertEquals("ChaosLobby", create.content());

        assertEquals(Message.Type.JOIN, join.type());
        assertEquals("ChaosLobby", join.content());

        assertEquals(Message.Type.PLAY_CARD, play.type());
        assertEquals("42", play.content());
    }

    /**
     * Verifies that unknown slash commands become UNKNOWN messages.
     */
    @Test
    void parseReturnsUnknownForUnknownSlashCommand() {
        Message message = Message.parse("/banana stuff");

        assertNotNull(message);
        assertEquals(Message.Type.UNKNOWN, message.type());
        assertEquals("/banana stuff", message.content());
    }

    /**
     * Verifies that plain text without a command or protocol separator becomes UNKNOWN.
     */
    @Test
    void parseReturnsUnknownForPlainText() {
        Message message = Message.parse("just some text");

        assertNotNull(message);
        assertEquals(Message.Type.UNKNOWN, message.type());
        assertEquals("just some text", message.content());
    }

    /**
     * Verifies encoding of a message with content.
     */
    @Test
    void encodeReturnsUnifiedProtocolFormat() {
        Message message = new Message(Message.Type.GLOBALCHAT, "hello");
        assertEquals("GLOBALCHAT|hello", message.encode());
    }

    /**
     * Verifies encoding uses an empty string for null content.
     */
    @Test
    void encodeUsesEmptyStringForNullContent() {
        Message message = new Message(Message.Type.INFO, null);
        assertEquals("INFO|", message.encode());
    }

    /**
     * Verifies that a type expecting content reports true.
     */
    @Test
    void expectsContentReturnsTrueForContentTypes() {
        Message message = new Message(Message.Type.NAME, "Ayano");
        assertTrue(message.expectsContent());
    }

    /**
     * Verifies that a type without payload reports false.
     */
    @Test
    void expectsContentReturnsFalseForControlTypes() {
        Message message = new Message(Message.Type.PING, "");
        assertFalse(message.expectsContent());
    }

    /**
     * Verifies that UNKNOWN does not expect content.
     */
    @Test
    void expectsContentReturnsFalseForUnknown() {
        Message message = new Message(Message.Type.UNKNOWN, "whatever");
        assertFalse(message.expectsContent());
    }

    /**
     * Verifies that control messages are structurally valid only without payload.
     */
    @Test
    void hasValidStructureChecksControlMessages() {
        Message valid = new Message(Message.Type.START, "");
        Message invalid = new Message(Message.Type.START, "unexpected");

        assertTrue(valid.hasValidStructure());
        assertFalse(invalid.hasValidStructure());
    }

    /**
     * Verifies that payload-oriented messages are structurally accepted.
     */
    @Test
    void hasValidStructureAcceptsPayloadMessages() {
        Message withPayload = new Message(Message.Type.CREATE, "Lobby1");
        Message withBlankPayload = new Message(Message.Type.CREATE, "");

        assertTrue(withPayload.hasValidStructure());
        assertTrue(withBlankPayload.hasValidStructure());
    }

    /**
     * Verifies that UNKNOWN messages are never structurally valid.
     */
    @Test
    void hasValidStructureRejectsUnknown() {
        Message message = new Message(Message.Type.UNKNOWN, "something");
        assertFalse(message.hasValidStructure());
    }

    /**
     * Verifies splitting of a normal chat payload with sender and message text.
     */
    @Test
    void splitChatPayloadSplitsSenderAndText() {
        Message message = new Message(Message.Type.GLOBALCHAT, "Alice|Hello");

        String[] parts = message.splitChatPayload();

        assertEquals(2, parts.length);
        assertEquals("Alice", parts[0]);
        assertEquals("Hello", parts[1]);
    }

    /**
     * Verifies fallback behavior when the chat payload has no separator.
     */
    @Test
    void splitChatPayloadFallsBackWhenSeparatorMissing() {
        Message message = new Message(Message.Type.GLOBALCHAT, "Hello without sender");

        String[] parts = message.splitChatPayload();

        assertEquals(2, parts.length);
        assertEquals("?", parts[0]);
        assertEquals("Hello without sender", parts[1]);
    }

    /**
     * Verifies fallback behavior when the chat payload is null.
     */
    @Test
    void splitChatPayloadHandlesNullContent() {
        Message message = new Message(Message.Type.GLOBALCHAT, null);

        String[] parts = message.splitChatPayload();

        assertEquals(2, parts.length);
        assertEquals("?", parts[0]);
        assertEquals("", parts[1]);
    }
}