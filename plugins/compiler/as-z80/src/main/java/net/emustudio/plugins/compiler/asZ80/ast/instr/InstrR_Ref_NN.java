package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrR_Ref_NN extends Node {
    // opcode=OPCODE_LD r=REG_A SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR

    public final int opcode;
    public final int reg;

    public InstrR_Ref_NN(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
        // child is expr
    }

    public InstrR_Ref_NN(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrR_Ref_NN(line, column, opcode, reg);
    }
}
