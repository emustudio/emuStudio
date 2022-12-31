/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

import java.util.Optional;
import java.util.function.Function;

public class ExprNumber extends Node {
    public final int number;

    public ExprNumber(int line, int column, int number) {
        super(line, column);
        this.number = number;
    }

    public ExprNumber(Token number, Function<Token, Integer> parser) {
        this(number.getLine(), number.getCharPositionInLine(), parser.apply(number));
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return Optional.of(new Evaluated(line, column, number));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprNumber(" + number + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprNumber(line, column, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprNumber that = (ExprNumber) o;
        return number == that.number;
    }
}
