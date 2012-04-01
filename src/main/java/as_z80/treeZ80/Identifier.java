/*
 * Identifier.java
 *
 * Created on Streda, 2007, október 10, 15:50
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

/**
 *
 * @author vbmacher
 */
public class Identifier extends Expression {
    private String name;
    private int line;
    private int col;
    
    /** Creates a new instance of Identifier */
    public Identifier(String name, int line, int col) {
        this.name = name;
        this.line = line;
        this.col = col;
    }

    /// compile time ///
    public int getSize() { return 0; }

    public int eval(Namespace env, int curr_addr) throws Exception {
        // identifier in expression can be only label, equ, or set statement. macro NOT
        // search in env for labels
        Label lab = env.getLabel(this.name);
        if ((lab != null) && (lab.getAddress() == null))
            throw new NeedMorePassException(this, lab.getLine(), lab.getColumn());
        else if (lab != null) {
            this.value = lab.getAddress();
            return this.value;
        }

        PseudoEQU equ = env.getEqu(this.name);
        if (equ != null) {
            this.value = equ.getValue();
            return this.value;
        }
        
        PseudoVAR set = env.getVar(this.name);
        if (set != null) {
            this.value = set.getValue();
            return this.value;
        }
        else
            throw new Exception("[" + line + "," + col +
                    "] Error: Unknown identifier (" + this.name + ")");
    }
    
}
