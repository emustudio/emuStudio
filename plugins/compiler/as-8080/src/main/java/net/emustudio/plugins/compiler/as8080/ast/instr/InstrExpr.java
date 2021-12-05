package net.emustudio.plugins.compiler.as8080.ast.instr;

public class InstrExpr extends Instr {
    private final int opcode;

    public InstrExpr(int opcode) {
        this.opcode = opcode;

        // child is expr
    }
}
