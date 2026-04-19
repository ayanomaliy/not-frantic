package ch.unibas.dmi.dbis.cs108.example.client.assets;

import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import com.fluxvend.svgfx.SvgImageView;

import java.util.Optional;

/**
 * Resolves visual and audio assets for cards and named game events.
 *
 * <p>The lookup methods in this class are JavaFX-independent and may be used from any thread.
 * Methods that construct {@link SvgImageView} instances must be called on the JavaFX
 * Application Thread.</p>
 *
 * <p>Card icon and sound resolution uses the following priority order:</p>
 * <ol>
 *   <li>{@code by_effect} for cards with a {@code SpecialEffect}</li>
 *   <li>{@code by_number} for numbered cards</li>
 *   <li>{@code by_type} for type-based defaults</li>
 *   <li>{@code fallback} as the final configured fallback</li>
 * </ol>
 *
 * <p>If a matching entry exists but the requested icon or sound field is {@code null}, lookup
 * stops there and returns {@link Optional#empty()}.</p>
 */
public final class AssetRegistry {

    private final AssetConfig config;

    /**
     * When {@code true} (default), a missing classpath resource is silently replaced by the
     * configured fallback asset. Set to {@code false} during development to surface every
     * unresolved path as a console warning without a substitution.
     */
    private boolean fallbackOnMissing = true;

    private AssetRegistry(AssetConfig config) {
        this.config = config;
    }

    /**
     * Loads a registry from the default classpath configuration.
     *
     * @return a registry backed by the loaded config, or an empty registry if loading fails
     */
    public static AssetRegistry load() {
        AssetConfig cfg = AssetConfigLoader.load();
        if (cfg == null) cfg = emptyConfig();
        return new AssetRegistry(cfg);
    }

    /**
     * Creates a registry from an existing configuration.
     *
     * @param config the configuration to use; {@code null} creates an empty registry
     * @return a registry backed by {@code config} or by an empty config if {@code config} is null
     */
    public static AssetRegistry of(AssetConfig config) {
        return new AssetRegistry(config != null ? config : emptyConfig());
    }

    /**
     * Controls whether missing classpath resources are silently replaced by the fallback asset.
     *
     * <p>Set to {@code false} during development to log a warning for every unresolved path and
     * return an empty result instead of substituting the fallback. Defaults to {@code true}.</p>
     *
     * @param fallbackOnMissing {@code true} to enable silent fallback; {@code false} for strict mode
     */
    public void setFallbackOnMissing(boolean fallbackOnMissing) {
        this.fallbackOnMissing = fallbackOnMissing;
    }

    /** Returns whether fallback substitution is enabled for missing resources. */
    public boolean isFallbackOnMissing() {
        return fallbackOnMissing;
    }

    // -------------------------------------------------------------------------
    // Icon resolution — no JavaFX required
    // -------------------------------------------------------------------------

