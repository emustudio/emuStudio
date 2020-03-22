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
package net.emustudio.plugins.compiler.asZ80;

import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;
import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.Token;

public class TokenImpl extends ComplexSymbol implements Token, Symbols {
    public final static int ERROR_DECIMAL_SIZE = 0xA01;
    public final static int ERROR_UNCLOSED_CHAR = 0xA02;
    public final static int ERROR_UNCLOSED_STRING = 0xA03;
    public final static int ERROR_UNKNOWN_TOKEN = 0xA04;

    private final int category;
    private final int lexerState;

    public TokenImpl(int id, int category, int lexerState, String text, Location left, Location right) {
        super(text, id, left, right);
        this.category = category;
        this.lexerState = lexerState;
    }

    public TokenImpl(int id, int category, int lexerState, String text, Location left, Location right, Object value) {
        super(text, id, left, right, value);
        this.category = category;
        this.lexerState = lexerState;
    }


    @Override
    public int getID() {
        return super.sym;
    }

    @Override
    public int getType() {
        return this.category;
    }

    @Override
    public String getText() {
        return getName();
    }

    @Override
    public String getErrorString() {
        switch (super.sym) {
            case ERROR_DECIMAL_SIZE:
                return "Literal has too big size (max. is 65535)";
            case ERROR_UNCLOSED_CHAR:
                return "Char is not closed with single quote (')";
            case ERROR_UNCLOSED_STRING:
                return "String is not closed with single quote (')";
            case ERROR_UNKNOWN_TOKEN:
                return "Unknown token";
        }
        return "";
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
    public int getLexerState() {
        return lexerState;
    }
}
