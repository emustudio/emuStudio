/*
 * Copyright (C) 2016 Peter Jakubčo
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
 *
 */
package net.sf.emustudio.ssem.assembler;

import emulib.plugins.compiler.Token;
import java_cup.runtime.ComplexSymbolFactory;

public class TokenImpl extends ComplexSymbolFactory.ComplexSymbol implements Token, Symbols {
    private final int category;
    private final int cchar;

    public TokenImpl(int id, int category, String text, int line, int column, int cchar) {
        super(
            text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column)
        );
        this.category = category;
        this.cchar = cchar;
    }

    public TokenImpl(int id, int category, String text, int line, int column, int cchar, Object value) {
        super(
            text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column), value
        );
        this.category = category;
        this.cchar = cchar;
    }

    @Override
    public int getID() {
        return super.sym;
    }

    @Override
    public int getType() {
        return category;
    }

    @Override
    public int getLine() {
        return super.getLeft().getLine();
    }

    @Override
    public int getColumn() {
        return super.getLeft().getColumn();
    }

    @Override
    public int getOffset() {
        return cchar;
    }

    @Override
    public int getLength() {
        return getName().length();
    }

    @Override
    public String getErrorString() {
        return "Unknown token";
    }

    @Override
    public String getText() {
        return getName();
    }

    @Override
    public boolean isInitialLexicalState() {
        return true;
    }
}
