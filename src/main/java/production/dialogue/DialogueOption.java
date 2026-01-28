package production.dialogue;

import production.script.Script;

import java.util.concurrent.locks.Condition;

/**
 * A single response option the player can choose.
 */
public final class DialogueOption {
    public final String text;         // what the player sees
    public final DialogueNode next;   // where this choice leads (null = end)
    public final Script script;     // side effect (null = none)
    public final Condition conditions[];

    public DialogueOption(String text, DialogueNode next, Script script,
                          Condition[] conditions) {
        this.text = text;
        this.next = next;
        this.script = script;
        this.conditions = conditions;
    }
}