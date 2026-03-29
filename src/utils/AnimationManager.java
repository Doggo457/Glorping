package utils;

import game2D.Animation;
import spells.SpellType;

import java.awt.Image;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Centralises the loading and storage of every animation and background image
 * used by the game.  Constructed once during start-up and queried by any
 * subsystem that needs a shared animation reference.
 *
 * <p>All sprite-sheet paths are resolved relative to the working directory
 * using the prefix {@code "src/resources/images/"}.  Single-frame pickup and
 * NPC animations, as well as background images, are loaded through an
 * {@code imageLoader} function supplied at construction time (typically
 * {@code GameCore::loadImage}).</p>
 */
public class AnimationManager {

    /** Common resource path prefix for all image assets. */
    private static final String IMG = "src/resources/images/";

    // -----------------------------------------------------------------
    // Image loader (wraps GameCore.loadImage which is protected)
    // -----------------------------------------------------------------

    private final Function<String, Image> imageLoader;

    // -----------------------------------------------------------------
    // Player animations
    // -----------------------------------------------------------------

    private Animation animPlayerIdle;
    private Animation animPlayerWalk;
    private Animation animPlayerJump;
    private Animation animPlayerCast;
    private Animation animPlayerDeath;
    private Animation animPlayerWallGrab;

    // -----------------------------------------------------------------
    // Enemy animations
    // -----------------------------------------------------------------

    private Animation[] animSlime      = new Animation[2];
    private Animation   animBatUp;
    private Animation   animBatDown;
    private Animation   animWizIdle;
    private Animation   animWizCast;
    private Animation[] animSkeleton   = new Animation[2];
    private Animation[] animSpider     = new Animation[2];
    private Animation   animGhost;
    private Animation[] animFireSpirit = new Animation[2];
    private Animation   animEnemyMissile;

    // -----------------------------------------------------------------
    // Spell projectile animations (keyed by SpellType)
    // -----------------------------------------------------------------

    private Map<SpellType, Animation> spellAnims = new EnumMap<>(SpellType.class);

    // -----------------------------------------------------------------
    // Pickup animations
    // -----------------------------------------------------------------

    private Animation animHealthPot;
    private Animation animManaPot;
    private Animation animWandItem;
    private Animation animGoldNugget;

    // -----------------------------------------------------------------
    // NPC animation
    // -----------------------------------------------------------------

    private Animation animNpc;

    // -----------------------------------------------------------------
    // Parallax background layers
    // -----------------------------------------------------------------

    private Image bgFar;
    private Image bgMid;
    private Image bgNear;

    // =================================================================
    // Constructor
    // =================================================================

    /**
     * Creates a new {@code AnimationManager}.
     *
     * @param imageLoader a function that loads an {@link Image} from a path
     *                    string (typically {@code GameCore::loadImage})
     */
    public AnimationManager(Function<String, Image> imageLoader) {
        this.imageLoader = imageLoader;
    }

    // =================================================================
    // Public entry point
    // =================================================================

    /**
     * Loads every animation and background image used by the game.
     * Must be called once during initialisation before any getters are used.
     */
    public void loadAll() {
        loadPlayerAnimations();
        loadEnemyAnimations();
        loadSpellAnimations();
        loadPickupAnimations();
        loadNpcAnimation();
        loadBackgrounds();
    }

    // =================================================================
    // Private loaders
    // =================================================================

