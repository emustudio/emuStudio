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
package net.emustudio.plugins.compiler.ssem;

import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;
import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.Token;

public class TokenImpl extends ComplexSymbol implements Token, Symbols {
    private final int category;

    public TokenImpl(int id, int category, String text, Location left, Location right) {
        super(text, id, left, right);
        this.category = category;
    }

    public TokenImpl(int id, int category, String text, Location left, Location right, Object value) {
        super(text, id, left, right, value);
        this.category = category;
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
        return getLeft().getLine();
    }

    @Override
    public int getColumn() {
        return getLeft().getColumn();
    }

    @Override
    public int getOffset() {
        return getLeft().getOffset();
    }

    @Override
    public int getLength() {
        return getRight().getOffset() - getLeft().getOffset();
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
        return super.sym != BNUM;
    }
}
