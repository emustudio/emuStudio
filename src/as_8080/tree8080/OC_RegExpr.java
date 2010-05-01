/*
 * OC_RegExpr.java
 *
 * Created on Sobota, 2007, september 29, 20:51
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

import as_8080.impl.HEXFileHandler;
import as_8080.impl.compileEnv;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.OpCodeNode;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
// this class uses only mvi instruction
public class OC_RegExpr extends OpCodeNode {
    private byte reg;
    private ExprNode expr;
    
    /** Creates a new instance of OC_RegExpr */
    public OC_RegExpr(String mnemo, byte reg, ExprNode expr, 
            int line, int column) {
        super(mnemo, line, column);
        this.reg = reg;
        this.expr = expr;
    }
    
    /// compile time ///

    public int getSize() { return 2; }
    public void pass1(IMessageReporter r) {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        
        if (expr.getValue() > 0xff)
            throw new Exception("["+line+","+column+"] Expression is too big (maximum is 0FFh)");
        return addr_start + 2;
    }

    // this can be only mvi instr
    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 6;
        
        if (expr.getEncValue(true).length() > 2)
            throw new Exception("["+line+","+column+"] Expression is too big (maximum is 0FFh)");
        opCode |= (reg << 3);
        hex.putCode(String.format("%1$02X",opCode));
        hex.putCode(expr.getEncValue(true));
    }
    
}
