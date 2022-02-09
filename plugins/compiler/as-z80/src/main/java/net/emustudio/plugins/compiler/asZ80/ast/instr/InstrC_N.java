package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrC_N extends Node {
    // opcode=OPCODE_JR c=(COND_NZ|COND_Z|COND_NC|COND_C) SEP_COMMA n=rExpression

    public final int opcode;
    public final int cond;

    public InstrC_N(int line, int column, int opcode, int cond) {
        super(line, column);
        this.opcode = opcode;
        this.cond = cond;
        // child is expr
    }

    public InstrC_N(Token opcode, Token cond) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), cond.getType());
    }



    @Override
    protected Node mkCopy() {
        return new InstrC_N(line, column, opcode, cond);
    }
}
