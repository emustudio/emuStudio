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
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.*;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrExpr extends Node {
    private final static Set<Integer> twoBytes = new HashSet<>();
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

    static {
        twoBytes.add(OPCODE_ADI);
        twoBytes.add(OPCODE_ACI);
        twoBytes.add(OPCODE_SUI);
        twoBytes.add(OPCODE_SBI);
        twoBytes.add(OPCODE_ANI);
        twoBytes.add(OPCODE_ORI);
        twoBytes.add(OPCODE_XRI);
        twoBytes.add(OPCODE_CPI);
        twoBytes.add(OPCODE_IN);
        twoBytes.add(OPCODE_OUT);

        opcodes.put(OPCODE_LDA, 0x3A);
        opcodes.put(OPCODE_STA, 0x32);
        opcodes.put(OPCODE_LHLD, 0x2A);
        opcodes.put(OPCODE_SHLD, 0x22);
        opcodes.put(OPCODE_ADI, 0xC6);
        opcodes.put(OPCODE_ACI, 0xCE);
        opcodes.put(OPCODE_SUI, 0xD6);
        opcodes.put(OPCODE_SBI, 0xDE);
        opcodes.put(OPCODE_ANI, 0xE6);
        opcodes.put(OPCODE_ORI, 0xF6);
        opcodes.put(OPCODE_XRI, 0xEE);
        opcodes.put(OPCODE_CPI, 0xFE);
        opcodes.put(OPCODE_JMP, 0xC3);
        opcodes.put(OPCODE_JC, 0xDA);
        opcodes.put(OPCODE_JNC, 0xD2);
        opcodes.put(OPCODE_JZ, 0xCA);
        opcodes.put(OPCODE_JNZ, 0xC2);
        opcodes.put(OPCODE_JM, 0xFA);
        opcodes.put(OPCODE_JP, 0xF2);
        opcodes.put(OPCODE_JPE, 0xEA);
        opcodes.put(OPCODE_JPO, 0xE2);
        opcodes.put(OPCODE_CALL, 0xCD);
        opcodes.put(OPCODE_CC, 0xDC);
        opcodes.put(OPCODE_CZ, 0xCC);
        opcodes.put(OPCODE_CNC, 0xD4);
        opcodes.put(OPCODE_CNZ, 0xC4);
        opcodes.put(OPCODE_CM, 0xFC);
        opcodes.put(OPCODE_CP, 0xF4);
        opcodes.put(OPCODE_CPE, 0xEC);
        opcodes.put(OPCODE_CPO, 0xE4);
        opcodes.put(OPCODE_IN, 0xDB);
        opcodes.put(OPCODE_OUT, 0xD3);
        opcodes.put(OPCODE_RST, 0xC7);
    }

    public final int opcode;

    public InstrExpr(SourceCodePosition position, int opcode) {
        super(position);
        this.opcode = opcode;
        // child is expr
    }

    public InstrExpr(String fileName, Token opcode) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType());
    }

    public int getExprSizeBytes() {
        if (opcode == OPCODE_RST) {
            return 0;
        } else if (twoBytes.contains(opcode)) {
            return 1;
        }
        return 2; // address
    }

    public Optional<Byte> eval() {
        byte result = (byte) (opcodes.get(opcode) & 0xFF);
        if (opcode == OPCODE_RST) {
            return collectChild(Evaluated.class)
                    .filter(e -> e.value >= 0 && e.value <= 7)
                    .map(e -> (byte) (result | (e.value << 3)));
        }

        return Optional.of(result);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrExpr(" + opcode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrExpr(position, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrExpr instrExpr = (InstrExpr) o;
        return opcode == instrExpr.opcode;
    }
}
