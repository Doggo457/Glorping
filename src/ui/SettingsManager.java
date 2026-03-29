package ui;

import sound.SoundManager;
import sound.MusicPlayer;

/**
 * Manages all game settings, cheats, and resolution state.
 * Extracted from GlorpingGame to centralise settings logic
 * and keep the main game class focused on gameplay.
 *
 * <p>Resolution and fullscreen changes are delegated to a
 * {@link ResizeCallback} supplied at construction time, since
 * those operations require direct access to the JFrame.</p>
 */
public class SettingsManager {

    // ── Callback interface ──────────────────────────────────────────────

    /**
     * Callback used to apply window resize and fullscreen changes.
     * The implementation should resize the JFrame and update the
     * rendering surface accordingly.
     */
    public interface ResizeCallback {
        /**
         * Applies the requested window dimensions and fullscreen mode.
         *
         * @param w          New window width in pixels
         * @param h          New window height in pixels
         * @param fullscreen True to enter fullscreen, false for windowed
         */
        void applyResize(int w, int h, boolean fullscreen);
    }

    // ── Fields ──────────────────────────────────────────────────────────

    /** Whether the player is invincible (cheat) */
    private boolean godMode = false;
    /** Whether mana never depletes (cheat) */
    private boolean infiniteMana = false;
    /** Whether the game is in fullscreen mode */
    private boolean fullscreen = false;
    /** Whether background music is enabled */
    private boolean musicEnabled = true;
    /** Whether sound effects are enabled */
    private boolean sfxEnabled = true;
    /** Whether the FPS counter is displayed */
    private boolean showFps = false;
    /** Whether screen-shake effects are active */
    private boolean screenShake = true;
    /** Whether debug overlays are drawn */
    private boolean debug = false;

    /** Currently selected tab on the settings screen */
    private String settingsTab = "video";
    /** Game state to return to when leaving settings */
    private GameState preSettingsState = GameState.MENU;

    /** Current window width in pixels */
    private int windowW = 800;
    /** Current window height in pixels */
    private int windowH = 600;

    /** Callback invoked when the window needs to be resized */
    private final ResizeCallback resizeCallback;

    // ── Constructor ─────────────────────────────────────────────────────

    /**
     * Creates a new SettingsManager with the given resize callback.
     *
     * @param resizeCallback Callback used to apply resolution and
     *                       fullscreen changes to the game window.
     *                       Must not be {@code null}.
     */
    public SettingsManager(ResizeCallback resizeCallback) {
        this.resizeCallback = resizeCallback;
    }

    // ── Settings action dispatcher ──────────────────────────────────────

    /**
     * Handles a named settings action triggered from the settings menu.
     * Actions correspond to UI button identifiers.
     *
     * <p>Supported actions:</p>
     * <ul>
     *   <li>{@code "tab_video"}, {@code "tab_audio"}, {@code "tab_gameplay"}
     *       &ndash; switch the active settings tab</li>
     *   <li>{@code "toggle_fullscreen"} &ndash; toggle fullscreen mode</li>
     *   <li>{@code "toggle_fps"} &ndash; toggle the FPS counter</li>
     *   <li>{@code "res_800_600"}, {@code "res_1024_768"},
     *       {@code "res_1280_720"}, {@code "res_1920_1080"}
     *       &ndash; change window resolution</li>
     *   <li>{@code "toggle_music"} &ndash; toggle background music</li>
     *   <li>{@code "toggle_sfx"} &ndash; toggle sound effects</li>
     *   <li>{@code "toggle_debug"} &ndash; toggle debug overlays</li>
     *   <li>{@code "toggle_god"} &ndash; toggle god mode (invincibility)</li>
     *   <li>{@code "toggle_mana"} &ndash; toggle infinite mana</li>
     *   <li>{@code "toggle_shake"} &ndash; toggle screen-shake effects</li>
     *   <li>{@code "back"} &ndash; close settings and return to the
     *       previous game state</li>
     * </ul>
     *
     * @param action The action identifier string
     * @param sounds The SoundManager instance for toggling SFX
     * @param music  The MusicPlayer instance for toggling music
     * @return The {@link GameState} to transition to if the state should
     *         change (i.e. for {@code "back"}), or {@code null} if the
     *         game state is unchanged
     */
    public GameState handleSettingsAction(String action, SoundManager sounds, MusicPlayer music) {
        if (action == null) return null;

        switch (action) {
            // ── Tab switching ───────────────────────────────────────
            case "tab_video":
                settingsTab = "video";
                break;
            case "tab_audio":
                settingsTab = "audio";
                break;
            case "tab_gameplay":
                settingsTab = "gameplay";
                break;

            // ── Video settings ──────────────────────────────────────
            case "toggle_fullscreen":
                toggleFullscreen();
                break;
            case "toggle_fps":
                showFps = !showFps;
                break;
            case "res_800_600":
                changeResolution(800, 600);
                break;
            case "res_1024_768":
                changeResolution(1024, 768);
                break;
            case "res_1280_720":
                changeResolution(1280, 720);
                break;
            case "res_1920_1080":
                changeResolution(1920, 1080);
                break;

            // ── Audio settings ──────────────────────────────────────
            case "toggle_music":
                musicEnabled = !musicEnabled;
                if (musicEnabled) {
                    music.startMusic();
                } else {
                    music.stopMusic();
                }
                break;
            case "toggle_sfx":
                sfxEnabled = !sfxEnabled;
                sounds.setEnabled(sfxEnabled);
                break;

            // ── Gameplay / cheat settings ────────────────────────────
            case "toggle_debug":
                debug = !debug;
                break;
            case "toggle_god":
                godMode = !godMode;
                break;
            case "toggle_mana":
                infiniteMana = !infiniteMana;
                break;
            case "toggle_shake":
                screenShake = !screenShake;
                break;

            // ── Back / close settings ───────────────────────────────
            case "back":
                settingsTab = "video";
                return preSettingsState;

            default:
                break;
        }

        return null;
    }

