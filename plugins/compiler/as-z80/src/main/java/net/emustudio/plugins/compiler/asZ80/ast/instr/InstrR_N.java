package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrR_N extends Node {
    // opcode=OPCODE_LD r=rRegister SEP_COMMA n=rExpression

    public final int opcode;
    public final int reg;

    public InstrR_N(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
        // child is expr
    }

    public InstrR_N(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    public byte eval() {
        int register = InstrR.registers.get(reg);
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
        return new InstrR_N(line, column, opcode, reg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrR_N that = (InstrR_N) o;

        if (opcode != that.opcode) return false;
        return reg == that.reg;
    }
}
