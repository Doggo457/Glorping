package entities;

import game2D.Animation;
import game2D.TileMap;
import projectiles.Projectile;
import spells.SpellType;
import spells.Wand;

/**
 * A ranged enemy wizard that shoots magic projectiles at the player.
 * The wizard maintains a preferred engagement distance, backing away
 * when the player is too close and advancing when too far.
 * It also strafes sideways occasionally to avoid standing still.
 */
public class WizardEnemy extends Enemy {

    private final Animation idleAnim;
    private final Animation castAnim;

    /** Animation displayed while casting a spell */
    private long castAnimTimer = 0;
    private static final long CAST_ANIM_DURATION = 400L;

    /** Time until the wizard can fire again (ms) */
    private long attackCooldown = 0;
    /** Base attack interval in ms */
    private static final long ATTACK_INTERVAL = 2200L;
    /** Projectile speed for enemy shots in px/ms */
    private static final float PROJ_SPEED = 0.22f;
    /** Ideal gap to maintain between wizard and player */
    private static final float PREFERRED_DIST = 180f;
    /** Strafe state and timer */
    private int strafeDir = 0;
    private long strafeTimer = 0;
    /** Jump cooldown for obstacle navigation */
    private long jumpCooldown = 0;
    private static final long JUMP_COOLDOWN_MS = 800L;
    private static final float JUMP_VY = -0.32f;

    /** The animation to use for the magic missile projectile */
    private final Animation missileAnim;

    /**
     * Constructs a wizard enemy.
     *
     * @param idleAnim   Idle/walking animation
     * @param castAnim   Casting animation (shown when firing)
     * @param missileAnim Animation for the magic missile projectiles this enemy fires
     * @param target     Player reference for AI targeting
     */
    public WizardEnemy(Animation idleAnim, Animation castAnim,
                       Animation missileAnim, Player target) {
        super(idleAnim, target);
        this.idleAnim   = idleAnim;
        this.castAnim   = castAnim;
        this.missileAnim = missileAnim;
        health = maxHealth = 60;
        damage = 5; // contact damage (small – wizard prefers range)
        speed  = 0.04f;
        aggroRange = 380f;
        attackCooldown = 1000; // small initial delay before first shot
    }

    /**
     * Wizard AI:
     * <ul>
     *   <li>Maintains preferred engagement distance from the player</li>
     *   <li>Strafes horizontally to stay unpredictable</li>
     *   <li>Fires a magic missile at the player on a cooldown</li>
     * </ul>
     *
     * @param elapsed Milliseconds since last update
     * @param tmap    Tile map (used for ground check in movement)
     */
    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        attackCooldown = Math.max(0, attackCooldown - elapsed);
        castAnimTimer  = Math.max(0, castAnimTimer  - elapsed);
        strafeTimer    = Math.max(0, strafeTimer    - elapsed);
        jumpCooldown   = Math.max(0, jumpCooldown   - elapsed);

        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 1.8f) aggro = false;

        if (!aggro) {
            setVelocityX(0);
            setAnimation(idleAnim);
            return;
        }

        // Compute direction towards player
        float dx = (target.getX() + target.getWidth()  / 2f)
                 - (getX()  + getWidth()  / 2f);
        float dy = (target.getY() + target.getHeight() / 2f)
                 - (getY()  + getHeight() / 2f);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;

        facingRight = dx > 0;

        // Distance-based approach / retreat with terrain checks
        if (dist < PREFERRED_DIST - 30) {
            // Too close – retreat opposite to player
            float retreatDir = -(dx / len);
            // Check if retreat direction has a wall or ledge
            boolean oldFacing = facingRight;
            facingRight = retreatDir > 0;
            if (onGround && (isWallAhead(tmap) || isLedgeAhead(tmap))) {
                // Can't retreat — strafe instead
                facingRight = oldFacing;
                if (strafeTimer <= 0) {
                    strafeDir = (Math.random() < 0.5) ? 1 : -1;
                    strafeTimer = 400 + (long)(Math.random() * 400);
                }
                setVelocityX(strafeDir * speed * 0.8f);
            } else {
                facingRight = dx > 0; // keep facing player while retreating
                setVelocityX(retreatDir * speed);
            }
        } else if (dist > PREFERRED_DIST + 50) {
            // Too far – move closer
            setVelocityX((dx / len) * speed);
            // Jump over walls to close distance
            if (onGround && jumpCooldown <= 0 && isWallAhead(tmap)
                    && canClimbWallAhead(tmap)) {
                setVelocityY(JUMP_VY);
                jumpCooldown = JUMP_COOLDOWN_MS;
            }
        } else {
            // At preferred distance – strafe sideways with terrain awareness
            if (strafeTimer <= 0) {
                strafeDir = (Math.random() < 0.5) ? 1 : -1;
                strafeTimer = 600 + (long)(Math.random() * 800);
            }
            // Check if strafe direction is blocked
            boolean oldFacing = facingRight;
            facingRight = strafeDir > 0;
            if (onGround && (isWallAhead(tmap) || isLedgeAhead(tmap))) {
                strafeDir = -strafeDir; // flip strafe direction
                strafeTimer = 400;
            }
            facingRight = dx > 0; // always face the player
            setVelocityX(strafeDir * speed * 0.6f);
        }

        // Stuck recovery — jump or reverse
        if (stuck && onGround && stuckCount >= 2) {
            if (jumpCooldown <= 0 && isWallAhead(tmap)) {
                setVelocityY(JUMP_VY);
                jumpCooldown = JUMP_COOLDOWN_MS;
            } else {
                facingRight = !facingRight;
                setVelocityX(facingRight ? speed : -speed);
            }
        }

        // Fire a projectile — only if we have line of sight, with shot leading
        if (attackCooldown <= 0 && hasLineOfSight(tmap)) {
            float cx = getX() + getWidth() / 2f;
            float cy = getY() + getHeight() / 2f;
            // Lead the shot based on player velocity
            float travelTime = dist / PROJ_SPEED;
            float aimX = target.getX() + target.getWidth() / 2f
                       + target.getVelocityX() * travelTime * 0.7f;
            float aimY = target.getY() + target.getHeight() / 2f
                       + target.getVelocityY() * travelTime * 0.5f;
            Projectile p = Wand.createEnemyProjectile(
                    SpellType.MAGIC_MISSILE, missileAnim,
                    cx, cy, aimX, aimY, PROJ_SPEED);
            pendingProjectiles.add(p);
            attackCooldown = ATTACK_INTERVAL + (long)(Math.random() * 800);
            castAnimTimer  = CAST_ANIM_DURATION;
        }

        // Show cast animation briefly after firing
        setAnimation(castAnimTimer > 0 ? castAnim : idleAnim);
        setScale(facingRight ? 1f : -1f, 1f);
    }
}
