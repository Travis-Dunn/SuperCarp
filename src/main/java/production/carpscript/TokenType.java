package production.carpscript;

/**
 * Every kind of token the lexer can produce.
 *
 * A token is a labeled chunk of source text. The lexer reads characters
 * and groups them into tokens, like splitting a sentence into labeled words.
 *
 * Source text:    [opnpc,Guard]
 * Tokens:         LBRACKET  IDENTIFIER("opnpc")  COMMA  IDENTIFIER("Guard")  RBRACKET
 *
 * Source text:    mes("Hello world")
 * Tokens:         IDENTIFIER("mes")  LPAREN  STRING("Hello world")  RPAREN
 *
 * Source text:    if (%quest = 0) {
 * Tokens:         IF  VARP("quest")  EQUALS  INTEGER(0)  LBRACE
 *
 * Notice that some tokens carry a value (the string content, the number,
 * the identifier name) while others are just structural markers (parentheses,
 * braces, commas). The Token class pairs a TokenType with its value.
 */
public enum TokenType {

    // ── Literals ──────────────────────────────────────────────────────

    /** An integer literal like 42 or 995. Value: the number as a String. */
    INTEGER,

    /** A quoted string like "Hello world". Value: the content WITHOUT quotes. */
    STRING,

    // ── Identifiers and Keywords ──────────────────────────────────────

    /**
     * A name — could be a command name (mes, p_delay), a trigger type
     * (opnpc), a subject name (Guard), or a type name (def_int).
     * Value: the name text.
     *
     * We don't try to distinguish command names from other identifiers
     * at the lexer level. The parser figures out what an identifier
     * means based on context.
     */
    IDENTIFIER,

    /**
     * A local variable reference like $coins or $temp.
     * Value: the name WITHOUT the $ prefix.
     * The $ is consumed by the lexer — the parser just sees the name.
     */
    LOCAL_VAR,

    /**
     * A player variable reference like %quest_progress.
     * Value: the name WITHOUT the % prefix.
     */
    PLAYER_VAR,

    // ── Keywords ──────────────────────────────────────────────────────
    // These could technically be identifiers, but promoting them to their
    // own token types makes the parser simpler — it can just check the
    // token type instead of checking "is this an identifier AND is its
    // value 'if'?"

    /** The 'if' keyword. */
    IF,

    /** The 'else' keyword. */
    ELSE,

    /** The 'return' keyword. */
    RETURN,

    /** The 'def_int' keyword (declare an integer local variable). */
    DEF_INT,

    /** The 'def_string' keyword (declare a string local variable). */
    DEF_STRING,

    // ── Symbols ───────────────────────────────────────────────────────

    LPAREN,     // (
    RPAREN,     // )
    LBRACE,     // {
    RBRACE,     // }
    LBRACKET,   // [
    RBRACKET,   // ]
    COMMA,      // ,
    SEMICOLON,  // ;
    EQUALS,     // =

    // ── Comparison operators ──────────────────────────────────────────
    // We only need a few for our minimal language.

    NOT_EQUALS, // !
    LESS_THAN,  // <
    GREATER_THAN, // >
    LESS_EQUAL,   // <=
    GREATER_EQUAL, // >=

    // ── Special ───────────────────────────────────────────────────────

    /** Marks the end of the token stream. The parser uses this to know
     *  when it's consumed all the input. */
    EOF
}