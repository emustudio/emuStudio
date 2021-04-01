/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.as8080.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

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
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);

        if (expr.getValue() > 0xff) {
            throw new ValueTooBigException(line, column, expr.getValue(), 0xFF);
        }
        return addr_start + 2;
    }

    // this can be only mvi instr
    @Override
    public void pass4(IntelHEX hex) throws Exception {
        int opCode = 6;

        if (expr.getEncValue(true).length() > 2) {
            throw new ValueTooBigException(line, column, expr.getValue(), 0xFF);
        }
        opCode |= (reg << 3);
        hex.putCode(String.format("%1$02X", opCode));
        hex.putCode(expr.getEncValue(true));
    }

    @Override
    public String toString() {
        return "OC_RegExpr{" +
            "reg=" + reg +
            ", expr=" + expr +
            '}';
    }
}
