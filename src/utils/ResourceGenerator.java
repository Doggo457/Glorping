package utils;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Generates all game image and sound assets programmatically at startup.
 * Resources are saved as PNG/WAV files in the game's resource directories.
 * Existing files are not overwritten, allowing artists to replace assets
 * without code changes.
 *
 * All sprites are 32x32 pixels per frame drawn with Java2D geometric primitives
 * in a simple but visually distinct pixel-art style.
 */
public final class ResourceGenerator {

    /** Base path for image assets relative to working directory */
    private static final String IMG_PATH   = "src/resources/images/";
    /** Base path for map tile images */
    private static final String MAP_PATH   = "src/resources/maps/";
    /** Base path for sound assets */
    private static final String SND_PATH   = "src/resources/sounds/";

    /** Prevent instantiation */
    private ResourceGenerator() {}

    /**
     * Entry point – generates all missing assets.
     * Called from App.main() before the game starts.
     */
    public static void generate() {
        ensureDirs();
        generateTiles();
        generateWizardSheet();
        generateSlimeSheet();
        generateBatSheet();
        generateWizardEnemySheet();
        generateSkeletonSheet();
        generateSpiderSheet();
        generateGhostSheet();
        generateFireSpiritSheet();
        generateProjectileSheets();
        generateExplosionSheet();
        generatePickupImages();
        generateNpcShopkeeper();
        generateBackgroundLayers();
        generateSounds();
        System.out.println("ResourceGenerator: assets ready.");
    }

    // =========================================================================
    // Directory helpers
    // =========================================================================

    private static void ensureDirs() {
        new File(IMG_PATH).mkdirs();
        new File(MAP_PATH).mkdirs();
        new File(SND_PATH).mkdirs();
    }

    private static boolean needsCreate(String path) {
        return !new File(path).exists();
    }

    private static void saveImage(BufferedImage img, String path) {
        try {
            ImageIO.write(img, "png", new File(path));
        } catch (IOException e) {
            System.err.println("ResourceGenerator: failed to save " + path);
        }
    }

    // =========================================================================
    // Tile images (32×32 each, stored in maps/)
    // =========================================================================

    /** Generates all tile images used by the tilemap. */
    private static void generateTiles() {
        generateStone();
        generateDirt();
        generateGold();
        generateWaterTile();
        generateLavaTile();
        generateExitTile();
    }

