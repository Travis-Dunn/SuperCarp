package production.dialogue;

import production.Data;
import production.Player;
import production.carpscript.ExecutionState;
import production.carpscript.ScriptState;
import production.character.Char;
import production.ui.*;
import whitetail.utility.logging.LogLevel;

import static production.character.CharRegistry.MISSING_CHAR;
import static production.ui.BitmapRegistry.MISSING_PORTRAIT;
import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class DialogueRenderer {
    private static boolean init;

    private static boolean active;

    private static int state;
    private static final int INACTIVE = 0;
    private static final int DLG_CHAR = 1;

    private static boolean clickToContinueFunction;

    private static int lineHeight;
    private static int textBoxMinY, textBoxMaxY;
    private static int textBoxMinX, textBoxMaxX;
    private static int portraitXNpc;
    private static int portraitXPlayer;
    private static int portraitY;
    private static int centerXNpc;
    private static int headerY, footerY;
    private static int op30Y, op31Y, op32Y;
    private static int op20Y, op21Y;
    private static int op1Y;

    private static int clickToContinueMinX = 124;
    private static int clickToContinueMaxX = 224;
    private static int clickToContinueMinY = 203;
    private static int clickToContinueMaxY = 209;

    /* Framebuffer space */
    private static int mouseX, mouseY;

    private static boolean interactiveHovered;

    private static final String CONTINUE_STR = "Click to continue";
    private static final String MISSING_STR = "MISSING STR";
    private static final String TOO_MANY_LINES_STR = "TOO MANY LINES!";
    private static String bodyText = MISSING_STR;
    private static String bodyLines[];

    private static Char speaker;
    private static Bitmap speakerPortrait;

    private static FontAtlas fontAtlas;

    private static int bodyARGB;
    private static int headerARGB;
    private static int interactiveARGB;
    private static int interactiveHoverARGB;

    public static boolean Init(FontAtlas fontAtlas, int bodyARGB,
            int headerARGB, int interactiveARGB, int interactiveHoverARGB) {
        assert(!init);

        if (fontAtlas == null) {
            LogFatalAndExit(ERR_STR_INIT_FONT_ATLAS_NULL);
            return init = false;
        }

        portraitXNpc = GameFrame.GetPortraitXNpc();
        portraitXPlayer = GameFrame.GetPortraitXPlayer();
        portraitY = GameFrame.GetPortraitY();
        centerXNpc = GameFrame.GetCenterXNpc();
        headerY = GameFrame.GetHeaderY();
        footerY = GameFrame.GetFooterY();
        speaker = MISSING_CHAR;
        speakerPortrait = MISSING_PORTRAIT;
        DialogueRenderer.fontAtlas = fontAtlas;
        DialogueRenderer.bodyARGB = bodyARGB;
        DialogueRenderer.headerARGB = headerARGB;
        DialogueRenderer.interactiveARGB = interactiveARGB;
        DialogueRenderer.interactiveHoverARGB = interactiveHoverARGB;
        textBoxMinY = GameFrame.GetTextBoxMinY();
        textBoxMaxY = GameFrame.GetTextBoxMaxY();
        textBoxMinX = GameFrame.GetTextBoxMinX();
        textBoxMaxX = GameFrame.GetTextBoxMaxX();
        lineHeight = DialogueRenderer.fontAtlas.lineHeight;
        op30Y = GameFrame.GetOp30Y();
        op31Y = GameFrame.GetOp31Y();
        op32Y = GameFrame.GetOp32Y();
        op20Y = GameFrame.GetOp20Y();
        op21Y = GameFrame.GetOp21Y();
        op1Y = GameFrame.GetOp1Y();

        state = INACTIVE;

        return init = true;
    }

    public static void Draw() {
        assert(init);

        switch (state) {
            case INACTIVE: return;
            case DLG_CHAR: DrawDlgChar();
        }
    }

    /**
     * Changes color of interactive text, such as "Click to continue".
     *
     * @param x mouse x coordinate in emulated framebuffer space
     * @param y mouse y coordinate in emulated framebuffer space
     */
    public static void UpdateMousePos(int x, int y) {
        assert(init);

        if (x >= clickToContinueMinX && x <= clickToContinueMaxX &&
                y >= clickToContinueMinY && y <= clickToContinueMaxY) {
            interactiveHovered = true;
        } else interactiveHovered = false;
    }

    public static void Clear() {
        assert(init);

        active = false;
        state = INACTIVE;
        bodyText = MISSING_STR;
        speaker = MISSING_CHAR;
        speakerPortrait = MISSING_PORTRAIT;
        bodyLines = new String[0];
    }

    public static boolean HandleClick(int x, int y) {
        assert(init);

        /* Only one interactable possible at the moment */
        if (state == DLG_CHAR) {
            if (x >= clickToContinueMinX && x <= clickToContinueMaxX &&
                y >= clickToContinueMinY && y <= clickToContinueMaxY) {
                ScriptState s = Player.GetDialogueScriptState();
                if (s.state == ExecutionState.SUSPENDED) {
                    Data.scriptRunner.resumeSuspendedScript(s, Data.playerVars);
                } else if (s.state == ExecutionState.FINISHED) {
                    Player.SetDialogueScriptState(null);
                }
                return true;
            }
        }
        return false;
    }

    private static void DrawDlgChar() {
        int argb0 = interactiveHovered ? interactiveHoverARGB : interactiveARGB;

        Renderer.DrawBitmap1(speakerPortrait, portraitXNpc, portraitY);
        TextRenderer.DrawLineCenter(fontAtlas, speaker.displayName, centerXNpc,
                headerY, headerARGB);
        int w = TextRenderer.DrawLineCenter(fontAtlas, CONTINUE_STR, centerXNpc,
                footerY, argb0);

        clickToContinueMinX = centerXNpc - (w / 2);
        clickToContinueMaxX = clickToContinueMinX + w;

        switch (bodyLines.length) {
            case 0: {
                TextRenderer.DrawLineCenter(fontAtlas, MISSING_STR,
                        centerXNpc, op1Y, bodyARGB);
            }
            break;
            case 1: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0], centerXNpc,
                        op1Y, bodyARGB);
            }
            break;
            case 2: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0], centerXNpc,
                        op20Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[1], centerXNpc,
                        op21Y, bodyARGB);
            }
            break;
            case 3: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0], centerXNpc,
                        op30Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[1], centerXNpc,
                        op31Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[2], centerXNpc,
                        op32Y, bodyARGB);
            }
            break;
            default: {
                TextRenderer.DrawLineCenter(fontAtlas, TOO_MANY_LINES_STR,
                        centerXNpc, op1Y, bodyARGB);
            }
        }
    }

    public static void SetSpeaker(Char c) {
        assert(init);

        speaker = c;
        if (speaker == null) {
            LogSession(LogLevel.WARNING, ERR_STR_SPEAKER_NULL);
            speaker = MISSING_CHAR;
        }

        speakerPortrait = c.getPortrait();
        if (speakerPortrait == null) {
            LogSession(LogLevel.WARNING, ERR_STR_SPEAKER_PORTRAIT_NULL);
            speakerPortrait = MISSING_PORTRAIT;
        }
    }

    public static void Activate() { assert(init); active = true; }
    public static void Deactivate() {
        assert(init);

        speaker = null;
        speakerPortrait = null;
        Player.ClearDialogueTarget();
        Player.ClearDialogueScriptState();
        state = INACTIVE;

        active = false;
    }

    public static void SetBodyText(String s) {
        bodyText = s != null ? s : MISSING_STR;

        bodyLines = bodyText.split("\\|");
    }

    public static boolean IsActive() { assert(init); return active; }

    public static void SetStateDlgChar() { assert(init); state = DLG_CHAR; }

    public static final String CLASS = DialogueRenderer.class.getSimpleName();
    private static final String ERR_STR_SPEAKER_NULL = CLASS + " tried to set" +
            " speaker to null.\n";
    private static final String ERR_STR_SPEAKER_PORTRAIT_NULL = CLASS +
            " tried to set speaker portrait to null.\n";
    private static final String ERR_STR_INIT_FONT_ATLAS_NULL = CLASS +
            " failed to initialize because the font atlas was null.\n";
}
