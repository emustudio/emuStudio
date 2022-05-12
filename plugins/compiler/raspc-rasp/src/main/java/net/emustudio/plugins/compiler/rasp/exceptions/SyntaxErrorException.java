package net.emustudio.plugins.compiler.rasp.exceptions;

public class SyntaxErrorException extends CompileException {
    public SyntaxErrorException(int line, int column, String message) {
        super(line, column, message);
    }

    public SyntaxErrorException(int line, int column, String message, Throwable cause) {
        super(line, column, message, cause);
    }
}
