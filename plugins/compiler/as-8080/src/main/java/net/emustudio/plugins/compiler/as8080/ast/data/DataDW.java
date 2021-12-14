package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

public class DataDW extends Node {

    public DataDW(int line, int column) {
        super(line, column);
        // child is expr
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new DataDW(line, column);
    }
}
