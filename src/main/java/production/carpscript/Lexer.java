package production.carpscript;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer (also called a "tokenizer" or "scanner") reads raw source text
 * and breaks it into a list of tokens.
 *
 * HOW IT WORKS:
 * ─────────────
 * The lexer maintains a cursor position in the source string. On each step,
 * it looks at the current character and decides what kind of token starts
 * here. Then it advances the cursor past that token and adds it to the list.
 *
 * For example, given the source text:
 *   mes("Hello")
 *
 * The lexer works through it character by character:
 *   - Sees 'm' → start of an identifier → reads "mes" → IDENTIFIER("mes")
 *   - Sees '(' → single character symbol → LPAREN
 *   - Sees '"' → start of a string → reads until closing '"' → STRING("Hello")
 *   - Sees ')' → single character symbol → RPAREN
 *
 * Whitespace and comments are skipped — they're meaningful to humans but
 * not to the parser.
 *
 * WHAT THE LEXER DOESN'T DO:
 * ──────────────────────────
 * The lexer doesn't understand structure. It doesn't know that mes is a
 * command name, or that "Hello" is an argument to it. It just sees labeled
 * chunks. The parser is the one that assembles tokens into meaning.
 *
 * ERROR HANDLING:
 * ───────────────
 * When the lexer hits a character it doesn't recognize, it throws an
 * exception with the line number. This catches problems like stray
 * characters or unsupported syntax early, with a clear error message.
 */
public class Lexer {

    /** The full source text being tokenized. */
    private final String source;

    /** Current position in the source string (index of next char to read). */
    private int pos;

