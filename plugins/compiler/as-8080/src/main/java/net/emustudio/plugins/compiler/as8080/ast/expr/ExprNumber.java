/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Optional;
import java.util.function.Function;

public class ExprNumber extends Node {
    public final int number;

    public ExprNumber(SourceCodePosition position, int number) {
        super(position);
        this.number = number;
    }

    public ExprNumber(String fileName, Token number, Function<Token, Integer> parser) {
        this(new SourceCodePosition(number.getLine(), number.getCharPositionInLine(), fileName), parser.apply(number));
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return Optional.of(new Evaluated(position, number));
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
        return new ExprNumber(position, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprNumber that = (ExprNumber) o;
        return number == that.number;
    }
}
