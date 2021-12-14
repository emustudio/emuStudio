package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

public class DataDS extends Node {
    public DataDS(int line, int column) {
        super(line, column);
        // child is expr
        // expr cannot be negative
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new DataDS(line, column);
    }
}