    /** Current line number (1-based, for error messages). */
    private int line;

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
    }

    /**
     * Tokenize the entire source string and return the list of tokens.
     * The list always ends with an EOF token.
     *
     * This is the main entry point. Typical usage:
     *   Lexer lexer = new Lexer(sourceCode);
     *   List<Token> tokens = lexer.tokenize();
     *   // hand tokens to the parser
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<Token>();

        while (pos < source.length()) {
            // Skip whitespace and comments before each token
            skipWhitespaceAndComments();

            // Check if we hit the end after skipping
            if (pos >= source.length()) {
                break;
            }

            // Read the next token
            Token token = readToken();
            tokens.add(token);
        }

        // Always end with EOF so the parser has a clean termination signal
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    // ── Core Token Reading ────────────────────────────────────────────

    /**
     * Read a single token starting at the current position.
     * Advances pos past the token.
     */
    private Token readToken() {
        char c = source.charAt(pos);

        // ── Single-character symbols ──────────────────────────────────
        // These are unambiguous: one character, one token.
        switch (c) {
            case '(': pos++; return new Token(TokenType.LPAREN,    "(", line);
            case ')': pos++; return new Token(TokenType.RPAREN,    ")", line);
            case '{': pos++; return new Token(TokenType.LBRACE,    "{", line);
            case '}': pos++; return new Token(TokenType.RBRACE,    "}", line);
            case '[': pos++; return new Token(TokenType.LBRACKET,  "[", line);
            case ']': pos++; return new Token(TokenType.RBRACKET,  "]", line);
            case ',': pos++; return new Token(TokenType.COMMA,     ",", line);
            case ';': pos++; return new Token(TokenType.SEMICOLON, ";", line);
        }

        // ── Operators that might be one or two characters ─────────────
        // '=' is EQUALS, but we need to distinguish from '==' if we ever
        // add that. '<' vs '<=', '>' vs '>=', '!' for NOT_EQUALS.
        if (c == '=') {
            pos++;
            return new Token(TokenType.EQUALS, "=", line);
        }

        if (c == '!') {
            pos++;
            return new Token(TokenType.NOT_EQUALS, "!", line);
        }

        if (c == '<') {
            pos++;
            if (pos < source.length() && source.charAt(pos) == '=') {
                pos++;
                return new Token(TokenType.LESS_EQUAL, "<=", line);
            }
            return new Token(TokenType.LESS_THAN, "<", line);
        }

        if (c == '>') {
            pos++;
            if (pos < source.length() && source.charAt(pos) == '=') {
                pos++;
                return new Token(TokenType.GREATER_EQUAL, ">=", line);
            }
            return new Token(TokenType.GREATER_THAN, ">", line);
        }

        // ── String literals ───────────────────────────────────────────
        // Start with a quote, read until the matching close quote.
        if (c == '"') {
            return readString();
        }

        // ── Numeric literals ──────────────────────────────────────────
        // Start with a digit (or a minus sign followed by a digit for negatives).
        if (isDigit(c) || (c == '-' && pos + 1 < source.length() && isDigit(source.charAt(pos + 1)))) {
            return readNumber();
        }

        // ── Variable references ───────────────────────────────────────
        // $ starts a local variable, % starts a player variable.
        // We consume the prefix and read the name.
        if (c == '$') {
            pos++; // consume the $
            String name = readIdentifierText();
            if (name.isEmpty()) {
                throw new LexerException("Expected variable name after '$'", line);
            }
            return new Token(TokenType.LOCAL_VAR, name, line);
        }

        if (c == '%') {
            pos++; // consume the %
            String name = readIdentifierText();
            if (name.isEmpty()) {
                throw new LexerException("Expected variable name after '%'", line);
            }
            return new Token(TokenType.PLAYER_VAR, name, line);
        }

        // ── Identifiers and keywords ──────────────────────────────────
        // Start with a letter or underscore. Could be a keyword (if, else,
        // return, def_int) or a regular identifier (mes, Guard, opnpc).
        if (isIdentifierStart(c)) {
            return readIdentifierOrKeyword();
        }

        // ── Nothing matched ───────────────────────────────────────────
        throw new LexerException("Unexpected character: '" + c + "'", line);
    }

    // ── Specialized Readers ───────────────────────────────────────────

    /**
     * Read a string literal. Called when pos is at the opening quote.
     * Handles the escape sequence \" for literal quotes inside strings.
     */
    private Token readString() {
        int startLine = line;
        pos++; // consume the opening quote

        StringBuilder sb = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);

            // Backslash escape: \" produces a literal quote in the string
            if (c == '\\' && pos + 1 < source.length()) {
                char next = source.charAt(pos + 1);
                if (next == '"') {
                    sb.append('"');
                    pos += 2;
                    continue;
                }
                if (next == 'n') {
                    sb.append('\n');
                    pos += 2;
                    continue;
                }
                if (next == '\\') {
                    sb.append('\\');
                    pos += 2;
                    continue;
                }
                // Unknown escape — just include the backslash literally
            }

            // Closing quote — we're done
            if (c == '"') {
                pos++; // consume the closing quote
                return new Token(TokenType.STRING, sb.toString(), startLine);
            }

            // Track newlines inside multi-line strings (if we ever allow them)
            if (c == '\n') {
                line++;
            }

            sb.append(c);
            pos++;
        }

        // If we get here, the string was never closed
        throw new LexerException("Unterminated string literal", startLine);
    }

    /**
     * Read a numeric literal. Called when pos is at a digit (or a leading minus).
     * For now, only integers — no decimals.
     */
    private Token readNumber() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        // Handle optional leading minus for negative numbers
        if (source.charAt(pos) == '-') {
            sb.append('-');
            pos++;
        }

        // Read digits
        while (pos < source.length() && isDigit(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            pos++;
        }

        return new Token(TokenType.INTEGER, sb.toString(), startLine);
    }

    /**
     * Read an identifier or keyword. Called when pos is at a letter or underscore.
     *
     * After reading the full text, we check if it matches a keyword.
     * If so, we return the keyword token type. Otherwise, it's an IDENTIFIER.
     *
     * This is how most languages handle keywords — they're just identifiers
     * that happen to match a reserved list.
     */
    private Token readIdentifierOrKeyword() {
        int startLine = line;
        String text = readIdentifierText();

        // Check for keywords
        if (text.equals("if"))         return new Token(TokenType.IF,         text, startLine);
        if (text.equals("else"))       return new Token(TokenType.ELSE,       text, startLine);
        if (text.equals("return"))     return new Token(TokenType.RETURN,     text, startLine);
        if (text.equals("def_int"))    return new Token(TokenType.DEF_INT,    text, startLine);
        if (text.equals("def_string")) return new Token(TokenType.DEF_STRING, text, startLine);

        // Not a keyword — it's a regular identifier
        return new Token(TokenType.IDENTIFIER, text, startLine);
    }

    /**
     * Read identifier characters starting at the current position.
     * An identifier is: letter or underscore, followed by letters, digits,
     * or underscores. This matches names like: mes, p_delay, Guard, quest_progress.
     *
     * Used by both readIdentifierOrKeyword() and the $ / % variable readers.
     */
    private String readIdentifierText() {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && isIdentifierPart(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            pos++;
        }
        return sb.toString();
    }

    // ── Whitespace and Comment Skipping ───────────────────────────────

    /**
     * Advance past whitespace and comments.
     *
     * We loop because comments and whitespace can be interleaved:
     *   // comment
     *   mes("hello")   // another comment
     *
     * After this method returns, pos is either at the start of a real token
     * or past the end of the source.
     */
    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);

            // Whitespace: skip and track newlines
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            if (c == '\n') {
                pos++;
                line++;
                continue;
            }

            // Line comments: // skip to end of line
            if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                pos += 2; // skip the //
                while (pos < source.length() && source.charAt(pos) != '\n') {
                    pos++;
                }
                // Don't consume the \n here — the next loop iteration handles it
                // and increments the line counter
                continue;
            }

            // Not whitespace or a comment — we found the start of a token
            break;
        }
    }

    // ── Character Classification ──────────────────────────────────────

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Can this character START an identifier? Letters and underscore. */
    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /** Can this character CONTINUE an identifier? Letters, digits, underscore. */
    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    // ── Error Handling ────────────────────────────────────────────────

    /**
     * Exception thrown when the lexer encounters invalid source text.
     * Includes the line number so the script author can find the problem.
     */
    public static class LexerException extends RuntimeException {
        public final int line;

        public LexerException(String message, int line) {
            super("Lexer error on line " + line + ": " + message);
            this.line = line;
        }
    }
}