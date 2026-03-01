package entities;

import game2D.Animation;
import game2D.TileMap;

/**
 * A bouncy ground-dwelling enemy that hops around constantly.
 * When idle it bounces in random directions; when the player is
 * nearby it hops aggressively toward them.
 */
public class Slime extends Enemy {

    private final Animation normalAnim;
    private final Animation squishAnim;

    /** Time remaining until the slime can hop again (ms) */
    private long hopCooldown = 0;
    /** Upward velocity applied when the slime hops */
    private static final float HOP_VY = -0.30f;
    /** Idle hop (smaller bounce) */
    private static final float IDLE_HOP_VY = -0.20f;
    /** Base hop interval when aggro (ms) */
    private static final long AGGRO_HOP_INTERVAL = 1000L;
    /** Base hop interval when idle (ms) */
    private static final long IDLE_HOP_INTERVAL = 2000L;
    /** Current idle wander direction (-1 or 1) */
    private int wanderDir = 1;

    /**
     * Constructs a slime enemy.
     *
     * @param normalAnim Round idle animation
     * @param squishAnim Squished pre-jump animation
     * @param target     Player reference for AI targeting
     */
    public Slime(Animation normalAnim, Animation squishAnim, Player target) {
        super(normalAnim, target);
        this.normalAnim = normalAnim;
        this.squishAnim = squishAnim;
        health = maxHealth = 30;
        damage = 10;
        speed  = 0.08f;
        aggroRange = 250f;
        // Randomise initial wander direction
        wanderDir = Math.random() < 0.5 ? -1 : 1;
    }

    /**
     * Slime AI: constantly bounces around. When the player is close,
     * hops aggressively toward them. Otherwise bounces in random directions.
     *
     * @param elapsed Milliseconds since last update
     * @param tmap    Tile map for wall checks
     */
    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        hopCooldown = Math.max(0, hopCooldown - elapsed);

        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 2f) aggro = false;

        if (onGround && hopCooldown <= 0) {
            setAnimation(squishAnim);

            if (aggro) {
                // Hop toward the player
                float dx = (target.getX() + target.getWidth() / 2f)
                         - (getX() + getWidth() / 2f);
                float dy = target.getY() - getY();
                facingRight = dx > 0;
                float dirX = (dx > 0) ? 1f : -1f;

                // Bigger hop if wall ahead or player is above
                float hopVy = HOP_VY;
                float hopVx = dirX * speed * 2.0f;
                if (isWallAhead(tmap)) {
                    hopVy = HOP_VY * 1.3f; // jump higher over walls
                    hopVx = dirX * speed * 2.5f;
                } else if (dy < -40) {
                    hopVy = HOP_VY * 1.2f; // bigger hop to reach player above
                }

                // Stuck recovery — hop in opposite direction
                if (stuck && stuckCount >= 3) {
                    dirX = -dirX;
                    facingRight = !facingRight;
                    hopVx = dirX * speed * 2.5f;
                    hopVy = HOP_VY * 1.3f;
                }

                setVelocityX(hopVx);
                setVelocityY(hopVy);
                hopCooldown = AGGRO_HOP_INTERVAL + (long)(Math.random() * 400);
            } else {
                // Idle bounce — reverse at walls and ledges
                if (isWallAhead(tmap) || isLedgeAhead(tmap)) {
                    wanderDir = -wanderDir;
                } else if (Math.random() < 0.3) {
                    wanderDir = -wanderDir;
                }
                facingRight = wanderDir > 0;
                setVelocityX(wanderDir * speed * 0.8f);
                setVelocityY(IDLE_HOP_VY);
                hopCooldown = IDLE_HOP_INTERVAL + (long)(Math.random() * 800);
            }
        } else if (!onGround) {
            // Air control — steer toward player or wander dir to escape holes
            float airVx = getVelocityX();
            if (aggro) {
                float dx = (target.getX() + target.getWidth() / 2f)
                         - (getX() + getWidth() / 2f);
                float nudge = (dx > 0 ? 1f : -1f) * speed * 0.1f;
                setVelocityX(airVx * 0.98f + nudge);
            } else {
                setVelocityX(airVx * 0.98f + wanderDir * speed * 0.08f);
            }
        } else {
            // On ground, waiting for cooldown
            setVelocityX(0);
            setAnimation(normalAnim);
        }

        // Flip sprite based on facing direction
        setScale(facingRight ? 1f : -1f, 1f);
    }
}
