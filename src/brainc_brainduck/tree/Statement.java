/**
 * Statement.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package brainc_brainduck.tree;

import plugins.compiler.HEXFileHandler;

public class Statement {

    public final static int HALT = 0;
    public final static int INC = 1;
    public final static int DEC = 2;
    public final static int INCV = 3;
    public final static int DECV = 4;
    public final static int PRINT = 5;
    public final static int LOAD = 6;
    public final static int LOOP = 7;
    public final static int ENDL = 8;
    private int instr;
    private int param;

    public Statement(int instr, int param) {
        this.instr = instr;
        this.param = param;
    }

    // prvá fáza vracia nasledujúcu adresu
    // od adresy addr_start
    public int pass1(int addr_start) throws Exception {
        if (instr == LOOP || instr == ENDL) {
            return addr_start + 1;
        } else {
            return addr_start + 2;
        }
    }

    public void pass2(HEXFileHandler hex) {
        if (instr == LOOP || instr == ENDL) {
            hex.putCode(String.format("%1$02X", instr));
        } else {
            hex.putCode(String.format("%1$02X%2$02X", instr, param));
        }
    }
}
