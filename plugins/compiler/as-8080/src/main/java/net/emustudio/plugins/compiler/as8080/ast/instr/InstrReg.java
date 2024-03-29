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
package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrReg extends Node {
    public final static Map<Integer, Integer> registers = new HashMap<>();
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

    static {
        opcodes.put(OPCODE_INR, 4);
        opcodes.put(OPCODE_DCR, 5);
        opcodes.put(OPCODE_ADD, 0x80);
        opcodes.put(OPCODE_ADC, 0x88);
        opcodes.put(OPCODE_SUB, 0x90);
        opcodes.put(OPCODE_SBB, 0x98);
        opcodes.put(OPCODE_ANA, 0xA0);
        opcodes.put(OPCODE_XRA, 0xA8);
        opcodes.put(OPCODE_ORA, 0xB0);
        opcodes.put(OPCODE_CMP, 0xB8);

        registers.put(REG_A, 7);
        registers.put(REG_B, 0);
        registers.put(REG_C, 1);
        registers.put(REG_D, 2);
        registers.put(REG_E, 3);
        registers.put(REG_H, 4);
        registers.put(REG_L, 5);
        registers.put(REG_M, 6);
    }

    public final int opcode;
    public final int reg;

    public InstrReg(SourceCodePosition position, int opcode, int reg) {
        super(position);
        this.opcode = opcode;
        this.reg = reg;
    }

    public InstrReg(String fileName, Token opcode, Token reg) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType(), reg.getType());
    }

    public byte eval() {
        int result = opcodes.get(opcode);
        int register = registers.get(reg);
        if (opcode == OPCODE_INR || opcode == OPCODE_DCR) {
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
        return new InstrReg(position, opcode, reg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrReg instrReg = (InstrReg) o;

        if (opcode != instrReg.opcode) return false;
        return reg == instrReg.reg;
    }
}
