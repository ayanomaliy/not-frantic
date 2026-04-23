package ch.unibas.dmi.dbis.cs108.example.client.assets;

import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardType;
import ch.unibas.dmi.dbis.cs108.example.model.game.SpecialEffect;
import com.fluxvend.svgfx.SvgImageView;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AssetRegistry}.
 *
 * <p>Lookup-oriented tests exercise the registry without JavaFX involvement. Tests that create
 * {@link SvgImageView} instances start the JavaFX toolkit once so icon creation can run safely
 * on the JavaFX Application Thread.</p>
 */
class AssetRegistryTest {

    // Shared registry loaded from the real classpath config, reused across tests.
    private static AssetRegistry registry;

    @BeforeAll
    static void loadRegistry() {
        registry = AssetRegistry.load();
        assertNotNull(registry, "AssetRegistry.load() must not return null");
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started.
        }
    }

    // -------------------------------------------------------------------------
    // getIconPath — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getIconPath_skipEffect_returnsSkipSvgPath() {
        Card skip = Card.specialSingleCard(0, CardColor.RED, SpecialEffect.SKIP);
        Optional<String> path = registry.getIconPath(skip);
        assertTrue(path.isPresent());
        assertEquals("icons/card_skip.svg", path.get());
    }

    @Test
    void getIconPath_giftEffect_returnsGiftSvgPath() {
        Card gift = Card.specialSingleCard(0, CardColor.GREEN, SpecialEffect.GIFT);
        assertEquals(Optional.of("icons/card_gift.svg"), registry.getIconPath(gift));
    }

    @Test
    void getIconPath_exchangeEffect_returnsExchangeSvgPath() {
        Card exchange = Card.specialSingleCard(0, CardColor.BLUE, SpecialEffect.EXCHANGE);
        assertEquals(Optional.of("icons/card_exchange.svg"), registry.getIconPath(exchange));
    }

    @Test
    void getIconPath_fantasticEffect_returnsSvgPath() {
        Card fantastic = Card.specialFourCard(0, SpecialEffect.FANTASTIC);
        assertEquals(Optional.of("icons/card_fantastic.svg"), registry.getIconPath(fantastic));
    }


    @Test
    void getIconPath_colorCardValueFive_returnsByNumberIcon() {
        Card red5 = Card.colorCard(0, CardColor.RED, 5);
        Optional<String> path = registry.getIconPath(red5);
        assertTrue(path.isPresent());
        assertEquals("icons/card_5.svg", path.get());
    }

    @Test
    void getIconPath_byNumber_allDigitsOneToNine_present() {
        for (int v = 1; v <= 9; v++) {
            Card card = Card.colorCard(0, CardColor.BLUE, v);
            Optional<String> path = registry.getIconPath(card);
            assertTrue(path.isPresent(), "Expected icon path for value " + v);
            assertEquals("icons/card_" + v + ".svg", path.get());
        }
    }

    @Test
    void getIconPath_effectTakesPriorityOverNumber() {
        // specialSingleCard has value=0, but SKIP has an effect → by_effect wins
        Card skip = Card.specialSingleCard(0, CardColor.RED, SpecialEffect.SKIP);
        assertEquals(Optional.of("icons/card_skip.svg"), registry.getIconPath(skip));
    }

    @Test
    void getIconPath_fuckYouCard_returnsEmpty_typeEntryHasNullIcon() {
        Card fy = Card.fuckYouCard(0);
        // by_type["FUCK_YOU"] has icon=null → empty
        assertTrue(registry.getIconPath(fy).isEmpty());
    }

    @Test
    void getIconPath_eventCard_returnsEmpty() {
        Card event = Card.eventCard(0);
        assertTrue(registry.getIconPath(event).isEmpty());
    }

    // -------------------------------------------------------------------------
    // getSoundId(Card) — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getSoundId_colorCard_returnsGenericSound() {
        Card red3 = Card.colorCard(0, CardColor.RED, 3);
        assertEquals(Optional.of("card_play_generic"), registry.getSoundId(red3));
    }

    @Test
    void getSoundId_blackCard_returnsGenericSound() {
        Card black = Card.blackCard(0, 7);
        assertEquals(Optional.of("card_play_generic"), registry.getSoundId(black));
    }

    // -------------------------------------------------------------------------
    // getSoundId(String eventName) — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getSoundId_event_gameStarted_returnsGameStart() {
        assertEquals(Optional.of("game_start"), registry.getSoundId("GAME_STARTED"));
    }

    @Test
    void getSoundId_event_gameEnded_returnsGameEnd() {
        assertEquals(Optional.of("game_end"), registry.getSoundId("GAME_ENDED"));
    }

    @Test
    void getSoundId_event_cardDrawn_returnsCardDraw() {
        assertEquals(Optional.of("card_draw"), registry.getSoundId("CARD_DRAWN"));
    }

    @Test
    void getSoundId_event_unknown_returnsEmpty() {
        assertTrue(registry.getSoundId("NONEXISTENT_EVENT").isEmpty());
    }

    @Test
    void getSoundId_event_null_returnsEmpty() {
        assertTrue(registry.getSoundId((String) null).isEmpty());
    }

    // -------------------------------------------------------------------------
    // getSoundPath — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getSoundPath_cardPlayGeneric_returnsMp3Path() {
        Optional<String> path = registry.getSoundPath("card_play_generic");
        assertTrue(path.isPresent());
        assertTrue(path.get().endsWith(".mp3"), "Sound path should end in .mp3");
    }

    @Test
    void getSoundPath_gameStart_returnsMp3Path() {
        Optional<String> path = registry.getSoundPath("game_start");
        assertTrue(path.isPresent());
        assertEquals("sounds/game_start.mp3", path.get());
    }

    @Test
    void getSoundPath_unknownId_returnsEmpty() {
        assertTrue(registry.getSoundPath("no_such_sound").isEmpty());
    }

    @Test
    void getSoundPath_null_returnsEmpty() {
        assertTrue(registry.getSoundPath(null).isEmpty());
    }

    // -------------------------------------------------------------------------
    // getBackgroundColor / getTextColor — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getBackgroundColor_red_returnsRedHex() {
        assertEquals(Optional.of("#e05050"), registry.getBackgroundColor(CardColor.RED));
    }

    @Test
    void getBackgroundColor_blue_returnsBlueHex() {
        assertEquals(Optional.of("#4a90d9"), registry.getBackgroundColor(CardColor.BLUE));
    }

    @Test
    void getTextColor_yellow_returnsDarkHex() {
        assertEquals(Optional.of("#333333"), registry.getTextColor(CardColor.YELLOW));
    }

    @Test
    void getTextColor_red_returnsWhite() {
        assertEquals(Optional.of("#ffffff"), registry.getTextColor(CardColor.RED));
    }

    @Test
    void getBackgroundColor_allColorsPresent() {
        for (CardColor color : CardColor.values()) {
            assertTrue(registry.getBackgroundColor(color).isPresent(),
                    "Missing background for " + color);
        }
    }

    @Test
    void getBackgroundColor_nullColor_returnsFallback() {
        Optional<String> bg = registry.getBackgroundColor(null);
        assertTrue(bg.isPresent(), "Null color should fall back to fallback background");
    }

    @Test
    void getTextColor_nullColor_returnsFallbackText() {
        Optional<String> text = registry.getTextColor(null);
        assertTrue(text.isPresent(), "Null color should fall back to fallback text color");
    }

    // -------------------------------------------------------------------------
    // getFallback — no JavaFX required
    // -------------------------------------------------------------------------

    @Test
    void getFallback_isNotNull() {
        assertNotNull(registry.getFallback());
    }

    @Test
    void getFallback_hasIcon() {
        assertNotNull(registry.getFallback().icon());
    }

    @Test
    void getFallback_hasBackground() {
        assertNotNull(registry.getFallback().background());
        assertTrue(registry.getFallback().background().startsWith("#"));
    }

    // -------------------------------------------------------------------------
    // getIconView / createIconView — requires JavaFX
    // -------------------------------------------------------------------------

    @Test
    void createIconView_validTestSvg_returnsNonEmpty() throws Exception {
        AtomicReference<Optional<SvgImageView>> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            result.set(registry.createIconView("test-icons/test_icon.svg", 48));
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(result.get().isPresent(), "Should load the test SVG from classpath");
    }

    @Test
    void createIconView_nullPath_returnsEmpty() throws Exception {
        AtomicReference<Optional<SvgImageView>> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            result.set(registry.createIconView(null, 48));
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(result.get().isEmpty());
    }

    @Test
    void createIconView_setsRequestedSize() throws Exception {
        AtomicReference<SvgImageView> view = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            registry.createIconView("test-icons/test_icon.svg", 64)
                    .ifPresent(view::set);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(view.get(), "View should have been created");
        assertEquals(64.0, view.get().getPrefWidth());
        assertEquals(64.0, view.get().getPrefHeight());
    }

    // -------------------------------------------------------------------------
    // Empty registry (no config) — graceful fallback
    // -------------------------------------------------------------------------

    @Test
    void emptyRegistry_getIconPath_returnsEmpty() {
        AssetRegistry empty = AssetRegistry.of(null);
        Card card = Card.colorCard(0, CardColor.RED, 5);
        assertTrue(empty.getIconPath(card).isEmpty());
    }

    @Test
    void emptyRegistry_getSoundId_returnsEmpty() {
        AssetRegistry empty = AssetRegistry.of(null);
        Card card = Card.specialSingleCard(0, CardColor.RED, SpecialEffect.SKIP);
        assertTrue(empty.getSoundId(card).isEmpty());
    }

    @Test
    void emptyRegistry_getBackgroundColor_returnsEmpty() {
        AssetRegistry empty = AssetRegistry.of(null);
        assertTrue(empty.getBackgroundColor(CardColor.RED).isEmpty());
    }
}
