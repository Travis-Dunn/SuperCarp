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

    private static int state;
    private static final int INACTIVE = 0;
    private static final int DLG_CHAR = 1;
    private static final int DLG_PLAYER = 2;
    private static final int DLG_OPTION = 3;

    private static int lineHeight;
    private static int textBoxMinY, textBoxMaxY;
    private static int textBoxMinX, textBoxMaxX;
    private static int portraitXNpc;
    private static int portraitXPlayer;
    private static int portraitY;
    private static int centerX;
    private static int centerXNpc;
    private static int centerXPlayer;
    private static int headerY, footerY;
    private static int op30Y, op31Y, op32Y;
    private static int op20Y, op21Y;
    private static int op1Y;

    private static int option40Y, option41Y, option42Y, option43Y;
    private static int option30Y, option31Y, option32Y;
    private static int option20Y, option21Y;
    private static int option10Y;
    private static int option0MinX, option0MaxX;
    private static int option0MinY, option0MaxY;
    private static int option1MinX, option1MaxX;
    private static int option1MinY, option1MaxY;
    private static int option2MinX, option2MaxX;
    private static int option2MinY, option2MaxY;
    private static int option3MinX, option3MaxX;
    private static int option3MinY, option3MaxY;
    private static boolean option0Hovered;
    private static boolean option1Hovered;
    private static boolean option2Hovered;
    private static boolean option3Hovered;

    private static int selectedOption;

    private static int clickToContinueMinX;
    private static int clickToContinueMaxX;
    private static int clickToContinueMinY;
    private static int clickToContinueMaxY;

    /* Framebuffer space */
    private static int mouseX, mouseY;

    private static boolean interactiveHovered;

    private static final String CONTINUE_STR = "Click to continue";
    private static final String SELECT_OPTION_STR = "Select an option";
    private static final String MISSING_STR = "MISSING STR";
    private static final String TOO_MANY_LINES_STR = "TOO MANY LINES!";
    private static final String MISSING_CHAR_DISPLAY_NAME_STR =
            "MISSING_NAME";
    private static String bodyText = MISSING_STR;

    private static final int MAX_BODY_LINES = 5;
    private static String[] bodyLines = new String[MAX_BODY_LINES];
    private static int bodyLineCount;

    private static Char speaker;
    private static Bitmap speakerPortrait;
    private static Bitmap playerPortrait;
    private static String charDisplayName;
    private static String playerDisplayName;

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
        centerX = GameFrame.GetCenterX();
        centerXNpc = GameFrame.GetCenterXNpc();
        centerXPlayer = GameFrame.GetCenterXPlayer();
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

        option40Y = GameFrame.GetOption40Y();
        option41Y = GameFrame.GetOption41Y();
        option42Y = GameFrame.GetOption42Y();
        option43Y = GameFrame.GetOption43Y();
        option30Y = GameFrame.GetOption30Y();
        option31Y = GameFrame.GetOption31Y();
        option32Y = GameFrame.GetOption32Y();
        option20Y = GameFrame.GetOption20Y();
        option21Y = GameFrame.GetOption21Y();
        option10Y = GameFrame.GetOption10Y();

        selectedOption = -1;

        state = INACTIVE;

        return init = true;
    }

    public static void Draw() {
        assert(init);

        switch (state) {
            case INACTIVE: return;
            case DLG_CHAR: DrawDlgChar(); break;
            case DLG_PLAYER: DrawDlgPlayer(); break;
            case DLG_OPTION: DrawDlgOption(); break;
        }

        interactiveHovered = false;
        option0Hovered = false;
        option1Hovered = false;
        option2Hovered = false;
        option3Hovered = false;
    }

    /**
     * Changes color of interactive text, such as "Click to continue".
     *
     * @param x mouse x coordinate in emulated framebuffer space
     * @param y mouse y coordinate in emulated framebuffer space
     */
    public static void UpdateMouseHover(int x, int y) {
        assert(init);

        if (state == DLG_CHAR || state == DLG_PLAYER) {
            if (clickToContinueMinX == 0) return;
            if (x >= clickToContinueMinX && x <= clickToContinueMaxX &&
                    y >= clickToContinueMinY && y <= clickToContinueMaxY)
                interactiveHovered = true;
        } else if (state == DLG_OPTION) {
            switch (bodyLineCount) {
                case 0: break;
                case 1: {
                    if (x >= option0MinX && x <= option0MaxX &&
                            y >= option0MinY && y <= option0MaxY)
                        option0Hovered = true;
                } break;
                case 2: {
                    if (x >= option0MinX && x <= option0MaxX &&
                            y >= option0MinY && y <= option0MaxY)
                        option0Hovered = true;
                    if (x >= option1MinX && x <= option1MaxX &&
                            y >= option1MinY && y <= option1MaxY)
                        option1Hovered = true;
                } break;
                case 3: {
                    if (x >= option0MinX && x <= option0MaxX &&
                            y >= option0MinY && y <= option0MaxY)
                        option0Hovered = true;
                    if (x >= option1MinX && x <= option1MaxX &&
                            y >= option1MinY && y <= option1MaxY)
                        option1Hovered = true;
                    if (x >= option2MinX && x <= option2MaxX &&
                            y >= option2MinY && y <= option2MaxY)
                        option2Hovered = true;
                } break;
                case 4: {
                    if (x >= option0MinX && x <= option0MaxX &&
                            y >= option0MinY && y <= option0MaxY)
                        option0Hovered = true;
                    if (x >= option1MinX && x <= option1MaxX &&
                            y >= option1MinY && y <= option1MaxY)
                        option1Hovered = true;
                    if (x >= option2MinX && x <= option2MaxX &&
                            y >= option2MinY && y <= option2MaxY)
                        option2Hovered = true;
                    if (x >= option3MinX && x <= option3MaxX &&
                            y >= option3MinY && y <= option3MaxY)
                        option3Hovered = true;
                }
            }
        }
    }

    public static boolean HandleClick(int x, int y) {
        assert(init);

        /* Only one interactable possible at the moment */
        if (state == DLG_CHAR || state == DLG_PLAYER) {
            if (interactiveHovered) {
                ScriptState s = Player.GetDialogueScriptState();
                if (s.state == ExecutionState.SUSPENDED) {
                    Data.scriptRunner.resumeSuspendedScript(s, Data.playerVars);
                } else if (s.state == ExecutionState.FINISHED) {
                    Player.SetDialogueScriptState(null);
                }
                return true;
            }
        } else if (state == DLG_OPTION) {
            if (option0Hovered) {
                selectedOption = 0;
                ResumeScript();
                return true;
            } else if (option1Hovered) {
                selectedOption = 1;
                ResumeScript();
                return true;
            } else if (option2Hovered) {
                selectedOption = 2;
                ResumeScript();
                return true;
            } else if (option3Hovered) {
                selectedOption = 3;
                ResumeScript();
                return true;
            }
        }
        return false;
    }

    private static void ResumeScript() {
        assert(init);

        ScriptState s = Player.GetDialogueScriptState();
        if (s.state == ExecutionState.SUSPENDED) {
            Data.scriptRunner.resumeSuspendedScript(s, Data.playerVars);
        } else if (s.state == ExecutionState.FINISHED) {
            Player.SetDialogueScriptState(null);
        }
    }

    private static void DrawDlgChar() {
        assert(init);

        if (state != DLG_CHAR) {
            LogSession(LogLevel.WARNING, ErrStrUnexpectedState(DLG_CHAR,
                    state));
            return;
        }

        int argb0 = interactiveHovered ? interactiveHoverARGB : interactiveARGB;

        Renderer.DrawBitmap1(speakerPortrait, portraitXNpc, portraitY);
        TextRenderer.DrawLineCenter(fontAtlas, charDisplayName, centerXNpc,
                headerY, headerARGB);
        long bounds = TextRenderer.DrawLineCenter(fontAtlas, CONTINUE_STR,
                centerXNpc, footerY, argb0);

        clickToContinueMinX = TextRenderer.TLX(bounds);
        clickToContinueMaxX = TextRenderer.BRX(bounds);
        clickToContinueMinY = TextRenderer.TLY(bounds);
        clickToContinueMaxY = TextRenderer.BRY(bounds);

        switch (bodyLineCount) {
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

    private static void DrawDlgPlayer() {
        assert(init);

        if (state != DLG_PLAYER) {
            LogSession(LogLevel.WARNING, ErrStrUnexpectedState(DLG_PLAYER,
                    state));
            return;
        }

        int argb0 = interactiveHovered ? interactiveHoverARGB : interactiveARGB;

        Renderer.DrawBitmap1(playerPortrait, portraitXPlayer, portraitY);
        TextRenderer.DrawLineCenter(fontAtlas, playerDisplayName, centerXPlayer,
                headerY, headerARGB);

        long bounds = TextRenderer.DrawLineCenter(fontAtlas, CONTINUE_STR,
                centerXPlayer, footerY, argb0);

        clickToContinueMinX = TextRenderer.TLX(bounds);
        clickToContinueMaxX = TextRenderer.BRX(bounds);
        clickToContinueMinY = TextRenderer.TLY(bounds);
        clickToContinueMaxY = TextRenderer.BRY(bounds);

        switch (bodyLineCount) {
            case 0: {
                TextRenderer.DrawLineCenter(fontAtlas, MISSING_STR,
                        centerXPlayer, op1Y, bodyARGB);
            }
            break;
            case 1: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0],
                        centerXPlayer, op1Y, bodyARGB);
            }
            break;
            case 2: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0],
                        centerXPlayer, op20Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[1],
                        centerXPlayer, op21Y, bodyARGB);
            }
            break;
            case 3: {
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0],
                        centerXPlayer, op30Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[1],
                        centerXPlayer, op31Y, bodyARGB);
                TextRenderer.DrawLineCenter(fontAtlas, bodyLines[2],
                        centerXPlayer, op32Y, bodyARGB);
            }
            break;
            default: {
                TextRenderer.DrawLineCenter(fontAtlas, TOO_MANY_LINES_STR,
                        centerXPlayer, op1Y, bodyARGB);
            }
        }
    }

    private static void DrawDlgOption() {
        assert(init);

        if (state != DLG_OPTION) {
            LogSession(LogLevel.WARNING, ErrStrUnexpectedState(DLG_OPTION,
                    state));
            return;
        }

        int argb0 = option0Hovered ? interactiveHoverARGB : interactiveARGB;
        int argb1 = option1Hovered ? interactiveHoverARGB : interactiveARGB;
        int argb2 = option2Hovered ? interactiveHoverARGB : interactiveARGB;
        int argb3 = option3Hovered ? interactiveHoverARGB : interactiveARGB;

        TextRenderer.DrawLineCenter(fontAtlas, SELECT_OPTION_STR, centerX,
                headerY, headerARGB);

        switch (bodyLineCount) {
            case 0: {
                TextRenderer.DrawLineCenter(fontAtlas, MISSING_STR,
                        centerXPlayer, op1Y, bodyARGB);
            }
            break;
            case 1: {
                long bounds = TextRenderer.DrawLineCenter(fontAtlas, bodyLines[0],
                        centerX, option10Y, argb0);

                option0MinX = TextRenderer.TLX(bounds);
                option0MaxX = TextRenderer.BRX(bounds);
                option0MinY = TextRenderer.TLY(bounds);
                option0MaxY = TextRenderer.BRY(bounds);
            }
            break;
            case 2: {
                long bounds0 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[0], centerX, option20Y, argb0);
                long bounds1 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[1], centerX, option21Y, argb1);

                option0MinX = TextRenderer.TLX(bounds0);
                option0MaxX = TextRenderer.BRX(bounds0);
                option0MinY = TextRenderer.TLY(bounds0);
                option0MaxY = TextRenderer.BRY(bounds0);

                option1MinX = TextRenderer.TLX(bounds1);
                option1MaxX = TextRenderer.BRX(bounds1);
                option1MinY = TextRenderer.TLY(bounds1);
                option1MaxY = TextRenderer.BRY(bounds1);
            }
            break;
            case 3: {
                long bounds0 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[0], centerX, option30Y, argb0);
                long bounds1 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[1], centerX, option31Y, argb1);
                long bounds2 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[2], centerX, option32Y, argb2);

                option0MinX = TextRenderer.TLX(bounds0);
                option0MaxX = TextRenderer.BRX(bounds0);
                option0MinY = TextRenderer.TLY(bounds0);
                option0MaxY = TextRenderer.BRY(bounds0);

                option1MinX = TextRenderer.TLX(bounds1);
                option1MaxX = TextRenderer.BRX(bounds1);
                option1MinY = TextRenderer.TLY(bounds1);
                option1MaxY = TextRenderer.BRY(bounds1);

                option2MinX = TextRenderer.TLX(bounds2);
                option2MaxX = TextRenderer.BRX(bounds2);
                option2MinY = TextRenderer.TLY(bounds2);
                option2MaxY = TextRenderer.BRY(bounds2);
            } break;
            case 4: {
                long bounds0 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[0], centerX, option40Y, argb0);
                long bounds1 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[1], centerX, option41Y, argb1);
                long bounds2 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[2], centerX, option42Y, argb2);
                long bounds3 = TextRenderer.DrawLineCenter(fontAtlas,
                        bodyLines[3], centerX, option43Y, argb3);

                option0MinX = TextRenderer.TLX(bounds0);
                option0MaxX = TextRenderer.BRX(bounds0);
                option0MinY = TextRenderer.TLY(bounds0);
                option0MaxY = TextRenderer.BRY(bounds0);

                option1MinX = TextRenderer.TLX(bounds1);
                option1MaxX = TextRenderer.BRX(bounds1);
                option1MinY = TextRenderer.TLY(bounds1);
                option1MaxY = TextRenderer.BRY(bounds1);

                option2MinX = TextRenderer.TLX(bounds2);
                option2MaxX = TextRenderer.BRX(bounds2);
                option2MinY = TextRenderer.TLY(bounds2);
                option2MaxY = TextRenderer.BRY(bounds2);

                option3MinX = TextRenderer.TLX(bounds3);
                option3MaxX = TextRenderer.BRX(bounds3);
                option3MinY = TextRenderer.TLY(bounds3);
                option3MaxY = TextRenderer.BRY(bounds3);
            }
            break;
            default: {
                TextRenderer.DrawLineCenter(fontAtlas, TOO_MANY_LINES_STR,
                        centerXPlayer, op1Y, bodyARGB);
            }
        }
    }

    public static void Deactivate() {
        assert(init);

        speaker = null;
        speakerPortrait = null;
        Player.ClearDialogueTarget();
        Player.ClearDialogueScriptState();
        state = INACTIVE;
        ClearBodyLines();
        bodyText = MISSING_STR;
        selectedOption = -1;
    }

    public static boolean IsActive() {
        assert(init);

        return state == DLG_CHAR || state == DLG_PLAYER ||
                state == DLG_OPTION; }

    public static void SetStateDlgChar(Char c, String s) {
        assert(init);

        state = DLG_CHAR;

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

        charDisplayName = c.displayName;
        if (charDisplayName == null) {
            LogSession(LogLevel.WARNING, ERR_STR_CHAR_DISPLAY_NAME_NULL);
            charDisplayName = MISSING_CHAR_DISPLAY_NAME_STR;
        }

        bodyText = s != null ? s : MISSING_STR;

        ParseBodyLines();
    }

    public static void SetStateDlgPlayer(String s) {
        assert(init);

        state = DLG_PLAYER;

        playerPortrait = Player.GetPortrait();

        playerDisplayName = Player.GetDisplayName();

        bodyText = s != null ? s : MISSING_STR;
        ParseBodyLines();
    }

    public static void SetStateDlgOption(String s) {
        assert(init);

        state = DLG_OPTION;

        bodyText = s != null ? s : MISSING_STR;
        ParseBodyLines();
    }

    private static void ParseBodyLines() {
        assert(init);

        int i, start = 0;
        int len = bodyText.length();

        bodyLineCount = 0;

        for (i = 0; i < len; ++i) {
            if (bodyText.charAt(i) == '|') {
                if (bodyLineCount < MAX_BODY_LINES) {
                    bodyLines[bodyLineCount++] = bodyText.substring(start, i);
                }
                start = i + 1;
            }
        }
        if (bodyLineCount < MAX_BODY_LINES) {
            bodyLines[bodyLineCount++] = bodyText.substring(start, len);
        }
    }

    private static void ClearBodyLines() {
        assert(init);

        int i;

        for (i = 0; i < bodyLineCount; ++i) {
            bodyLines[i] = "";
        }

        bodyLineCount = 0;
    }

    public static void ClearSelectedOption() {
        assert(init);

        selectedOption = -1;
    }
    public static int GetSelectedOption() {
        assert(init);

        return selectedOption;
    }

    public static final String CLASS = DialogueRenderer.class.getSimpleName();
    private static final String ERR_STR_SPEAKER_NULL = CLASS + " tried to set" +
            " speaker to null.\n";
    private static final String ERR_STR_SPEAKER_PORTRAIT_NULL = CLASS +
            " tried to set speaker portrait to null.\n";
    private static final String ERR_STR_INIT_FONT_ATLAS_NULL = CLASS +
            " failed to initialize because the font atlas was null.\n";
    private static final String ERR_STR_CHAR_DISPLAY_NAME_NULL = CLASS +
            " tried to set char display name to null";
    private static String ErrStrUnexpectedState(int expected, int actual) {
        return String.format("%s tried to draw dialogue for state [%d], but " +
                "the state was [%d].\n", CLASS, expected, actual);
    }
}
