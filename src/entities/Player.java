package entities;

import game2D.Animation;
import projectiles.Projectile;
import sound.SoundManager;
import spells.Wand;
import spells.WandType;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The player-controlled wizard character.
 * Manages movement, jumping, spell casting, health/mana, and wand inventory.
 *
 * The player uses four animation states driven by movement and action flags:
 * idle, walk, jump/fall, and cast. State transitions happen automatically
 * based on velocity and input flags set by GlorpingGame.
 *
 * Physics model:
 * <ul>
 *   <li>Horizontal: constant speed while key held, immediate stop on release</li>
 *   <li>Vertical: gravity from Entity base class; jump impulse on SPACE press</li>
 *   <li>Double-jump: one mid-air jump is allowed per grounded period</li>
 * </ul>
 */
public class Player extends Entity {

    // =========================================================================
    // Animation states
    // =========================================================================

    private Animation idleAnim;
    private Animation walkAnim;
    private Animation jumpAnim;
    private Animation castAnim;
    private Animation deathAnim;
    private Animation wallGrabAnim;

    // =========================================================================
    // Stats
    // =========================================================================

    private int mana;
    private int maxMana;
    private int gold = 0;

    // =========================================================================
    // Physics constants
    // =========================================================================

    /** Horizontal run speed in px/ms */
    private static final float MOVE_SPEED  = 0.18f;
    /** Initial vertical velocity when jumping (negative = up) */
    private static final float JUMP_VY     = -0.36f;
    /** Extra jump for double-jump (slightly weaker) */
    private static final float DJUMP_VY    = -0.28f;

    // =========================================================================
    // Jump state
    // =========================================================================

    /** Whether the player has used their double-jump this airborne period */
    private boolean doubleJumpUsed = false;
    /** True when the player is clinging to a wall (pressing into it while airborne) */
    private boolean wallGrabbing = false;

    // =========================================================================
    // Input flags (set by GlorpingGame per-frame)
    // =========================================================================

    private boolean moveLeftHeld  = false;
    private boolean moveRightHeld = false;
    private boolean jumpPressed   = false;  // consume-once flag
    private boolean shootHeld     = false;
    /** World-space aim target set by mouse position */
    private float aimWorldX = 0;
    private float aimWorldY = 0;
    /** True while the player is attached to a grappling hook rope */
    private boolean hooked = false;
    /** Horizontal swing force applied per ms while hooked */
    private static final float SWING_FORCE = 0.0003f;
    /** Invincibility frames timer (ms remaining) */
    private long iframeTimer = 0;
    /** Duration of invincibility after being hit (ms) */
    private static final long IFRAME_DURATION = 600L;
    /** True on blink-off frames during iframes (used for semi-transparent draw) */
    private boolean iframeBlink = false;

    // =========================================================================
    // Wand inventory
    // =========================================================================

    private final List<Wand> wands = new ArrayList<>();
    private int activeWandIndex = 0;

    // =========================================================================
    // Sound / footstep pacing
    // =========================================================================

    private long footstepTimer = 0;
    private static final long FOOTSTEP_INTERVAL = 320L;

    // =========================================================================
    // Mana regeneration
    // =========================================================================

