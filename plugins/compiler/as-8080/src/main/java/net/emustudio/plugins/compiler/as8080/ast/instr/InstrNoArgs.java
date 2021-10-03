package net.emustudio.plugins.compiler.as8080.ast.instr;

import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class InstrNoArgs extends Instr {
    private final Token opcode;

    public InstrNoArgs(Token opcode) {
        this.opcode = Objects.requireNonNull(opcode);
    }


}
