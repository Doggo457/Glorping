package utils;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Generates procedural cave levels and saves them as TileMap text files.
 *
 * The generation pipeline:
 * 1. High-density cellular automata to create thick cave walls
 * 2. Carve large elliptical caverns at random locations
 * 3. Connect all caverns with winding tunnels
 * 4. Add stalactites and stalagmites along cave edges
 * 5. Settle liquid pools at the bottom of open spaces
 * 6. Place floating platforms in tall caverns
 * 7. Ensure spawn-to-exit connectivity
 */
public final class MapGenerator {

    // Map dimensions in tiles
    private static final int MAP_W     = 150;
    private static final int MAP_H     = 60;
    private static final int TILE_SIZE = 32;

    // Border thickness — thick solid edges so it feels enclosed
    private static final int BORDER = 3;

    // Starting zone
    private static final int SPAWN_CLEAR_X = BORDER + 2;
    private static final int SPAWN_CLEAR_Y = MAP_H / 2 - 4;
    private static final int SPAWN_CLEAR_W = 8;
    private static final int SPAWN_CLEAR_H = 9;

    private MapGenerator() {}

    private static final int NUM_LEVELS = 5;

    public static void generate() {
        for (int i = 0; i < NUM_LEVELS; i++) {
            generate(i);
        }
        System.out.println("MapGenerator: " + NUM_LEVELS + " levels ready.");
    }

    public static void generate(int levelIndex) {
        boolean lava = levelIndex >= 2;
        String path = "src/resources/maps/level" + (levelIndex + 1) + ".txt";
        long seed = System.nanoTime() ^ (levelIndex * 0x9E3779B97F4A7C15L);
        generateLevel(path, seed, lava, levelIndex);
    }

    private static void generateLevel(String path, long seed, boolean lava, int levelIndex) {
        Random rng = new Random(seed);

        // 1. Cellular automata base — high density = mostly rock
        float density = 0.62f + levelIndex * 0.015f;
        boolean[][] walls = initNoise(rng, density);
        for (int i = 0; i < 7; i++) walls = smooth(walls);

        // 2. Carve large cavern rooms into the rock
        List<int[]> caverns = carveCaverns(walls, rng, levelIndex);

        // 3. Connect caverns with winding tunnels
        connectCaverns(walls, caverns, rng);

        // 4. Convert to tile chars
        char[][] tiles = buildTileChars(walls, rng, lava, levelIndex);

        // 5. Add stalactites and stalagmites
        addFormations(tiles, rng, levelIndex);

        // 6. Settle liquid into pool bottoms
        settleLiquid(tiles, rng, lava, levelIndex);

        // 7. Spawn/exit zones
        clearSpawnZone(tiles);
        clearExitZone(tiles);

        // 8. Floating platforms in tall open areas
        placeFloatingIslands(tiles, rng, levelIndex);

        // 9. Ensure path connectivity
        if (!isPathConnected(tiles)) {
            carvePath(tiles, rng);
        }

        writeMapFile(path, tiles, lava);
    }

    // =========================================================================
    // Cellular automata
    // =========================================================================

