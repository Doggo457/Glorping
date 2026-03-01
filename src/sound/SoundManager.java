package sound;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralises all sound effect playback.
 * Sound files are registered by name and played with adjustable volume.
 * Uses {@code javax.sound.sampled} for volume-aware playback and the custom
 * {@link SoundFilter} (echo effect for special events).
 *
 * The echo filter is applied to explosion sounds to give a cave reverb feeling.
 */
public class SoundManager {

    /** Map of sound name → file path */
    private final Map<String, String> soundPaths = new HashMap<>();
    /** Whether sound effects are enabled */
    private boolean enabled = true;
    /** Volume 0-100 as a percentage */
    private int volume = 80;

    /**
     * Registers all game sound effects.
     * Paths are relative to the working directory (lib/handout/).
     */
    public SoundManager() {
        register("shoot",        "sounds/shoot.wav");
        register("hit",          "sounds/hit.wav");
        register("player_death", "sounds/player_death.wav");
        register("jump",         "sounds/jump.wav");
        register("pickup",       "sounds/pickup.wav");
        register("enemy_death",  "sounds/enemy_death.wav");
        register("explosion",    "sounds/explosion.wav");
        register("footstep",     "sounds/footstep.wav");
    }

    /**
     * Registers a sound with the given name and file path.
     *
     * @param name Name used to reference this sound
     * @param path File path relative to working directory
     */
    public void register(String name, String path) {
        soundPaths.put(name, path);
    }

    /**
     * Plays a sound effect by name with the current volume setting.
     * Starts playback in a background thread so it does not block the game loop.
     *
     * @param name Registered sound name
     */
    public void play(String name) {
        if (!enabled || volume <= 0) return;
        String path = soundPaths.get(name);
        if (path == null) return;
        final int vol = volume;
        new Thread(() -> playWithVolume(path, vol)).start();
    }

    /**
     * Plays a sound with the cave echo filter applied.
     * Used for explosions to emphasise the cave environment.
     *
     * @param name Registered sound name
     */
    public void playWithEcho(String name) {
        if (!enabled || volume <= 0) return;
        String path = soundPaths.get(name);
        if (path == null) {
            play(name);
            return;
        }
        // Cave echo: 0.15s delay, 0.45 decay, 3 repeats
        new SoundFilter(path, 0.15f, 0.45f, 3, volume / 100f).start();
    }

    /**
     * Plays a WAV file at the given volume using javax.sound.sampled.
     * Uses FloatControl.Type.MASTER_GAIN to set volume as decibels.
     *
     * @param path File path relative to working directory
     * @param vol  Volume percentage 0-100
     */
    private void playWithVolume(String path, int vol) {
        try {
            File file = new File(path);
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);

            // Convert percentage to decibels (0% = -80dB silence, 100% = 0dB full)
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = 20f * (float) Math.log10(vol / 100f);
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }

            clip.start();
            Thread.sleep(100);
            while (clip.isRunning()) Thread.sleep(100);
            clip.close();
        } catch (Exception ignored) {}
    }

    /**
     * Enables or disables all sound effects.
     *
     * @param enabled True to enable, false to silence
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return True if sound effects are currently enabled */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets the SFX volume.
     *
     * @param vol Volume percentage 0-100
     */
    public void setVolume(int vol) { this.volume = Math.max(0, Math.min(100, vol)); }

    /** @return Current volume 0-100 */
    public int getVolume() { return volume; }
}
