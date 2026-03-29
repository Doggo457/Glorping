package ui;

import java.awt.event.*;
import java.util.Map;

import entities.GrapplingHook;
import entities.Player;
import projectiles.Projectile;
import sound.SoundManager;
import spells.SpellType;
import world.Camera;
import world.Level;
import game2D.Animation;

/**
 * Centralizes all mouse and keyboard input handling for the game.
 * <p>
 * This class extracts input logic from the main game class so that
 * {@code GlorpingGame} can delegate all input events here. It implements
 * {@link MouseListener} and {@link MouseMotionListener} and expects keyboard
 * events to be forwarded via {@link #keyPressed(KeyEvent)} and
 * {@link #keyReleased(KeyEvent)}.
 * <p>
 * Game-state-affecting actions (e.g. changing the game state, accessing the
 * player, camera, or level) are performed through the {@link GameActions}
 * callback interface supplied at construction time.
 */
public class InputHandler implements MouseListener, MouseMotionListener {

    // =========================================================================
    // Callback interface
    // =========================================================================

    /**
     * Callback interface that the main game class implements so that this
     * handler can read and modify game state without holding direct references
     * to every subsystem.
     */
    public interface GameActions {
        /** Sets the current game state. */
        void setGameState(GameState state);

        /** Returns the current game state. */
        GameState getGameState();

        /** Returns the player entity. */
        Player getPlayer();

        /** Returns the currently active level. */
        Level getCurrentLevel();

        /** Returns the camera. */
        Camera getCamera();

        /** Returns the sound manager. */
        SoundManager getSounds();

        /** Returns the HUD. */
        HUD getHud();

        /** Returns the menu. */
        Menu getMenu();

        /** Returns the map of spell-type to animation used for projectiles. */
        Map<SpellType, Animation> getSpellAnims();

        /** Converts a raw screen X coordinate to the 800x600 game coordinate. */
        int screenToGameX(int sx);

        /** Converts a raw screen Y coordinate to the 800x600 game coordinate. */
        int screenToGameY(int sy);

        /** Handles a named menu action string returned by the menu system. */
        void handleMenuAction(String action);
    }

    // =========================================================================
    // Fields
    // =========================================================================

    /** Reference to the game's action callbacks. */
    private final GameActions game;

    /** Last known mouse position in raw screen (window) coordinates. */
    private int mouseScreenX;
    private int mouseScreenY;

    /** Whether the left mouse button is currently held down. */
    private boolean mouseDown;

    /** Whether the right mouse button is currently held down. */
    private boolean rightMouseDown;

    /** Whether the jump key is currently held (prevents repeated jumps). */
    private boolean jumpKeyHeld;

    /** The active grappling hook, or {@code null} if none is deployed. */
    private GrapplingHook grapplingHook;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a new InputHandler that delegates game-state queries and
     * mutations to the supplied {@link GameActions} callback.
     *
     * @param game the callback interface for accessing and modifying game state
     */
    public InputHandler(GameActions game) {
        this.game = game;
    }

    // =========================================================================
    // Getters and setters
    // =========================================================================

    /**
     * Returns the last known mouse X position in raw screen coordinates.
     *
     * @return mouse screen X
     */
    public int getMouseScreenX() {
        return mouseScreenX;
    }

    /**
     * Returns the last known mouse Y position in raw screen coordinates.
     *
     * @return mouse screen Y
     */
    public int getMouseScreenY() {
        return mouseScreenY;
    }

    /**
     * Returns {@code true} if the left mouse button is currently held down.
     *
     * @return whether LMB is pressed
     */
    public boolean isMouseDown() {
        return mouseDown;
    }

    /**
     * Returns the current grappling hook, or {@code null} if none is active.
     *
     * @return the grappling hook
     */
    public GrapplingHook getGrapplingHook() {
        return grapplingHook;
    }

    /**
     * Sets the grappling hook reference. Pass {@code null} to clear it.
     *
     * @param hook the grappling hook to set
     */
    public void setGrapplingHook(GrapplingHook hook) {
        this.grapplingHook = hook;
    }

    /**
     * Returns {@code true} if the jump key is currently held.
     *
     * @return whether the jump key is held
     */
    public boolean isJumpKeyHeld() {
        return jumpKeyHeld;
    }

    /**
     * Sets the jump-key-held flag. Useful when resetting input state
     * (e.g. on game restart).
     *
     * @param held whether the jump key should be considered held
     */
    public void setJumpKeyHeld(boolean held) {
        this.jumpKeyHeld = held;
    }

    // =========================================================================
    // Keyboard input
    // =========================================================================

