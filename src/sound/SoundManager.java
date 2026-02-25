package sound;

import game2D.Sound;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralises all sound effect playback.
 * Sound files are registered by name and played via the library's
 * {@link game2D.Sound} class (standard playback) or the custom
 * {@link SoundFilter} (echo effect for special events).
 *
 * The echo filter is applied to explosion sounds to give a cave reverb feeling.
 */
public class SoundManager {

    /** Map of sound name → file path */
    private final Map<String, String> soundPaths = new HashMap<>();
    /** Whether sound effects are enabled */
    private boolean enabled = true;

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
     * Plays a sound effect by name.
     * Starts playback in a background thread so it does not block the game loop.
     *
     * @param name Registered sound name
     */
    public void play(String name) {
        if (!enabled) return;
        String path = soundPaths.get(name);
        if (path == null) return;
        new Sound(path).start();
    }

    /**
     * Plays a sound with the cave echo filter applied.
     * Used for explosions to emphasise the cave environment.
     *
     * @param name Registered sound name
     */
    public void playWithEcho(String name) {
        if (!enabled) return;
        String path = soundPaths.get(name);
        if (path == null) {
            play(name);
            return;
        }
        // Cave echo: 0.15s delay, 0.45 decay, 3 repeats
        new SoundFilter(path, 0.15f, 0.45f, 3).start();
    }

    /**
     * Enables or disables all sound effects.
     *
     * @param enabled True to enable, false to silence
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return True if sound effects are currently enabled */
    public boolean isEnabled() { return enabled; }
}
