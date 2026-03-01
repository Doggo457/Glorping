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
    private static final long TICK_INTERVAL = 30L;
    /** Maximum number of tiles to simulate per tick */
    private static final int  MAX_PER_TICK   = 150;

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

            // 1) Try to fall straight down
            if (isEmpty(tmap, x, y + 1)) {
                moveTile(tmap, self, x, y, x, y + 1, pos, toRemove, toAdd);
                continue;
            }

            // 2) Try to fall diagonally (down-left / down-right)
            boolean tryLeft = rng.nextBoolean();
            int d1 = tryLeft ? -1 : 1;
            int d2 = -d1;
            if (isEmpty(tmap, x + d1, y + 1)) {
                moveTile(tmap, self, x, y, x + d1, y + 1, pos, toRemove, toAdd);
                continue;
            }
            if (isEmpty(tmap, x + d2, y + 1)) {
                moveTile(tmap, self, x, y, x + d2, y + 1, pos, toRemove, toAdd);
                continue;
            }

            // 3) Spread sideways — but only if there's a drop-off reachable
            //    in that direction (prevents pointless jitter on flat surfaces)
            boolean movedSide = false;
            for (int d : new int[]{d1, d2}) {
                if (isEmpty(tmap, x + d, y) && canReachDrop(tmap, x + d, y, d, self)) {
                    moveTile(tmap, self, x, y, x + d, y, pos, toRemove, toAdd);
                    movedSide = true;
                    break;
                }
            }
        }

        liquidTiles.removeAll(toRemove);
        liquidTiles.addAll(toAdd);
    }

    /** Checks if a tile position is valid and empty. */
    private boolean isEmpty(TileMap tmap, int x, int y) {
        return tmap.valid(x, y) && tmap.getTileChar(x, y) == '.';
    }

    /** Moves a liquid tile from (ox,oy) to (nx,ny), updating tracking lists. */
    private void moveTile(TileMap tmap, char self, int ox, int oy, int nx, int ny,
                          int[] pos, List<int[]> toRemove, List<int[]> toAdd) {
        tmap.setTileChar(self, nx, ny);
        tmap.setTileChar('.', ox, oy);
        toRemove.add(pos);
        toAdd.add(new int[]{nx, ny});
    }

    /**
     * Scans sideways from (x,y) in direction dx to see if there's an empty
     * tile below within a short range. This prevents water from jittering
     * sideways on a flat settled surface — it only flows if it can actually
     * reach a lower point.
     */
    private boolean canReachDrop(TileMap tmap, int x, int y, int dx, char self) {
        // Check up to 5 tiles ahead for a drop-off
        for (int i = 0; i < 5; i++) {
            if (!tmap.valid(x, y)) return false;
            char c = tmap.getTileChar(x, y);
            // Hit a wall — no drop reachable this way
            if (c != '.' && c != self) return false;
            // Found a drop-off below this position
            if (isEmpty(tmap, x, y + 1)) return true;
            x += dx;
        }
        return false;
    }
}
