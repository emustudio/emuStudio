package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

public class PseudoOrg extends Node {

    public PseudoOrg(int line, int column) {
        super(line, column);
        // expr is the only child
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoOrg(line, column);
    }
}