    private static void generateStone() {
        String path = MAP_PATH + "stone.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Base dark grey
        g.setColor(new Color(55, 55, 60));
        g.fillRect(0, 0, 32, 32);
        // Lighter facets
        g.setColor(new Color(75, 75, 80));
        g.fillRect(0, 0, 16, 1);
        g.fillRect(0, 0, 1, 16);
        // Dark cracks
        g.setColor(new Color(30, 30, 33));
        g.drawLine(5, 8, 12, 15);
        g.drawLine(18, 3, 25, 10);
        g.drawLine(8, 20, 15, 28);
        g.drawLine(22, 18, 30, 24);
        // Highlight top-left corner
        g.setColor(new Color(90, 90, 95));
        g.drawLine(0, 0, 3, 0);
        g.drawLine(0, 0, 0, 3);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateDirt() {
        String path = MAP_PATH + "dirt.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(101, 67, 33));
        g.fillRect(0, 0, 32, 32);
        // Speckles
        g.setColor(new Color(80, 50, 20));
        int[] sx = {3,9,14,20,25,6,17,27,11,23};
        int[] sy = {4,11,6,15,22,25,29,8,20,3};
        for (int i = 0; i < sx.length; i++) g.fillRect(sx[i], sy[i], 2, 2);
        // Lighter specks
        g.setColor(new Color(130, 90, 50));
        int[] lx = {7,18,4,25,13};
        int[] ly = {17,5,28,12,24};
        for (int i = 0; i < lx.length; i++) g.fillRect(lx[i], ly[i], 2, 2);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateGold() {
        String path = MAP_PATH + "gold.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Stone base
        g.setColor(new Color(55, 55, 60));
        g.fillRect(0, 0, 32, 32);
        // Gold ore veins
        g.setColor(new Color(218, 165, 32));
        g.fillOval(6, 8, 8, 6);
        g.fillOval(17, 14, 10, 7);
        g.fillOval(10, 22, 12, 6);
        // Brighter highlights on ore
        g.setColor(new Color(255, 215, 0));
        g.fillOval(8, 9, 4, 3);
        g.fillOval(20, 15, 5, 3);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateWaterTile() {
        String path = MAP_PATH + "water.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 80, 180));
        g.fillRect(0, 0, 32, 32);
        // Wave highlights
        g.setColor(new Color(60, 130, 220));
        g.drawArc(0, 4, 16, 8, 0, 180);
        g.drawArc(16, 4, 16, 8, 0, 180);
        g.drawArc(0, 16, 16, 8, 0, 180);
        g.drawArc(16, 16, 16, 8, 0, 180);
        g.setColor(new Color(100, 160, 255, 120));
        g.fillRect(0, 0, 32, 6);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateLavaTile() {
        String path = MAP_PATH + "lava.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(180, 40, 0));
        g.fillRect(0, 0, 32, 32);
        // Bright orange blobs
        g.setColor(new Color(255, 120, 0));
        g.fillOval(4, 6, 10, 8);
        g.fillOval(18, 12, 10, 10);
        g.fillOval(8, 20, 12, 8);
        // Hot centre spots
        g.setColor(new Color(255, 220, 50));
        g.fillOval(8, 8, 4, 4);
        g.fillOval(22, 15, 4, 4);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateExitTile() {
        String path = MAP_PATH + "exit.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 20, 0));
        g.fillRect(0, 0, 32, 32);
        // Portal swirl
        g.setColor(new Color(0, 200, 80));
        g.drawOval(4, 4, 24, 24);
        g.drawOval(7, 7, 18, 18);
        g.setColor(new Color(100, 255, 150));
        g.fillOval(11, 11, 10, 10);
        // Arrow
        g.setColor(Color.WHITE);
        g.drawLine(13, 16, 19, 16);
        g.drawLine(17, 13, 20, 16);
        g.drawLine(17, 19, 20, 16);
        g.dispose();
        saveImage(img, path);
    }

    // =========================================================================
    // Player wizard sprite sheet (128×160, 4 cols × 5 rows, 32×32 per frame)
    // Row 0: Idle (frames 0–3)
    // Row 1: Walk (frames 4–7)
    // Row 2: Jump/Fall (frames 8–11)
    // Row 3: Cast spell (frames 12–15)
    // Row 4: Wall grab (frames 16–19)
    // =========================================================================

    /** Draws the player wizard sprite sheet. */
    private static void generateWizardSheet() {
        String path = IMG_PATH + "wizard.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(128, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();

        // Idle: subtle Y bob
        int[] bobY = {0, -1, -2, -1};
        for (int f = 0; f < 4; f++) drawWizard(g, f * 32, 0 + bobY[f], false, false);

        // Walk: leg swing
        int[] legSwing = {-3, 0, 3, 0};
        for (int f = 0; f < 4; f++) drawWizardWalk(g, f * 32, 32, legSwing[f]);

        // Jump / Fall
        for (int f = 0; f < 4; f++) drawWizardJump(g, f * 32, 64, f < 2);

        // Cast
        for (int f = 0; f < 4; f++) drawWizardCast(g, f * 32, 96, f);

        // Wall grab
        for (int f = 0; f < 4; f++) drawWizardWallGrab(g, f * 32, 128, f);

        // Dash (row 5, frames 20–23): wizard leaning forward with motion blur
        for (int f = 0; f < 4; f++) drawWizardDash(g, f * 32, 160, f);

        g.dispose();
        saveImage(sheet, path);
    }

    /** Draws a wizard at (bx, by) on the given Graphics2D context. */
    private static void drawWizard(Graphics2D g, int bx, int by, boolean left, boolean cast) {
        // Hat
        int[] hx = {bx+10, bx+16, bx+22, bx+20, bx+12};
        int[] hy = {by+12, by+2,  by+12, by+15, by+15};
        g.setColor(new Color(60, 0, 100));
        g.fillPolygon(hx, hy, 5);
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+8, by+13, 16, 3);
        // Face
        g.setColor(new Color(255, 210, 170));
        g.fillOval(bx+10, by+14, 12, 10);
        // Eyes
        g.setColor(Color.BLACK);
        g.fillRect(bx+12, by+17, 2, 2);
        g.fillRect(bx+18, by+17, 2, 2);
        // Robe body
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+8, by+24, 16, 8);
        // Arms
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+4,  by+24, 5, 3);
        g.fillRect(bx+23, by+24, 5, 3);
        // Staff (right side)
        g.setColor(new Color(139, 90, 43));
        g.drawLine(bx+27, by+24, bx+30, by+15);
        g.setColor(new Color(0, 180, 255));
        g.fillOval(bx+28, by+12, 5, 5);
    }

    private static void drawWizardWalk(Graphics2D g, int bx, int by, int legOff) {
        drawWizard(g, bx, by, false, false);
        // Legs with swing
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+10, by+28, 4, 4);
        g.fillRect(bx+18, by+28, 4, 4);
        g.setColor(new Color(40, 40, 80));
        g.fillRect(bx+10+legOff/2, by+29, 3, 3);
        g.fillRect(bx+18-legOff/2, by+29, 3, 3);
    }

    private static void drawWizardJump(Graphics2D g, int bx, int by, boolean ascending) {
        drawWizard(g, bx, by - (ascending ? 2 : 0), false, false);
        // Robe flares when jumping
        g.setColor(new Color(80, 0, 130));
        if (ascending) {
            g.fillRect(bx+6, by+27, 20, 4);
        } else {
            // Falling – arms spread slightly
            g.setColor(new Color(60, 0, 100));
            g.fillRect(bx+2, by+22, 6, 3);
            g.fillRect(bx+24, by+22, 6, 3);
        }
    }

    private static void drawWizardCast(Graphics2D g, int bx, int by, int frame) {
        drawWizard(g, bx, by, false, true);
        // Casting arm extended forward
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+24, by+22 - frame, 7, 3);
        // Spell glow at tip
        Color[] glows = {new Color(255,100,0), new Color(255,200,0),
                         new Color(100,100,255), new Color(255,255,255)};
        g.setColor(glows[frame % glows.length]);
        g.fillOval(bx+29, by+19 - frame, 6, 6);
    }

