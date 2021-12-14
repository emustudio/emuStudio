package net.emustudio.plugins.compiler.as8080.exceptions.used;

import net.emustudio.plugins.compiler.as8080.CompileError;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;

public class FatalError extends CompileException {

    public FatalError(int line, int column, String why) {
        super(line, column, "Fatal error (cannot continue): " + why);
    }

    public static void now(int line, int column, String why) {
        throw new FatalError(line, column, why);
    }

    public static void now(CompileError error) {
        now(error.line, error.column, error.msg);
    }
}
