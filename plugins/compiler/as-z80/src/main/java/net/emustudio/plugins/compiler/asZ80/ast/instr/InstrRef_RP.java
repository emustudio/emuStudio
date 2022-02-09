package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrRef_RP extends Node {
    // opcode=OPCODE_LD SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR SEP_COMMA REG_A
    // opcode=OPCODE_JP SEP_LPAR REG_HL SEP_RPAR


    public final int opcode;
    public final int regPair;

    public InstrRef_RP(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
    }

    public InstrRef_RP(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrRef_RP(line, column, opcode, regPair);
    }
}
