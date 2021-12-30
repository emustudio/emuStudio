package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;

import java.util.Objects;

public class Evaluated extends Node {
    public final int address;
    private int sizeBytes;

    public Evaluated(int line, int column, int address, int sizeBytes) {
        super(line, column);
        this.address = address;
        this.sizeBytes = sizeBytes;

        // children are sizeBytes * 1-byte ExprNumbers
    }

    public int getValue() {
        return ((ExprNumber) getChild(0)).number;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(int sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(line, column, address, sizeBytes);
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
        return address == evaluated.address && sizeBytes == evaluated.sizeBytes;
    }

    @Override
    protected String toStringShallow() {
        return "Evaluated(" +
            "addr=" + address +
            ", size=" + sizeBytes +
            ')';
    }
}
