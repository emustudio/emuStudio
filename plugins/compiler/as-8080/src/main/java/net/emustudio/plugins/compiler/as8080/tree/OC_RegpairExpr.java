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
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

// this class uses only lxi instruction
public class OC_RegpairExpr extends OpCodeNode {
    private final byte regpair;
    private final ExprNode expr;

    public OC_RegpairExpr(String mnemo, byte regpair, ExprNode expr, int line, int column) {
        super(mnemo, line, column);
        this.regpair = regpair;
        this.expr = expr;
    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return addr_start + 3;
    }

    @Override
    public void pass4(IntelHEX hex) {
        int opCode = 1 | (regpair << 4);
        hex.putCode(String.format("%1$02X", opCode));
        hex.putCode(expr.getEncValue(false));
    }
}
