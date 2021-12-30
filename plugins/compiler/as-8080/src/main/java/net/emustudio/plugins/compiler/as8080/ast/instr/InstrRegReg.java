package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegReg extends Node {
    public final int opcode;
    public final int srcReg;
    public final int dstReg;

    public InstrRegReg(int line, int column, int opcode, int dst, int src) {
        super(line, column);
        this.opcode = opcode;
        this.srcReg = src;
        this.dstReg = dst;
    }

    public InstrRegReg(Token opcode, Token dst, Token src) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), dst.getType(), src.getType());
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
        return new InstrRegReg(line, column, opcode, dstReg, srcReg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegReg that = (InstrRegReg) o;

        if (opcode != that.opcode) return false;
        if (srcReg != that.srcReg) return false;
        return dstReg == that.dstReg;
    }
}
