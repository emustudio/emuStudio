/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.exceptions.NeedMorePassException;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoBlock;

public class IfPseudoNode extends PseudoBlock {
    private ExprNode expr;
    private Statement stat;
    private boolean condTrue; // for pass4

    public IfPseudoNode(ExprNode expr, Statement stat, int line, int column) {
        super(line, column);
        this.expr = expr;
        this.stat = stat;
        this.condTrue = false;
    }

    // if doesnt have and id
    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getSize() {
        if (expr.getValue() != 0) {
            return stat.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public void pass1() throws Exception {
    }

    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        stat.pass1(env);

        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return stat.pass2(env, addr_start);
            } else {
                return addr_start;
            }
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "IF expression");
        }
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        if (condTrue) {
            stat.pass4(hex);
        }
    }
}
