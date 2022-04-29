package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

public class Evaluated extends Node {
    public final int value;
    public final boolean isAddress;

    public Evaluated(int line, int column, int value, boolean isAddress) {
        super(line, column);
        this.value = value;
        this.isAddress = isAddress;
    }

    public Evaluated(int line, int column, int value) {
        this(line, column, value, false);
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(line, column, value, isAddress);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Evaluated evaluated = (Evaluated) o;
        return value == evaluated.value;
    }

    @Override
    protected String toStringShallow() {
        return "Evaluated(" + value + ")";
    }
}
