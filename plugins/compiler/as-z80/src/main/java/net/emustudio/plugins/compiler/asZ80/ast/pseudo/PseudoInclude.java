package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.plugins.compiler.asZ80.ParsingUtils;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoInclude extends Node {
    public final String filename;

    public PseudoInclude(int line, int column, String fileName) {
        super(line, column);
        this.filename = Objects.requireNonNull(fileName);
    }

    public PseudoInclude(Token fileName) {
        this(fileName.getLine(), fileName.getCharPositionInLine(), ParsingUtils.parseLitString(fileName));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoInclude('" + filename + "')";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoInclude(line, column, filename);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoInclude that = (PseudoInclude) o;
        return Objects.equals(filename, that.filename);
    }
}