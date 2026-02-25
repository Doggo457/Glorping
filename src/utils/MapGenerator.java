package utils;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Generates procedural cave levels and saves them as TileMap text files.
 * The cave generation uses a cellular automata algorithm: initialise with
 * random noise, then apply smoothing rules iteratively to produce organic
 * cave shapes.
 *
 * Level 1 – "Shallow Cave": lighter density, mixed stone/dirt, small water pools.
 * Level 2 – "Deep Cavern": denser walls, more gold ore, lava pools instead of water.
 */
public final class MapGenerator {

    // Map dimensions in tiles
    private static final int MAP_W       = 100;
    private static final int MAP_H       = 40;
    private static final int TILE_SIZE   = 32;

    // Starting zone – keep clear so the player can spawn
    private static final int SPAWN_CLEAR_X = 5;
    private static final int SPAWN_CLEAR_Y = MAP_H / 2 - 3;
    private static final int SPAWN_CLEAR_W = 6;
    private static final int SPAWN_CLEAR_H = 7;

    /** Prevent instantiation */
    private MapGenerator() {}

    /** Total number of cave levels to generate */
    private static final int NUM_LEVELS = 5;

    /**
     * Entry point – generates all level map files.
     * Called from App.main() before the game starts.
     */
    public static void generate() {
        for (int i = 0; i < NUM_LEVELS; i++) {
            generate(i);
        }
        System.out.println("MapGenerator: " + NUM_LEVELS + " levels ready.");
    }

    /**
     * Generates (or regenerates) a single level's map file using a random seed.
     * Levels 0-1 use water, levels 2+ use lava. Deeper levels have more gold.
     *
     * @param levelIndex 0-based level index
     */
    public static void generate(int levelIndex) {
        boolean lava = levelIndex >= 2;
        String path = "maps/level" + (levelIndex + 1) + ".txt";
        long seed = System.nanoTime() ^ (levelIndex * 0x9E3779B97F4A7C15L);
        generateLevel(path, seed, lava, levelIndex);
    }

    /**
     * Generates a single level and writes it to disk.
     * Deeper levels get denser caves and more gold ore.
     *
     * @param path       Output file path relative to working directory
     * @param seed       Random seed for generation
     * @param lava       True to use lava pools instead of water
     * @param levelIndex 0-based level index for difficulty scaling
     */
    private static void generateLevel(String path, long seed, boolean lava, int levelIndex) {
        Random rng = new Random(seed);
        // ~50% fill — deeper levels slightly denser
        float density = 0.52f + levelIndex * 0.02f;
        boolean[][] walls = initNoise(rng, density);
        for (int i = 0; i < 5; i++) walls = smooth(walls);

        char[][] tiles = buildTileChars(walls, rng, lava, levelIndex);
        clearSpawnZone(tiles);
        clearExitZone(tiles);
        placeFloatingIslands(tiles, rng, levelIndex);
        if (!isPathConnected(tiles)) {
            carvePath(tiles, rng);
        }
        writeMapFile(path, tiles, lava);
    }

    // =========================================================================
    // Cave generation helpers
    // =========================================================================

