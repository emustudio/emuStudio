/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import net.jcip.annotations.Immutable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.ssem.CompilerChecks.checkOperandOutOfBounds;
import static net.emustudio.plugins.compiler.ssem.CompilerChecks.checkUnknownInstruction;

@Immutable
public class Instruction {
    private final static Map<Integer, Byte> OPCODES = Map.of(
            SSEMParser.JMP, (byte) 0, // 000
            SSEMParser.JPR, (byte) 4, // 100
            SSEMParser.LDN, (byte) 2, // 010
            SSEMParser.STO, (byte) 6, // 110
            SSEMParser.SUB, (byte) 1, // 001
            SSEMParser.CMP, (byte) 3, // 011
            SSEMParser.STP, (byte) 7, // 111,
            SSEMParser.NUM, (byte) 0,
            SSEMParser.BNUM, (byte) 0
    );

    public final int tokenType;
    public final long operand;

    public Instruction(int tokenType, long operand, SourceCodePosition instrPosition, Optional<SourceCodePosition> operandPosition) {
        checkUnknownInstruction(!OPCODES.containsKey(tokenType), instrPosition);
        operandPosition.ifPresent(t -> checkOperandOutOfBounds(t, tokenType, operand));
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
