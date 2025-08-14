/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
