package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegPairExpr extends Node {
    public final int opcode;
    public final int regPair;

    public InstrRegPairExpr(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
        // child is expr
    }

    public InstrRegPairExpr(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegPairExpr(" + opcode + "," + regPair +")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegPairExpr(line, column, opcode, regPair);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegPairExpr that = (InstrRegPairExpr) o;

        if (opcode != that.opcode) return false;
        return regPair == that.regPair;
    }
}
