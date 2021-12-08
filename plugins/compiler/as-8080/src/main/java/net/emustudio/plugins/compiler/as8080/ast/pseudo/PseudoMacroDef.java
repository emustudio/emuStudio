package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoMacroDef extends Node {
    public final String id;

    public PseudoMacroDef(int line, int column, String id) {
        super(line, column);
        this.id = Objects.requireNonNull(id);
        // parameters are the first children
        // statements are followed
    }

    public PseudoMacroDef(Token id) {
        this(id.getLine(), id.getCharPositionInLine(), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
