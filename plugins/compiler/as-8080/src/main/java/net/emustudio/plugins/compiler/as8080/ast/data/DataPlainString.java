package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.parseLitString;

public class DataPlainString extends Node {
    public final String string;

    public DataPlainString(int line, int column, String string) {
        super(line, column);
        this.string = Objects.requireNonNull(string);
    }

    public DataPlainString(Token string) {
        this(string.getLine(), string.getCharPositionInLine(), parseLitString(string));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "DataPlainString('" + string + "')";
    }

    @Override
    protected Node mkCopy() {
        return new DataPlainString(line, column, string);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataPlainString that = (DataPlainString) o;
        return Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return string != null ? string.hashCode() : 0;
    }
}
