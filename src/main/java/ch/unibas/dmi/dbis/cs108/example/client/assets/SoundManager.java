package ch.unibas.dmi.dbis.cs108.example.client.assets;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Plays short sound effects for card actions and game events.
 *
 * <p>Clips are lazy-loaded and cached by sound ID on first use. If the media toolkit is
 * unavailable (headless server tests) or a resource is missing, the call fails silently.
 * When muted, no clips are loaded or played.</p>
 */
public final class SoundManager {

    private final AssetRegistry registry;
    private final Map<String, AudioClip> cache = new HashMap<>();
    private boolean muted = false;

    public SoundManager(AssetRegistry registry) {
        this.registry = registry;
    }

    /**
     * Plays the sound associated with the given sound ID.
     *
     * <p>If no path is found for {@code soundId}, falls back to the configured fallback sound.
     * Does nothing when muted or when {@code soundId} is {@code null}.</p>
     *
     * @param soundId the sound identifier such as {@code "card_play_generic"}
     */
    public void play(String soundId) {
        if (muted || soundId == null) return;
        try {
            AudioClip clip = cache.computeIfAbsent(soundId, this::loadClip);
            if (clip != null) clip.play();
        } catch (Exception e) {
            System.err.println("[SoundManager] Cannot play '" + soundId + "': " + e.getMessage());
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    private AudioClip loadClip(String soundId) {
        Optional<String> pathOpt = registry.getSoundPath(soundId);
        if (pathOpt.isEmpty()) {
            // Sound ID unknown in config — use the fallback path directly.
            AssetConfig.FallbackEntry fb = registry.getFallback();
            if (fb != null && fb.sound() != null) {
                pathOpt = Optional.of(fb.sound());
            }
        }
        if (pathOpt.isEmpty()) return null;

        String path = pathOpt.get();
        String absolutePath = path.startsWith("/") ? path : "/" + path;
        URL url = SoundManager.class.getResource(absolutePath);
        if (url == null) {
            System.err.println("[SoundManager] Sound not found: " + absolutePath);
            if (registry.isFallbackOnMissing()) return loadFallbackClip();
            return null;
        }
        try {
            return new AudioClip(url.toExternalForm());
        } catch (Exception e) {
            System.err.println("[SoundManager] Cannot load clip: " + absolutePath + " — " + e.getMessage());
            if (registry.isFallbackOnMissing()) return loadFallbackClip();
            return null;
        }
    }

    private AudioClip loadFallbackClip() {
        AssetConfig.FallbackEntry fb = registry.getFallback();
        if (fb == null || fb.sound() == null) return null;
        // FallbackEntry.sound is a direct file path, not a sound ID.
        String abs = fb.sound().startsWith("/") ? fb.sound() : "/" + fb.sound();
        URL url = SoundManager.class.getResource(abs);
        if (url == null) return null;
        try {
            return new AudioClip(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }
}
