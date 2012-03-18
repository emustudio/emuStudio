/*
 * OC_RegpairExpr.java
 *
 * Created on Sobota, 2007, september 29, 20:47
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2012 Peter Jakubčo <pjakubco at gmail.com>
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

import as_8080.impl.CompileEnv;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.OpCodeNode;
import emulib.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
// this class uses only lxi instruction
public class OC_RegpairExpr extends OpCodeNode {

    private byte regpair;
    private ExprNode expr;

    /** Creates a new instance of OC_RegpairExpr */
    public OC_RegpairExpr(String mnemo, byte regpair, ExprNode expr, int line,
            int column) {
        super(mnemo, line, column);
        this.regpair = regpair;
        this.expr = expr;
    }

    /// compile time ///
    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return addr_start + 3;
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 1 | (regpair << 4);
        hex.putCode(String.format("%1$02X", opCode));
        hex.putCode(expr.getEncValue(false));
    }
}
