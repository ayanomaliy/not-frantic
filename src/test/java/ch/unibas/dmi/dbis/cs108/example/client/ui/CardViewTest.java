package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CardView}.
 *
 * <p>These tests create card views on the JavaFX Application Thread because the control may
 * construct {@link com.fluxvend.svgfx.SvgImageView} nodes during initialization.</p>
 */
class CardViewTest {

    private static AssetRegistry registry;

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

    @BeforeAll
    static void loadRegistry() {
        registry = AssetRegistry.load();
        assertNotNull(registry);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CardView createOnFxThread(int cardId) throws Exception {
        AtomicReference<CardView> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ref.set(new CardView(cardId, registry, null));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "CardView constructor threw: " + err.get());
        return ref.get();
    }

    private static boolean containsLabel(Node node) {
        if (node instanceof Label) {
            return true;
        }

        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable()
                    .stream()
                    .anyMatch(CardViewTest::containsLabel);
        }

        return false;
    }

    private static Label findFirstLabel(Node node) {
        if (node instanceof Label label) {
            return label;
        }

        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable()
                    .stream()
                    .map(CardViewTest::findFirstLabel)
                    .filter(label -> label != null)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Basic construction
    // -------------------------------------------------------------------------

    @Test
    void construct_redColorCard_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(0)); // Red 1
    }

    @Test
    void construct_blackCard_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(72)); // Black 1
    }

    @Test
    void construct_skipCard_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(82)); // Red Skip
    }

    @Test
    void construct_fantasticFourCard_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(106)); // Fantastic Four
    }

    @Test
    void construct_unknownCardId_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(9999)); // not in deck
    }

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------

    @Test
    void construct_prefSizeIs90x130() throws Exception {
        CardView view = createOnFxThread(0);

        assertEquals(90, view.getPrefWidth());
        assertEquals(130, view.getPrefHeight());
    }

    // -------------------------------------------------------------------------
    // CSS class
    // -------------------------------------------------------------------------

    @Test
    void construct_hasGameCardButtonStyleClass() throws Exception {
        CardView view = createOnFxThread(0);

        assertTrue(view.getStyleClass().contains("game-card-button"));
    }

    // -------------------------------------------------------------------------
    // Children
    // -------------------------------------------------------------------------

    @Test
    void construct_alwaysHasAtLeastOneChild() throws Exception {
        CardView view = createOnFxThread(0);

        assertFalse(view.getChildren().isEmpty());
    }

    @Test
    void construct_hasLabelSomewhereInsideCardView() throws Exception {
        CardView view = createOnFxThread(0);

        assertTrue(containsLabel(view), "Expected a Label somewhere inside CardView");
    }

    @Test
    void construct_unknownId_hasFallbackTextSomewhereInsideCardView() throws Exception {
        CardView view = createOnFxThread(9999);

        Label label = findFirstLabel(view);

        assertNotNull(label, "Expected a fallback Label somewhere inside CardView");
        assertFalse(label.getText().isBlank());
    }

    // -------------------------------------------------------------------------
    // onPlay callback
    // -------------------------------------------------------------------------

    @Test
    void construct_onPlayNull_doesNotThrow() throws Exception {
        assertNotNull(createOnFxThread(0));
    }

    @Test
    void construct_onPlayCallback_isWired() throws Exception {
        AtomicBoolean played = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CardView> ref = new AtomicReference<>();

        Platform.runLater(() -> {
            ref.set(new CardView(0, registry, () -> played.set(true)));
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        CountDownLatch clickLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            var handler = ref.get().getOnMouseClicked();
            assertNotNull(handler, "MouseClicked handler should be set when onPlay is non-null");
            clickLatch.countDown();
        });

        assertTrue(clickLatch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // Empty registry safety
    // -------------------------------------------------------------------------

    @Test
    void construct_emptyRegistry_doesNotThrow() throws Exception {
        AssetRegistry empty = AssetRegistry.of(null);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                new CardView(0, empty, null);
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "CardView must not throw with empty registry: " + err.get());
    }
}