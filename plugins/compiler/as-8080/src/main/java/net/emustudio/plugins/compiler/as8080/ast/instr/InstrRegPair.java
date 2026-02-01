/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrRegPair extends Node {
    public final static Map<Integer, Integer> regpairs = new HashMap<>();
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

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

    public final int opcode;
    public final int regPair;

    public InstrRegPair(SourceCodePosition position, int opcode, int regPair) {
        super(position);
        this.opcode = opcode;
        this.regPair = regPair;
    }

    public InstrRegPair(String fileName, Token opcode, Token regPair) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType(), regPair.getType());
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
        return new InstrRegPair(position, opcode, regPair);
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