    /**
     * Initialises a boolean wall grid with random noise.
     * Border cells are always walls to keep the player inside.
     *
     * @param rng      Random number generator
     * @param density  Probability (0–1) that a cell starts as a wall
     * @return 2D boolean array where true = wall
     */
    private static boolean[][] initNoise(Random rng, float density) {
        boolean[][] w = new boolean[MAP_W][MAP_H];
        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                if (x == 0 || x == MAP_W - 1 || y == 0 || y == MAP_H - 1) {
                    w[x][y] = true; // solid border
                } else {
                    w[x][y] = rng.nextFloat() < density;
                }
            }
        }
        return w;
    }

    /**
     * Applies one iteration of the cellular automata smoothing rule.
     * A cell becomes a wall if it has 5 or more wall neighbours.
     *
     * @param w Input wall grid
     * @return Smoothed wall grid
     */
    private static boolean[][] smooth(boolean[][] w) {
        boolean[][] next = new boolean[MAP_W][MAP_H];
        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                if (x == 0 || x == MAP_W - 1 || y == 0 || y == MAP_H - 1) {
                    next[x][y] = true;
                } else {
                    int n = countNeighbourWalls(w, x, y);
                    next[x][y] = (n >= 5);
                }
            }
        }
        return next;
    }

    /**
     * Counts the number of wall cells in the 8-directional neighbourhood.
     *
     * @param w Wall grid
     * @param x Tile X
     * @param y Tile Y
     * @return Number of neighbouring walls (0–8)
     */
    private static int countNeighbourWalls(boolean[][] w, int x, int y) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || nx >= MAP_W || ny < 0 || ny >= MAP_H) count++;
                else if (w[nx][ny]) count++;
            }
        }
        return count;
    }

    /**
     * Converts the boolean wall grid to tile characters.
     * Wall tiles are assigned as stone, dirt, or gold based on random weights.
     * Deeper levels have more gold ore. Open areas near the bottom get liquid pools.
     *
     * @param walls      Boolean wall grid
     * @param rng        Random source
     * @param lava       True to use lava instead of water
     * @param levelIndex Level index for scaling gold frequency
     * @return 2D char array suitable for writing to a map file
     */
    private static char[][] buildTileChars(boolean[][] walls, Random rng, boolean lava, int levelIndex) {
        char[][] tiles = new char[MAP_W][MAP_H];
        char liquid = lava ? 'l' : 'w';
        float goldChance = 0.05f + levelIndex * 0.02f;

        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                if (walls[x][y]) {
                    float r = rng.nextFloat();
                    if (r < goldChance)           tiles[x][y] = 'g'; // gold ore
                    else if (r < goldChance + 0.23f) tiles[x][y] = 'd'; // dirt
                    else                          tiles[x][y] = 's'; // stone
                } else {
                    if (y > MAP_H * 2 / 3 && rng.nextFloat() < 0.08f) {
                        tiles[x][y] = liquid;
                    } else {
                        tiles[x][y] = '.';
                    }
                }
            }
        }
        return tiles;
    }

    /**
     * Clears a rectangular zone near the left side for the player to spawn into.
     *
     * @param tiles Tile character grid
     */
    private static void clearSpawnZone(char[][] tiles) {
        for (int x = SPAWN_CLEAR_X; x < SPAWN_CLEAR_X + SPAWN_CLEAR_W; x++) {
            for (int y = SPAWN_CLEAR_Y; y < SPAWN_CLEAR_Y + SPAWN_CLEAR_H; y++) {
                if (x >= 0 && x < MAP_W && y >= 0 && y < MAP_H) {
                    tiles[x][y] = '.';
                }
            }
        }
        // Floor beneath spawn
        for (int x = SPAWN_CLEAR_X; x < SPAWN_CLEAR_X + SPAWN_CLEAR_W; x++) {
            int floorY = SPAWN_CLEAR_Y + SPAWN_CLEAR_H;
            if (floorY < MAP_H) tiles[x][floorY] = 's';
        }
    }

    /**
     * Places the exit marker tile near the right edge of the map.
     *
     * @param tiles Tile character grid
     */
    private static void clearExitZone(char[][] tiles) {
        int ex = MAP_W - 4;
        int midY = MAP_H / 2;
        for (int y = midY - 3; y <= midY + 3; y++) {
            if (y >= 0 && y < MAP_H) tiles[ex][y] = '.';
            if (ex + 1 < MAP_W && y >= 0 && y < MAP_H) tiles[ex + 1][y] = '.';
        }
        // Floor below exit
        for (int x = ex; x <= ex + 1; x++) {
            if (midY + 4 < MAP_H) tiles[x][midY + 4] = 's';
        }
        // Exit marker tile
        tiles[ex][midY] = 'e';
    }

    /**
     * Checks whether the spawn zone and exit zone are connected via passable
     * tiles using a flood-fill (BFS) from the spawn area.
     *
     * @param tiles Tile character grid
     * @return True if a path exists from spawn to exit
     */
    private static boolean isPathConnected(char[][] tiles) {
        // BFS from centre of spawn zone.
        // A tile is only reachable if it AND the tile above it are passable
        // (the player needs at least 2 tiles of vertical clearance).
        int startX = SPAWN_CLEAR_X + SPAWN_CLEAR_W / 2;
        int startY = MAP_H / 2;
        int exitX  = MAP_W - 4;
        int exitY  = MAP_H / 2;

        boolean[][] visited = new boolean[MAP_W][MAP_H];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int px = pos[0], py = pos[1];
            if (px == exitX && py == exitY) return true;

            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = px + d[0], ny = py + d[1];
                if (nx < 0 || nx >= MAP_W || ny < 0 || ny >= MAP_H) continue;
                if (visited[nx][ny]) continue;
                char c = tiles[nx][ny];
                if (c == '.' || c == 'w' || c == 'l' || c == 'e') {
                    // Check that the tile above is also passable (player height)
                    boolean headRoom = true;
                    if (ny - 1 >= 0) {
                        char above = tiles[nx][ny - 1];
                        if (above != '.' && above != 'w' && above != 'l' && above != 'e') {
                            headRoom = false;
                        }
                    }
                    if (headRoom) {
                        visited[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }
        return false;
    }

    /**
     * Carves a wandering path from the spawn zone towards the exit by
     * punching holes through the thinnest walls. The path meanders
     * vertically to avoid a straight corridor.
     *
     * @param tiles Tile character grid
     * @param rng   Random source for vertical drift
     */
    private static void carvePath(char[][] tiles, Random rng) {
        int x = SPAWN_CLEAR_X + SPAWN_CLEAR_W;
        int y = MAP_H / 2;
        int exitX = MAP_W - 4;
        int exitY = MAP_H / 2;

        while (x <= exitX) {
            // Punch a 3-tile-tall hole (player needs head room)
            for (int dy = -1; dy <= 1; dy++) {
                int cy = y + dy;
                if (cy > 0 && cy < MAP_H - 1) {
                    char c = tiles[x][cy];
                    if (c != '.' && c != 'e' && c != 'w' && c != 'l') {
                        tiles[x][cy] = '.';
                    }
                }
            }

            // Near the exit, steer toward the exit Y to ensure connection
            if (x >= exitX - 5) {
                if (y < exitY) y++;
                else if (y > exitY) y--;
            } else {
                // Drift vertically to create a natural-looking path
                int drift = rng.nextInt(3) - 1; // -1, 0, or +1
                y = Math.max(3, Math.min(MAP_H - 4, y + drift));

                // Sometimes take a larger vertical step for variety
                if (rng.nextFloat() < 0.15f) {
                    int bigDrift = rng.nextInt(5) - 2; // -2 to +2
                    y = Math.max(3, Math.min(MAP_H - 4, y + bigDrift));
                }
            }

            x++;
        }
    }

    // =========================================================================
    // Floating islands
    // =========================================================================

    /**
     * Places floating stone platforms in open air areas to create parkour
     * opportunities. More islands are placed in deeper levels.
     *
     * @param tiles      Tile character grid
     * @param rng        Random source
     * @param levelIndex Level index – deeper = more islands
     */
    private static void placeFloatingIslands(char[][] tiles, Random rng, int levelIndex) {
        int numIslands = 12 + levelIndex * 4;

        for (int i = 0; i < numIslands; i++) {
            int platW = 3 + rng.nextInt(5); // 3–7 tiles wide
            int px = 8 + rng.nextInt(MAP_W - 16 - platW);
            int py = 4 + rng.nextInt(MAP_H - 10);

            // Only place if the area is mostly open air
            boolean clear = true;
            for (int dx = -1; dx <= platW && clear; dx++) {
                for (int dy = -2; dy <= 1 && clear; dy++) {
                    int cx = px + dx, cy = py + dy;
                    if (cx < 0 || cx >= MAP_W || cy < 0 || cy >= MAP_H) continue;
                    char c = tiles[cx][cy];
                    if (c != '.' && dy <= 0) clear = false;
                }
            }
            if (!clear) continue;

            // Place the platform
            for (int dx = 0; dx < platW; dx++) {
                int tx = px + dx;
                if (tx > 0 && tx < MAP_W - 1) {
                    tiles[tx][py] = 's';
                }
            }
        }
    }

    // =========================================================================
    // File writing
    // =========================================================================

    /**
     * Writes the complete TileMap text file to disk using the expected format:
     * <pre>
     * width height tileW tileH
     * #char=image.png
     * ...
     * #map
     * row0data
     * row1data
     * ...
     * </pre>
     *
     * @param path  Destination file path
     * @param tiles 2D tile character grid
     * @param lava  True if this level uses lava tiles
     */
    private static void writeMapFile(String path, char[][] tiles, boolean lava) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            // Header line
            pw.printf("%d %d %d %d%n", MAP_W, MAP_H, TILE_SIZE, TILE_SIZE);
            // Character→image mappings
            pw.println("#s=stone.png");
            pw.println("#d=dirt.png");
            pw.println("#g=gold.png");
            if (lava) {
                pw.println("#l=lava.png");
            } else {
                pw.println("#w=water.png");
            }
            pw.println("#e=exit.png");
            pw.println("#map");
            // Map rows
            for (int y = 0; y < MAP_H; y++) {
                StringBuilder sb = new StringBuilder(MAP_W);
                for (int x = 0; x < MAP_W; x++) {
                    char c = tiles[x][y];
                    // Replace lava/water chars with '.' if the tile type is not used
                    if (!lava && c == 'l') c = '.';
                    if (lava  && c == 'w') c = '.';
                    sb.append(c);
                }
                pw.println(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("MapGenerator: failed to write " + path + ": " + e);
        }
    }
}
