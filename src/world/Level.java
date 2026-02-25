package world;

import entities.*;
import game2D.*;
import physics.CollisionHandler;
import projectiles.Projectile;
import sound.SoundManager;
import spells.SpellType;
import spells.Wand;
import spells.WandType;
import utils.MapGenerator;
import utils.MathUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Represents a single playable level, encapsulating the tile map,
 * enemies, pickups, and the cellular automata simulation.
 *
 * Level handles all inter-object interactions:
 * <ul>
 *   <li>Projectile vs tile collision (destruction of dirt/gold tiles)</li>
 *   <li>Projectile vs entity collision (damage)</li>
 *   <li>Player vs enemy contact damage</li>
 *   <li>Player vs pickup collection</li>
 *   <li>Player vs exit tile detection</li>
 *   <li>Lava damage to entities</li>
 * </ul>
 */
public class Level {

    // =========================================================================
    // Level data
    // =========================================================================

    private final TileMap tmap = new TileMap();
    private final List<Enemy>      enemies     = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Pickup>     pickups     = new ArrayList<>();
    private final CellularAutomata cellSim     = new CellularAutomata();
    private final ParticleSystem   particles;

    /** Index of this level (0-based) */
    private final int levelIndex;
    /** Map folder and file for reloading */
    private final String mapFolder;
    private final String mapFile;
    /** True once the player has reached the exit tile */
    private boolean exitReached = false;
    /** Player spawn position in world pixels */
    private float spawnX, spawnY;
    /** Tracks which tile positions have been used for enemy/pickup spawns to prevent overlap */
    private final Set<Long> usedSpawnTiles = new HashSet<>();
    /** Most recent pickup feedback message (consumed by GlorpingGame for HUD display) */
    private String lastPickupMessage = null;
    /** Animation used for dynamically spawned gold nugget pickups */
    private Animation goldNuggetAnim = null;

    private static final Font PRICE_FONT = new Font("Monospaced", Font.BOLD, 11);

    /** Tracks accumulated mining damage on tiles. Key = tileKey(tx,ty), value = damage dealt so far. */
    private final Map<Long, Integer> tileDamage = new HashMap<>();
    /** Durability of stone tiles — takes this much mining damage to break */
    private static final int STONE_DURABILITY = 4;
    /** Durability of dirt tiles */
    private static final int DIRT_DURABILITY = 1;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a level and loads its tile map from the given file.
     *
     * @param mapFolder  Path to the maps directory
     * @param mapFile    Map file name within mapFolder
     * @param levelIndex 0-based level number
     * @param particles  Shared particle system
     */
    public Level(String mapFolder, String mapFile, int levelIndex, ParticleSystem particles) {
        this.levelIndex = levelIndex;
        this.mapFolder  = mapFolder;
        this.mapFile    = mapFile;
        this.particles  = particles;

        if (!tmap.loadMap(mapFolder, mapFile)) {
            System.err.println("Level: failed to load map " + mapFolder + "/" + mapFile);
        }

        cellSim.scanMap(tmap);
        computeSpawnPoint();
    }

    // =========================================================================
    // Reset / Regenerate
    // =========================================================================

    /**
     * Clears all runtime state (enemies, projectiles, pickups, exit flag)
     * so the level can be re-populated from scratch.
     */
    public void reset() {
        enemies.clear();
        projectiles.clear();
        pickups.clear();
        usedSpawnTiles.clear();
        tileDamage.clear();
        exitReached = false;
    }

    /**
     * Regenerates the terrain by creating a new random map file,
     * reloading the TileMap, and rescanning liquids.
     * Call {@link #reset()} before this, then {@link #populate} after.
     */
    public void regenerate() {
        MapGenerator.generate(levelIndex);
        if (!tmap.loadMap(mapFolder, mapFile)) {
            System.err.println("Level: failed to reload map " + mapFolder + "/" + mapFile);
        }
        cellSim.scanMap(tmap);
        computeSpawnPoint();
    }

    /** Sets the spawn point based on the cleared spawn zone in the map. */
    private void computeSpawnPoint() {
        spawnX = tmap.getTileWidth()  * 6f;
        spawnY = tmap.getTileHeight() * (tmap.getMapHeight() / 2 - 2);
    }

