package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.CompileException;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import net.jcip.annotations.Immutable;

import java.util.Map;
import java.util.Objects;

@Immutable
public class Instruction {
    private final static Map<Integer, Byte> OPCODES = Map.of(
        SSEMParser.JMP, (byte)0, // 000
        SSEMParser.SUB, (byte)1, // 001
        SSEMParser.LDN, (byte)2, // 010
        SSEMParser.CMP, (byte)3, // 011
        SSEMParser.JRP, (byte)4, // 100
        SSEMParser.STO, (byte)6, // 110
        SSEMParser.STP, (byte)7, // 111,
        SSEMParser.BNUM, (byte)0,
        SSEMParser.NUM, (byte)0
    );

    public final int tokenType;
    public final int operand;

    public Instruction(int tokenType) {
        if (!OPCODES.containsKey(tokenType)) {
            throw new CompileException("Unknown instruction");
        }
        this.tokenType = tokenType;
        this.operand = 0;
    }

    public Instruction(int tokenType, int operand) {
        if (!OPCODES.containsKey(tokenType)) {
            throw new CompileException("Unknown instruction");
        }
        this.tokenType = tokenType;
        this.operand = operand;
    }

    public int getOpcode() {
        return OPCODES.get(tokenType);
    }

    public String toString() {
        return String.format("%02d %02d", getOpcode(), operand);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instruction that = (Instruction) o;
        return tokenType == that.tokenType && operand == that.operand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, operand);
    }
}
