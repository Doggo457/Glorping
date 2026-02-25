package ui;

import entities.Player;
import spells.Wand;
import java.awt.*;
import java.util.List;

/**
 * Renders the in-game heads-up display (HUD).
 * Draws health bar, mana bar, wand inventory, gold counter,
 * and a brief on-screen message system.
 * All elements are drawn in screen space (not affected by camera offset).
 */
public class HUD {

    private static final int BAR_W = 150;
    private static final int BAR_H = 14;
    private static final int MARGIN = 12;
    /** Extra top offset to avoid the JFrame title bar area */
    private static final int TOP_Y = 36;

    /** Temporary on-screen message text */
    private String message = "";
    /** Remaining display time for the message in ms */
    private long messageTimer = 0;

    private static final Font HUD_FONT  = new Font("Monospaced", Font.BOLD,  12);
    private static final Font MSG_FONT  = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font WAND_FONT = new Font("Monospaced", Font.PLAIN, 11);

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Decrements the temporary message timer.
     *
     * @param elapsed Milliseconds since last update
     */
    public void update(long elapsed) {
        if (messageTimer > 0) messageTimer = Math.max(0, messageTimer - elapsed);
    }

    // =========================================================================
    // Draw
    // =========================================================================

    /**
     * Draws all HUD elements onto the screen.
     *
     * @param g            Graphics context (screen space)
     * @param player       Player to read stats from
     * @param screenWidth  Viewport width in pixels
     * @param screenHeight Viewport height in pixels
     */
    public void draw(Graphics2D g, Player player, int screenWidth, int screenHeight) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawHealthBar(g, player);
        drawManaBar(g, player);
        drawWandSlots(g, player, screenWidth);
        drawGold(g, player, screenWidth);

        if (messageTimer > 0) drawMessage(g, screenWidth, screenHeight);
    }

    // =========================================================================
    // Bar helpers
    // =========================================================================

    /** Draws the health bar in the top-left area. */
    private void drawHealthBar(Graphics2D g, Player player) {
        int x = MARGIN;
        int y = TOP_Y;

        drawBar(g, x, y, BAR_W, BAR_H,
                player.getHealth(), player.getMaxHealth(),
                new Color(120, 20, 20), new Color(220, 40, 40), "HP");
    }

    /** Draws the mana bar next to the health bar. */
    private void drawManaBar(Graphics2D g, Player player) {
        int x = MARGIN + BAR_W + 10;
        int y = TOP_Y;

        drawBar(g, x, y, BAR_W, BAR_H,
                player.getMana(), player.getMaxMana(),
                new Color(20, 50, 160), new Color(60, 120, 240), "MP");
    }

    /**
     * Draws a labelled status bar.
     *
     * @param g      Graphics context
     * @param x      Left edge
     * @param y      Top edge
     * @param w      Bar width
     * @param h      Bar height
     * @param val    Current value
     * @param max    Maximum value
     * @param dark   Dark background colour
     * @param bright Filled portion colour
     * @param label  Short label (e.g. "HP")
     */
    private void drawBar(Graphics2D g, int x, int y, int w, int h,
                          int val, int max, Color dark, Color bright, String label) {
        float frac = max > 0 ? (float) val / max : 0f;

        // Background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x - 1, y - 1, w + 2, h + 2, 4, 4);

        // Dark track
        g.setColor(dark);
        g.fillRoundRect(x, y, w, h, 3, 3);

        // Filled portion
        g.setColor(bright);
        int fill = Math.max(0, (int)(w * frac));
        if (fill > 0) g.fillRoundRect(x, y, fill, h, 3, 3);

        // Label + values
        g.setFont(HUD_FONT);
        g.setColor(Color.WHITE);
        String txt = label + " " + val + "/" + max;
        g.drawString(txt, x + 4, y + h - 2);
    }

    // =========================================================================
    // Wand inventory
    // =========================================================================

    /** Draws the wand inventory slots in the bottom-left corner. */
    private void drawWandSlots(Graphics2D g, Player player, int screenWidth) {
        List<Wand> wands = player.getWands();
        if (wands.isEmpty()) return;

        int slotW  = 90;
        int slotH  = 20;
        int baseX  = MARGIN;
        int baseY  = TOP_Y + BAR_H + 10;

        g.setFont(WAND_FONT);

        for (int i = 0; i < wands.size(); i++) {
            Wand w = wands.get(i);
            boolean active = (i == player.getActiveWandIndex());

            int x = baseX;
            int y = baseY + i * (slotH + 4);

            // Slot background
            g.setColor(active ? new Color(80, 40, 120, 200) : new Color(30, 30, 50, 160));
            g.fillRoundRect(x, y, slotW, slotH, 4, 4);

            // Border
            g.setColor(active ? new Color(180, 100, 255) : new Color(80, 80, 120));
            g.drawRoundRect(x, y, slotW, slotH, 4, 4);

            // Wand name
            g.setColor(active ? Color.WHITE : new Color(180, 180, 200));
            g.drawString((i + 1) + " " + w.getDisplayName(), x + 6, y + slotH - 5);

            // Cooldown bar
            if (active && !w.isReady()) {
                float frac = w.getCooldownFraction();
                g.setColor(new Color(0, 180, 255, 160));
                g.fillRect(x, y + slotH - 3, (int)(slotW * (1f - frac)), 3);
            }
        }
    }

    // =========================================================================
    // Gold display
    // =========================================================================

    /** Draws the gold counter in the top-right corner. */
    private void drawGold(Graphics2D g, Player player, int screenWidth) {
        g.setFont(HUD_FONT);
        String txt = "GOLD: " + player.getGold();
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(txt);

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(screenWidth - tw - MARGIN - 6, TOP_Y - 2, tw + 10, 18, 4, 4);
        g.setColor(new Color(255, 215, 0));
        g.drawString(txt, screenWidth - tw - MARGIN, TOP_Y + 12);
    }

    // =========================================================================
    // On-screen message
    // =========================================================================

    /**
     * Queues a temporary on-screen message (e.g. "Level Complete!").
     *
     * @param text     Message to display
     * @param durationMs How long to display it in milliseconds
     */
    public void showMessage(String text, long durationMs) {
        message = text;
        messageTimer = durationMs;
    }

    /** Draws the temporary message centred on screen. */
    private void drawMessage(Graphics2D g, int screenWidth, int screenHeight) {
        g.setFont(MSG_FONT);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(message);
        int x = (screenWidth  - tw) / 2;
        int y = screenHeight / 3;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x - 12, y - 18, tw + 24, 28, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(message, x, y);
    }
}
