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

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class ExprUnary extends Node {
    private final static Map<Integer, Function<Integer, Integer>> unaryOps = Map.of(
            OP_ADD, x -> x,
            OP_SUBTRACT, x -> -x,
            OP_NOT, x -> ~x,
            OP_NOT_2, x -> ~x
    );
    public final int operationCode;
    private final Function<Integer, Integer> operation;

    public ExprUnary(SourceCodePosition position, int op) {
        super(position);
        this.operationCode = op;
        this.operation = Objects.requireNonNull(unaryOps.get(op), "Unknown unary operation");
        // child is expr
    }

    public ExprUnary(String fileName, Token op) {
        this(positionFromToken(fileName, op), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return getChild(0)
                .eval(currentAddress, env)
                .map(childEval -> new Evaluated(position, operation.apply(childEval.value)));
    }

    @Override
    protected String toStringShallow() {
        return "ExprUnary(" + operationCode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprUnary(position, operationCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprUnary exprUnary = (ExprUnary) o;
        return operationCode == exprUnary.operationCode;
    }
}
