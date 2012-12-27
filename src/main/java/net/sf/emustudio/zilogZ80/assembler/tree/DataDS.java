/*
 * DataDS.java
 *
 * Created on Streda, 2008, august 13, 11:47
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.plugins.compiler.HEXFileHandler;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.impl.NeedMorePassException;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.DataValue;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;

public class DataDS extends DataValue {

    private Expression expression = null;

    public DataDS(Expression expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }

    @Override
    public int getSize() {
        return expression.getValue();
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        try {
            int val = expression.eval(env, addr_start);
            return addr_start + val;
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] Error: DS expression can't be ambiguous");
        }
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        String str = "";
        if (!expression.is8Bit()) {
            throw new Exception("[" + line + "," + column + "] Error:"
                    + " value too large");
        }
        if (expression.getValue() < 0) {
            throw new Exception("[" + line + "," + column + "] Error:"
                    + " value can't be negative");
        }

        for (int i = 0; i < expression.getValue(); i++) {
            str += "00";
        }
        hex.putCode(str);
    }
}
