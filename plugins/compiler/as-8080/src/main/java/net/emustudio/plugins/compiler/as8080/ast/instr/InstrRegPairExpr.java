/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrRegPairExpr extends Node {
    public final int opcode;
    public final int regPair;

    public InstrRegPairExpr(SourceCodePosition position, int opcode, int regPair) {
        super(position);
        this.opcode = opcode;
        this.regPair = regPair;
        // child is expr
    }

    public InstrRegPairExpr(String fileName, Token opcode, Token regPair) {
        this(new SourceCodePosition(opcode.getLine(), opcode.getCharPositionInLine(), fileName), opcode.getType(), regPair.getType());
    }

    public byte eval() {
        int rp = InstrRegPair.regpairs.get(regPair);
        return (byte) ((1 | (rp << 4)) & 0xFF);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrRegPairExpr(" + opcode + "," + regPair + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrRegPairExpr(position, opcode, regPair);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrRegPairExpr that = (InstrRegPairExpr) o;

        if (opcode != that.opcode) return false;
        return regPair == that.regPair;
    }
}
