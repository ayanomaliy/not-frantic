package ch.unibas.dmi.dbis.cs108.example.controller;

import ch.unibas.dmi.dbis.cs108.example.service.Message;
import ch.unibas.dmi.dbis.cs108.example.service.ServerService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClientSession}.
 */
class ClientSessionTest {

    /**
     * Fake socket backed by in-memory streams.
     */
    private static class FakeSocket extends Socket {

        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private boolean closed = false;

        /**
         * Creates a fake socket with the given raw input text.
         *
         * @param inputText text that will be returned by the socket input stream
         */
        FakeSocket(String inputText) {
            this.input = new ByteArrayInputStream(inputText.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ByteArrayInputStream getInputStream() {
            return input;
        }

        @Override
        public ByteArrayOutputStream getOutputStream() {
            return output;
        }

        @Override
        public synchronized void close() {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return new SocketAddress() {
                @Override
                public String toString() {
                    return "fake-remote";
                }
            };
        }

        /**
         * Returns all text written to the socket output.
         *
         * @return UTF-8 output text
         */
        String getWrittenText() {
            return output.toString(StandardCharsets.UTF_8);
        }

        /**
         * Returns all written output lines, trimmed of blank trailing lines.
         *
         * @return list of output lines
         */
        List<String> getWrittenLines() {
            String text = getWrittenText();
            if (text.isEmpty()) {
                return List.of();
            }

            String[] raw = text.split("\\R");
            List<String> lines = new ArrayList<>();
            for (String line : raw) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        }
    }

    /**
     * Fake server service that records interactions from the session.
     */
    private static class FakeServerService extends ServerService {

        private int registerCount = 0;
        private int unregisterCount = 0;

        private ClientSession lastRegisteredSession;
        private ClientSession lastUnregisteredSession;
        private ClientSession lastHandledSession;
        private Message lastHandledMessage;

        @Override
        public synchronized void registerClient(ClientSession session) {
            registerCount++;
            lastRegisteredSession = session;
        }

        @Override
        public synchronized void unregisterClient(ClientSession session) {
            unregisterCount++;
            lastUnregisteredSession = session;
        }

        @Override
        public synchronized void handleMessage(ClientSession session, Message message) {
            lastHandledSession = session;
            lastHandledMessage = message;
        }
    }

    /**
     * Fake server controller that records removed sessions.
     */
    private static class FakeServerController extends ServerController {

        private int removeCount = 0;
        private ClientSession lastRemovedSession;

        /**
         * Creates the fake controller.
         *
         * @param serverService backing fake server service
         */
        FakeServerController(ServerService serverService) {
            super(0, serverService);
        }

        @Override
        public void removeSession(ClientSession session) {
            removeCount++;
            lastRemovedSession = session;
        }
    }

    /**
     * Injects a PrintWriter into the private out field so send() can be tested
     * without running the full session loop.
     *
     * @param session the session under test
     * @param writer the writer to inject
     * @throws Exception if reflection fails
     */
    private static void setOut(ClientSession session, PrintWriter writer) throws Exception {
        Field outField = ClientSession.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(session, writer);
    }

    /**
     * Verifies send() does nothing when the output writer has not been initialized.
     */
    @Test
    void sendDoesNothingWhenOutIsNull() {
        FakeSocket socket = new FakeSocket("");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        assertDoesNotThrow(() -> session.send("INFO|hello"));
        assertTrue(socket.getWrittenLines().isEmpty());
    }

    /**
     * Verifies send() writes to the output stream when a writer is present.
     *
     * @throws Exception if reflection setup fails
     */
    @Test
    void sendWritesToOutputWhenOutExists() throws Exception {
        FakeSocket socket = new FakeSocket("");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(outBytes, StandardCharsets.UTF_8),
                true
        );
        setOut(session, writer);

        session.send("INFO|hello");

        String written = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(written.contains("INFO|hello"));
    }

    /**
     * Verifies player-name getter and setter.
     */
    @Test
    void getAndSetPlayerNameWork() {
        FakeSocket socket = new FakeSocket("");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        assertEquals("Anonymous", session.getPlayerName());

        session.setPlayerName("Alice");

        assertEquals("Alice", session.getPlayerName());
    }

    /**
     * Verifies heartbeat time can be updated.
     *
     * @throws Exception if timing is interrupted
     */
    @Test
    void updateHeartbeatTimeChangesTimestamp() throws Exception {
        FakeSocket socket = new FakeSocket("");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        long before = session.getLastHeartbeatTime();
        Thread.sleep(2L);
        session.updateHeartbeatTime();
        long after = session.getLastHeartbeatTime();

        assertTrue(after >= before);
    }

