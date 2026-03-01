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
import ui.Menu;
import world.Camera;
import world.Level;
import world.ParticleSystem;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JFrame;

/**
 * GlorpingGame -- the main game class for a Noita-inspired 2D cave explorer.
 */
@SuppressWarnings("serial")
public class GlorpingGame extends GameCore implements MouseListener, MouseMotionListener {

    // =========================================================================
    // Screen constants
    // =========================================================================

    public static final int SCREEN_W = 800;
    public static final int SCREEN_H = 600;

    /** Window resolution (can differ from logical SCREEN_W/H; game scales up) */
    private int windowW = 800;
    private int windowH = 600;

    // =========================================================================
    // Game state
    // =========================================================================

    private GameState gameState = GameState.MENU;
    private int currentLevelIndex = 0;
    private static final int TOTAL_LEVELS = 5;

    // =========================================================================
    // Core subsystems
    // =========================================================================

    private Player       player;
    private Level[]      levels;
    private Camera       camera;
    private ParticleSystem particles;
    private HUD          hud;
    private Menu         menu;
    private SoundManager sounds;
    private MusicPlayer  music;

    // =========================================================================
    // Animations (loaded once, shared across levels)
    // =========================================================================

    // Player
    private Animation animPlayerIdle;
    private Animation animPlayerWalk;
    private Animation animPlayerJump;
    private Animation animPlayerCast;
    private Animation animPlayerDeath;
    private Animation animPlayerWallGrab;

    // Enemies
    private Animation[] animSlime = new Animation[2];
    private Animation animBatUp, animBatDown;
    private Animation animWizIdle, animWizCast;
    private Animation[] animSkeleton = new Animation[2];
    private Animation[] animSpider = new Animation[2];
    private Animation animGhost;
    private Animation[] animFireSpirit = new Animation[2];

    // Projectiles
    private Map<SpellType, Animation> spellAnims = new EnumMap<>(SpellType.class);

    // Pickups
    private Animation animHealthPot, animManaPot, animWandItem, animGoldNugget;
    private Animation animEnemyMissile;

    // Shop wand (random wand offered each level)
    private Wand shopWand = null;

    // NPC
    private Animation animNpc;

    // =========================================================================
    // Parallax background layers
    // =========================================================================

    private Image bgFar, bgMid, bgNear;

    // =========================================================================
    // Input state
    // =========================================================================

    private int mouseScreenX = 0;
    private int mouseScreenY = 0;
    private boolean mouseDown = false;

    // =========================================================================
    // Debug
    // =========================================================================

    private boolean debug = false;

    // Grappling hook
    private GrapplingHook grapplingHook = null;
    private boolean rightMouseDown = false;

    // =========================================================================
    // Settings / cheats
    // =========================================================================

    private boolean godMode      = false;
    private boolean infiniteMana = false;
    private boolean fullscreen   = false;
    private boolean musicEnabled = true;
    private boolean sfxEnabled   = true;
    private boolean showFps      = false;
    private boolean screenShake  = true;
    private GameState preSettingsState = GameState.MENU;
    private String settingsTab = "main"; // "main", "video", "audio", "gameplay"
    private boolean jumpKeyHeld = false;

    // =========================================================================
    // Fullscreen scaling
    // =========================================================================

    /** Off-screen buffer for rendering at fixed 800x600 resolution. */
    private BufferedImage renderBuffer;
    /** Cached graphics context for renderBuffer (avoids per-frame allocation). */
    private Graphics2D renderG;
    /** Actual pixel width of GameCore's internal buffer (matches window). */
    private int bufferW = SCREEN_W;
    /** Actual pixel height of GameCore's internal buffer (matches window). */
    private int bufferH = SCREEN_H;

    // Cached draw objects to avoid per-frame allocation (reduces GC pressure)
    private static final Font FPS_FONT = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font DEBUG_FONT = new Font("Monospaced", Font.PLAIN, 11);
    private static final Color FPS_COLOR = new Color(200, 200, 200, 180);
    private static final Color CROSSHAIR_COLOR = new Color(255, 255, 255, 160);

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

