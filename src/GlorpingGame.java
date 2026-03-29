import entities.*;
import game2D.*;
import projectiles.Projectile;
import sound.MusicPlayer;
import sound.SoundManager;
import spells.SpellType;
import spells.Wand;
import spells.WandType;
import ui.GameState;
import ui.HUD;
import ui.InputHandler;
import ui.Menu;
import ui.Renderer;
import ui.SettingsManager;
import utils.AnimationManager;
import world.Camera;
import world.Level;
import world.LevelManager;
import world.ParticleSystem;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;
import javax.swing.JFrame;

/**
 * GlorpingGame -- the main game class for a Noita-inspired 2D cave explorer.
 *
 * <p>Acts as a thin orchestrator, delegating to specialised managers:</p>
 * <ul>
 *   <li>{@link AnimationManager} -- loading and storing all animations</li>
 *   <li>{@link InputHandler} -- keyboard and mouse input</li>
 *   <li>{@link SettingsManager} -- settings, cheats, resolution</li>
 *   <li>{@link LevelManager} -- level lifecycle, shop, upgrades</li>
 *   <li>{@link Renderer} -- all drawing and coordinate conversion</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class GlorpingGame extends GameCore implements InputHandler.GameActions {

    // =========================================================================
    // Screen constants
    // =========================================================================

    public static final int SCREEN_W = 800;
    public static final int SCREEN_H = 600;

    // =========================================================================
    // Game state
    // =========================================================================

    private GameState gameState = GameState.MENU;

    // =========================================================================
    // Core subsystems
    // =========================================================================

    private Player           player;
    private Camera           camera;
    private ParticleSystem   particles;
    private HUD              hud;
    private Menu             menu;
    private SoundManager     sounds;
    private MusicPlayer      music;

    // =========================================================================
    // Extracted managers
    // =========================================================================

    private AnimationManager anims;
    private InputHandler     input;
    private SettingsManager  settings;
    private LevelManager     levelMgr;
    private Renderer         renderer;

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) {
        GlorpingGame game = new GlorpingGame();
        game.init();
        game.run(false, SCREEN_W, SCREEN_H);
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    public void init() {
        setTitle("Glorping");
        setSize(SCREEN_W, SCREEN_H);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // Managers
        renderer = new Renderer();
        anims    = new AnimationManager(this::loadImage);
        anims.loadAll();

        input    = new InputHandler(this);
        settings = new SettingsManager(this::applyResize);

        addMouseListener(input);
        addMouseMotionListener(input);

        particles = new ParticleSystem();
        camera    = new Camera(SCREEN_W, SCREEN_H);
        hud       = new HUD();
        menu      = new Menu();
        sounds    = new SoundManager();
        music     = new MusicPlayer();

        levelMgr = new LevelManager(particles);
        levelMgr.initLevels(anims.getGoldNugget(), anims.getWandItem());

        player = createPlayer();
        levelMgr.populateCurrentLevel(player,
                anims.getSlime(), anims.getBatUp(), anims.getBatDown(),
                anims.getWizIdle(), anims.getWizCast(), anims.getEnemyMissile(),
                anims.getSkeleton(), anims.getSpider(), anims.getGhost(),
                anims.getFireSpirit(),
                anims.getHealthPot(), anims.getManaPot(), anims.getWandItem(),
                anims.getSpellAnims());

        renderer.setBackgrounds(anims.getBgFar(), anims.getBgMid(), anims.getBgNear());
        music.startMusic();
    }

    // =========================================================================
    // Player factory
    // =========================================================================

    private Player createPlayer() {
        Player p = new Player(anims.getPlayerIdle(), anims.getPlayerWalk(),
                              anims.getPlayerJump(), anims.getPlayerCast(),
                              anims.getPlayerDeath(), anims.getPlayerWallGrab());
        p.addWand(new Wand(WandType.BASIC, anims.getSpellAnims()));
        levelMgr.placePlayerAtSpawn(p, camera);
        return p;
    }

    // =========================================================================
    // Game loop -- update
    // =========================================================================

    @Override
    public void update(long elapsed) {
        menu.update(elapsed);
        hud.update(elapsed);

        switch (gameState) {
            case PLAYING: updatePlaying(elapsed); break;
            default: break;
        }
    }

    private void updatePlaying(long elapsed) {
        Level lv = levelMgr.getCurrentLevel();

        if (settings.isGodMode())      player.heal(player.getMaxHealth());
        if (settings.isInfiniteMana()) player.restoreMana(player.getMaxMana());

        // Convert screen mouse to game-buffer coordinates
        int gameMouseX = renderer.screenToGameX(input.getMouseScreenX());
        int gameMouseY = renderer.screenToGameY(input.getMouseScreenY());

        float worldAimX = gameMouseX - camera.getDrawX();
        float worldAimY = gameMouseY - camera.getDrawY();
        player.setAimTarget(worldAimX, worldAimY);

        if (input.isMouseDown()) {
            Projectile p = player.tryShoot(sounds);
            if (p != null) lv.addProjectile(p);
        }

        if (player.shouldPlayFootstep()) {
            sounds.play("footstep");
            particles.spawnFootstepDust(
                    player.getX() + player.getWidth() / 2f,
                    player.getY() + player.getHeight());
        }

        // Update grappling hook
        GrapplingHook hook = input.getGrapplingHook();
        if (hook != null && hook.getState() == GrapplingHook.HookState.FLYING) {
            hook.update(elapsed, lv.getTileMap(), player);
            if (!hook.isActive()) { hook.restorePlayerGravity(player); input.setGrapplingHook(null); }
        }

        player.update(elapsed);

        // Rope constraint after player movement but before tile collision
        hook = input.getGrapplingHook();
        if (hook != null && hook.getState() == GrapplingHook.HookState.ATTACHED) {
            hook.update(elapsed, lv.getTileMap(), player);
            if (!hook.isActive()) { hook.restorePlayerGravity(player); input.setGrapplingHook(null); }
        }

        lv.update(elapsed, player, sounds);

        String pickupMsg = lv.consumePickupMessage();
        if (pickupMsg != null) hud.showMessage(pickupMsg, 1500);

        camera.follow(player.getX(), player.getY(),
                      player.getWidth(), player.getHeight(),
                      lv.getWorldWidth(), lv.getWorldHeight());

        particles.update(elapsed);

        if (!player.isAlive()) {
            GrapplingHook h = input.getGrapplingHook();
            if (h != null) h.restorePlayerGravity(player);
            input.setGrapplingHook(null);
            gameState = GameState.DEAD;
            music.stopMusic();
        }

        if (lv.isExitReached()) {
            GrapplingHook h = input.getGrapplingHook();
            if (h != null) h.restorePlayerGravity(player);
            input.setGrapplingHook(null);
            gameState = levelMgr.advanceLevel(sounds, music, anims.getSpellAnims());
        }
    }

    // =========================================================================
    // Game loop -- draw
    // =========================================================================

    @Override
    public void draw(Graphics2D bg) {
        Graphics2D g = renderer.getRenderGraphics();

        // Feed mouse position to menu (in game-buffer coordinates)
        menu.setMousePos(renderer.screenToGameX(input.getMouseScreenX()),
                         renderer.screenToGameY(input.getMouseScreenY()));

        int gameMouseX = renderer.screenToGameX(input.getMouseScreenX());
        int gameMouseY = renderer.screenToGameY(input.getMouseScreenY());
        boolean debug = settings.isDebug();

        if (gameState != GameState.MENU && gameState != GameState.SETTINGS) {
            renderer.drawWorld(g, levelMgr.getCurrentLevel(), player, camera,
                    particles, input.getGrapplingHook(), gameMouseX, gameMouseY, debug);
        } else if (gameState == GameState.SETTINGS
                   && settings.getPreSettingsState() != GameState.MENU) {
            renderer.drawWorld(g, levelMgr.getCurrentLevel(), player, camera,
                    particles, input.getGrapplingHook(), gameMouseX, gameMouseY, debug);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, SCREEN_W, SCREEN_H);
        }

        switch (gameState) {
            case MENU:
                menu.drawMainMenu(g, SCREEN_W, SCREEN_H);
                break;
            case PLAYING:
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                if (debug) renderer.drawDebugInfo(g, player, levelMgr.getCurrentLevel(),
                        camera, particles, levelMgr.getCurrentLevelIndex(),
                        levelMgr.getTotalLevels(), getFPS());
                break;
            case PAUSED:
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawPauseMenu(g, SCREEN_W, SCREEN_H);
                break;
            case SETTINGS:
                menu.drawSettingsMenu(g, SCREEN_W, SCREEN_H, settings.getSettingsTab(),
                        settings.isGodMode(), settings.isInfiniteMana(),
                        settings.isFullscreen(), settings.isDebug(),
                        settings.isMusicEnabled(), settings.isSfxEnabled(),
                        settings.isShowFps(), settings.isScreenShake(),
                        settings.getWindowW(), settings.getWindowH(),
                        settings.getPreSettingsState() == GameState.PAUSED);
                break;
            case DEAD:
                menu.drawDeathScreen(g, SCREEN_W, SCREEN_H, player.getGold());
                break;
            case LEVEL_COMPLETE:
                renderer.drawWorld(g, levelMgr.getCurrentLevel(), player, camera,
                        particles, input.getGrapplingHook(), gameMouseX, gameMouseY, debug);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawLevelCompleteScreen(g, SCREEN_W, SCREEN_H, false, player.getGold());
                break;
            case WIN:
                renderer.drawWorld(g, levelMgr.getCurrentLevel(), player, camera,
                        particles, input.getGrapplingHook(), gameMouseX, gameMouseY, debug);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawLevelCompleteScreen(g, SCREEN_W, SCREEN_H, true, player.getGold());
                break;
            case SHOP:
                renderer.drawWorld(g, levelMgr.getCurrentLevel(), player, camera,
                        particles, input.getGrapplingHook(), gameMouseX, gameMouseY, debug);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                Wand sw = levelMgr.getShopWand();
                menu.drawShopMenu(g, SCREEN_W, SCREEN_H,
                        player.getGold(), player.getMaxHealth(), player.getMaxMana(),
                        sw, player.getWands().size(),
                        player.getActiveWand() != null ? player.getActiveWand().getDisplayName() : "");
                break;
            default:
                break;
        }

        if (settings.isShowFps()) renderer.drawFpsCounter(g, getFPS());

        // Scale the 800x600 buffer into GameCore's actual buffer
        renderer.scaleToScreen(bg);
    }

    // =========================================================================
    // Keyboard input (forwarded to InputHandler)
    // =========================================================================

    @Override
    public void keyPressed(KeyEvent e)  { input.keyPressed(e); }

    @Override
    public void keyReleased(KeyEvent e) { input.keyReleased(e); }

    // =========================================================================
    // InputHandler.GameActions implementation
    // =========================================================================

    @Override public void setGameState(GameState state)        { this.gameState = state; }
    @Override public GameState getGameState()                  { return gameState; }
    @Override public Player getPlayer()                        { return player; }
    @Override public Level getCurrentLevel()                   { return levelMgr.getCurrentLevel(); }
    @Override public Camera getCamera()                        { return camera; }
    @Override public SoundManager getSounds()                  { return sounds; }
    @Override public HUD getHud()                              { return hud; }
    @Override public Menu getMenu()                            { return menu; }
    @Override public Map<SpellType, Animation> getSpellAnims() { return anims.getSpellAnims(); }
    @Override public int screenToGameX(int sx)                 { return renderer.screenToGameX(sx); }
    @Override public int screenToGameY(int sy)                 { return renderer.screenToGameY(sy); }

    // =========================================================================
    // Menu action handler
    // =========================================================================

    @Override
    public void handleMenuAction(String action) {
        // Try settings actions first
        GameState settingsResult = settings.handleSettingsAction(action, sounds, music);
        if (settingsResult != null) {
            gameState = settingsResult;
            return;
        }

        switch (action) {
            // Main menu
            case "start":
                restartGame(); break;
            case "settings":
                settings.setPreSettingsState(
                        gameState == GameState.PAUSED ? GameState.PAUSED : GameState.MENU);
                settings.setSettingsTab("video");
                gameState = GameState.SETTINGS; break;
            case "quit":
                stop(); break;

            // Pause menu
            case "resume":
                gameState = GameState.PLAYING; break;
            case "main_menu":
                gameState = GameState.MENU;
                music.stopMusic(); break;

            // Death / Win / Level Complete
            case "restart":
                restartGame(); break;
            case "next_level":
                GrapplingHook h = input.getGrapplingHook();
                if (h != null) h.restorePlayerGravity(player);
                input.setGrapplingHook(null);
                gameState = levelMgr.advanceLevel(sounds, music, anims.getSpellAnims());
                break;

            // Shop actions
            case "buy_hp":
                levelMgr.buyUpgrade("hp", player, hud, sounds); break;
            case "buy_mp":
                levelMgr.buyUpgrade("mp", player, hud, sounds); break;
            case "buy_heal":
                levelMgr.buyUpgrade("heal", player, hud, sounds); break;
            case "buy_wand":
                levelMgr.buyShopWand(player, hud, sounds); break;
            case "shop_continue":
                continueFromShop(); break;
        }
    }

    // =========================================================================
    // Level transitions
    // =========================================================================

    private void continueFromShop() {
        levelMgr.continueFromShop(player, particles,
                anims.getGoldNugget(), anims.getWandItem(),
                anims.getSlime(), anims.getBatUp(), anims.getBatDown(),
                anims.getWizIdle(), anims.getWizCast(), anims.getEnemyMissile(),
                anims.getSkeleton(), anims.getSpider(), anims.getGhost(),
                anims.getFireSpirit(),
                anims.getHealthPot(), anims.getManaPot(), anims.getWandItem(),
                anims.getSpellAnims(), hud, music, settings.isMusicEnabled());
        levelMgr.placePlayerAtSpawn(player, camera);
        gameState = GameState.PLAYING;
    }

    private void restartGame() {
        player = createPlayer();
        levelMgr.restartGame(player, particles,
                anims.getGoldNugget(), anims.getWandItem(),
                anims.getSlime(), anims.getBatUp(), anims.getBatDown(),
                anims.getWizIdle(), anims.getWizCast(), anims.getEnemyMissile(),
                anims.getSkeleton(), anims.getSpider(), anims.getGhost(),
                anims.getFireSpirit(),
                anims.getHealthPot(), anims.getManaPot(), anims.getWandItem(),
                anims.getSpellAnims(), music, settings.isMusicEnabled());
        levelMgr.placePlayerAtSpawn(player, camera);
        input.setJumpKeyHeld(false);
        gameState = GameState.PLAYING;
    }

    // =========================================================================
    // Resolution / Fullscreen (SettingsManager.ResizeCallback)
    // =========================================================================

    private void applyResize(int w, int h, boolean fullscreen) {
        if (fullscreen) {
            // Windowed borderless — undecorated window covering the whole screen
            Rectangle screenBounds =
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                                       .getDefaultScreenDevice()
                                       .getDefaultConfiguration().getBounds();
            dispose();
            setUndecorated(true);
            setBounds(screenBounds);
            setVisible(true);
            resizeGameCoreBuffer(screenBounds.width, screenBounds.height);
        } else {
            dispose();
            setUndecorated(false);
            setSize(w, h);
            setLocationRelativeTo(null);
            setVisible(true);
            settings.setFullscreen(false);
            resizeGameCoreBuffer(w, h);
        }
        // Re-attach mouse listeners lost when dispose() destroyed the native peer
        addMouseListener(input);
        addMouseMotionListener(input);
    }

    /**
     * Uses reflection to replace GameCore's private buffer and bg fields
     * so the internal back-buffer matches the current window size.
     */
    private void resizeGameCoreBuffer(int w, int h) {
        renderer.resizeBuffer(w, h);
        try {
            Field bufField = GameCore.class.getDeclaredField("buffer");
            Field bgField  = GameCore.class.getDeclaredField("bg");
            bufField.setAccessible(true);
            bgField.setAccessible(true);

            // Dispose old graphics to prevent native resource leak
            Graphics2D oldBg = (Graphics2D) bgField.get(this);
            if (oldBg != null) oldBg.dispose();

            BufferedImage newBuf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D newBg = newBuf.createGraphics();
            newBg.setClip(0, 0, w, h);

            bufField.set(this, newBuf);
            bgField.set(this, newBg);
        } catch (Exception e) {
            System.err.println("Failed to resize game buffer: " + e);
        }
    }
}
