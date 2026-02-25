package world;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a pool of transient visual {@link Particle} objects.
 * Provides factory methods for common particle effects (spell impacts,
 * explosions, blood splats, sparks, footstep dust).
 * Dead particles are pruned each update cycle.
 */
public class ParticleSystem {

    /** All active particles */
    private final List<Particle> particles = new ArrayList<>();

    /** Maximum particles allowed at once (performance cap) */
    private static final int MAX_PARTICLES = 400;

    // =========================================================================
    // Update & draw
    // =========================================================================

    /**
     * Advances all particles and removes expired ones.
     *
     * @param elapsed Milliseconds since last update
     */
    public void update(long elapsed) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(elapsed);
            if (p.isDead()) it.remove();
        }
    }

    /**
     * Draws all active particles.
     *
     * @param g       Graphics context
     * @param xOffset Camera draw X offset
     * @param yOffset Camera draw Y offset
     */
    public void draw(Graphics2D g, int xOffset, int yOffset) {
        for (Particle p : particles) {
            p.draw(g, xOffset, yOffset);
        }
    }

    /** Removes all particles (e.g. on level change). */
    public void clear() { particles.clear(); }

    /** @return Number of active particles */
    public int getCount() { return particles.size(); }

    // =========================================================================
    // Emission helpers
    // =========================================================================

    /**
     * Spawns a burst of particles at a world position.
     *
     * @param x        World X origin
     * @param y        World Y origin
     * @param count    Number of particles to spawn
     * @param colour   Particle colour
     * @param speed    Max speed in px/ms
     * @param lifetime Particle lifetime in ms
     * @param gravity  Whether particles are affected by gravity
     */
    public void burst(float x, float y, int count, Color colour,
                      float speed, float lifetime, boolean gravity) {
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            double angle = Math.random() * Math.PI * 2;
            float s = (float)(Math.random() * speed);
            float vx = (float) Math.cos(angle) * s;
            float vy = (float) Math.sin(angle) * s;
            float r  = 2f + (float)(Math.random() * 3f);
            float lt = lifetime * (0.5f + (float) Math.random() * 0.5f);
            particles.add(new Particle(x, y, vx, vy, colour, r, lt, gravity));
        }
    }

    // =========================================================================
    // Named effect factories
    // =========================================================================

    /**
     * Fireball impact: orange/yellow burst with gravity-affected sparks.
     *
     * @param x World X
     * @param y World Y
     */
    public void spawnFireImpact(float x, float y) {
        burst(x, y, 12, new Color(255, 140, 0), 0.25f, 400, true);
        burst(x, y, 6,  new Color(255, 220, 80), 0.35f, 300, true);
        burst(x, y, 4,  new Color(255, 50, 0, 180), 0.15f, 600, false);
    }

    /**
     * Lightning bolt impact: white and blue sparks, no gravity.
     *
     * @param x World X
     * @param y World Y
     */
    public void spawnLightningImpact(float x, float y) {
        burst(x, y, 10, Color.WHITE,                0.4f, 250, false);
        burst(x, y, 8,  new Color(120, 120, 255),   0.3f, 350, false);
    }

    /**
     * Magic missile impact: purple sparkle cloud.
     *
     * @param x World X
     * @param y World Y
     */
    public void spawnMissileImpact(float x, float y) {
        burst(x, y, 8, new Color(180, 80, 255), 0.2f, 500, false);
        burst(x, y, 4, Color.WHITE,             0.3f, 300, false);
    }

    /**
     * Explosion: large orange-red burst with gravity sparks.
     *
     * @param x World X
     * @param y World Y
     */
    public void spawnExplosion(float x, float y) {
        burst(x, y, 20, new Color(255, 100, 0), 0.5f, 700, true);
        burst(x, y, 15, new Color(200, 50,  0), 0.4f, 900, true);
        burst(x, y, 10, new Color(255, 200, 0), 0.6f, 500, false);
        burst(x, y, 8,  Color.WHITE,            0.7f, 350, false);
    }

    /**
     * Enemy death: quick coloured burst.
     *
     * @param x      World X
     * @param y      World Y
     * @param colour Enemy's representative colour
     */
    public void spawnDeathBurst(float x, float y, Color colour) {
        burst(x, y, 10, colour,     0.3f, 500, true);
        burst(x, y, 5,  Color.WHITE, 0.2f, 300, false);
    }

    /**
     * Footstep dust: small grey puffs that rise briefly then fall.
     *
     * @param x World X
     * @param y World Y (foot position)
     */
    public void spawnFootstepDust(float x, float y) {
        for (int i = 0; i < 3 && particles.size() < MAX_PARTICLES; i++) {
            float vx = (float)((Math.random() - 0.5) * 0.06f);
            float vy = -(float)(Math.random() * 0.05f);
            particles.add(new Particle(x, y, vx, vy,
                    new Color(160, 140, 120, 180), 2f, 300f, true));
        }
    }

    /**
     * Water splash: blue droplets scattered upward.
     *
     * @param x World X
     * @param y World Y (water surface)
     */
    public void spawnWaterSplash(float x, float y) {
        burst(x, y, 8, new Color(80, 150, 255), 0.2f, 500, true);
    }
}
