package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class ExprUnary extends Node {
    public final int operation;

    public ExprUnary(int line, int column, int op) {
        super(line, column);
        this.operation = op;
        // child is expr
    }

    public ExprUnary(Token op) {
        this(op.getLine(), op.getCharPositionInLine(), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprUnary(" + operation + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprUnary exprUnary = (ExprUnary) o;
        return operation == exprUnary.operation;
    }

    @Override
    public int hashCode() {
        return operation;
    }
}
