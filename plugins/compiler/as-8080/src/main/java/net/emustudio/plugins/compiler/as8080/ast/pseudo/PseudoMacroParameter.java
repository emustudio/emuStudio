package net.emustudio.plugins.compiler.as8080.ast.pseudo;


import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

/**
 * Macro definition parameter
 */
public class PseudoMacroParameter extends Node {

    public PseudoMacroParameter() {
        super(0, 0);
        // the only child is ExprId
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroParameter();
    }
}
