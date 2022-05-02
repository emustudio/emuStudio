package net.emustudio.plugins.compiler.asZ80.ast.data;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

public class DataDW extends Node {

    public DataDW(int line, int column) {
        super(line, column);
        // child is expr 2 byte
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new DataDW(line, column);
    }
}
