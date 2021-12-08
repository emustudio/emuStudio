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
}
