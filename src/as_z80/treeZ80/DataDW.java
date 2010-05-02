/*
 * DataDW.java
 *
 * Created on Streda, 2008, august 13, 11:48
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import as_z80.impl.HEXFileHandler;
import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.DataValue;
import as_z80.treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DataDW extends DataValue{
    private Expression expression = null;
    
    /** Creates a new instance of DataDW */
    public DataDW(Expression expr, int line, int column) {
        super(line,column);
        this.expression = expr;
    }
    
    /// compile time ///
    public int getSize() { return 2; }
    
    public void pass1() {}

    public int pass2(Namespace env, int addr_start) throws Exception {
        expression.eval(env, addr_start);
        return addr_start + 2;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        hex.putCode(expression.encodeValue(2));
    }

}
