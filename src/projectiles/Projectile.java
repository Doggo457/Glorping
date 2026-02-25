package projectiles;

import game2D.Animation;
import game2D.Sprite;
import spells.SpellType;

/**
 * Represents a spell projectile fired by the player or an enemy wizard.
 * Projectiles move with an initial velocity, optionally fall under gravity,
 * and expire after a maximum lifetime.
 *
 * A projectile is inactive (and should be removed) when {@link #isActive()}
 * returns false.
 */
public class Projectile extends Sprite {

    /** Type of spell this projectile represents */
    private final SpellType type;
    /** Damage dealt on impact */
    private int damage;
    /** True if fired by the player; false if fired by an enemy */
    private final boolean playerOwned;
    /** Remaining lifetime in milliseconds */
    private float lifetime;
    /** Whether gravity affects this projectile */
    private final boolean affectedByGravity;
    /** True while the projectile is in-flight and hasn't been deactivated */
    private boolean active = true;

    /** Gravitational acceleration shared with Entity */
    private static final float GRAVITY = 0.0004f;
    private static final float MAX_FALL = 0.8f;

    /**
     * Constructs a projectile.
     *
     * @param type         Spell type (determines damage, lifetime, gravity)
     * @param anim         Animation to display
     * @param x            Initial world X (projectile centre)
     * @param y            Initial world Y (projectile centre)
     * @param vx           Horizontal velocity in px/ms
     * @param vy           Vertical velocity in px/ms
     * @param playerOwned  True if the player fired this projectile
     */
    public Projectile(SpellType type, Animation anim,
                      float x, float y, float vx, float vy, boolean playerOwned) {
        super(anim);
        this.type = type;
        this.playerOwned = playerOwned;

        // Centre the projectile on the spawn point
        setPosition(x - getWidth() / 2f, y - getHeight() / 2f);
        setVelocity(vx, vy);

        // Configure per-spell-type properties
        switch (type) {
            case FIREBALL:
                damage = 25; lifetime = 3000; affectedByGravity = true;
                break;
            case LIGHTNING_BOLT:
                damage = 40; lifetime = 1200; affectedByGravity = false;
                break;
            case MAGIC_MISSILE:
                damage = 15; lifetime = 4000; affectedByGravity = false;
                break;
            case EXPLODE:
                damage = 60; lifetime = 400;  affectedByGravity = false;
                break;
            case SPARK:
                damage = 8;  lifetime = 1000; affectedByGravity = true;
                break;
            default:
                damage = 10; lifetime = 2000; affectedByGravity = false;
                break;
        }
    }

    /**
     * Updates position, applies gravity if applicable, and decrements lifetime.
     *
     * @param elapsed Milliseconds since last update
     */
    @Override
    public void update(long elapsed) {
        if (!active) return;

        if (affectedByGravity) {
            float newVy = getVelocityY() + GRAVITY * elapsed;
            setVelocityY(Math.min(newVy, MAX_FALL));
        }

        super.update(elapsed);
        lifetime -= elapsed;
        if (lifetime <= 0) active = false;
    }

    /** Deactivates this projectile (hit a wall or entity). */
    public void deactivate() { active = false; }

    /** @return True if the projectile is still in-flight */
    public boolean isActive() { return active; }

    /** @return Damage dealt on impact */
    public int getDamage() { return damage; }

    /** @return True if fired by the player */
    public boolean isPlayerOwned() { return playerOwned; }

    /** @return The spell type */
    public SpellType getType() { return type; }

    /**
     * Returns the centre X world coordinate of the projectile.
     *
     * @return Centre X in pixels
     */
    public float getCentreX() { return getX() + getWidth() / 2f; }

    /**
     * Returns the centre Y world coordinate of the projectile.
     *
     * @return Centre Y in pixels
     */
    public float getCentreY() { return getY() + getHeight() / 2f; }
}