    /**
     * Returns the icon resource path for the given card.
     *
     * @param card the card whose icon should be resolved
     * @return the classpath-relative icon path, or an empty optional if no icon is configured
     */
    public Optional<String> getIconPath(Card card) {
        AssetConfig.CardAssetEntry entry = resolveCardEntry(card);
        return entry != null ? Optional.ofNullable(entry.icon()) : Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Icon view factory — must be called on the JavaFX Application Thread
    // -------------------------------------------------------------------------

    /**
     * Creates a sized {@link SvgImageView} for the given card.
     *
     * <p>This method must be called on the JavaFX Application Thread.</p>
     *
     * @param card the card whose icon should be loaded
     * @param size the preferred width and height in pixels
     * @return the created image view, or an empty optional if no icon can be resolved or loaded
     */
    public Optional<SvgImageView> getIconView(Card card, double size) {
        return getIconPath(card).flatMap(path -> createIconView(path, size));
    }

    /**
     * Creates a {@link SvgImageView} from an icon resource path.
     *
     * <p>If {@code iconPath} does not start with {@code /}, this method prepends it before
     * loading. This method must be called on the JavaFX Application Thread.</p>
     *
     * @param iconPath classpath-relative icon path such as {@code "icons/card_skip.svg"}
     * @param size preferred width and height in pixels
     * @return the created image view, or an empty optional if the path is blank or cannot be loaded
     */
    public Optional<SvgImageView> createIconView(String iconPath, double size) {
        if (iconPath == null || iconPath.isBlank()) return Optional.empty();
        String absolutePath = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
        if (AssetRegistry.class.getResource(absolutePath) == null) {
            System.err.println("[AssetRegistry] Icon not found: " + absolutePath);
            if (fallbackOnMissing) return tryFallbackIconView(size);
            return Optional.empty();
        }
        try {
            SvgImageView view = new SvgImageView(absolutePath);
            view.setPrefWidth(size);
            view.setPrefHeight(size);
            return Optional.of(view);
        } catch (Exception e) {
            System.err.println("[AssetRegistry] Cannot load icon: " + absolutePath + " — " + e.getMessage());
            if (fallbackOnMissing) return tryFallbackIconView(size);
            return Optional.empty();
        }
    }

    private Optional<SvgImageView> tryFallbackIconView(double size) {
        if (config.fallback() == null || config.fallback().icon() == null) return Optional.empty();
        String fb = config.fallback().icon();
        String abs = fb.startsWith("/") ? fb : "/" + fb;
        if (AssetRegistry.class.getResource(abs) == null) return Optional.empty();
        try {
            SvgImageView view = new SvgImageView(abs);
            view.setPrefWidth(size);
            view.setPrefHeight(size);
            return Optional.of(view);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Sound resolution — no JavaFX required
    // -------------------------------------------------------------------------

    /**
     * Returns the sound identifier associated with a card.
     *
     * @param card the card whose sound should be resolved
     * @return the sound identifier, or an empty optional if none is configured
     */
    public Optional<String> getSoundId(Card card) {
        AssetConfig.CardAssetEntry entry = resolveCardEntry(card);
        if (entry != null) return Optional.ofNullable(entry.sound());
        return config.fallback() != null
                ? Optional.ofNullable(config.fallback().sound())
                : Optional.empty();
    }

    /**
     * Returns the sound identifier mapped to a named game event.
     *
     * @param eventName event name such as {@code "GAME_STARTED"}
     * @return the configured sound identifier, or an empty optional if none is mapped
     */
    public Optional<String> getSoundId(String eventName) {
        if (eventName == null) return Optional.empty();
        return Optional.ofNullable(config.eventSounds().get(eventName));
    }

    /**
     * Returns the resource path for a sound identifier.
     *
     * @param soundId sound identifier such as {@code "card_play_generic"}
     * @return the classpath-relative sound path, or an empty optional if the identifier is unknown
     */
    public Optional<String> getSoundPath(String soundId) {
        if (soundId == null) return Optional.empty();
        return Optional.ofNullable(config.sounds().get(soundId));
    }

    // -------------------------------------------------------------------------
    // Color resolution — no JavaFX required
    // -------------------------------------------------------------------------

    /**
     * Returns the configured background color for a card color.
     *
     * @param color the card color to resolve, or {@code null} to use the fallback directly
     * @return the configured background color, or an empty optional if no value is available
     */
    public Optional<String> getBackgroundColor(CardColor color) {
        if (color != null) {
            AssetConfig.CardColorEntry e = config.byColor().get(color.name());
            if (e != null) return Optional.of(e.background());
        }
        return config.fallback() != null
                ? Optional.ofNullable(config.fallback().background())
                : Optional.empty();
    }

    /**
     * Returns the configured text color for a card color.
     *
     * @param color the card color to resolve, or {@code null} to use the fallback directly
     * @return the configured text color, or an empty optional if no value is available
     */
    public Optional<String> getTextColor(CardColor color) {
        if (color != null) {
            AssetConfig.CardColorEntry e = config.byColor().get(color.name());
            if (e != null) return Optional.of(e.text());
        }
        return config.fallback() != null
                ? Optional.ofNullable(config.fallback().text())
                : Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Fallback — no JavaFX required
    // -------------------------------------------------------------------------

    /**
     * Returns the configured fallback asset entry.
     *
     * @return the fallback entry, or {@code null} if none is configured
     */
    public AssetConfig.FallbackEntry getFallback() {
        return config.fallback();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the most specific asset entry for a card.
     *
     * @param card the card to resolve
     * @return the matching entry using effect, number, then type priority; {@code null} if none match
     */
    private AssetConfig.CardAssetEntry resolveCardEntry(Card card) {
        if (card == null) return null;
        if (card.effect() != null) {
            AssetConfig.CardAssetEntry e = config.byEffect().get(card.effect().name());
            if (e != null) return e;
        }
        if (card.value() > 0) {
            AssetConfig.CardAssetEntry e = config.byNumber().get(String.valueOf(card.value()));
            if (e != null) return e;
        }
        if (card.type() != null) {
            AssetConfig.CardAssetEntry e = config.byType().get(card.type().name());
            if (e != null) return e;
        }
        return null;
    }

    private static AssetConfig emptyConfig() {
        return new AssetConfig(
                java.util.Map.of(), java.util.Map.of(),
                java.util.Map.of(), java.util.Map.of(),
                java.util.Map.of(), java.util.Map.of(),
                null);
    }
}
