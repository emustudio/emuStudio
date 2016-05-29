/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ram.compiler.impl;

import emulib.plugins.compiler.Token;
import java_cup.runtime.Symbol;

public class TokenImpl extends Symbol implements Token, Symbols {
    public final static int ERROR_UNKNOWN_TOKEN = 0xA05;
    private final String text; // hodnota tokenu
    private final int row;     // číslo riadka
    private final int col;     // číslo stĺpca
    private final int offset;  // pozícia tokenu
    private final int length;  // dĺžka tokenu
    private final int type;    // typ tokenu
    private final boolean initial;

    public TokenImpl(int ID, int type, String text,
            int line, int column, int offset, Object val, boolean initial) {
        super(ID, val);
        this.type = type;
        this.text = text;
        this.row = line;
        this.col = column;
        this.offset = offset;
        this.initial = initial;
        this.length = (text == null) ? 0 : text.length();
    }

    @Override
    public int getID() {
        return super.sym;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getErrorString() {
        switch (super.sym) {
            case ERROR_UNKNOWN_TOKEN:
                return "Unknown token";
        }
        return "";
    }

    @Override
    public int getLine() {
        return row;
    }

    @Override
    public int getColumn() {
        return col;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public boolean isInitialLexicalState() {
        return initial;
    }
}
