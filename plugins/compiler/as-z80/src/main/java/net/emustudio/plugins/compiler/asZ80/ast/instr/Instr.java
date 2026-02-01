/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class Instr extends Node {
    public final int opcode;
    public final int x;
    public final int z;
    private int y;

    public Instr(SourceCodePosition position, int opcode, int x, int y, int z) {
        super(position);
        this.opcode = opcode;
        this.x = x;
        this.y = y;
        this.z = z;

        // children might be expr in the same order as when compiled
    }

    public Instr(String fileName, Token opcode, int x, int y, int z) {
        this(positionFromToken(fileName, opcode), opcode.getType(), x, y, z);
    }

    public Instr(String fileName, Token opcode, int x, int q, int p, int z) {
        this(positionFromToken(fileName, opcode), opcode.getType(), x, (p << 1) | q, z);
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
        return new Instr(position, opcode, x, y, z);
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
