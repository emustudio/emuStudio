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

public class InstrNoArgs extends Node {
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

    static {
        opcodes.put(OPCODE_STC, 0x37);
        opcodes.put(OPCODE_CMC, 0x3F);
        opcodes.put(OPCODE_CMA, 0x2F);
        opcodes.put(OPCODE_DAA, 0x27);
        opcodes.put(OPCODE_NOP, 0);
        opcodes.put(OPCODE_RLC, 7);
        opcodes.put(OPCODE_RRC, 0xF);
        opcodes.put(OPCODE_RAL, 0x17);
        opcodes.put(OPCODE_RAR, 0x1F);
        opcodes.put(OPCODE_XCHG, 0xEB);
        opcodes.put(OPCODE_XTHL, 0xE3);
        opcodes.put(OPCODE_SPHL, 0xF9);
        opcodes.put(OPCODE_PCHL, 0xE9);
        opcodes.put(OPCODE_RET, 0xC9);
        opcodes.put(OPCODE_RC, 0xD8);
        opcodes.put(OPCODE_RNC, 0xD0);
        opcodes.put(OPCODE_RZ, 0xC8);
        opcodes.put(OPCODE_RNZ, 0xC0);
        opcodes.put(OPCODE_RM, 0xF8);
        opcodes.put(OPCODE_RP, 0xF0);
        opcodes.put(OPCODE_RPE, 0xE8);
        opcodes.put(OPCODE_RPO, 0xE0);
        opcodes.put(OPCODE_EI, 0xFB);
        opcodes.put(OPCODE_DI, 0xF3);
        opcodes.put(OPCODE_HLT, 0x76);
    }

    public final int opcode;

    public InstrNoArgs(SourceCodePosition position, int opcode) {
        super(position);
        this.opcode = opcode;
    }

    public InstrNoArgs(String fileName, Token opcode) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType());
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
        return new InstrNoArgs(position, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrNoArgs that = (InstrNoArgs) o;
        return opcode == that.opcode;
    }
}
