package production.dialogue.warehouse;

import production.dialogue.DialogueNode;
import production.dialogue.DialogueOption;

public class BilboDialogue {
    private static final DialogueNode GOODBYE = new DialogueNode(
            "Bilbo",
            "Take care, adventurer.",
            null,
            null
    );

    /* We need a way for a node to lead to the running of a script, but we don't
    have scripts yet */
    private static final DialogueNode TRADE = new DialogueNode(
            "Bilbo",
            "Certainly!",
            null,
            null
    );

    public static final DialogueNode GREETING = new DialogueNode(
            "Bilbo",
            "Hello there. What can I do for you?",
            new DialogueOption[] {
                    new DialogueOption("Who are you?", WHO_ARE_YOU),
                    new DialogueOption("I'd like to trade.", TRADE),
                    new DialogueOption("Nothing, bye.", GOODBYE)
            },
            null
    );

    private static final DialogueNode WHO_ARE_YOU = new DialogueNode(
            "Bilbo",
            "I'm Bilbo, and this is Hobbiton.",
            GREETING  // loops back
    );

    private BilboDialogue() {}
}
