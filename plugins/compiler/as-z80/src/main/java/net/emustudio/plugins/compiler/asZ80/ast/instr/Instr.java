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
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class Instr extends Node {
    public final int opcode;
    public final int x;
    public final int z;
    private int y;

    public Instr(int line, int column, int opcode, int x, int y, int z) {
        super(line, column);
        this.opcode = opcode;
        this.x = x;
        this.y = y;
        this.z = z;

        // children might be expr in the same order as when compiled
    }

    public Instr(Token opcode, int x, int y, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), x, y, z);
    }

    public Instr(Token opcode, int x, int q, int p, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), x, (p << 1) | q, z);
    }

    public void setY(int y) {
        // for RST
        this.y = y;
    }

    public byte eval() {
        return (byte) (((x << 6) | (y << 3) | (z & 7)) & 0xFF);
    }

    public boolean hasRelativeAddress() {
        // DJNZ, JR, JR cc
        return (x == 0 && z == 0 && y >= 2 && y <= 7);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new Instr(line, column, opcode, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Instr instr = (Instr) o;
        return opcode == instr.opcode && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "Instr(" + opcode + ",  x=" + x + ", y=" + y + ", z=" + z + ")";
    }
}
