package net.emustudio.plugins.compiler.as8080.ast;

public class Evaluated extends Node {
    public final int value;

    public Evaluated(int line, int column, int value) {
        super(line, column);
        this.value = value;
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(line, column, value);
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
