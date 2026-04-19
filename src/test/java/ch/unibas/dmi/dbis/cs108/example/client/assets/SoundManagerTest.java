package ch.unibas.dmi.dbis.cs108.example.client.assets;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SoundManager}.
 *
 * <p>Mute/logic tests run without JavaFX. Playback tests run on the JavaFX Application Thread
 * but verify only that no exception escapes — actual audio output is not asserted because the
 * test environment may be headless.</p>
 */
class SoundManagerTest {

    private static AssetRegistry registry;
    private SoundManager manager;

    @BeforeAll
    static void loadRegistry() {
        registry = AssetRegistry.load();
        assertNotNull(registry);
    }

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

    @BeforeEach
    void createManager() {
        manager = new SoundManager(registry);
    }

    // -------------------------------------------------------------------------
    // Mute toggle
    // -------------------------------------------------------------------------

    @Test
    void isMuted_defaultsFalse() {
        assertFalse(manager.isMuted());
    }

    @Test
    void setMuted_true_returnsTrueFromIsMuted() {
        manager.setMuted(true);
        assertTrue(manager.isMuted());
    }

    @Test
    void setMuted_falseAfterTrue_returnsFalse() {
        manager.setMuted(true);
        manager.setMuted(false);
        assertFalse(manager.isMuted());
    }

    // -------------------------------------------------------------------------
    // play — null / unknown IDs must never throw
    // -------------------------------------------------------------------------

    @Test
    void play_nullId_doesNotThrow() {
        assertDoesNotThrow(() -> manager.play(null));
    }

    @Test
    void play_unknownId_doesNotThrow() {
        assertDoesNotThrow(() -> manager.play("no_such_sound_xyz"));
    }

    @Test
    void play_whenMuted_doesNotThrow() {
        manager.setMuted(true);
        assertDoesNotThrow(() -> manager.play("card_play_generic"));
    }

    // -------------------------------------------------------------------------
    // play — known IDs on the JavaFX thread must not throw
    // -------------------------------------------------------------------------

    @Test
    void play_knownId_doesNotThrow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicThrowable thrown = new AtomicThrowable();
        Platform.runLater(() -> {
            try {
                manager.play("card_play_generic");
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(thrown.get(), "play() must not throw: " + thrown.get());
    }

    @Test
    void play_cardDrawSound_doesNotThrow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicThrowable thrown = new AtomicThrowable();
        Platform.runLater(() -> {
            try {
                manager.play("card_draw");
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(thrown.get(), "play() must not throw: " + thrown.get());
    }

    @Test
    void play_gameStartSound_doesNotThrow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicThrowable thrown = new AtomicThrowable();
        Platform.runLater(() -> {
            try {
                manager.play("game_start");
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(thrown.get(), "play() must not throw: " + thrown.get());
    }

    @Test
    void play_muteToggle_doesNotThrowAfterRepeatedCalls() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicThrowable thrown = new AtomicThrowable();
        Platform.runLater(() -> {
            try {
                manager.play("card_play_generic");
                manager.setMuted(true);
                manager.play("card_play_generic");
                manager.setMuted(false);
                manager.play("card_play_generic");
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(thrown.get(), "play() must not throw: " + thrown.get());
    }

    // -------------------------------------------------------------------------
    // Empty registry — must never throw
    // -------------------------------------------------------------------------

    @Test
    void play_emptyRegistry_doesNotThrow() {
        AssetRegistry empty = AssetRegistry.of(null);
        SoundManager emptyManager = new SoundManager(empty);
        assertDoesNotThrow(() -> emptyManager.play("card_play_generic"));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static final class AtomicThrowable {
        private volatile Throwable value;
        void set(Throwable t) { value = t; }
        Throwable get() { return value; }
    }
}
