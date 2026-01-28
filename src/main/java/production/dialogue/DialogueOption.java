package production.dialogue;

/**
 * A single response option the player can choose.
 */
public final class DialogueOption {
    public final String text;         // what the player sees
    public final DialogueNode next;   // where this choice leads (null = end)
    public final Runnable action;     // side effect (null = none)

    public DialogueOption(String text, DialogueNode next) {
        this.text = text;
        this.next = next;
        this.action = null;
    }

    public DialogueOption(String text, DialogueNode next, Runnable action) {
        this.text = text;
        this.next = next;
        this.action = action;
    }

    /**
     * Terminal option with side effect (e.g., "Yes, let's trade" -> opens shop)
     */
    public DialogueOption(String text, Runnable action) {
        this.text = text;
        this.next = null;
        this.action = action;
    }
}