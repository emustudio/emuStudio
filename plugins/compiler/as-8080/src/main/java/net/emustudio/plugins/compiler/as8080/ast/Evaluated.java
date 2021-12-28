package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;

import java.util.Objects;

public class Evaluated extends Node {
    public final int address;
    public final int sizeBytes;

    public Evaluated(int line, int column, int address, int sizeBytes) {
        super(line, column);
        this.address = address;
        this.sizeBytes = sizeBytes;

        // children are sizeBytes * 1-byte ExprNumbers
    }

    public int getValue() {
        return ((ExprNumber) getChild(0)).number;
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
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, sizeBytes);
    }
}
