package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrNN extends Node {
    // opcode=OPCODE_JP nn=rExpression
    // opcode=OPCODE_CALL nn=rExpression
    public final int opcode;

    public InstrNN(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
        // child is expr
    }

    public InstrNN(Token opcode) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrNN(line, column, opcode);
    }
}