    private static void drawWizardWallGrab(Graphics2D g, int bx, int by, int frame) {
        // Wizard pressed flat against wall on right side, hands gripping edge
        // Body shifted right so hands touch x=31 (the wall boundary)

        // Hat – shifted right towards wall
        int[] hx = {bx+16, bx+22, bx+28, bx+26, bx+18};
        int[] hy = {by+10, by+1,  by+10, by+13, by+13};
        g.setColor(new Color(60, 0, 100));
        g.fillPolygon(hx, hy, 5);
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+14, by+11, 16, 3);
        // Face – looking towards wall
        g.setColor(new Color(255, 210, 170));
        g.fillOval(bx+16, by+12, 12, 10);
        // Eyes – looking at wall
        g.setColor(Color.BLACK);
        g.fillRect(bx+21, by+15, 2, 2);
        g.fillRect(bx+25, by+15, 2, 2);
        // Robe body – shifted right
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+14, by+22, 14, 10);
        // Upper arm reaching up to grip wall at x=31
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+27, by+12 - (frame % 2), 4, 10);
        // Upper hand gripping wall edge (touches x=31)
        g.setColor(new Color(255, 210, 170));
        g.fillRect(bx+28, by+10 - (frame % 2), 4, 4);
        // Lower arm gripping wall
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+27, by+23 + (frame % 2), 4, 5);
        // Lower hand gripping wall edge
        g.setColor(new Color(255, 210, 170));
        g.fillRect(bx+28, by+23 + (frame % 2), 4, 3);
        // Legs dangling loosely
        g.setColor(new Color(60, 0, 100));
        int legOff = (frame % 2 == 0) ? 0 : 2;
        g.fillRect(bx+16, by+30, 4, 3 + legOff);
        g.fillRect(bx+22, by+30, 4, 3 + (2 - legOff));
    }

    private static void drawWizardDash(Graphics2D g, int bx, int by, int frame) {
        // Wizard leaning forward with motion trails behind
        int lean = 3; // lean forward offset

        // Motion trail (fading afterimages behind)
        Composite old = g.getComposite();
        for (int t = 2; t >= 1; t--) {
            float alpha = 0.15f * t;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            int trailX = bx - t * 5 - frame * 2;
            drawWizard(g, trailX, by, false, false);
        }
        g.setComposite(old);

        // Main wizard body (leaning forward)
        // Hat – leaning forward
        int[] hx = {bx+10+lean, bx+16+lean, bx+22+lean, bx+20+lean, bx+12+lean};
        int[] hy = {by+14, by+4, by+14, by+17, by+17};
        g.setColor(new Color(60, 0, 100));
        g.fillPolygon(hx, hy, 5);
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+8+lean, by+15, 16, 3);
        // Face
        g.setColor(new Color(255, 210, 170));
        g.fillOval(bx+10+lean, by+16, 12, 10);
        // Eyes – determined
        g.setColor(Color.BLACK);
        g.fillRect(bx+13+lean, by+19, 2, 2);
        g.fillRect(bx+19+lean, by+19, 2, 2);
        // Robe body – leaning
        g.setColor(new Color(80, 0, 130));
        g.fillRect(bx+8+lean, by+26, 16, 6);
        // Arms swept back
        g.setColor(new Color(60, 0, 100));
        g.fillRect(bx+2, by+24, 7, 3);
        g.fillRect(bx+4, by+26, 5, 3);
        // Legs in running pose
        g.setColor(new Color(60, 0, 100));
        int step = (frame % 2 == 0) ? 3 : -2;
        g.fillRect(bx+10+lean+step, by+30, 4, 3);
        g.fillRect(bx+18+lean-step, by+30, 4, 3);
    }

    // =========================================================================
    // Enemy sprite sheets (64×32, 2 cols × 1 row, 32×32 per frame)
    // =========================================================================

    private static void generateSlimeSheet() {
        String path = IMG_PATH + "slime.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        // Frame 0: normal round blob
        g.setColor(new Color(50, 180, 50));
        g.fillOval(4, 8, 24, 20);
        g.setColor(new Color(80, 220, 80));
        g.fillOval(6, 6, 20, 16);
        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(9, 11, 4, 4);
        g.fillOval(19, 11, 4, 4);
        g.setColor(Color.WHITE);
        g.fillOval(10, 12, 2, 2);
        g.fillOval(20, 12, 2, 2);
        // Frame 1: squished wider
        g.setColor(new Color(50, 180, 50));
        g.fillOval(35, 16, 26, 14);
        g.setColor(new Color(80, 220, 80));
        g.fillOval(37, 14, 22, 12);
        g.setColor(Color.BLACK);
        g.fillOval(40, 17, 4, 3);
        g.fillOval(51, 17, 4, 3);
        g.setColor(Color.WHITE);
        g.fillOval(41, 17, 2, 2);
        g.fillOval(52, 17, 2, 2);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void generateBatSheet() {
        String path = IMG_PATH + "bat.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawBat(g, 0, 0, true);   // wings up
        drawBat(g, 32, 0, false); // wings down
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawBat(Graphics2D g, int bx, int by, boolean wingsUp) {
        g.setColor(new Color(30, 15, 50));
        // Body
        g.fillOval(bx+12, by+12, 10, 10);
        // Wings
        int wingY = wingsUp ? by + 5 : by + 16;
        // Left wing
        int[] lwx = {bx+12, bx+0,  bx+4};
        int[] lwy = {by+16, wingY, by+20};
        g.fillPolygon(lwx, lwy, 3);
        // Right wing
        int[] rwx = {bx+22, bx+32, bx+28};
        int[] rwy = {by+16, wingY, by+20};
        g.fillPolygon(rwx, rwy, 3);
        // Eyes
        g.setColor(new Color(220, 20, 20));
        g.fillOval(bx+13, by+14, 3, 3);
        g.fillOval(bx+18, by+14, 3, 3);
        // Ears
        g.setColor(new Color(30, 15, 50));
        g.fillPolygon(new int[]{bx+14,bx+12,bx+16}, new int[]{by+12,by+6,by+6}, 3);
        g.fillPolygon(new int[]{bx+18,bx+16,bx+20}, new int[]{by+12,by+6,by+6}, 3);
    }

    private static void generateWizardEnemySheet() {
        String path = IMG_PATH + "wiz_enemy.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        // Dark hostile wizard variant
        drawEnemyWizard(g, 0,  0, false);
        drawEnemyWizard(g, 32, 0, true);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawEnemyWizard(Graphics2D g, int bx, int by, boolean casting) {
        // Hat – red/dark
        int[] hx = {bx+10, bx+16, bx+22, bx+20, bx+12};
        int[] hy = {by+10, by+2,  by+10, by+13, by+13};
        g.setColor(new Color(120, 0, 0));
        g.fillPolygon(hx, hy, 5);
        g.setColor(new Color(160, 0, 0));
        g.fillRect(bx+8, by+11, 16, 3);
        // Face
        g.setColor(new Color(200, 160, 120));
        g.fillOval(bx+10, by+13, 12, 9);
        // Glowing eyes
        g.setColor(new Color(255, 80, 0));
        g.fillOval(bx+12, by+15, 3, 3);
        g.fillOval(bx+18, by+15, 3, 3);
        // Robe
        g.setColor(new Color(100, 0, 0));
        g.fillRect(bx+8, by+22, 16, 10);
        // Arm
        g.setColor(new Color(80, 0, 0));
        if (casting) {
            g.fillRect(bx+23, by+20, 8, 3);
            g.setColor(new Color(255, 100, 0));
            g.fillOval(bx+29, by+17, 5, 5);
        } else {
            g.fillRect(bx+23, by+22, 6, 3);
        }
    }

    private static void generateSkeletonSheet() {
        String path = IMG_PATH + "skeleton.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        // Frame 0: walk
        drawSkeleton(g, 0, 0, false);
        // Frame 1: attack (arm raised)
        drawSkeleton(g, 32, 0, true);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawSkeleton(Graphics2D g, int bx, int by, boolean attacking) {
        // Skull
        g.setColor(new Color(220, 210, 190));
        g.fillOval(bx + 10, by + 2, 12, 12);
        // Eye sockets
        g.setColor(new Color(40, 10, 10));
        g.fillOval(bx + 12, by + 5, 3, 4);
        g.fillOval(bx + 18, by + 5, 3, 4);
        // Jaw
        g.setColor(new Color(200, 190, 170));
        g.fillRect(bx + 12, by + 11, 8, 3);
        // Ribcage
        g.setColor(new Color(210, 200, 180));
        g.fillRect(bx + 12, by + 14, 8, 8);
        g.setColor(new Color(40, 30, 30));
        for (int i = 0; i < 3; i++) {
            g.drawLine(bx + 13, by + 16 + i * 2, bx + 19, by + 16 + i * 2);
        }
        // Legs
        g.setColor(new Color(200, 190, 170));
        g.fillRect(bx + 12, by + 22, 3, 8);
        g.fillRect(bx + 17, by + 22, 3, 8);
        // Arms
        if (attacking) {
            g.fillRect(bx + 8, by + 14, 4, 3);
            g.fillRect(bx + 5, by + 8, 3, 6);
            // Sword raised
            g.setColor(new Color(180, 180, 200));
            g.fillRect(bx + 5, by + 1, 2, 9);
        } else {
            g.setColor(new Color(200, 190, 170));
            g.fillRect(bx + 8, by + 14, 4, 3);
            g.fillRect(bx + 20, by + 14, 4, 3);
        }
    }

    private static void generateSpiderSheet() {
        String path = IMG_PATH + "spider.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        // Frame 0: crawl
        drawSpider(g, 0, 0, false);
        // Frame 1: leap
        drawSpider(g, 32, 0, true);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawSpider(Graphics2D g, int bx, int by, boolean leaping) {
        int bodyY = leaping ? by + 10 : by + 14;
        // Body
        g.setColor(new Color(50, 35, 25));
        g.fillOval(bx + 10, bodyY, 12, 8);
        // Head
        g.setColor(new Color(60, 40, 30));
        g.fillOval(bx + 7, bodyY + 1, 7, 6);
        // Eyes (multiple small red dots)
        g.setColor(new Color(200, 20, 20));
        g.fillRect(bx + 8, bodyY + 2, 2, 1);
        g.fillRect(bx + 11, bodyY + 2, 2, 1);
        g.fillRect(bx + 8, bodyY + 4, 2, 1);
        g.fillRect(bx + 11, bodyY + 4, 2, 1);
        // Legs (4 per side)
        g.setColor(new Color(45, 30, 20));
        int legSpread = leaping ? 4 : 2;
        for (int i = 0; i < 4; i++) {
            int lx = bx + 11 + i * 3;
            // Left legs (going up-left)
            g.drawLine(lx, bodyY + 4, lx - 5 - legSpread, bodyY + 10 + (leaping ? -4 : 0));
            // Right legs (going up-right)
            g.drawLine(lx, bodyY + 4, lx + 5 + legSpread, bodyY + 10 + (leaping ? -4 : 0));
        }
    }

    private static void generateGhostSheet() {
        String path = IMG_PATH + "ghost.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawGhost(g, 0, 0);
        drawGhost(g, 32, 0);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawGhost(Graphics2D g, int bx, int by) {
        // Translucent body
        g.setColor(new Color(180, 200, 220, 160));
        g.fillOval(bx + 6, by + 2, 20, 18);
        // Wavy bottom
        g.fillRect(bx + 6, by + 15, 20, 6);
        for (int i = 0; i < 4; i++) {
            int wx = bx + 7 + i * 5;
            g.fillOval(wx, by + 18, 5, 8);
        }
        // Dark eyes
        g.setColor(new Color(20, 20, 60, 200));
        g.fillOval(bx + 11, by + 8, 4, 5);
        g.fillOval(bx + 18, by + 8, 4, 5);
        // Mouth
        g.fillOval(bx + 14, by + 15, 5, 3);
    }

    private static void generateFireSpiritSheet() {
        String path = IMG_PATH + "fire_spirit.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawFireSpirit(g, 0, 0);
        drawFireSpirit(g, 32, 0);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawFireSpirit(Graphics2D g, int bx, int by) {
        // Fiery core
        g.setColor(new Color(255, 120, 0, 200));
        g.fillOval(bx + 8, by + 8, 16, 16);
        g.setColor(new Color(255, 200, 50, 180));
        g.fillOval(bx + 10, by + 10, 12, 12);
        g.setColor(new Color(255, 255, 180, 160));
        g.fillOval(bx + 12, by + 12, 8, 8);
        // Flame tendrils
        g.setColor(new Color(255, 80, 0, 150));
        int[] fx1 = {bx + 10, bx + 14, bx + 18};
        int[] fy1 = {by + 8, by + 1, by + 8};
        g.fillPolygon(fx1, fy1, 3);
        int[] fx2 = {bx + 6, bx + 10, bx + 14};
        int[] fy2 = {by + 12, by + 4, by + 12};
        g.fillPolygon(fx2, fy2, 3);
        int[] fx3 = {bx + 18, bx + 22, bx + 26};
        int[] fy3 = {by + 12, by + 4, by + 12};
        g.fillPolygon(fx3, fy3, 3);
        // Eyes
        g.setColor(new Color(40, 10, 0));
        g.fillOval(bx + 13, by + 14, 3, 3);
        g.fillOval(bx + 18, by + 14, 3, 3);
    }

    // =========================================================================
    // Projectile sprite sheets (64×32, 2 frames each)
    // =========================================================================

    private static void generateProjectileSheets() {
        generateFireball();
        generateLightning();
        generateMagicMissile();
        generateSpark();
    }

    private static void generateFireball() {
        String path = IMG_PATH + "fireball.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawFireball(g, 0,  0, 14);
        drawFireball(g, 32, 0, 12);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawFireball(Graphics2D g, int bx, int by, int size) {
        g.setColor(new Color(255, 80, 0, 120));
        g.fillOval(bx+16-size, by+16-size, size*2, size*2);
        g.setColor(new Color(255, 160, 0));
        g.fillOval(bx+16-size+3, by+16-size+3, size*2-6, size*2-6);
        g.setColor(new Color(255, 255, 100));
        g.fillOval(bx+16-4, by+16-4, 8, 8);
    }

    private static void generateLightning() {
        String path = IMG_PATH + "lightning.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Frame 0 – thin bolt
        g.setColor(new Color(180, 180, 255, 180));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, 16, 8, 10); g.drawLine(8, 10, 14, 20); g.drawLine(14, 20, 20, 8);
        g.drawLine(20, 8, 26, 18); g.drawLine(26, 18, 32, 14);
        g.setColor(new Color(255, 255, 255));
        g.setStroke(new BasicStroke(1));
        g.drawLine(2, 16, 10, 10); g.drawLine(10, 10, 15, 20); g.drawLine(15, 20, 21, 8);
        // Frame 1 – brighter
        g.setColor(new Color(100, 100, 255, 200));
        g.setStroke(new BasicStroke(3));
        g.drawLine(32, 16, 40, 9); g.drawLine(40, 9, 46, 19); g.drawLine(46, 19, 52, 7);
        g.drawLine(52, 7, 58, 17); g.drawLine(58, 17, 64, 13);
        g.setColor(new Color(255, 255, 255));
        g.setStroke(new BasicStroke(1));
        g.drawLine(34, 16, 42, 9); g.drawLine(42, 9, 47, 19); g.drawLine(47, 19, 53, 7);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void generateMagicMissile() {
        String path = IMG_PATH + "magic_missile.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawOrb(g, 0,  0, new Color(180, 100, 255), new Color(220, 160, 255), 12);
        drawOrb(g, 32, 0, new Color(150, 80,  220), new Color(200, 130, 255), 10);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void generateSpark() {
        String path = IMG_PATH + "spark.png";
        if (!needsCreate(path)) return;
        BufferedImage sheet = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        drawOrb(g, 0,  0, new Color(255, 160, 0), new Color(255, 220, 80), 8);
        drawOrb(g, 32, 0, new Color(255, 100, 0), new Color(255, 180, 50), 6);
        g.dispose();
        saveImage(sheet, path);
    }

    private static void drawOrb(Graphics2D g, int bx, int by, Color outer, Color inner, int r) {
        g.setColor(new Color(outer.getRed(), outer.getGreen(), outer.getBlue(), 100));
        g.fillOval(bx+16-r-2, by+16-r-2, (r+2)*2, (r+2)*2);
        g.setColor(outer);
        g.fillOval(bx+16-r, by+16-r, r*2, r*2);
        g.setColor(inner);
        g.fillOval(bx+16-r/2, by+16-r/2, r, r);
        g.setColor(Color.WHITE);
        g.fillOval(bx+13, by+13, 4, 4);
    }

    private static void generateExplosionSheet() {
        String path = IMG_PATH + "explosion.png";
        if (!needsCreate(path)) return;
        // 4 frames: expanding circle
        BufferedImage sheet = new BufferedImage(128, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        int[] radii = {4, 9, 14, 18};
        Color[] outer = {new Color(255,200,0,255), new Color(255,140,0,220),
                         new Color(200,80,0,160),  new Color(120,50,0,80)};
        for (int f = 0; f < 4; f++) {
            int bx = f * 32;
            int r = radii[f];
            g.setColor(outer[f]);
            g.fillOval(bx+16-r, 16-r, r*2, r*2);
            if (r > 6) {
                g.setColor(new Color(255, 230, 100, outer[f].getAlpha()));
                g.fillOval(bx+16-r/2, 16-r/2, r, r);
            }
        }
        g.dispose();
        saveImage(sheet, path);
    }

    // =========================================================================
    // Pickup images (32×32 each)
    // =========================================================================

    private static void generatePickupImages() {
        generatePotion(IMG_PATH + "health_potion.png", new Color(200, 30, 30), new Color(255, 80, 80));
        generatePotion(IMG_PATH + "mana_potion.png",   new Color(20, 60, 200), new Color(80, 130, 255));
        generateWandItem();
        generateGoldNugget();
    }

    private static void generatePotion(String path, Color base, Color bright) {
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Bottle body
        g.setColor(base);
        g.fillOval(9, 16, 14, 14);
        g.fillRect(12, 10, 8, 8);
        // Cork
        g.setColor(new Color(139, 90, 43));
        g.fillRect(13, 7, 6, 5);
        // Shine
        g.setColor(bright);
        g.fillOval(11, 20, 5, 4);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateWandItem() {
        String path = IMG_PATH + "wand_item.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Wand shaft
        g.setColor(new Color(139, 90, 43));
        g.setStroke(new BasicStroke(3));
        g.drawLine(5, 27, 23, 9);
        // Gem tip
        g.setColor(new Color(0, 200, 255));
        g.fillOval(20, 6, 8, 8);
        g.setColor(Color.WHITE);
        g.fillOval(21, 7, 3, 3);
        // Glow
        g.setColor(new Color(0, 200, 255, 80));
        g.fillOval(17, 3, 14, 14);
        g.dispose();
        saveImage(img, path);
    }

    private static void generateGoldNugget() {
        String path = IMG_PATH + "gold_nugget.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Gold chunk
        g.setColor(new Color(180, 140, 20));
        g.fillOval(8, 12, 16, 14);
        // Bright face
        g.setColor(new Color(255, 215, 0));
        g.fillOval(10, 13, 12, 10);
        // Highlight
        g.setColor(new Color(255, 245, 150));
        g.fillOval(12, 14, 5, 4);
        g.dispose();
        saveImage(img, path);
    }

    // =========================================================================
    // NPC shopkeeper (32×32 single frame)
    // =========================================================================

    private static void generateNpcShopkeeper() {
        String path = IMG_PATH + "npc_shopkeeper.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Hat – merchant style (green)
        g.setColor(new Color(40, 120, 40));
        g.fillRect(8, 4, 16, 6);
        g.fillRect(6, 10, 20, 3);
        // Face
        g.setColor(new Color(255, 210, 170));
        g.fillOval(10, 12, 12, 10);
        // Eyes – friendly
        g.setColor(Color.BLACK);
        g.fillOval(13, 15, 2, 2);
        g.fillOval(19, 15, 2, 2);
        // Smile
        g.setColor(Color.BLACK);
        g.drawArc(13, 16, 6, 4, 180, 180);
        // Body – merchant robe (green)
        g.setColor(new Color(50, 140, 50));
        g.fillRect(8, 22, 16, 10);
        // Gold coin badge
        g.setColor(new Color(255, 215, 0));
        g.fillOval(13, 24, 6, 6);
        g.setColor(new Color(200, 170, 0));
        g.drawOval(13, 24, 6, 6);
        g.dispose();
        saveImage(img, path);
    }

    // =========================================================================
    // Parallax background layers
    // =========================================================================

    private static void generateBackgroundLayers() {
        generateFarBackground();
        generateMidBackground();
        generateNearBackground();
    }

    private static void generateFarBackground() {
        String path = IMG_PATH + "bg_far.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Deep dark cave gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(5, 5, 15),
                                              0, 600, new Color(10, 5, 20));
        g.setPaint(gp);
        g.fillRect(0, 0, 800, 600);
        // Distant stars/sparkles
        java.util.Random rng = new java.util.Random(42);
        g.setColor(new Color(150, 150, 200, 120));
        for (int i = 0; i < 80; i++) {
            int sx = rng.nextInt(800), sy = rng.nextInt(400);
            g.fillRect(sx, sy, 1 + rng.nextInt(2), 1 + rng.nextInt(2));
        }
        g.dispose();
        saveImage(img, path);
    }

    private static void generateMidBackground() {
        String path = IMG_PATH + "bg_mid.png";
        if (!needsCreate(path)) return;
        // Wide image for slow parallax scrolling
        BufferedImage img = new BufferedImage(1600, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(8, 8, 18));
        g.fillRect(0, 0, 1600, 600);
        // Rocky silhouette shapes in mid-distance
        g.setColor(new Color(20, 20, 35));
        java.util.Random rng = new java.util.Random(99);
        for (int x = 0; x < 1600; x += 60 + rng.nextInt(40)) {
            int h = 80 + rng.nextInt(120);
            g.fillRect(x, 600 - h, 50 + rng.nextInt(30), h);
        }
        // Stalactites from ceiling
        for (int x = 30; x < 1600; x += 80 + rng.nextInt(60)) {
            int h = 40 + rng.nextInt(80);
            int w = 10 + rng.nextInt(20);
            int[] sx = {x, x+w/2, x+w};
            int[] sy = {0, h, 0};
            g.fillPolygon(sx, sy, 3);
        }
        g.dispose();
        saveImage(img, path);
    }

    private static void generateNearBackground() {
        String path = IMG_PATH + "bg_near.png";
        if (!needsCreate(path)) return;
        BufferedImage img = new BufferedImage(2400, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(12, 10, 22));
        g.fillRect(0, 0, 2400, 600);
        // Close cave walls – darker, bigger shapes
        g.setColor(new Color(18, 15, 30));
        java.util.Random rng = new java.util.Random(77);
        for (int x = 0; x < 2400; x += 80 + rng.nextInt(60)) {
            int h = 100 + rng.nextInt(150);
            g.fillRect(x, 600 - h, 60 + rng.nextInt(40), h);
        }
        // Large near stalactites
        g.setColor(new Color(25, 20, 40));
        for (int x = 20; x < 2400; x += 120 + rng.nextInt(80)) {
            int h = 60 + rng.nextInt(100);
            int w = 20 + rng.nextInt(30);
            int[] sx = {x, x+w/2, x+w};
            int[] sy = {0, h, 0};
            g.fillPolygon(sx, sy, 3);
        }
        g.dispose();
        saveImage(img, path);
    }

    // =========================================================================
    // Sound generation – simple synthesised WAV files
    // =========================================================================

    /** Generates all required sound effect WAV files. */
    private static void generateSounds() {
        // shoot: rising zap (400→800Hz sweep)
        generateSweepSound(SND_PATH + "shoot.wav", 400, 800, 0.15f, 0.6f);
        // hit: short thud
        generateToneSound(SND_PATH + "hit.wav", 180, 0.1f, 0.8f, true);
        // player death: descending tone
        generateSweepSound(SND_PATH + "player_death.wav", 600, 100, 0.8f, 0.5f);
        // jump: quick ascending blip
        generateSweepSound(SND_PATH + "jump.wav", 300, 600, 0.12f, 0.5f);
        // pickup: pleasant arpeggio (three notes)
        generatePickupSound(SND_PATH + "pickup.wav");
        // enemy death: short descending
        generateSweepSound(SND_PATH + "enemy_death.wav", 400, 120, 0.3f, 0.6f);
        // explosion: noise burst
        generateNoiseSound(SND_PATH + "explosion.wav", 0.5f, 0.7f);
        // footstep: very short low thud
        generateToneSound(SND_PATH + "footstep.wav", 80, 0.06f, 0.4f, true);
    }

    /**
     * Generates a frequency-sweep WAV (e.g. rising zap or falling tone).
     *
     * @param path      Output file path
     * @param startHz   Starting frequency in Hz
     * @param endHz     Ending frequency in Hz
     * @param durationS Duration in seconds
     * @param vol       Peak amplitude 0.0–1.0
     */
    private static void generateSweepSound(String path, float startHz, float endHz,
                                            float durationS, float vol) {
        if (!needsCreate(path)) return;
        int sampleRate = 44100;
        int numSamples = (int)(sampleRate * durationS);
        byte[] buf = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double freq = startHz + (endHz - startHz) * t / durationS;
            double envelope = Math.exp(-3.0 * t / durationS);
            double sample = Math.sin(2 * Math.PI * freq * t) * envelope * vol;
            short s = (short)(sample * Short.MAX_VALUE);
            buf[2 * i]     = (byte)(s & 0xFF);
            buf[2 * i + 1] = (byte)((s >> 8) & 0xFF);
        }
        writeWav(path, buf, sampleRate);
    }

    /**
     * Generates a simple sustained tone WAV.
     *
     * @param path      Output file path
     * @param hz        Frequency in Hz
     * @param durationS Duration in seconds
     * @param vol       Peak amplitude
     * @param percussive True to apply fast exponential decay
     */
    private static void generateToneSound(String path, float hz, float durationS,
                                           float vol, boolean percussive) {
        if (!needsCreate(path)) return;
        int sampleRate = 44100;
        int numSamples = (int)(sampleRate * durationS);
        byte[] buf = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double envelope = percussive ? Math.exp(-8.0 * t / durationS) : 1.0;
            double sample = Math.sin(2 * Math.PI * hz * t) * envelope * vol;
            short s = (short)(sample * Short.MAX_VALUE);
            buf[2 * i]     = (byte)(s & 0xFF);
            buf[2 * i + 1] = (byte)((s >> 8) & 0xFF);
        }
        writeWav(path, buf, sampleRate);
    }

    /**
     * Generates a short ascending three-note arpeggio for item pickups.
     *
     * @param path Output file path
     */
    private static void generatePickupSound(String path) {
        if (!needsCreate(path)) return;
        int sampleRate = 44100;
        float[] notes = {523.25f, 659.25f, 783.99f}; // C5, E5, G5
        float noteDur = 0.08f;
        int samplesPerNote = (int)(sampleRate * noteDur);
        int total = samplesPerNote * notes.length;
        byte[] buf = new byte[total * 2];
        for (int n = 0; n < notes.length; n++) {
            for (int i = 0; i < samplesPerNote; i++) {
                double t = (double) i / sampleRate;
                double env = Math.exp(-5.0 * t / noteDur);
                double s = Math.sin(2 * Math.PI * notes[n] * t) * env * 0.6;
                short sh = (short)(s * Short.MAX_VALUE);
                int idx = (n * samplesPerNote + i) * 2;
                buf[idx]     = (byte)(sh & 0xFF);
                buf[idx + 1] = (byte)((sh >> 8) & 0xFF);
            }
        }
        writeWav(path, buf, sampleRate);
    }

    /**
     * Generates a white-noise burst for explosions.
     *
     * @param path      Output file path
     * @param durationS Duration in seconds
     * @param vol       Volume
     */
    private static void generateNoiseSound(String path, float durationS, float vol) {
        if (!needsCreate(path)) return;
        int sampleRate = 44100;
        int numSamples = (int)(sampleRate * durationS);
        byte[] buf = new byte[numSamples * 2];
        java.util.Random rng = new java.util.Random(0);
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double envelope = Math.exp(-4.0 * t / durationS);
            double sample = (rng.nextDouble() * 2 - 1) * envelope * vol;
            short s = (short)(sample * Short.MAX_VALUE);
            buf[2 * i]     = (byte)(s & 0xFF);
            buf[2 * i + 1] = (byte)((s >> 8) & 0xFF);
        }
        writeWav(path, buf, sampleRate);
    }

    /**
     * Writes raw 16-bit mono PCM samples to a WAV file.
     *
     * @param path       Destination file path
     * @param pcmData    Raw 16-bit little-endian PCM samples
     * @param sampleRate Sample rate in Hz
     */
    private static void writeWav(String path, byte[] pcmData, int sampleRate) {
        try {
            AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(pcmData), fmt,
                    pcmData.length / fmt.getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(path));
        } catch (IOException e) {
            System.err.println("ResourceGenerator: failed to write WAV " + path);
        }
    }
}
