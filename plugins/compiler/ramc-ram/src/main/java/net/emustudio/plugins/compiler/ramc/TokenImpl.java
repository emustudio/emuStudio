/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.ramc;

import java_cup.runtime.Symbol;
import net.emustudio.emulib.plugins.compiler.Token;

public class TokenImpl extends Symbol implements Token, Symbols {
    public final static int ERROR_UNKNOWN_TOKEN = 0xA05;
    private final String text;
    private final int row;
    private final int col;
    private final int offset;
    private final int length;
    private final int type;
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
        if (super.sym == ERROR_UNKNOWN_TOKEN) {
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
