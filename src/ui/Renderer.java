package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import entities.Player;
import entities.GrapplingHook;
import world.Camera;
import world.Level;
import world.ParticleSystem;

/**
 * Centralises all rendering and drawing logic for the game.
 * Manages a fixed 800x600 render buffer that is scaled to fit the
 * actual screen/window size, handling coordinate conversion between
 * screen-space and game-space.
 */
public class Renderer {

    // =========================================================================
    // Constants
    // =========================================================================

    /** Fixed internal render width. */
    public static final int SCREEN_W = 800;

    /** Fixed internal render height. */
    public static final int SCREEN_H = 600;

    /** Font used for the FPS counter overlay. */
    private static final Font FPS_FONT = new Font("Monospaced", Font.PLAIN, 12);

    /** Font used for the debug information overlay. */
    private static final Font DEBUG_FONT = new Font("Monospaced", Font.PLAIN, 11);

    /** Semi-transparent colour for the FPS counter text. */
    private static final Color FPS_COLOR = new Color(200, 200, 200, 180);

    /** Semi-transparent colour for the aiming crosshair. */
    private static final Color CROSSHAIR_COLOR = new Color(255, 255, 255, 160);

    // =========================================================================
    // Fields
    // =========================================================================

    /** Off-screen buffer at the fixed internal resolution. */
    private BufferedImage renderBuffer;

    /** Graphics context for {@link #renderBuffer}. */
    private Graphics2D renderG;

    /** Current actual screen/window width (may differ from SCREEN_W in fullscreen). */
    private int bufferW = SCREEN_W;

    /** Current actual screen/window height (may differ from SCREEN_H in fullscreen). */
    private int bufferH = SCREEN_H;

    /** Far parallax background layer. */
    private Image bgFar;

    /** Mid parallax background layer. */
    private Image bgMid;

