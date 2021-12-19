package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

/**
 * Macro call arguments are passed when macro is called.
 */
public class PseudoMacroArgument extends Node {

    public PseudoMacroArgument() {
        super(0, 0);
        // the only child is expr
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroArgument();
    }
}
