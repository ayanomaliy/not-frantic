package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.controller.ClientSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ServerService}.
 *
 * <p>These tests focus on the message-routing, lobby-management, naming,
 * and chat-related behavior of the server service. They intentionally avoid
 * deep game-engine assertions and instead target the server-side coordination
 * logic that is comparatively easy to verify in isolation.</p>
 */
class ServerServiceTest {

    /**
     * Simple test double for {@link ClientSession} that records all outgoing
     * encoded messages instead of writing to a real socket.
     */
    private static class TestClientSession extends ClientSession {

        private final List<String> sentMessages = new ArrayList<>();
        private String playerName = "Anonymous";

        /**
         * Creates a new test session with the default anonymous name.
         */
        TestClientSession() {
            super(null, null, null);
        }

        @Override
        public void send(String text) {
            sentMessages.add(text);
        }

        @Override
        public String getPlayerName() {
            return playerName;
        }

        @Override
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        /**
         * Returns all sent raw protocol messages.
         *
         * @return the recorded outgoing messages
         */
        List<String> getSentMessages() {
            return sentMessages;
        }

        /**
         * Returns the most recently sent message.
         *
         * @return the last sent message, or {@code null} if none was sent
         */
        String getLastMessage() {
            if (sentMessages.isEmpty()) {
                return null;
            }
            return sentMessages.get(sentMessages.size() - 1);
        }

        /**
         * Returns whether any sent message contains the given fragment.
         *
         * @param text the fragment to search for
         * @return {@code true} if a sent message contains the text
         */
        boolean containsSentMessage(String text) {
            return sentMessages.stream().anyMatch(m -> m.contains(text));
        }

        /**
         * Counts how many sent messages contain the given fragment.
         *
         * @param text the fragment to search for
         * @return the number of matching messages
         */
        long countMessagesContaining(String text) {
            return sentMessages.stream().filter(m -> m.contains(text)).count();
        }
    }

