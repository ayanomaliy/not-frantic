package ch.unibas.dmi.dbis.cs108.example.controller;

import ch.unibas.dmi.dbis.cs108.example.service.ServerService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ServerController}.
 *
 * <p>These tests focus on behavior that makes sense to verify in isolation:
 * session removal and heartbeat-monitor handling. The {@code start()} method
 * is intentionally not unit-tested here because it opens a real server socket,
 * enters an infinite accept loop, and is therefore better suited for
 * integration testing.</p>
 */
class ServerControllerTest {

    /**
     * Simple fake client session for heartbeat-monitor tests.
     */
    private static class FakeClientSession extends ClientSession {

        private long lastHeartbeatTime;
        private int sendCount = 0;
        private int disconnectCount = 0;
        private String lastSentText;
        private final String playerName;

        /**
         * Creates a fake client session.
         *
         * @param playerName the player name to report
         * @param lastHeartbeatTime the timestamp returned by {@link #getLastHeartbeatTime()}
         */
        FakeClientSession(String playerName, long lastHeartbeatTime) {
            super(null, null, null);
            this.playerName = playerName;
            this.lastHeartbeatTime = lastHeartbeatTime;
        }

        @Override
        public void send(String text) {
            sendCount++;
            lastSentText = text;
        }

        @Override
        public long getLastHeartbeatTime() {
            return lastHeartbeatTime;
        }

        @Override
        public void disconnect() {
            disconnectCount++;
        }

        @Override
        public String getPlayerName() {
            return playerName;
        }

        /**
         * Returns how many times {@code send(...)} was called.
         *
         * @return the send count
         */
        int getSendCount() {
            return sendCount;
        }

        /**
         * Returns how many times {@code disconnect()} was called.
         *
         * @return the disconnect count
         */
        int getDisconnectCount() {
            return disconnectCount;
        }

        /**
         * Returns the last sent raw protocol text.
         *
         * @return the last sent text
         */
        String getLastSentText() {
            return lastSentText;
        }
    }

    /**
     * Returns the private session list from the controller.
     *
     * @param controller the controller under test
     * @return the mutable session list used internally
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static List<ClientSession> getSessions(ServerController controller) throws Exception {
        Field field = ServerController.class.getDeclaredField("sessions");
        field.setAccessible(true);
        return (List<ClientSession>) field.get(controller);
    }

    /**
     * Invokes the private heartbeat-monitor starter.
     *
     * @param controller the controller under test
     * @throws Exception if reflection fails
     */
    private static void startHeartbeatMonitor(ServerController controller) throws Exception {
        Method method = ServerController.class.getDeclaredMethod("startHeartbeatMonitor");
        method.setAccessible(true);
        method.invoke(controller);
    }

    /**
     * Verifies that {@link ServerController#removeSession(ClientSession)}
     * removes the given session from the active session list.
     *
     * @throws Exception if reflection fails
     */
    @Test
    void removeSessionRemovesSessionFromList() throws Exception {
        ServerController controller = new ServerController(5555, new ServerService());

        FakeClientSession first = new FakeClientSession("Alice", System.currentTimeMillis());
        FakeClientSession second = new FakeClientSession("Bob", System.currentTimeMillis());

        List<ClientSession> sessions = getSessions(controller);
        sessions.add(first);
        sessions.add(second);

        assertEquals(2, sessions.size());

        controller.removeSession(first);

        assertEquals(1, sessions.size());
        assertFalse(sessions.contains(first));
        assertTrue(sessions.contains(second));
    }

    /**
     * Verifies that the heartbeat monitor sends a ping to active sessions
     * and does not disconnect a healthy client.
     *
     * <p>This test waits slightly longer than one heartbeat interval so the
     * background daemon thread has time to run once.</p>
     *
     * @throws Exception if reflection or waiting fails
     */
    @Test
    void heartbeatMonitorSendsPingToHealthySession() throws Exception {
        ServerController controller = new ServerController(5555, new ServerService());

        FakeClientSession healthy = new FakeClientSession(
                "Alice",
                System.currentTimeMillis()
        );

        getSessions(controller).add(healthy);

        startHeartbeatMonitor(controller);

        Thread.sleep(5500);

        assertTrue(healthy.getSendCount() >= 1);
        assertEquals("PING|", healthy.getLastSentText());
        assertEquals(0, healthy.getDisconnectCount());
    }

    /**
     * Verifies that the heartbeat monitor disconnects a timed-out client.
     *
     * <p>The fake session reports an old heartbeat timestamp so that the
     * monitor considers it timed out on its first check.</p>
     *
     * @throws Exception if reflection or waiting fails
     */
    @Test
    void heartbeatMonitorDisconnectsTimedOutSession() throws Exception {
        ServerController controller = new ServerController(5555, new ServerService());

        FakeClientSession timedOut = new FakeClientSession(
                "Bob",
                System.currentTimeMillis() - 20000
        );

        getSessions(controller).add(timedOut);

        startHeartbeatMonitor(controller);

        Thread.sleep(5500);

        assertTrue(timedOut.getSendCount() >= 1);
        assertEquals("PING|", timedOut.getLastSentText());
        assertTrue(timedOut.getDisconnectCount() >= 1);
    }
}