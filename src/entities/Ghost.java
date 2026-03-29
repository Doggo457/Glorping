package entities;

import game2D.Animation;
import game2D.TileMap;

/**
 * A floating ghost that drifts slowly toward the player.
 * Ghosts ignore gravity, move through open space, and have an
 * eerie bobbing motion. They deal moderate damage on contact
 * and are hard to hit due to their erratic vertical movement.
 */
public class Ghost extends Enemy {

    private final Animation floatAnim;
    private float bobPhase;
    private static final float BOB_SPEED = 0.003f;
    private static final float BOB_AMP = 25f;
    private float prevBob = 0;

    public Ghost(Animation floatAnim, Player target) {
        super(floatAnim, target);
        this.floatAnim = floatAnim;
        health = maxHealth = 25;
        damage = 12;
        speed = 0.035f;
        aggroRange = 300f;
        bobPhase = (float)(Math.random() * Math.PI * 2);
    }

    @Override
    protected boolean isFlying() { return true; }

    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 2f) aggro = false;

        if (!aggro) {
            setVelocityX(0);
            setVelocityY(0);
            // Gentle idle bob
            bobPhase += BOB_SPEED * elapsed;
            float curBob = (float) Math.sin(bobPhase);
            safeShiftY(tmap, (curBob - prevBob) * BOB_AMP * 0.3f);
            prevBob = curBob;
            return;
        }

        // Drift toward player
        float dx = (target.getX() + target.getWidth() / 2f)
                 - (getX() + getWidth() / 2f);
        float dy = (target.getY() + target.getHeight() / 2f)
                 - (getY() + getHeight() / 2f);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;

        float bvx, bvy;
        if (stuck && !hasLineOfSight(tmap)) {
            // Navigate around obstacle — try perpendicular direction
            float perpX = -dy / len;
            float perpY =  dx / len;
            int side = (stuckCount % 2 == 0) ? 1 : -1;
            bvx = ((dx / len) * 0.3f + perpX * side * 0.7f) * speed * 1.3f;
            bvy = ((dy / len) * 0.3f + perpY * side * 0.7f) * speed * 1.3f;
        } else {
            bvx = (dx / len) * speed;
            bvy = (dy / len) * speed;
        }

        setVelocityX(bvx);
        setVelocityY(bvy);

        // Steer away from walls
        applyWallAvoidance(tmap, speed * 0.6f);

        // Eerie vertical bobbing while pursuing
        bobPhase += BOB_SPEED * elapsed;
        float curBob = (float) Math.sin(bobPhase);
        safeShiftY(tmap, (curBob - prevBob) * BOB_AMP);
        prevBob = curBob;

        facingRight = dx > 0;
        setScale(facingRight ? 1f : -1f, 1f);
        setAnimation(floatAnim);
    }
}
