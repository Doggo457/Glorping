package spells;

import game2D.Animation;
import projectiles.Projectile;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a wand that the player can use to cast spells.
 * Each wand type has a different set of available spells, fire rate,
 * and mana cost per shot. The wand cycles through its spell list on each cast.
 *
 * Projectile animations are supplied externally (loaded by GlorpingGame) and
 * stored in a map for lookup.
 */
public class Wand {

    // =========================================================================
    // Wand configuration
    // =========================================================================

    private final WandType wandType;
    /** Ordered list of spell types loaded into this wand */
    private final SpellType[] spells;
    /** Index into spells[] pointing at the next spell to fire */
    private int spellIndex = 0;
    /** Minimum milliseconds between consecutive shots */
    private final long castCooldownMs;
    /** Mana consumed per shot */
    private final int manaPerCast;
    /** Base speed of projectiles in px/ms */
    private final float projectileSpeed;

    /** Milliseconds remaining before the wand can fire again */
    private long cooldownRemaining = 0;

    /** Animations indexed by SpellType for building Projectile objects */
    private final Map<SpellType, Animation> spellAnimations;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Creates a wand of the specified type.
     *
     * @param type           Wand archetype
     * @param spellAnimations Map of SpellType → Animation for projectile visuals
     */
    public Wand(WandType type, Map<SpellType, Animation> spellAnimations) {
        this.wandType = type;
        this.spellAnimations = new HashMap<>(spellAnimations);

        switch (type) {
            case FIRE_WAND:
                spells = new SpellType[]{SpellType.FIREBALL, SpellType.SPARK,
                                         SpellType.FIREBALL};
                castCooldownMs  = 450;
                manaPerCast     = 10;
                projectileSpeed = 0.30f;
                break;
            case LIGHTNING_WAND:
                spells = new SpellType[]{SpellType.LIGHTNING_BOLT};
                castCooldownMs  = 800;
                manaPerCast     = 22;
                projectileSpeed = 0.65f;
                break;
            case CHAOS_WAND:
                spells = SpellType.values();
                castCooldownMs  = 200;
                manaPerCast     = 12;
                projectileSpeed = 0.45f;
                break;
            case EXPLOSION_WAND:
                spells = new SpellType[]{SpellType.EXPLODE};
                castCooldownMs  = 1200;
                manaPerCast     = 30;
                projectileSpeed = 0.28f;
                break;
            case SPARK_WAND:
                spells = new SpellType[]{SpellType.SPARK, SpellType.SPARK, SpellType.MAGIC_MISSILE};
                castCooldownMs  = 120;
                manaPerCast     = 3;
                projectileSpeed = 0.50f;
                break;
            case SNIPER_WAND:
                spells = new SpellType[]{SpellType.LIGHTNING_BOLT};
                castCooldownMs  = 1400;
                manaPerCast     = 18;
                projectileSpeed = 0.90f;
                break;
            case INFERNO_WAND:
                spells = new SpellType[]{SpellType.FIREBALL, SpellType.EXPLODE,
                                         SpellType.FIREBALL, SpellType.FIREBALL};
                castCooldownMs  = 500;
                manaPerCast     = 16;
                projectileSpeed = 0.32f;
                break;
            case STORM_WAND:
                spells = new SpellType[]{SpellType.LIGHTNING_BOLT, SpellType.LIGHTNING_BOLT,
                                         SpellType.SPARK};
                castCooldownMs  = 300;
                manaPerCast     = 14;
                projectileSpeed = 0.55f;
                break;
            case GATLING_WAND:
                spells = new SpellType[]{SpellType.MAGIC_MISSILE};
                castCooldownMs  = 80;
                manaPerCast     = 2;
                projectileSpeed = 0.35f;
                break;
            case DEMOLITION_WAND:
                spells = new SpellType[]{SpellType.EXPLODE, SpellType.EXPLODE};
                castCooldownMs  = 900;
                manaPerCast     = 35;
                projectileSpeed = 0.25f;
                break;
            case ARCANE_WAND:
                spells = new SpellType[]{SpellType.MAGIC_MISSILE};
                castCooldownMs  = 600;
                manaPerCast     = 8;
                projectileSpeed = 0.70f;
                break;
            case HELLFIRE_WAND:
                spells = new SpellType[]{SpellType.FIREBALL, SpellType.FIREBALL,
                                         SpellType.FIREBALL};
                castCooldownMs  = 180;
                manaPerCast     = 7;
                projectileSpeed = 0.38f;
                break;
            case TEMPEST_WAND:
                spells = new SpellType[]{SpellType.LIGHTNING_BOLT, SpellType.SPARK,
                                         SpellType.LIGHTNING_BOLT, SpellType.SPARK};
                castCooldownMs  = 250;
                manaPerCast     = 10;
                projectileSpeed = 0.50f;
                break;
            case NOVA_WAND:
                spells = new SpellType[]{SpellType.EXPLODE, SpellType.SPARK,
                                         SpellType.SPARK, SpellType.SPARK};
                castCooldownMs  = 350;
                manaPerCast     = 15;
                projectileSpeed = 0.40f;
                break;
            case MYSTIC_WAND:
                spells = new SpellType[]{SpellType.MAGIC_MISSILE, SpellType.LIGHTNING_BOLT};
                castCooldownMs  = 400;
                manaPerCast     = 13;
                projectileSpeed = 0.55f;
                break;
            case SCATTER_WAND:
                spells = new SpellType[]{SpellType.SPARK, SpellType.SPARK,
                                         SpellType.SPARK, SpellType.SPARK, SpellType.SPARK};
                castCooldownMs  = 60;
                manaPerCast     = 1;
                projectileSpeed = 0.45f;
                break;
            case METEOR_WAND:
                spells = new SpellType[]{SpellType.FIREBALL, SpellType.EXPLODE,
                                         SpellType.FIREBALL, SpellType.EXPLODE};
                castCooldownMs  = 700;
                manaPerCast     = 22;
                projectileSpeed = 0.26f;
                break;
            default: // BASIC
                spells = new SpellType[]{SpellType.MAGIC_MISSILE};
                castCooldownMs  = 280;
                manaPerCast     = 5;
                projectileSpeed = 0.42f;
                break;
        }
    }

