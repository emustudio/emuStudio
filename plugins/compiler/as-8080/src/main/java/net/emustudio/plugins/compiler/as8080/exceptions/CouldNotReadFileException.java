package net.emustudio.plugins.compiler.as8080.exceptions;

public class CouldNotReadFileException extends CompileException {
    private final static String MSG = "Could not read file: ";

    public CouldNotReadFileException(int line, int column, String filename, Throwable cause) {
        super(line, column, MSG + filename, cause);
    }
}
