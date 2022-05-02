package net.emustudio.plugins.compiler.ssem;

import net.jcip.annotations.Immutable;
import org.antlr.v4.runtime.Token;

@Immutable
public class Position {
    public final static Position UNKNOWN = new Position(-1, -1);

    public final int line;
    public final int column;

    public Position(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public static Position of(Token token) {
        if (token == null) {
            return UNKNOWN;
        }
        return new Position(token.getLine(), token.getCharPositionInLine());
    }

    public static Position unknown() {
        return UNKNOWN;
    }
}
