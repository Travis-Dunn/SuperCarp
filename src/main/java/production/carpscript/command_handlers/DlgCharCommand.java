package production.carpscript.command_handlers;

import production.carpscript.CommandHandler;
import production.carpscript.ExecutionState;
import production.carpscript.ScriptState;
import production.character.Char;
import production.character.CharRegistry;
import production.dialogue.DialogueRenderer;

import java.util.Map;

public class DlgCharCommand implements CommandHandler {
    @Override
    public Object execute(ScriptState state, Object[] args, Map<String, Object> playerVars) {
        /* call a dialogue renderer method to set state, using the args
        AND
        state.triggerSubject
        which, in this case, is the name of the Char that's been clicked on.
        We can use that to look the Char up in case we need to check it's state.
         */
        Char c = CharRegistry.get(state.triggerSubject);
        DialogueRenderer.SetStateDlgChar(c, (String)args[0]);

        state.state = ExecutionState.SUSPENDED;

        return null;
    }
}
