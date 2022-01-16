package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.ParsingUtils.parseLitString;

public class ExprString extends Node {
    public final String string;

    public ExprString(int line, int column, String string) {
        super(line, column);
        this.string = Objects.requireNonNull(string);
    }

    public ExprString(Token str) {
        this(str.getLine(), str.getCharPositionInLine(), parseLitString(str));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new ExprString(line, column, string);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprString that = (ExprString) o;
        return Objects.equals(string, that.string);
    }

    @Override
    protected String toStringShallow() {
        return "ExprString(" + string + ")";
    }
}
