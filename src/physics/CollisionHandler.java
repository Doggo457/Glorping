package physics;

import entities.Entity;
import game2D.Sprite;
import game2D.TileMap;
import game2D.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles multi-level collision detection and resolution.
 *
 * Two levels of analysis are used:
 * <ol>
 *   <li><b>Broad phase</b> – bounding-box or bounding-circle overlap test to
 *       quickly discard non-colliding pairs.</li>
 *   <li><b>Narrow phase</b> – precise corner/edge point sampling against the
 *       TileMap to find which tiles an entity overlaps and resolve the
 *       penetration by repositioning the entity outside solid tiles.</li>
 * </ol>
 *
 * Tiles whose character is '.' (empty), 'w' (water), 'l' (lava), or 'e' (exit)
 * are treated as passable; all other characters are solid.
 */
public final class CollisionHandler {

    /** Characters treated as non-solid (passable) */
    private static final String PASSABLE = ".wle";

    /** Inset from sprite edges to avoid catching on adjacent tiles */
    private static final int INSET = 6;

    /** Prevent instantiation */
    private CollisionHandler() {}

    // =========================================================================
    // Entity vs Tile Map
    // =========================================================================

    /**
     * Resolves collisions between an entity and the tile map.
     * Checks eight points around the entity's bounding box and pushes the
     * entity out of any solid tiles it overlaps.
     * Also sets {@code entity.setOnGround(true)} when the entity is resting
     * on a solid tile below it.
     *
     * @param entity Entity to test and resolve
     * @param tmap   Tile map to test against
     * @return List of tiles that were collided with this frame (for debug/events)
     */
    public static List<Tile> resolveEntityTileCollision(Entity entity, TileMap tmap) {
        List<Tile> hit = new ArrayList<>();

        float x  = entity.getX();
        float y  = entity.getY();
        int   w  = entity.getWidth();
        int   h  = entity.getHeight();
        int   tw = tmap.getTileWidth();
        int   th = tmap.getTileHeight();

        // --- Bottom edge (ground detection) ---
        float bY  = y + h;
        float lBX = x + INSET;
        float rBX = x + w - INSET;
        float cX  = x + w / 2f;

        boolean bottomHit = false;
        bottomHit |= checkAndRecord(tmap, lBX, bY, hit);
        bottomHit |= checkAndRecord(tmap, rBX, bY, hit);
        bottomHit |= checkAndRecord(tmap, cX,  bY, hit);

        if (bottomHit && entity.getVelocityY() >= 0) {
            int tileRow = (int)(bY / th);
            entity.setY(tileRow * th - h);
            entity.setVelocityY(0);
            entity.setOnGround(true);
        }

        // --- Top edge (ceiling) ---
        float tY = y;
        boolean topHit = false;
        topHit |= checkAndRecord(tmap, lBX, tY, hit);
        topHit |= checkAndRecord(tmap, rBX, tY, hit);
        topHit |= checkAndRecord(tmap, cX,  tY, hit);

        if (topHit && entity.getVelocityY() < 0) {
            int tileRow = (int)(tY / th) + 1;
            entity.setY(tileRow * th);
            entity.setVelocityY(0);
        }

        // Re-read position after vertical resolution so horizontal checks
        // use the corrected Y (prevents sideways push when landing on blocks)
        x = entity.getX();
        y = entity.getY();

        // --- Left edge ---
        float lX  = x;
        float midY = y + h / 2f;
        float topEdgeY  = y + INSET;
        float botEdgeY  = y + h - INSET;

        boolean leftHit = false;
        leftHit |= checkAndRecord(tmap, lX, topEdgeY, hit);
        leftHit |= checkAndRecord(tmap, lX, botEdgeY, hit);
        leftHit |= checkAndRecord(tmap, lX, midY, hit);

        if (leftHit && entity.getVelocityX() <= 0) {
            int tileCol = (int)(lX / tw) + 1;
            entity.setX(tileCol * tw);
            entity.setVelocityX(0);
        }

        // --- Right edge ---
        float rX = x + w;
        boolean rightHit = false;
        rightHit |= checkAndRecord(tmap, rX, topEdgeY, hit);
        rightHit |= checkAndRecord(tmap, rX, botEdgeY, hit);
        rightHit |= checkAndRecord(tmap, rX, midY, hit);

        if (rightHit && entity.getVelocityX() >= 0) {
            int tileCol = (int)(rX / tw);
            entity.setX(tileCol * tw - w);
            entity.setVelocityX(0);
        }

        return hit;
    }

