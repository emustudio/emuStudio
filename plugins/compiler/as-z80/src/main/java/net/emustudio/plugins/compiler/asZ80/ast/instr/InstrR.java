package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class InstrR extends Node {
    // opcode=OPCODE_INC r=rRegister
    // opcode=OPCODE_DEC r=rRegister
    // opcode=OPCODE_ADD REG_A SEP_COMMA r=rRegister
    // opcode=OPCODE_ADC REG_A SEP_COMMA r=rRegister
    // opcode=OPCODE_SUB r=rRegister
    // opcode=OPCODE_SBC REG_A SEP_COMMA r=rRegister
    // opcode=OPCODE_AND r=rRegister
    // opcode=OPCODE_XOR r=rRegister
    // opcode=OPCODE_OR r=rRegister
    // opcode=OPCODE_CP r=rRegister

    private final static Map<Integer, Integer> opcodes = new HashMap<>();
    public final static Map<Integer, Integer> registers = new HashMap<>();
    public final int opcode;
    public final int reg;

    static {
        opcodes.put(OPCODE_INC, 4);
        opcodes.put(OPCODE_DEC, 5);
        opcodes.put(OPCODE_ADD, 0x80);
        opcodes.put(OPCODE_ADC, 0x88);
        opcodes.put(OPCODE_SUB, 0x90);
        opcodes.put(OPCODE_SBC, 0x98);
        opcodes.put(OPCODE_AND, 0xA0);
        opcodes.put(OPCODE_XOR, 0xA8);
        opcodes.put(OPCODE_OR, 0xB0);
        opcodes.put(OPCODE_CP, 0xB8);

        registers.put(REG_A, 7);
        registers.put(REG_B, 0);
        registers.put(REG_C, 1);
        registers.put(REG_D, 2);
        registers.put(REG_E, 3);
        registers.put(REG_H, 4);
        registers.put(REG_L, 5);
        registers.put(REG_HL, 6);
    }

    public InstrR(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
    }

    public InstrR(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    public byte eval() {
        int result = opcodes.get(opcode);
        int register = registers.get(reg);
        if (opcode == OPCODE_INC || opcode == OPCODE_DEC) {
            return (byte) ((result | (register << 3)) & 0xFF);
        }
        return (byte) ((result | register) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrReg(" + opcode + "," + reg + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrR(line, column, opcode, reg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrR instrReg = (InstrR) o;

        if (opcode != instrReg.opcode) return false;
        return reg == instrReg.reg;
    }
}
