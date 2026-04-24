package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CardBacksideViewTest {

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

    private CardBacksideView createOnFxThread() throws Exception {
        AtomicReference<CardBacksideView> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new CardBacksideView());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "CardBacksideView constructor threw: " + err.get());
        return ref.get();
    }

    @Test
    void construct_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread());
    }

    @Test
    void construct_prefSizeIs90x130() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertEquals(90, view.getPrefWidth());
        assertEquals(130, view.getPrefHeight());
    }

    @Test
    void construct_minSizeIs90x130() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertEquals(90, view.getMinWidth());
        assertEquals(130, view.getMinHeight());
    }

    @Test
    void construct_maxSizeIs90x130() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertEquals(90, view.getMaxWidth());
        assertEquals(130, view.getMaxHeight());
    }

    @Test
    void construct_hasCardViewStyleClass() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertTrue(view.getStyleClass().contains("card-view"));
    }

    @Test
    void construct_hasCardBacksideStyleClass() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertTrue(view.getStyleClass().contains("card-backside"));
    }

    @Test
    void construct_hasChildNode() throws Exception {
        CardBacksideView view = createOnFxThread();
        assertFalse(view.getChildren().isEmpty(), "Expected SVG child node when asset is present");
    }
}
