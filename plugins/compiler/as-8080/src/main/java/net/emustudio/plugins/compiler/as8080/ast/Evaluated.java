package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;

public class Evaluated extends Node {

    public Evaluated(int line, int column) {
        super(line, column);
        // children are sizeBytes * 1-byte ExprNumbers
    }

    public int getValue() {
        return ((ExprNumber) getChild(0)).number;
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(line, column);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
