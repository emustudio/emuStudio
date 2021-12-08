package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

public class DataDB extends Node {

    public DataDB(int line, int column) {
        super(line, column);
        // child is string, expr or 8-bit instruction
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
