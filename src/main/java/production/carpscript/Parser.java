package production.carpscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The parser reads a token stream from the lexer and produces compiled
 * instruction arrays for each trigger block.
 *
 * WHAT IT DOES:
 * ─────────────
 * The parser understands the STRUCTURE of the language. While the lexer
 * just identifies pieces (this is a string, this is a parenthesis), the
 * parser knows how pieces fit together: "this IDENTIFIER followed by LPAREN
 * means a function call" or "this IF followed by a condition and braces
 * means a conditional block."
 *
 * As it recognizes structure, it emits instructions. A function call emits
 * PUSH instructions for each argument followed by an INVOKE. An if/else
 * emits comparison instructions and JUMP/JUMP_IF_NOT. The output is the
 * same instruction arrays we hand-built in Steps 1 and 2.
 *
 * PARSER STYLE: RECURSIVE DESCENT
 * ────────────────────────────────
 * This is a "recursive descent" parser — the simplest kind. Each grammar
 * rule (trigger block, statement, if-statement, expression) is a method.
 * Methods call each other to handle nesting: parseIfStatement() calls
 * parseStatement() for the body, which might call parseIfStatement() again
 * for a nested if. The call stack mirrors the nesting of the source code.
 *
 * BACKPATCHING:
 * ─────────────
 * The trickiest part is jump targets. When we encounter "if (condition) {"
 * we need to emit JUMP_IF_NOT, but we don't yet know WHERE to jump to —
 * we haven't parsed the if-body yet, so we don't know how many instructions
 * it will be. The solution is "backpatching": emit the jump with a dummy
 * target (0), remember its index, parse the body, then go back and fill in
 * the real target. You'll see this pattern in parseIfStatement().
 *
 * GRAMMAR (what the parser accepts):
 * ───────────────────────────────────
 *   file            →  trigger_block*
 *   trigger_block   →  '[' IDENTIFIER ',' IDENTIFIER ']' statement*
 *   statement       →  if_statement | return_statement | var_decl
 *                     | var_assignment | command_call
 *   if_statement    →  'if' '(' condition ')' '{' statement* '}'
 *                      ('else' '{' statement* '}')?
 *   return_statement → 'return'
 *   var_decl        →  ('def_int'|'def_string') LOCAL_VAR '=' expression
 *   var_assignment  →  LOCAL_VAR '=' expression
 *                     | PLAYER_VAR '=' expression
 *   command_call    →  IDENTIFIER '(' expression_list? ')'
 *   condition       →  expression ('='|'!'|'<'|'>'|'<='|'>=') expression
 *   expression      →  INTEGER | STRING | LOCAL_VAR | PLAYER_VAR
 *                     | command_call
 */
public class Parser {

    /** The token stream from the lexer. */
    private final List<Token> tokens;

    /** Current position in the token list. */
    private int pos;

    /** Source file name (for error messages and CompiledScript metadata). */
    private final String sourceFile;

    // ── Per-trigger-block state (reset for each block) ────────────────

    /** Instructions being built for the current trigger block. */
    private List<Instruction> instructions;

    /**
     * Local variable name → slot index mapping for the current block.
     * Each trigger block has its own independent set of locals.
     *
     * When the parser sees "def_int $coins = ...", it assigns $coins the
     * next available slot number (0, 1, 2, ...) and records it here.
     * When it later sees "$coins" in an expression, it looks up the slot.
     */
    private Map<String, Integer> localVarSlots;

    /** Next available local variable slot. */
    private int nextLocalSlot;

    public Parser(List<Token> tokens, String sourceFile) {
        this.tokens = tokens;
        this.sourceFile = sourceFile;
        this.pos = 0;
    }

    // ══════════════════════════════════════════════════════════════════
    //  Top-Level Parsing
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse the entire token stream and return all compiled trigger blocks.
     *
     * A source file can contain multiple trigger blocks:
     *   [opnpc,Guard]
     *   ...
     *   [opnpc,Shopkeeper]
     *   ...
     *
     * Each becomes a separate CompiledScript.
     */
    public List<CompiledScript> parseFile() {
        List<CompiledScript> scripts = new ArrayList<CompiledScript>();

        while (peek().type != TokenType.EOF) {
            scripts.add(parseTriggerBlock());
        }

        return scripts;
    }

