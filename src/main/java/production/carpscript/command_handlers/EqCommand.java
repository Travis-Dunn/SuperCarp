package production.carpscript.command_handlers;

import production.carpscript.CommandHandler;
import production.carpscript.ScriptState;

import java.util.Map;

public class EqCommand implements CommandHandler {
    @Override
    public Object execute(ScriptState state, Object[] args, Map<String, Object> playerVars) {
        return args[0].equals(args[1]) ? 1 : 0;
    }
}
