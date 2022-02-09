package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrC extends Node {
    // opcode=OPCODE_RET c=cCondition

    public final int opcode;
    public final int cond;

    public InstrC(int line, int column, int opcode, int cond) {
        super(line, column);
        this.opcode = opcode;
        this.cond = cond;
    }

    public InstrC(Token opcode, Token cond) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), cond.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrC(line, column, opcode, cond);
    }
}
