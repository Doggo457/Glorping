package entities;

import game2D.Animation;
import game2D.TileMap;

/**
 * A ground-dwelling skeleton warrior that patrols back and forth.
 * When the player is nearby it charges aggressively, jumps over
 * obstacles, and climbs ledges to reach the player.
 */
public class Skeleton extends Enemy {

    private final Animation walkAnim;
    private final Animation attackAnim;

    private int wanderDir = 1;
    private long dirChangeTimer = 0;
    private static final long DIR_CHANGE_INTERVAL = 2500L;
    private long attackAnimTimer = 0;
    private static final long ATTACK_ANIM_DURATION = 300L;

    private static final float JUMP_VY = -0.35f;
    private long jumpCooldown = 0;
    private static final long JUMP_COOLDOWN_MS = 600L;

    public Skeleton(Animation walkAnim, Animation attackAnim, Player target) {
        super(walkAnim, target);
        this.walkAnim = walkAnim;
        this.attackAnim = attackAnim;
        health = maxHealth = 50;
        damage = 50;
        speed = 0.05f;
        aggroRange = 200f;
        wanderDir = Math.random() < 0.5 ? -1 : 1;
    }

    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        dirChangeTimer = Math.max(0, dirChangeTimer - elapsed);
        attackAnimTimer = Math.max(0, attackAnimTimer - elapsed);
        jumpCooldown = Math.max(0, jumpCooldown - elapsed);

        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 2.5f) aggro = false;

        if (aggro) {
            moveTowardPlayer(elapsed);
            float charge = dist < 80 ? 2.5f : 1.6f;
            setVelocityX(getVelocityX() * charge);
            if (dist < 60) attackAnimTimer = ATTACK_ANIM_DURATION;

            // Jump over walls and climb ledges
            if (onGround && jumpCooldown <= 0 && isWallAhead(tmap)) {
                if (canClimbWallAhead(tmap)) {
                    setVelocityY(JUMP_VY);
                    jumpCooldown = JUMP_COOLDOWN_MS;
                } else if (stuck) {
                    // Wall is too tall — reverse and try to find another route
                    facingRight = !facingRight;
                    setVelocityX(-getVelocityX());
                }
            }

            // Drop off ledges to chase player below
            if (onGround && isLedgeAhead(tmap) && isPlayerBelow(120f)) {
                if (isSafeDropTowardPlayer(tmap, 8)) {
                    // Allow the drop — don't reverse direction
                    setVelocityX(getVelocityX());
                }
            } else if (onGround && isLedgeAhead(tmap) && !isPlayerBelow(120f)) {
                // Don't walk off ledges when player isn't below — jump across gap
                if (jumpCooldown <= 0) {
                    setVelocityY(JUMP_VY);
                    jumpCooldown = JUMP_COOLDOWN_MS;
                }
            }

            // Jump up toward player if they're above
            float dy = target.getY() - getY();
            if (onGround && jumpCooldown <= 0 && dy < -40 && dist < 160) {
                setVelocityY(JUMP_VY);
                jumpCooldown = JUMP_COOLDOWN_MS;
            }

            // Stuck recovery: reverse direction to try alternate path
            if (stuck && onGround && stuckCount >= 3) {
                facingRight = !facingRight;
                setVelocityX(facingRight ? speed * 2f : -speed * 2f);
                if (jumpCooldown <= 0) {
                    setVelocityY(JUMP_VY);
                    jumpCooldown = JUMP_COOLDOWN_MS;
                }
            }
        } else {
            // Patrol back and forth, reverse at walls/ledges
            if (dirChangeTimer <= 0) {
                wanderDir = -wanderDir;
                dirChangeTimer = DIR_CHANGE_INTERVAL + (long)(Math.random() * 1500);
            }
            if (onGround && (isWallAhead(tmap) || isLedgeAhead(tmap))) {
                wanderDir = -wanderDir;
                dirChangeTimer = DIR_CHANGE_INTERVAL;
            }
            facingRight = wanderDir > 0;
            setVelocityX(wanderDir * speed * 0.5f);
        }

        setAnimation(attackAnimTimer > 0 ? attackAnim : walkAnim);
        setScale(facingRight ? 1f : -1f, 1f);
    }
}
