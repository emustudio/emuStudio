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

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrXD extends Node {
    public final int opcode;
    public final int prefix;
    public final int x;
    public final int y;
    public final int z;

    public InstrXD(SourceCodePosition position, int opcode, int prefix, int x, int y, int z) {
        super(position);
        this.opcode = opcode;
        this.prefix = prefix;
        this.x = x;
        this.y = y;
        this.z = z;

        // 1. child is maybe expr (II+d) or N
        // 2. child is maybe N if (II+d) is defined
    }

    public InstrXD(String fileName, Token opcode, int prefix, int x, int y, int z) {
        this(positionFromToken(fileName, opcode), opcode.getType(), prefix, x, y, z);
    }

    public InstrXD(String fileName, Token opcode, int prefix, int x, int q, int p, int z) {
        this(positionFromToken(fileName, opcode), opcode.getType(), prefix, x, (p << 1) | q, z);
    }

    public byte[] eval() {
        return new byte[]{
                (byte) prefix,
                (byte) (((x << 6) | (y << 3) | (z & 7)) & 0xFF)
        };
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new InstrXD(position, opcode, prefix, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InstrXD instr = (InstrXD) o;
        return opcode == instr.opcode && prefix == instr.prefix && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "InstrXD(" + opcode + ", prefix=" + prefix + ", x=" + x + ", y=" + y + ", z=" + z + ")";
    }

}
