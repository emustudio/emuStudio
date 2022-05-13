/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrRegPair extends Node {
    private final static Map<Integer, Integer> opcodes = new HashMap<>();
    public final static Map<Integer, Integer> regpairs = new HashMap<>();

    public final int opcode;
    public final int regPair;

    static {
        opcodes.put(OPCODE_STAX, 2);
        opcodes.put(OPCODE_LDAX, 0xA);
        opcodes.put(OPCODE_PUSH, 0xC5);
        opcodes.put(OPCODE_POP, 0xC1);
        opcodes.put(OPCODE_DAD, 9);
        opcodes.put(OPCODE_INX, 3);
        opcodes.put(OPCODE_DCX, 0xB);

        regpairs.put(REG_B, 0);
        regpairs.put(REG_D, 1);
        regpairs.put(REG_H, 2);
        regpairs.put(REG_PSW, 3);
        regpairs.put(REG_SP, 3);
    }

    public InstrRegPair(int line, int column, int opcode, int regPair) {
        super(line, column);
        this.opcode = opcode;
        this.regPair = regPair;
    }

    public InstrRegPair(Token opcode, Token regPair) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), regPair.getType());
    }

    public byte eval() {
        int result = opcodes.get(opcode);
        int rp = regpairs.get(regPair);
        return (byte) ((result | (rp << 4)) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegPair(" + opcode + "," + regPair + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegPair(line, column, opcode, regPair);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegPair that = (InstrRegPair) o;

        if (opcode != that.opcode) return false;
        return regPair == that.regPair;
    }
}