    // =========================================================================
    // Game-loop integration
    // =========================================================================

    /**
     * Decrements the fire cooldown timer.
     *
     * @param elapsed Milliseconds since last update
     */
    public void update(long elapsed) {
        if (cooldownRemaining > 0) {
            cooldownRemaining = Math.max(0, cooldownRemaining - elapsed);
        }
    }

    /**
     * Attempts to cast a spell towards the given target world position.
     * Returns a Projectile if the wand is ready and the caster has enough mana;
     * returns null otherwise.
     *
     * @param spawnX   World X of the projectile origin
     * @param spawnY   World Y of the projectile origin
     * @param targetX  World X of the aim target
     * @param targetY  World Y of the aim target
     * @param mana     Current mana of the caster
     * @return A new Projectile, or null if the cast failed
     */
    public Projectile tryCast(float spawnX, float spawnY,
                               float targetX, float targetY, int mana) {
        if (cooldownRemaining > 0 || mana < manaPerCast) return null;

        SpellType spellType = spells[spellIndex];
        spellIndex = (spellIndex + 1) % spells.length;

        // Compute normalised direction vector
        float dx = targetX - spawnX;
        float dy = targetY - spawnY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;
        float vx = (dx / len) * projectileSpeed;
        float vy = (dy / len) * projectileSpeed;

        // Add slight random spread for CHAOS wand
        if (wandType == WandType.CHAOS_WAND) {
            vx += (float)(Math.random() - 0.5) * 0.06f;
            vy += (float)(Math.random() - 0.5) * 0.06f;
        }

        Animation anim = spellAnimations.getOrDefault(
                spellType,
                spellAnimations.get(SpellType.MAGIC_MISSILE));

        cooldownRemaining = castCooldownMs;
        return new Projectile(spellType, anim, spawnX, spawnY, vx, vy, true);
    }

    /**
     * Creates an enemy projectile aimed at a target (no mana check).
     * Used by WizardEnemy.
     *
     * @param spawnX  World X origin
     * @param spawnY  World Y origin
     * @param targetX Aim X
     * @param targetY Aim Y
     * @param speed   Projectile speed in px/ms
     * @return New Projectile owned by an enemy
     */
    public static Projectile createEnemyProjectile(SpellType type, Animation anim,
                                                    float spawnX, float spawnY,
                                                    float targetX, float targetY,
                                                    float speed) {
        float dx = targetX - spawnX;
        float dy = targetY - spawnY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;
        float vx = (dx / len) * speed;
        float vy = (dy / len) * speed;
        return new Projectile(type, anim, spawnX, spawnY, vx, vy, false);
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** @return Mana cost per cast */
    public int getManaCost() { return manaPerCast; }

    /** @return True if the wand is ready to fire */
    public boolean isReady() { return cooldownRemaining == 0; }

    /** @return The wand type */
    public WandType getType() { return wandType; }

    /**
     * @return Cooldown fraction 0.0 (ready) to 1.0 (just fired),
     *         useful for the HUD charge indicator.
     */
    public float getCooldownFraction() {
        return (float) cooldownRemaining / castCooldownMs;
    }

    /** @return Human-readable display name for the HUD */
    public String getDisplayName() {
        switch (wandType) {
            case FIRE_WAND:      return "Fire Wand";
            case LIGHTNING_WAND: return "Lightning";
            case CHAOS_WAND:     return "Chaos Wand";
            case EXPLOSION_WAND: return "Boom Wand";
            case SPARK_WAND:     return "Spark Wand";
            case SNIPER_WAND:    return "Sniper Wand";
            case INFERNO_WAND:   return "Inferno Wand";
            case STORM_WAND:     return "Storm Wand";
            case GATLING_WAND:   return "Gatling Wand";
            case DEMOLITION_WAND:return "Demo Wand";
            case ARCANE_WAND:    return "Arcane Wand";
            case HELLFIRE_WAND:  return "Hellfire Wand";
            case TEMPEST_WAND:   return "Tempest Wand";
            case NOVA_WAND:      return "Nova Wand";
            case MYSTIC_WAND:    return "Mystic Wand";
            case SCATTER_WAND:   return "Scatter Wand";
            case METEOR_WAND:    return "Meteor Wand";
            default:             return "Basic Wand";
        }
    }
}
