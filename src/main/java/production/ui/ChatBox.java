package production.ui;

import production.Data;
import production.DisplayConfig;
import production.sprite.SpriteRenderer;
import production.sprite.SpriteSys;

/**
 * Simple chat box that displays the last N messages.
 * Newest message at the bottom.
 */
public final class ChatBox {
    public static final int LINE_COUNT = 5;

    private static final String[] lines = new String[LINE_COUNT];
    private static int count = 0;  // total messages received (for indexing)

    private static FontAtlas font;
    private static int color = 0xFFFFFFFF;  // default white

    /* hard-coded position - bottom-left of screen */
    private static final int X = 4;
    private static final int Y_BOTTOM = DisplayConfig.GetEmulatedH() - 4;  // 4px from bottom

    private ChatBox() {}

    /**
     * Initialize with a font. Call once during game init.
     */
    public static void Init(FontAtlas fontAtlas) {
        font = fontAtlas;
        for (int i = 0; i < LINE_COUNT; i++) {
            lines[i] = null;
        }
        count = 0;
    }

    /**
     * Set the text color (ARGB packed int).
     */
    public static void setColor(int argb) {
        color = argb;
    }

    /**
     * Add a message to the chat box.
     * Older messages scroll up; oldest discarded when full.
     */
    public static void AddMsg(String message) {
        if (font == null) {
            System.out.println("[ChatBox] " + message);
            return;
        }

        /* shift everything up */
        for (int i = 0; i < LINE_COUNT - 1; i++) {
            lines[i] = lines[i + 1];
        }
        lines[LINE_COUNT - 1] = message;
        count++;
    }

    /**
     * Draw the chat box. Call during render, after sprites.
     */
    public static void Draw() {
        if (font == null) return;

        byte[] fb = SpriteSys.GetFramebuffer();
        int fbW = DisplayConfig.GetEmulatedW();
        int fbH = DisplayConfig.GetEmulatedH();

        /* calculate Y position for topmost line */
        int lineHeight = font.lineHeight;
        int totalHeight = LINE_COUNT * lineHeight;
        int startY = Y_BOTTOM - totalHeight;

        for (int i = 0; i < LINE_COUNT; i++) {
            if (lines[i] != null) {
                int y = startY + (i * lineHeight);
                TextRenderer.draw(fb, fbW, fbH, font, lines[i], X, y, color);
            }
        }
    }

    /**
     * Clear all messages.
     */
    public static void clear() {
        for (int i = 0; i < LINE_COUNT; i++) {
            lines[i] = null;
        }
        count = 0;
    }

    public static final String CLASS = ChatBox.class.getSimpleName();
}