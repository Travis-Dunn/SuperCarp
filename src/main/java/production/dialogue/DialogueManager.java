package production.dialogue;

import production.Data;
import production.Player;
import production.character.Char;
import production.sprite.SpritePalette;
import production.ui.FontAtlas;
import production.ui.TextRenderer;
import production.ui.UIRenderer;

/**
 * Manages dialogue state and rendering.
 * Immediate-mode rendering — call draw() each frame when active.
 */
public final class DialogueManager {

    /* --- state --- */
    private static boolean init;
    private static DialogueNode currentNode;
    private static Char currentChar;
    private static Char pendingTalkTarget;

    /* --- layout constants (adjust to taste) --- */
    private static final int BOX_MARGIN = 8;
    private static final int BOX_HEIGHT = 60;
    private static final int TEXT_PADDING = 6;
    private static final int OPTION_HEIGHT = 12;

    /* --- palette indices (adjust to your palette) --- */
    private static final int BG_COLOR = 0;       // background fill
    private static final int BORDER_COLOR = 11;  // border
    private static final int TEXT_COLOR = 0xFFFFFFFF;  // white, raw ARGB
    private static final int OPTION_HOVER_COLOR = 5;   // highlight for hovered option

    /* --- rendering state --- */
    private static FontAtlas font;
    private static SpritePalette palette;

    /* --- hit testing (computed during draw, used for clicks) --- */
    private static final int MAX_OPTIONS = 8;
    private static final int[] optionY = new int[MAX_OPTIONS];
    private static final int[] optionH = new int[MAX_OPTIONS];
    private static int optionCount;
    private static int boxX, boxY, boxW, boxH;

    private DialogueManager() {}

    public static void Init(FontAtlas fontAtlas, SpritePalette pal) {
        font = fontAtlas;
        palette = pal;
        currentNode = null;
        currentChar = null;
        pendingTalkTarget = null;
        init = true;
    }

    /* --- state queries --- */

    public static boolean isActive() {
        return currentNode != null;
    }

    public static boolean hasPendingTarget() {
        return pendingTalkTarget != null;
    }

    public static Char getPendingTarget() {
        return pendingTalkTarget;
    }

    /* --- control --- */

    /**
     * Set a pending talk target. Called when player clicks a Char
     * but isn't adjacent yet.
     */
    public static void setPendingTarget(Char c) {
        pendingTalkTarget = c;
    }

    public static void clearPendingTarget() {
        pendingTalkTarget = null;
    }

    /**
     * Start dialogue with a Char. Called when player is adjacent.
     */
    public static void start(Char c, DialogueNode rootNode) {
        if (rootNode == null) return;

        currentChar = c;
        currentNode = rootNode;
        pendingTalkTarget = null;

        /* stop player movement when dialogue starts */
        Player.clearPath();
    }

    /**
     * Advance to the next node (for click-to-continue nodes).
     */
    public static void advance() {
        if (currentNode == null) return;

        if (currentNode.isTerminal()) {
            end();
        } else if (!currentNode.hasOptions()) {
            currentNode = currentNode.getNext();
            if (currentNode == null) {
                end();
            }
        }
    }

    /**
     * Select an option by index.
     */
    public static void selectOption(int index) {
        if (currentNode == null || !currentNode.hasOptions()) return;
        if (index < 0 || index >= currentNode.options.length) return;

        DialogueOption option = currentNode.options[index];

        /* run script if present */
        if (option.script != null) {
            option.script.run();
        }

        /* advance to next node */
        if (option.next != null) {
            currentNode = option.next;
        } else {
            end();
        }
    }

    /**
     * End the current dialogue.
     */
    public static void end() {
        currentNode = null;
        currentChar = null;
    }

    /* --- rendering --- */

