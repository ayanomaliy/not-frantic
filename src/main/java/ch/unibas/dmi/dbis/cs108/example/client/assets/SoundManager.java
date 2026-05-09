package ch.unibas.dmi.dbis.cs108.example.client.assets;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Plays short sound effects and manages looping background music.
 *
 * <p>Short effects use {@link AudioClip}. The background music uses
 * {@link MediaPlayer}, because it supports looping and live volume changes.</p>
 */
public final class SoundManager {

    private final AssetRegistry registry;
    private final Map<String, AudioClip> cache = new HashMap<>();

    private boolean muted = false;

    private MediaPlayer backgroundPlayer;
    private String currentBackgroundSoundId;
    private double musicVolume = 0.35;

    public SoundManager(AssetRegistry registry) {
        this.registry = registry;
    }

    /**
     * Plays a short sound effect.
     *
     * @param soundId the sound identifier such as {@code "card_play_generic"}
     */
    public void play(String soundId) {
        if (muted || soundId == null) {
            return;
        }

        try {
            AudioClip clip = cache.computeIfAbsent(soundId, this::loadClip);
            if (clip != null) {
                clip.play();
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] Cannot play '" + soundId + "': " + e.getMessage());
        }
    }

    /**
     * Starts looping background music. Calling this again with the same sound id
     * does not restart the track.
     *
     * @param soundId the configured sound id, for example {@code "background_music"}
     */
    public void playBackgroundMusic(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return;
        }

        if (backgroundPlayer != null && soundId.equals(currentBackgroundSoundId)) {
            return;
        }

        stopBackgroundMusic();

        Optional<String> pathOpt = registry.getSoundPath(soundId);
        if (pathOpt.isEmpty()) {
            System.err.println("[SoundManager] Background music id not found: " + soundId);
            return;
        }

        String path = pathOpt.get();
        String absolutePath = path.startsWith("/") ? path : "/" + path;

        URL url = SoundManager.class.getResource(absolutePath);
        if (url == null) {
            System.err.println("[SoundManager] Background music not found: " + absolutePath);
            return;
        }

        try {
            Media media = new Media(url.toExternalForm());
            backgroundPlayer = new MediaPlayer(media);
            backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundPlayer.setVolume(musicVolume);
            backgroundPlayer.play();

            currentBackgroundSoundId = soundId;
        } catch (Exception e) {
            System.err.println("[SoundManager] Cannot start background music: " + e.getMessage());
            backgroundPlayer = null;
            currentBackgroundSoundId = null;
        }
    }

    /**
     * Stops the background music.
     */
    public void stopBackgroundMusic() {
        if (backgroundPlayer != null) {
            try {
                backgroundPlayer.stop();
                backgroundPlayer.dispose();
            } catch (Exception ignored) {
                // Ignore media shutdown errors.
            }
        }

        backgroundPlayer = null;
        currentBackgroundSoundId = null;
    }

    /**
     * Sets the background music volume.
     *
     * @param volume value between 0.0 and 1.0
     */
    public void setMusicVolume(double volume) {
        musicVolume = clamp(volume);

        if (backgroundPlayer != null) {
            backgroundPlayer.setVolume(musicVolume);
        }
    }

    /**
     * Returns the current background music volume.
     *
     * @return value between 0.0 and 1.0
     */
    public double getMusicVolume() {
        return musicVolume;
    }

    /**
     * Mutes only short sound effects. Background music volume is controlled
     * separately through {@link #setMusicVolume(double)}.
     *
     * @param muted whether sound effects should be muted
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }

        if (value > 1.0) {
            return 1.0;
        }

        return value;
    }

    private AudioClip loadClip(String soundId) {
        Optional<String> pathOpt = registry.getSoundPath(soundId);

        if (pathOpt.isEmpty()) {
            AssetConfig.FallbackEntry fallback = registry.getFallback();
            if (fallback != null && fallback.sound() != null) {
                pathOpt = Optional.of(fallback.sound());
            }
        }

        if (pathOpt.isEmpty()) {
            return null;
        }

        String path = pathOpt.get();
        String absolutePath = path.startsWith("/") ? path : "/" + path;

        URL url = SoundManager.class.getResource(absolutePath);
        if (url == null) {
            System.err.println("[SoundManager] Sound not found: " + absolutePath);

            if (registry.isFallbackOnMissing()) {
                return loadFallbackClip();
            }

            return null;
        }

        try {
            return new AudioClip(url.toExternalForm());
        } catch (Exception e) {
            System.err.println("[SoundManager] Cannot load clip: " + absolutePath + " — " + e.getMessage());

            if (registry.isFallbackOnMissing()) {
                return loadFallbackClip();
            }

            return null;
        }
    }

    private AudioClip loadFallbackClip() {
        AssetConfig.FallbackEntry fallback = registry.getFallback();

        if (fallback == null || fallback.sound() == null) {
            return null;
        }

        String absolutePath = fallback.sound().startsWith("/")
                ? fallback.sound()
                : "/" + fallback.sound();

        URL url = SoundManager.class.getResource(absolutePath);
        if (url == null) {
            return null;
        }

        try {
            return new AudioClip(url.toExternalForm());
        } catch (Exception ignored) {
            return null;
        }
    }
}