package utils;

/**
 * Static utility methods for common mathematical operations
 * used across the game, such as distance, angle, and clamping.
 */
public final class MathUtils {

    /** Private constructor to prevent instantiation of utility class */
    private MathUtils() {}

    /**
     * Computes the Euclidean distance between two 2D points.
     *
     * @param x1 X coordinate of point 1
     * @param y1 Y coordinate of point 1
     * @param x2 X coordinate of point 2
     * @param y2 Y coordinate of point 2
     * @return Distance in pixels
     */
    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Clamps a float value within a specified range.
     *
     * @param value Value to clamp
     * @param min   Minimum allowed value
     * @param max   Maximum allowed value
     * @return Clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps an integer value within a specified range.
     *
     * @param value Value to clamp
     * @param min   Minimum allowed value
     * @param max   Maximum allowed value
     * @return Clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Returns the angle in radians from point (x1, y1) towards (x2, y2).
     *
     * @param x1 Source X
     * @param y1 Source Y
     * @param x2 Target X
     * @param y2 Target Y
     * @return Angle in radians
     */
    public static float angleTo(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    /**
     * Linearly interpolates between two float values.
     *
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Returns a random float between min (inclusive) and max (exclusive).
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Random float in [min, max)
     */
    public static float randomRange(float min, float max) {
        return min + (float)(Math.random() * (max - min));
    }

    /**
     * Returns a random integer between min (inclusive) and max (exclusive).
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Random int in [min, max)
     */
    public static int randomInt(int min, int max) {
        return min + (int)(Math.random() * (max - min));
    }
}
