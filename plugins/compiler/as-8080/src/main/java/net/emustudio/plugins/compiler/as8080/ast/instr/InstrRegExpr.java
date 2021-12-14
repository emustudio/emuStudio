package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegExpr extends Node {
    public final int opcode;
    public final int reg;

    public InstrRegExpr(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
        // child is expr
    }

    public InstrRegExpr(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegExpr(" + opcode + "," + reg +")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegExpr(line, column, opcode, reg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegExpr that = (InstrRegExpr) o;

        if (opcode != that.opcode) return false;
        return reg == that.reg;
    }

    @Override
    public int hashCode() {
        int result = opcode;
        result = 31 * result + reg;
        return result;
    }
}
