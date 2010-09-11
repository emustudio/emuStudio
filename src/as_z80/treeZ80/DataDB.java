/*
 * DBData.java
 *
 * Created on Streda, 2008, august 13, 11:46
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

import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.DataValue;
import as_z80.treeZ80Abstract.Expression;
import as_z80.treeZ80Abstract.Instruction;
import emuLib8.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class DataDB extends DataValue {

    private Expression expression = null;
    private String literalString = null;
    private Instruction opcode = null;

    /** Creates a new instance of DBData */
    public DataDB(Expression expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }

    public DataDB(String literalString, int line, int column) {
        super(line, column);
        this.literalString = literalString;
    }

    public DataDB(Instruction opcode, int line, int column) {
        super(line, column);
        this.opcode = opcode;
    }

    /// compile time ///
    @Override
    public int getSize() {
        if (expression != null) {
            return 1;
        } else if (literalString != null) {
            return literalString.length();
        } else if (opcode != null) {
            return opcode.getSize();
        }
        return 0;
    }

    @Override
    public void pass1() throws Exception {
        if (opcode != null) {
            opcode.pass1();
        }
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        int next;
        if (expression != null) {
            expression.eval(env, addr_start);
            next = addr_start + 1;
        } else if (literalString != null) {
            next = addr_start + literalString.length();
        } else if (opcode != null) {
            next = opcode.pass2(env, addr_start);
        } else {
            next = addr_start;
        }
        return next;
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        if (expression != null) {
            String s = expression.encodeValue(1);
            if (s.length() > 2) {
                throw new Exception("[" + line + "," + column
                        + "] Error: value too large");
            }
            hex.putCode(s);
        } else if (literalString != null) {
            hex.putCode(this.encodeValue(literalString));
        } else if (opcode != null) {
            opcode.pass4(hex);
        }
    }
}
