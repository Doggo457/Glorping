package world;

import game2D.TileMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulates simple fluid/material behaviour at the tile level, similar in
 * spirit to Noita's powder simulation (but on a coarser tile grid).
 *
 * <p>Rules implemented:</p>
 * <ul>
 *   <li><b>Water ('w')</b>: Falls straight down if the tile below is empty ('.').
 *       If blocked below, tries to flow left or right.</li>
 *   <li><b>Lava ('l')</b>: Same as water but also damages entities that touch it
 *       (handled by CollisionHandler – not this class).</li>
 *   <li><b>Dirt ('d') burn</b>: If an adjacent fire particle is nearby, dirt tiles
 *       may be removed over time (handled externally by Level via
 *       {@link #destroyTile(TileMap, int, int)}).</li>
 * </ul>
 *
 * For performance, only a random subset of liquid tiles is simulated per frame.
 */
public class CellularAutomata {

    /** Milliseconds between simulation ticks */
    private static final long TICK_INTERVAL = 120L;
    /** Maximum number of tiles to simulate per tick */
    private static final int  MAX_PER_TICK   = 30;

    private long tickTimer = 0;
    /** Accumulated list of known liquid tile positions [x, y] pairs */
    private final List<int[]> liquidTiles = new ArrayList<>();
    private final java.util.Random rng = new java.util.Random();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Advances the simulation by {@code elapsed} milliseconds.
     * Only processes one tick per TICK_INTERVAL to limit CPU usage.
     *
     * @param elapsed Elapsed ms since last call
     * @param tmap    Tile map to update
     */
    public void update(long elapsed, TileMap tmap) {
        tickTimer += elapsed;
        if (tickTimer < TICK_INTERVAL) return;
        tickTimer -= TICK_INTERVAL;

        // Rebuild liquid tile list from a region scan (cheap: only borders change)
        // For efficiency we scan a rolling window rather than the whole map each tick.
        simulateLiquids(tmap);
    }

    /**
     * Scans the entire tile map and rebuilds the internal liquid tile list.
     * Call this once after loading a new level.
     *
     * @param tmap Tile map to scan
     */
    public void scanMap(TileMap tmap) {
        liquidTiles.clear();
        for (int x = 0; x < tmap.getMapWidth(); x++) {
            for (int y = 0; y < tmap.getMapHeight(); y++) {
                char c = tmap.getTileChar(x, y);
                if (c == 'w' || c == 'l') liquidTiles.add(new int[]{x, y});
            }
        }
    }

    /**
     * Destroys a tile (sets it to empty '.').
     * Used by the game when a spell destroys a destructible tile.
     *
     * @param tmap Tile map
     * @param tx   Tile X coordinate
     * @param ty   Tile Y coordinate
     * @return True if the tile was destructible and was removed
     */
    public boolean destroyTile(TileMap tmap, int tx, int ty) {
        if (!tmap.valid(tx, ty)) return false;
        char c = tmap.getTileChar(tx, ty);
        // Only dirt and gold are destructible; stone is indestructible
        if (c == 'd' || c == 'g') {
            tmap.setTileChar('.', tx, ty);
            return true;
        }
        return false;
    }

    // =========================================================================
    // Simulation
    // =========================================================================

    /**
     * Simulates a random subset of liquid tiles for one tick.
     * Liquids flow downward first; if blocked, they spread sideways.
     *
     * @param tmap Tile map to update
     */
    private void simulateLiquids(TileMap tmap) {
        if (liquidTiles.isEmpty()) return;

        // Shuffle so flow direction is unbiased
        Collections.shuffle(liquidTiles, rng);

        int processed = 0;
        List<int[]> toRemove = new ArrayList<>();
        List<int[]> toAdd    = new ArrayList<>();

        for (int[] pos : liquidTiles) {
            if (processed++ >= MAX_PER_TICK) break;
            int x = pos[0];
            int y = pos[1];

            if (!tmap.valid(x, y)) { toRemove.add(pos); continue; }
            char self = tmap.getTileChar(x, y);
            if (self != 'w' && self != 'l') { toRemove.add(pos); continue; }

            // Try to fall down
            if (tmap.valid(x, y + 1) && tmap.getTileChar(x, y + 1) == '.') {
                tmap.setTileChar(self, x, y + 1);
                tmap.setTileChar('.', x, y);
                toRemove.add(pos);
                toAdd.add(new int[]{x, y + 1});
                continue;
            }

            // Try to flow sideways (random left/right bias)
            boolean tryLeft = rng.nextBoolean();
            int d1 = tryLeft ? -1 : 1;
            int d2 = -d1;

            boolean moved = false;
            for (int d : new int[]{d1, d2}) {
                if (tmap.valid(x + d, y) && tmap.getTileChar(x + d, y) == '.') {
                    tmap.setTileChar(self, x + d, y);
                    tmap.setTileChar('.', x, y);
                    toRemove.add(pos);
                    toAdd.add(new int[]{x + d, y});
                    moved = true;
                    break;
                }
            }
        }

        liquidTiles.removeAll(toRemove);
        liquidTiles.addAll(toAdd);
    }
}
