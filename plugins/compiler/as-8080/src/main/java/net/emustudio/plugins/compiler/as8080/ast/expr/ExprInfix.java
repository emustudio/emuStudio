package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class ExprInfix extends Node {
    public final int operation;

    public ExprInfix(int line, int column, int op) {
        super(line, column);
        this.operation = op;
        // children are: left, right
    }

    public ExprInfix(Token op) {
        this(op.getLine(), op.getCharPositionInLine(), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprInfix(" + operation + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprInfix(line, column, operation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprInfix infix = (ExprInfix) o;
        return operation == infix.operation;
    }

    @Override
    public int hashCode() {
        return operation;
    }
}
