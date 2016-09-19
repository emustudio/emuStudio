/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.compiler;

import emulib.plugins.compiler.Token;
import java_cup.runtime.ComplexSymbolFactory;

/**
 *
 * @author miso
 */
public class TokenImpl extends ComplexSymbolFactory.ComplexSymbol implements Token, Symbols {

    /**
     * The type of the Token (RESERVED, PREPROCESSOR...)
     *
     */
    private final int type;

    /**
     * 0-based starting offset of token position
     */
    private final int offset;

    public TokenImpl(int id, int type, String text, int line, int column, int offset) {
        super(text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column));
        this.type = type;
        this.offset = offset;
    }

    public TokenImpl(int id, int type, String text, int line, int column, int offset, Object value) {
        super(text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column), value);
        this.type = type;
        this.offset = offset;
    }

    @Override
    public int getID() {
        return super.sym;
    }

    @Override
    public int getType() {
        return this.type;
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
        return this.offset;
    }

    @Override
    public int getLength() {
        return getName().length();
    }

    @Override
    public String getErrorString() {
        return "Invalid token";
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