    private static boolean[][] initNoise(Random rng, float density) {
        boolean[][] w = new boolean[MAP_W][MAP_H];
        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                // Thick solid border
                if (x < BORDER || x >= MAP_W - BORDER ||
                    y < BORDER || y >= MAP_H - BORDER) {
                    w[x][y] = true;
                } else {
                    w[x][y] = rng.nextFloat() < density;
                }
            }
        }
        return w;
    }

    private static boolean[][] smooth(boolean[][] w) {
        boolean[][] next = new boolean[MAP_W][MAP_H];
        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                if (x < BORDER || x >= MAP_W - BORDER ||
                    y < BORDER || y >= MAP_H - BORDER) {
                    next[x][y] = true;
                } else {
                    int n = countNeighbourWalls(w, x, y);
                    // Standard rule: become wall if 5+ neighbours are walls
                    // Also become wall if 0 neighbours are walls (fill isolated dots)
                    next[x][y] = (n >= 5) || (n == 0);
                }
            }
        }
        return next;
    }

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

    // =========================================================================
    // Cavern carving
    // =========================================================================

    /**
     * Carves large elliptical cavern rooms into the solid rock.
     * Returns the centre points so they can be connected with tunnels.
     */
    private static List<int[]> carveCaverns(boolean[][] walls, Random rng, int levelIndex) {
        List<int[]> centres = new ArrayList<>();
        int numCaverns = 8 + levelIndex * 2;

        // Always include spawn and exit regions as cavern centres
        centres.add(new int[]{SPAWN_CLEAR_X + SPAWN_CLEAR_W / 2, MAP_H / 2});
        centres.add(new int[]{MAP_W - 6, MAP_H / 2});

        for (int i = 0; i < numCaverns; i++) {
            int cx = BORDER + 10 + rng.nextInt(MAP_W - BORDER * 2 - 20);
            int cy = BORDER + 5 + rng.nextInt(MAP_H - BORDER * 2 - 10);
            int radiusX = 5 + rng.nextInt(9);  // 5-13 tiles wide radius
            int radiusY = 4 + rng.nextInt(6);  // 4-9 tiles tall radius
            carveEllipse(walls, cx, cy, radiusX, radiusY);
            centres.add(new int[]{cx, cy});
        }

        return centres;
    }

    /** Carves an elliptical open area into the wall grid. */
    private static void carveEllipse(boolean[][] walls, int cx, int cy, int rx, int ry) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            for (int y = cy - ry; y <= cy + ry; y++) {
                if (x < BORDER || x >= MAP_W - BORDER ||
                    y < BORDER || y >= MAP_H - BORDER) continue;
                float dx = (float)(x - cx) / rx;
                float dy = (float)(y - cy) / ry;
                if (dx * dx + dy * dy <= 1.0f) {
                    walls[x][y] = false;
                }
            }
        }
    }

    // =========================================================================
    // Tunnel connections
    // =========================================================================

    /**
     * Connects all cavern centres with winding tunnels.
     * Each cavern connects to its nearest unconnected neighbour,
     * forming a spanning tree so every cavern is reachable.
     */
    private static void connectCaverns(boolean[][] walls, List<int[]> caverns, Random rng) {
        if (caverns.size() < 2) return;

        boolean[] connected = new boolean[caverns.size()];
        connected[0] = true; // spawn cavern is the root

        for (int iter = 0; iter < caverns.size() - 1; iter++) {
            int bestFrom = -1, bestTo = -1;
            double bestDist = Double.MAX_VALUE;

            // Find nearest unconnected cavern to any connected one
            for (int i = 0; i < caverns.size(); i++) {
                if (!connected[i]) continue;
                for (int j = 0; j < caverns.size(); j++) {
                    if (connected[j]) continue;
                    double dist = Math.hypot(
                            caverns.get(i)[0] - caverns.get(j)[0],
                            caverns.get(i)[1] - caverns.get(j)[1]);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestFrom = i;
                        bestTo = j;
                    }
                }
            }
            if (bestTo < 0) break;

            connected[bestTo] = true;
            carveTunnel(walls, caverns.get(bestFrom), caverns.get(bestTo), rng);
        }
    }

    /**
     * Carves a winding tunnel between two points.
     * The tunnel is 4-5 tiles wide and meanders with random drift.
     */
    private static void carveTunnel(boolean[][] walls, int[] from, int[] to, Random rng) {
        float x = from[0], y = from[1];
        float tx = to[0], ty = to[1];
        int width = 2 + rng.nextInt(2); // radius 2-3 = diameter 4-6

        while (Math.abs(x - tx) > 1 || Math.abs(y - ty) > 1) {
            // Carve a circle at current position
            for (int dx = -width; dx <= width; dx++) {
                for (int dy = -width; dy <= width; dy++) {
                    if (dx * dx + dy * dy <= width * width) {
                        int cx = (int)x + dx, cy = (int)y + dy;
                        if (cx >= BORDER && cx < MAP_W - BORDER &&
                            cy >= BORDER && cy < MAP_H - BORDER) {
                            walls[cx][cy] = false;
                        }
                    }
                }
            }

            // Move toward target with random drift for organic feel
            float dirX = tx - x, dirY = ty - y;
            float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);
            if (len > 0) { dirX /= len; dirY /= len; }

            // Add perpendicular drift
            float driftAmount = (rng.nextFloat() - 0.5f) * 2.5f;
            x += dirX * 1.5f + (-dirY) * driftAmount;
            y += dirY * 1.5f + dirX * driftAmount;

            // Clamp to map
            x = Math.max(BORDER, Math.min(MAP_W - BORDER - 1, x));
            y = Math.max(BORDER, Math.min(MAP_H - BORDER - 1, y));
        }
    }

    // =========================================================================
    // Tile assignment
    // =========================================================================

    private static char[][] buildTileChars(boolean[][] walls, Random rng,
                                            boolean lava, int levelIndex) {
        char[][] tiles = new char[MAP_W][MAP_H];
        float goldChance = 0.04f + levelIndex * 0.02f;

        for (int x = 0; x < MAP_W; x++) {
            for (int y = 0; y < MAP_H; y++) {
                if (walls[x][y]) {
                    // Assign wall material
                    float r = rng.nextFloat();
                    if (r < goldChance)                tiles[x][y] = 'g';
                    else if (r < goldChance + 0.30f)   tiles[x][y] = 'd';
                    else                               tiles[x][y] = 's';
                } else {
                    tiles[x][y] = '.';
                }
            }
        }
        return tiles;
    }

    // =========================================================================
    // Cave formations — stalactites & stalagmites
    // =========================================================================

    /**
     * Adds stalactites (hanging from ceilings) and stalagmites (rising from floors)
     * along the edges of open spaces for visual detail.
     */
    private static void addFormations(char[][] tiles, Random rng, int levelIndex) {
        float chance = 0.12f + levelIndex * 0.02f;

        for (int x = BORDER; x < MAP_W - BORDER; x++) {
            for (int y = BORDER; y < MAP_H - BORDER; y++) {
                if (!isWall(tiles[x][y])) continue;

                // Stalactite: wall tile with open space below
                if (y + 1 < MAP_H && tiles[x][y + 1] == '.' && rng.nextFloat() < chance) {
                    int length = 1 + rng.nextInt(3);
                    for (int dy = 1; dy <= length; dy++) {
                        int ny = y + dy;
                        if (ny >= MAP_H - BORDER) break;
                        if (tiles[x][ny] != '.') break;
                        tiles[x][ny] = 's';
                    }
                }

                // Stalagmite: wall tile with open space above
                if (y - 1 >= 0 && tiles[x][y - 1] == '.' && rng.nextFloat() < chance) {
                    int length = 1 + rng.nextInt(2);
                    for (int dy = 1; dy <= length; dy++) {
                        int ny = y - dy;
                        if (ny < BORDER) break;
                        if (tiles[x][ny] != '.') break;
                        tiles[x][ny] = 's';
                    }
                }
            }
        }
    }

    // =========================================================================
    // Liquid pools
    // =========================================================================

    /**
     * Creates lakes of liquid in the bottom third of the map.
     * For each column in the lower zone, scans upward from the floor
     * and fills open tiles up to a water level, so liquid naturally
     * pools at the base of caverns forming connected lakes.
     */
    private static void settleLiquid(char[][] tiles, Random rng,
                                      boolean lava, int levelIndex) {
        char liquid = lava ? 'l' : 'w';

        // Only place liquid in the bottom third of the map
        int lakeZoneTop = MAP_H * 2 / 3;
        // Water level: how many tiles above a floor liquid can fill
        int maxDepth = 3 + levelIndex;

        for (int x = BORDER; x < MAP_W - BORDER; x++) {
            // Scan upward from the bottom, filling open tiles that sit on solid ground
            for (int y = MAP_H - BORDER - 1; y >= lakeZoneTop; y--) {
                if (tiles[x][y] != '.') continue;

                // Must be sitting on solid ground or existing liquid
                boolean onFloor = (y + 1 >= MAP_H - BORDER) ||
                                  isWall(tiles[x][y + 1]) ||
                                  tiles[x][y + 1] == liquid;
                if (!onFloor) continue;

                // Fill upward from the floor up to maxDepth tiles
                int filled = 0;
                for (int ly = y; ly >= lakeZoneTop && filled < maxDepth; ly--) {
                    if (tiles[x][ly] != '.') break;
                    tiles[x][ly] = liquid;
                    filled++;
                }
                break; // only fill the lowest cavity per column
            }
        }
    }

    // =========================================================================
    // Spawn & exit zones
    // =========================================================================

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

    private static void clearExitZone(char[][] tiles) {
        int ex = MAP_W - 6;
        int midY = MAP_H / 2;
        // Clear a wider area for the exit
        for (int dx = -1; dx <= 2; dx++) {
            for (int y = midY - 4; y <= midY + 4; y++) {
                int cx = ex + dx;
                if (cx >= 0 && cx < MAP_W && y >= 0 && y < MAP_H) {
                    tiles[cx][y] = '.';
                }
            }
        }
        // Floor below exit
        for (int dx = -1; dx <= 2; dx++) {
            int cx = ex + dx;
            if (cx >= 0 && cx < MAP_W && midY + 5 < MAP_H) {
                tiles[cx][midY + 5] = 's';
            }
        }
        tiles[ex][midY] = 'e';
    }

    // =========================================================================
    // Connectivity
    // =========================================================================

    private static boolean isPathConnected(char[][] tiles) {
        int startX = SPAWN_CLEAR_X + SPAWN_CLEAR_W / 2;
        int startY = MAP_H / 2;
        int exitX  = MAP_W - 6;
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
                if (isPassable(tiles[nx][ny])) {
                    boolean headRoom = true;
                    if (ny - 1 >= 0 && !isPassable(tiles[nx][ny - 1])) {
                        headRoom = false;
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
     * Carves a wide winding path from spawn to exit as a fallback.
     * Uses 5-tile-tall clearance and meanders vertically.
     */
    private static void carvePath(char[][] tiles, Random rng) {
        int x = SPAWN_CLEAR_X + SPAWN_CLEAR_W;
        int y = MAP_H / 2;
        int exitX = MAP_W - 6;
        int exitY = MAP_H / 2;

        while (x <= exitX) {
            // Carve a 5-tile-tall passage
            for (int dy = -2; dy <= 2; dy++) {
                int cy = y + dy;
                if (cy > BORDER && cy < MAP_H - BORDER) {
                    if (isWall(tiles[x][cy])) {
                        tiles[x][cy] = '.';
                    }
                }
            }

            if (x >= exitX - 8) {
                // Steer toward exit
                if (y < exitY) y++;
                else if (y > exitY) y--;
            } else {
                int drift = rng.nextInt(3) - 1;
                y = Math.max(BORDER + 3, Math.min(MAP_H - BORDER - 3, y + drift));

                if (rng.nextFloat() < 0.2f) {
                    int bigDrift = rng.nextInt(7) - 3;
                    y = Math.max(BORDER + 3, Math.min(MAP_H - BORDER - 3, y + bigDrift));
                }
            }
            x++;
        }
    }

    // =========================================================================
    // Floating islands
    // =========================================================================

    private static void placeFloatingIslands(char[][] tiles, Random rng, int levelIndex) {
        int numIslands = 20 + levelIndex * 5;

        for (int i = 0; i < numIslands; i++) {
            int platW = 3 + rng.nextInt(6);
            int px = BORDER + 6 + rng.nextInt(MAP_W - BORDER * 2 - 12 - platW);
            int py = BORDER + 4 + rng.nextInt(MAP_H - BORDER * 2 - 8);

            // Only place if there's a tall open area (at least 5 tiles clear above)
            boolean clear = true;
            for (int dx = -1; dx <= platW && clear; dx++) {
                for (int dy = -4; dy <= 1 && clear; dy++) {
                    int cx = px + dx, cy = py + dy;
                    if (cx < 0 || cx >= MAP_W || cy < 0 || cy >= MAP_H) continue;
                    if (isWall(tiles[cx][cy]) && dy <= 0) clear = false;
                }
            }
            if (!clear) continue;

            for (int dx = 0; dx < platW; dx++) {
                int tx = px + dx;
                if (tx > BORDER && tx < MAP_W - BORDER) {
                    tiles[tx][py] = 's';
                }
            }
        }
    }

    // =========================================================================
    // Tile helpers
    // =========================================================================

    private static boolean isWall(char c) {
        return c == 's' || c == 'd' || c == 'g';
    }

    private static boolean isPassable(char c) {
        return c == '.' || c == 'w' || c == 'l' || c == 'e';
    }

    // =========================================================================
    // File writing
    // =========================================================================

    private static void writeMapFile(String path, char[][] tiles, boolean lava) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.printf("%d %d %d %d%n", MAP_W, MAP_H, TILE_SIZE, TILE_SIZE);
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
            for (int y = 0; y < MAP_H; y++) {
                StringBuilder sb = new StringBuilder(MAP_W);
                for (int x = 0; x < MAP_W; x++) {
                    char c = tiles[x][y];
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
