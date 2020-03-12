/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.raspc;

import java_cup.runtime.ComplexSymbolFactory;
import net.emustudio.emulib.plugins.compiler.Token;

public class TokenImpl extends ComplexSymbolFactory.ComplexSymbol implements Token, Symbols {

    /**
     * The type of the Token (RESERVED, PREPROCESSOR...)
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
