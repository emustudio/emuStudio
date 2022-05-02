package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoVar extends Node {
    public final String id;

    public PseudoVar(int line, int column, String id) {
        super(line, column);
        this.id = Objects.requireNonNull(id);
        // expr is the only child
    }

    public PseudoVar(Token id) {
        this(id.getLine(), id.getCharPositionInLine(), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoVar(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoVar(line, column, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoVar pseudoVar = (PseudoVar) o;
        return Objects.equals(id, pseudoVar.id);
    }
}