        addMouseListener(this);
        addMouseMotionListener(this);

        renderBuffer = new BufferedImage(SCREEN_W, SCREEN_H, BufferedImage.TYPE_INT_ARGB);
        renderG = renderBuffer.createGraphics();
        renderG.setClip(0, 0, SCREEN_W, SCREEN_H);
        renderG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        loadPlayerAnimations();
        loadEnemyAnimations();
        loadSpellAnimations();
        loadPickupAnimations();
        loadNpcAnimation();
        loadBackgrounds();

        particles = new ParticleSystem();
        camera    = new Camera(SCREEN_W, SCREEN_H);
        hud       = new HUD();
        menu      = new Menu();
        sounds    = new SoundManager();
        music     = new MusicPlayer();

        // 5 cave levels
        levels = new Level[TOTAL_LEVELS];
        for (int i = 0; i < TOTAL_LEVELS; i++) {
            String mapFile = "level" + (i + 1) + ".txt";
            levels[i] = new Level("maps", mapFile, i, particles);
            levels[i].setGoldNuggetAnim(animGoldNugget);
            levels[i].setWandItemAnim(animWandItem);
        }

        player = createPlayer();
        levels[0].populate(player,
                animSlime, animBatUp, animBatDown,
                animWizIdle, animWizCast, animEnemyMissile,
                animSkeleton, animSpider, animGhost, animFireSpirit,
                animHealthPot, animManaPot, animWandItem, spellAnims);

