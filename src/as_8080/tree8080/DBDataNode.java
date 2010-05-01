/*
 * DBData.java
 *
 * Created on Sobota, 2007, september 22, 9:13
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
import as_8080.tree8080Abstract.DataValueNode;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.OpCodeNode;

/**
 *
 * @author vbmacher
 */
public class DBDataNode extends DataValueNode {
    private ExprNode expression = null;
    private String literalString = null;
    private OpCodeNode opcode = null;
    
    /** Creates a new instance of DBData */
    public DBDataNode(ExprNode expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }
    
    public DBDataNode(String literalString, int line, int column) {
        super(line,column);
        this.literalString = literalString;
    }
    
    public DBDataNode(OpCodeNode opcode, int line, int column) {
        super(line,column);
        this.opcode = opcode;
    }
    
    /// compile time ///
    public int getSize() { 
        if (expression != null) return 1;
        else if (literalString != null) return literalString.length();
        else if (opcode != null) return opcode.getSize();
        return 0;
    }
    
    public void pass1() throws Exception {
        if (opcode != null) opcode.pass1(null);
    }
    
    public int pass2(compileEnv env, int addr_start) throws Exception {
        if (expression != null) {
            expression.eval(env,addr_start);
            return addr_start + 1;
        }
        else if (literalString != null) return addr_start + literalString.length();
        else if (opcode != null) return opcode.pass2(env,addr_start);
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        if (expression != null) {
            if (expression.getEncValue(true).length() > 2)
                throw new Exception("[" + line + "," + column + "] value too large");
            hex.putCode(expression.getEncValue(true));
        } else if (literalString != null)
            hex.putCode(this.getEncString(literalString));
        else if (opcode != null) opcode.pass4(hex);
    }
}
