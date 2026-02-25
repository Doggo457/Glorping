package spells;

/**
 * Defines the different types of spells that can be cast by wands.
 * Each type has distinct damage, speed, and behaviour properties
 * configured in the Projectile class.
 */
public enum SpellType {
    /** Standard magic missile - fast, low damage, no gravity */
    MAGIC_MISSILE,
    /** Fireball - medium speed, arcs under gravity, moderate damage */
    FIREBALL,
    /** Lightning bolt - very fast, high damage, pierces targets */
    LIGHTNING_BOLT,
    /** Explosion spell - creates large AoE blast on impact */
    EXPLODE,
    /** Spark - fast weak scatter shot, affected by gravity */
    SPARK
}
