package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrNoArgs extends Node {
    public final int opcode;

    public InstrNoArgs(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
    }

    public InstrNoArgs(Token opcode) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrNoArgs(" + opcode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrNoArgs(line, column, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrNoArgs that = (InstrNoArgs) o;
        return opcode == that.opcode;
    }
}
