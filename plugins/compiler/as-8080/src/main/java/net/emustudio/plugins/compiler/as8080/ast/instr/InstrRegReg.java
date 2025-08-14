/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegReg extends Node {
    public final int opcode; // MOV only
    public final int srcReg;
    public final int dstReg;

    public InstrRegReg(SourceCodePosition position, int opcode, int dst, int src) {
        super(position);
        this.opcode = opcode;
        this.srcReg = src;
        this.dstReg = dst;
    }

    public InstrRegReg(String fileName, Token opcode, Token dst, Token src) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType(), dst.getType(), src.getType());
    }

    public byte eval() {
        int srcRegister = InstrReg.registers.get(srcReg);
        int dstRegister = InstrReg.registers.get(dstReg);
        return (byte) ((0x40 | (dstRegister << 3) | (srcRegister)) & 0xFF); // TODO: mov M, M == HLT
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegReg(" + opcode + "," + dstReg + "," + srcReg + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegReg(position, opcode, dstReg, srcReg);
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
