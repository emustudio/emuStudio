/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
