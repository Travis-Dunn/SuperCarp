package production.dialogue;

/**
 * A single node in a dialogue tree.
 * Represents one "screen" of dialogue.
 */
public final class DialogueNode {
    public final String speaker;      // "Hans", "Player", null for narration
    public final String text;
    public final DialogueOption[] options;  // null = click to continue/end
    private DialogueNode next;   // used when options is null

    public DialogueNode(String speaker, String text, DialogueOption[] options,
                        DialogueNode next) {
        this.speaker = speaker;
        this.text = text;
        this.options = options;
        this.next = next;
    }

    public boolean hasOptions() {
        return options != null && options.length > 0;
    }

    public boolean isTerminal() {
        return options == null && next == null;
    }

    public void setNext(DialogueNode next) { this.next = next; }

    DialogueNode getNext() { return next; }
}