    // =========================================================================
    // Enemy / pickup population
    // =========================================================================

    /**
     * Populates the level with enemies and pickups based on the level index.
     * Clears any existing entities first, then places enemies at unique
     * open tile positions spread across the map.
     *
     * @param player         Player reference for enemy targeting
     * @param animSlime      Slime animation pair [normal, squished]
     * @param animBatUp      Bat wings-up animation
     * @param animBatDown    Bat wings-down animation
     * @param animWizIdle    Enemy wizard idle animation
     * @param animWizCast    Enemy wizard cast animation
     * @param animMissile    Animation for wizard projectiles
     * @param animHealthPot  Health potion animation
     * @param animManaPot    Mana potion animation
     * @param animWandItem   Wand item animation
     * @param spellAnims     Spell type → animation map for wand pickups
     */
    public void populate(Player player,
                         Animation[] animSlime,
                         Animation animBatUp, Animation animBatDown,
                         Animation animWizIdle, Animation animWizCast,
                         Animation animMissile,
                         Animation animHealthPot, Animation animManaPot,
                         Animation animWandItem,
                         Map<SpellType, Animation> spellAnims) {

        // Clear old entities so they don't pile up across restarts
        reset();

        int mapW = tmap.getMapWidth();
        int mapH = tmap.getMapHeight();
        int tw   = tmap.getTileWidth();
        int th   = tmap.getTileHeight();

        // Scatter slimes evenly across the map
        int slimeCount = 6 + levelIndex * 3;
        int slimeSpacing = Math.max(1, (mapW - 20) / (slimeCount + 1));
        for (int i = 0; i < slimeCount; i++) {
            int startX = 12 + i * slimeSpacing;
            int[] pos = findOpenFloor(startX, mapH);
            if (pos != null) {
                Slime s = new Slime(animSlime[0], animSlime[1], player);
                s.setPosition(pos[0] * tw, pos[1] * th - s.getHeight());
                enemies.add(s);
            }
        }

        // Bats spread across the map in upper cave regions
        int batCount = 3 + levelIndex * 2;
        int batSpacing = Math.max(1, (mapW - 20) / (batCount + 1));
        for (int i = 0; i < batCount; i++) {
            int startX = 15 + i * batSpacing;
            int[] pos = findOpenSpace(startX, mapH / 2);
            if (pos != null) {
                Bat b = new Bat(animBatUp, animBatDown, player);
                b.setPosition(pos[0] * tw, pos[1] * th);
                enemies.add(b);
            }
        }

        // Wizard enemies (ranged) spread across the right two-thirds
        int wizCount = 2 + levelIndex;
        int wizSpacing = Math.max(1, (mapW / 2) / (wizCount + 1));
        for (int i = 0; i < wizCount; i++) {
            int startX = mapW / 3 + i * wizSpacing;
            int[] pos = findOpenFloor(startX, mapH);
            if (pos != null) {
                WizardEnemy w = new WizardEnemy(animWizIdle, animWizCast, animMissile, player);
                w.setPosition(pos[0] * tw, pos[1] * th - w.getHeight());
                enemies.add(w);
            }
        }

        // Pickups
        placePickups(animHealthPot, animManaPot, animWandItem, spellAnims, mapW, mapH, tw, th);
    }

