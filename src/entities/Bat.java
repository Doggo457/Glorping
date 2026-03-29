package entities;

import game2D.Animation;
import game2D.TileMap;

/**
 * A flying enemy that pursues the player with a sine-wave oscillation
 * pattern, giving it an erratic flight path that is harder to dodge.
 * Bats are immune to gravity (they are always actively flying).
 */
public class Bat extends Enemy {

    private final Animation wingUpAnim;
    private final Animation wingDownAnim;

    /** Phase accumulator for the sine-wave oscillation */
    private float sinePhase = (float)(Math.random() * Math.PI * 2);
    /** Oscillation frequency in radians per millisecond */
    private static final float SINE_SPEED = 0.004f;
    /** Oscillation amplitude in pixels */
    private static final float SINE_AMP = 45f;
    /** Previous sine value, used to compute perpendicular offset delta */
    private float prevSine = 0;

    /**
     * Constructs a bat enemy.
     *
     * @param wingUpAnim   Wings-up animation frame
     * @param wingDownAnim Wings-down animation frame
     * @param target       Player reference for AI targeting
     */
    public Bat(Animation wingUpAnim, Animation wingDownAnim, Player target) {
        super(wingUpAnim, target);
        this.wingUpAnim   = wingUpAnim;
        this.wingDownAnim = wingDownAnim;
        health = maxHealth = 20;
        damage = 8;
        speed  = 0.06f;
        aggroRange = 320f;
    }

    /**
     * Bats are not affected by gravity – they self-propel.
     *
     * @return Always true
     */
    @Override
    protected boolean isFlying() { return true; }

    /**
     * Bat AI: pursues the player with a sine-wave perpendicular oscillation.
     * Alternates wing animations in sync with the oscillation phase.
     *
     * @param elapsed Milliseconds since last update
     * @param tmap    Tile map (not used by bat AI)
     */
    @Override
    public void updateAI(long elapsed, TileMap tmap) {
        float dist = distanceTo(target);
        if (dist < aggroRange) aggro = true;
        if (dist > aggroRange * 1.5f) aggro = false;

        if (!aggro) {
            setVelocityX(0);
            setVelocityY(0);
            setAnimation(wingUpAnim);
            return;
        }

        // Direction vector towards player's centre
        float dx = (target.getX() + target.getWidth()  / 2f)
                 - (getX()  + getWidth()  / 2f);
        float dy = (target.getY() + target.getHeight() / 2f)
                 - (getY()  + getHeight() / 2f);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;

        // If stuck, try to navigate around the obstacle
        float bvx, bvy;
        if (stuck && !hasLineOfSight(tmap)) {
            // Steer perpendicular to break free, alternating direction
            float perpX = -dy / len;
            float perpY =  dx / len;
            int side = (stuckCount % 2 == 0) ? 1 : -1;
            bvx = ((dx / len) * 0.3f + perpX * side * 0.7f) * speed * 1.5f;
            bvy = ((dy / len) * 0.3f + perpY * side * 0.7f) * speed * 1.5f;
        } else {
            bvx = (dx / len) * speed;
            bvy = (dy / len) * speed;
        }

        // Perpendicular oscillation (90° rotation of direction vector)
        sinePhase += SINE_SPEED * elapsed;
        float curSine = (float) Math.sin(sinePhase);
        float sineD = curSine - prevSine;
        prevSine = curSine;

        // Perpendicular unit vector: rotate (dx,dy) by 90°
        float px = -dy / len;
        float py =  dx / len;
        safeShiftX(tmap, px * sineD * SINE_AMP);
        safeShiftY(tmap, py * sineD * SINE_AMP);

        setVelocityX(bvx);
        setVelocityY(bvy);

        // Steer away from walls
        applyWallAvoidance(tmap, speed * 0.8f);

        facingRight = dx > 0;
        setScale(facingRight ? 1f : -1f, 1f);

        // Toggle wing animation with oscillation
        setAnimation((sinePhase % (Math.PI) < Math.PI / 2) ? wingUpAnim : wingDownAnim);
    }
}
