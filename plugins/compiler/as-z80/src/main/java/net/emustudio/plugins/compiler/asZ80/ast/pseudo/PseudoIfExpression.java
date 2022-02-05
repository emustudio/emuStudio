package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

public class PseudoIfExpression extends Node {
    public PseudoIfExpression(int line, int column) {
        super(line, column);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoIfExpression(line, column);
    }
}
