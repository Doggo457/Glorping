package world;

import utils.MathUtils;

/**
 * Manages the game camera/viewport that follows the player through the world.
 * The camera is clamped so it never shows outside the world bounds.
 * All draw offset calculations are provided via getDrawX() and getDrawY().
 */
public class Camera {

    /** Current world-space X position of the camera's top-left corner */
    private float x;
    /** Current world-space Y position of the camera's top-left corner */
    private float y;
    /** Width of the screen/viewport in pixels */
    private final int screenWidth;
    /** Height of the screen/viewport in pixels */
    private final int screenHeight;
    /** Smoothing factor for camera movement (0=instant, 1=never moves) */
    private static final float SMOOTH = 0.12f;

    /**
     * Constructs a camera for the given screen dimensions.
     *
     * @param screenWidth  Width of the viewport in pixels
     * @param screenHeight Height of the viewport in pixels
     */
    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Smoothly moves the camera to centre on the target position,
     * clamping within the specified world bounds.
     *
     * @param targetX  World X of the entity to follow (its left edge)
     * @param targetY  World Y of the entity to follow (its top edge)
     * @param entityW  Width of the entity
     * @param entityH  Height of the entity
     * @param worldW   Total world width in pixels
     * @param worldH   Total world height in pixels
     */
    public void follow(float targetX, float targetY, int entityW, int entityH,
                       int worldW, int worldH) {
        float desiredX = targetX + entityW / 2f - screenWidth / 2f;
        float desiredY = targetY + entityH / 2f - screenHeight / 2f;

        // Smooth interpolation towards the desired position
        x = MathUtils.lerp(x, desiredX, 1f - SMOOTH);
        y = MathUtils.lerp(y, desiredY, 1f - SMOOTH);

        // Clamp so the camera doesn't show outside the world
        x = MathUtils.clamp(x, 0, Math.max(0, worldW - screenWidth));
        y = MathUtils.clamp(y, 0, Math.max(0, worldH - screenHeight));
    }

    /**
     * Snaps the camera instantly to centre on a world position.
     * Useful when loading a new level.
     *
     * @param worldX World X position to centre on
     * @param worldY World Y position to centre on
     * @param worldW World width in pixels
     * @param worldH World height in pixels
     */
    public void snapTo(float worldX, float worldY, int worldW, int worldH) {
        x = worldX - screenWidth / 2f;
        y = worldY - screenHeight / 2f;
        x = MathUtils.clamp(x, 0, Math.max(0, worldW - screenWidth));
        y = MathUtils.clamp(y, 0, Math.max(0, worldH - screenHeight));
    }

    /**
     * Returns the X draw offset to pass to sprite/tilemap draw calls.
     * This is the negative of the camera world X, shifting everything
     * so the camera position becomes screen position (0,0).
     *
     * @return X draw offset in pixels
     */
    public int getDrawX() { return -(int) x; }

    /**
     * Returns the Y draw offset to pass to sprite/tilemap draw calls.
     *
     * @return Y draw offset in pixels
     */
    public int getDrawY() { return -(int) y; }

    /** @return Current camera world X */
    public float getX() { return x; }

    /** @return Current camera world Y */
    public float getY() { return y; }
}
