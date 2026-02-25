package ui;

import spells.Wand;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the main menu, pause menu, death screen, win screen, settings,
 * and shop screen. All menu options are rendered as clickable buttons with
 * hover highlighting. Mouse position is fed in via {@link #setMousePos}.
 */
public class Menu {

    private static final Font TITLE_FONT  = new Font("Monospaced", Font.BOLD,  36);
    private static final Font BODY_FONT   = new Font("Monospaced", Font.PLAIN, 16);
    private static final Font SMALL_FONT  = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font CTRL_FONT   = new Font("Monospaced", Font.ITALIC, 12);

    private static final Color BTN_NORMAL  = new Color(180, 170, 200);
    private static final Color BTN_HOVER   = Color.WHITE;
    private static final Color BTN_BG_HOVER = new Color(255, 255, 255, 30);

    /** Animated title flicker accumulator */
    private float flickerPhase = 0f;

    /** Current mouse position for hover detection */
    private int mouseX, mouseY;

    /** Buttons registered during the last draw call */
    private final List<MenuButton> buttons = new ArrayList<>();

    // =========================================================================
    // Button tracking
    // =========================================================================

    /** A clickable menu button with a bounding rectangle and action id. */
    private static class MenuButton {
        final Rectangle bounds;
        final String action;
        MenuButton(Rectangle bounds, String action) {
            this.bounds = bounds;
            this.action = action;
        }
    }

    /** Sets the current mouse position for hover highlighting. */
    public void setMousePos(int x, int y) {
        mouseX = x;
        mouseY = y;
    }

    /**
     * Tests a click at (x,y) against the buttons drawn in the last frame.
     * @return The action string of the clicked button, or null if none hit.
     */
    public String handleClick(int x, int y) {
        for (MenuButton btn : buttons) {
            if (btn.bounds.contains(x, y)) {
                return btn.action;
            }
        }
        return null;
    }

    // =========================================================================
    // Update
    // =========================================================================

    public void update(long elapsed) {
        flickerPhase += 0.003f * elapsed;
    }

    // =========================================================================
    // Draw methods
    // =========================================================================

    public void drawMainMenu(Graphics2D g, int screenWidth, int screenHeight) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.92f);
        drawStarfield(g, screenWidth, screenHeight);

        // Title with flicker
        float alpha = 0.8f + 0.2f * (float) Math.sin(flickerPhase);
        drawCentredText(g, TITLE_FONT,
                new Color(180, 100, 255, (int)(alpha * 255)),
                "G L O R P I N G", screenWidth, screenHeight / 3 - 20);
        drawCentredText(g, BODY_FONT, new Color(120, 80, 200),
                "Cave Explorer", screenWidth, screenHeight / 3 + 20);

        // Clickable buttons
        int optY = screenHeight / 2 + 10;
        drawButton(g, BODY_FONT, "Start Game", screenWidth, optY, "start");
        drawButton(g, BODY_FONT, "Settings",   screenWidth, optY + 40, "settings");
        drawButton(g, BODY_FONT, "Quit",        screenWidth, optY + 80, "quit");

        // Controls hint
        int ctrlY = screenHeight - 100;
        drawCentredText(g, SMALL_FONT, new Color(160, 140, 180), "Controls:", screenWidth, ctrlY);
        drawCentredText(g, CTRL_FONT, new Color(140, 120, 160),
                "A/D or Arrows - Move    SPACE - Jump    LMB - Shoot",
                screenWidth, ctrlY + 20);
        drawCentredText(g, CTRL_FONT, new Color(140, 120, 160),
                "1-4 - Select Wand    Q - Next Wand    ESC - Pause",
                screenWidth, ctrlY + 38);
    }

    public void drawPauseMenu(Graphics2D g, int screenWidth, int screenHeight) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.65f);
        drawCentredText(g, TITLE_FONT, new Color(200, 160, 255), "PAUSED", screenWidth, screenHeight / 2 - 60);

        int optY = screenHeight / 2;
        drawButton(g, BODY_FONT, "Resume",   screenWidth, optY,      "resume");
        drawButton(g, BODY_FONT, "Settings", screenWidth, optY + 40, "settings");
        drawButton(g, BODY_FONT, "Main Menu", screenWidth, optY + 80, "main_menu");
    }

    public void drawDeathScreen(Graphics2D g, int screenWidth, int screenHeight, int gold) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.80f);

        float alpha = 0.7f + 0.3f * (float) Math.sin(flickerPhase * 0.7f);
        drawCentredText(g, TITLE_FONT,
                new Color(200, 40, 40, (int)(alpha * 255)),
                "YOU DIED", screenWidth, screenHeight / 2 - 60);
        drawCentredText(g, BODY_FONT, new Color(255, 215, 0),
                "Gold collected: " + gold, screenWidth, screenHeight / 2);

        int optY = screenHeight / 2 + 40;
        drawButton(g, BODY_FONT, "Restart",   screenWidth, optY,      "restart");
        drawButton(g, BODY_FONT, "Main Menu", screenWidth, optY + 40, "main_menu");
    }

    public void drawLevelCompleteScreen(Graphics2D g, int screenWidth, int screenHeight,
                                         boolean isLastLevel, int gold) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.75f);

        String title = isLastLevel ? "YOU WIN!" : "LEVEL COMPLETE";
        Color titleCol = isLastLevel ? new Color(255, 215, 0) : new Color(100, 255, 120);
        drawCentredText(g, TITLE_FONT, titleCol, title, screenWidth, screenHeight / 2 - 60);
        drawCentredText(g, BODY_FONT, new Color(255, 215, 0),
                "Gold: " + gold, screenWidth, screenHeight / 2);

        int optY = screenHeight / 2 + 40;
        if (!isLastLevel) {
            drawButton(g, BODY_FONT, "Next Level", screenWidth, optY, "next_level");
            drawButton(g, BODY_FONT, "Main Menu",  screenWidth, optY + 40, "main_menu");
        } else {
            drawButton(g, BODY_FONT, "Main Menu", screenWidth, optY, "main_menu");
        }
    }

    public void drawSettingsMenu(Graphics2D g, int screenWidth, int screenHeight,
                                  boolean godMode, boolean infiniteMana, boolean fullscreen,
                                  boolean debug, boolean musicOn) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.85f);

        drawCentredText(g, TITLE_FONT, new Color(200, 180, 255),
                "S E T T I N G S", screenWidth, screenHeight / 2 - 150);

        int rowY = screenHeight / 2 - 70;
        drawToggleButton(g, screenWidth, rowY,       "Music",          musicOn,      "toggle_music");
        drawToggleButton(g, screenWidth, rowY + 35,  "Fullscreen",     fullscreen,   "toggle_fullscreen");
        drawToggleButton(g, screenWidth, rowY + 70,  "Debug Overlay",  debug,        "toggle_debug");
        drawToggleButton(g, screenWidth, rowY + 115, "God Mode",       godMode,      "toggle_god");
        drawToggleButton(g, screenWidth, rowY + 150, "Infinite Mana",  infiniteMana, "toggle_mana");

        int actionY = rowY + 200;
        drawButton(g, BODY_FONT, "Restart Game", screenWidth, actionY,      "restart",
                new Color(200, 80, 80), new Color(255, 120, 120));
        drawButton(g, BODY_FONT, "Main Menu",    screenWidth, actionY + 40, "main_menu",
                new Color(200, 80, 80), new Color(255, 120, 120));
        drawButton(g, BODY_FONT, "Back",         screenWidth, actionY + 80, "back",
                new Color(150, 130, 170), BTN_HOVER);
    }

    /**
     * Draws the shop screen showing available upgrades.
     */
    public void drawShopMenu(Graphics2D g, int screenWidth, int screenHeight,
                              int gold, int maxHealth, int maxMana, Wand shopWand) {
        buttons.clear();
        drawDarkOverlay(g, screenWidth, screenHeight, 0.80f);

        drawCentredText(g, TITLE_FONT, new Color(255, 200, 80),
                "S H O P", screenWidth, 80);
        drawCentredText(g, BODY_FONT, new Color(255, 215, 0),
                "Gold: " + gold, screenWidth, 120);

        int optY = 160;
        int hpCost = 20 + (maxHealth - 100) / 10 * 5;
        int mpCost = 15 + (maxMana - 50) / 10 * 5;
        int healCost = 10;

        drawShopItem(g, screenWidth, optY,
                "Max Health +20", "Currently: " + maxHealth, hpCost, gold, "buy_hp");
        drawShopItem(g, screenWidth, optY + 65,
                "Max Mana +20", "Currently: " + maxMana, mpCost, gold, "buy_mp");
        drawShopItem(g, screenWidth, optY + 130,
                "Full Heal", "Restore all HP & MP", healCost, gold, "buy_heal");

        // Random wand for sale
        if (shopWand != null) {
            int wandCost = 15 + shopWand.getManaCost() * 2;
            drawShopItem(g, screenWidth, optY + 195,
                    shopWand.getDisplayName(), "Mana/shot: " + shopWand.getManaCost(),
                    wandCost, gold, "buy_wand");
        } else {
            // Already bought
            drawCentredText(g, SMALL_FONT, new Color(130, 130, 130),
                    "Wand sold!", screenWidth, optY + 220);
        }

        int bottomY = screenHeight - 60;
        drawButton(g, BODY_FONT, "Continue to Next Level", screenWidth, bottomY, "shop_continue",
                new Color(100, 255, 120), new Color(150, 255, 170));
    }

    // =========================================================================
    // Button drawing helpers
    // =========================================================================

    /** Draws a clickable button centred at the given Y. */
    private void drawButton(Graphics2D g, Font font, String label, int screenWidth, int y, String action) {
        drawButton(g, font, label, screenWidth, y, action, BTN_NORMAL, BTN_HOVER);
    }

    private void drawButton(Graphics2D g, Font font, String label, int screenWidth, int y, String action,
                              Color normalCol, Color hoverCol) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textH = fm.getHeight();
        int padX = 20, padY = 6;

        int bx = (screenWidth - textW) / 2 - padX;
        int by = y - textH + fm.getDescent() - padY;
        int bw = textW + padX * 2;
        int bh = textH + padY * 2;

        Rectangle rect = new Rectangle(bx, by, bw, bh);
        boolean hover = rect.contains(mouseX, mouseY);

        if (hover) {
            g.setColor(BTN_BG_HOVER);
            g.fillRoundRect(bx, by, bw, bh, 8, 8);
            g.setColor(new Color(255, 255, 255, 60));
            g.drawRoundRect(bx, by, bw, bh, 8, 8);
        }

        g.setColor(hover ? hoverCol : normalCol);
        g.drawString(label, (screenWidth - textW) / 2, y);

        buttons.add(new MenuButton(rect, action));
    }

    /** Draws a toggle button with ON/OFF badge. */
    private void drawToggleButton(Graphics2D g, int screenWidth, int y,
                                    String label, boolean enabled, String action) {
        g.setFont(BODY_FONT);
        FontMetrics fm = g.getFontMetrics();
        String badge = enabled ? " ON " : " OFF";
        Color badgeCol = enabled ? new Color(80, 220, 100) : new Color(200, 80, 80);

        String full = label + "    " + badge;
        int textW = fm.stringWidth(full);
        int textH = fm.getHeight();
        int padX = 20, padY = 6;

        int bx = (screenWidth - textW) / 2 - padX;
        int by = y - textH + fm.getDescent() - padY;
        int bw = textW + padX * 2;
        int bh = textH + padY * 2;

        Rectangle rect = new Rectangle(bx, by, bw, bh);
        boolean hover = rect.contains(mouseX, mouseY);

        if (hover) {
            g.setColor(BTN_BG_HOVER);
            g.fillRoundRect(bx, by, bw, bh, 8, 8);
            g.setColor(new Color(255, 255, 255, 60));
            g.drawRoundRect(bx, by, bw, bh, 8, 8);
        }

        // Draw label
        int startX = (screenWidth - textW) / 2;
        g.setColor(hover ? BTN_HOVER : BTN_NORMAL);
        g.drawString(label + "    ", startX, y);
        int labelW = fm.stringWidth(label + "    ");

        // Draw badge
        g.setColor(badgeCol);
        g.drawString(badge, startX + labelW, y);

        buttons.add(new MenuButton(rect, action));
    }

    /** Draws a shop item row with name, description, cost, and buy button. */
    private void drawShopItem(Graphics2D g, int screenWidth, int y,
                                String name, String desc, int cost, int gold, String action) {
        boolean canAfford = gold >= cost;

        g.setFont(BODY_FONT);
        FontMetrics fm = g.getFontMetrics();
        int totalW = 350;
        int bx = (screenWidth - totalW) / 2;

        // Background panel
        g.setColor(new Color(40, 30, 60, 180));
        g.fillRoundRect(bx, y, totalW, 55, 10, 10);
        g.setColor(new Color(100, 80, 140, 120));
        g.drawRoundRect(bx, y, totalW, 55, 10, 10);

        // Item name and description
        g.setColor(Color.WHITE);
        g.drawString(name, bx + 15, y + 22);
        g.setFont(SMALL_FONT);
        g.setColor(new Color(170, 160, 190));
        g.drawString(desc, bx + 15, y + 42);

        // Buy button on the right
        String buyLabel = cost + " G";
        g.setFont(BODY_FONT);
        int buyW = fm.stringWidth(buyLabel) + 20;
        int buyX = bx + totalW - buyW - 10;
        int buyY = y + 12;
        int buyH = 30;

        Rectangle buyRect = new Rectangle(buyX, buyY, buyW, buyH);
        boolean hover = buyRect.contains(mouseX, mouseY);

        Color bgCol = canAfford
                ? (hover ? new Color(80, 180, 80) : new Color(50, 120, 50))
                : new Color(80, 60, 60);
        g.setColor(bgCol);
        g.fillRoundRect(buyX, buyY, buyW, buyH, 6, 6);

        g.setColor(canAfford ? Color.WHITE : new Color(150, 130, 130));
        g.setFont(SMALL_FONT);
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(buyLabel, buyX + (buyW - fm2.stringWidth(buyLabel)) / 2,
                buyY + buyH / 2 + fm2.getAscent() / 2 - 2);

        if (canAfford) {
            buttons.add(new MenuButton(buyRect, action));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void drawDarkOverlay(Graphics2D g, int w, int h, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
    }

    private void drawCentredText(Graphics2D g, Font font, Color colour,
                                   String text, int screenWidth, int y) {
        g.setFont(font);
        g.setColor(colour);
        FontMetrics fm = g.getFontMetrics();
        int x = (screenWidth - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private void drawStarfield(Graphics2D g, int w, int h) {
        java.util.Random r = new java.util.Random(123);
        g.setColor(new Color(180, 160, 200, 80));
        for (int i = 0; i < 60; i++) {
            int sx = r.nextInt(w), sy = r.nextInt(h);
            g.fillRect(sx, sy, 1 + r.nextInt(2), 1 + r.nextInt(2));
        }
    }
}