    private long manaRegenTimer = 0;
    private static final long MANA_REGEN_INTERVAL = 500L;
    private static final int  MANA_REGEN_AMOUNT   = 2;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructs the player with the given animation set.
     *
     * @param idleAnim     Idle animation
     * @param walkAnim     Walking animation
     * @param jumpAnim     Jump/fall animation
     * @param castAnim     Spell-casting animation
     * @param deathAnim    Death animation
     * @param wallGrabAnim Wall grab animation
     */
    public Player(Animation idleAnim, Animation walkAnim,
                  Animation jumpAnim, Animation castAnim, Animation deathAnim,
                  Animation wallGrabAnim) {
        super(idleAnim);
        this.idleAnim     = idleAnim;
        this.walkAnim     = walkAnim;
        this.jumpAnim     = jumpAnim;
        this.castAnim     = castAnim;
        this.deathAnim    = deathAnim;
        this.wallGrabAnim = wallGrabAnim;

        health = maxHealth = 100;
        mana   = maxMana   = 120;
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates the player each frame:
     * applies horizontal movement, handles jump input, advances mana regen,
     * updates the active wand cooldown, and selects the correct animation.
     *
     * @param elapsed Milliseconds since last update
     */
    @Override
    public void update(long elapsed) {
        if (!alive) {
            super.update(elapsed);
            return;
        }

        // ---- Horizontal movement ----
        if (hooked) {
            // While on the rope, A/D add swing force instead of setting velocity.
            // Releasing keys does NOT zero velocity — momentum is preserved.
            if (moveLeftHeld && !moveRightHeld) {
                setVelocityX(getVelocityX() - SWING_FORCE * elapsed);
                facingRight = false;
            } else if (moveRightHeld && !moveLeftHeld) {
                setVelocityX(getVelocityX() + SWING_FORCE * elapsed);
                facingRight = true;
            }
        } else {
            float moveSpeed = MOVE_SPEED * speedMultiplier;
            if (moveLeftHeld && !moveRightHeld) {
                setVelocityX(-moveSpeed);
                facingRight = false;
            } else if (moveRightHeld && !moveLeftHeld) {
                setVelocityX(moveSpeed);
                facingRight = true;
            } else {
                setVelocityX(0);
            }
        }

        // ---- Jump ----
        if (jumpPressed) {
            jumpPressed = false;
            if (onGround) {
                setVelocityY(JUMP_VY);
                doubleJumpUsed = false;
            } else if (!doubleJumpUsed) {
                setVelocityY(DJUMP_VY);
                doubleJumpUsed = true;
            }
        }

        // Reset double-jump when landing or wall grabbing
        if (onGround || wallGrabbing) doubleJumpUsed = false;

        // ---- Wand update ----
        Wand active = getActiveWand();
        if (active != null) active.update(elapsed);

        // ---- Invincibility frames ----
        if (iframeTimer > 0) iframeTimer = Math.max(0, iframeTimer - elapsed);

        // ---- Mana regeneration ----
        manaRegenTimer += elapsed;
        if (manaRegenTimer >= MANA_REGEN_INTERVAL) {
            manaRegenTimer -= MANA_REGEN_INTERVAL;
            mana = Math.min(maxMana, mana + MANA_REGEN_AMOUNT);
        }

        // ---- Invincibility blink (tracked for draw, does NOT hide the sprite) ----
        iframeBlink = iframeTimer > 0 && (iframeTimer / 80) % 2 == 0;

        // ---- Animation selection ----
        updateAnimation(elapsed);

        // ---- Sprite scale (flip) ----
        setScale(facingRight ? 1f : -1f, 1f);

        super.update(elapsed);
    }

    /**
     * Selects the correct animation based on current player state.
     *
     * @param elapsed Elapsed time for footstep timing
     */
    private void updateAnimation(long elapsed) {
        if (!alive) {
            setAnimation(deathAnim);
            return;
        }
        if (shootHeld && getActiveWand() != null) {
            setAnimation(castAnim);
        } else if (wallGrabbing) {
            setAnimation(wallGrabAnim);
        } else if (!onGround) {
            setAnimation(jumpAnim);
        } else if (Math.abs(getVelocityX()) > 0.01f) {
            setAnimation(walkAnim);
            footstepTimer += elapsed;
        } else {
            setAnimation(idleAnim);
        }
    }

    // =========================================================================
    // Draw (iframe transparency)
    // =========================================================================

    @Override
    public void draw(Graphics2D g) {
        if (iframeBlink) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            super.draw(g);
            g.setComposite(old);
        } else {
            super.draw(g);
        }
    }

    // =========================================================================
    // Shooting
    // =========================================================================

    /**
     * Attempts to cast a spell from the active wand towards the world-space
     * aim point.
     *
     * @param sounds Sound manager to play the shoot effect
     * @return A new Projectile if the cast succeeded, null otherwise
     */
    public Projectile tryShoot(SoundManager sounds) {
        if (!alive || !shootHeld) return null;
        Wand w = getActiveWand();
        if (w == null) return null;

        float cx = getX() + getWidth()  / 2f;
        float cy = getY() + getHeight() / 2f;
        Projectile p = w.tryCast(cx, cy, aimWorldX, aimWorldY, mana);
        if (p != null) {
            mana -= w.getManaCost();
            sounds.play("shoot");
        }
        return p;
    }

    // =========================================================================
    // Input setters (called from GlorpingGame)
    // =========================================================================

    /** @param left True while the left movement key is held */
    public void setMoveLeft(boolean left)   { moveLeftHeld  = left; }
    /** @param right True while the right movement key is held */
    public void setMoveRight(boolean right) { moveRightHeld = right; }
    /** Called by Level after collision to indicate wall contact state. */
    public void setWallGrabbing(boolean grabbing) {
        wallGrabbing = grabbing;
        // Override animation immediately since this runs after updateAnimation()
        if (grabbing && alive && !shootHeld) {
            setAnimation(wallGrabAnim);
        }
    }
    /** @return True if pressing A */
    public boolean isMoveLeftHeld() { return moveLeftHeld; }
    /** @return True if pressing D */
    public boolean isMoveRightHeld() { return moveRightHeld; }
    /** Signals a jump request (consumed once). */
    public void pressJump()                 { jumpPressed = true; }
    /** Sets whether the player is attached to a grappling hook. */
    public void setHooked(boolean hooked)   { this.hooked = hooked; }
    /** @param shoot True while the fire button is held */
    public void setShootHeld(boolean shoot) { shootHeld = shoot; }

