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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class ExprInfix extends Node {
    private final static Map<Integer, BiFunction<Integer, Integer, Integer>> infixOps = new HashMap<>();

    static {
        infixOps.put(OP_ADD, Integer::sum);
        infixOps.put(OP_SUBTRACT, (x, y) -> x - y);
        infixOps.put(OP_DIVIDE, (x, y) -> x / y);  // can throw!
        infixOps.put(OP_MULTIPLY, (x, y) -> x * y);
        infixOps.put(OP_MOD, (x, y) -> x % y);
        infixOps.put(OP_MOD_2, (x, y) -> x % y);
        infixOps.put(OP_AND, (x, y) -> x & y);
        infixOps.put(OP_OR, (x, y) -> x | y);
        infixOps.put(OP_XOR, (x, y) -> x ^ y);
        infixOps.put(OP_SHL, (x, y) -> x << y);
        infixOps.put(OP_SHL_2, (x, y) -> x << y);
        infixOps.put(OP_SHR, (x, y) -> x >>> y);
        infixOps.put(OP_SHR_2, (x, y) -> x >>> y);
        infixOps.put(OP_EQUAL, (x, y) -> ((x.equals(y)) ? 1 : 0));
        infixOps.put(OP_LT, (x, y) -> (x < y) ? 1 : 0);
        infixOps.put(OP_LTE, (x, y) -> (x <= y) ? 1 : 0);
        infixOps.put(OP_GT, (x, y) -> (x > y) ? 1 : 0);
        infixOps.put(OP_GTE, (x, y) -> (x >= y) ? 1 : 0);
    }

    public final int operationCode;
    private final BiFunction<Integer, Integer, Integer> operation;

    public ExprInfix(int line, int column, int op) {
        super(line, column);
        this.operationCode = op;
        this.operation = Objects.requireNonNull(infixOps.get(op), "Unknown infix operation");
        // children are: left, right
    }

    public ExprInfix(Token op) {
        this(op.getLine(), op.getCharPositionInLine(), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        Node leftChild = getChild(0);
        Node rightChild = getChild(1);

        Optional<Evaluated> left = leftChild.eval(currentAddress, env);
        Optional<Evaluated> right = rightChild.eval(currentAddress, env);

        if (left.isPresent() && right.isPresent()) {
            int l = left.get().value;
            int r = right.get().value;
            return Optional.of(new Evaluated(line, column, operation.apply(l, r)));
        }

        return Optional.empty();
    }

    @Override
    protected String toStringShallow() {
        return "ExprInfix(" + operationCode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprInfix(line, column, operationCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprInfix exprInfix = (ExprInfix) o;
        return operationCode == exprInfix.operationCode;
    }
}
