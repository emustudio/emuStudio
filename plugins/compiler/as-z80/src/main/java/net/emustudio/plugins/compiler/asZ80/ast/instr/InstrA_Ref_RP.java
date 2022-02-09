package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrA_Ref_RP extends Node {
    // opcode=OPCODE_LD REG_A SEP_COMMA SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR

    public final int opcode;
    public final int regPair;

    public InstrA_Ref_RP(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
    }

    public InstrA_Ref_RP(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrA_Ref_RP(line, column, opcode, regPair);
    }
}
