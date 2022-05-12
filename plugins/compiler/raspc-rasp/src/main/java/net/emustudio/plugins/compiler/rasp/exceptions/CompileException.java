package net.emustudio.plugins.compiler.rasp.exceptions;

public class CompileException extends RuntimeException {
    public final int line;
    public final int column;

    public CompileException(int line, int column, String message) {
        super("[" + line + "," + column + "] " + message);

        this.column = column;
        this.line = line;
    }

    public CompileException(int line, int column, String message, Throwable cause) {
        super("[" + line + "," + column + "] " + message, cause);

        this.column = column;
        this.line = line;
    }
}
