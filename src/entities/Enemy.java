package entities;

import game2D.Animation;
import game2D.TileMap;
import physics.CollisionHandler;
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

    // =========================================================================
    // Stuck detection
    // =========================================================================

    private float stuckCheckX, stuckCheckY;
    private long stuckTimer = 0;
    private static final long STUCK_CHECK_INTERVAL = 800L;
    private static final float STUCK_THRESHOLD = 4f;
    /** True when the enemy has been stuck for at least one check interval */
    protected boolean stuck = false;
    /** Counts consecutive stuck checks — higher = more drastic recovery */
    protected int stuckCount = 0;

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
            updateStuckDetection(elapsed);
        }
    }

    private void updateStuckDetection(long elapsed) {
        stuckTimer += elapsed;
        if (stuckTimer >= STUCK_CHECK_INTERVAL) {
            stuckTimer -= STUCK_CHECK_INTERVAL;
            float dx = getX() - stuckCheckX;
            float dy = getY() - stuckCheckY;
            float moved = (float) Math.sqrt(dx * dx + dy * dy);
            if (moved < STUCK_THRESHOLD && aggro) {
                stuck = true;
                stuckCount++;
            } else {
                stuck = false;
                stuckCount = 0;
            }
            stuckCheckX = getX();
            stuckCheckY = getY();
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
    // Pathfinding helpers
    // =========================================================================

    /**
     * Returns true if there is a solid wall immediately ahead of this enemy
     * at foot level. Used by ground enemies to detect obstacles.
     */
    protected boolean isWallAhead(TileMap tmap) {
        if (tmap == null) return false;
        float probeX = facingRight
                ? getX() + getWidth() + 2
                : getX() - 2;
        float probeY = getY() + getHeight() / 2f;
        return CollisionHandler.isSolid(tmap, probeX, probeY);
    }

    /**
     * Returns true if the wall ahead can be climbed (1-2 blocks high with
     * open space above). Ground enemies can jump over these.
     */
    protected boolean canClimbWallAhead(TileMap tmap) {
        if (tmap == null) return false;
        int tw = tmap.getTileWidth();
        int th = tmap.getTileHeight();
        float probeX = facingRight
                ? getX() + getWidth() + 4
                : getX() - 4;
        float footY = getY() + getHeight();
        // Check if 1-2 tiles above the wall top are open
        boolean wallAtFeet  = CollisionHandler.isSolid(tmap, probeX, footY - th * 0.5f);
        boolean openAbove1  = !CollisionHandler.isSolid(tmap, probeX, footY - th * 1.5f);
        boolean openAbove2  = !CollisionHandler.isSolid(tmap, probeX, footY - th * 2.5f);
        return wallAtFeet && openAbove1 && openAbove2;
    }

    /**
     * Returns true if there is a ledge/drop ahead (no ground in front).
     */
    protected boolean isLedgeAhead(TileMap tmap) {
        if (tmap == null) return false;
        float probeX = facingRight
                ? getX() + getWidth() + 2
                : getX() - 2;
        float belowFeet = getY() + getHeight() + 4;
        return !CollisionHandler.isSolid(tmap, probeX, belowFeet);
    }

    /**
     * Returns true if the drop ahead is safe (player is below, and the drop
     * is not into a death pit — there is ground within maxDropTiles tiles).
     */
    protected boolean isSafeDropTowardPlayer(TileMap tmap, int maxDropTiles) {
        if (tmap == null) return false;
        // Only drop if player is actually below us
        if (target.getY() < getY()) return false;
        float probeX = facingRight
                ? getX() + getWidth() / 2f + 8
                : getX() + getWidth() / 2f - 8;
        int th = tmap.getTileHeight();
        float footY = getY() + getHeight();
        for (int i = 1; i <= maxDropTiles; i++) {
            if (CollisionHandler.isSolid(tmap, probeX, footY + th * i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the player is reachable by dropping down (within
     * a few tiles below and roughly aligned horizontally).
     */
    protected boolean isPlayerBelow(float maxHorizDist) {
        float dy = target.getY() - getY();
        float dx = Math.abs((target.getX() + target.getWidth() / 2f)
                          - (getX() + getWidth() / 2f));
        return dy > 20 && dx < maxHorizDist;
    }

    /**
     * Checks if there is a clear line of sight to the player (no solid
     * tiles blocking the path). Uses a simple raycast with step sampling.
     */
    protected boolean hasLineOfSight(TileMap tmap) {
        if (tmap == null) return true;
        float sx = getX() + getWidth() / 2f;
        float sy = getY() + getHeight() / 2f;
        float ex = target.getX() + target.getWidth() / 2f;
        float ey = target.getY() + target.getHeight() / 2f;
        float dx = ex - sx, dy = ey - sy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 1f) return true;
        int steps = (int)(dist / 12f); // check every 12 pixels
        if (steps < 2) steps = 2;
        for (int i = 1; i < steps; i++) {
            float t = (float) i / steps;
            if (CollisionHandler.isSolid(tmap, sx + dx * t, sy + dy * t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * For flying enemies: steers velocity away from nearby solid tiles.
     * Samples 8 directions around the enemy and pushes velocity away
     * from any solid neighbours.
     */
    protected void applyWallAvoidance(TileMap tmap, float strength) {
        if (tmap == null) return;
        float cx = getX() + getWidth() / 2f;
        float cy = getY() + getHeight() / 2f;
        float probeR = Math.max(getWidth(), getHeight()) * 0.8f;
        float avoidX = 0, avoidY = 0;
        for (int i = 0; i < 8; i++) {
            float angle = (float)(i * Math.PI / 4.0);
            float px = cx + (float) Math.cos(angle) * probeR;
            float py = cy + (float) Math.sin(angle) * probeR;
            if (CollisionHandler.isSolid(tmap, px, py)) {
                // Push away from this direction
                avoidX -= (float) Math.cos(angle);
                avoidY -= (float) Math.sin(angle);
            }
        }
        if (avoidX != 0 || avoidY != 0) {
            float len = (float) Math.sqrt(avoidX * avoidX + avoidY * avoidY);
            setVelocityX(getVelocityX() + (avoidX / len) * strength);
            setVelocityY(getVelocityY() + (avoidY / len) * strength);
        }
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** @return Contact damage dealt to the player on collision */
    public int getDamage() { return damage; }
}
