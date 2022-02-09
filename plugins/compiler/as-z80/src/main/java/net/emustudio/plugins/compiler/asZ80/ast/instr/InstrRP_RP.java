package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrRP_RP extends Node {
    // opcode=OPCODE_LD dst=REG_SP SEP_COMMA src=REG_HL
    // opcode=OPCODE_EX dst=REG_AF SEP_COMMA src=REG_AFF
    // opcode=OPCODE_EX dst=REG_DE SEP_COMMA src=REG_HL

    public final int opcode;
    public final int srcRegPair;
    public final int dstRegPair;

    public InstrRP_RP(int line, int column, int opcode, int srcRegPair, int dstRegPair) {
        super(line, column);
        this.opcode = opcode;
        this.srcRegPair = srcRegPair;
        this.dstRegPair = dstRegPair;
    }

    public InstrRP_RP(Token opcode, Token srcRegPair, Token dstRegPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), srcRegPair.getType(), dstRegPair.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrRP_RP(line, column, opcode, srcRegPair, dstRegPair);
    }
}
