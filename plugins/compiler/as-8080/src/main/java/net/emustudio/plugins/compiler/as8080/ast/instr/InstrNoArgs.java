package net.emustudio.plugins.compiler.as8080.ast.instr;

import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class InstrNoArgs extends Instr {
    private final int opcode;

    public InstrNoArgs(int opcode) {
        this.opcode = opcode;
    }


}
