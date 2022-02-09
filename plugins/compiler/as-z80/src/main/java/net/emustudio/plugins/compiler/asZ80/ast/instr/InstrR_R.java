package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrR_R extends Node {
    // opcode=OPCODE_LD dst=rRegister SEP_COMMA src=rRegister

    public final int opcode;
    public final int srcReg;
    public final int dstReg;

    public InstrR_R(int line, int column, int opcode, int dst, int src) {
        super(line, column);
        this.opcode = opcode;
        this.srcReg = src;
        this.dstReg = dst;
    }

    public InstrR_R(Token opcode, Token dst, Token src) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), dst.getType(), src.getType());
    }

    public byte eval() {
        int srcRegister = InstrR.registers.get(srcReg);
        int dstRegister = InstrR.registers.get(dstReg);
        return (byte)((0x40 | (dstRegister << 3) | (srcRegister)) & 0xFF); // TODO: mov M, M == HLT
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegReg(" + opcode + ","+ dstReg +","+ srcReg +")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrR_R(line, column, opcode, dstReg, srcReg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrR_R that = (InstrR_R) o;

        if (opcode != that.opcode) return false;
        if (srcReg != that.srcReg) return false;
        return dstReg == that.dstReg;
    }
}
