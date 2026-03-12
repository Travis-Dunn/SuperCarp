package production.dialogue;

import production.ui.GameFrame;

public final class DialogueRenderer {
    private static boolean init;

    private static int portraitXNpc;
    private static int portraitXPlayer;
    private static int portraitY;

    public static boolean Init() {
        assert(!init);

        portraitXNpc = GameFrame.GetPortraitXNpc();
        portraitXPlayer = GameFrame.GetPortraitXPlayer();
        portraitY = GameFrame.GetPortraitY();

        return init = true;
    }

}
