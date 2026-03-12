package production.carpscript;

/**
 * A single token produced by the lexer.
 *
 * A token is the smallest meaningful unit of source code. It pairs a type
 * (what kind of thing is this?) with a value (what exactly was written?)
 * and a line number (where in the source file was it?).
 *
 * The line number is purely for error messages. When the parser encounters
 * something unexpected, it can tell you "error on line 14" instead of just
 * "error somewhere." This is a small investment that saves enormous
 * debugging time later.
 */
public class Token {

    public final TokenType type;

    /**
     * The token's value. What this contains depends on the type:
     *   INTEGER    → the digits as a string, e.g. "42"
     *   STRING     → the string content without quotes, e.g. "Hello"
     *   IDENTIFIER → the name, e.g. "mes" or "Guard"
     *   LOCAL_VAR  → the variable name without $, e.g. "coins"
     *   PLAYER_VAR → the variable name without %, e.g. "quest_progress"
     *   Keywords   → the keyword text, e.g. "if"
     *   Symbols    → the symbol text, e.g. "(" or "{"
     *   EOF        → empty string
     */
    public final String value;

    /** The line number in the source file (1-based). */
    public final int line;

    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    @Override
    public String toString() {
        switch (type) {
            case STRING:
                return type + "(\"" + value + "\") line " + line;
            case INTEGER:
            case IDENTIFIER:
            case LOCAL_VAR:
            case PLAYER_VAR:
                return type + "(" + value + ") line " + line;
            default:
                return type + " line " + line;
        }
    }
}