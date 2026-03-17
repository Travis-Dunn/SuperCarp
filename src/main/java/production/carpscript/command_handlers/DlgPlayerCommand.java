package production.carpscript.command_handlers;

import production.carpscript.CommandHandler;
import production.carpscript.ExecutionState;
import production.carpscript.ScriptState;
import production.dialogue.DialogueRenderer;

import java.util.Map;

public class DlgPlayerCommand implements CommandHandler {
    @Override
    public Object execute(ScriptState state, Object[] args, Map<String, Object> playerVars) {
        DialogueRenderer.SetStateDlgPlayer((String)args[0]);

        state.state = ExecutionState.SUSPENDED;
        return null;
    }
}
