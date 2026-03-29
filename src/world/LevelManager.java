package world;

import entities.Player;
import game2D.Animation;
import projectiles.Projectile;
import sound.MusicPlayer;
import sound.SoundManager;
import spells.SpellType;
import spells.Wand;
import spells.WandType;
import ui.GameState;
import ui.HUD;

import java.util.Map;

/**
 * Manages level lifecycle, transitions, and the between-level shop.
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *   <li>Loading and storing all {@link Level} instances</li>
 *   <li>Advancing to the next level (or triggering a win)</li>
 *   <li>Presenting a random shop wand between levels</li>
 *   <li>Processing shop purchases (HP, MP, heal, wand)</li>
 *   <li>Restarting the game from level 1</li>
 *   <li>Placing the player at the current level's spawn point</li>
 * </ul>
 *
 * <p>This class was extracted from {@code GlorpingGame} to reduce its
 * size and isolate level-management concerns.</p>
 */
public class LevelManager {

    // =========================================================================
    // Constants
    // =========================================================================

    /** Total number of levels in the game. */
    public static final int TOTAL_LEVELS = 5;

    // =========================================================================
    // Fields
    // =========================================================================

    /** Array of all game levels, indexed 0 to {@link #TOTAL_LEVELS} - 1. */
    private final Level[] levels;

    /** Index of the level the player is currently on (0-based). */
    private int currentLevelIndex = 0;