    /** Near parallax background layer. */
    private Image bgNear;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates the renderer, initialising the 800x600 render buffer with
     * antialiasing enabled.
     */
    public Renderer() {
        renderBuffer = new BufferedImage(SCREEN_W, SCREEN_H, BufferedImage.TYPE_INT_ARGB);
        renderG = renderBuffer.createGraphics();
        renderG.setClip(0, 0, SCREEN_W, SCREEN_H);
        renderG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets the three parallax background layers.
     *
     * @param far  the furthest (slowest-scrolling) layer
     * @param mid  the middle layer
     * @param near the nearest (fastest-scrolling) layer
     */
    public void setBackgrounds(Image far, Image mid, Image near) {
        this.bgFar  = far;
        this.bgMid  = mid;
        this.bgNear = near;
    }

    // =========================================================================
    // Buffer access
    // =========================================================================

    /**
     * Returns the Graphics2D context for the fixed-size render buffer.
     * All game drawing should target this graphics object.
     *
     * @return the 800x600 buffer graphics context
     */
    public Graphics2D getRenderGraphics() {
        return renderG;
    }

    // =========================================================================
    // Scaling / presentation
    // =========================================================================

    /**
     * Scales the internal 800x600 render buffer into the actual screen buffer,
     * maintaining aspect ratio and centring with black letterbox bars.
     *
     * @param bg the graphics context of the actual screen/window buffer
     */
    public void scaleToScreen(Graphics2D bg) {
        bg.setColor(Color.BLACK);
        bg.fillRect(0, 0, bufferW, bufferH);

        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale  = Math.min(scaleX, scaleY);

        int drawW = (int)(SCREEN_W * scale);
        int drawH = (int)(SCREEN_H * scale);
        int drawX = (bufferW - drawW) / 2;
        int drawY = (bufferH - drawH) / 2;

        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        bg.drawImage(renderBuffer, drawX, drawY, drawW, drawH, null);
    }

    // =========================================================================
    // World drawing
    // =========================================================================

    /**
     * Draws the complete game world: parallax backgrounds, level tiles,
     * particles, the player, grappling hook, and aiming crosshair.
     *
     * @param g          graphics context (the 800x600 render buffer)
     * @param level      the current level to draw
     * @param player     the player entity
     * @param camera     the camera providing draw offsets
     * @param particles  the particle system
     * @param hook       the grappling hook (may be null)
     * @param mouseGameX mouse X in game coordinates
     * @param mouseGameY mouse Y in game coordinates
     * @param debug      whether to draw debug overlays on the level
     */
    public void drawWorld(Graphics2D g, Level level, Player player, Camera camera,
                          ParticleSystem particles, GrapplingHook hook,
                          int mouseGameX, int mouseGameY, boolean debug) {

        int xOff = camera.getDrawX();
        int yOff = camera.getDrawY();

        // Parallax backgrounds (far to near)
        drawParallaxLayer(g, bgFar,  xOff, yOff, 0.08f, SCREEN_W, SCREEN_H);
        drawParallaxLayer(g, bgMid,  xOff, yOff, 0.28f, SCREEN_W, SCREEN_H);
        drawParallaxLayer(g, bgNear, xOff, yOff, 0.55f, SCREEN_W, SCREEN_H);

        // Level tiles and entities
        level.draw(g, xOff, yOff, debug);

        // Particles
        particles.draw(g, xOff, yOff);

        // Player
        player.setOffsets(xOff, yOff);
        player.drawTransformed(g);

        // Grappling hook
        if (hook != null && hook.isActive()) {
            hook.draw(g, xOff, yOff, player);
        }

        // Crosshair
        drawCrosshair(g, mouseGameX, mouseGameY);
    }

    // =========================================================================
    // Parallax
    // =========================================================================

    /**
     * Draws a single tiling parallax layer, scrolling at the given speed
     * relative to the camera offset.
     *
     * @param g     graphics context
     * @param img   the background image to tile
     * @param xOff  camera X offset
     * @param yOff  camera Y offset
     * @param speed parallax scroll speed factor (0 = static, 1 = same as camera)
     * @param sw    viewport width
     * @param sh    viewport height
     */
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

    // =========================================================================
    // Crosshair
    // =========================================================================

    /**
     * Draws a small crosshair (lines + circle) at the given game coordinates.
     *
     * @param g  graphics context
     * @param gx crosshair X in game coordinates
     * @param gy crosshair Y in game coordinates
     */
    private void drawCrosshair(Graphics2D g, int gx, int gy) {
        g.setColor(CROSSHAIR_COLOR);
        int s = 6;
        g.drawLine(gx - s, gy, gx + s, gy);
        g.drawLine(gx, gy - s, gx, gy + s);
        g.drawOval(gx - 3, gy - 3, 6, 6);
    }

    // =========================================================================
    // Debug / HUD overlays
    // =========================================================================

    /**
     * Draws the debug information overlay showing FPS, player position, enemy
     * count, particle count, ground state, current level, and a bounding-box
     * rectangle around the player.
     *
     * @param g            graphics context
     * @param player       the player entity
     * @param level        the current level
     * @param camera       the camera (for bounding box offset)
     * @param particles    the particle system
     * @param currentLevel the current level index (0-based, displayed as 1-based)
     * @param totalLevels  the total number of levels
     * @param fps          the current frames-per-second value
     */
    public void drawDebugInfo(Graphics2D g, Player player, Level level,
                              Camera camera, ParticleSystem particles,
                              int currentLevel, int totalLevels, double fps) {
        g.setFont(DEBUG_FONT);
        g.setColor(Color.YELLOW);
        int x = SCREEN_W - 180;

        g.drawString(String.format("FPS: %.0f", fps), x, 20);
        g.drawString(String.format("Player: %.0f,%.0f",
                     player.getX(), player.getY()), x, 34);
        g.drawString("Enemies: " + level.getEnemies().size(), x, 48);
        g.drawString("Particles: " + particles.getCount(), x, 62);
        g.drawString("Ground: " + player.isOnGround(), x, 76);
        g.drawString("Level: " + (currentLevel + 1) + "/" + totalLevels, x, 90);

        // Player bounding box
        g.setColor(Color.RED);
        int ox = camera.getDrawX();
        int oy = camera.getDrawY();
        g.drawRect((int)player.getX() + ox, (int)player.getY() + oy,
                   player.getWidth(), player.getHeight());
    }

    /**
     * Draws a simple FPS counter in the top-right corner.
     *
     * @param g   graphics context
     * @param fps the current frames-per-second value
     */
    public void drawFpsCounter(Graphics2D g, double fps) {
        g.setFont(FPS_FONT);
        g.setColor(FPS_COLOR);
        g.drawString(String.format("FPS: %.0f", fps), SCREEN_W - 90, 20);
    }

    // =========================================================================
    // Coordinate conversion
    // =========================================================================

    /**
     * Converts a raw mouse X coordinate from the JFrame/screen to the
     * corresponding X in the 800x600 game buffer. In windowed mode at the
     * native resolution this is a direct pass-through; in fullscreen the
     * scaled/centred draw area is accounted for.
     *
     * @param sx the screen-space mouse X
     * @return the game-space X coordinate
     */
    public int screenToGameX(int sx) {
        if (bufferW == SCREEN_W && bufferH == SCREEN_H) return sx;
        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale  = Math.min(scaleX, scaleY);
        int drawW = (int)(SCREEN_W * scale);
        int drawX = (bufferW - drawW) / 2;
        return (int)((sx - drawX) / scale);
    }

    /**
     * Converts a raw mouse Y coordinate from the JFrame/screen to the
     * corresponding Y in the 800x600 game buffer. In windowed mode at the
     * native resolution this is a direct pass-through; in fullscreen the
     * scaled/centred draw area is accounted for.
     *
     * @param sy the screen-space mouse Y
     * @return the game-space Y coordinate
     */
    public int screenToGameY(int sy) {
        if (bufferW == SCREEN_W && bufferH == SCREEN_H) return sy;
        float scaleX = (float) bufferW / SCREEN_W;
        float scaleY = (float) bufferH / SCREEN_H;
        float scale  = Math.min(scaleX, scaleY);
        int drawH = (int)(SCREEN_H * scale);
        int drawY = (bufferH - drawH) / 2;
        return (int)((sy - drawY) / scale);
    }

    // =========================================================================
    // Buffer management
    // =========================================================================

    /**
     * Updates the tracked screen/window dimensions. This is called when the
     * window is resized or fullscreen is toggled. It does not recreate the
     * internal render buffer (which stays at 800x600) -- it only updates the
     * values used for scaling and coordinate conversion.
     *
     * @param w the new screen/window width
     * @param h the new screen/window height
     */
    public void resizeBuffer(int w, int h) {
        this.bufferW = w;
        this.bufferH = h;
    }

    /**
     * Returns the current screen/window buffer width.
     *
     * @return buffer width in pixels
     */
    public int getBufferW() {
        return bufferW;
    }

    /**
     * Returns the current screen/window buffer height.
     *
     * @return buffer height in pixels
     */
    public int getBufferH() {
        return bufferH;
    }
}
