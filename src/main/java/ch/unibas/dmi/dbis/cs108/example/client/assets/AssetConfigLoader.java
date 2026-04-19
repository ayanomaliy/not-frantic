package ch.unibas.dmi.dbis.cs108.example.client.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@link AssetConfig} instances from JSON resources on the classpath.
 *
 * <p>This loader uses a small built-in recursive-descent parser instead of an external JSON
 * dependency. The parser supports the subset needed by the asset configuration format:
 * objects, strings, integers, and {@code null} values.</p>
 *
 * <p>If a resource cannot be found or parsed, the loader returns {@code null} and writes a
 * warning to standard error so callers can fall back gracefully.</p>
 */
public final class AssetConfigLoader {

    private static final String CONFIG_PATH = "/config/asset-config.json";

    private AssetConfigLoader() {}

    /**
     * Reads and parses the asset config from the classpath.
     *
     * @return the parsed config, or {@code null} if the file is absent or invalid
     */
    public static AssetConfig load() {
        return load(CONFIG_PATH);
    }

    /**
     * Reads and parses an asset configuration from the given classpath resource.
     *
     * <p>This overload is primarily intended for tests that need to point at an alternate
     * resource location.</p>
     *
     * @param resourcePath absolute classpath path starting with {@code /}
     * @return the parsed config, or {@code null} if the resource is missing or malformed
     */
    public static AssetConfig load(String resourcePath) {
        try (InputStream in = AssetConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("[AssetConfigLoader] Resource not found: " + resourcePath);
                return null;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> root = new JsonParser(json).parseObject();
            return extract(root);
        } catch (IOException | ClassCastException | NullPointerException e) {
            System.err.println("[AssetConfigLoader] Failed to load config: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Extraction: typed AssetConfig from raw Map<String,Object>
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static AssetConfig extract(Map<String, Object> root) {
        Map<String, Object> cards = (Map<String, Object>) root.get("cards");

        Map<String, AssetConfig.CardAssetEntry> byEffect =
                parseCardAssetMap((Map<String, Object>) cards.get("by_effect"));
        Map<String, AssetConfig.CardAssetEntry> byType =
                parseCardAssetMap((Map<String, Object>) cards.get("by_type"));
        Map<String, AssetConfig.CardColorEntry> byColor =
                parseCardColorMap((Map<String, Object>) cards.get("by_color"));
        Map<String, AssetConfig.CardAssetEntry> byNumber =
                parseCardAssetMap((Map<String, Object>) cards.getOrDefault("by_number", Map.of()));

        Map<String, String> sounds =
                parseStringMap((Map<String, Object>) root.getOrDefault("sounds", Map.of()));

        Map<String, String> eventSounds = new LinkedHashMap<>();
        Object eventsRaw = root.get("events");
        if (eventsRaw instanceof Map<?, ?> eventsMap) {
            for (Map.Entry<?, ?> entry : eventsMap.entrySet()) {
                Map<String, Object> ev = (Map<String, Object>) entry.getValue();
                eventSounds.put((String) entry.getKey(), (String) ev.get("sound"));
            }
        }

        AssetConfig.FallbackEntry fallback = null;
        Object fbRaw = root.get("fallback");
        if (fbRaw instanceof Map<?, ?> fb) {
            fallback = new AssetConfig.FallbackEntry(
                    (String) fb.get("icon"),
                    (String) fb.get("sound"),
                    (String) fb.get("background"),
                    (String) fb.get("text")
            );
        }

        return new AssetConfig(byEffect, byType, byColor, byNumber, sounds, eventSounds, fallback);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AssetConfig.CardAssetEntry> parseCardAssetMap(
            Map<String, Object> raw) {
        Map<String, AssetConfig.CardAssetEntry> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            result.put(e.getKey(),
                    new AssetConfig.CardAssetEntry((String) v.get("icon"), (String) v.get("sound")));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AssetConfig.CardColorEntry> parseCardColorMap(
            Map<String, Object> raw) {
        Map<String, AssetConfig.CardColorEntry> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            result.put(e.getKey(),
                    new AssetConfig.CardColorEntry(
                            (String) v.get("background"), (String) v.get("text")));
        }
        return result;
    }

    private static Map<String, String> parseStringMap(Map<String, Object> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            result.put(e.getKey(), (String) e.getValue());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Minimal recursive-descent JSON parser
    // Handles: objects {}, strings "", integers, null
    // No arrays needed for this config schema.
    // -------------------------------------------------------------------------

    static final class JsonParser {

        private final String src;
        private int pos;

        JsonParser(String src) {
            this.src = src;
            this.pos = 0;
        }

        /** Advances past ASCII whitespace characters. */
        private void ws() {
            while (pos < src.length() && src.charAt(pos) <= ' ') pos++;
        }

        /** Returns the next non-whitespace character without consuming it. */
        private char peek() {
            ws();
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        /**
         * Verifies that the next non-whitespace character matches {@code expected}.
         *
         * @param expected the required next character
         * @throws IllegalStateException if the input ends early or a different character is found
         */
        private void eat(char expected) {
            ws();
            if (pos >= src.length()) {
                throw new IllegalStateException(
                        "Unexpected end of input, expected '" + expected + "'");
            }
            char got = src.charAt(pos++);
            if (got != expected) {
                throw new IllegalStateException(
                        "Expected '" + expected + "' but got '" + got + "' at position " + (pos - 1));
            }
        }

        /**
         * Parses a JSON value.
         *
         * @return the parsed object, string, integer, or {@code null} value
         */
        Object parseValue() {
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '"') return parseString();
            if (src.startsWith("null", pos)) {
                ws();
                pos += 4;
                return null;
            }
            return parseInteger();
        }

        /**
         * Parses a JSON object.
         *
         * @return the parsed object as a {@code Map<String, Object>}
         */
        Map<String, Object> parseObject() {
            eat('{');
            Map<String, Object> map = new LinkedHashMap<>();
            if (peek() == '}') {
                pos++;
                return map;
            }
            do {
                String key = parseString();
                eat(':');
                map.put(key, parseValue());
                ws();
            } while (peek() == ',' && ++pos > 0);
            eat('}');
            return map;
        }

        /**
         * Parses a JSON string, including the basic escape sequences used in the config file.
         *
         * @return the decoded string value
         */
        String parseString() {
            eat('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != '"') {
                if (src.charAt(pos) == '\\') {
                    pos++;
                    if (pos >= src.length()) break;
                    char esc = src.charAt(pos++);
                    sb.append(switch (esc) {
                        case '"'  -> '"';
                        case '\\' -> '\\';
                        case '/'  -> '/';
                        case 'n'  -> '\n';
                        case 'r'  -> '\r';
                        case 't'  -> '\t';
                        default   -> esc;
                    });
                } else {
                    sb.append(src.charAt(pos++));
                }
            }
            if (pos >= src.length()) {
                throw new IllegalStateException("Unterminated string");
            }
            pos++; // consume closing '"'
            return sb.toString();
        }

        /**
         * Parses a JSON integer literal.
         *
         * @return the parsed integer
         */
        private int parseInteger() {
            ws();
            int start = pos;
            if (pos < src.length() && src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            if (start == pos) {
                throw new IllegalStateException("Expected a value at position " + pos);
            }
            return Integer.parseInt(src.substring(start, pos));
        }
    }
}
