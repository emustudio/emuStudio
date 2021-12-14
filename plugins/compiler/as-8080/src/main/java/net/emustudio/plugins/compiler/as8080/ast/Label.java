package net.emustudio.plugins.compiler.as8080.ast;

import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.parseLabel;

public class Label extends Node {
    public final String label;

    public Label(int line, int column, String label) {
        super(line, column);
        this.label = Objects.requireNonNull(label);
    }

    public Label(Token label) {
        this(label.getLine(), label.getCharPositionInLine(), parseLabel(label));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "Label(" + label +")";
    }

    @Override
    protected Node mkCopy() {
        return new Label(line, column, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Label label1 = (Label) o;
        return Objects.equals(label, label1.label);
    }

    @Override
    public int hashCode() {
        return label != null ? label.hashCode() : 0;
    }
}
