package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.antlr.v4.runtime.Token;

public class InstrRef_NN_R extends Node {
    // opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_A
    // opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_HL

    public final int opcode;
    public final int reg;

    public InstrRef_NN_R(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
        // child is expr
    }

    public InstrRef_NN_R(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    @Override
    protected Node mkCopy() {
        return new InstrRef_NN_R(line, column, opcode, reg);
    }
}
