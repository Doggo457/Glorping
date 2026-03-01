package entities;

import game2D.Animation;
import game2D.TileMap;

/**
 * A fast-moving ground enemy that scurries along the floor.
 * Spiders are small, quick, and attack in swarms. They leap
 * at the player when close enough and jump over obstacles.
 */
public class Spider extends Enemy {

    private final Animation crawlAnim;
    private final Animation leapAnim;

    private int wanderDir = 1;
    private long leapCooldown = 0;
    private long wanderFlipCooldown = 0;
    private static final long WANDER_FLIP_COOLDOWN_MS = 500L;
    private static final long LEAP_INTERVAL = 1800L;
    private static final float LEAP_VY = -0.25f;
    private static final float LEAP_VX_MULT = 3.0f;
    private static final float WALL_JUMP_VY = -0.28f;

    public Spider(Animation crawlAnim, Animation leapAnim, Player target) {
        super(crawlAnim, target);
        this.crawlAnim = crawlAnim;
        this.leapAnim = leapAnim;
        health = maxHealth = 18;
        damage = 7;
        speed = 0.10f;
        aggroRange = 180f;
        wanderDir = Math.random() < 0.5 ? -1 : 1;
    }

    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        leapCooldown = Math.max(0, leapCooldown - elapsed);
        wanderFlipCooldown = Math.max(0, wanderFlipCooldown - elapsed);

        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 2.5f) aggro = false;

        if (aggro) {
            float dx = (target.getX() + target.getWidth() / 2f)
                     - (getX() + getWidth() / 2f);
            facingRight = dx > 0;
            float dirX = dx > 0 ? 1f : -1f;

            if (onGround && leapCooldown <= 0 && dist < 120) {
                // Leap at the player — aim upward if they're above
                float dy = target.getY() - getY();
                float leapVy = (dy < -30) ? LEAP_VY * 1.4f : LEAP_VY;
                setVelocityX(dirX * speed * LEAP_VX_MULT);
                setVelocityY(leapVy);
                leapCooldown = LEAP_INTERVAL + (long)(Math.random() * 600);
                setAnimation(leapAnim);
            } else if (onGround && isWallAhead(tmap) && leapCooldown <= 0) {
                // Jump over obstacles
                setVelocityX(dirX * speed * 2.0f);
                setVelocityY(WALL_JUMP_VY);
                leapCooldown = LEAP_INTERVAL;
                setAnimation(leapAnim);
            } else if (onGround && isLedgeAhead(tmap) && isPlayerBelow(100f)
                       && isSafeDropTowardPlayer(tmap, 8)) {
                // Drop off ledge to chase player below
                setVelocityX(dirX * speed * 1.5f);
                setAnimation(crawlAnim);
            } else if (onGround && isLedgeAhead(tmap) && leapCooldown <= 0) {
                // Jump across gaps
                setVelocityX(dirX * speed * 2.5f);
                setVelocityY(LEAP_VY);
                leapCooldown = LEAP_INTERVAL;
                setAnimation(leapAnim);
            } else if (onGround) {
                setVelocityX(dirX * speed * 1.5f);
                setAnimation(crawlAnim);
                // Stuck recovery — reverse and leap
                if (stuck && stuckCount >= 2 && leapCooldown <= 0) {
                    facingRight = !facingRight;
                    setVelocityX(-dirX * speed * 2.5f);
                    setVelocityY(WALL_JUMP_VY);
                    leapCooldown = LEAP_INTERVAL;
                    setAnimation(leapAnim);
                }
            } else {
                // In air from leap
                setVelocityX(getVelocityX() * 0.99f);
                setAnimation(leapAnim);
            }
        } else {
            // Idle scurry - reverse at walls and ledges (with cooldown to prevent jitter)
            if (onGround) {
                boolean blockedAhead = isWallAhead(tmap) || isLedgeAhead(tmap);
                if (blockedAhead && wanderFlipCooldown <= 0) {
                    wanderDir = -wanderDir;
                    wanderFlipCooldown = WANDER_FLIP_COOLDOWN_MS;
                    // Check if both directions are blocked (tiny platform)
                    facingRight = wanderDir > 0;
                    if (isWallAhead(tmap) || isLedgeAhead(tmap)) {
                        // Stuck on tiny platform — just idle in place
                        setVelocityX(0);
                        setAnimation(crawlAnim);
                        setScale(facingRight ? 1f : -1f, 1f);
                        return;
                    }
                } else if (Math.random() < 0.01) {
                    wanderDir = -wanderDir;
                }
                facingRight = wanderDir > 0;
                setVelocityX(wanderDir * speed * 0.6f);
                setAnimation(crawlAnim);
            }
        }

        setScale(facingRight ? 1f : -1f, 1f);
    }
}
