package production.ui;

import production.dialogue.DialogueRenderer;

public final class InterfaceController {
    public static void CloseInterfaces() {
        DialogueRenderer.Deactivate();
    }
}
