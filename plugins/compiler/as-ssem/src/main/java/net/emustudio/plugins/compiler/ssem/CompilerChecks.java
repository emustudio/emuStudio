package net.emustudio.plugins.compiler.ssem;

public class CompilerChecks {

    public static void checkStartLineDefined(boolean defined, Position pos, int startLine) {
        if (defined) {
            throw new CompileException(pos.line, pos.column, "Start line is already defined (at line " + startLine + ")!");
        }
    }

    public static void checkLineOutOfBounds(Position pos, int line) {
        if (line < 0 || line > 31) {
            throw new CompileException(pos.line, pos.column, "Line number is out of bounds <0;31>: " + line);
        }
    }

    public static void checkDuplicateLineDefinition(boolean duplicate, Position pos, int line) {
        if (duplicate) {
            throw new CompileException(pos.line, pos.column, "Duplicate line definition: " + line);
        }
    }

    public static void checkUnknownInstruction(boolean unknown, Position pos) {
        if (unknown) {
            throw new CompileException(pos.line, pos.column, "Unrecognized instruction");
        }
    }

    public static void checkOperandOutOfBounds(Position pos, int tokenType, long operand) {
        if (tokenType != SSEMLexer.BNUM && tokenType != SSEMLexer.NUM && (operand < 0 || operand > 31)) {
            throw new CompileException(
                pos.line, pos.column, "Operand must be between <0, 31>; it was " + operand
            );
        }
    }
}
