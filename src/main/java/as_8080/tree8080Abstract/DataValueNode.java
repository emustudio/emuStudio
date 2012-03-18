/*
 * DataValueNode.java
 *
 * Created on Sobota, 2007, september 29, 8:54
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

package as_8080.tree8080Abstract;

import as_8080.impl.CompileEnv;
import emulib.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public abstract class DataValueNode {
    protected int line;
    protected int column;
    
    public DataValueNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    /// compile time ///
    public abstract int getSize();
    public abstract int pass2(CompileEnv env, int addr_start) throws Exception;
    public abstract void pass4(HEXFileHandler hex) throws Exception;
    
        // encode string to hex codes
    protected String getEncString(String literal) {
        byte[] byts = literal.getBytes();
        String enc = "";
        
        for (int i = 0; i < byts.length; i++)
            enc += ExprNode.getEncValue((int)byts[i],true);
        return enc;
    }

    
}
