/*
 * PseudoIF.java
 *
 * Created on Sobota, 2007, september 29, 13:39
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * DON'T REPEAT YOURSELF
 * 
 * Copyright (C) 2007-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package as_z80.treeZ80;

import as_z80.impl.NeedMorePassException;
import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.Expression;
import as_z80.treeZ80Abstract.Pseudo;
import emulib.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class PseudoIF extends Pseudo {

    private Expression expr;
    private Program subprogram;
    private boolean condTrue; // => for pass4; if this is true,
    // then generate code, otherwise not.

    /** Creates a new instance of PseudoIF */
    public PseudoIF(Expression expr, Program stat, int line, int column) {
        super(line, column);
        this.expr = expr;
        this.subprogram = stat;
        this.condTrue = false;
    }

    /// compile time ///
    @Override
    public int getSize() {
        if (condTrue) {
            return subprogram.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public void pass1() throws Exception {
        subprogram.pass1();
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return subprogram.pass2(env, addr_start);
            } else {
                return addr_start;
            }
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] Error: IF expression can't be ambiguous");
        }
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        if (condTrue) {
            subprogram.pass4(hex);
        }
    }
}
