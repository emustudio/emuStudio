/*
 * TokenRAM.java
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
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

package ramc_ram.impl;

import java_cup.runtime.Symbol;
import plugins.compiler.IToken;

/**
 *
 * @author Peter Jakubčo <pjakubco at gmail.com>
 */
public class TokenRAM extends Symbol implements IToken,SymRAM {
    public final static int ERROR_UNKNOWN_TOKEN = 0xA05;

    private String text; // hodnota tokenu
    private int row;     // číslo riadka
    private int col;     // číslo stĺpca
    private int offset;  // pozícia tokenu
    private int length;  // dĺžka tokenu
    private int type;    // typ tokenu
    private boolean initial;
 
    public TokenRAM(int ID, int type, String text,
    		int line, int column, int offset, Object val, boolean initial) {
        super(ID,val);
        this.type = type;
        this.text = text;
        this.row = line;
        this.col = column;
        this.offset = offset;
        this.initial = initial;
        this.length = (text==null)?0:text.length();
    }

    public int getID() { return super.sym; }
    public int getType() { return type; }

    public String getText() { return text; }
    public String getErrorString() {
        switch (super.sym) {
            case ERROR_UNKNOWN_TOKEN: return "Unknown token";
        }
        return "";
    }
    public int getLine() { return row; }
    public int getColumn() { return col; }
    public int getOffset() { return offset; }
    public int getLength() { return length; }
    public boolean isInitialLexicalState() { return initial; }
}