    /**
     * Handles a key-press event. Delegates to {@link #handlePlayingKeyPress}
     * when in the {@link GameState#PLAYING} state, or to
     * {@link #handleEscape()} when the Escape key is pressed.
     *
     * @param e the key event
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {
            handleEscape();
            return;
        }

        switch (game.getGameState()) {
            case PLAYING:
                handlePlayingKeyPress(key);
                break;
            default:
                break;
        }
    }

    /**
     * Handles key presses during active gameplay. Supports:
     * <ul>
     *   <li>A / Left Arrow -- move left</li>
     *   <li>D / Right Arrow -- move right</li>
     *   <li>Space / Up Arrow -- jump (releases grappling hook first)</li>
     *   <li>P -- pause</li>
     *   <li>1-4 -- select wand by slot</li>
     *   <li>Q -- cycle to next wand</li>
     *   <li>E -- swap wand with ground wand</li>
     * </ul>
     *
     * @param key the key code from the {@link KeyEvent}
     */
    private void handlePlayingKeyPress(int key) {
        Player player = game.getPlayer();

        switch (key) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:
                player.setMoveLeft(true);  break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT:
                player.setMoveRight(true); break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_UP:
                if (!jumpKeyHeld) {
                    // Release hook on jump to launch off swing
                    if (grapplingHook != null) {
                        grapplingHook.restorePlayerGravity(player);
                        grapplingHook.release();
                        grapplingHook = null;
                    }
                    player.pressJump();
                    game.getSounds().play("jump");
                    jumpKeyHeld = true;
                }
                break;
            case KeyEvent.VK_P:
                game.setGameState(GameState.PAUSED); break;
            case KeyEvent.VK_1: player.selectWand(0); break;
            case KeyEvent.VK_2: player.selectWand(1); break;
            case KeyEvent.VK_3: player.selectWand(2); break;
            case KeyEvent.VK_4: player.selectWand(3); break;
            case KeyEvent.VK_Q: player.nextWand();    break;
            case KeyEvent.VK_E: tryWandSwap();        break;
        }
    }

    /**
     * Handles key-release events. Stops left/right movement and clears the
     * jump-held flag when the corresponding keys are released.
     *
     * @param e the key event
     */
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) return;

        Player player = game.getPlayer();

        switch (key) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:
                player.setMoveLeft(false);  break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT:
                player.setMoveRight(false); break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_UP:
                jumpKeyHeld = false; break;
        }
    }

    /**
     * Handles the Escape key depending on the current game state:
     * <ul>
     *   <li>PLAYING -- pause the game</li>
     *   <li>PAUSED -- resume playing</li>
     *   <li>SETTINGS -- return to the previous state</li>
     *   <li>DEAD / WIN / LEVEL_COMPLETE -- return to main menu</li>
     *   <li>MENU -- no action</li>
     * </ul>
     */
    private void handleEscape() {
        switch (game.getGameState()) {
            case PLAYING:
                game.setGameState(GameState.PAUSED); break;
            case PAUSED:
                game.setGameState(GameState.PLAYING); break;
            case SETTINGS:
                // The main game class tracks preSettingsState; delegate via handleMenuAction
                game.handleMenuAction("back"); break;
            case DEAD:
            case WIN:
            case LEVEL_COMPLETE:
                game.handleMenuAction("main_menu"); break;
            case MENU:
                break;
            default: break;
        }
    }

    // =========================================================================
    // Mouse input
    // =========================================================================

    /**
     * Handles mouse-button presses.
     * <ul>
     *   <li>LMB while playing -- begin shooting</li>
     *   <li>LMB on a menu -- click the menu button</li>
     *   <li>RMB while playing -- fire grappling hook toward the cursor</li>
     * </ul>
     *
     * @param e the mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (game.getGameState() == GameState.PLAYING) {
                mouseDown = true;
                game.getPlayer().setShootHeld(true);
            } else {
                int gx = game.screenToGameX(e.getX());
                int gy = game.screenToGameY(e.getY());
                String action = game.getMenu().handleClick(gx, gy);
                if (action != null) {
                    game.handleMenuAction(action);
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (game.getGameState() == GameState.PLAYING) {
                rightMouseDown = true;
                // Fire grappling hook toward mouse
                int gx = game.screenToGameX(e.getX());
                int gy = game.screenToGameY(e.getY());
                Player player = game.getPlayer();
                Camera camera = game.getCamera();
                float worldAimX = gx - camera.getDrawX();
                float worldAimY = gy - camera.getDrawY();
                float px = player.getX() + player.getWidth() / 2f;
                float py = player.getY() + player.getHeight() / 2f;
                grapplingHook = new GrapplingHook(px, py, worldAimX, worldAimY);
                game.getSounds().play("shoot");
            }
        }
    }

    /**
     * Handles mouse-button releases.
     * <ul>
     *   <li>LMB -- stop shooting</li>
     *   <li>RMB -- release grappling hook</li>
     * </ul>
     *
     * @param e the mouse event
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseDown = false;
            game.getPlayer().setShootHeld(false);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightMouseDown = false;
            if (grapplingHook != null) {
                grapplingHook.restorePlayerGravity(game.getPlayer());
                grapplingHook.release();
                grapplingHook = null;
            }
        }
    }

    /**
     * Tracks the mouse position when the mouse is moved (no buttons held).
     *
     * @param e the mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseScreenX = e.getX();
        mouseScreenY = e.getY();
    }

    /**
     * Tracks the mouse position when the mouse is dragged (button held).
     *
     * @param e the mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseScreenX = e.getX();
        mouseScreenY = e.getY();
    }

    /** Not used. */
    @Override
    public void mouseClicked(MouseEvent e) {}

    /** Not used. */
    @Override
    public void mouseEntered(MouseEvent e) {}

    /** Not used. */
    @Override
    public void mouseExited(MouseEvent e) {}

    // =========================================================================
    // Wand swap
    // =========================================================================

    /**
     * Attempts to swap the player's active wand with a nearby ground wand
     * in the current level. If a swap occurs, a message is shown on the HUD.
     */
    private void tryWandSwap() {
        Level lv = game.getCurrentLevel();
        Player player = game.getPlayer();
        String msg = lv.trySwapWand(player, game.getSounds(), game.getSpellAnims());
        if (msg != null) game.getHud().showMessage(msg, 1500);
    }
}
