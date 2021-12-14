package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrExpr extends Node {
    public final int opcode;

    public InstrExpr(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
        // child is expr
    }

    public InstrExpr(Token opcode) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrExpr(" + opcode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrExpr(line, column, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrExpr instrExpr = (InstrExpr) o;
        return opcode == instrExpr.opcode;
    }

    @Override
    public int hashCode() {
        return opcode;
    }
}
