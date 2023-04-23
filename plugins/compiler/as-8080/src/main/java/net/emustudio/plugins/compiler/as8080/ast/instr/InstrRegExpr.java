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

public class InstrRegExpr extends Node {
    public final int opcode; // MVI only
    public final int reg;

    public InstrRegExpr(SourceCodePosition position, int opcode, int reg) {
        super(position);
        this.opcode = opcode;
        this.reg = reg;
        // child is expr
    }

    public InstrRegExpr(String fileName, Token opcode, Token reg) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType(), reg.getType());
    }

    public byte eval() {
        int register = InstrReg.registers.get(reg);
        return (byte) ((6 | (register << 3)) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegExpr(" + opcode + "," + reg + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegExpr(position, opcode, reg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegExpr that = (InstrRegExpr) o;

        if (opcode != that.opcode) return false;
        return reg == that.reg;
    }
}
