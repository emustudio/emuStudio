package net.emustudio.plugins.compiler.as8080.ast.pseudo;


import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;

/**
 * Macro definition parameter
 */
public class PseudoMacroParameter extends Node {

    public PseudoMacroParameter(int line, int column) {
        super(line, column);
        // the only child is ExprId
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroParameter(line, column);
    }
}
