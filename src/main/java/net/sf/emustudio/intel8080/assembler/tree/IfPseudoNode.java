/*
 * IfPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 13:39
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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

import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoBlock;
import emulib.plugins.compiler.HEXFileHandler;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.impl.NeedMorePassException;

/**
 *
 * @author vbmacher
 */
public class IfPseudoNode extends PseudoBlock {

    private ExprNode expr;
    private Statement stat;
    private boolean condTrue; // for pass4

    /** Creates a new instance of IfPseudoNode */
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

    /// compile time ///
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
        stat.pass1();
    }

    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return stat.pass2(env, addr_start);
            } else {
                return addr_start;
            }
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] IF expression can't be ambiguous");
        }
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        if (condTrue) {
            stat.pass4(hex);
        }
    }
}