        music.startMusic();
    }

    // =========================================================================
    // Animation loaders
    // =========================================================================

    private void loadPlayerAnimations() {
        animPlayerIdle     = new Animation();
        animPlayerWalk     = new Animation();
        animPlayerJump     = new Animation();
        animPlayerCast     = new Animation();
        animPlayerDeath    = new Animation();
        animPlayerWallGrab = new Animation();

        animPlayerIdle.loadAnimationSeries(    "images/wizard.png", 4, 6, 150, 0, 4);
        animPlayerWalk.loadAnimationSeries(    "images/wizard.png", 4, 6, 120, 4, 4);
        animPlayerJump.loadAnimationSeries(    "images/wizard.png", 4, 6, 180, 8, 4);
        animPlayerCast.loadAnimationSeries(    "images/wizard.png", 4, 6, 100, 12, 4);
        animPlayerDeath.loadAnimationSeries(   "images/wizard.png", 4, 6, 200, 8, 4);
        animPlayerDeath.setLoop(false);
        animPlayerWallGrab.loadAnimationSeries("images/wizard.png", 4, 6, 200, 16, 4);
    }

    private void loadEnemyAnimations() {
        animSlime[0] = new Animation();
        animSlime[1] = new Animation();
        animSlime[0].loadAnimationSeries("images/slime.png", 2, 1, 300, 0, 1);
        animSlime[1].loadAnimationSeries("images/slime.png", 2, 1, 200, 1, 1);

        animBatUp   = new Animation();
        animBatDown = new Animation();
        animBatUp.loadAnimationSeries(  "images/bat.png", 2, 1, 200, 0, 1);
        animBatDown.loadAnimationSeries("images/bat.png", 2, 1, 200, 1, 1);

        animWizIdle = new Animation();
        animWizCast = new Animation();
        animWizIdle.loadAnimationSeries("images/wiz_enemy.png", 2, 1, 400, 0, 1);
        animWizCast.loadAnimationSeries("images/wiz_enemy.png", 2, 1, 150, 1, 1);

        animEnemyMissile = new Animation();
        animEnemyMissile.loadAnimationSeries("images/magic_missile.png", 2, 1, 150, 0, 2);

        animSkeleton[0] = new Animation();
        animSkeleton[1] = new Animation();
        animSkeleton[0].loadAnimationSeries("images/skeleton.png", 2, 1, 300, 0, 1);
        animSkeleton[1].loadAnimationSeries("images/skeleton.png", 2, 1, 200, 1, 1);

        animSpider[0] = new Animation();
        animSpider[1] = new Animation();
        animSpider[0].loadAnimationSeries("images/spider.png", 2, 1, 200, 0, 1);
        animSpider[1].loadAnimationSeries("images/spider.png", 2, 1, 150, 1, 1);

        animGhost = new Animation();
        animGhost.loadAnimationSeries("images/ghost.png", 2, 1, 400, 0, 2);

        animFireSpirit[0] = new Animation();
        animFireSpirit[1] = new Animation();
        animFireSpirit[0].loadAnimationSeries("images/fire_spirit.png", 2, 1, 150, 0, 2);
        animFireSpirit[1].loadAnimationSeries("images/fireball.png", 2, 1, 120, 0, 2);
    }

    private void loadSpellAnimations() {
        for (SpellType t : SpellType.values()) {
            Animation a = new Animation();
            switch (t) {
                case FIREBALL:
                    a.loadAnimationSeries("images/fireball.png",      2, 1, 120, 0, 2); break;
                case LIGHTNING_BOLT:
                    a.loadAnimationSeries("images/lightning.png",     2, 1, 80,  0, 2); break;
                case MAGIC_MISSILE:
                    a.loadAnimationSeries("images/magic_missile.png", 2, 1, 150, 0, 2); break;
                case EXPLODE:
                    a.loadAnimationSeries("images/explosion.png",     4, 1, 100, 0, 4);
                    a.setLoop(false);
                    break;
                default:
                    a.loadAnimationSeries("images/spark.png",         2, 1, 100, 0, 2); break;
            }
            spellAnims.put(t, a);
        }
    }

    private void loadPickupAnimations() {
        animHealthPot = new Animation();
        animHealthPot.addFrame(loadImage("images/health_potion.png"), 1000);

        animManaPot = new Animation();
        animManaPot.addFrame(loadImage("images/mana_potion.png"), 1000);

        animWandItem = new Animation();
        animWandItem.addFrame(loadImage("images/wand_item.png"), 1000);

        animGoldNugget = new Animation();
        animGoldNugget.addFrame(loadImage("images/gold_nugget.png"), 1000);
    }

    private void loadNpcAnimation() {
        animNpc = new Animation();
        animNpc.addFrame(loadImage("images/npc_shopkeeper.png"), 1000);
    }

    private void loadBackgrounds() {
        bgFar  = loadImage("images/bg_far.png");
        bgMid  = loadImage("images/bg_mid.png");
        bgNear = loadImage("images/bg_near.png");
    }

    // =========================================================================
    // Player factory
    // =========================================================================

    private Player createPlayer() {
        Player p = new Player(animPlayerIdle, animPlayerWalk,
                              animPlayerJump, animPlayerCast, animPlayerDeath,
                              animPlayerWallGrab);
        p.addWand(new Wand(WandType.BASIC, spellAnims));
        placePlayerAtSpawn(p, 0);
        return p;
    }

    /** Moves the player to the spawn point of the given level. */
    private void placePlayerAtSpawn(Player p, int levelIdx) {
        Level lv = levels[levelIdx];
        p.setPosition(lv.getSpawnX(), lv.getSpawnY());
        p.setVelocity(0, 0);
        camera.snapTo(p.getX(), p.getY(), lv.getWorldWidth(), lv.getWorldHeight());
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
            case PAUSED:  break;
            default:      break;
        }
    }

    private void updatePlaying(long elapsed) {
        Level lv = levels[currentLevelIndex];

        if (godMode)      player.heal(player.getMaxHealth());
        if (infiniteMana) player.restoreMana(player.getMaxMana());

        // Convert screen mouse to game-buffer coordinates
        int gameMouseX = screenToGameX(mouseScreenX);
        int gameMouseY = screenToGameY(mouseScreenY);

        float worldAimX = gameMouseX - camera.getDrawX();
        float worldAimY = gameMouseY - camera.getDrawY();
        player.setAimTarget(worldAimX, worldAimY);

        if (mouseDown) {
            Projectile p = player.tryShoot(sounds);
            if (p != null) lv.addProjectile(p);
        }

        if (player.shouldPlayFootstep()) {
            sounds.play("footstep");
            particles.spawnFootstepDust(
                    player.getX() + player.getWidth() / 2f,
                    player.getY() + player.getHeight());
        }

        // Update hook projectile flight (before physics so it can attach this frame)
        if (grapplingHook != null && grapplingHook.getState() == GrapplingHook.HookState.FLYING) {
            grapplingHook.update(elapsed, lv.getTileMap(), player);
            if (!grapplingHook.isActive()) { grapplingHook.restorePlayerGravity(player); grapplingHook = null; }
        }

        player.update(elapsed);

        // Apply rope constraint AFTER player movement but BEFORE tile collision.
        // This lets the pendulum set velocity/position, then collision resolution
        // only corrects if the player actually overlaps a tile.
        if (grapplingHook != null && grapplingHook.getState() == GrapplingHook.HookState.ATTACHED) {
            grapplingHook.update(elapsed, lv.getTileMap(), player);
            if (!grapplingHook.isActive()) { grapplingHook.restorePlayerGravity(player); grapplingHook = null; }
        }

        lv.update(elapsed, player, sounds);

        String pickupMsg = lv.consumePickupMessage();
        if (pickupMsg != null) hud.showMessage(pickupMsg, 1500);

        camera.follow(player.getX(), player.getY(),
                      player.getWidth(), player.getHeight(),
                      lv.getWorldWidth(), lv.getWorldHeight());

        particles.update(elapsed);

        if (!player.isAlive()) {
            if (grapplingHook != null) grapplingHook.restorePlayerGravity(player);
            grapplingHook = null;
            gameState = GameState.DEAD;
            music.stopMusic();
        }

        // Check level exit
        if (lv.isExitReached()) {
            advanceLevel();
        }
    }

    // =========================================================================
    // Game loop -- draw
    // =========================================================================

    @Override
    public void draw(Graphics2D bg) {
        // Render everything into the fixed 800x600 buffer
        Graphics2D g = renderG;

        // Feed mouse position to menu (in game-buffer coordinates)
        menu.setMousePos(screenToGameX(mouseScreenX), screenToGameY(mouseScreenY));

        if (gameState != GameState.MENU && gameState != GameState.SETTINGS) {
            drawWorld(g);
        } else if (gameState == GameState.SETTINGS && preSettingsState != GameState.MENU) {
            drawWorld(g);
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
                if (debug) drawDebugInfo(g);
                break;
            case PAUSED:
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawPauseMenu(g, SCREEN_W, SCREEN_H);
                break;
            case SETTINGS:
                menu.drawSettingsMenu(g, SCREEN_W, SCREEN_H, settingsTab,
                        godMode, infiniteMana, fullscreen, debug, musicEnabled,
                        sfxEnabled, showFps, screenShake, windowW, windowH);
                break;
            case DEAD:
                menu.drawDeathScreen(g, SCREEN_W, SCREEN_H, player.getGold());
                break;
            case LEVEL_COMPLETE:
                drawWorld(g);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawLevelCompleteScreen(g, SCREEN_W, SCREEN_H,
                        false, player.getGold());
                break;
            case WIN:
                drawWorld(g);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawLevelCompleteScreen(g, SCREEN_W, SCREEN_H,
                        true, player.getGold());
                break;
            case SHOP:
                drawWorld(g);
                hud.draw(g, player, SCREEN_W, SCREEN_H);
                menu.drawShopMenu(g, SCREEN_W, SCREEN_H,
                        player.getGold(), player.getMaxHealth(), player.getMaxMana(),
                        shopWand, player.getWands().size(),
                        player.getActiveWand() != null ? player.getActiveWand().getDisplayName() : "");
                break;
            default:
                break;
        }

        // FPS overlay (drawn on top of everything)
        if (showFps) drawFpsCounter(g);

        // Scale the 800x600 renderBuffer into GameCore's actual buffer (bg)
        // In windowed mode bufferW/H == SCREEN_W/H so this is a 1:1 copy.
        // In fullscreen bufferW/H == screen resolution so it scales up.
        bg.setColor(Color.BLACK);
        bg.fillRect(0, 0, bufferW, bufferH);

        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale = Math.min(scaleX, scaleY);
        int drawW = (int)(SCREEN_W * scale);
        int drawH = (int)(SCREEN_H * scale);
        int drawX = (bufferW - drawW) / 2;
        int drawY = (bufferH - drawH) / 2;

        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        bg.drawImage(renderBuffer, drawX, drawY, drawW, drawH, null);
    }

    private void drawWorld(Graphics2D g) {
        int xOff = camera.getDrawX();
        int yOff = camera.getDrawY();

        drawParallaxLayer(g, bgFar, xOff, yOff, 0.08f, SCREEN_W, SCREEN_H);
        drawParallaxLayer(g, bgMid, xOff, yOff, 0.28f, SCREEN_W, SCREEN_H);
        drawParallaxLayer(g, bgNear, xOff, yOff, 0.55f, SCREEN_W, SCREEN_H);

        levels[currentLevelIndex].draw(g, xOff, yOff, debug);

        particles.draw(g, xOff, yOff);

        player.setOffsets(xOff, yOff);
        player.drawTransformed(g);

        // Draw grappling hook rope
        if (grapplingHook != null && grapplingHook.isActive()) {
            grapplingHook.draw(g, xOff, yOff, player);
        }

        drawCrosshair(g);
    }

    private void drawParallaxLayer(Graphics2D g, Image img, int xOff, int yOff,
                                    float speed, int sw, int sh) {
        if (img == null) return;
        int imgW = img.getWidth(null);
        int imgH = img.getHeight(null);
        if (imgW <= 0 || imgH <= 0) return;

        int layerX = (int)(xOff * speed);
        layerX = layerX % imgW;
        if (layerX > 0) layerX -= imgW;

        int layerY = (int)(yOff * speed * 0.3f);
        layerY = layerY % imgH;
        if (layerY > 0) layerY -= imgH;

        for (int dy = layerY; dy < sh; dy += imgH) {
            for (int dx = layerX; dx < sw; dx += imgW) {
                g.drawImage(img, dx, dy, null);
            }
        }
    }

    private void drawCrosshair(Graphics2D g) {
        int gx = screenToGameX(mouseScreenX);
        int gy = screenToGameY(mouseScreenY);
        g.setColor(CROSSHAIR_COLOR);
        int s = 6;
        g.drawLine(gx - s, gy, gx + s, gy);
        g.drawLine(gx, gy - s, gx, gy + s);
        g.drawOval(gx - 3, gy - 3, 6, 6);
    }

    private void drawDebugInfo(Graphics2D g) {
        g.setFont(DEBUG_FONT);
        g.setColor(Color.YELLOW);
        int x = SCREEN_W - 180;
        Level lv = levels[currentLevelIndex];
        g.drawString(String.format("FPS: %.0f", getFPS()), x, 20);
        g.drawString(String.format("Player: %.0f,%.0f",
                player.getX(), player.getY()), x, 34);
        g.drawString("Enemies: " + lv.getEnemies().size(), x, 48);
        g.drawString("Particles: " + particles.getCount(), x, 62);
        g.drawString("Ground: " + player.isOnGround(), x, 76);
        g.drawString("Level: " + (currentLevelIndex + 1) + "/" + TOTAL_LEVELS, x, 90);

        g.setColor(Color.RED);
        int ox = camera.getDrawX(), oy = camera.getDrawY();
        g.drawRect((int)player.getX() + ox, (int)player.getY() + oy,
                   player.getWidth(), player.getHeight());
    }

    private void drawFpsCounter(Graphics2D g) {
        g.setFont(FPS_FONT);
        g.setColor(FPS_COLOR);
        g.drawString(String.format("FPS: %.0f", getFPS()), SCREEN_W - 90, 20);
    }

    // =========================================================================
    // Fullscreen coordinate mapping
    // =========================================================================

    /**
     * Converts a raw mouse X from the JFrame to the 800x600 game coordinate.
     * In windowed mode this is a direct pass-through.
     * In fullscreen the scaled/centred draw area is accounted for.
     */
    private int screenToGameX(int sx) {
        if (bufferW == SCREEN_W && bufferH == SCREEN_H) return sx;
        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale = Math.min(scaleX, scaleY);
        int drawW = (int)(SCREEN_W * scale);
        int drawX = (bufferW - drawW) / 2;
        return (int)((sx - drawX) / scale);
    }

    /** Converts a raw mouse Y from the JFrame to the 800x600 game coordinate. */
    private int screenToGameY(int sy) {
        if (bufferW == SCREEN_W && bufferH == SCREEN_H) return sy;
        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale = Math.min(scaleX, scaleY);
        int drawH = (int)(SCREEN_H * scale);
        int drawY = (bufferH - drawH) / 2;
        return (int)((sy - drawY) / scale);
    }

    // =========================================================================
    // Level transition
    // =========================================================================

    /** Advances to the next level, showing shop between levels. */
    private void advanceLevel() {
        if (grapplingHook != null) grapplingHook.restorePlayerGravity(player);
        grapplingHook = null;
        currentLevelIndex++;
        if (currentLevelIndex >= TOTAL_LEVELS) {
            currentLevelIndex = TOTAL_LEVELS - 1; // keep valid for drawWorld
            gameState = GameState.WIN;
            music.stopMusic();
            sounds.playWithEcho("pickup");
            return;
        }

        // Show shop between levels — pick a random wand to sell
        WandType[] allWands = WandType.values();
        WandType shopType = allWands[(int)(Math.random() * allWands.length)];
        shopWand = new Wand(shopType, spellAnims);
        gameState = GameState.SHOP;
        sounds.playWithEcho("pickup");
    }

    /** Called when the player clicks "Continue" in the shop. */
    private void continueFromShop() {
        particles.clear();
        Level next = levels[currentLevelIndex];
        next.reset();
        next.regenerate();
        next.setGoldNuggetAnim(animGoldNugget);
        next.setWandItemAnim(animWandItem);
        next.populate(player,
                animSlime, animBatUp, animBatDown,
                animWizIdle, animWizCast, animEnemyMissile,
                animSkeleton, animSpider, animGhost, animFireSpirit,
                animHealthPot, animManaPot, animWandItem, spellAnims);

        placePlayerAtSpawn(player, currentLevelIndex);
        hud.showMessage("Level " + (currentLevelIndex + 1) + "!", 2000);
        music.stopMusic();
        if (musicEnabled) music.startMusic();
        gameState = GameState.PLAYING;
    }

    /**
     * Fully restarts the game from level 1 with freshly generated terrain.
     */
    private void restartGame() {
        currentLevelIndex = 0;
        particles.clear();

        for (Level lv : levels) {
            lv.reset();
            lv.regenerate();
        }

        player = createPlayer();
        for (Level lv : levels) {
            lv.setGoldNuggetAnim(animGoldNugget);
            lv.setWandItemAnim(animWandItem);
        }
        levels[0].populate(player,
                animSlime, animBatUp, animBatDown,
                animWizIdle, animWizCast, animEnemyMissile,
                animSkeleton, animSpider, animGhost, animFireSpirit,
                animHealthPot, animManaPot, animWandItem, spellAnims);
        music.stopMusic();
        if (musicEnabled) music.startMusic();
        jumpKeyHeld = false;
        gameState = GameState.PLAYING;
    }

    // =========================================================================
    // Keyboard input
    // =========================================================================

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {
            handleEscape();
            return;
        }

        switch (gameState) {
            case PLAYING:
                handlePlayingKeyPress(key);
                break;
            default:
                break;
        }
    }

    private void handlePlayingKeyPress(int key) {
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
                    sounds.play("jump");
                    jumpKeyHeld = true;
                }
                break;
            case KeyEvent.VK_P:
                gameState = GameState.PAUSED; break;
            case KeyEvent.VK_1: player.selectWand(0); break;
            case KeyEvent.VK_2: player.selectWand(1); break;
            case KeyEvent.VK_3: player.selectWand(2); break;
            case KeyEvent.VK_4: player.selectWand(3); break;
            case KeyEvent.VK_Q: player.nextWand();    break;
            case KeyEvent.VK_E: tryWandSwap();        break;
        }
    }

    /** Attempts to swap the player's active wand with a nearby ground wand. */
    private void tryWandSwap() {
        Level lv = levels[currentLevelIndex];
        String msg = lv.trySwapWand(player, sounds, spellAnims);
        if (msg != null) hud.showMessage(msg, 1500);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) return;
        switch (key) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:
                player.setMoveLeft(false);  break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT:
                player.setMoveRight(false); break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_UP:
                jumpKeyHeld = false; break;
        }
    }

    private void handleEscape() {
        switch (gameState) {
            case PLAYING:
                gameState = GameState.PAUSED; break;
            case PAUSED:
                gameState = GameState.PLAYING; break;
            case SETTINGS:
                gameState = preSettingsState; break;
            case DEAD:
            case WIN:
            case LEVEL_COMPLETE:
                gameState = GameState.MENU;
                music.stopMusic(); break;
            case MENU:
                break;
            default: break;
        }
    }

    // =========================================================================
    // Mouse input
    // =========================================================================

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (gameState == GameState.PLAYING) {
                mouseDown = true;
                player.setShootHeld(true);
            } else {
                int gx = screenToGameX(e.getX());
                int gy = screenToGameY(e.getY());
                String action = menu.handleClick(gx, gy);
                if (action != null) {
                    handleMenuAction(action);
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (gameState == GameState.PLAYING) {
                rightMouseDown = true;
                // Fire grappling hook toward mouse
                int gx = screenToGameX(e.getX());
                int gy = screenToGameY(e.getY());
                float worldAimX = gx - camera.getDrawX();
                float worldAimY = gy - camera.getDrawY();
                float px = player.getX() + player.getWidth() / 2f;
                float py = player.getY() + player.getHeight() / 2f;
                grapplingHook = new GrapplingHook(px, py, worldAimX, worldAimY);
                sounds.play("shoot");
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseDown = false;
            player.setShootHeld(false);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightMouseDown = false;
            if (grapplingHook != null) {
                grapplingHook.restorePlayerGravity(player);
                grapplingHook.release();
                grapplingHook = null;
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseScreenX = e.getX();
        mouseScreenY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseScreenX = e.getX();
        mouseScreenY = e.getY();
    }

    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}

    // =========================================================================
    // Menu action handler
    // =========================================================================

    private void handleMenuAction(String action) {
        switch (action) {
            // Main menu
            case "start":
                restartGame(); break;
            case "settings":
                preSettingsState = gameState == GameState.PAUSED ? GameState.PAUSED : GameState.MENU;
                settingsTab = "video";
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
                advanceLevel(); break;

            // Settings tabs
            case "tab_video":
                settingsTab = "video"; break;
            case "tab_audio":
                settingsTab = "audio"; break;
            case "tab_gameplay":
                settingsTab = "gameplay"; break;

            // Video settings
            case "toggle_fullscreen":
                toggleFullscreen(); break;
            case "toggle_fps":
                showFps = !showFps; break;
            case "res_800_600":
                changeResolution(800, 600); break;
            case "res_1024_768":
                changeResolution(1024, 768); break;
            case "res_1280_720":
                changeResolution(1280, 720); break;
            case "res_1920_1080":
                changeResolution(1920, 1080); break;

            // Audio settings
            case "toggle_music":
                musicEnabled = !musicEnabled;
                if (musicEnabled) music.startMusic();
                else music.stopMusic();
                break;
            case "toggle_sfx":
                sfxEnabled = !sfxEnabled;
                sounds.setEnabled(sfxEnabled);
                break;

            // Gameplay settings
            case "toggle_debug":
                debug = !debug; break;
            case "toggle_god":
                godMode = !godMode; break;
            case "toggle_mana":
                infiniteMana = !infiniteMana; break;
            case "toggle_shake":
                screenShake = !screenShake; break;

            case "back":
                settingsTab = "video"; // reset to first tab
                gameState = preSettingsState; break;

            // Shop actions
            case "buy_hp":
                buyUpgrade("hp"); break;
            case "buy_mp":
                buyUpgrade("mp"); break;
            case "buy_heal":
                buyUpgrade("heal"); break;
            case "buy_wand":
                buyShopWand(); break;
            case "shop_continue":
                continueFromShop(); break;
        }
    }

    // =========================================================================
    // Shop upgrades
    // =========================================================================

    private void buyUpgrade(String type) {
        switch (type) {
            case "hp": {
                int cost = 20 + (player.getMaxHealth() - 100) / 10 * 5;
                if (player.spendGold(cost)) {
                    player.increaseMaxHealth(20);
                    hud.showMessage("+20 Max HP!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
            case "mp": {
                int cost = 15 + (player.getMaxMana() - 50) / 10 * 5;
                if (player.spendGold(cost)) {
                    player.increaseMaxMana(20);
                    hud.showMessage("+20 Max Mana!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
            case "heal": {
                int cost = 10;
                if (player.spendGold(cost)) {
                    player.heal(player.getMaxHealth());
                    player.restoreMana(player.getMaxMana());
                    hud.showMessage("Fully Healed!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
        }
    }

    /** Buys the random wand offered in the shop. */
    private void buyShopWand() {
        if (shopWand == null) return;
        int cost = 15 + shopWand.getManaCost() * 2;
        if (player.spendGold(cost)) {
            if (player.getWands().size() >= 4) {
                // Inventory full — swap out the currently active wand
                Wand old = player.swapActiveWand(shopWand);
                hud.showMessage("Swapped " + old.getDisplayName() + " for "
                        + shopWand.getDisplayName() + "!", 2000);
            } else {
                player.addWand(shopWand);
                hud.showMessage("Bought " + shopWand.getDisplayName() + "!", 1500);
            }
            sounds.play("pickup");
            shopWand = null; // can only buy once
        }
    }

    // =========================================================================
    // Resolution / Fullscreen
    // =========================================================================

    private void changeResolution(int w, int h) {
        windowW = w;
        windowH = h;
        if (fullscreen) {
            // Exit fullscreen and apply the new windowed resolution
            toggleFullscreen();
        } else {
            setSize(w, h);
            setLocationRelativeTo(null);
            resizeGameCoreBuffer(w, h);
        }
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        GraphicsDevice gd =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getDefaultScreenDevice();
        if (fullscreen && gd.isFullScreenSupported()) {
            dispose();
            setUndecorated(true);
            gd.setFullScreenWindow(this);
            setVisible(true);
            // Resize GameCore's internal buffer to match screen resolution
            resizeGameCoreBuffer(getWidth(), getHeight());
        } else {
            gd.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setSize(windowW, windowH);
            setLocationRelativeTo(null);
            setVisible(true);
            fullscreen = false;
            // Restore GameCore's buffer to the chosen window resolution
            resizeGameCoreBuffer(windowW, windowH);
        }
    }

    /**
     * Uses reflection to replace GameCore's private buffer and bg fields
     * so the internal back-buffer matches the current window size.
     */
    private void resizeGameCoreBuffer(int w, int h) {
        bufferW = w;
        bufferH = h;
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
