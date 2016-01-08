/*
 * Copyright (C) 2007-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.intel8080.assembler.exceptions.ValueTooBigException;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.OpCodeNode;

// this class uses only mvi instruction
public class OC_RegExpr extends OpCodeNode {
    private final byte reg;
    private final ExprNode expr;

    public OC_RegExpr(String mnemo, byte reg, ExprNode expr, int line, int column) {
        super(mnemo, line, column);
        this.reg = reg;
        this.expr = expr;
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);

        if (expr.getValue() > 0xff) {
            throw new ValueTooBigException(line, column, expr.getValue(), 0xFF);
        }
        return addr_start + 2;
    }

    // this can be only mvi instr
    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        int opCode = 6;

        if (expr.getEncValue(true).length() > 2) {
            throw new ValueTooBigException(line, column, expr.getValue(), 0xFF);
        }
        opCode |= (reg << 3);
        hex.putCode(String.format("%1$02X", opCode));
        hex.putCode(expr.getEncValue(true));
    }
}
