package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OtherPlayerViewTest {

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

    private OtherPlayerView createOnFxThread(String name, int handSize, String color) throws Exception {
        AtomicReference<OtherPlayerView> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new OtherPlayerView(name, handSize, color));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "OtherPlayerView constructor threw: " + err.get());
        return ref.get();
    }

    @Test
    void construct_doesNotThrow_red() throws Exception {
        assertNotNull(createOnFxThread("Alice", 5, "red"));
    }

    @Test
    void construct_doesNotThrow_green() throws Exception {
        assertNotNull(createOnFxThread("Bob", 3, "green"));
    }

    @Test
    void construct_doesNotThrow_blue() throws Exception {
        assertNotNull(createOnFxThread("Charlie", 0, "blue"));
    }

    @Test
    void construct_fanCardCount_matchesHandSize() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 4, "red");
        assertEquals(4, view.getFanCardCount());
    }

    @Test
    void construct_fanCardCount_zeroHandSize() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 0, "red");
        assertEquals(0, view.getFanCardCount());
    }

    @Test
    void construct_handSizeGetter_returnsConstructedValue() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 6, "teal");
        assertEquals(6, view.getHandSize());
    }

    @Test
    void setHandSize_updatesCardCount() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 3, "red");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.setHandSize(7);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(7, view.getFanCardCount());
    }

    @Test
    void setHandSize_updatesHandSizeGetter() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 3, "red");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.setHandSize(5);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(5, view.getHandSize());
    }

    @Test
    void setFanRotation_appliesRotation() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 3, "red");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.setFanRotation(180);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(180.0, view.getFanRotation(), 0.001);
    }

    @Test
    void setFanRotation_doesNotAffectHandSize() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 4, "blue");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.setFanRotation(220);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(4, view.getFanCardCount());
    }

    @Test
    void hideCards_restoresFanCount() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 3, "red");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.setHandSize(0);
            view.hideCards(); // restores to current handSize (0), not original 3
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, view.getFanCardCount());
    }

    @Test
    void revealCards_noopWithoutRegistry() throws Exception {
        OtherPlayerView view = createOnFxThread("Alice", 3, "red");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view.revealCards(List.of(1, 2, 5));
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // registry is null, so reveal is a no-op — fan still holds 3 backsides
        assertEquals(3, view.getFanCardCount());
    }
}
