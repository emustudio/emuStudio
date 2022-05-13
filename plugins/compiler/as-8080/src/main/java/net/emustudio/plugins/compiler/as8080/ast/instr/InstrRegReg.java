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

public class InstrRegReg extends Node {
    public final int opcode; // MOV only
    public final int srcReg;
    public final int dstReg;

    public InstrRegReg(int line, int column, int opcode, int dst, int src) {
        super(line, column);
        this.opcode = opcode;
        this.srcReg = src;
        this.dstReg = dst;
    }

    public InstrRegReg(Token opcode, Token dst, Token src) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), dst.getType(), src.getType());
    }

    public byte eval() {
        int srcRegister = InstrReg.registers.get(srcReg);
        int dstRegister = InstrReg.registers.get(dstReg);
        return (byte)((0x40 | (dstRegister << 3) | (srcRegister)) & 0xFF); // TODO: mov M, M == HLT
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegReg(" + opcode + ","+ dstReg +","+ srcReg +")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegReg(line, column, opcode, dstReg, srcReg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegReg that = (InstrRegReg) o;

        if (opcode != that.opcode) return false;
        if (srcReg != that.srcReg) return false;
        return dstReg == that.dstReg;
    }
}
