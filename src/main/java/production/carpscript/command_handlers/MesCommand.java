package production.carpscript.command_handlers;

import production.carpscript.CommandHandler;
import production.carpscript.ScriptState;
import production.ui.ChatBox;

import java.util.Map;

public class MesCommand implements CommandHandler {
    @Override
    public Object execute(ScriptState state, Object[] args, Map<String, Object> playerVars) {
        if (args != null && args.length > 0) {
            if (args[0] instanceof String) {
                ChatBox.AddMsg((String) args[0]);
            }
        }
        return null;
    }
}
