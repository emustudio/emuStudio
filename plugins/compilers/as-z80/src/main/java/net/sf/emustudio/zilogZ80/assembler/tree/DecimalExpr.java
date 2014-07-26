/*
 * DecimalExpr.java
 *
 * Created on Sobota, 2007, september 29, 9:56
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

package net.sf.emustudio.zilogZ80.assembler.tree;

import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;

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
        if ((value & 0xFF) == value) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    public int eval(Namespace env, int curr_addr) throws Exception {
        return value;
    }

}
