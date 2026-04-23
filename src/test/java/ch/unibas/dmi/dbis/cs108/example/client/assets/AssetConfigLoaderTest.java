package ch.unibas.dmi.dbis.cs108.example.client.assets;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AssetConfigLoader} and {@link AssetConfigLoader.JsonParser}.
 *
 * <p>Integration tests verify loading against the real {@code asset-config.json} resource.
 * Parser-focused tests use inline JSON strings so they can validate parsing behavior without
 * depending on external files.</p>
 */
class AssetConfigLoaderTest {

    // -------------------------------------------------------------------------
    // Integration: load from real classpath resource
    // -------------------------------------------------------------------------

    @Test
    void load_returnsNonNull() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg, "AssetConfigLoader.load() must not return null when the file exists");
    }

    @Test
    void load_byEffect_containsAllExpectedEffects() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, AssetConfig.CardAssetEntry> byEffect = cfg.byEffect();
        assertTrue(byEffect.containsKey("SKIP"));
        assertTrue(byEffect.containsKey("GIFT"));
        assertTrue(byEffect.containsKey("EXCHANGE"));
        assertTrue(byEffect.containsKey("FANTASTIC"));
        assertTrue(byEffect.containsKey("FANTASTIC_FOUR"));
        assertTrue(byEffect.containsKey("SECOND_CHANCE"));
        assertTrue(byEffect.containsKey("EQUALITY"));
        assertTrue(byEffect.containsKey("COUNTERATTACK"));
        assertTrue(byEffect.containsKey("NICE_TRY"));
    }

    @Test
    void load_byType_containsAllCardTypes() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, AssetConfig.CardAssetEntry> byType = cfg.byType();
        assertTrue(byType.containsKey("COLOR"));
        assertTrue(byType.containsKey("BLACK"));
        assertTrue(byType.containsKey("FUCK_YOU"));
        assertTrue(byType.containsKey("EVENT"));
    }

    @Test
    void load_byColor_red_hasCorrectColors() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        AssetConfig.CardColorEntry red = cfg.byColor().get("RED");
        assertNotNull(red);
        assertEquals("#e05050", red.background());
        assertEquals("#ffffff", red.text());
    }

    @Test
    void load_byColor_yellow_hasDarkText() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        AssetConfig.CardColorEntry yellow = cfg.byColor().get("YELLOW");
        assertNotNull(yellow);
        assertEquals("#333333", yellow.text(), "Yellow cards need dark text for readability");
    }

    @Test
    void load_byColor_containsAllFiveColors() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, AssetConfig.CardColorEntry> byColor = cfg.byColor();
        for (String color : new String[]{"RED", "GREEN", "BLUE", "YELLOW", "BLACK"}) {
            assertTrue(byColor.containsKey(color), "Missing color entry: " + color);
        }
    }

    @Test
    void load_byNumber_containsDigitsOneToNine() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, AssetConfig.CardAssetEntry> byNumber = cfg.byNumber();
        for (int i = 1; i <= 9; i++) {
            assertTrue(byNumber.containsKey(String.valueOf(i)), "Missing by_number entry: " + i);
        }
    }

    @Test
    void load_byNumber_iconPathMatchesNumber() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        AssetConfig.CardAssetEntry five = cfg.byNumber().get("5");
        assertNotNull(five);
        assertEquals("icons/card_5.svg", five.icon());
    }

    @Test
    void load_sounds_containsExpectedIds() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, String> sounds = cfg.sounds();
        assertTrue(sounds.containsKey("card_play_generic"));
        assertTrue(sounds.containsKey("card_draw"));
        assertTrue(sounds.containsKey("game_start"));
        assertTrue(sounds.containsKey("game_end"));
    }

    @Test
    void load_sounds_pathEndsWithMp3() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        for (Map.Entry<String, String> e : cfg.sounds().entrySet()) {
            assertTrue(e.getValue().endsWith(".mp3"),
                    "Sound path should end in .mp3: " + e.getValue());
        }
    }

    @Test
    void load_eventSounds_containsGameEvents() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        Map<String, String> events = cfg.eventSounds();
        assertTrue(events.containsKey("GAME_STARTED"));
        assertTrue(events.containsKey("GAME_ENDED"));
        assertTrue(events.containsKey("CARD_DRAWN"));
    }

    @Test
    void load_eventSounds_gameStartedPointsToGameStart() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        assertEquals("game_start", cfg.eventSounds().get("GAME_STARTED"));
    }

    @Test
    void load_fallback_isNotNull() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        assertNotNull(cfg.fallback(), "Fallback entry should be present in config");
    }

    @Test
    void load_fallback_hasIconAndBackground() {
        AssetConfig cfg = AssetConfigLoader.load();
        assertNotNull(cfg);
        AssetConfig.FallbackEntry fb = cfg.fallback();
        assertNotNull(fb.icon());
        assertNotNull(fb.background());
        assertNotNull(fb.text());
    }

    @Test
    void load_missingResource_returnsNull() {
        AssetConfig cfg = AssetConfigLoader.load("/config/does-not-exist.json");
        assertNull(cfg, "Missing resource should return null, not throw");
    }

    // -------------------------------------------------------------------------
    // Unit: JsonParser directly (inline JSON, no file I/O)
    // -------------------------------------------------------------------------

    @Test
    void parser_emptyObject_returnsEmptyMap() {
        Map<String, Object> result = new AssetConfigLoader.JsonParser("{}").parseObject();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parser_simpleStringValue() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("{\"key\": \"value\"}").parseObject();
        assertEquals("value", result.get("key"));
    }

    @Test
    void parser_nullValue() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("{\"k\": null}").parseObject();
        assertNull(result.get("k"));
        assertTrue(result.containsKey("k"), "Key with null value must still be present");
    }

    @Test
    void parser_integerValue() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("{\"v\": 42}").parseObject();
        assertEquals(42, result.get("v"));
    }

    @Test
    void parser_nestedObject() {
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>)
                new AssetConfigLoader.JsonParser("{\"outer\": {\"inner\": \"hello\"}}")
                        .parseObject()
                        .get("outer");
        assertEquals("hello", inner.get("inner"));
    }

    @Test
    void parser_multipleEntries() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("{\"a\": \"1\", \"b\": \"2\", \"c\": null}")
                        .parseObject();
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertNull(result.get("c"));
        assertEquals(3, result.size());
    }

    @Test
    void parser_stringWithEscapes() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("{\"k\": \"line1\\nline2\"}")
                        .parseObject();
        assertEquals("line1\nline2", result.get("k"));
    }

    @Test
    void parser_whitespaceAroundTokens_isIgnored() {
        Map<String, Object> result =
                new AssetConfigLoader.JsonParser("  {  \"key\"  :  \"val\"  }  ")
                        .parseObject();
        assertEquals("val", result.get("key"));
    }

    @Test
    void parser_invalidChar_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                new AssetConfigLoader.JsonParser("{\"k\": @invalid}").parseObject());
    }

    @Test
    void parser_unterminatedString_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                new AssetConfigLoader.JsonParser("{\"k\": \"unterminated}").parseObject());
    }
}
