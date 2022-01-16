package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Node;

import java.io.IOException;
import java.util.Objects;

public class CompileError {
    public static final int ERROR_ALREADY_DECLARED = 1;
    public static final int ERROR_INFINITE_LOOP_DETECTED = 2;
    public static final int ERROR_CANNOT_READ_FILE = 3;
    public static final int ERROR_NOT_DEFINED = 4;
    public static final int ERROR_AMBIGUOUS_EXPRESSION = 5;
    public static final int ERROR_IF_EXPRESSION_REFERENCES_OWN_BLOCK = 6;
    public static final int ERROR_DECLARATION_REFERENCES_ITSELF = 7;
    public static final int ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH = 8;
    public static final int ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED = 9;
    public static final int ERROR_VALUE_MUST_BE_POSITIVE = 10;

    public final int line;
    public final int column;
    public final String msg;
    public final int errorCode;

    private CompileError(int line, int column, int errorCode, String msg) {
        this.line = line;
        this.column = column;
        this.errorCode = errorCode;
        this.msg = Objects.requireNonNull(msg);
    }

    private CompileError(Node node, int errorCode, String msg) {
        this(node.line, node.column, errorCode, msg);
    }


    public static CompileError alreadyDeclared(Node node, String what) {
        return new CompileError(node, ERROR_ALREADY_DECLARED, what + " was already declared");
    }

    public static CompileError infiniteLoopDetected(Node node, String what) {
        return new CompileError(node, ERROR_INFINITE_LOOP_DETECTED, "Infinite " + what + " loop detected");
    }

    public static CompileError couldNotReadFile(Node node, String filename, IOException e) {
        return new CompileError(
            node, ERROR_CANNOT_READ_FILE, "Could not read file: " + filename + " (" + e.getMessage() + ")"
        );
    }

    public static CompileError notDefined(Node node, String what) {
        return new CompileError(node, ERROR_NOT_DEFINED, "Not defined: " + what);
    }

    public static CompileError ambiguousExpression(Node node) {
        return new CompileError(node, ERROR_AMBIGUOUS_EXPRESSION, "Ambiguous expression");
    }

    public static CompileError ifExpressionReferencesOwnBlock(Node node) {
        return new CompileError(
            node, ERROR_IF_EXPRESSION_REFERENCES_OWN_BLOCK, "If expression references declaration in its own block"
        );
    }

    public static CompileError declarationReferencesItself(Node node) {
        return new CompileError(node, ERROR_DECLARATION_REFERENCES_ITSELF, "Declaration references itself");
    }

    public static CompileError macroArgumentsDoNotMatch(Node node) {
        return new CompileError(
            node, ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH, "Macro call arguments do not match with defined parameters"
        );
    }

    public static CompileError expressionIsBiggerThanExpected(Node node, int expectedBytes, int wasBytes) {
        return new CompileError(
            node,
            ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED,
            "Expression (" + wasBytes + " bytes) is bigger than expected (" + expectedBytes + " byte(s))"
        );
    }

    public static CompileError valueMustBePositive(Node node) {
        return new CompileError(node, ERROR_VALUE_MUST_BE_POSITIVE, "Value must be positive");
    }

    @Override
    public String toString() {
        return "CompileError{" +
            "line=" + line +
            ", column=" + column +
            ", msg='" + msg + '\'' +
            ", errorCode=" + errorCode +
            '}';
    }
}
