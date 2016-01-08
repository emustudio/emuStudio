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
import net.sf.emustudio.intel8080.assembler.exceptions.AmbiguousException;
import net.sf.emustudio.intel8080.assembler.exceptions.NegativeValueException;
import net.sf.emustudio.intel8080.assembler.exceptions.ValueTooBigException;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.exceptions.NeedMorePassException;
import net.sf.emustudio.intel8080.assembler.treeAbstract.DataValueNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;

public class DSDataNode extends DataValueNode {

    private ExprNode expression = null;

    public DSDataNode(ExprNode expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }

    @Override
    public int getSize() {
        return expression.getValue();
    }

    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        try {
            int val = expression.eval(env, addr_start);
            return addr_start + val;
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "DS expression");
        }
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        String str = "";

        if (expression.getValue() > 0xFF) {
            throw new ValueTooBigException(line, column, expression.getValue(), 0xFF);
        } else if (expression.getValue() < 0) {
            throw new NegativeValueException(line, column, expression.getValue());
        }

        for (int i = 0; i < expression.getValue(); i++) {
            str += "00";
        }
        hex.putCode(str);
    }
}