    /**
     * Sets the world-space aim target for the active wand.
     *
     * @param wx World X of the aim point
     * @param wy World Y of the aim point
     */
    public void setAimTarget(float wx, float wy) {
        aimWorldX = wx;
        aimWorldY = wy;
    }

    // =========================================================================
    // Wand management
    // =========================================================================

    /**
     * Adds a wand to the player's inventory (max 4 wands).
     *
     * @param wand Wand to add
     */
    public void addWand(Wand wand) {
        if (wands.size() < 4) wands.add(wand);
    }

    /** Cycles to the next wand in the inventory. */
    public void nextWand() {
        if (!wands.isEmpty())
            activeWandIndex = (activeWandIndex + 1) % wands.size();
    }

    /** Switches to wand at the given inventory index (0-based). */
    public void selectWand(int index) {
        if (index >= 0 && index < wands.size()) activeWandIndex = index;
    }

    /** @return The currently active wand, or null if inventory is empty */
    public Wand getActiveWand() {
        if (wands.isEmpty()) return null;
        return wands.get(activeWandIndex);
    }

    /**
     * Swaps the active wand with a new one and returns the old wand.
     * If inventory is empty, just adds the new wand and returns null.
     */
    public Wand swapActiveWand(Wand newWand) {
        if (wands.isEmpty()) {
            wands.add(newWand);
            return null;
        }
        Wand old = wands.get(activeWandIndex);
        wands.set(activeWandIndex, newWand);
        return old;
    }

    /** @return All wands in the inventory */
    public List<Wand> getWands() { return wands; }

    /** @return Active wand index */
    public int getActiveWandIndex() { return activeWandIndex; }

    // =========================================================================
    // Mana
    // =========================================================================

    /**
     * Restores mana, capped at maxMana.
     *
     * @param amount Amount to restore
     */
    public void restoreMana(int amount) { mana = Math.min(maxMana, mana + amount); }

    /** @return Current mana */
    public int getMana()    { return mana; }
    /** @return Maximum mana */
    public int getMaxMana() { return maxMana; }

    // =========================================================================
    // Gold
    // =========================================================================

    /** Adds collected gold. */
    public void addGold(int amount) { gold += amount; }
    /** @return Total gold collected */
    public int getGold() { return gold; }

    /**
     * Spends gold if the player has enough.
     * @param cost Amount to spend
     * @return True if the purchase succeeded
     */
    public boolean spendGold(int cost) {
        if (gold >= cost) { gold -= cost; return true; }
        return false;
    }

    /** Increases max health and heals by the same amount. */
    public void increaseMaxHealth(int amount) {
        maxHealth += amount;
        health = Math.min(maxHealth, health + amount);
    }

    /** Increases max mana and restores by the same amount. */
    public void increaseMaxMana(int amount) {
        maxMana += amount;
        mana = Math.min(maxMana, mana + amount);
    }

    // =========================================================================
    // Invincibility / damage
    // =========================================================================

    @Override
    public void takeDamage(int amount) {
        if (iframeTimer > 0) return;  // still invincible
        super.takeDamage(amount);
        // Don't let HURT state stun the player — keep them moving
        if (alive) {
            state = EntityState.IDLE;
            iframeTimer = IFRAME_DURATION;
        }
    }

    /** @return True if the player is currently in invincibility frames */
    public boolean isInvincible() { return iframeTimer > 0; }

    // =========================================================================
    // Death hook
    // =========================================================================

    @Override
    protected void onDeath() {
        setAnimation(deathAnim);
        setVelocityX(0);
        // Keep visible so the death animation can play
        show();
    }

    // =========================================================================
    // Footstep helper
    // =========================================================================

    /**
     * Returns true if a footstep sound should play this frame,
     * resetting the timer.
     *
     * @return True if a footstep should play
     */
    public boolean shouldPlayFootstep() {
        if (onGround && Math.abs(getVelocityX()) > 0.01f
                && footstepTimer >= FOOTSTEP_INTERVAL) {
            footstepTimer = 0;
            return true;
        }
        return false;
    }
}
