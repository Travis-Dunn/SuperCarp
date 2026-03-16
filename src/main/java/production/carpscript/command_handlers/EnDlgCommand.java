package production.carpscript.command_handlers;

import production.carpscript.CommandHandler;
import production.carpscript.ScriptState;
import production.dialogue.DialogueRenderer;

import java.util.Map;

public class EnDlgCommand implements CommandHandler {
    @Override
    public Object execute(ScriptState state, Object[] args, Map<String, Object> playerVars) {
        DialogueRenderer.Deactivate();

        return null;
    }
}
