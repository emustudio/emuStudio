package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrRP_Ref_NN extends Node {
    // opcode=OPCODE_LD rp=REG_HL SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR

    public final int opcode;
    public final int regPair;

    public InstrRP_Ref_NN(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
        // child is expr
    }

    public InstrRP_Ref_NN(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrRP_Ref_NN(line, column, opcode, regPair);
    }
}
