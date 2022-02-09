package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrC_NN extends Node {
    // opcode=OPCODE_JP c=cCondition SEP_COMMA nn=rExpression
    // opcode=OPCODE_CALL c=cCondition SEP_COMMA nn=rExpression

    public final int opcode;
    public final int cond;

    public InstrC_NN(int line, int column, int opcode, int cond) {
        super(line, column);
        this.opcode = opcode;
        this.cond = cond;
        // child is expr
    }

    public InstrC_NN(Token opcode, Token cond) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), cond.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrC_NN(line, column, opcode, cond);
    }
}
