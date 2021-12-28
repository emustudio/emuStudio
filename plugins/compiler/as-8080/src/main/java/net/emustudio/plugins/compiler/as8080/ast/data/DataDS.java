package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

/**
 * "Data Store" node. Inserts some space.
 * Child is an expression which must not use forward references and must not be negative.
 */
public class DataDS extends Node {
    public DataDS(int line, int column) {
        super(line, column);
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
