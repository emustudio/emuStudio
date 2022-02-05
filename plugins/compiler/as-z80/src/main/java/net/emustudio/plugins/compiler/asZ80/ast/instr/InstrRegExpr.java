package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegExpr extends Node {
    public final int opcode; // MVI only
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

    public byte eval() {
        int register = InstrReg.registers.get(reg);
        return (byte) ((6 | (register << 3)) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegExpr(" + opcode + "," + reg + ")";
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
}
