/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Map;
import java.util.Objects;
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
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        Evaluated childEval = getChild(0).eval(currentAddress, env);
        return childEval != null ? new Evaluated(position, operation.apply(childEval.value)) : null;
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