    /**
     * Draw the dialogue panel. Call each frame when isActive().
     *
     * @param mouseX mouse X in framebuffer coords (-1 if not hovering)
     * @param mouseY mouse Y in framebuffer coords
     */
    public static void draw(byte[] fb, int fbW, int fbH, int mouseX, int mouseY) {
        if (!init || currentNode == null) return;

        /* compute box dimensions */
        boxX = BOX_MARGIN;
        boxW = fbW - (BOX_MARGIN * 2);
        boxH = BOX_HEIGHT;
        boxY = fbH - BOX_MARGIN - boxH;

        /* expand box if we have options */
        if (currentNode.hasOptions()) {
            int extraHeight = currentNode.options.length * OPTION_HEIGHT;
            boxH += extraHeight;
            boxY -= extraHeight;
        }

        /* draw background */
        UIRenderer.DrawRectWithBorder(fb, fbW, fbH,
                boxX, boxY, boxW, boxH,
                BG_COLOR, BORDER_COLOR, palette);

        int textX = boxX + TEXT_PADDING;
        int textY = boxY + TEXT_PADDING;

        /* draw speaker name */
        if (currentNode.speaker != null) {
            TextRenderer.draw(fb, fbW, fbH, font,
                    currentNode.speaker + ":",
                    textX, textY, TEXT_COLOR);
            textY += font.lineHeight;
        }

        /* draw main text */
        TextRenderer.draw(fb, fbW, fbH, font,
                currentNode.text,
                textX, textY, TEXT_COLOR,
                boxW - (TEXT_PADDING * 2), TextRenderer.ALIGN_LEFT);

        /* draw options or continue prompt */
        if (currentNode.hasOptions()) {
            drawOptions(fb, fbW, fbH, mouseX, mouseY);
        } else if (!currentNode.isTerminal()) {
            /* click to continue indicator */
            int promptY = boxY + boxH - TEXT_PADDING - font.lineHeight;
            TextRenderer.draw(fb, fbW, fbH, font,
                    "[Click to continue]",
                    textX, promptY, TEXT_COLOR);
        }
    }

    private static void drawOptions(byte[] fb, int fbW, int fbH,
                                    int mouseX, int mouseY) {
        DialogueOption[] options = currentNode.options;
        optionCount = Math.min(options.length, MAX_OPTIONS);

        int textX = boxX + TEXT_PADDING;
        int startY = boxY + boxH - TEXT_PADDING - (optionCount * OPTION_HEIGHT);

        for (int i = 0; i < optionCount; i++) {
            int y = startY + (i * OPTION_HEIGHT);
            optionY[i] = y;
            optionH[i] = OPTION_HEIGHT;

            /* check hover */
            boolean hovered = mouseX >= boxX && mouseX < boxX + boxW &&
                    mouseY >= y && mouseY < y + OPTION_HEIGHT;

            /* draw highlight if hovered */
            if (hovered) {
                UIRenderer.DrawRect(fb, fbW, fbH,
                        boxX + 1, y, boxW - 2, OPTION_HEIGHT,
                        OPTION_HOVER_COLOR, palette);
            }

            /* draw option text */
            String text = (i + 1) + ". " + options[i].text;
            TextRenderer.draw(fb, fbW, fbH, font, text, textX, y, TEXT_COLOR);
        }
    }

    /* --- input handling --- */

    /**
     * Handle a mouse click in framebuffer coordinates.
     * Returns true if the click was consumed by dialogue.
     */
    public static boolean handleClick(int fbX, int fbY) {
        if (!isActive()) return false;

        /* check if click is in dialogue box */
        if (!UIRenderer.pointInRect(fbX, fbY, boxX, boxY, boxW, boxH)) {
            return false;  /* click outside box - could end dialogue or ignore */
        }

        if (currentNode.hasOptions()) {
            /* check each option */
            for (int i = 0; i < optionCount; i++) {
                if (fbY >= optionY[i] && fbY < optionY[i] + optionH[i]) {
                    selectOption(i);
                    return true;
                }
            }
        } else {
            /* click to continue */
            advance();
        }

        return true;
    }

    public static final String CLASS = DialogueManager.class.getSimpleName();
}