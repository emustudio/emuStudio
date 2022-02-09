package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRP_NN extends Node {
    // opcode=OPCODE_LD rp=rRegPair SEP_COMMA nn=rExpression

    public final int opcode;
    public final int regPair;

    public InstrRP_NN(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
        // child is expr
    }

    public InstrRP_NN(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    public byte eval() {
        int rp = InstrRP.regpairs.get(regPair);
        return (byte) ((1 | (rp << 4)) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegPairExpr(" + opcode + "," + regPair + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRP_NN(line, column, opcode, regPair);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRP_NN that = (InstrRP_NN) o;

        if (opcode != that.opcode) return false;
        return regPair == that.regPair;
    }
}
