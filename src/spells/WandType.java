package spells;

/**
 * Defines the different wand archetypes the player can find.
 * Each type loads a different set of spells with different stats.
 */
public enum WandType {
    /** Starter wand - fires magic missiles, moderate speed and cost */
    BASIC,
    /** Fire wand - fireballs and sparks, affected by gravity */
    FIRE_WAND,
    /** Lightning wand - fast high-damage bolts, high mana cost */
    LIGHTNING_WAND,
    /** Chaos wand - random spells from all types, rapid fire */
    CHAOS_WAND,
    /** Explosion wand - slow but devastating AoE blasts */
    EXPLOSION_WAND,
    /** Spark wand - rapid-fire weak sparks, very low mana cost */
    SPARK_WAND,
    /** Sniper wand - extremely fast single shots, long cooldown */
    SNIPER_WAND,
    /** Inferno wand - alternating fireballs and explosions */
    INFERNO_WAND,
    /** Storm wand - rapid lightning and spark bursts */
    STORM_WAND,
    /** Gatling wand - ultra-fast weak magic missiles */
    GATLING_WAND,
    /** Demolition wand - double explosions, high mana cost */
    DEMOLITION_WAND,
    /** Arcane wand - slow but powerful magic missiles */
    ARCANE_WAND,
    /** Hellfire wand - rapid stream of fireballs */
    HELLFIRE_WAND,
    /** Tempest wand - alternating lightning and sparks */
    TEMPEST_WAND,
    /** Nova wand - explosion followed by spark scatter */
    NOVA_WAND,
    /** Mystic wand - magic missile and lightning combo */
    MYSTIC_WAND,
    /** Scatter wand - rapid-fire sparks, very cheap */
    SCATTER_WAND,
    /** Meteor wand - heavy fireball and explosion barrage */
    METEOR_WAND
}