    private void loadPlayerAnimations() {
        animPlayerIdle     = new Animation();
        animPlayerWalk     = new Animation();
        animPlayerJump     = new Animation();
        animPlayerCast     = new Animation();
        animPlayerDeath    = new Animation();
        animPlayerWallGrab = new Animation();

        animPlayerIdle.loadAnimationSeries(    IMG +"wizard.png", 4, 6, 150, 0, 4);
        animPlayerWalk.loadAnimationSeries(    IMG +"wizard.png", 4, 6, 120, 4, 4);
        animPlayerJump.loadAnimationSeries(    IMG +"wizard.png", 4, 6, 180, 8, 4);
        animPlayerCast.loadAnimationSeries(    IMG +"wizard.png", 4, 6, 100, 12, 4);
        animPlayerDeath.loadAnimationSeries(   IMG +"wizard.png", 4, 6, 200, 8, 4);
        animPlayerDeath.setLoop(false);
        animPlayerWallGrab.loadAnimationSeries(IMG +"wizard.png", 4, 6, 200, 16, 4);
    }

    private void loadEnemyAnimations() {
        animSlime[0] = new Animation();
        animSlime[1] = new Animation();
        animSlime[0].loadAnimationSeries(IMG +"slime.png", 2, 1, 300, 0, 1);
        animSlime[1].loadAnimationSeries(IMG +"slime.png", 2, 1, 200, 1, 1);

        animBatUp   = new Animation();
        animBatDown = new Animation();
        animBatUp.loadAnimationSeries(  IMG +"bat.png", 2, 1, 200, 0, 1);
        animBatDown.loadAnimationSeries(IMG +"bat.png", 2, 1, 200, 1, 1);

        animWizIdle = new Animation();
        animWizCast = new Animation();
        animWizIdle.loadAnimationSeries(IMG +"wiz_enemy.png", 2, 1, 400, 0, 1);
        animWizCast.loadAnimationSeries(IMG +"wiz_enemy.png", 2, 1, 150, 1, 1);

        animEnemyMissile = new Animation();
        animEnemyMissile.loadAnimationSeries(IMG +"magic_missile.png", 2, 1, 150, 0, 2);

        animSkeleton[0] = new Animation();
        animSkeleton[1] = new Animation();
        animSkeleton[0].loadAnimationSeries(IMG +"skeleton.png", 2, 1, 300, 0, 1);
        animSkeleton[1].loadAnimationSeries(IMG +"skeleton.png", 2, 1, 200, 1, 1);

        animSpider[0] = new Animation();
        animSpider[1] = new Animation();
        animSpider[0].loadAnimationSeries(IMG +"spider.png", 2, 1, 200, 0, 1);
        animSpider[1].loadAnimationSeries(IMG +"spider.png", 2, 1, 150, 1, 1);

        animGhost = new Animation();
        animGhost.loadAnimationSeries(IMG +"ghost.png", 2, 1, 400, 0, 2);

        animFireSpirit[0] = new Animation();
        animFireSpirit[1] = new Animation();
        animFireSpirit[0].loadAnimationSeries(IMG +"fire_spirit.png", 2, 1, 150, 0, 2);
        animFireSpirit[1].loadAnimationSeries(IMG +"fireball.png",    2, 1, 120, 0, 2);
    }

    private void loadSpellAnimations() {
        for (SpellType t : SpellType.values()) {
            Animation a = new Animation();
            switch (t) {
                case FIREBALL:
                    a.loadAnimationSeries(IMG +"fireball.png",      2, 1, 120, 0, 2); break;
                case LIGHTNING_BOLT:
                    a.loadAnimationSeries(IMG +"lightning.png",     2, 1, 80,  0, 2); break;
                case MAGIC_MISSILE:
                    a.loadAnimationSeries(IMG +"magic_missile.png", 2, 1, 150, 0, 2); break;
                case EXPLODE:
                    a.loadAnimationSeries(IMG +"explosion.png",     4, 1, 100, 0, 4);
                    a.setLoop(false);
                    break;
                default:
                    a.loadAnimationSeries(IMG +"spark.png",         2, 1, 100, 0, 2); break;
            }
            spellAnims.put(t, a);
        }
    }

