package entities;

import game2D.Animation;
import game2D.TileMap;
import projectiles.Projectile;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all computer-controlled enemy sprites.
 * Provides common fields (speed, aggro range, damage) and helper
 * methods used by the AI subclasses.
 *
 * Each subclass implements {@link #updateAI(long, TileMap)} to define
 * its own behaviour pattern.
 */
public abstract class Enemy extends Entity {

    // =========================================================================
    // AI fields
    // =========================================================================

    /** Reference to the player target – all enemies chase or shoot the player */
    protected Player target;
    /** Distance in pixels within which this enemy becomes aggro */
    protected float aggroRange;
    /** Whether the enemy has noticed the player */
    protected boolean aggro = false;
    /** Contact damage dealt when this enemy touches the player */
    protected int damage;
    /** Base movement speed in px/ms */
    protected float speed;
    /** Accumulated time for AI decision making */
    protected long aiTimer = 0;

    /**
     * Pending projectiles produced by this enemy's AI this frame.
     * GlorpingGame collects these each update and adds them to the
     * live projectile list.
     */
    protected final List<Projectile> pendingProjectiles = new ArrayList<>();

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructs an enemy with the given animation.
     *
     * @param anim   Starting animation
     * @param target Player reference for AI targeting
     */
    public Enemy(Animation anim, Player target) {
        super(anim);
        this.target = target;
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates the enemy: applies physics and advances animation.
     * AI is run separately via {@link #runAI} after collision resolution
     * so that onGround is accurate.
     *
     * @param elapsed Milliseconds since last update
     */
    @Override
    public void update(long elapsed) {
        super.update(elapsed);
        if (alive) {
            aiTimer += elapsed;
            lastElapsed = elapsed;
        }
    }

    /** Elapsed time stored for deferred AI call */
    private long lastElapsed = 0;

    /**
     * Runs the AI after collision has been resolved so onGround is correct.
     * Called by Level each tick after resolveEntityTileCollision.
     *
     * @param tmap Current tile map
     */
    public void runAI(TileMap tmap) {
        if (alive) {
            updateAI(lastElapsed, tmap);
        }
    }

    /**
     * AI update method – each subclass provides its specific behaviour.
     * Called once per game loop tick while the enemy is alive.
     *
     * @param elapsed Milliseconds since last update
     * @param tmap    Current tile map (may be null if not supplied)
     */
    public abstract void updateAI(long elapsed, TileMap tmap);

    // =========================================================================
    // Helpers used by subclass AI
    // =========================================================================

    /**
     * Returns the Euclidean distance from this enemy's centre to
     * another entity's centre.
     *
     * @param other Target entity
     * @return Distance in pixels
     */
    protected float distanceTo(Entity other) {
        float dx = (getX() + getWidth()  / 2f) - (other.getX() + other.getWidth()  / 2f);
        float dy = (getY() + getHeight() / 2f) - (other.getY() + other.getHeight() / 2f);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Moves the enemy horizontally towards the player's X position,
     * reversing direction if it walks into a wall.
     *
     * @param elapsed Milliseconds elapsed
     */
    protected void moveTowardPlayer(long elapsed) {
        if (target == null) return;
        float dx = (target.getX() + target.getWidth() / 2f)
                 - (getX() + getWidth() / 2f);
        float dirX = (dx > 0) ? 1f : -1f;
        facingRight = dirX > 0;
        setVelocityX(dirX * speed * speedMultiplier);
    }

    /**
     * Drains and returns any projectiles queued by this enemy during
     * the last AI update. Called by Level each tick.
     *
     * @return List of newly-spawned enemy projectiles
     */
    public List<Projectile> drainPendingProjectiles() {
        List<Projectile> result = new ArrayList<>(pendingProjectiles);
        pendingProjectiles.clear();
        return result;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** @return Contact damage dealt to the player on collision */
    public int getDamage() { return damage; }
}
