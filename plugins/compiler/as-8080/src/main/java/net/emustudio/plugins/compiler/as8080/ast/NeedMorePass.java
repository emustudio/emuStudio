package net.emustudio.plugins.compiler.as8080.ast;

public class NeedMorePass extends Node {

    public NeedMorePass(int line, int column) {
        super(line, column);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new NeedMorePass(line, column);
    }
}
