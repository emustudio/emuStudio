/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.asZ80.ParsingUtils.parseLitString;

public class ExprString extends Node {
    public final String string;

    public ExprString(int line, int column, String string) {
        super(line, column);
        this.string = Objects.requireNonNull(string);
    }

    public ExprString(Token str) {
        this(str.getLine(), str.getCharPositionInLine(), parseLitString(str));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new ExprString(line, column, string);
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        if (string.length() == 1) {
            return Optional.of(new Evaluated(line, column, string.charAt(0) & 0xFF));
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprString that = (ExprString) o;
        return Objects.equals(string, that.string);
    }

    @Override
    protected String toStringShallow() {
        return "ExprString(" + string + ")";
    }
}
