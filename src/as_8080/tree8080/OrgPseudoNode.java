/*
 * OrgPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 10:32
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package as_8080.tree8080;

import as_8080.impl.NeedMorePassException;
import as_8080.impl.CompileEnv;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.PseudoNode;
import emuLib8.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class OrgPseudoNode extends PseudoNode {

    private ExprNode expr;

    /** Creates a new instance of OrgPseudoNode */
    public OrgPseudoNode(ExprNode expr, int line, int column) {
        super(line, column);
        this.expr = expr;
    }

    /// compile time ///
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String getName() {
        return "";
    }

    // org only changes current address
    // if expr isnt valuable, then error exception is thrown
    // it cant help even more passes, because its recursive:
    // org label
    // mvi a,50
    // label: hlt
    // label address cant be evaluated
    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        int val = addr_start;
        try {
            val = expr.eval(parentEnv, addr_start);
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] ORG expression can't be ambiguous");
        }
        return val;
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        hex.setNextAddress(expr.getValue());
    }
}
