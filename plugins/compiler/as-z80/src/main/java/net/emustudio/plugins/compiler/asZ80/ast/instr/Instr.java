package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class Instr extends Node {
    // opcode=OPCODE_NOP
    // opcode=OPCODE_RLCA
    // opcode=OPCODE_RRCA
    // opcode=OPCODE_RLA
    // opcode=OPCODE_RRA
    // opcode=OPCODE_DJNZ
    // opcode=OPCODE_DAA
    // opcode=OPCODE_CPL
    // opcode=OPCODE_SCF
    // opcode=OPCODE_CCF
    // opcode=OPCODE_HALT
    // opcode=OPCODE_RET
    // opcode=OPCODE_EXX
    // opcode=OPCODE_DI
    // opcode=OPCODE_EI

    private final static Map<Integer, Integer> opcodes = new HashMap<>();
    public final int opcode;

    static {
        opcodes.put(OPCODE_NOP, 0);
        opcodes.put(OPCODE_RLCA, 7);
        opcodes.put(OPCODE_RRCA, 0x0F);
        opcodes.put(OPCODE_RLA, 0x17);
        opcodes.put(OPCODE_RRA, 0x1F);
        opcodes.put(OPCODE_DAA, 0x27);
        opcodes.put(OPCODE_CPL, 0x2F);
        opcodes.put(OPCODE_SCF, 0x37);
        opcodes.put(OPCODE_CCF, 0x3F);
        opcodes.put(OPCODE_HALT, 0x76);
        opcodes.put(OPCODE_RET, 0xC9);
        opcodes.put(OPCODE_EXX, 0xD9);
        opcodes.put(OPCODE_DI, 0xF3);
        opcodes.put(OPCODE_EI, 0xFB);


//        opcodes.put(OPCODE_XCHG, 0xEB);
//        opcodes.put(OPCODE_XTHL, 0xE3);
//        opcodes.put(OPCODE_SPHL, 0xF9);
//        opcodes.put(OPCODE_PCHL, 0xE9);
//        opcodes.put(OPCODE_RC, 0xD8);
//        opcodes.put(OPCODE_RNC, 0xD0);
//        opcodes.put(OPCODE_RZ, 0xC8);
//        opcodes.put(OPCODE_RNZ, 0xC0);
//        opcodes.put(OPCODE_RM, 0xF8);
//        opcodes.put(OPCODE_RP, 0xF0);
//        opcodes.put(OPCODE_RPE, 0xE8);
//        opcodes.put(OPCODE_RPO, 0xE0);
    }

    public Instr(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
    }

    public Instr(Token opcode) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType());
    }

    public byte eval() {
        return (byte) (opcodes.get(opcode) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrNoArgs(" + opcode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new Instr(line, column, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instr that = (Instr) o;
        return opcode == that.opcode;
    }
}
