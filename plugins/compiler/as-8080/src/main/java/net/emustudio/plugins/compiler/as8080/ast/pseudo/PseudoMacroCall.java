package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoMacroCall extends Node {
    public final String id;

    public PseudoMacroCall(int line, int column, String id) {
        super(line, column);
        this.id = Objects.requireNonNull(id);
        // children are exprs (arguments)
    }

    public PseudoMacroCall(Token id) {
        this(id.getLine(), id.getCharPositionInLine(), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoMacroCall(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroCall(line, column, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoMacroCall that = (PseudoMacroCall) o;
        return Objects.equals(id, that.id);
    }
}