    /**
     * Parse a single trigger block: the header and all its statements.
     *
     * Syntax:  [trigger_type,subject]
     *          statement*
     *
     * The block ends when we hit another '[' (next trigger) or EOF.
     */
    private CompiledScript parseTriggerBlock() {
        // ── Reset per-block state ─────────────────────────────────────
        instructions = new ArrayList<Instruction>();
        localVarSlots = new HashMap<String, Integer>();
        nextLocalSlot = 0;

        // ── Parse the trigger header: [type,subject] ──────────────────
        expect(TokenType.LBRACKET, "Expected '[' to start trigger header");
        String triggerType = expect(TokenType.IDENTIFIER, "Expected trigger type").value;
        expect(TokenType.COMMA, "Expected ',' in trigger header");
        String subject = expect(TokenType.IDENTIFIER, "Expected trigger subject").value;
        expect(TokenType.RBRACKET, "Expected ']' to close trigger header");

        // ── Parse statements until we hit the next trigger or EOF ─────
        // We know a new trigger block starts with '[', so we stop there.
        while (peek().type != TokenType.EOF && peek().type != TokenType.LBRACKET) {
            parseStatement();
        }

        // ── Build the CompiledScript ──────────────────────────────────
        Instruction[] instrArray = instructions.toArray(new Instruction[instructions.size()]);
        return new CompiledScript(triggerType, subject, instrArray, sourceFile);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Statement Parsing
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse a single statement. Looks at the current token to decide
     * what kind of statement this is, then delegates to the right method.
     */
    private void parseStatement() {
        Token current = peek();

        switch (current.type) {
            case IF:
                parseIfStatement();
                break;

            case RETURN:
                parseReturnStatement();
                break;

            case DEF_INT:
            case DEF_STRING:
                parseVarDeclaration();
                break;

            case LOCAL_VAR:
                parseLocalVarAssignment();
                break;

            case PLAYER_VAR:
                parsePlayerVarAssignment();
                break;

            case IDENTIFIER:
                // An identifier at statement level is a command call: mes("hello")
                parseCommandCallStatement();
                break;

            default:
                throw new ParseException("Unexpected token: " + current.type +
                        " (" + current.value + ")", current.line);
        }
    }

    // ── If / Else ─────────────────────────────────────────────────────

    /**
     * Parse an if/else statement.
     *
     * Syntax:  if (condition) { statements } [else { statements }]
     *
     * This is where backpatching happens. Here's the instruction pattern:
     *
     *   [condition instructions]    ← pushes 1 (true) or 0 (false)
     *   JUMP_IF_NOT ???             ← placeholder, filled in later
     *   [if-body instructions]
     *   JUMP ???                    ← only if there's an else; also placeholder
     *   [else-body instructions]    ← JUMP_IF_NOT target points here
     *   ...                         ← JUMP target points here
     *
     * The ??? values get filled in once we know how long each body is.
     */
    private void parseIfStatement() {
        advance(); // consume 'if'
        expect(TokenType.LPAREN, "Expected '(' after 'if'");

        // ── Parse the condition ───────────────────────────────────────
        // The condition pushes a 1 or 0 onto the stack.
        parseCondition();

        expect(TokenType.RPAREN, "Expected ')' after condition");

        // ── Emit JUMP_IF_NOT with placeholder target ──────────────────
        int jumpIfNotIndex = instructions.size();
        emit(Opcode.JUMP_IF_NOT, 0); // placeholder — will backpatch

        // ── Parse the if-body ─────────────────────────────────────────
        expect(TokenType.LBRACE, "Expected '{' to start if-body");
        while (peek().type != TokenType.RBRACE) {
            if (peek().type == TokenType.EOF) {
                throw new ParseException("Unexpected end of file inside if-body", peek().line);
            }
            parseStatement();
        }
        advance(); // consume '}'

        // ── Handle optional else ──────────────────────────────────────
        if (peek().type == TokenType.ELSE) {
            advance(); // consume 'else'

            // Before the else-body, the if-body needs to jump OVER it.
            // Otherwise execution would fall through from the if-body
            // into the else-body.
            int jumpOverElseIndex = instructions.size();
            emit(Opcode.JUMP, 0); // placeholder

            // NOW we know where the else-body starts — backpatch JUMP_IF_NOT
            backpatch(jumpIfNotIndex, instructions.size());

            // Parse the else-body
            expect(TokenType.LBRACE, "Expected '{' to start else-body");
            while (peek().type != TokenType.RBRACE) {
                if (peek().type == TokenType.EOF) {
                    throw new ParseException("Unexpected end of file inside else-body", peek().line);
                }
                parseStatement();
            }
            advance(); // consume '}'

            // Backpatch the jump that skips the else-body
            backpatch(jumpOverElseIndex, instructions.size());

        } else {
            // No else — JUMP_IF_NOT just jumps past the if-body
            backpatch(jumpIfNotIndex, instructions.size());
        }
    }

    // ── Return ────────────────────────────────────────────────────────

    private void parseReturnStatement() {
        advance(); // consume 'return'
        emit(Opcode.RETURN);
    }

    // ── Variable Declaration ──────────────────────────────────────────

    /**
     * Parse a local variable declaration.
     *
     * Syntax:  def_int $name = expression
     *          def_string $name = expression
     *
     * Emits: [expression instructions], POP_LOCAL slot
     *
     * The type keyword (def_int / def_string) tells us the intended type,
     * but our minimal VM doesn't enforce types at runtime — it's informational,
     * matching how RuneScript works. We just assign a slot number.
     */
    private void parseVarDeclaration() {
        advance(); // consume def_int or def_string

        Token varToken = expect(TokenType.LOCAL_VAR,
                "Expected local variable name ($name) after type declaration");
        String varName = varToken.value;

        // Register this variable and get its slot number
        if (localVarSlots.containsKey(varName)) {
            throw new ParseException("Local variable '$" + varName +
                    "' is already declared", varToken.line);
        }
        int slot = nextLocalSlot++;
        localVarSlots.put(varName, Integer.valueOf(slot));

        expect(TokenType.EQUALS, "Expected '=' in variable declaration");

        // Parse the initial value expression — this pushes a value onto the stack
        parseExpression();

        // Pop the value into the local variable slot
        emit(Opcode.POP_LOCAL, slot);
    }

    // ── Variable Assignment ───────────────────────────────────────────

    /**
     * Parse a local variable assignment.
     * Syntax:  $name = expression
     */
    private void parseLocalVarAssignment() {
        Token varToken = advance(); // consume the LOCAL_VAR token
        String varName = varToken.value;

        Integer slot = localVarSlots.get(varName);
        if (slot == null) {
            throw new ParseException("Undeclared local variable: '$" + varName +
                    "' — did you forget def_int or def_string?", varToken.line);
        }

        expect(TokenType.EQUALS, "Expected '=' in variable assignment");
        parseExpression();
        emit(Opcode.POP_LOCAL, slot.intValue());
    }

    /**
     * Parse a player variable assignment.
     * Syntax:  %name = expression
     */
    private void parsePlayerVarAssignment() {
        Token varToken = advance(); // consume the PLAYER_VAR token
        String varName = varToken.value;

        expect(TokenType.EQUALS, "Expected '=' in variable assignment");
        parseExpression();
        emit(Opcode.POP_VARP, varName);
    }

    // ── Command Call (as a statement) ─────────────────────────────────

    /**
     * Parse a command call used as a statement (not inside an expression).
     *
     * Syntax:  command_name(arg1, arg2, ...)
     *
     * This is for commands like mes("hello") that are used on their own
     * line and whose return value (if any) is discarded.
     */
    private void parseCommandCallStatement() {
        parseCommandCall();
        // Note: if the command returns a value, it's left on the stack.
        // For statement-level calls like mes() that return null, this is fine.
        // For commands that DO return values (like inv_total), calling them
        // as a bare statement is unusual but harmless — the value just sits
        // on the stack unused. A more complete compiler would emit a POP
        // to discard it, but for our minimal version this works.
    }

    // ══════════════════════════════════════════════════════════════════
    //  Condition Parsing
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse a condition: two expressions separated by a comparison operator.
     *
     * Syntax:  expression op expression
     * Where op is: = ! < > <= >=
     *
     * Emits: [left expr], [right expr], INVOKE comparison_command 2
     *
     * The comparison command pushes 1 (true) or 0 (false) onto the stack,
     * which JUMP_IF_NOT then consumes.
     */
    private void parseCondition() {
        // Left side
        parseExpression();

        // Operator — determines which comparison command to invoke
        Token opToken = peek();
        String comparisonCommand;

        switch (opToken.type) {
            case EQUALS:        comparisonCommand = "eq"; break;
            case NOT_EQUALS:    comparisonCommand = "ne"; break;
            case LESS_THAN:     comparisonCommand = "lt"; break;
            case GREATER_THAN:  comparisonCommand = "gt"; break;
            case LESS_EQUAL:    comparisonCommand = "le"; break;
            case GREATER_EQUAL: comparisonCommand = "ge"; break;
            default:
                throw new ParseException("Expected comparison operator (= ! < > <= >=) " +
                        "but got: " + opToken.type, opToken.line);
        }
        advance(); // consume the operator

        // Right side
        parseExpression();

        // Emit the comparison as a command invocation
        // This pops both values, compares them, and pushes 1 or 0
        emit(Opcode.INVOKE, comparisonCommand, 2);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Expression Parsing
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse a single expression: a value that gets pushed onto the stack.
     *
     * An expression can be:
     *   - An integer literal:     42       → PUSH_INT 42
     *   - A string literal:       "hello"  → PUSH_STRING "hello"
     *   - A local variable:       $coins   → PUSH_LOCAL slot
     *   - A player variable:      %quest   → PUSH_VARP "quest"
     *   - A command call:         inv_total(995) → [args], INVOKE
     */
    private void parseExpression() {
        Token current = peek();

        switch (current.type) {
            case INTEGER:
                advance();
                emit(Opcode.PUSH_INT, Integer.parseInt(current.value));
                break;

            case STRING:
                advance();
                emit(Opcode.PUSH_STRING, current.value);
                break;

            case LOCAL_VAR: {
                advance();
                Integer slot = localVarSlots.get(current.value);
                if (slot == null) {
                    throw new ParseException("Undeclared local variable: '$" +
                            current.value + "'", current.line);
                }
                emit(Opcode.PUSH_LOCAL, slot.intValue());
                break;
            }

            case PLAYER_VAR:
                advance();
                emit(Opcode.PUSH_VARP, current.value);
                break;

            case IDENTIFIER:
                // An identifier in expression position must be a command call
                // that returns a value (like inv_total).
                parseCommandCall();
                break;

            default:
                throw new ParseException("Expected expression but got: " +
                        current.type + " (" + current.value + ")", current.line);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Command Call Parsing
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse a command call: name(arg1, arg2, ...)
     *
     * Emits PUSH instructions for each argument (left to right), then
     * INVOKE with the command name and argument count.
     *
     * Example:  give(995, 1000)
     * Emits:    PUSH_INT 995
     *           PUSH_INT 1000
     *           INVOKE "give" (args: 2)
     */
    private void parseCommandCall() {
        Token nameToken = expect(TokenType.IDENTIFIER, "Expected command name");
        String commandName = nameToken.value;

        expect(TokenType.LPAREN, "Expected '(' after command name '" + commandName + "'");

        // Parse argument list (zero or more expressions separated by commas)
        int argCount = 0;
        if (peek().type != TokenType.RPAREN) {
            parseExpression();
            argCount++;

            while (peek().type == TokenType.COMMA) {
                advance(); // consume ','
                parseExpression();
                argCount++;
            }
        }

        expect(TokenType.RPAREN, "Expected ')' after arguments");

        emit(Opcode.INVOKE, commandName, argCount);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Instruction Emission Helpers
    // ══════════════════════════════════════════════════════════════════

    /** Emit an instruction with no operand. */
    private void emit(Opcode opcode) {
        instructions.add(new Instruction(opcode));
    }

    /** Emit an instruction with one operand. */
    private void emit(Opcode opcode, Object operand) {
        instructions.add(new Instruction(opcode, operand));
    }

    /** Emit an instruction with two operands (used for INVOKE). */
    private void emit(Opcode opcode, Object operand, int operand2) {
        instructions.add(new Instruction(opcode, operand, operand2));
    }

    /**
     * Backpatch a jump instruction's target.
     *
     * When we emit a JUMP or JUMP_IF_NOT, we often don't know the target
     * yet. We emit it with target 0, then come back and fix it once we
     * know where execution should jump to.
     *
     * @param instructionIndex  The index of the jump instruction to fix
     * @param target            The instruction index to jump to
     */
    private void backpatch(int instructionIndex, int target) {
        Instruction old = instructions.get(instructionIndex);
        instructions.set(instructionIndex,
                new Instruction(old.opcode, Integer.valueOf(target), old.operand2));
    }

    // ══════════════════════════════════════════════════════════════════
    //  Token Stream Navigation
    // ══════════════════════════════════════════════════════════════════

    /** Look at the current token without consuming it. */
    private Token peek() {
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // return EOF
        }
        return tokens.get(pos);
    }

    /** Consume the current token and return it. Advances to the next. */
    private Token advance() {
        Token token = peek();
        pos++;
        return token;
    }

    /**
     * Consume the current token, asserting it has the expected type.
     * If it doesn't match, throw a parse error with a helpful message.
     */
    private Token expect(TokenType expected, String errorMessage) {
        Token token = peek();
        if (token.type != expected) {
            throw new ParseException(errorMessage +
                    " (got " + token.type + ": '" + token.value + "')", token.line);
        }
        return advance();
    }

    // ── Error Handling ────────────────────────────────────────────────

    public static class ParseException extends RuntimeException {
        public final int line;

        public ParseException(String message, int line) {
            super("Parse error on line " + line + " — " + message);
            this.line = line;
        }
    }
}