    /** Places health potions, mana potions, and a wand pickup at unique floor positions. */
    private void placePickups(Animation hpAnim, Animation mpAnim, Animation wandAnim,
                               Map<SpellType, Animation> spellAnims,
                               int mapW, int mapH, int tw, int th) {
        // Health potions spread across the map
        for (int i = 0; i < 3; i++) {
            int startX = 10 + i * (mapW / 4);
            int[] pos = findOpenFloor(startX, mapH);
            if (pos != null) {
                pickups.add(new Pickup(hpAnim, pos[0] * tw + tw / 4f,
                        pos[1] * th - 32, Pickup.PickupType.HEALTH_POTION));
            }
        }
        // Mana potions
        for (int i = 0; i < 2; i++) {
            int startX = 18 + i * (mapW / 3);
            int[] pos = findOpenFloor(startX, mapH);
            if (pos != null) {
                pickups.add(new Pickup(mpAnim, pos[0] * tw + tw / 4f,
                        pos[1] * th - 32, Pickup.PickupType.MANA_POTION));
            }
        }
        // Wand pickups: 1-3 random wands scattered across the level
        WandType[] wandPool = {
            WandType.FIRE_WAND, WandType.LIGHTNING_WAND, WandType.CHAOS_WAND,
            WandType.EXPLOSION_WAND, WandType.SPARK_WAND, WandType.SNIPER_WAND,
            WandType.INFERNO_WAND, WandType.STORM_WAND, WandType.GATLING_WAND,
            WandType.DEMOLITION_WAND, WandType.ARCANE_WAND, WandType.HELLFIRE_WAND,
            WandType.TEMPEST_WAND, WandType.NOVA_WAND, WandType.MYSTIC_WAND,
            WandType.SCATTER_WAND, WandType.METEOR_WAND
        };
        Random rng = new Random();
        int wandCount = 1 + rng.nextInt(3); // 1 to 3 wands per level
        for (int i = 0; i < wandCount; i++) {
            int startX = 10 + rng.nextInt(Math.max(1, mapW - 20));
            int[] wpos = findOpenFloor(startX, mapH);
            if (wpos != null) {
                WandType wt = wandPool[rng.nextInt(wandPool.length)];
                Wand w = new Wand(wt, spellAnims);
                pickups.add(new Pickup(wandAnim, wpos[0] * tw + tw / 4f,
                        wpos[1] * th - 32, w));
            }
        }
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates all level entities, checks collisions, and runs the cell sim.
     *
     * @param elapsed Milliseconds since last update
     * @param player  Player reference
     * @param sounds  Sound manager
     */
    public void update(long elapsed, Player player, SoundManager sounds) {
        if (!player.isAlive()) return;

        // Cellular automata
        cellSim.update(elapsed, tmap);

        // Player tile collision
        CollisionHandler.resolveEntityTileCollision(player, tmap);

        // Wall grab detection: check if player is pressing into a wall while airborne
        checkWallGrab(player);

        // Environmental effects (water slowdown, lava damage)
        applyEnvironmentEffect(player);
        checkLavaDamage(player, sounds);

        // Enemies
        updateEnemies(elapsed, player, sounds);

        // Projectiles
        updateProjectiles(elapsed, player, sounds);

        // Pickups
        updatePickups(player, sounds);

        // Exit detection
        checkExit(player);
    }

    /** Updates all enemies, collecting any new projectiles they fire. */
    private void updateEnemies(long elapsed, Player player, SoundManager sounds) {
        List<Pickup> droppedPickups = new ArrayList<>();
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.isAlive()) {
                float cx = e.getX() + e.getWidth() / 2f;
                float cy = e.getY() + e.getHeight() / 2f;
                particles.spawnDeathBurst(cx, cy, Color.GREEN);
                sounds.play("enemy_death");
                // Drop gold nugget pickup
                if (goldNuggetAnim != null) {
                    droppedPickups.add(new Pickup(goldNuggetAnim,
                            cx - 16, cy - 16, Pickup.PickupType.GOLD_NUGGET));
                }
                it.remove();
                continue;
            }
            e.update(elapsed);
            CollisionHandler.resolveEntityTileCollision(e, tmap);
            applyEnvironmentEffect(e);
            e.runAI(tmap);

            // Collect projectiles fired by this enemy
            for (Projectile p : e.drainPendingProjectiles()) {
                projectiles.add(p);
            }

            // Contact damage
            if (CollisionHandler.boundingBoxCollision(player, e)) {
                player.takeDamage(e.getDamage());
                if (!player.isAlive()) sounds.play("player_death");
            }
        }
        // Add dropped pickups after iteration to avoid ConcurrentModificationException
        pickups.addAll(droppedPickups);
    }

    /** Updates all in-flight projectiles and handles collisions. */
    private void updateProjectiles(long elapsed, Player player, SoundManager sounds) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update(elapsed);
            if (!p.isActive()) { it.remove(); continue; }

            // Tile collision
            char tileChar = CollisionHandler.resolveProjectileTileCollision(p, tmap);
            if (tileChar != '.' && tileChar != 'w' && tileChar != 'l' && tileChar != 'e') {
                spawnImpactParticles(p);
                // All projectiles mine tiles; explosions use AoE radius
                if (p.getType() == SpellType.EXPLODE) {
                    destroyTilesAround(p, 1, player);
                    particles.spawnExplosion(p.getCentreX(), p.getCentreY());
                    sounds.playWithEcho("explosion");
                } else {
                    mineTile(p, player);
                }
                p.deactivate();
                it.remove();
                continue;
            }

            // Entity collision
            if (p.isPlayerOwned()) {
                // Check vs enemies
                for (Enemy e : enemies) {
                    if (!e.isAlive()) continue;
                    if (CollisionHandler.boundingCircleCollision(p, e)) {
                        e.takeDamage(p.getDamage());
                        sounds.play("hit");
                        spawnImpactParticles(p);
                        p.deactivate();
                        break;
                    }
                }
            } else {
                // Enemy projectile vs player
                if (CollisionHandler.boundingCircleCollision(p, player)) {
                    player.takeDamage(p.getDamage());
                    sounds.play("hit");
                    if (!player.isAlive()) sounds.play("player_death");
                    spawnImpactParticles(p);
                    p.deactivate();
                }
            }
            if (!p.isActive()) it.remove();
        }
    }

    /** Spawns visual particles appropriate to the projectile's spell type. */
    private void spawnImpactParticles(Projectile p) {
        float cx = p.getCentreX(), cy = p.getCentreY();
        switch (p.getType()) {
            case FIREBALL:       particles.spawnFireImpact(cx, cy);     break;
            case LIGHTNING_BOLT: particles.spawnLightningImpact(cx, cy); break;
            case EXPLODE:        particles.spawnExplosion(cx, cy);      break;
            default:             particles.spawnMissileImpact(cx, cy);  break;
        }
    }

    /**
     * Returns the mining damage a spell type deals to tiles.
     * Higher = breaks tiles faster. Fire/explosion = 4 (one-shots stone),
     * lightning = 3, basic missile/spark = 1.
     */
    private int getMiningDamage(SpellType type) {
        switch (type) {
            case FIREBALL:       return 4;
            case EXPLODE:        return 4;
            case LIGHTNING_BOLT: return 3;
            case MAGIC_MISSILE:  return 1;
            case SPARK:          return 1;
            default:             return 1;
        }
    }

    /** Gets the durability of a tile character. Returns 0 if indestructible. */
    private int getTileDurability(char c) {
        switch (c) {
            case 'd': return DIRT_DURABILITY;
            case 'g': return DIRT_DURABILITY;
            case 's': return STONE_DURABILITY;
            default:  return 0; // indestructible (borders, liquids, etc.)
        }
    }

    /** Applies mining damage to a single tile at the projectile's impact point. */
    private void mineTile(Projectile p, Player player) {
        int tx = (int)(p.getCentreX() / tmap.getTileWidth());
        int ty = (int)(p.getCentreY() / tmap.getTileHeight());
        applyMineDamage(tx, ty, getMiningDamage(p.getType()), player);
    }

    /** Applies mining damage to a tile, destroying it if durability is exceeded. */
    private void applyMineDamage(int tx, int ty, int damage, Player player) {
        if (!tmap.valid(tx, ty)) return;
        char c = tmap.getTileChar(tx, ty);
        int durability = getTileDurability(c);
        if (durability <= 0) return; // indestructible

        long key = tileKey(tx, ty);
        int accumulated = tileDamage.getOrDefault(key, 0) + damage;
        if (accumulated >= durability) {
            // Tile breaks
            tileDamage.remove(key);
            if (c == 'g') {
                player.addGold(5);
                lastPickupMessage = "+5 Gold";
            }
            tmap.setTileChar('.', tx, ty);
            cellSim.scanMap(tmap);
        } else {
            tileDamage.put(key, accumulated);
        }
    }

    /** Destroys tiles in a square radius using AoE mining damage (explosions). */
    private void destroyTilesAround(Projectile p, int radius, Player player) {
        int tx = (int)(p.getCentreX() / tmap.getTileWidth());
        int ty = (int)(p.getCentreY() / tmap.getTileHeight());
        int damage = getMiningDamage(p.getType());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                applyMineDamage(tx + dx, ty + dy, damage, player);
            }
        }
    }

    /**
     * Detects whether the player is pressing into a wall while airborne.
     * Uses a velocity-based check: after collision resolution, if the player
     * is holding a direction key but their horizontal velocity was zeroed
     * by the collision handler, they must be blocked by a wall.
     */
    private void checkWallGrab(Player player) {
        boolean pressingLeft  = player.isMoveLeftHeld() && !player.isMoveRightHeld();
        boolean pressingRight = player.isMoveRightHeld() && !player.isMoveLeftHeld();
        boolean blocked = (pressingLeft || pressingRight)
                          && Math.abs(player.getVelocityX()) < 0.01f;

        if (!blocked) {
            player.setWallGrabbing(false);
            return;
        }

        // Verify there's real ground below the player's centre (not just a wall corner)
        float cx = player.getX() + player.getWidth() / 2f;
        float by = player.getY() + player.getHeight() + 1;
        boolean realGround = CollisionHandler.isSolid(tmap, cx, by);

        if (realGround) {
            // Actually standing on ground, not wall grabbing
            player.setWallGrabbing(false);
            return;
        }

        // Pressing into a wall while truly airborne — wall grab
        player.setWallGrabbing(true);
        // The collision handler may have falsely set onGround from the wall corner;
        // override it so gravity continues and the player slides down the wall.
        player.setOnGround(false);
        player.setVelocityY(Math.min(player.getVelocityY(), 0.05f));
    }

    /** Checks the tile under an entity's feet and sets water slowdown. */
    private void applyEnvironmentEffect(Entity entity) {
        float cx = entity.getX() + entity.getWidth()  / 2f;
        float cy = entity.getY() + entity.getHeight() - 2f;
        int tx = (int)(cx / tmap.getTileWidth());
        int ty = (int)(cy / tmap.getTileHeight());
        if (tmap.valid(tx, ty) && tmap.getTileChar(tx, ty) == 'w') {
            entity.setSpeedMultiplier(0.45f);
        } else {
            entity.setSpeedMultiplier(1.0f);
        }
    }

    /** Checks if an entity is touching a lava tile and applies damage. */
    private void checkLavaDamage(Entity entity) {
        float cx = entity.getX() + entity.getWidth()  / 2f;
        float cy = entity.getY() + entity.getHeight() - 2f;
        int tx = (int)(cx / tmap.getTileWidth());
        int ty = (int)(cy / tmap.getTileHeight());
        if (tmap.valid(tx, ty) && tmap.getTileChar(tx, ty) == 'l') {
            entity.takeDamage(1);
        }
    }

    /** Checks lava for player and enemies. */
    private void checkLavaDamage(Player player, SoundManager sounds) {
        checkLavaDamage(player);
        if (!player.isAlive()) sounds.play("player_death");
        for (Enemy e : enemies) {
            if (e.isAlive()) checkLavaDamage(e);
        }
    }

    /** Checks if the player is close to the exit tile. */
    private void checkExit(Player player) {
        int tx = (int)((player.getX() + player.getWidth() / 2f)  / tmap.getTileWidth());
        int ty = (int)((player.getY() + player.getHeight() / 2f) / tmap.getTileHeight());
        if (tmap.valid(tx, ty) && tmap.getTileChar(tx, ty) == 'e') {
            exitReached = true;
        }
    }

    /** Checks pickup proximity and collects any the player is touching (wands excluded — use E to swap). */
    private void updatePickups(Player player, SoundManager sounds) {
        for (Pickup pu : pickups) {
            if (pu.isCollected()) continue;
            pu.update(10); // bob update
            // Wand pickups require manual interaction (E key), skip auto-collect
            if (pu.getPickupType() == Pickup.PickupType.WAND) continue;
            if (CollisionHandler.boundingBoxCollision(player, pu)) {
                String msg = pu.collect(player, sounds);
                if (msg != null) lastPickupMessage = msg;
            }
        }
    }

    // =========================================================================
    // Projectile / pickup management
    // =========================================================================

    /** Adds a player-fired projectile to the level. */
    public void addProjectile(Projectile p) { projectiles.add(p); }

    /** Adds a pickup to the level (used for gold nuggets and shop items). */
    public void addPickup(Pickup p) { pickups.add(p); }

    /** Sets the animation used for spawning gold nugget pickups. */
    public void setGoldNuggetAnim(Animation anim) { goldNuggetAnim = anim; }

    /** Animation used for wand item pickups (needed to create ground wand after swap) */
    private Animation wandItemAnim = null;

    /** Sets the wand item animation for creating swapped ground wands. */
    public void setWandItemAnim(Animation anim) { wandItemAnim = anim; }

    /**
     * Tries to swap the player's active wand with a nearby wand pickup.
     * Called when the player presses the interact key (E).
     *
     * @param player Player performing the swap
     * @param sounds Sound manager
     * @param spellAnims Spell animations map (needed if creating new pickups)
     * @return Feedback message or null if no wand nearby
     */
    public String trySwapWand(Player player, SoundManager sounds,
                               Map<SpellType, Animation> spellAnims) {
        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;
        float bestDist = 60f; // max interaction distance in pixels
        Pickup bestWand = null;

        for (Pickup pu : pickups) {
            if (pu.isCollected() || pu.getPickupType() != Pickup.PickupType.WAND) continue;
            float dx = (pu.getX() + pu.getWidth() / 2f) - px;
            float dy = (pu.getY() + pu.getHeight() / 2f) - py;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) {
                bestDist = dist;
                bestWand = pu;
            }
        }

        if (bestWand == null) return null;

        Wand groundWand = bestWand.getWand();
        if (groundWand == null) return null;

        // If player has room in inventory (<4 wands), just pick it up
        if (player.getWands().size() < 4) {
            player.addWand(groundWand);
            sounds.play("pickup");
            bestWand.dismiss();
            return "Picked up " + groundWand.getDisplayName() + "!";
        }

        // Swap: replace active wand with ground wand, drop old wand
        Wand oldWand = player.swapActiveWand(groundWand);
        sounds.play("pickup");

        // Replace the ground pickup with the old wand
        float dropX = bestWand.getX();
        float dropY = bestWand.getY();
        bestWand.dismiss();
        if (oldWand != null && wandItemAnim != null) {
            pickups.add(new Pickup(wandItemAnim, dropX, dropY, oldWand));
        }

        return "Swapped for " + groundWand.getDisplayName() + "!";
    }

    // =========================================================================
    // Draw
    // =========================================================================

    /**
     * Draws the tile map, all entities, projectiles, and pickups.
     *
     * @param g       Graphics context
     * @param xOffset Camera draw X offset
     * @param yOffset Camera draw Y offset
     * @param debug   True to draw collision debug overlays
     */
    public void draw(Graphics2D g, int xOffset, int yOffset, boolean debug) {
        tmap.draw(g, xOffset, yOffset);

        for (Pickup pu : new ArrayList<>(pickups)) {
            if (!pu.isCollected()) {
                pu.setOffsets(xOffset, yOffset);
                pu.draw(g);
                // Draw price label above shop items
                if (pu.getPriceLabel() != null) {
                    g.setFont(PRICE_FONT);
                    FontMetrics fm = g.getFontMetrics();
                    String label = pu.getPriceLabel();
                    int lx = (int) pu.getX() + xOffset + pu.getWidth() / 2
                             - fm.stringWidth(label) / 2;
                    int ly = (int) pu.getY() + yOffset - 8;
                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillRoundRect(lx - 4, ly - 12, fm.stringWidth(label) + 8, 16, 4, 4);
                    g.setColor(new Color(255, 215, 0));
                    g.drawString(label, lx, ly);
                }
                // Draw wand name + [E] hint above wand pickups
                if (pu.getPickupType() == Pickup.PickupType.WAND && pu.getWand() != null) {
                    g.setFont(PRICE_FONT);
                    FontMetrics fm = g.getFontMetrics();
                    String label = pu.getWand().getDisplayName() + " [E]";
                    int lx = (int) pu.getX() + xOffset + pu.getWidth() / 2
                             - fm.stringWidth(label) / 2;
                    int ly = (int) pu.getY() + yOffset - 8;
                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillRoundRect(lx - 4, ly - 12, fm.stringWidth(label) + 8, 16, 4, 4);
                    g.setColor(new Color(180, 220, 255));
                    g.drawString(label, lx, ly);
                }
            }
        }

        for (Enemy e : enemies) {
            if (e.isAlive()) {
                e.setOffsets(xOffset, yOffset);
                e.drawTransformed(g);
                // Health bar above enemy
                int barW = Math.min(e.getWidth(), 28);
                int barH = 3;
                int bx = (int) e.getX() + xOffset + (e.getWidth() - barW) / 2;
                int by = (int) e.getY() + yOffset - 6;
                float frac = (float) e.getHealth() / e.getMaxHealth();
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRect(bx - 1, by - 1, barW + 2, barH + 2);
                g.setColor(new Color(180, 30, 30));
                g.fillRect(bx, by, barW, barH);
                g.setColor(new Color(50, 200, 50));
                g.fillRect(bx, by, (int) (barW * frac), barH);
            }
        }

        for (Projectile p : projectiles) {
            if (p.isActive()) {
                p.setOffsets(xOffset, yOffset);
                p.draw(g);
            }
        }

        if (debug) {
            tmap.drawBorder(g, xOffset, yOffset, Color.GREEN);
        }
    }

    // =========================================================================
    // Open-tile finders (for enemy/pickup placement)
    // =========================================================================

    /**
     * Packs a tile coordinate pair into a single long for use in the
     * usedSpawnTiles set.
     */
    private static long tileKey(int tx, int ty) {
        return ((long) tx << 32) | (ty & 0xFFFFFFFFL);
    }

    /**
     * Finds an open floor tile (empty tile with solid below) near tile column X.
     * Skips positions already used by another entity to prevent stacking.
     *
     * @param startX Starting tile X (clamped to map)
     * @param maxY   Maximum tile Y to search
     * @return [tileX, tileY] of the open floor, or null if not found
     */
    private int[] findOpenFloor(int startX, int maxY) {
        int x = MathUtils.clamp(startX, 2, tmap.getMapWidth() - 3);
        for (int tries = 0; tries < 50; tries++) {
            int tx = MathUtils.clamp(x + tries, 2, tmap.getMapWidth() - 3);
            for (int ty = 3; ty < maxY - 1; ty++) {
                char above = tmap.getTileChar(tx, ty);
                char below = tmap.getTileChar(tx, ty + 1);
                char twoAbove = tmap.getTileChar(tx, ty - 1);
                if (above == '.' && twoAbove == '.' && (below == 's' || below == 'd')) {
                    long key = tileKey(tx, ty);
                    if (!usedSpawnTiles.contains(key)) {
                        usedSpawnTiles.add(key);
                        return new int[]{tx, ty};
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds an open air tile near tile column X in the upper half of the map.
     * Skips positions already used by another entity.
     *
     * @param startX Starting tile X
     * @param maxY   Maximum tile row
     * @return [tileX, tileY] or null
     */
    private int[] findOpenSpace(int startX, int maxY) {
        int x = MathUtils.clamp(startX, 2, tmap.getMapWidth() - 3);
        for (int tries = 0; tries < 40; tries++) {
            int tx = MathUtils.clamp(x + tries, 2, tmap.getMapWidth() - 3);
            for (int ty = 2; ty < Math.min(maxY, tmap.getMapHeight() - 2); ty++) {
                if (tmap.getTileChar(tx, ty) == '.') {
                    long key = tileKey(tx, ty);
                    if (!usedSpawnTiles.contains(key)) {
                        usedSpawnTiles.add(key);
                        return new int[]{tx, ty};
                    }
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** @return The tile map for this level */
    public TileMap getTileMap() { return tmap; }

    /** @return List of active enemies */
    public List<Enemy> getEnemies() { return enemies; }

    /** @return True if the player has touched the exit tile */
    public boolean isExitReached() { return exitReached; }

    /** @return Player spawn world X */
    public float getSpawnX() { return spawnX; }

    /** @return Player spawn world Y */
    public float getSpawnY() { return spawnY; }

    /** Consumes and returns the last pickup feedback message, or null. */
    public String consumePickupMessage() {
        String msg = lastPickupMessage;
        lastPickupMessage = null;
        return msg;
    }

    /** @return Total world width in pixels */
    public int getWorldWidth()  { return tmap.getPixelWidth(); }

    /** @return Total world height in pixels */
    public int getWorldHeight() { return tmap.getPixelHeight(); }
}
