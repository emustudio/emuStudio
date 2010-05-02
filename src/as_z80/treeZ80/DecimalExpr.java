/*
 * DecimalExpr.java
 *
 * Created on Sobota, 2007, september 29, 9:56
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

package as_z80.treeZ80;

import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DecimalExpr extends Expression {
    
    /** Creates a new instance of DecimalExpr */
    public DecimalExpr(int value) {
        this.value = value;
    }
    
    /// compile time ///

    public int getSize() {
        if ((value & 0xFF) == value) return 1;
        else return 2;
    }

    public int eval(Namespace env, int curr_addr) throws Exception {
        return value;
    }

}