    private void loadPickupAnimations() {
        animHealthPot = new Animation();
        animHealthPot.addFrame(imageLoader.apply(IMG +"health_potion.png"), 1000);

        animManaPot = new Animation();
        animManaPot.addFrame(imageLoader.apply(IMG +"mana_potion.png"), 1000);

        animWandItem = new Animation();
        animWandItem.addFrame(imageLoader.apply(IMG +"wand_item.png"), 1000);

        animGoldNugget = new Animation();
        animGoldNugget.addFrame(imageLoader.apply(IMG +"gold_nugget.png"), 1000);
    }

    private void loadNpcAnimation() {
        animNpc = new Animation();
        animNpc.addFrame(imageLoader.apply(IMG +"npc_shopkeeper.png"), 1000);
    }

    private void loadBackgrounds() {
        bgFar  = imageLoader.apply(IMG +"bg_far.png");
        bgMid  = imageLoader.apply(IMG +"bg_mid.png");
        bgNear = imageLoader.apply(IMG +"bg_near.png");
    }

    // =================================================================
    // Getters -- Player
    // =================================================================

    /** @return idle animation for the player character */
    public Animation getPlayerIdle()     { return animPlayerIdle; }
    /** @return walking animation for the player character */
    public Animation getPlayerWalk()     { return animPlayerWalk; }
    /** @return jumping animation for the player character */
    public Animation getPlayerJump()     { return animPlayerJump; }
    /** @return casting animation for the player character */
    public Animation getPlayerCast()     { return animPlayerCast; }
    /** @return death animation for the player character */
    public Animation getPlayerDeath()    { return animPlayerDeath; }
    /** @return wall-grab animation for the player character */
    public Animation getPlayerWallGrab() { return animPlayerWallGrab; }

    // =================================================================
    // Getters -- Enemies
    // =================================================================

    /** @return two-element array of slime animations (idle, move) */
    public Animation[] getSlime()        { return animSlime; }
    /** @return bat wings-up animation */
    public Animation getBatUp()          { return animBatUp; }
    /** @return bat wings-down animation */
    public Animation getBatDown()        { return animBatDown; }
    /** @return wizard enemy idle animation */
    public Animation getWizIdle()        { return animWizIdle; }
    /** @return wizard enemy casting animation */
    public Animation getWizCast()        { return animWizCast; }
    /** @return two-element array of skeleton animations (idle, move) */
    public Animation[] getSkeleton()     { return animSkeleton; }
    /** @return two-element array of spider animations (idle, move) */
    public Animation[] getSpider()       { return animSpider; }
    /** @return ghost animation */
    public Animation getGhost()          { return animGhost; }
    /** @return two-element array of fire spirit animations (body, fireball) */
    public Animation[] getFireSpirit()   { return animFireSpirit; }
    /** @return enemy missile projectile animation */
    public Animation getEnemyMissile()   { return animEnemyMissile; }

    // =================================================================
    // Getters -- Spells
    // =================================================================

    /** @return map of spell type to projectile animation */
    public Map<SpellType, Animation> getSpellAnims() { return spellAnims; }

    // =================================================================
    // Getters -- Pickups
    // =================================================================

    /** @return health potion pickup animation */
    public Animation getHealthPot()   { return animHealthPot; }
    /** @return mana potion pickup animation */
    public Animation getManaPot()     { return animManaPot; }
    /** @return wand item pickup animation */
    public Animation getWandItem()    { return animWandItem; }
    /** @return gold nugget pickup animation */
    public Animation getGoldNugget()  { return animGoldNugget; }

    // =================================================================
    // Getters -- NPC
    // =================================================================

    /** @return shopkeeper NPC animation */
    public Animation getNpc() { return animNpc; }

    // =================================================================
    // Getters -- Backgrounds
    // =================================================================

    /** @return far parallax background layer */
    public Image getBgFar()  { return bgFar; }
    /** @return mid parallax background layer */
    public Image getBgMid()  { return bgMid; }
    /** @return near parallax background layer */
    public Image getBgNear() { return bgNear; }
}
