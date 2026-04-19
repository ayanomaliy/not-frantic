package ch.unibas.dmi.dbis.cs108.example.client.assets;

import java.util.Map;

/**
 * Immutable representation of the contents of {@code asset-config.json}.
 *
 * <p>All maps exposed by this class are unmodifiable snapshots. Values that are {@code null}
 * in the JSON source remain {@code null} in the corresponding record fields.</p>
 */
public final class AssetConfig {

    /**
     * Asset references for a card entry.
     *
     * @param icon classpath-relative icon path, or {@code null} if no icon is configured
     * @param sound sound identifier, or {@code null} if no sound is configured
     */
    public record CardAssetEntry(String icon, String sound) {}

    /**
     * Display colors for a card color entry.
     *
     * @param background CSS-compatible background color value
     * @param text CSS-compatible text color value
     */
    public record CardColorEntry(String background, String text) {}

    /**
     * Fallback assets used when no more specific mapping exists.
     *
     * @param icon classpath-relative fallback icon path, or {@code null}
     * @param sound classpath-relative fallback sound path (not an ID), or {@code null}
     * @param background fallback background color, or {@code null}
     * @param text fallback text color, or {@code null}
     */
    public record FallbackEntry(String icon, String sound, String background, String text) {}

    private final Map<String, CardAssetEntry> byEffect;
    private final Map<String, CardAssetEntry> byType;
    private final Map<String, CardColorEntry> byColor;
    private final Map<String, CardAssetEntry> byNumber;
    private final Map<String, String> sounds;
    private final Map<String, String> eventSounds;
    private final FallbackEntry fallback;

    /**
     * Creates an immutable asset configuration snapshot.
     *
     * @param byEffect card asset entries keyed by {@code SpecialEffect} name
     * @param byType card asset entries keyed by {@code CardType} name
     * @param byColor color entries keyed by {@code CardColor} name
     * @param byNumber card asset entries keyed by card face value
     * @param sounds sound resource paths keyed by sound identifier
     * @param eventSounds sound identifiers keyed by event name
     * @param fallback fallback values used when no specific mapping exists
     */
    public AssetConfig(
            Map<String, CardAssetEntry> byEffect,
            Map<String, CardAssetEntry> byType,
            Map<String, CardColorEntry> byColor,
            Map<String, CardAssetEntry> byNumber,
            Map<String, String> sounds,
            Map<String, String> eventSounds,
            FallbackEntry fallback) {
        this.byEffect = Map.copyOf(byEffect);
        this.byType = Map.copyOf(byType);
        this.byColor = Map.copyOf(byColor);
        this.byNumber = Map.copyOf(byNumber);
        this.sounds = Map.copyOf(sounds);
        this.eventSounds = Map.copyOf(eventSounds);
        this.fallback = fallback;
    }

    /** Returns icon and sound entries keyed by {@code SpecialEffect} name. */
    public Map<String, CardAssetEntry> byEffect() { return byEffect; }

    /** Returns icon and sound entries keyed by {@code CardType} name. */
    public Map<String, CardAssetEntry> byType() { return byType; }

    /** Returns background and text color entries keyed by {@code CardColor} name. */
    public Map<String, CardColorEntry> byColor() { return byColor; }

    /** Returns icon and sound entries keyed by the card face value such as {@code "1"} or {@code "9"}. */
    public Map<String, CardAssetEntry> byNumber() { return byNumber; }

    /** Returns sound resource paths keyed by sound identifier such as {@code "card_play_generic"}. */
    public Map<String, String> sounds() { return sounds; }

    /** Returns event-to-sound mappings keyed by event name such as {@code "GAME_STARTED"}. */
    public Map<String, String> eventSounds() { return eventSounds; }

    /** Returns the fallback entry, or {@code null} if no fallback is configured. */
    public FallbackEntry fallback() { return fallback; }
}