    /**
     * Verifies disconnect closes the socket and unregisters/removes exactly once.
     */
    @Test
    void disconnectClosesSocketAndNotifiesCollaboratorsOnlyOnce() {
        FakeSocket socket = new FakeSocket("");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        session.disconnect();
        session.disconnect();

        assertTrue(socket.isClosed());
        assertEquals(1, service.unregisterCount);
        assertSame(session, service.lastUnregisteredSession);
        assertEquals(1, controller.removeCount);
        assertSame(session, controller.lastRemovedSession);
    }

    /**
     * Verifies run() registers the client, sends WELCOME, forwards a normal
     * message to the server service, and disconnects afterwards.
     */
    @Test
    void runRegistersWelcomesForwardsNormalMessageAndDisconnects() {
        FakeSocket socket = new FakeSocket("NAME|Alice\n");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        session.run();

        List<String> lines = socket.getWrittenLines();

        assertEquals(1, service.registerCount);
        assertSame(session, service.lastRegisteredSession);

        assertTrue(lines.contains("INFO|WELCOME"));

        assertSame(session, service.lastHandledSession);
        assertNotNull(service.lastHandledMessage);
        assertEquals(Message.Type.NAME, service.lastHandledMessage.type());
        assertEquals("Alice", service.lastHandledMessage.content());

        assertEquals(1, service.unregisterCount);
        assertEquals(1, controller.removeCount);
        assertTrue(socket.isClosed());
    }

    /**
     * Verifies run() returns an error for invalid input that parses to null.
     */
    @Test
    void runSendsErrorForInvalidCommand() {
        FakeSocket socket = new FakeSocket("   \n");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        session.run();

        List<String> lines = socket.getWrittenLines();

        assertTrue(lines.contains("INFO|WELCOME"));
        assertTrue(lines.contains("ERROR|Invalid command."));
        assertNull(service.lastHandledMessage);
    }

    /**
     * Verifies run() answers PING with PONG and does not forward it to the server.
     */
    @Test
    void runHandlesPingDirectly() {
        FakeSocket socket = new FakeSocket("PING|\n");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        long before = session.getLastHeartbeatTime();
        session.run();
        long after = session.getLastHeartbeatTime();

        List<String> lines = socket.getWrittenLines();

        assertTrue(lines.contains("INFO|WELCOME"));
        assertTrue(lines.contains("PONG|"));
        assertNull(service.lastHandledMessage);
        assertTrue(after >= before);
    }

    /**
     * Verifies run() handles PONG locally and does not forward it to the server.
     */
    @Test
    void runHandlesPongDirectly() {
        FakeSocket socket = new FakeSocket("PONG|\n");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        long before = session.getLastHeartbeatTime();
        session.run();
        long after = session.getLastHeartbeatTime();

        List<String> lines = socket.getWrittenLines();

        assertTrue(lines.contains("INFO|WELCOME"));
        assertFalse(lines.contains("PONG|"));
        assertNull(service.lastHandledMessage);
        assertTrue(after >= before);
    }

    /**
     * Verifies run() forwards QUIT once and then stops reading further input.
     */
    @Test
    void runStopsAfterQuit() {
        FakeSocket socket = new FakeSocket("QUIT|\nNAME|ShouldNotBeHandled\n");
        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(socket, service, controller);

        session.run();

        assertNotNull(service.lastHandledMessage);
        assertEquals(Message.Type.QUIT, service.lastHandledMessage.type());
        assertEquals("", service.lastHandledMessage.content());
    }

    /**
     * Verifies run() survives stream initialization failures and still disconnects.
     */
    @Test
    void runStillDisconnectsWhenSocketThrows() {
        Socket brokenSocket = new Socket() {
            private boolean closed = false;

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public java.io.OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public synchronized void close() {
                closed = true;
            }

            @Override
            public boolean isClosed() {
                return closed;
            }

            @Override
            public SocketAddress getRemoteSocketAddress() {
                return new SocketAddress() {
                    @Override
                    public String toString() {
                        return "broken";
                    }
                };
            }
        };

        FakeServerService service = new FakeServerService();
        FakeServerController controller = new FakeServerController(service);

        ClientSession session = new ClientSession(brokenSocket, service, controller);

        assertDoesNotThrow(session::run);

        assertEquals(1, service.unregisterCount);
        assertEquals(1, controller.removeCount);
    }
}