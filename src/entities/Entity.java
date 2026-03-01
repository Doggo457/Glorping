package entities;

import game2D.Animation;
import game2D.Sprite;
import game2D.TileMap;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Base class for all game entities (player and enemies).
 * Extends the library's Sprite to add health, damage feedback,
 * gravity-driven physics, and a hurt-flash visual effect.
 *
 * Subclasses must call {@link #update(long)} to advance physics and
 * animation, and should override {@link #draw(Graphics2D)} if extra
 * rendering is needed.
 */
public abstract class Entity extends Sprite {

    // =========================================================================
    // Entity state
    // =========================================================================

    /** Defines the possible behavioural states of an entity. */
    public enum EntityState { IDLE, WALKING, JUMPING, FALLING, ATTACKING, HURT, DEAD }

    protected EntityState state = EntityState.IDLE;
    protected int health;
    protected int maxHealth;
    protected boolean alive = true;
    /** True when the entity is resting on a solid tile */
    protected boolean onGround = false;
    /** True when the entity should face right (affects drawTransformed flip) */
    protected boolean facingRight = true;
    /** Speed multiplier applied by environmental effects (e.g. water = 0.5) */
    protected float speedMultiplier = 1.0f;
    /** When true, gravity is not applied (used by grappling hook) */
    protected boolean gravityDisabled = false;

    // =========================================================================
    // Hurt flash
    // =========================================================================

    /** Remaining hurt-flash display time in milliseconds */
    private long hurtTimer = 0;
    private static final long HURT_DURATION = 200L;

    // =========================================================================
    // Gravity
    // =========================================================================

    /** Gravitational acceleration in pixels/ms² */
    protected static final float GRAVITY = 0.0004f;
    /** Maximum downward fall speed in px/ms */
    protected static final float MAX_FALL_SPEED = 0.6f;

    // =========================================================================
    // Draw offset tracking (needed for hurt-flash overlay)
    // =========================================================================

    private int drawOffX = 0;
    private int drawOffY = 0;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates an entity with the supplied animation.
     *
     * @param anim Starting animation – must have at least one frame
     */
    public Entity(Animation anim) {
        super(anim);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Reduces the entity's health by {@code amount}.
     * Triggers the hurt-flash visual effect. If health reaches zero the entity
     * transitions to DEAD state and {@link #onDeath()} is called.
     *
     * @param amount Positive damage amount
     */
    public void takeDamage(int amount) {
        if (!alive) return;
        health = Math.max(0, health - amount);
        hurtTimer = HURT_DURATION;
        state = EntityState.HURT;
        if (health <= 0) {
            alive = false;
            state = EntityState.DEAD;
            onDeath();
        }
    }

    /**
     * Restores health, capped at maxHealth.
     *
     * @param amount Positive heal amount
     */
    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    /**
     * Hook called when the entity's health reaches zero.
     * Subclasses can override for custom death behaviour (death animation, drops, etc.).
     */
    protected void onDeath() { hide(); }

    // =========================================================================
    // Update / draw
    // =========================================================================

    /**
     * Updates the entity: applies gravity (if not on ground), advances velocity,
     * animates, and decrements the hurt timer.
     *
     * @param elapsed Milliseconds since last update
     */
    @Override
    public void update(long elapsed) {
        // Apply gravity if not on the ground, not flying, and gravity not disabled
        if (!onGround && !isFlying() && !gravityDisabled) {
            float newVy = getVelocityY() + GRAVITY * elapsed;
            setVelocityY(Math.min(newVy, MAX_FALL_SPEED));
        }
        // Reset onGround each frame; CollisionHandler will set it back to true when needed
        onGround = false;

        super.update(elapsed);

        if (hurtTimer > 0) {
            hurtTimer = Math.max(0, hurtTimer - elapsed);
            if (hurtTimer == 0 && state == EntityState.HURT) {
                state = EntityState.IDLE;
            }
        }
    }

    /**
     * Draws the entity. If a hurt-flash is active, a semi-transparent
     * red overlay is drawn over the sprite.
     *
     * @param g Graphics context
     */
    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        if (hurtTimer > 0 && isVisible()) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.RED);
            g.fillRect((int) getX() + drawOffX, (int) getY() + drawOffY,
                       getWidth(), getHeight());
            g.setComposite(old);
        }
    }

    /**
     * Overrides setOffsets so the hurt-flash overlay knows where to draw.
     *
     * @param x Draw X offset
     * @param y Draw Y offset
     */
    @Override
    public void setOffsets(int x, int y) {
        super.setOffsets(x, y);
        drawOffX = x;
        drawOffY = y;
    }

    // =========================================================================
    // Getters / state helpers
    // =========================================================================

    /** Sets the speed multiplier (1.0 = normal, 0.5 = half speed in water, etc.) */
    public void setSpeedMultiplier(float m) { speedMultiplier = m; }

    /** @return Current speed multiplier */
    public float getSpeedMultiplier() { return speedMultiplier; }

    /** @return True if this entity is still alive */
    public boolean isAlive() { return alive; }

    /** @return Current health */
    public int getHealth() { return health; }

    /** @return Maximum health */
    public int getMaxHealth() { return maxHealth; }

    /** @return True if the entity is resting on solid ground */
    public boolean isOnGround() { return onGround; }

    /**
     * Sets whether the entity is on the ground.
     * Called by {@link physics.CollisionHandler} after tile collision resolution.
     *
     * @param onGround True if standing on a solid tile
     */
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    /** @return Current entity state */
    public EntityState getState() { return state; }

    /** Disables or enables gravity for this entity (used by grappling hook). */
    public void setGravityDisabled(boolean disabled) { gravityDisabled = disabled; }

    /** @return True when this entity should not be affected by gravity (e.g. bats) */
    protected boolean isFlying() { return false; }

    // =========================================================================
    // Static helper
    // =========================================================================

    /**
     * Creates a minimal single-frame Animation from a plain colour.
     * Useful as a fallback when image loading fails.
     *
     * @param width  Frame width in pixels
     * @param height Frame height in pixels
     * @param colour Fill colour
     * @return A one-frame Animation
     */
    public static Animation createColorAnimation(int width, int height, Color colour) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(colour);
        g.fillRect(0, 0, width, height);
        g.dispose();
        Animation anim = new Animation();
        anim.addFrame(img, 500);
        return anim;
    }
}
