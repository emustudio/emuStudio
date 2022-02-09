package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrRef_RP_RP extends Node {
    // opcode=OPCODE_EX SEP_LPAR src=REG_SP SEP_RPAR SEP_COMMA dst=REG_HL

    public final int opcode;
    public final int srcRegPair;
    public final int dstRegPair;

    public InstrRef_RP_RP(int line, int column, int opcode, int srcRegPair, int dstRegPair) {
        super(line, column);
        this.opcode = opcode;
        this.srcRegPair = srcRegPair;
        this.dstRegPair = dstRegPair;
    }

    public InstrRef_RP_RP(Token opcode, Token srcRegPair, Token dstRegPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), srcRegPair.getType(), dstRegPair.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrRef_RP_RP(line, column, opcode, srcRegPair, dstRegPair);
    }
}
