package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CircularTablePaneTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // already started
        }
    }

    private CircularTablePane createOnFxThread() throws Exception {
        AtomicReference<CircularTablePane> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new CircularTablePane(new Label("center")));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "CircularTablePane constructor threw: " + err.get());
        return ref.get();
    }

    private static ClientState.PlayerInfo info(String name, int hand, String color) {
        return new ClientState.PlayerInfo(name, hand, color);
    }

    @Test
    void construct_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread());
    }

    @Test
    void setPlayerSlots_empty_hasNoSlots() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(pane.getPlayerSlots().isEmpty());
    }

    @Test
    void setPlayerSlots_onePlayer_hasSingleSlot() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(info("Bob", 3, "green")), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, pane.getPlayerSlots().size());
    }

    @Test
    void setPlayerSlots_twoPlayers_hasTwoSlots() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(
                    info("Bob", 3, "green"),
                    info("Charlie", 5, "blue")
            ), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, pane.getPlayerSlots().size());
        assertTrue(pane.getPlayerSlots().containsKey("Bob"));
        assertTrue(pane.getPlayerSlots().containsKey("Charlie"));
    }

    @Test
    void setPlayerSlots_twoPlayers_angles_are_320_and_40() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(
                    info("Bob", 3, "green"),
                    info("Charlie", 5, "blue")
            ), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Double> angles = pane.getSlotAngles();
        assertEquals(2, angles.size());
        assertEquals(320.0, angles.get(0), 0.001);
        assertEquals(40.0, angles.get(1), 0.001);
    }

    @Test
    void setPlayerSlots_onePlayer_angle_is_0() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(info("Bob", 3, "green")), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Double> angles = pane.getSlotAngles();
        assertEquals(1, angles.size());
        assertEquals(0.0, angles.get(0), 0.001);
    }

    @Test
    void setPlayerSlots_threePlayersClearPrevious() throws Exception {
        CircularTablePane pane = createOnFxThread();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pane.setPlayerSlots(List.of(info("Bob", 2, "green")), "Alice");
            pane.setPlayerSlots(List.of(
                    info("Bob", 2, "green"),
                    info("Charlie", 4, "blue"),
                    info("Dave", 1, "yellow")
            ), "Alice");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, pane.getPlayerSlots().size());
    }

    @Test
    void setPlayerSlots_fivePlayers_noThrow() throws Exception {
        CircularTablePane pane = createOnFxThread();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                pane.setPlayerSlots(List.of(
                        info("P1", 5, "green"),
                        info("P2", 3, "blue"),
                        info("P3", 7, "yellow"),
                        info("P4", 2, "purple"),
                        info("P5", 4, "teal")
                ), "Alice");
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get());
        assertEquals(5, pane.getPlayerSlots().size());
    }

    @Test
    void gameView_getCircularTablePane_returnsNonNull() throws Exception {
        AtomicReference<CircularTablePane> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            GameView view = new GameView();
            ref.set(view.getCircularTablePane());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(ref.get());
    }
}
