package entities;

import game2D.TileMap;
import physics.CollisionHandler;

import java.awt.*;

/**
 * A grappling hook fired with right-click. The hook is a heavy
 * projectile affected by gravity. On hitting a solid tile it
 * anchors and the player swings as a pendulum.
 *
 * While attached, Entity gravity is disabled on the player and
 * all gravity is handled here — decomposed into tangential force
 * so the pendulum accelerates properly without fighting the rope.
 */
public class GrapplingHook {

    public enum HookState { FLYING, ATTACHED }

    private HookState state = HookState.FLYING;

    // Hook tip position
    private float hookX, hookY;
    // Hook velocity (affected by gravity while flying)
    private float velX, velY;
    // Anchor point once attached
    private float anchorX, anchorY;
    // Fixed rope length (set once on attach, never changes)
    private float ropeLength;

    // Hook projectile: fast but heavy, arcs downward
    private static final float HOOK_SPEED   = 0.55f;
    private static final float HOOK_GRAVITY  = 0.0003f;  // gravity on the hook itself
    private static final float MAX_RANGE     = 640f;     // ~20 tiles at 32px each

    // Swing physics
    private static final float SWING_GRAVITY = 0.0004f;  // match Entity.GRAVITY
    private static final float DAMPING       = 0.998f;

    private float distanceTravelled = 0;
    private boolean active = true;

    public GrapplingHook(float startX, float startY, float targetX, float targetY) {
        this.hookX = startX;
        this.hookY = startY;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;

        velX = (dx / len) * HOOK_SPEED;
        velY = (dy / len) * HOOK_SPEED;
    }

    public void update(long elapsed, TileMap tmap, Player player) {
        if (!active) return;

        switch (state) {
            case FLYING:
                updateFlying(elapsed, tmap);
                break;
            case ATTACHED:
                updateSwing(elapsed, player, tmap);
                break;
        }
    }

    private void updateFlying(long elapsed, TileMap tmap) {
        // Apply gravity to hook projectile (arcs like a heavy object)
        velY += HOOK_GRAVITY * elapsed;

        hookX += velX * elapsed;
        hookY += velY * elapsed;
        distanceTravelled += (float) Math.sqrt(velX * velX + velY * velY) * elapsed;

        if (CollisionHandler.isSolid(tmap, hookX, hookY)) {
            anchorX = hookX;
            anchorY = hookY;
            state = HookState.ATTACHED;
            return;
        }

        // Kill hook if it's gone too far
        if (distanceTravelled > MAX_RANGE) {
            active = false;
        }
    }

    private void updateSwing(long elapsed, Player player, TileMap tmap) {
        // Disable entity gravity and normal movement — we handle it ourselves
        player.setGravityDisabled(true);
        player.setHooked(true);

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        float dx = px - anchorX;
        float dy = py - anchorY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Set rope length once on first attached frame
        if (ropeLength <= 0) {
            ropeLength = dist;
            if (ropeLength < 20f) ropeLength = 20f;
        }

        // Get current velocity
        float pvx = player.getVelocityX();
        float pvy = player.getVelocityY();

        // Apply full gravity to velocity first
        pvy += SWING_GRAVITY * elapsed;

        // Now enforce rope constraint
        if (dist > 1f) {
            float nx = dx / dist;
            float ny = dy / dist;

            // Tangent direction (perpendicular to rope)
            float tx = -ny;
            float ty = nx;

            // Decompose velocity into radial and tangential
            float radialSpeed = pvx * nx + pvy * ny;
            float tangentSpeed = pvx * tx + pvy * ty;

            if (dist >= ropeLength) {
                // Snap position to rope length
                player.setX(anchorX + nx * ropeLength - player.getWidth() / 2f);
                player.setY(anchorY + ny * ropeLength - player.getHeight() / 2f);

                // Remove outward radial velocity, keep only tangential
                if (radialSpeed > 0) {
                    pvx = tx * tangentSpeed;
                    pvy = ty * tangentSpeed;
                }
            }

            // Apply damping
            pvx *= DAMPING;
            pvy *= DAMPING;
        }

        player.setVelocityX(pvx);
        player.setVelocityY(pvy);

        // Detach if anchor tile got destroyed
        if (!CollisionHandler.isSolid(tmap, anchorX, anchorY)) {
            release();
        }
    }

    public void release() {
        active = false;
    }

    /**
     * Must be called when the hook is released or deactivated
     * to re-enable gravity and normal movement on the player.
     */
    public void restorePlayerGravity(Player player) {
        player.setGravityDisabled(false);
        player.setHooked(false);
    }

    public void draw(Graphics2D g, int xOff, int yOff, Player player) {
        if (!active) return;

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        float endX = state == HookState.ATTACHED ? anchorX : hookX;
        float endY = state == HookState.ATTACHED ? anchorY : hookY;

        int sx = (int) px + xOff;
        int sy = (int) py + yOff;
        int ex = (int) endX + xOff;
        int ey = (int) endY + yOff;

        Stroke oldStroke = g.getStroke();

        // Draw rope as a catenary (sagging curve)
        float ropeDx = ex - sx;
        float ropeDy = ey - sy;
        float ropeLen = (float) Math.sqrt(ropeDx * ropeDx + ropeDy * ropeDy);
        float slack = state == HookState.ATTACHED
                ? Math.max(0, ropeLen * 0.08f)
                : ropeLen * 0.04f;

        int segments = 12;
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(140, 100, 50));
        int prevRx = sx, prevRy = sy;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            int rx = (int)(sx + ropeDx * t);
            int ry = (int)(sy + ropeDy * t);
            // Parabolic sag: peaks at midpoint (t=0.5)
            float sag = slack * 4f * t * (1f - t);
            ry += (int) sag;
            g.drawLine(prevRx, prevRy, rx, ry);
            prevRx = rx;
            prevRy = ry;
        }

        // Hook tip
        g.setColor(new Color(180, 180, 180));
        g.fillRect(ex - 2, ey - 2, 5, 5);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(ex - 2, ey - 2, 5, 5);

        g.setStroke(oldStroke);
    }

    public boolean isActive() { return active; }
    public HookState getState() { return state; }
}
