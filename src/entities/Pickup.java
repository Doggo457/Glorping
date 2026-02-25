package entities;

import game2D.Animation;
import sound.SoundManager;
import spells.Wand;

/**
 * A collectible item the player can walk over to gain health, mana,
 * a new wand, gold, or shop upgrades.
 * Pickups hover in place (no gravity) and bob slightly for visibility.
 */
public class Pickup extends Entity {

    /** Types of collectible item */
    public enum PickupType {
        HEALTH_POTION, MANA_POTION, WAND, GOLD_NUGGET,
        SHOP_HEAL, SHOP_MAX_HP, SHOP_MAX_MANA, SHOP_RESTORE
    }

    private final PickupType pickupType;
    /** For WAND pickups: the wand to give the player */
    private final Wand wand;
    /** Whether this pickup has already been collected */
    private boolean collected = false;
    /** Bob animation accumulator */
    private float bobPhase = (float)(Math.random() * Math.PI * 2);
    /** World Y at rest (bob oscillates around this) */
    private final float baseY;
    /** Gold cost for shop items (0 = free) */
    private final int goldCost;
    /** Price label displayed above shop items */
    private final String priceLabel;

    /**
     * Constructs a potion or gold nugget pickup.
     *
     * @param anim   Item animation
     * @param spawnX World X
     * @param spawnY World Y
     * @param type   Pickup type
     */
    public Pickup(Animation anim, float spawnX, float spawnY, PickupType type) {
        super(anim);
        this.pickupType = type;
        this.wand = null;
        this.baseY = spawnY;
        this.goldCost = 0;
        this.priceLabel = null;
        health = maxHealth = 1;
        setPosition(spawnX, spawnY);
    }

    /**
     * Constructs a wand pickup.
     *
     * @param anim   Item animation
     * @param spawnX World X
     * @param spawnY World Y
     * @param wand   Wand to award on collection
     */
    public Pickup(Animation anim, float spawnX, float spawnY, Wand wand) {
        super(anim);
        this.pickupType = PickupType.WAND;
        this.wand = wand;
        this.baseY = spawnY;
        this.goldCost = 0;
        this.priceLabel = null;
        health = maxHealth = 1;
        setPosition(spawnX, spawnY);
    }

    /**
     * Constructs a shop item pickup with a gold cost.
     *
     * @param anim     Item animation
     * @param spawnX   World X
     * @param spawnY   World Y
     * @param type     Shop pickup type
     * @param cost     Gold cost to purchase
     * @param label    Label text shown above the item
     */
    public Pickup(Animation anim, float spawnX, float spawnY,
                  PickupType type, int cost, String label) {
        super(anim);
        this.pickupType = type;
        this.wand = null;
        this.baseY = spawnY;
        this.goldCost = cost;
        this.priceLabel = label;
        health = maxHealth = 1;
        setPosition(spawnX, spawnY);
    }

    /** Pickups float, so gravity must not be applied. */
    @Override
    protected boolean isFlying() { return true; }

    /**
     * Updates the bob animation. The item slowly oscillates up and down.
     *
     * @param elapsed Milliseconds since last update
     */
    @Override
    public void update(long elapsed) {
        if (collected) return;
        bobPhase += 0.002f * elapsed;
        setY(baseY + (float) Math.sin(bobPhase) * 4f);
        getAnimation().update(elapsed);
    }

    /**
     * Applies the pickup effect to the player.
     * Shop items require enough gold; if the player can't afford it
     * the pickup is not collected and stays in the world.
     *
     * @param player Player to apply the effect to
     * @param sounds SoundManager for the pickup jingle
     * @return A feedback message to display, or null
     */
    public String collect(Player player, SoundManager sounds) {
        if (collected) return null;

        // Shop items require gold payment
        if (goldCost > 0 && !player.spendGold(goldCost)) {
            return null; // Can't afford – item stays
        }

        collected = true;
        String msg = null;
        switch (pickupType) {
            case HEALTH_POTION:
                player.heal(35);
                sounds.play("pickup");
                msg = "+35 HP";
                break;
            case MANA_POTION:
                player.restoreMana(50);
                sounds.play("pickup");
                msg = "+50 Mana";
                break;
            case WAND:
                if (wand != null) {
                    player.addWand(wand);
                    sounds.play("pickup");
                    msg = "New wand!";
                }
                break;
            case GOLD_NUGGET:
                player.addGold(5);
                sounds.play("pickup");
                msg = "+5 Gold";
                break;
            case SHOP_HEAL:
                player.heal(player.getMaxHealth());
                sounds.play("pickup");
                msg = "Fully healed!";
                break;
            case SHOP_MAX_HP:
                player.increaseMaxHealth(25);
                sounds.play("pickup");
                msg = "+25 Max HP!";
                break;
            case SHOP_MAX_MANA:
                player.increaseMaxMana(30);
                sounds.play("pickup");
                msg = "+30 Max Mana!";
                break;
            case SHOP_RESTORE:
                player.heal(player.getMaxHealth());
                player.restoreMana(player.getMaxMana());
                sounds.play("pickup");
                msg = "Fully restored!";
                break;
        }
        hide();
        return msg;
    }

    /** Marks this pickup as collected without applying its effect. Used by wand swap logic. */
    public void dismiss() {
        collected = true;
        hide();
    }

    /** @return True if this pickup has already been collected */
    public boolean isCollected() { return collected; }

    /** @return The type of pickup */
    public PickupType getPickupType() { return pickupType; }

    /** @return Gold cost (0 for free pickups) */
    public int getGoldCost() { return goldCost; }

    /** @return Price label text, or null if free */
    public String getPriceLabel() { return priceLabel; }

    /** @return The wand stored in this pickup, or null if not a wand pickup */
    public Wand getWand() { return wand; }
}
