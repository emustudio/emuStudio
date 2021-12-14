package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.function.Function;

public class ExprNumber extends Node {
    public final int number;

    public ExprNumber(int line, int column, int number) {
        super(line, column);
        this.number = number;
    }

    public ExprNumber(Token number, Function<Token, Integer> parser) {
        this(number.getLine(), number.getCharPositionInLine(), parser.apply(number));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprNumber(" + number + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprNumber(line, column, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprNumber that = (ExprNumber) o;
        return number == that.number;
    }

    @Override
    public int hashCode() {
        return number;
    }
}
