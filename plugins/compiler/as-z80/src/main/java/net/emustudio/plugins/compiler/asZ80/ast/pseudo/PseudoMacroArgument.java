package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

/**
 * Macro call arguments are passed when macro is called.
 */
public class PseudoMacroArgument extends Node {

    public PseudoMacroArgument(int line, int column) {
        super(line, column);
        // the only child is expr
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroArgument(line, column);
    }
}
