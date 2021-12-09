package net.emustudio.plugins.compiler.as8080.exceptions;

public class AlreadyDeclaredException extends CompileException {
    private final static String MSG = " was already declared";

    public AlreadyDeclaredException(int line, int column, String message) {
        super(line, column, message + MSG);
    }
}