    /** Random wand offered for sale in the shop between levels. */
    private Wand shopWand = null;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates the level manager and loads all level maps from disk.
     *
     * @param particles Shared particle system passed to each {@link Level}
     */
    public LevelManager(ParticleSystem particles) {
        levels = new Level[TOTAL_LEVELS];
        for (int i = 0; i < TOTAL_LEVELS; i++) {
            String mapFile = "level" + (i + 1) + ".txt";
            levels[i] = new Level("src/resources/maps", mapFile, i, particles);
        }
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * Sets the gold nugget and wand item animations on every level.
     * Call this once after all animations have been loaded.
     *
     * @param goldAnim Animation for gold nugget pickups dropped by enemies
     * @param wandAnim Animation for wand item pickups on the ground
     */
    public void initLevels(Animation goldAnim, Animation wandAnim) {
        for (Level lv : levels) {
            lv.setGoldNuggetAnim(goldAnim);
            lv.setWandItemAnim(wandAnim);
        }
    }

    // =========================================================================
    // Population
    // =========================================================================

    /**
     * Populates the current level with enemies and pickups using the
     * provided animation assets.
     *
     * @param player       Player reference for enemy targeting
     * @param slime        Slime animation pair [normal, squished]
     * @param batUp        Bat wings-up animation
     * @param batDown      Bat wings-down animation
     * @param wizIdle      Enemy wizard idle animation
     * @param wizCast      Enemy wizard cast animation
     * @param enemyMissile Animation for wizard projectiles
     * @param skeleton     Skeleton animation pair [walk, attack]
     * @param spider       Spider animation pair [walk, attack]
     * @param ghost        Ghost animation
     * @param fireSpirit   Fire spirit animation pair [idle, attack]
     * @param healthPot    Health potion animation
     * @param manaPot      Mana potion animation
     * @param wandItem     Wand item animation
     * @param spellAnims   Spell type to animation map for wand pickups
     */
    public void populateCurrentLevel(Player player,
                                     Animation[] slime,
                                     Animation batUp, Animation batDown,
                                     Animation wizIdle, Animation wizCast,
                                     Animation enemyMissile,
                                     Animation[] skeleton,
                                     Animation[] spider,
                                     Animation ghost,
                                     Animation[] fireSpirit,
                                     Animation healthPot, Animation manaPot,
                                     Animation wandItem,
                                     Map<SpellType, Animation> spellAnims) {
        levels[currentLevelIndex].populate(player,
                slime, batUp, batDown, wizIdle, wizCast, enemyMissile,
                skeleton, spider, ghost, fireSpirit,
                healthPot, manaPot, wandItem, spellAnims);
    }

    // =========================================================================
    // Level access
    // =========================================================================

    /**
     * Returns the level the player is currently on.
     *
     * @return The active {@link Level} instance
     */
    public Level getCurrentLevel() {
        return levels[currentLevelIndex];
    }

    /**
     * Returns the 0-based index of the current level.
     *
     * @return Current level index
     */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    /**
     * Returns the total number of levels in the game.
     *
     * @return {@link #TOTAL_LEVELS}
     */
    public int getTotalLevels() {
        return TOTAL_LEVELS;
    }

    // =========================================================================
    // Level advancement
    // =========================================================================

    /**
     * Advances to the next level. If the player has completed all levels,
     * returns {@link GameState#WIN}. Otherwise generates a random shop wand
     * and returns {@link GameState#SHOP}.
     *
     * @param sounds     Sound manager for the level-complete jingle
     * @param music      Music player to stop on win
     * @param spellAnims Spell animations needed to create the shop wand
     * @return {@code GameState.WIN} if all levels are done,
     *         {@code GameState.SHOP} otherwise
     */
    public GameState advanceLevel(SoundManager sounds, MusicPlayer music,
                                  Map<SpellType, Animation> spellAnims) {
        currentLevelIndex++;
        if (currentLevelIndex >= TOTAL_LEVELS) {
            currentLevelIndex = TOTAL_LEVELS - 1;
            music.stopMusic();
            sounds.playWithEcho("pickup");
            return GameState.WIN;
        }
        WandType[] allWands = WandType.values();
        WandType shopType = allWands[(int)(Math.random() * allWands.length)];
        shopWand = new Wand(shopType, spellAnims);
        sounds.playWithEcho("pickup");
        return GameState.SHOP;
    }

    // =========================================================================
    // Shop continuation
    // =========================================================================

    /**
     * Called when the player clicks "Continue" in the shop screen.
     * Resets and regenerates the next level, populates it with enemies
     * and pickups, places the player at the spawn point, displays a
     * level message, and restarts the background music.
     *
     * @param player       Player to reposition
     * @param particles    Particle system to clear
     * @param goldAnim     Gold nugget animation for enemy drops
     * @param wandAnim     Wand item animation for ground pickups
     * @param slime        Slime animation pair
     * @param batUp        Bat wings-up animation
     * @param batDown      Bat wings-down animation
     * @param wizIdle      Wizard idle animation
     * @param wizCast      Wizard cast animation
     * @param enemyMissile Wizard projectile animation
     * @param skeleton     Skeleton animation pair
     * @param spider       Spider animation pair
     * @param ghost        Ghost animation
     * @param fireSpirit   Fire spirit animation pair
     * @param healthPot    Health potion animation
     * @param manaPot      Mana potion animation
     * @param wandItem     Wand item animation
     * @param spellAnims   Spell type to animation map
     * @param hud          HUD for displaying the level message
     * @param music        Music player to restart
     * @param musicEnabled Whether music should be started
     */
    public void continueFromShop(Player player, ParticleSystem particles,
                                 Animation goldAnim, Animation wandAnim,
                                 Animation[] slime,
                                 Animation batUp, Animation batDown,
                                 Animation wizIdle, Animation wizCast,
                                 Animation enemyMissile,
                                 Animation[] skeleton,
                                 Animation[] spider,
                                 Animation ghost,
                                 Animation[] fireSpirit,
                                 Animation healthPot, Animation manaPot,
                                 Animation wandItem,
                                 Map<SpellType, Animation> spellAnims,
                                 HUD hud, MusicPlayer music,
                                 boolean musicEnabled) {
        particles.clear();
        Level next = levels[currentLevelIndex];
        next.reset();
        next.regenerate();
        next.setGoldNuggetAnim(goldAnim);
        next.setWandItemAnim(wandAnim);
        next.populate(player,
                slime, batUp, batDown, wizIdle, wizCast, enemyMissile,
                skeleton, spider, ghost, fireSpirit,
                healthPot, manaPot, wandItem, spellAnims);

        player.setPosition(next.getSpawnX(), next.getSpawnY());
        player.setVelocity(0, 0);
        hud.showMessage("Level " + (currentLevelIndex + 1) + "!", 2000);
        music.stopMusic();
        if (musicEnabled) music.startMusic();
    }

    // =========================================================================
    // Game restart
    // =========================================================================

    /**
     * Fully restarts the game from level 1. Resets all levels, regenerates
     * their terrain, sets pickup animations, and populates the first level.
     *
     * <p>The caller is responsible for creating a fresh {@link Player}
     * instance before calling this method.</p>
     *
     * @param player       Freshly created player
     * @param particles    Particle system to clear
     * @param goldAnim     Gold nugget animation
     * @param wandAnim     Wand item animation
     * @param slime        Slime animation pair
     * @param batUp        Bat wings-up animation
     * @param batDown      Bat wings-down animation
     * @param wizIdle      Wizard idle animation
     * @param wizCast      Wizard cast animation
     * @param enemyMissile Wizard projectile animation
     * @param skeleton     Skeleton animation pair
     * @param spider       Spider animation pair
     * @param ghost        Ghost animation
     * @param fireSpirit   Fire spirit animation pair
     * @param healthPot    Health potion animation
     * @param manaPot      Mana potion animation
     * @param wandItem     Wand item animation
     * @param spellAnims   Spell type to animation map
     * @param music        Music player to restart
     * @param musicEnabled Whether music should be started
     */
    public void restartGame(Player player, ParticleSystem particles,
                            Animation goldAnim, Animation wandAnim,
                            Animation[] slime,
                            Animation batUp, Animation batDown,
                            Animation wizIdle, Animation wizCast,
                            Animation enemyMissile,
                            Animation[] skeleton,
                            Animation[] spider,
                            Animation ghost,
                            Animation[] fireSpirit,
                            Animation healthPot, Animation manaPot,
                            Animation wandItem,
                            Map<SpellType, Animation> spellAnims,
                            MusicPlayer music, boolean musicEnabled) {
        currentLevelIndex = 0;
        particles.clear();

        for (Level lv : levels) {
            lv.reset();
            lv.regenerate();
        }

        for (Level lv : levels) {
            lv.setGoldNuggetAnim(goldAnim);
            lv.setWandItemAnim(wandAnim);
        }

        levels[0].populate(player,
                slime, batUp, batDown, wizIdle, wizCast, enemyMissile,
                skeleton, spider, ghost, fireSpirit,
                healthPot, manaPot, wandItem, spellAnims);

        music.stopMusic();
        if (musicEnabled) music.startMusic();
    }

    // =========================================================================
    // Shop purchases
    // =========================================================================

    /**
     * Processes a shop upgrade purchase. Supports three upgrade types:
     * <ul>
     *   <li>{@code "hp"}   — Increases max health by 20.
     *       Cost: {@code 20 + (maxHealth - 100) / 10 * 5} gold.</li>
     *   <li>{@code "mp"}   — Increases max mana by 20.
     *       Cost: {@code 15 + (maxMana - 50) / 10 * 5} gold.</li>
     *   <li>{@code "heal"} — Fully restores health and mana.
     *       Cost: 10 gold.</li>
     * </ul>
     *
     * @param type   Upgrade type: "hp", "mp", or "heal"
     * @param player Player to apply the upgrade to
     * @param hud    HUD for displaying a purchase confirmation message
     * @param sounds Sound manager for the purchase sound effect
     */
    public void buyUpgrade(String type, Player player, HUD hud, SoundManager sounds) {
        switch (type) {
            case "hp": {
                int cost = 20 + (player.getMaxHealth() - 100) / 10 * 5;
                if (player.spendGold(cost)) {
                    player.increaseMaxHealth(20);
                    hud.showMessage("+20 Max HP!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
            case "mp": {
                int cost = 15 + (player.getMaxMana() - 50) / 10 * 5;
                if (player.spendGold(cost)) {
                    player.increaseMaxMana(20);
                    hud.showMessage("+20 Max Mana!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
            case "heal": {
                int cost = 10;
                if (player.spendGold(cost)) {
                    player.heal(player.getMaxHealth());
                    player.restoreMana(player.getMaxMana());
                    hud.showMessage("Fully Healed!", 1500);
                    sounds.play("pickup");
                }
                break;
            }
        }
    }

    /**
     * Attempts to buy the shop wand currently on offer. If the player's
     * wand inventory is full (4 wands), the active wand is swapped out.
     * The wand can only be purchased once per shop visit.
     *
     * <p>Cost formula: {@code 15 + wandManaCost * 2} gold.</p>
     *
     * @param player Player buying the wand
     * @param hud    HUD for displaying a purchase message
     * @param sounds Sound manager for the purchase sound effect
     */
    public void buyShopWand(Player player, HUD hud, SoundManager sounds) {
        if (shopWand == null) return;
        int cost = 15 + shopWand.getManaCost() * 2;
        if (player.spendGold(cost)) {
            if (player.getWands().size() >= 4) {
                // Inventory full — swap out the currently active wand
                Wand old = player.swapActiveWand(shopWand);
                hud.showMessage("Swapped " + old.getDisplayName() + " for "
                        + shopWand.getDisplayName() + "!", 2000);
            } else {
                player.addWand(shopWand);
                hud.showMessage("Bought " + shopWand.getDisplayName() + "!", 1500);
            }
            sounds.play("pickup");
            shopWand = null; // can only buy once per shop visit
        }
    }

    /**
     * Returns the wand currently offered in the shop, or {@code null}
     * if it has already been purchased or no shop is active.
     *
     * @return The shop wand, or {@code null}
     */
    public Wand getShopWand() {
        return shopWand;
    }

    // =========================================================================
    // Player positioning
    // =========================================================================

    /**
     * Places the player at the current level's spawn point and snaps
     * the camera to centre on the player immediately. This avoids
     * the camera smoothly panning from a previous position.
     *
     * @param player Player to reposition
     * @param camera Camera to snap to the new position
     */
    public void placePlayerAtSpawn(Player player, Camera camera) {
        Level lv = levels[currentLevelIndex];
        player.setPosition(lv.getSpawnX(), lv.getSpawnY());
        player.setVelocity(0, 0);
        camera.snapTo(player.getX(), player.getY(),
                lv.getWorldWidth(), lv.getWorldHeight());
    }
}