    // ── Resolution & fullscreen ─────────────────────────────────────────

    /**
     * Changes the window resolution and notifies the resize callback.
     *
     * @param w New width in pixels
     * @param h New height in pixels
     */
    public void changeResolution(int w, int h) {
        windowW = w;
        windowH = h;
        resizeCallback.applyResize(windowW, windowH, fullscreen);
    }

    /**
     * Toggles between fullscreen and windowed mode and notifies the
     * resize callback with the current window dimensions.
     */
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        resizeCallback.applyResize(windowW, windowH, fullscreen);
    }

    // ── Getters & setters ───────────────────────────────────────────────

    /** @return True if god mode (invincibility) is active */
    public boolean isGodMode() { return godMode; }
    /** @param godMode True to enable god mode */
    public void setGodMode(boolean godMode) { this.godMode = godMode; }

    /** @return True if infinite mana cheat is active */
    public boolean isInfiniteMana() { return infiniteMana; }
    /** @param infiniteMana True to enable infinite mana */
    public void setInfiniteMana(boolean infiniteMana) { this.infiniteMana = infiniteMana; }

    /** @return True if the game is in fullscreen mode */
    public boolean isFullscreen() { return fullscreen; }
    /** @param fullscreen True for fullscreen, false for windowed */
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }

    /** @return True if background music is enabled */
    public boolean isMusicEnabled() { return musicEnabled; }
    /** @param musicEnabled True to enable music */
    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    /** @return True if sound effects are enabled */
    public boolean isSfxEnabled() { return sfxEnabled; }
    /** @param sfxEnabled True to enable sound effects */
    public void setSfxEnabled(boolean sfxEnabled) { this.sfxEnabled = sfxEnabled; }

    /** @return True if the FPS counter should be displayed */
    public boolean isShowFps() { return showFps; }
    /** @param showFps True to show the FPS counter */
    public void setShowFps(boolean showFps) { this.showFps = showFps; }

    /** @return True if screen-shake effects are enabled */
    public boolean isScreenShake() { return screenShake; }
    /** @param screenShake True to enable screen shake */
    public void setScreenShake(boolean screenShake) { this.screenShake = screenShake; }

    /** @return True if debug overlays are enabled */
    public boolean isDebug() { return debug; }
    /** @param debug True to show debug overlays */
    public void setDebug(boolean debug) { this.debug = debug; }

    /** @return The currently selected settings tab name */
    public String getSettingsTab() { return settingsTab; }
    /** @param settingsTab Tab identifier ("video", "audio", "gameplay") */
    public void setSettingsTab(String settingsTab) { this.settingsTab = settingsTab; }

    /** @return The game state saved before opening settings */
    public GameState getPreSettingsState() { return preSettingsState; }
    /** @param preSettingsState State to restore when leaving settings */
    public void setPreSettingsState(GameState preSettingsState) { this.preSettingsState = preSettingsState; }

    /** @return Current window width in pixels */
    public int getWindowW() { return windowW; }
    /** @param windowW Window width in pixels */
    public void setWindowW(int windowW) { this.windowW = windowW; }

    /** @return Current window height in pixels */
    public int getWindowH() { return windowH; }
    /** @param windowH Window height in pixels */
    public void setWindowH(int windowH) { this.windowH = windowH; }
}