    /**
     * Registers a client and verifies that initial information is sent.
     */
    @Test
    void registerClientSendsInitialInfoAndLists() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        assertTrue(session.containsSentMessage("INFO|Connected to server."));
        assertTrue(session.containsSentMessage("LOBBIES|"));
        assertTrue(session.containsSentMessage("ALLPLAYERS|Anonymous"));
    }

    /**
     * Verifies that malformed messages are rejected.
     */
    @Test
    void handleMessageRejectsMalformedProtocolMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.handleMessage(session, null);

        assertEquals("ERROR|Malformed protocol message.", session.getLastMessage());
    }

    /**
     * Verifies that an empty name is rejected.
     */
    @Test
    void handleNameRejectsEmptyName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, "   "));

        assertEquals("ERROR|Name cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that a first-time name assignment succeeds.
     */
    @Test
    void handleNameSetsInitialName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, "Ayano"));

        assertEquals("Ayano", session.getPlayerName());
        assertTrue(session.containsSentMessage("INFO|Your name has been set to Ayano"));
    }

    /**
     * Verifies that duplicate names become unique automatically.
     */
    @Test
    void handleNameMakesDuplicateNamesUnique() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Ayano"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Ayano"));

        assertEquals("Ayano", first.getPlayerName());
        assertEquals("Ayano(2)", second.getPlayerName());
        assertTrue(second.containsSentMessage("INFO|Your requested name was taken. Your name has been set to Ayano(2)"));
    }

    /**
     * Verifies that creating a lobby succeeds and produces the expected updates.
     */
    @Test
    void handleCreateLobbyCreatesLobbyAndJoinsCreator() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.NAME, "Ayano"));

        service.handleMessage(session, new Message(Message.Type.CREATE, "ChaosLobby"));

        assertTrue(session.containsSentMessage("INFO|Lobby created and joined: ChaosLobby"));
        assertTrue(session.containsSentMessage("INFO|Ayano joined the lobby."));
        assertTrue(session.containsSentMessage("PLAYERS|Ayano"));
        assertTrue(session.containsSentMessage("LOBBIES|ChaosLobby:WAITING:1:5"));
    }

    /**
     * Verifies that creating a lobby with an invalid name is rejected.
     */
    @Test
    void handleCreateLobbyRejectsInvalidCharacters() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.CREATE, "bad|name"));

        assertEquals("ERROR|Lobby name contains invalid characters.", session.getLastMessage());
    }

    /**
     * Verifies that a second client can join an existing lobby.
     */
    @Test
    void handleJoinLobbyLetsPlayerJoinExistingLobby() {
        ServerService service = new ServerService();

        TestClientSession owner = new TestClientSession();
        TestClientSession joiner = new TestClientSession();

        service.registerClient(owner);
        service.registerClient(joiner);

        service.handleMessage(owner, new Message(Message.Type.NAME, "Owner"));
        service.handleMessage(joiner, new Message(Message.Type.NAME, "Joiner"));

        service.handleMessage(owner, new Message(Message.Type.CREATE, "ChaosLobby"));
        service.handleMessage(joiner, new Message(Message.Type.JOIN, "ChaosLobby"));

        assertTrue(joiner.containsSentMessage("INFO|Joined lobby: ChaosLobby"));
        assertTrue(owner.containsSentMessage("INFO|Joiner joined the lobby."));
        assertTrue(joiner.containsSentMessage("PLAYERS|Owner,Joiner")
                || joiner.containsSentMessage("PLAYERS|Joiner,Owner"));
    }

    /**
     * Verifies that joining a non-existing lobby is rejected.
     */
    @Test
    void handleJoinLobbyRejectsMissingLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.JOIN, "DoesNotExist"));

        assertEquals("ERROR|Lobby does not exist.", session.getLastMessage());
    }

    /**
     * Verifies that leaving a lobby sends a confirmation and clears the player's
     * local lobby player list.
     */
    @Test
    void handleLeaveRemovesPlayerFromLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.NAME, "Ayano"));
        service.handleMessage(session, new Message(Message.Type.CREATE, "ChaosLobby"));

        service.handleMessage(session, new Message(Message.Type.LEAVE, ""));

        assertTrue(session.containsSentMessage("INFO|You left lobby: ChaosLobby"));
        assertTrue(session.containsSentMessage("PLAYERS|"));
    }

    /**
     * Verifies that lobby chat is rejected when the player is not inside a lobby.
     */
    @Test
    void handleLobbyChatRejectsPlayerWithoutLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.LOBBYCHAT, "hello"));

        assertEquals("ERROR|You are not in a lobby.", session.getLastMessage());
    }

    /**
     * Verifies that global chat is broadcast to all connected clients.
     */
    @Test
    void handleGlobalChatBroadcastsToAllConnectedClients() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.handleMessage(first, new Message(Message.Type.GLOBALCHAT, "hello world"));

        assertTrue(first.containsSentMessage("GLOBALCHAT|Alice|hello world"));
        assertTrue(second.containsSentMessage("GLOBALCHAT|Alice|hello world"));
    }

    /**
     * Verifies that empty global chat is rejected.
     */
    @Test
    void handleGlobalChatRejectsEmptyMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.GLOBALCHAT, "   "));

        assertEquals("ERROR|Global chat message cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that whispering to another connected player delivers a private
     * message to the target and a confirmation copy to the sender.
     */
    @Test
    void handleWhisperChatSendsToTargetAndSender() {
        ServerService service = new ServerService();
        TestClientSession sender = new TestClientSession();
        TestClientSession target = new TestClientSession();

        service.registerClient(sender);
        service.registerClient(target);

        service.handleMessage(sender, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(target, new Message(Message.Type.NAME, "Bob"));

        sender.getSentMessages().clear();
        target.getSentMessages().clear();

        service.handleMessage(sender, new Message(Message.Type.WHISPERCHAT, "Bob|secret"));

        assertTrue(target.containsSentMessage("WHISPERCHAT|FROM|Alice|secret"));
        assertTrue(sender.containsSentMessage("WHISPERCHAT|TO|Bob|secret"));
    }

    /**
     * Verifies that whispering to oneself is rejected.
     */
    @Test
    void handleWhisperChatRejectsWhisperToSelf() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.NAME, "Alice"));

        service.handleMessage(session, new Message(Message.Type.WHISPERCHAT, "Alice|hello"));

        assertEquals("ERROR|You cannot whisper to yourself.", session.getLastMessage());
    }

    /**
     * Verifies that broadcast messages are delivered to all connected clients.
     */
    @Test
    void handleBroadcastSendsToAllConnectedClients() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.handleMessage(first, new Message(Message.Type.BROADCAST, "Server-wide announcement"));

        assertTrue(first.containsSentMessage("BROADCAST|Alice|Server-wide announcement"));
        assertTrue(second.containsSentMessage("BROADCAST|Alice|Server-wide announcement"));
    }

    /**
     * Verifies that a client may not send a server-only message type.
     */
    @Test
    void handleMessageRejectsServerOnlyMessageTypes() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.GAME_STATE, "phase:TURN_START"));

        assertEquals("ERROR|Client may not send server-only message types.", session.getLastMessage());
    }

    /**
     * Verifies that requesting the player list outside a lobby is rejected.
     */
    @Test
    void handlePlayersRejectsClientWithoutLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.PLAYERS, ""));

        assertEquals("ERROR|You are not in a lobby.", session.getLastMessage());
    }

    /**
     * Verifies that requesting all players returns the global player list.
     */
    @Test
    void handleAllPlayersReturnsConnectedClients() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));

        second.getSentMessages().clear();

        service.handleMessage(second, new Message(Message.Type.ALLPLAYERS, ""));

        assertEquals("ALLPLAYERS|Alice,Bob", second.getLastMessage());
    }

    /**
     * Verifies that unregistering a client removes them from the global player list.
     */
    @Test
    void unregisterClientUpdatesGlobalPlayerList() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.unregisterClient(second);

        assertTrue(first.containsSentMessage("ALLPLAYERS|Alice"));
    }


    /**
     * Creates a string consisting of one repeated character.
     *
     * @param c the repeated character
     * @param count the number of repetitions
     * @return the generated string
     */
    private static String repeated(char c, int count) {
        return String.valueOf(c).repeat(count);
    }

    /**
     * Verifies that renaming to the same name is rejected.
     */
    @Test
    void handleNameRejectsSameName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, "Alice"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.NAME, "Alice"));

        assertEquals("ERROR|You already have this name.", session.getLastMessage());
    }

    /**
     * Verifies that a name containing invalid characters is rejected.
     */
    @Test
    void handleNameRejectsInvalidCharacters() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, "Ali|ce"));

        assertEquals("ERROR|Name contains invalid characters.", session.getLastMessage());
    }

    /**
     * Verifies that a too-long name is rejected.
     */
    @Test
    void handleNameRejectsTooLongName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, repeated('a', 21)));

        assertEquals("ERROR|Name is too long. Maximum 20 characters.", session.getLastMessage());
    }

    /**
     * Verifies that renaming after the first assignment uses the rename wording.
     */
    @Test
    void handleNameUsesRenameMessageAfterInitialNaming() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.NAME, "Alice"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.NAME, "Bob"));

        assertEquals("Bob", session.getPlayerName());
        assertTrue(session.containsSentMessage("INFO|Your name is now Bob"));
    }

    /**
     * Verifies that renaming inside a lobby informs the lobby.
     */
    @Test
    void handleNameBroadcastsRenameInsideLobby() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));

        service.handleMessage(first, new Message(Message.Type.CREATE, "Lobby1"));
        service.handleMessage(second, new Message(Message.Type.JOIN, "Lobby1"));

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.handleMessage(first, new Message(Message.Type.NAME, "Charlie"));

        assertTrue(first.containsSentMessage("INFO|Alice changed their name to Charlie"));
        assertTrue(second.containsSentMessage("INFO|Alice changed their name to Charlie"));
    }

    /**
     * Verifies that creating a lobby with an empty name is rejected.
     */
    @Test
    void handleCreateLobbyRejectsEmptyName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.CREATE, "   "));

        assertEquals("ERROR|Lobby name cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that creating a lobby with a too-long name is rejected.
     */
    @Test
    void handleCreateLobbyRejectsTooLongName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.CREATE, repeated('x', 21)));

        assertEquals("ERROR|Lobby name is too long. Maximum 20 characters.", session.getLastMessage());
    }

    /**
     * Verifies that creating an already existing lobby is rejected.
     */
    @Test
    void handleCreateLobbyRejectsExistingLobby() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.CREATE, "Lobby1"));
        second.getSentMessages().clear();

        service.handleMessage(second, new Message(Message.Type.CREATE, "Lobby1"));

        assertEquals("ERROR|Lobby already exists.", second.getLastMessage());
    }

    /**
     * Verifies that joining a lobby with an empty name is rejected.
     */
    @Test
    void handleJoinLobbyRejectsEmptyName() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();
        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.JOIN, "   "));

        assertEquals("ERROR|Lobby name cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that joining the current lobby again returns an info message.
     */
    @Test
    void handleJoinLobbyRecognizesAlreadyInLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.CREATE, "Lobby1"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.JOIN, "Lobby1"));

        assertEquals("INFO|You are already in lobby: Lobby1", session.getLastMessage());
    }

    /**
     * Verifies that a lobby chat message is broadcast inside a lobby.
     */
    @Test
    void handleLobbyChatBroadcastsWithinLobby() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        service.handleMessage(first, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(second, new Message(Message.Type.NAME, "Bob"));
        service.handleMessage(first, new Message(Message.Type.CREATE, "Lobby1"));
        service.handleMessage(second, new Message(Message.Type.JOIN, "Lobby1"));

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.handleMessage(first, new Message(Message.Type.LOBBYCHAT, "hello lobby"));

        assertTrue(first.containsSentMessage("LOBBYCHAT|Alice|hello lobby"));
        assertTrue(second.containsSentMessage("LOBBYCHAT|Alice|hello lobby"));
    }

    /**
     * Verifies that an empty lobby chat message is rejected.
     */
    @Test
    void handleLobbyChatRejectsEmptyMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.CREATE, "Lobby1"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.LOBBYCHAT, "   "));

        assertEquals("ERROR|Lobby chat message cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that an overly long global chat message is rejected.
     */
    @Test
    void handleGlobalChatRejectsTooLongMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.GLOBALCHAT, repeated('a', 201)));

        assertEquals("ERROR|Global chat message is too long. Maximum 200 characters.", session.getLastMessage());
    }

    /**
     * Verifies that a multiline global chat message is rejected.
     */
    @Test
    void handleGlobalChatRejectsMultilineMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.GLOBALCHAT, "hello\nworld"));

        assertEquals("ERROR|Global chat message contains invalid characters.", session.getLastMessage());
    }

    /**
     * Verifies that an empty whisper payload is rejected.
     */
    @Test
    void handleWhisperChatRejectsEmptyPayload() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.WHISPERCHAT, "   "));

        assertEquals("ERROR|Whisper message cannot be empty.", session.getLastMessage());
    }

    /**
     * Verifies that a malformed whisper payload is rejected.
     */
    @Test
    void handleWhisperChatRejectsMalformedPayload() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.WHISPERCHAT, "BobOnly"));

        assertEquals("ERROR|Usage: /whisper <player> <message>", session.getLastMessage());
    }

    /**
     * Verifies that whispering to an unknown player is rejected.
     */
    @Test
    void handleWhisperChatRejectsUnknownTarget() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.NAME, "Alice"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.WHISPERCHAT, "Bob|secret"));

        assertEquals("ERROR|Player not found: Bob", session.getLastMessage());
    }

    /**
     * Verifies that a too-long whisper message is rejected.
     */
    @Test
    void handleWhisperChatRejectsTooLongMessage() {
        ServerService service = new ServerService();
        TestClientSession sender = new TestClientSession();
        TestClientSession target = new TestClientSession();

        service.registerClient(sender);
        service.registerClient(target);

        service.handleMessage(sender, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(target, new Message(Message.Type.NAME, "Bob"));
        sender.getSentMessages().clear();

        service.handleMessage(sender, new Message(Message.Type.WHISPERCHAT, "Bob|" + repeated('x', 201)));

        assertEquals("ERROR|Whisper message is too long. Maximum 200 characters.", sender.getLastMessage());
    }

    /**
     * Verifies that a multiline whisper message is rejected.
     */
    @Test
    void handleWhisperChatRejectsMultilineMessage() {
        ServerService service = new ServerService();
        TestClientSession sender = new TestClientSession();
        TestClientSession target = new TestClientSession();

        service.registerClient(sender);
        service.registerClient(target);

        service.handleMessage(sender, new Message(Message.Type.NAME, "Alice"));
        service.handleMessage(target, new Message(Message.Type.NAME, "Bob"));
        sender.getSentMessages().clear();

        service.handleMessage(sender, new Message(Message.Type.WHISPERCHAT, "Bob|line1\nline2"));

        assertEquals("ERROR|Whisper message contains invalid characters.", sender.getLastMessage());
    }

    /**
     * Verifies that an empty broadcast is rejected.
     */
    @Test
    void handleBroadcastRejectsEmptyMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.BROADCAST, "   "));

        assertEquals("ERROR|Broadcast message must not be empty.", session.getLastMessage());
    }

    /**
     * Verifies that a multiline broadcast is rejected.
     */
    @Test
    void handleBroadcastRejectsMultilineMessage() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);

        service.handleMessage(session, new Message(Message.Type.BROADCAST, "hello\nworld"));

        assertEquals("ERROR|Broadcast message must be a single line.", session.getLastMessage());
    }

    /**
     * Verifies that broadcast falls back to "Unknown" when the sender name is blank.
     */
    @Test
    void handleBroadcastUsesUnknownForBlankSenderName() {
        ServerService service = new ServerService();
        TestClientSession sender = new TestClientSession();
        TestClientSession receiver = new TestClientSession();

        service.registerClient(sender);
        service.registerClient(receiver);

        sender.setPlayerName(" ");
        sender.getSentMessages().clear();
        receiver.getSentMessages().clear();

        service.handleMessage(sender, new Message(Message.Type.BROADCAST, "announcement"));

        assertTrue(sender.containsSentMessage("BROADCAST|Unknown|announcement"));
        assertTrue(receiver.containsSentMessage("BROADCAST|Unknown|announcement"));
    }

    /**
     * Verifies that LOBBIES rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForLobbies() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.LOBBIES, "oops"));

        assertEquals("ERROR|LOBBIES must not contain content.", session.getLastMessage());
    }

    /**
     * Verifies that PLAYERS rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForPlayers() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.PLAYERS, "oops"));

        assertEquals("ERROR|PLAYERS must not contain content.", session.getLastMessage());
    }

    /**
     * Verifies that ALLPLAYERS rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForAllPlayers() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.ALLPLAYERS, "oops"));

        assertEquals("ERROR|ALLPLAYERS must not contain content.", session.getLastMessage());
    }

    /**
     * Verifies that START rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForStart() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.START, "oops"));

        assertEquals("ERROR|Malformed protocol message.", session.getLastMessage());
    }

    /**
     * Verifies that QUIT rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForQuit() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.QUIT, "oops"));

        assertEquals("ERROR|Malformed protocol message.", session.getLastMessage());
    }


    /**
     * Verifies that QUIT without content succeeds.
     */
    @Test
    void handleQuitSendsGoodbye() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.QUIT, ""));

        assertEquals("INFO|Goodbye.", session.getLastMessage());
    }

    /**
     * Verifies that LEAVE rejects unexpected content.
     */
    @Test
    void handleMessageRejectsContentForLeave() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.LEAVE, "oops"));

        assertEquals("ERROR|Malformed protocol message.", session.getLastMessage());
    }

    /**
     * Verifies that START outside a lobby is rejected.
     */
    @Test
    void handleStartRejectsClientWithoutLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.START, ""));

        assertEquals("ERROR|You are not in a lobby.", session.getLastMessage());
    }

    /**
     * Verifies that START with too few players is rejected.
     */
    @Test
    void handleStartRejectsLobbyWithTooFewPlayers() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.CREATE, "Lobby1"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.START, ""));

        assertEquals("ERROR|Need at least 2 players in the lobby to start.", session.getLastMessage());
    }

    /**
     * Verifies that GET_HAND reports no active game when none exists.
     */
    @Test
    void handleGetHandRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_HAND, ""));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that GET_GAME_STATE reports no active game when none exists.
     */
    @Test
    void handleGetGameStateRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_GAME_STATE, ""));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that DRAW_CARD reports no active game when none exists.
     */
    @Test
    void handleDrawCardRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.DRAW_CARD, ""));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that PLAY_CARD reports no active game when none exists.
     */
    @Test
    void handlePlayCardRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.PLAY_CARD, "42"));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that END_TURN reports no active game when none exists.
     */
    @Test
    void handleEndTurnRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.END_TURN, ""));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that EFFECT_RESPONSE reports no active game when none exists.
     */
    @Test
    void handleEffectResponseRejectsWithoutActiveGame() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.EFFECT_RESPONSE, "ANYTHING"));

        assertEquals("ERROR|No active game.", session.getLastMessage());
    }

    /**
     * Verifies that GET_ROUND_END is rejected outside a lobby.
     */
    @Test
    void handleGetRoundEndRejectsOutsideLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_ROUND_END, ""));

        assertEquals("ERROR|Not in a lobby.", session.getLastMessage());
    }

    /**
     * Verifies that GET_GAME_END is rejected outside a lobby.
     */
    @Test
    void handleGetGameEndRejectsOutsideLobby() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_GAME_END, ""));

        assertEquals("ERROR|Not in a lobby.", session.getLastMessage());
    }

    /**
     * Verifies that GET_ROUND_END reports missing scores when in a lobby
     * without an active or finished game.
     */
    @Test
    void handleGetRoundEndRejectsWhenNoScoresAvailable() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.CREATE, "Lobby1"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_ROUND_END, ""));

        assertEquals("ERROR|No round scores available.", session.getLastMessage());
    }

    /**
     * Verifies that GET_GAME_END reports missing result when no scores exist.
     */
    @Test
    void handleGetGameEndRejectsWhenNoResultAvailable() {
        ServerService service = new ServerService();
        TestClientSession session = new TestClientSession();

        service.registerClient(session);
        service.handleMessage(session, new Message(Message.Type.CREATE, "Lobby1"));
        session.getSentMessages().clear();

        service.handleMessage(session, new Message(Message.Type.GET_GAME_END, ""));

        assertEquals("ERROR|No game result available.", session.getLastMessage());
    }

    /**
     * Verifies that unregistering a client without a lobby does not fail and
     * updates the remaining global player list.
     */
    @Test
    void unregisterClientWithoutLobbyUpdatesGlobalList() {
        ServerService service = new ServerService();
        TestClientSession first = new TestClientSession();
        TestClientSession second = new TestClientSession();

        service.registerClient(first);
        service.registerClient(second);

        first.getSentMessages().clear();
        second.getSentMessages().clear();

        service.unregisterClient(second);

        assertTrue(first.containsSentMessage("ALLPLAYERS|Anonymous"));
    }
}