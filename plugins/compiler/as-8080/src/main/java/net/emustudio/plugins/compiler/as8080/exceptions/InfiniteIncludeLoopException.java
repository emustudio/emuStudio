package net.emustudio.plugins.compiler.as8080.exceptions;

public class InfiniteIncludeLoopException extends CompileException {
    private static final String MSG = "Infinite include loop detected";

    public InfiniteIncludeLoopException(int line, int column) {
        super(line, column, MSG);
    }
}
