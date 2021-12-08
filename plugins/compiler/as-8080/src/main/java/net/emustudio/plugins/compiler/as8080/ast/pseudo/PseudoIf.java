package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

public class PseudoIf extends Node {

    public PseudoIf(int line, int column) {
        super(line, column);
        // expr is the first child
        // statement is the second child
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