    /**
     * Resolves tile collisions for a projectile sprite (simplified:
     * only checks the centre point).
     *
     * @param sprite Projectile sprite to test
     * @param tmap   Tile map
     * @return The tile character at the projectile's centre, or '.' if none
     */
    public static char resolveProjectileTileCollision(Sprite sprite, TileMap tmap) {
        float cx = sprite.getX() + sprite.getWidth()  / 2f;
        float cy = sprite.getY() + sprite.getHeight() / 2f;
        int tx = (int)(cx / tmap.getTileWidth());
        int ty = (int)(cy / tmap.getTileHeight());
        if (!tmap.valid(tx, ty)) return 's'; // treat out-of-bounds as solid
        return tmap.getTileChar(tx, ty);
    }

    // =========================================================================
    // Entity vs Entity
    // =========================================================================

    /**
     * Broad-phase bounding-box overlap test between two sprites.
     *
     * @param s1 First sprite
     * @param s2 Second sprite
     * @return True if the bounding boxes overlap
     */
    public static boolean boundingBoxCollision(Sprite s1, Sprite s2) {
        return s1.getX() < s2.getX() + s2.getWidth()  &&
               s1.getX() + s1.getWidth()  > s2.getX() &&
               s1.getY() < s2.getY() + s2.getHeight() &&
               s1.getY() + s1.getHeight() > s2.getY();
    }

    /**
     * Bounding-circle overlap test between two sprites.
     * Uses the sprite's radius as computed by the Sprite class (half the
     * larger dimension).
     *
     * @param s1 First sprite
     * @param s2 Second sprite
     * @return True if the bounding circles overlap
     */
    public static boolean boundingCircleCollision(Sprite s1, Sprite s2) {
        float cx1 = s1.getX() + s1.getWidth()  / 2f;
        float cy1 = s1.getY() + s1.getHeight() / 2f;
        float cx2 = s2.getX() + s2.getWidth()  / 2f;
        float cy2 = s2.getY() + s2.getHeight() / 2f;
        float dx = cx1 - cx2;
        float dy = cy1 - cy2;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        return dist < (s1.getRadius() + s2.getRadius());
    }

    // =========================================================================
    // Tile query helpers
    // =========================================================================

    /**
     * Checks if the pixel point {@code (px, py)} lies in a solid tile
     * and records it in the hit list if so.
     *
     * @param tmap Tile map
     * @param px   World pixel X
     * @param py   World pixel Y
     * @param hit  List to add the tile to if solid
     * @return True if the tile is solid
     */
    private static boolean checkAndRecord(TileMap tmap, float px, float py, List<Tile> hit) {
        int tx = (int)(px / tmap.getTileWidth());
        int ty = (int)(py / tmap.getTileHeight());
        if (!tmap.valid(tx, ty)) return true; // out-of-bounds = solid border
        char c = tmap.getTileChar(tx, ty);
        if (PASSABLE.indexOf(c) < 0) {
            Tile t = tmap.getTile(tx, ty);
            if (t != null && !hit.contains(t)) hit.add(t);
            return true;
        }
        return false;
    }

    /**
     * Returns true if the tile at pixel position (px, py) is solid.
     *
     * @param tmap Tile map
     * @param px   World pixel X
     * @param py   World pixel Y
     * @return True if solid
     */
    public static boolean isSolid(TileMap tmap, float px, float py) {
        int tx = (int)(px / tmap.getTileWidth());
        int ty = (int)(py / tmap.getTileHeight());
        if (!tmap.valid(tx, ty)) return true;
        return PASSABLE.indexOf(tmap.getTileChar(tx, ty)) < 0;
    }

    /**
     * Returns true if the tile character represents a harmful tile
     * (lava or explosion residue) that should damage entities.
     *
     * @param c Tile character
     * @return True if harmful
     */
    public static boolean isHarmful(char c) {
        return c == 'l'; // lava
    }
}
