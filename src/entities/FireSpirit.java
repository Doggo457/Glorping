package entities;

import game2D.Animation;
import game2D.TileMap;
import projectiles.Projectile;
import spells.SpellType;
import spells.Wand;

/**
 * A fast-moving fire elemental that flies erratically and shoots
 * fireballs at the player. Appears in deeper lava-themed levels.
 * Combines bat-like flight with wizard-like ranged attacks.
 */
public class FireSpirit extends Enemy {

    private final Animation flyAnim;
    private final Animation fireballAnim;

    private float sinePhase;
    private static final float SINE_SPEED = 0.005f;
    private static final float SINE_AMP = 35f;
    private float prevSine = 0;

    private long attackCooldown;
    private static final long ATTACK_INTERVAL = 2800L;
    private static final float PROJ_SPEED = 0.18f;

    public FireSpirit(Animation flyAnim, Animation fireballAnim, Player target) {
        super(flyAnim, target);
        this.flyAnim = flyAnim;
        this.fireballAnim = fireballAnim;
        health = maxHealth = 35;
        damage = 10;
        speed = 0.07f;
        aggroRange = 350f;
        sinePhase = (float)(Math.random() * Math.PI * 2);
        attackCooldown = 1500 + (long)(Math.random() * 1000);
    }

    @Override
    protected boolean isFlying() { return true; }

    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        attackCooldown = Math.max(0, attackCooldown - elapsed);

        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 1.8f) aggro = false;

        if (!aggro) {
            setVelocityX(0);
            setVelocityY(0);
            return;
        }

        // Fly toward player with oscillation
        float dx = (target.getX() + target.getWidth() / 2f)
                 - (getX() + getWidth() / 2f);
        float dy = (target.getY() + target.getHeight() / 2f)
                 - (getY() + getHeight() / 2f);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;

        float bvx, bvy;
        if (stuck && !hasLineOfSight(tmap)) {
            float perpX = -dy / len;
            float perpY =  dx / len;
            int side = (stuckCount % 2 == 0) ? 1 : -1;
            bvx = ((dx / len) * 0.3f + perpX * side * 0.7f) * speed * 1.5f;
            bvy = ((dy / len) * 0.3f + perpY * side * 0.7f) * speed * 1.5f;
        } else {
            bvx = (dx / len) * speed;
            bvy = (dy / len) * speed;
        }

        setVelocityX(bvx);
        setVelocityY(bvy);

        // Steer away from walls
        applyWallAvoidance(tmap, speed * 0.8f);

        // Perpendicular sine oscillation
        sinePhase += SINE_SPEED * elapsed;
        float curSine = (float) Math.sin(sinePhase);
        float sineD = curSine - prevSine;
        prevSine = curSine;
        float px = -dy / len, py = dx / len;
        safeShiftX(tmap, px * sineD * SINE_AMP);
        safeShiftY(tmap, py * sineD * SINE_AMP);

        // Shoot fireballs with shot leading
        if (attackCooldown <= 0 && dist < 300 && hasLineOfSight(tmap)) {
            float cx = getX() + getWidth() / 2f;
            float cy = getY() + getHeight() / 2f;
            // Predict where the player will be
            float travelTime = dist / PROJ_SPEED;
            float aimX = target.getX() + target.getWidth() / 2f
                       + target.getVelocityX() * travelTime * 0.6f;
            float aimY = target.getY() + target.getHeight() / 2f
                       + target.getVelocityY() * travelTime * 0.4f;
            Projectile p = Wand.createEnemyProjectile(
                    SpellType.FIREBALL, fireballAnim,
                    cx, cy, aimX, aimY, PROJ_SPEED);
            pendingProjectiles.add(p);
            attackCooldown = ATTACK_INTERVAL + (long)(Math.random() * 1000);
        }

        facingRight = dx > 0;
        setScale(facingRight ? 1f : -1f, 1f);
        setAnimation(flyAnim);
    }
}
