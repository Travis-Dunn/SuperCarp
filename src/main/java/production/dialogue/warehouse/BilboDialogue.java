package production.dialogue.warehouse;

import production.dialogue.DialogueNode;
import production.dialogue.DialogueOption;

public class BilboDialogue {
    public static final DialogueNode GOODBYE;
    public static final DialogueNode TRADE;
    public static final DialogueNode WHO_ARE_YOU;
    public static final DialogueNode GREETING;

    static {
        GOODBYE = new DialogueNode(
                "Bilbo",
                "Take care, adventurer.",
                null,
                null
        );
        TRADE = new DialogueNode(
                "Bilbo",
                "Certainly!",
                null,
                null
        );
        WHO_ARE_YOU = new DialogueNode(
                "Bilbo",
                "I'm Bilbo, and this is Hobbiton.",
                null,
                null
        );
        GREETING = new DialogueNode(
                "Bilbo",
                "Hello there. What can I do for you?",
                new DialogueOption[] {
                        new DialogueOption("Who are you?", WHO_ARE_YOU,
                                null, null),
                        new DialogueOption("I'd like to trade.", TRADE,
                                null, null),
                        new DialogueOption("Nothing, bye.", GOODBYE,
                                null, null)
                },
                null
        );
        WHO_ARE_YOU.setNext(GREETING);
    }

    private BilboDialogue() {}
}
