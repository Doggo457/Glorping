package world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;

/**
 * A single visual particle used for spell impacts, explosions,
 * footsteps, and other transient effects.
 * Particles have position, velocity, colour, size, and lifetime.
 */
public class Particle {

    /** World X position in pixels */
    private float x, y;
    /** Velocity in pixels per millisecond */
    private float vx, vy;
    /** Colour of the particle */
    private Color colour;
    /** Radius in pixels */
    private float radius;
    /** Remaining lifetime in milliseconds */
    private float lifetime;
    /** Starting lifetime used to compute fade-out alpha */
    private float maxLifetime;
    /** Whether gravity affects this particle */
    private boolean affectedByGravity;
    /** Gravity constant in px/ms² */
    private static final float GRAVITY = 0.0003f;

    /**
     * Constructs a new particle with the given properties.
     *
     * @param x        World X position
     * @param y        World Y position
     * @param vx       Horizontal velocity (px/ms)
     * @param vy       Vertical velocity (px/ms)
     * @param colour   Display colour
     * @param radius   Radius in pixels
     * @param lifetime Duration in milliseconds before expiry
     * @param gravity  True if the particle should fall under gravity
     */
    public Particle(float x, float y, float vx, float vy,
                    Color colour, float radius, float lifetime, boolean gravity) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.colour = colour;
        this.radius = radius;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.affectedByGravity = gravity;
    }

    /**
     * Updates the particle's position and decrements its lifetime.
     *
     * @param elapsed Milliseconds elapsed since last update
     */
    public void update(long elapsed) {
        if (affectedByGravity) vy += GRAVITY * elapsed;
        x += vx * elapsed;
        y += vy * elapsed;
        lifetime -= elapsed;
    }

    /**
     * Draws the particle as a filled circle with alpha fade-out.
     *
     * @param g       The graphics context to draw on
     * @param xOffset Camera X draw offset
     * @param yOffset Camera Y draw offset
     */
    public void draw(Graphics2D g, int xOffset, int yOffset) {
        float alpha = Math.max(0f, lifetime / maxLifetime);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(colour);
        int px = (int)(x + xOffset - radius);
        int py = (int)(y + yOffset - radius);
        int diam = (int)(radius * 2);
        g.fillOval(px, py, diam, diam);
        g.setComposite(old);
    }

    /** @return True if this particle has expired */
    public boolean isDead() { return lifetime <= 0; }

    /** @return World X position */
    public float getX() { return x; }

    /** @return World Y position */
    public float getY() { return y; }
}
